package net.minecraft.core;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.resources.HolderSetCodec;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.resources.RegistryLoader;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;

public class RegistryCodecs {
   private static <T> MapCodec<RegistryCodecs.RegistryEntry<T>> withNameAndId(ResourceKey<? extends Registry<T>> pRegistryKey, MapCodec<T> pElementCodec) {
      return RecordCodecBuilder.mapCodec((p_206309_) -> {
         return p_206309_.group(ResourceKey.codec(pRegistryKey).fieldOf("name").forGetter(RegistryCodecs.RegistryEntry::key), Codec.INT.fieldOf("id").forGetter(RegistryCodecs.RegistryEntry::id), pElementCodec.forGetter(RegistryCodecs.RegistryEntry::value)).apply(p_206309_, RegistryCodecs.RegistryEntry::new);
      });
   }

   public static <T> Codec<Registry<T>> networkCodec(ResourceKey<? extends Registry<T>> pRegistryKey, Lifecycle pLifecycle, Codec<T> pElementCodec) {
      return withNameAndId(pRegistryKey, pElementCodec.fieldOf("element")).codec().listOf().xmap((p_206298_) -> {
         WritableRegistry<T> writableregistry = new MappedRegistry<>(pRegistryKey, pLifecycle, (Function<T, Holder.Reference<T>>)null);

         for(RegistryCodecs.RegistryEntry<T> registryentry : p_206298_) {
            writableregistry.registerMapping(registryentry.id(), registryentry.key(), registryentry.value(), pLifecycle);
         }

         return writableregistry;
      }, (p_206314_) -> {
         ImmutableList.Builder<RegistryCodecs.RegistryEntry<T>> builder = ImmutableList.builder();

         for(T t : p_206314_) {
            builder.add(new RegistryCodecs.RegistryEntry<>(p_206314_.getResourceKey(t).get(), p_206314_.getId(t), t));
         }

         return builder.build();
      });
   }

   public static <E> Codec<Registry<E>> dataPackAwareCodec(ResourceKey<? extends Registry<E>> pRegistryKey, Lifecycle pLifecycle, Codec<E> pElementCodec) {
      Codec<Map<ResourceKey<E>, E>> codec = directCodec(pRegistryKey, pElementCodec);
      Encoder<Registry<E>> encoder = codec.comap((p_206271_) -> {
         return ImmutableMap.copyOf(p_206271_.entrySet());
      });
      return Codec.of(encoder, dataPackAwareDecoder(pRegistryKey, pElementCodec, codec, pLifecycle), "DataPackRegistryCodec for " + pRegistryKey);
   }

   private static <E> Decoder<Registry<E>> dataPackAwareDecoder(final ResourceKey<? extends Registry<E>> pRegistryKey, final Codec<E> pElementCodec, Decoder<Map<ResourceKey<E>, E>> pDirectDecoder, Lifecycle pLifecycle) {
      final Decoder<WritableRegistry<E>> decoder = pDirectDecoder.map((p_206302_) -> {
         WritableRegistry<E> writableregistry = new MappedRegistry<>(pRegistryKey, pLifecycle, (Function<E, Holder.Reference<E>>)null);
         p_206302_.forEach((p_206275_, p_206276_) -> {
            writableregistry.register(p_206275_, p_206276_, pLifecycle);
         });
         return writableregistry;
      });
      return new Decoder<Registry<E>>() {
         public <T> DataResult<Pair<Registry<E>, T>> decode(DynamicOps<T> p_206352_, T p_206353_) {
            DataResult<Pair<WritableRegistry<E>, T>> dataresult = decoder.decode(p_206352_, p_206353_);
            if (p_206352_ instanceof RegistryOps<?> registryops) {
               return registryops.registryLoader().map((p_206338_) -> {
                  return this.overrideFromResources(dataresult, registryops, p_206338_.loader());
               }).orElseGet(() -> {
                  return DataResult.error("Can't load registry with this ops");
               });
            } else {
               return dataresult.map((p_206331_) -> {
                  return p_206331_.mapFirst((p_206344_) -> {
                     return p_206344_;
                  });
               });
            }
         }

         private <T> DataResult<Pair<Registry<E>, T>> overrideFromResources(DataResult<Pair<WritableRegistry<E>, T>> p_206340_, RegistryOps<?> p_206341_, RegistryLoader p_206342_) {
            return p_206340_.flatMap((p_206350_) -> {
               return p_206342_.overrideRegistryFromResources(p_206350_.getFirst(), pRegistryKey, pElementCodec, p_206341_.getAsJson()).map((p_206334_) -> {
                  return Pair.of(p_206334_, (T)p_206350_.getSecond());
               });
            });
         }
      };
   }

   private static <T> Codec<Map<ResourceKey<T>, T>> directCodec(ResourceKey<? extends Registry<T>> pRegistryKey, Codec<T> pElementCodec) {
      // FORGE: Fix MC-197860
      return new net.minecraftforge.common.LenientUnboundedMapCodec<>(ResourceKey.codec(pRegistryKey), pElementCodec);
   }

   public static <E> Codec<HolderSet<E>> homogeneousList(ResourceKey<? extends Registry<E>> pRegistryKey, Codec<E> pElementCodec) {
      return homogeneousList(pRegistryKey, pElementCodec, false);
   }

   public static <E> Codec<HolderSet<E>> homogeneousList(ResourceKey<? extends Registry<E>> pRegistryKey, Codec<E> pElementCodec, boolean pDisallowInline) {
      return HolderSetCodec.create(pRegistryKey, RegistryFileCodec.create(pRegistryKey, pElementCodec), pDisallowInline);
   }

   public static <E> Codec<HolderSet<E>> homogeneousList(ResourceKey<? extends Registry<E>> pRegistryKey) {
      return homogeneousList(pRegistryKey, false);
   }

   public static <E> Codec<HolderSet<E>> homogeneousList(ResourceKey<? extends Registry<E>> pRegistryKey, boolean pDisallowInline) {
      return HolderSetCodec.create(pRegistryKey, RegistryFixedCodec.create(pRegistryKey), pDisallowInline);
   }

   static record RegistryEntry<T>(ResourceKey<T> key, int id, T value) {
   }
}
