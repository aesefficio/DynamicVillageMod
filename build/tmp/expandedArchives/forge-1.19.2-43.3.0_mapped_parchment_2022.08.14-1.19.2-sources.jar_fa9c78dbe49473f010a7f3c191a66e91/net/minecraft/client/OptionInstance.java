package net.minecraft.client;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;
import net.minecraft.client.gui.components.AbstractOptionSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.TooltipAccessor;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.OptionEnum;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public final class OptionInstance<T> {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final OptionInstance.Enum<Boolean> BOOLEAN_VALUES = new OptionInstance.Enum<>(ImmutableList.of(Boolean.TRUE, Boolean.FALSE), Codec.BOOL);
   private static final int TOOLTIP_WIDTH = 200;
   private final OptionInstance.TooltipSupplierFactory<T> tooltip;
   final Function<T, Component> toString;
   private final OptionInstance.ValueSet<T> values;
   private final Codec<T> codec;
   private final T initialValue;
   private final Consumer<T> onValueUpdate;
   final Component caption;
   T value;

   public static OptionInstance<Boolean> createBoolean(String pKey, boolean pInitialValue, Consumer<Boolean> pOnValueUpdate) {
      return createBoolean(pKey, noTooltip(), pInitialValue, pOnValueUpdate);
   }

   public static OptionInstance<Boolean> createBoolean(String pKey, boolean pInitialValue) {
      return createBoolean(pKey, noTooltip(), pInitialValue, (p_231548_) -> {
      });
   }

   public static OptionInstance<Boolean> createBoolean(String pKey, OptionInstance.TooltipSupplierFactory<Boolean> pTooltip, boolean pInitialValue) {
      return createBoolean(pKey, pTooltip, pInitialValue, (p_231513_) -> {
      });
   }

   public static OptionInstance<Boolean> createBoolean(String pKey, OptionInstance.TooltipSupplierFactory<Boolean> pTooltip, boolean pInitialValue, Consumer<Boolean> pOnValueUpdate) {
      return new OptionInstance<>(pKey, pTooltip, (p_231544_, p_231545_) -> {
         return p_231545_ ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF;
      }, BOOLEAN_VALUES, pInitialValue, pOnValueUpdate);
   }

   public OptionInstance(String pKey, OptionInstance.TooltipSupplierFactory<T> pTooltip, OptionInstance.CaptionBasedToString<T> p_231494_, OptionInstance.ValueSet<T> pValues, T pInitialValue, Consumer<T> pOnValueUpdate) {
      this(pKey, pTooltip, p_231494_, pValues, pValues.codec(), pInitialValue, pOnValueUpdate);
   }

   public OptionInstance(String pKey, OptionInstance.TooltipSupplierFactory<T> pTooltip, OptionInstance.CaptionBasedToString<T> p_231486_, OptionInstance.ValueSet<T> pValues, Codec<T> pCodec, T pInitalValue, Consumer<T> pOnValueUpdate) {
      this.caption = Component.translatable(pKey);
      this.tooltip = pTooltip;
      this.toString = (p_231506_) -> {
         return p_231486_.toString(this.caption, p_231506_);
      };
      this.values = pValues;
      this.codec = pCodec;
      this.initialValue = pInitalValue;
      this.onValueUpdate = pOnValueUpdate;
      this.value = this.initialValue;
   }

   public static <T> OptionInstance.TooltipSupplierFactory<T> noTooltip() {
      return (p_231500_) -> {
         return (p_231553_) -> {
            return ImmutableList.of();
         };
      };
   }

   public static <T> OptionInstance.TooltipSupplierFactory<T> cachedConstantTooltip(Component pTooltip) {
      return (p_231542_) -> {
         List<FormattedCharSequence> list = splitTooltip(p_231542_, pTooltip);
         return (p_231534_) -> {
            return list;
         };
      };
   }

   public static <T extends OptionEnum> OptionInstance.CaptionBasedToString<T> forOptionEnum() {
      return (p_231538_, p_231539_) -> {
         return p_231539_.getCaption();
      };
   }

   protected static List<FormattedCharSequence> splitTooltip(Minecraft pMinecraft, Component pTooltip) {
      return pMinecraft.font.split(pTooltip, 200);
   }

   public AbstractWidget createButton(Options pOptions, int p_231509_, int p_231510_, int p_231511_) {
      OptionInstance.TooltipSupplier<T> tooltipsupplier = this.tooltip.apply(Minecraft.getInstance());
      return this.values.createButton(tooltipsupplier, pOptions, p_231509_, p_231510_, p_231511_).apply(this);
   }

   public T get() {
      return this.value;
   }

   public Codec<T> codec() {
      return this.codec;
   }

   public String toString() {
      return this.caption.getString();
   }

   public void set(T p_231515_) {
      T t = this.values.validateValue(p_231515_).orElseGet(() -> {
         LOGGER.error("Illegal option value " + p_231515_ + " for " + this.caption);
         return this.initialValue;
      });
      if (!Minecraft.getInstance().isRunning()) {
         this.value = t;
      } else {
         if (!Objects.equals(this.value, t)) {
            this.value = t;
            this.onValueUpdate.accept(this.value);
         }

      }
   }

   public OptionInstance.ValueSet<T> values() {
      return this.values;
   }

   @OnlyIn(Dist.CLIENT)
   public static record AltEnum<T>(List<T> values, List<T> altValues, BooleanSupplier altCondition, OptionInstance.CycleableValueSet.ValueSetter<T> valueSetter, Codec<T> codec) implements OptionInstance.CycleableValueSet<T> {
      public CycleButton.ValueListSupplier<T> valueListSupplier() {
         return CycleButton.ValueListSupplier.create(this.altCondition, this.values, this.altValues);
      }

      public Optional<T> validateValue(T p_231570_) {
         return (this.altCondition.getAsBoolean() ? this.altValues : this.values).contains(p_231570_) ? Optional.of(p_231570_) : Optional.empty();
      }

      public OptionInstance.CycleableValueSet.ValueSetter<T> valueSetter() {
         return this.valueSetter;
      }

      public Codec<T> codec() {
         return this.codec;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public interface CaptionBasedToString<T> {
      Component toString(Component pCaption, T p_231582_);
   }

   @OnlyIn(Dist.CLIENT)
   public static record ClampingLazyMaxIntRange(int minInclusive, IntSupplier maxSupplier) implements OptionInstance.IntRangeBase, OptionInstance.SliderableOrCyclableValueSet<Integer> {
      public Optional<Integer> validateValue(Integer p_231590_) {
         return Optional.of(Mth.clamp(p_231590_, this.minInclusive(), this.maxInclusive()));
      }

      public int maxInclusive() {
         return this.maxSupplier.getAsInt();
      }

      public Codec<Integer> codec() {
         Function<Integer, DataResult<Integer>> function = (p_231596_) -> {
            int i = this.maxSupplier.getAsInt() + 1;
            return p_231596_.compareTo(this.minInclusive) >= 0 && p_231596_.compareTo(i) <= 0 ? DataResult.success(p_231596_) : DataResult.error("Value " + p_231596_ + " outside of range [" + this.minInclusive + ":" + i + "]", p_231596_);
         };
         return Codec.INT.flatXmap(function, function);
      }

      public boolean createCycleButton() {
         return true;
      }

      public CycleButton.ValueListSupplier<Integer> valueListSupplier() {
         return CycleButton.ValueListSupplier.create(IntStream.range(this.minInclusive, this.maxInclusive() + 1).boxed().toList());
      }

      public int minInclusive() {
         return this.minInclusive;
      }
   }

   @OnlyIn(Dist.CLIENT)
   interface CycleableValueSet<T> extends OptionInstance.ValueSet<T> {
      CycleButton.ValueListSupplier<T> valueListSupplier();

      default OptionInstance.CycleableValueSet.ValueSetter<T> valueSetter() {
         return OptionInstance::set;
      }

      default Function<OptionInstance<T>, AbstractWidget> createButton(OptionInstance.TooltipSupplier<T> p_231612_, Options p_231613_, int p_231614_, int p_231615_, int p_231616_) {
         return (p_231610_) -> {
            return CycleButton.builder(p_231610_.toString).withValues(this.valueListSupplier()).withTooltip(p_231612_).withInitialValue(p_231610_.value).create(p_231614_, p_231615_, p_231616_, 20, p_231610_.caption, (p_231620_, p_231621_) -> {
               this.valueSetter().set(p_231610_, p_231621_);
               p_231613_.save();
            });
         };
      }

      @OnlyIn(Dist.CLIENT)
      public interface ValueSetter<T> {
         void set(OptionInstance<T> p_231623_, T p_231624_);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static record Enum<T>(List<T> values, Codec<T> codec) implements OptionInstance.CycleableValueSet<T> {
      public Optional<T> validateValue(T p_231632_) {
         return this.values.contains(p_231632_) ? Optional.of(p_231632_) : Optional.empty();
      }

      public CycleButton.ValueListSupplier<T> valueListSupplier() {
         return CycleButton.ValueListSupplier.create(this.values);
      }

      public Codec<T> codec() {
         return this.codec;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static record IntRange(int minInclusive, int maxInclusive) implements OptionInstance.IntRangeBase {
      public Optional<Integer> validateValue(Integer p_231645_) {
         return p_231645_.compareTo(this.minInclusive()) >= 0 && p_231645_.compareTo(this.maxInclusive()) <= 0 ? Optional.of(p_231645_) : Optional.empty();
      }

      public Codec<Integer> codec() {
         return Codec.intRange(this.minInclusive, this.maxInclusive + 1);
      }

      public int minInclusive() {
         return this.minInclusive;
      }

      public int maxInclusive() {
         return this.maxInclusive;
      }
   }

   @OnlyIn(Dist.CLIENT)
   interface IntRangeBase extends OptionInstance.SliderableValueSet<Integer> {
      int minInclusive();

      int maxInclusive();

      default double toSliderValue(Integer p_231663_) {
         return (double)Mth.map((float)p_231663_.intValue(), (float)this.minInclusive(), (float)this.maxInclusive(), 0.0F, 1.0F);
      }

      default Integer fromSliderValue(double p_231656_) {
         return Mth.floor(Mth.map(p_231656_, 0.0D, 1.0D, (double)this.minInclusive(), (double)this.maxInclusive()));
      }

      default <R> OptionInstance.SliderableValueSet<R> xmap(final IntFunction<? extends R> p_231658_, final ToIntFunction<? super R> p_231659_) {
         return new OptionInstance.SliderableValueSet<R>() {
            public Optional<R> validateValue(R p_231674_) {
               return IntRangeBase.this.validateValue(Integer.valueOf(p_231659_.applyAsInt(p_231674_))).map(p_231658_::apply);
            }

            public double toSliderValue(R p_231678_) {
               return IntRangeBase.this.toSliderValue(p_231659_.applyAsInt(p_231678_));
            }

            public R fromSliderValue(double p_231676_) {
               return p_231658_.apply(IntRangeBase.this.fromSliderValue(p_231676_));
            }

            public Codec<R> codec() {
               return IntRangeBase.this.codec().xmap(p_231658_::apply, p_231659_::applyAsInt);
            }
         };
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static record LazyEnum<T>(Supplier<List<T>> values, Function<T, Optional<T>> validateValue, Codec<T> codec) implements OptionInstance.CycleableValueSet<T> {
      public Optional<T> validateValue(T p_231689_) {
         return this.validateValue.apply(p_231689_);
      }

      public CycleButton.ValueListSupplier<T> valueListSupplier() {
         return CycleButton.ValueListSupplier.create(this.values.get());
      }

      public Codec<T> codec() {
         return this.codec;
      }
   }

   @OnlyIn(Dist.CLIENT)
   static final class OptionInstanceSliderButton<N> extends AbstractOptionSliderButton implements TooltipAccessor {
      private final OptionInstance<N> instance;
      private final OptionInstance.SliderableValueSet<N> values;
      private final OptionInstance.TooltipSupplier<N> tooltip;

      OptionInstanceSliderButton(Options pOptions, int pX, int pY, int pWidth, int pHeight, OptionInstance<N> pInstance, OptionInstance.SliderableValueSet<N> pValues, OptionInstance.TooltipSupplier<N> pTooltip) {
         super(pOptions, pX, pY, pWidth, pHeight, pValues.toSliderValue(pInstance.get()));
         this.instance = pInstance;
         this.values = pValues;
         this.tooltip = pTooltip;
         this.updateMessage();
      }

      protected void updateMessage() {
         this.setMessage(this.instance.toString.apply(this.instance.get()));
      }

      protected void applyValue() {
         this.instance.set(this.values.fromSliderValue(this.value));
         this.options.save();
      }

      public List<FormattedCharSequence> getTooltip() {
         return this.tooltip.apply(this.values.fromSliderValue(this.value));
      }
   }

   @OnlyIn(Dist.CLIENT)
   interface SliderableOrCyclableValueSet<T> extends OptionInstance.CycleableValueSet<T>, OptionInstance.SliderableValueSet<T> {
      boolean createCycleButton();

      default Function<OptionInstance<T>, AbstractWidget> createButton(OptionInstance.TooltipSupplier<T> p_231713_, Options p_231714_, int p_231715_, int p_231716_, int p_231717_) {
         return this.createCycleButton() ? OptionInstance.CycleableValueSet.super.createButton(p_231713_, p_231714_, p_231715_, p_231716_, p_231717_) : OptionInstance.SliderableValueSet.super.createButton(p_231713_, p_231714_, p_231715_, p_231716_, p_231717_);
      }
   }

   @OnlyIn(Dist.CLIENT)
   interface SliderableValueSet<T> extends OptionInstance.ValueSet<T> {
      double toSliderValue(T pValue);

      T fromSliderValue(double pValue);

      default Function<OptionInstance<T>, AbstractWidget> createButton(OptionInstance.TooltipSupplier<T> pTooltip, Options pOptions, int p_231721_, int p_231722_, int p_231723_) {
         return (p_231730_) -> {
            return new OptionInstance.OptionInstanceSliderButton<>(pOptions, p_231721_, p_231722_, p_231723_, 20, p_231730_, this, pTooltip);
         };
      }
   }

   @FunctionalInterface
   @OnlyIn(Dist.CLIENT)
   public interface TooltipSupplier<T> extends Function<T, List<FormattedCharSequence>> {
   }

   @OnlyIn(Dist.CLIENT)
   public interface TooltipSupplierFactory<T> extends Function<Minecraft, OptionInstance.TooltipSupplier<T>> {
   }

   @OnlyIn(Dist.CLIENT)
   public static enum UnitDouble implements OptionInstance.SliderableValueSet<Double> {
      INSTANCE;

      public Optional<Double> validateValue(Double p_231747_) {
         return p_231747_ >= 0.0D && p_231747_ <= 1.0D ? Optional.of(p_231747_) : Optional.empty();
      }

      public double toSliderValue(Double p_231756_) {
         return p_231756_;
      }

      public Double fromSliderValue(double p_231741_) {
         return p_231741_;
      }

      public <R> OptionInstance.SliderableValueSet<R> xmap(final DoubleFunction<? extends R> p_231751_, final ToDoubleFunction<? super R> p_231752_) {
         return new OptionInstance.SliderableValueSet<R>() {
            public Optional<R> validateValue(R p_231773_) {
               return UnitDouble.this.validateValue(p_231752_.applyAsDouble(p_231773_)).map(p_231751_::apply);
            }

            public double toSliderValue(R p_231777_) {
               return UnitDouble.this.toSliderValue(p_231752_.applyAsDouble(p_231777_));
            }

            public R fromSliderValue(double p_231775_) {
               return p_231751_.apply(UnitDouble.this.fromSliderValue(p_231775_));
            }

            public Codec<R> codec() {
               return UnitDouble.this.codec().xmap(p_231751_::apply, p_231752_::applyAsDouble);
            }
         };
      }

      public Codec<Double> codec() {
         return Codec.either(Codec.doubleRange(0.0D, 1.0D), Codec.BOOL).xmap((p_231743_) -> {
            return p_231743_.map((p_231760_) -> {
               return p_231760_;
            }, (p_231745_) -> {
               return p_231745_ ? 1.0D : 0.0D;
            });
         }, Either::left);
      }
   }

   @OnlyIn(Dist.CLIENT)
   interface ValueSet<T> {
      Function<OptionInstance<T>, AbstractWidget> createButton(OptionInstance.TooltipSupplier<T> pTooltip, Options pOptions, int p_231781_, int p_231782_, int p_231783_);

      Optional<T> validateValue(T pValue);

      Codec<T> codec();
   }
}