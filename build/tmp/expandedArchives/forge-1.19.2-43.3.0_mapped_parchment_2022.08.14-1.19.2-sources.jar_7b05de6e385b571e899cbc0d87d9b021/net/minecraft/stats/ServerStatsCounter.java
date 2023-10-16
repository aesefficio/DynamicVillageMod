package net.minecraft.stats;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.ClientboundAwardStatsPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.player.Player;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

/**
 * Server-side implementation of {@link net.minecraft.stats.StatsCounter}; handles counting, serialising, and de-
 * serialising statistics, as well as sending them to connected clients via the {@linkplain
 * net.minecraft.network.protocol.game.ClientboundAwardStatsPacket award stats packet}.
 */
public class ServerStatsCounter extends StatsCounter {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final MinecraftServer server;
   private final File file;
   private final Set<Stat<?>> dirty = Sets.newHashSet();

   public ServerStatsCounter(MinecraftServer pServer, File pFile) {
      this.server = pServer;
      this.file = pFile;
      if (pFile.isFile()) {
         try {
            this.parseLocal(pServer.getFixerUpper(), FileUtils.readFileToString(pFile));
         } catch (IOException ioexception) {
            LOGGER.error("Couldn't read statistics file {}", pFile, ioexception);
         } catch (JsonParseException jsonparseexception) {
            LOGGER.error("Couldn't parse statistics file {}", pFile, jsonparseexception);
         }
      }

   }

   public void save() {
      try {
         FileUtils.writeStringToFile(this.file, this.toJson());
      } catch (IOException ioexception) {
         LOGGER.error("Couldn't save stats", (Throwable)ioexception);
      }

   }

   public void setValue(Player pPlayer, Stat<?> pStat, int pValue) {
      super.setValue(pPlayer, pStat, pValue);
      this.dirty.add(pStat);
   }

   private Set<Stat<?>> getDirty() {
      Set<Stat<?>> set = Sets.newHashSet(this.dirty);
      this.dirty.clear();
      return set;
   }

   public void parseLocal(DataFixer pFixerUpper, String pJson) {
      try {
         JsonReader jsonreader = new JsonReader(new StringReader(pJson));

         label51: {
            try {
               jsonreader.setLenient(false);
               JsonElement jsonelement = Streams.parse(jsonreader);
               if (!jsonelement.isJsonNull()) {
                  CompoundTag compoundtag = fromJson(jsonelement.getAsJsonObject());
                  if (!compoundtag.contains("DataVersion", 99)) {
                     compoundtag.putInt("DataVersion", 1343);
                  }

                  compoundtag = NbtUtils.update(pFixerUpper, DataFixTypes.STATS, compoundtag, compoundtag.getInt("DataVersion"));
                  if (!compoundtag.contains("stats", 10)) {
                     break label51;
                  }

                  CompoundTag compoundtag1 = compoundtag.getCompound("stats");
                  Iterator iterator = compoundtag1.getAllKeys().iterator();

                  while(true) {
                     if (!iterator.hasNext()) {
                        break label51;
                     }

                     String s = (String)iterator.next();
                     if (compoundtag1.contains(s, 10)) {
                        Util.ifElse(Registry.STAT_TYPE.getOptional(new ResourceLocation(s)), (p_12844_) -> {
                           CompoundTag compoundtag2 = compoundtag1.getCompound(s);

                           for(String s1 : compoundtag2.getAllKeys()) {
                              if (compoundtag2.contains(s1, 99)) {
                                 Util.ifElse(this.getStat(p_12844_, s1), (p_144252_) -> {
                                    this.stats.put(p_144252_, compoundtag2.getInt(s1));
                                 }, () -> {
                                    LOGGER.warn("Invalid statistic in {}: Don't know what {} is", this.file, s1);
                                 });
                              } else {
                                 LOGGER.warn("Invalid statistic value in {}: Don't know what {} is for key {}", this.file, compoundtag2.get(s1), s1);
                              }
                           }

                        }, () -> {
                           LOGGER.warn("Invalid statistic type in {}: Don't know what {} is", this.file, s);
                        });
                     }
                  }
               }

               LOGGER.error("Unable to parse Stat data from {}", (Object)this.file);
            } catch (Throwable throwable1) {
               try {
                  jsonreader.close();
               } catch (Throwable throwable) {
                  throwable1.addSuppressed(throwable);
               }

               throw throwable1;
            }

            jsonreader.close();
            return;
         }

         jsonreader.close();
      } catch (IOException | JsonParseException jsonparseexception) {
         LOGGER.error("Unable to parse Stat data from {}", this.file, jsonparseexception);
      }

   }

   private <T> Optional<Stat<T>> getStat(StatType<T> pType, String pLocation) {
      return Optional.ofNullable(ResourceLocation.tryParse(pLocation)).flatMap(pType.getRegistry()::getOptional).map(pType::get);
   }

   private static CompoundTag fromJson(JsonObject pJson) {
      CompoundTag compoundtag = new CompoundTag();

      for(Map.Entry<String, JsonElement> entry : pJson.entrySet()) {
         JsonElement jsonelement = entry.getValue();
         if (jsonelement.isJsonObject()) {
            compoundtag.put(entry.getKey(), fromJson(jsonelement.getAsJsonObject()));
         } else if (jsonelement.isJsonPrimitive()) {
            JsonPrimitive jsonprimitive = jsonelement.getAsJsonPrimitive();
            if (jsonprimitive.isNumber()) {
               compoundtag.putInt(entry.getKey(), jsonprimitive.getAsInt());
            }
         }
      }

      return compoundtag;
   }

   protected String toJson() {
      Map<StatType<?>, JsonObject> map = Maps.newHashMap();

      for(Object2IntMap.Entry<Stat<?>> entry : this.stats.object2IntEntrySet()) {
         Stat<?> stat = entry.getKey();
         map.computeIfAbsent(stat.getType(), (p_12822_) -> {
            return new JsonObject();
         }).addProperty(getKey(stat).toString(), entry.getIntValue());
      }

      JsonObject jsonobject = new JsonObject();

      for(Map.Entry<StatType<?>, JsonObject> entry1 : map.entrySet()) {
         jsonobject.add(Registry.STAT_TYPE.getKey(entry1.getKey()).toString(), entry1.getValue());
      }

      JsonObject jsonobject1 = new JsonObject();
      jsonobject1.add("stats", jsonobject);
      jsonobject1.addProperty("DataVersion", SharedConstants.getCurrentVersion().getWorldVersion());
      return jsonobject1.toString();
   }

   private static <T> ResourceLocation getKey(Stat<T> pStat) {
      return pStat.getType().getRegistry().getKey(pStat.getValue());
   }

   public void markAllDirty() {
      this.dirty.addAll(this.stats.keySet());
   }

   public void sendStats(ServerPlayer pPlayer) {
      Object2IntMap<Stat<?>> object2intmap = new Object2IntOpenHashMap<>();

      for(Stat<?> stat : this.getDirty()) {
         object2intmap.put(stat, this.getValue(stat));
      }

      pPlayer.connection.send(new ClientboundAwardStatsPacket(object2intmap));
   }
}