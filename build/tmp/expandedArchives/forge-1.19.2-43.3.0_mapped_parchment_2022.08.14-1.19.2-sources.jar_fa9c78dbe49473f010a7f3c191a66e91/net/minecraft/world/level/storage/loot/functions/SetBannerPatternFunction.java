package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSyntaxException;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * LootItemFunction that sets the banner patterns for a banner item. Optionally appends to any existing patterns.
 */
public class SetBannerPatternFunction extends LootItemConditionalFunction {
   final List<Pair<Holder<BannerPattern>, DyeColor>> patterns;
   final boolean append;

   SetBannerPatternFunction(LootItemCondition[] pConditions, List<Pair<Holder<BannerPattern>, DyeColor>> pPatterns, boolean pAppend) {
      super(pConditions);
      this.patterns = pPatterns;
      this.append = pAppend;
   }

   /**
    * Called to perform the actual action of this function, after conditions have been checked.
    */
   protected ItemStack run(ItemStack pStack, LootContext pContext) {
      CompoundTag compoundtag = BlockItem.getBlockEntityData(pStack);
      if (compoundtag == null) {
         compoundtag = new CompoundTag();
      }

      BannerPattern.Builder bannerpattern$builder = new BannerPattern.Builder();
      this.patterns.forEach(bannerpattern$builder::addPattern);
      ListTag listtag = bannerpattern$builder.toListTag();
      ListTag listtag1;
      if (this.append) {
         listtag1 = compoundtag.getList("Patterns", 10).copy();
         listtag1.addAll(listtag);
      } else {
         listtag1 = listtag;
      }

      compoundtag.put("Patterns", listtag1);
      BlockItem.setBlockEntityData(pStack, BlockEntityType.BANNER, compoundtag);
      return pStack;
   }

   public LootItemFunctionType getType() {
      return LootItemFunctions.SET_BANNER_PATTERN;
   }

   public static SetBannerPatternFunction.Builder setBannerPattern(boolean pAppend) {
      return new SetBannerPatternFunction.Builder(pAppend);
   }

   public static class Builder extends LootItemConditionalFunction.Builder<SetBannerPatternFunction.Builder> {
      private final ImmutableList.Builder<Pair<Holder<BannerPattern>, DyeColor>> patterns = ImmutableList.builder();
      private final boolean append;

      Builder(boolean pAppend) {
         this.append = pAppend;
      }

      protected SetBannerPatternFunction.Builder getThis() {
         return this;
      }

      public LootItemFunction build() {
         return new SetBannerPatternFunction(this.getConditions(), this.patterns.build(), this.append);
      }

      public SetBannerPatternFunction.Builder addPattern(ResourceKey<BannerPattern> pPattern, DyeColor pColor) {
         return this.addPattern(Registry.BANNER_PATTERN.getHolderOrThrow(pPattern), pColor);
      }

      public SetBannerPatternFunction.Builder addPattern(Holder<BannerPattern> pPattern, DyeColor pColor) {
         this.patterns.add(Pair.of(pPattern, pColor));
         return this;
      }
   }

   public static class Serializer extends LootItemConditionalFunction.Serializer<SetBannerPatternFunction> {
      /**
       * Serialize the value by putting its data into the JsonObject.
       */
      public void serialize(JsonObject p_165307_, SetBannerPatternFunction p_165308_, JsonSerializationContext p_165309_) {
         super.serialize(p_165307_, p_165308_, p_165309_);
         JsonArray jsonarray = new JsonArray();
         p_165308_.patterns.forEach((p_231003_) -> {
            JsonObject jsonobject = new JsonObject();
            jsonobject.addProperty("pattern", p_231003_.getFirst().unwrapKey().orElseThrow(() -> {
               return new JsonSyntaxException("Unknown pattern: " + p_231003_.getFirst());
            }).location().toString());
            jsonobject.addProperty("color", p_231003_.getSecond().getName());
            jsonarray.add(jsonobject);
         });
         p_165307_.add("patterns", jsonarray);
         p_165307_.addProperty("append", p_165308_.append);
      }

      public SetBannerPatternFunction deserialize(JsonObject p_165299_, JsonDeserializationContext p_165300_, LootItemCondition[] p_165301_) {
         ImmutableList.Builder<Pair<Holder<BannerPattern>, DyeColor>> builder = ImmutableList.builder();
         JsonArray jsonarray = GsonHelper.getAsJsonArray(p_165299_, "patterns");

         for(int i = 0; i < jsonarray.size(); ++i) {
            JsonObject jsonobject = GsonHelper.convertToJsonObject(jsonarray.get(i), "pattern[" + i + "]");
            String s = GsonHelper.getAsString(jsonobject, "pattern");
            Optional<Holder<BannerPattern>> optional = Registry.BANNER_PATTERN.getHolder(ResourceKey.create(Registry.BANNER_PATTERN_REGISTRY, new ResourceLocation(s)));
            if (optional.isEmpty()) {
               throw new JsonSyntaxException("Unknown pattern: " + s);
            }

            String s1 = GsonHelper.getAsString(jsonobject, "color");
            DyeColor dyecolor = DyeColor.byName(s1, (DyeColor)null);
            if (dyecolor == null) {
               throw new JsonSyntaxException("Unknown color: " + s1);
            }

            builder.add(Pair.of(optional.get(), dyecolor));
         }

         boolean flag = GsonHelper.getAsBoolean(p_165299_, "append");
         return new SetBannerPatternFunction(p_165301_, builder.build(), flag);
      }
   }
}