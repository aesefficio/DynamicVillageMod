package net.minecraft.util;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Keyable;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;

public interface StringRepresentable {
   int PRE_BUILT_MAP_THRESHOLD = 16;

   String getSerializedName();

   static <E extends Enum<E> & StringRepresentable> StringRepresentable.EnumCodec<E> fromEnum(Supplier<E[]> pElementsSupplier) {
      E[] ae = pElementsSupplier.get();
      if (ae.length > 16) {
         Map<String, E> map = Arrays.stream(ae).collect(Collectors.toMap((p_184753_) -> {
            return p_184753_.getSerializedName();
         }, (p_216435_) -> {
            return p_216435_;
         }));
         return new StringRepresentable.EnumCodec<>(ae, (p_216438_) -> {
            return (E)(p_216438_ == null ? null : map.get(p_216438_));
         });
      } else {
         return new StringRepresentable.EnumCodec<>(ae, (p_216443_) -> {
            for(E e : ae) {
               if (e.getSerializedName().equals(p_216443_)) {
                  return e;
               }
            }

            return (E)null;
         });
      }
   }

   static Keyable keys(final StringRepresentable[] pSerializables) {
      return new Keyable() {
         public <T> Stream<T> keys(DynamicOps<T> p_184758_) {
            return Arrays.stream(pSerializables).map(StringRepresentable::getSerializedName).map(p_184758_::createString);
         }
      };
   }

   /** @deprecated */
   @Deprecated
   public static class EnumCodec<E extends Enum<E> & StringRepresentable> implements Codec<E> {
      private Codec<E> codec;
      private Function<String, E> resolver;

      public EnumCodec(E[] pValues, Function<String, E> pResolver) {
         this.codec = ExtraCodecs.orCompressed(ExtraCodecs.stringResolverCodec((p_216461_) -> {
            return p_216461_.getSerializedName();
         }, pResolver), ExtraCodecs.idResolverCodec((p_216454_) -> {
            return p_216454_.ordinal();
         }, (p_216459_) -> {
            return (E)(p_216459_ >= 0 && p_216459_ < pValues.length ? pValues[p_216459_] : null);
         }, -1));
         this.resolver = pResolver;
      }

      public <T> DataResult<Pair<E, T>> decode(DynamicOps<T> pOps, T pInput) {
         return this.codec.decode(pOps, pInput);
      }

      public <T> DataResult<T> encode(E pInput, DynamicOps<T> pOps, T pPrefix) {
         return this.codec.encode(pInput, pOps, pPrefix);
      }

      @Nullable
      public E byName(@Nullable String pName) {
         return this.resolver.apply(pName);
      }
   }
}