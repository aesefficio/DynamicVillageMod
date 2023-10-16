package net.minecraft.util.valueproviders;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.function.Function;
import net.minecraft.core.Registry;

public abstract class FloatProvider implements SampledFloat {
   private static final Codec<Either<Float, FloatProvider>> CONSTANT_OR_DISPATCH_CODEC = Codec.either(Codec.FLOAT, Registry.FLOAT_PROVIDER_TYPES.byNameCodec().dispatch(FloatProvider::getType, FloatProviderType::codec));
   public static final Codec<FloatProvider> CODEC = CONSTANT_OR_DISPATCH_CODEC.xmap((p_146515_) -> {
      return p_146515_.map(ConstantFloat::of, (p_146518_) -> {
         return p_146518_;
      });
   }, (p_146513_) -> {
      return p_146513_.getType() == FloatProviderType.CONSTANT ? Either.left(((ConstantFloat)p_146513_).getValue()) : Either.right(p_146513_);
   });

   /**
    * Creates a codec for a FloatProvider that only accepts numbers in the given range.
    */
   public static Codec<FloatProvider> codec(float pMinInclusive, float pMaxInclusive) {
      Function<FloatProvider, DataResult<FloatProvider>> function = (p_146511_) -> {
         if (p_146511_.getMinValue() < pMinInclusive) {
            return DataResult.error("Value provider too low: " + pMinInclusive + " [" + p_146511_.getMinValue() + "-" + p_146511_.getMaxValue() + "]");
         } else {
            return p_146511_.getMaxValue() > pMaxInclusive ? DataResult.error("Value provider too high: " + pMaxInclusive + " [" + p_146511_.getMinValue() + "-" + p_146511_.getMaxValue() + "]") : DataResult.success(p_146511_);
         }
      };
      return CODEC.flatXmap(function, function);
   }

   public abstract float getMinValue();

   public abstract float getMaxValue();

   public abstract FloatProviderType<?> getType();
}