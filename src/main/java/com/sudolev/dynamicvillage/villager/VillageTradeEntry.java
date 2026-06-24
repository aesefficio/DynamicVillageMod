package com.sudolev.dynamicvillage.villager;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

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

   /** An item + count pair used for trade costs and results. */
   public record TradeItem(ResourceLocation item, int count) {
      public static final Codec<TradeItem> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
               ResourceLocation.CODEC.fieldOf("item").forGetter(TradeItem::item),
               Codec.INT.optionalFieldOf("count", 1).forGetter(TradeItem::count)
            )
            .apply(instance, TradeItem::new)
      );
   }
}
