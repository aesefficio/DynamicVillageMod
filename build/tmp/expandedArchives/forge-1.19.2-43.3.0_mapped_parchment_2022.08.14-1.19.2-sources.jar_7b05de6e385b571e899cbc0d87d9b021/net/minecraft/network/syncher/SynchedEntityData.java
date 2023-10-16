package net.minecraft.network.syncher;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;

/**
 * Keeps data in sync from server to client for an entity.
 * A maximum of 254 parameters per entity class can be registered. The system then ensures that these values are updated
 * on the client whenever they change on the server.
 * 
 * Use {@link #defineId} to register a piece of data for your entity class.
 * Use {@link #define} during {@link Entity#defineSynchedData} to set the default value for a given parameter.
 */
public class SynchedEntityData {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Object2IntMap<Class<? extends Entity>> ENTITY_ID_POOL = new Object2IntOpenHashMap<>();
   private static final int EOF_MARKER = 255;
   private static final int MAX_ID_VALUE = 254;
   private final Entity entity;
   private final Int2ObjectMap<SynchedEntityData.DataItem<?>> itemsById = new Int2ObjectOpenHashMap<>();
   private final ReadWriteLock lock = new ReentrantReadWriteLock();
   private boolean isEmpty = true;
   private boolean isDirty;

   public SynchedEntityData(Entity pEntity) {
      this.entity = pEntity;
   }

   /**
    * Register a piece of data to be kept in sync for an entity class.
    * This method must be called during a static initializer of an entity class and the first parameter of this method
    * must be that entity class.
    */
   public static <T> EntityDataAccessor<T> defineId(Class<? extends Entity> pClazz, EntityDataSerializer<T> pSerializer) {
      if (true || LOGGER.isDebugEnabled()) { // Forge: This is very useful for mods that register keys on classes that are not their own
         try {
            Class<?> oclass = Class.forName(Thread.currentThread().getStackTrace()[2].getClassName());
            if (!oclass.equals(pClazz)) {
               // Forge: log at warn, mods should not add to classes that they don't own, and only add stacktrace when in debug is enabled as it is mostly not needed and consumes time
               if (LOGGER.isDebugEnabled()) LOGGER.warn("defineId called for: {} from {}", pClazz, oclass, new RuntimeException());
               else LOGGER.warn("defineId called for: {} from {}", pClazz, oclass);
            }
         } catch (ClassNotFoundException classnotfoundexception) {
         }
      }

      int j;
      if (ENTITY_ID_POOL.containsKey(pClazz)) {
         j = ENTITY_ID_POOL.getInt(pClazz) + 1;
      } else {
         int i = 0;
         Class<?> oclass1 = pClazz;

         while(oclass1 != Entity.class) {
            oclass1 = oclass1.getSuperclass();
            if (ENTITY_ID_POOL.containsKey(oclass1)) {
               i = ENTITY_ID_POOL.getInt(oclass1) + 1;
               break;
            }
         }

         j = i;
      }

      if (j > 254) {
         throw new IllegalArgumentException("Data value id is too big with " + j + "! (Max is 254)");
      } else {
         ENTITY_ID_POOL.put(pClazz, j);
         return pSerializer.createAccessor(j);
      }
   }

   /**
    * Set the default value for a data parameter. Call this during {@link Entity#defineSynchedData}.
    */
   public <T> void define(EntityDataAccessor<T> pKey, T pValue) {
      int i = pKey.getId();
      if (i > 254) {
         throw new IllegalArgumentException("Data value id is too big with " + i + "! (Max is 254)");
      } else if (this.itemsById.containsKey(i)) {
         throw new IllegalArgumentException("Duplicate id value for " + i + "!");
      } else if (EntityDataSerializers.getSerializedId(pKey.getSerializer()) < 0) {
         throw new IllegalArgumentException("Unregistered serializer " + pKey.getSerializer() + " for " + i + "!");
      } else {
         this.createDataItem(pKey, pValue);
      }
   }

   private <T> void createDataItem(EntityDataAccessor<T> pKey, T pValue) {
      SynchedEntityData.DataItem<T> dataitem = new SynchedEntityData.DataItem<>(pKey, pValue);
      this.lock.writeLock().lock();
      this.itemsById.put(pKey.getId(), dataitem);
      this.isEmpty = false;
      this.lock.writeLock().unlock();
   }

   private <T> SynchedEntityData.DataItem<T> getItem(EntityDataAccessor<T> pKey) {
      this.lock.readLock().lock();

      SynchedEntityData.DataItem<T> dataitem;
      try {
         dataitem = (SynchedEntityData.DataItem<T>)this.itemsById.get(pKey.getId());
      } catch (Throwable throwable) {
         CrashReport crashreport = CrashReport.forThrowable(throwable, "Getting synched entity data");
         CrashReportCategory crashreportcategory = crashreport.addCategory("Synched entity data");
         crashreportcategory.setDetail("Data ID", pKey);
         throw new ReportedException(crashreport);
      } finally {
         this.lock.readLock().unlock();
      }

      return dataitem;
   }

   /**
    * Get the value of the given key for this entity.
    */
   public <T> T get(EntityDataAccessor<T> pKey) {
      return this.getItem(pKey).getValue();
   }

   /**
    * Set the value of the given key for this entity.
    */
   public <T> void set(EntityDataAccessor<T> pKey, T pValue) {
      SynchedEntityData.DataItem<T> dataitem = this.getItem(pKey);
      if (ObjectUtils.notEqual(pValue, dataitem.getValue())) {
         dataitem.setValue(pValue);
         this.entity.onSyncedDataUpdated(pKey);
         dataitem.setDirty(true);
         this.isDirty = true;
      }

   }

   /**
    * Whether any keys have changed since the last synchronization packet to the client.
    */
   public boolean isDirty() {
      return this.isDirty;
   }

   /**
    * Encode the given data entries into the buffer.
    */
   public static void pack(@Nullable List<SynchedEntityData.DataItem<?>> pEntries, FriendlyByteBuf pBuffer) {
      if (pEntries != null) {
         for(SynchedEntityData.DataItem<?> dataitem : pEntries) {
            writeDataItem(pBuffer, dataitem);
         }
      }

      pBuffer.writeByte(255);
   }

   /**
    * Gets all data entries which have changed since the last check and clears their dirty flag.
    */
   @Nullable
   public List<SynchedEntityData.DataItem<?>> packDirty() {
      List<SynchedEntityData.DataItem<?>> list = null;
      if (this.isDirty) {
         this.lock.readLock().lock();

         for(SynchedEntityData.DataItem<?> dataitem : this.itemsById.values()) {
            if (dataitem.isDirty()) {
               dataitem.setDirty(false);
               if (list == null) {
                  list = Lists.newArrayList();
               }

               list.add(dataitem.copy());
            }
         }

         this.lock.readLock().unlock();
      }

      this.isDirty = false;
      return list;
   }

   /**
    * Get all values from the data entries.
    */
   @Nullable
   public List<SynchedEntityData.DataItem<?>> getAll() {
      List<SynchedEntityData.DataItem<?>> list = null;
      this.lock.readLock().lock();

      for(SynchedEntityData.DataItem<?> dataitem : this.itemsById.values()) {
         if (list == null) {
            list = Lists.newArrayList();
         }

         list.add(dataitem.copy());
      }

      this.lock.readLock().unlock();
      return list;
   }

   private static <T> void writeDataItem(FriendlyByteBuf pBuffer, SynchedEntityData.DataItem<T> pEntry) {
      EntityDataAccessor<T> entitydataaccessor = pEntry.getAccessor();
      int i = EntityDataSerializers.getSerializedId(entitydataaccessor.getSerializer());
      if (i < 0) {
         throw new EncoderException("Unknown serializer type " + entitydataaccessor.getSerializer());
      } else {
         pBuffer.writeByte(entitydataaccessor.getId());
         pBuffer.writeVarInt(i);
         entitydataaccessor.getSerializer().write(pBuffer, pEntry.getValue());
      }
   }

   /**
    * Decode the data written by {@link #pack}.
    */
   @Nullable
   public static List<SynchedEntityData.DataItem<?>> unpack(FriendlyByteBuf pBuffer) {
      List<SynchedEntityData.DataItem<?>> list = null;

      int i;
      while((i = pBuffer.readUnsignedByte()) != 255) {
         if (list == null) {
            list = Lists.newArrayList();
         }

         int j = pBuffer.readVarInt();
         EntityDataSerializer<?> entitydataserializer = EntityDataSerializers.getSerializer(j);
         if (entitydataserializer == null) {
            throw new DecoderException("Unknown serializer type " + j);
         }

         list.add(genericHelper(pBuffer, i, entitydataserializer));
      }

      return list;
   }

   private static <T> SynchedEntityData.DataItem<T> genericHelper(FriendlyByteBuf pBuffer, int pId, EntityDataSerializer<T> pSerializer) {
      return new SynchedEntityData.DataItem<>(pSerializer.createAccessor(pId), pSerializer.read(pBuffer));
   }

   /**
    * Updates the data using the given entries. Used on the client when the update packet is received.
    */
   public void assignValues(List<SynchedEntityData.DataItem<?>> pEntries) {
      this.lock.writeLock().lock();

      try {
         for(SynchedEntityData.DataItem<?> dataitem : pEntries) {
            SynchedEntityData.DataItem<?> dataitem1 = this.itemsById.get(dataitem.getAccessor().getId());
            if (dataitem1 != null) {
               this.assignValue(dataitem1, dataitem);
               this.entity.onSyncedDataUpdated(dataitem.getAccessor());
            }
         }
      } finally {
         this.lock.writeLock().unlock();
      }

      this.isDirty = true;
   }

   private <T> void assignValue(SynchedEntityData.DataItem<T> pTarget, SynchedEntityData.DataItem<?> pSource) {
      if (!Objects.equals(pSource.accessor.getSerializer(), pTarget.accessor.getSerializer())) {
         throw new IllegalStateException(String.format(Locale.ROOT, "Invalid entity data item type for field %d on entity %s: old=%s(%s), new=%s(%s)", pTarget.accessor.getId(), this.entity, pTarget.value, pTarget.value.getClass(), pSource.value, pSource.value.getClass()));
      } else {
         pTarget.setValue((T)pSource.getValue());
      }
   }

   public boolean isEmpty() {
      return this.isEmpty;
   }

   /**
    * Clears the dirty flag, marking all entries as unchanged.
    */
   public void clearDirty() {
      this.isDirty = false;
      this.lock.readLock().lock();

      for(SynchedEntityData.DataItem<?> dataitem : this.itemsById.values()) {
         dataitem.setDirty(false);
      }

      this.lock.readLock().unlock();
   }

   public static class DataItem<T> {
      final EntityDataAccessor<T> accessor;
      T value;
      private boolean dirty;

      public DataItem(EntityDataAccessor<T> pAccessor, T pValue) {
         this.accessor = pAccessor;
         this.value = pValue;
         this.dirty = true;
      }

      public EntityDataAccessor<T> getAccessor() {
         return this.accessor;
      }

      public void setValue(T pValue) {
         this.value = pValue;
      }

      public T getValue() {
         return this.value;
      }

      public boolean isDirty() {
         return this.dirty;
      }

      public void setDirty(boolean pDirty) {
         this.dirty = pDirty;
      }

      public SynchedEntityData.DataItem<T> copy() {
         return new SynchedEntityData.DataItem<>(this.accessor, this.accessor.getSerializer().copy(this.value));
      }
   }
}
