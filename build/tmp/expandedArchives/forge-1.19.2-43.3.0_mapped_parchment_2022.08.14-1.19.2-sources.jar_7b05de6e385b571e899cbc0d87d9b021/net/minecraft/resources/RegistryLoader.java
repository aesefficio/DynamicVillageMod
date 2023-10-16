package net.minecraft.resources;

import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.WritableRegistry;

public class RegistryLoader {
   private final RegistryResourceAccess resources;
   private final Map<ResourceKey<? extends Registry<?>>, RegistryLoader.ReadCache<?>> readCache = new IdentityHashMap<>();

   RegistryLoader(RegistryResourceAccess pResources) {
      this.resources = pResources;
   }

   public <E> DataResult<? extends Registry<E>> overrideRegistryFromResources(WritableRegistry<E> p_206763_, ResourceKey<? extends Registry<E>> p_206764_, Codec<E> p_206765_, DynamicOps<JsonElement> p_206766_) {
      Map<ResourceKey<E>, RegistryResourceAccess.EntryThunk<E>> map = this.resources.listResources(p_206764_);
      DataResult<WritableRegistry<E>> dataresult = DataResult.success(p_206763_, Lifecycle.stable());

      for(Map.Entry<ResourceKey<E>, RegistryResourceAccess.EntryThunk<E>> entry : net.minecraftforge.common.ForgeHooks.filterThunks(map)) {
         dataresult = dataresult.flatMap((p_214227_) -> {
            return this.overrideElementFromResources(p_214227_, p_206764_, p_206765_, entry.getKey(), Optional.of(entry.getValue()), p_206766_).map((p_206761_) -> {
               return p_214227_;
            });
         });
      }

      return dataresult.setPartial(p_206763_);
   }

   <E> DataResult<Holder<E>> overrideElementFromResources(WritableRegistry<E> p_206768_, ResourceKey<? extends Registry<E>> p_206769_, Codec<E> p_206770_, ResourceKey<E> p_206771_, DynamicOps<JsonElement> p_206772_) {
      Optional<RegistryResourceAccess.EntryThunk<E>> optional = this.resources.getResource(p_206771_);
      return this.overrideElementFromResources(p_206768_, p_206769_, p_206770_, p_206771_, optional, p_206772_);
   }

   private <E> DataResult<Holder<E>> overrideElementFromResources(WritableRegistry<E> p_214229_, ResourceKey<? extends Registry<E>> p_214230_, Codec<E> p_214231_, ResourceKey<E> p_214232_, Optional<RegistryResourceAccess.EntryThunk<E>> p_214233_, DynamicOps<JsonElement> p_214234_) {
      RegistryLoader.ReadCache<E> readcache = this.readCache(p_214230_);
      DataResult<Holder<E>> dataresult = readcache.values.get(p_214232_);
      if (dataresult != null) {
         return dataresult;
      } else {
         // Prevents an exception from disrupting codec decoding when calling this with prematurely frozen registries - see comment in LevelStorageSource
         DataResult<Holder<E>> maybeHolder = p_214229_.getOrCreateHolder(p_214232_);
         if (maybeHolder.error().isPresent()) {
            return maybeHolder;
         }
         Holder<E> holder = maybeHolder.result().get();
         readcache.values.put(p_214232_, DataResult.success(holder));
         DataResult<Holder<E>> dataresult1;
         if (p_214233_.isEmpty()) {
            if (p_214229_.containsKey(p_214232_)) {
               dataresult1 = DataResult.success(holder, Lifecycle.stable());
            } else {
               dataresult1 = DataResult.error("Missing referenced custom/removed registry entry for registry " + p_214230_ + " named " + p_214232_.location());
            }
         } else {
            DataResult<RegistryResourceAccess.ParsedEntry<E>> dataresult2 = p_214233_.get().parseElement(p_214234_, p_214231_);
            Optional<RegistryResourceAccess.ParsedEntry<E>> optional = dataresult2.result();
            if (optional.isPresent()) {
               RegistryResourceAccess.ParsedEntry<E> parsedentry = optional.get();
               p_214229_.registerOrOverride(parsedentry.fixedId(), p_214232_, parsedentry.value(), dataresult2.lifecycle());
            }

            dataresult1 = dataresult2.map((p_206756_) -> {
               return holder;
            });
         }

         readcache.values.put(p_214232_, dataresult1);
         return dataresult1;
      }
   }

   private <E> RegistryLoader.ReadCache<E> readCache(ResourceKey<? extends Registry<E>> p_206774_) {
      return (RegistryLoader.ReadCache<E>)this.readCache.computeIfAbsent(p_206774_, (p_206782_) -> {
         return new RegistryLoader.ReadCache();
      });
   }

   public RegistryLoader.Bound bind(RegistryAccess.Writable p_206758_) {
      return new RegistryLoader.Bound(p_206758_, this);
   }

   public static record Bound(RegistryAccess.Writable access, RegistryLoader loader) {
      public <E> DataResult<? extends Registry<E>> overrideRegistryFromResources(ResourceKey<? extends Registry<E>> p_206790_, Codec<E> p_206791_, DynamicOps<JsonElement> p_206792_) {
         WritableRegistry<E> writableregistry = this.access.ownedWritableRegistryOrThrow(p_206790_);
         return this.loader.overrideRegistryFromResources(writableregistry, p_206790_, p_206791_, p_206792_);
      }

      public <E> DataResult<Holder<E>> overrideElementFromResources(ResourceKey<? extends Registry<E>> p_206794_, Codec<E> p_206795_, ResourceKey<E> p_206796_, DynamicOps<JsonElement> p_206797_) {
         WritableRegistry<E> writableregistry = this.access.ownedWritableRegistryOrThrow(p_206794_);
         return this.loader.overrideElementFromResources(writableregistry, p_206794_, p_206795_, p_206796_, p_206797_);
      }
   }

   static final class ReadCache<E> {
      final Map<ResourceKey<E>, DataResult<Holder<E>>> values = Maps.newIdentityHashMap();
   }
}
