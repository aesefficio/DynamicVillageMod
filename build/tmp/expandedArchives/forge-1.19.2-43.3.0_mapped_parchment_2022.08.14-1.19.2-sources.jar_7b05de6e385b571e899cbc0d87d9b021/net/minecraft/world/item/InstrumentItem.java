package net.minecraft.world.item;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

public class InstrumentItem extends Item {
   private static final String TAG_INSTRUMENT = "instrument";
   private TagKey<Instrument> instruments;

   public InstrumentItem(Item.Properties p_220099_, TagKey<Instrument> p_220100_) {
      super(p_220099_);
      this.instruments = p_220100_;
   }

   /**
    * allows items to add custom lines of information to the mouseover description
    */
   public void appendHoverText(ItemStack p_220115_, @Nullable Level p_220116_, List<Component> p_220117_, TooltipFlag p_220118_) {
      super.appendHoverText(p_220115_, p_220116_, p_220117_, p_220118_);
      Optional<ResourceKey<Instrument>> optional = this.getInstrument(p_220115_).flatMap(Holder::unwrapKey);
      if (optional.isPresent()) {
         MutableComponent mutablecomponent = Component.translatable(Util.makeDescriptionId("instrument", optional.get().location()));
         p_220117_.add(mutablecomponent.withStyle(ChatFormatting.GRAY));
      }

   }

   public static ItemStack create(Item p_220108_, Holder<Instrument> p_220109_) {
      ItemStack itemstack = new ItemStack(p_220108_);
      setSoundVariantId(itemstack, p_220109_);
      return itemstack;
   }

   public static void setRandom(ItemStack p_220111_, TagKey<Instrument> p_220112_, RandomSource p_220113_) {
      Optional<Holder<Instrument>> optional = Registry.INSTRUMENT.getTag(p_220112_).flatMap((p_220103_) -> {
         return p_220103_.getRandomElement(p_220113_);
      });
      if (optional.isPresent()) {
         setSoundVariantId(p_220111_, optional.get());
      }

   }

   private static void setSoundVariantId(ItemStack p_220120_, Holder<Instrument> p_220121_) {
      CompoundTag compoundtag = p_220120_.getOrCreateTag();
      compoundtag.putString("instrument", p_220121_.unwrapKey().orElseThrow(() -> {
         return new IllegalStateException("Invalid instrument");
      }).location().toString());
   }

   /**
    * returns a list of items with the same ID, but different meta (eg: dye returns 16 items)
    */
   public void fillItemCategory(CreativeModeTab p_220105_, NonNullList<ItemStack> p_220106_) {
      if (this.allowedIn(p_220105_)) {
         for(Holder<Instrument> holder : Registry.INSTRUMENT.getTagOrEmpty(this.instruments)) {
            p_220106_.add(create(Items.GOAT_HORN, holder));
         }
      }

   }

   /**
    * Called to trigger the item's "innate" right click behavior. To handle when this item is used on a Block, see
    * {@link #onItemUse}.
    */
   public InteractionResultHolder<ItemStack> use(Level p_220123_, Player p_220124_, InteractionHand p_220125_) {
      ItemStack itemstack = p_220124_.getItemInHand(p_220125_);
      Optional<Holder<Instrument>> optional = this.getInstrument(itemstack);
      if (optional.isPresent()) {
         Instrument instrument = optional.get().value();
         p_220124_.startUsingItem(p_220125_);
         play(p_220123_, p_220124_, instrument);
         p_220124_.getCooldowns().addCooldown(this, instrument.useDuration());
         return InteractionResultHolder.consume(itemstack);
      } else {
         return InteractionResultHolder.fail(itemstack);
      }
   }

   /**
    * How long it takes to use or consume an item
    */
   public int getUseDuration(ItemStack p_220131_) {
      Optional<Holder<Instrument>> optional = this.getInstrument(p_220131_);
      return optional.isPresent() ? optional.get().value().useDuration() : 0;
   }

   private Optional<Holder<Instrument>> getInstrument(ItemStack p_220135_) {
      CompoundTag compoundtag = p_220135_.getTag();
      if (compoundtag != null) {
         ResourceLocation resourcelocation = ResourceLocation.tryParse(compoundtag.getString("instrument"));
         if (resourcelocation != null) {
            return Registry.INSTRUMENT.getHolder(ResourceKey.create(Registry.INSTRUMENT_REGISTRY, resourcelocation));
         }
      }

      Iterator<Holder<Instrument>> iterator = Registry.INSTRUMENT.getTagOrEmpty(this.instruments).iterator();
      return iterator.hasNext() ? Optional.of(iterator.next()) : Optional.empty();
   }

   /**
    * returns the action that specifies what animation to play when the items is being used
    */
   public UseAnim getUseAnimation(ItemStack p_220133_) {
      return UseAnim.TOOT_HORN;
   }

   private static void play(Level p_220127_, Player p_220128_, Instrument p_220129_) {
      SoundEvent soundevent = p_220129_.soundEvent();
      float f = p_220129_.range() / 16.0F;
      p_220127_.playSound(p_220128_, p_220128_, soundevent, SoundSource.RECORDS, f, 1.0F);
      p_220127_.gameEvent(GameEvent.INSTRUMENT_PLAY, p_220128_.position(), GameEvent.Context.of(p_220128_));
   }
}