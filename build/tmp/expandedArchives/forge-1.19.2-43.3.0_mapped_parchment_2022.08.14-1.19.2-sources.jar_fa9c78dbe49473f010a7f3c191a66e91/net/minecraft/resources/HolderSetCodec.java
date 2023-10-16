package net.minecraft.resources;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;

public class HolderSetCodec<E> implements Codec<HolderSet<E>> {
   private final ResourceKey<? extends Registry<E>> registryKey;
   private final Codec<Holder<E>> elementCodec;
   private final Codec<List<Holder<E>>> homogenousListCodec;
   private final Codec<Either<TagKey<E>, List<Holder<E>>>> registryAwareCodec;
   private final Codec<net.minecraftforge.registries.holdersets.ICustomHolderSet<E>> forgeDispatchCodec;
   private final Codec<Either<net.minecraftforge.registries.holdersets.ICustomHolderSet<E>, Either<TagKey<E>, List<Holder<E>>>>> combinedCodec;

   private static <E> Codec<List<Holder<E>>> homogenousList(Codec<Holder<E>> pHolderCodec, boolean pDisallowInline) {
      Function<List<Holder<E>>, DataResult<List<Holder<E>>>> function = ExtraCodecs.ensureHomogenous(Holder::kind);
      Codec<List<Holder<E>>> codec = pHolderCodec.listOf().flatXmap(function, function);
      return pDisallowInline ? codec : Codec.either(codec, pHolderCodec).xmap((p_206664_) -> {
         return p_206664_.map((p_206694_) -> {
            return p_206694_;
         }, List::of);
      }, (p_206684_) -> {
         return p_206684_.size() == 1 ? Either.right(p_206684_.get(0)) : Either.left(p_206684_);
      });
   }

   public static <E> Codec<HolderSet<E>> create(ResourceKey<? extends Registry<E>> pRegistryKey, Codec<Holder<E>> pHolderCodec, boolean pDisallowInline) {
      return new HolderSetCodec<>(pRegistryKey, pHolderCodec, pDisallowInline);
   }

   private HolderSetCodec(ResourceKey<? extends Registry<E>> p_206660_, Codec<Holder<E>> p_206661_, boolean p_206662_) {
      this.registryKey = p_206660_;
      this.elementCodec = p_206661_;
      this.homogenousListCodec = homogenousList(p_206661_, p_206662_);
      this.registryAwareCodec = Codec.either(TagKey.hashedCodec(p_206660_), this.homogenousListCodec);
      // FORGE: make registry-specific dispatch codec and make forge-or-vanilla either codec
      this.forgeDispatchCodec = ExtraCodecs.lazyInitializedCodec(() -> net.minecraftforge.registries.ForgeRegistries.HOLDER_SET_TYPES.get().getCodec())
          .dispatch(net.minecraftforge.registries.holdersets.ICustomHolderSet::type, type -> type.makeCodec(p_206660_, p_206661_, p_206662_));
      this.combinedCodec = new ExtraCodecs.EitherCodec<>(this.forgeDispatchCodec, this.registryAwareCodec);
   }

   public <T> DataResult<Pair<HolderSet<E>, T>> decode(DynamicOps<T> p_206696_, T p_206697_) {
      if (p_206696_ instanceof RegistryOps<T> registryops) {
         Optional<? extends Registry<E>> optional = registryops.registry(this.registryKey);
         if (optional.isPresent()) {
            Registry<E> registry = optional.get();
            // FORGE: use the wrapped codec to decode custom/tag/list instead of just tag/list
            return this.combinedCodec.decode(p_206696_, p_206697_).map((p_206682_) -> {
               return p_206682_.mapFirst((p_206679_) -> {
                  return p_206679_.map(java.util.function.Function.identity(), tagOrList -> tagOrList.map(registry::getOrCreateTag, HolderSet::direct));
               });
            });
         }
      }

      return this.decodeWithoutRegistry(p_206696_, p_206697_);
   }

   public <T> DataResult<T> encode(HolderSet<E> p_206674_, DynamicOps<T> p_206675_, T p_206676_) {
      if (p_206675_ instanceof RegistryOps<T> registryops) {
         Optional<? extends Registry<E>> optional = registryops.registry(this.registryKey);
         if (optional.isPresent()) {
            if (!p_206674_.isValidInRegistry(optional.get())) {
               return DataResult.error("HolderSet " + p_206674_ + " is not valid in current registry set");
            }

            // FORGE: use the dispatch codec to encode custom holdersets, otherwise fall back to vanilla tag/list
            if (p_206674_ instanceof net.minecraftforge.registries.holdersets.ICustomHolderSet<E> customHolderSet)
                return this.forgeDispatchCodec.encode(customHolderSet, p_206675_, p_206676_);
            return this.registryAwareCodec.encode(p_206674_.unwrap().mapRight(List::copyOf), p_206675_, p_206676_);
         }
      }

      return this.encodeWithoutRegistry(p_206674_, p_206675_, p_206676_);
   }

   private <T> DataResult<Pair<HolderSet<E>, T>> decodeWithoutRegistry(DynamicOps<T> p_206671_, T p_206672_) {
      return this.elementCodec.listOf().decode(p_206671_, p_206672_).flatMap((p_206666_) -> {
         List<Holder.Direct<E>> list = new ArrayList<>();

         for(Holder<E> holder : p_206666_.getFirst()) {
            if (!(holder instanceof Holder.Direct)) {
               return DataResult.error("Can't decode element " + holder + " without registry");
            }

            Holder.Direct<E> direct = (Holder.Direct)holder;
            list.add(direct);
         }

         return DataResult.success(new Pair<>(HolderSet.direct(list), p_206666_.getSecond()));
      });
   }

   private <T> DataResult<T> encodeWithoutRegistry(HolderSet<E> p_206690_, DynamicOps<T> p_206691_, T p_206692_) {
      return this.homogenousListCodec.encode(p_206690_.stream().toList(), p_206691_, p_206692_);
   }
}
