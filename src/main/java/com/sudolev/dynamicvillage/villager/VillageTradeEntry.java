package com.sudolev.dynamicvillage.villager;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

/**
 * A single data-driven villager trade, loaded from
 * {@code data/<namespace>/dynamicvillage/trades/<name>.json} (see {@link VillageTradeLoader}).
 *
 * <p>Data pack authors only need {@code level}, {@code cost}, and {@code result}; the rest have
 * sensible defaults. Example trade object (inside a file's {@code "trades"} array):
 * <pre>
 * {
 *   "level": 1,
 *   "cost":   { "item": "minecraft:emerald", "count": 2 },
 *   "result": { "item": "create:andesite_alloy", "count": 8 },
 *   "max_uses": 8,
 *   "xp": 8,
 *   "price_multiplier": 0.02
 * }
 * </pre>
 *
 * <p>Richer forms are also accepted (all backward compatible with the simple form above):
 * <ul>
 *   <li>{@code "count": { "min": 2, "max": 5 }} — a random amount rolled per generated offer.</li>
 *   <li>{@code "tag": "c:ingots/zinc"} instead of {@code "item"} — resolves to the first item in
 *       that item tag, so a trade can reference "whatever zinc ingot this pack provides".</li>
 * </ul>
 */
public record VillageTradeEntry(
   int level,
   TradeItem cost,
   Optional<TradeItem> cost2,
   TradeItem result,
   int maxUses,
   int xp,
   float priceMultiplier
) {
   public static final Codec<VillageTradeEntry> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            Codec.INT.fieldOf("level").forGetter(VillageTradeEntry::level),
            TradeItem.CODEC.fieldOf("cost").forGetter(VillageTradeEntry::cost),
            TradeItem.CODEC.optionalFieldOf("cost2").forGetter(VillageTradeEntry::cost2),
            TradeItem.CODEC.fieldOf("result").forGetter(VillageTradeEntry::result),
            Codec.INT.optionalFieldOf("max_uses", 12).forGetter(VillageTradeEntry::maxUses),
            Codec.INT.optionalFieldOf("xp", 2).forGetter(VillageTradeEntry::xp),
            Codec.FLOAT.optionalFieldOf("price_multiplier", 0.05F).forGetter(VillageTradeEntry::priceMultiplier)
         )
         .apply(instance, VillageTradeEntry::new)
   );

   /**
    * An item reference + count used for trade costs and results. Exactly one of {@code item} or
    * {@code tag} must be present; validation happens where the trade is applied (a bad spec is
    * logged and skipped, never fatal). {@code count} accepts either a bare integer or a
    * {@code {min,max}} range.
    *
    * @param item exact item id (mutually exclusive with {@code tag})
    * @param tag  item tag id; resolves to the first registered item in the tag
    * @param count fixed count, or a min/max range rolled per offer (defaults to 1)
    */
   public record TradeItem(Optional<ResourceLocation> item, Optional<ResourceLocation> tag, CountRange count, Optional<CompoundTag> components) {
      public static final Codec<TradeItem> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
               ResourceLocation.CODEC.optionalFieldOf("item").forGetter(TradeItem::item),
               ResourceLocation.CODEC.optionalFieldOf("tag").forGetter(TradeItem::tag),
               CountRange.CODEC.optionalFieldOf("count", CountRange.of(1)).forGetter(TradeItem::count),
               CompoundTag.CODEC.optionalFieldOf("components").forGetter(TradeItem::components)
            )
            .apply(instance, TradeItem::new)
      );

      /** True when exactly one of {@code item}/{@code tag} is set (the only valid form). */
      public boolean isValid() {
         return item.isPresent() ^ tag.isPresent();
      }
   }

   /**
    * A count that is either fixed ({@code min == max}) or a random range. Serialises as a bare int
    * when fixed (e.g. {@code "count": 3}) and as {@code {"min":m,"max":n}} when it is a range, so the
    * simple integer form used by existing packs keeps working unchanged.
    */
   public record CountRange(int min, int max) {
      private static final Codec<CountRange> OBJECT_CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
               Codec.INT.fieldOf("min").forGetter(CountRange::min),
               Codec.INT.fieldOf("max").forGetter(CountRange::max)
            )
            .apply(instance, CountRange::new)
      );

      public static final Codec<CountRange> CODEC = Codec.either(Codec.INT, OBJECT_CODEC)
         .xmap(
            either -> either.map(CountRange::of, range -> range),
            range -> range.min() == range.max() ? Either.left(range.min()) : Either.right(range)
         );

      public static CountRange of(int fixed) {
         return new CountRange(fixed, fixed);
      }

      /** Rolls a concrete count in {@code [min, max]}; clamps to at least 1 so an offer is always valid. */
      public int sample(RandomSource random) {
         int lo = Math.max(1, Math.min(min, max));
         int hi = Math.max(1, Math.max(min, max));
         return lo >= hi ? lo : lo + random.nextInt(hi - lo + 1);
      }
   }
}
