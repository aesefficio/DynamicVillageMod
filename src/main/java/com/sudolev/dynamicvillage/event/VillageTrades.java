package com.sudolev.dynamicvillage.event;

import com.mojang.logging.LogUtils;
import com.sudolev.dynamicvillage.VillageLife;
import com.sudolev.dynamicvillage.villager.VillageTradeEntry;
import com.sudolev.dynamicvillage.villager.VillageTradeEntry.CountRange;
import com.sudolev.dynamicvillage.villager.VillageTradeEntry.TradeItem;
import com.sudolev.dynamicvillage.villager.VillageTradeLoader;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.npc.VillagerTrades.ItemListing;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Applies data-driven villager trades (see {@link VillageTradeLoader}) to whichever profession is
 * being set up. All trades come from data packs, so packs can add, change, or remove them.
 *
 * <p>Each cost/result may reference an item by exact id or by item tag (resolved to the first
 * registered item in the tag), and each count may be a fixed number or a {@code {min,max}} range
 * rolled per generated offer.
 */
@Mod.EventBusSubscriber(
   modid = VillageLife.MODID
)
public class VillageTrades {
   private static final Logger LOGGER = LogUtils.getLogger();

   /** Register the data pack reload listener that loads trade definitions. */
   @SubscribeEvent
   public static void onAddReloadListeners(AddReloadListenerEvent event) {
      event.addListener(new VillageTradeLoader());
   }

   @SubscribeEvent
   public static void onVillagerTrades(VillagerTradesEvent event) {
      ResourceLocation professionId = BuiltInRegistries.VILLAGER_PROFESSION.getKey(event.getType());
      if (professionId == null) {
         return;
      }

      List<VillageTradeEntry> entries = VillageTradeLoader.forProfession(professionId);
      if (entries.isEmpty()) {
         return;
      }

      Int2ObjectMap<List<ItemListing>> trades = event.getTrades();
      int added = 0;

      for (VillageTradeEntry entry : entries) {
         int level = Math.max(1, Math.min(5, entry.level()));
         if (level != entry.level()) {
            LOGGER.warn("[DynamicVillage] Trade for {}: level {} is outside 1-5; using {}.", professionId, entry.level(), level);
         }
         List<ItemListing> levelTrades = trades.get(level);
         if (levelTrades == null) {
            LOGGER.warn("[DynamicVillage] Trade for {}: no trade list exists for level {}; skipping.", professionId, level);
            continue;
         }
         warnIfComponentsIgnored(entry, professionId);

         Item costItem = resolve(entry.cost(), professionId);
         Item resultItem = resolve(entry.result(), professionId);
         if (costItem == null || resultItem == null) {
            continue;
         }

         Item cost2Item = null;
         if (entry.cost2().isPresent()) {
            cost2Item = resolve(entry.cost2().get(), professionId);
            if (cost2Item == null) {
               continue;
            }
         }

         // Capture resolved items + count providers; roll fresh counts and build fresh stacks per
         // offer so nothing mutable is shared across villagers.
         final Item fCost = costItem;
         final CountRange costCount = entry.cost().count();
         final Item fResult = resultItem;
         final CountRange resultCount = entry.result().count();
         final java.util.Optional<net.minecraft.nbt.CompoundTag> resultTag = entry.result().components();
         final Item fCost2 = cost2Item;
         final CountRange cost2Count = entry.cost2().map(TradeItem::count).orElse(CountRange.of(1));
         final int maxUses = entry.maxUses();
         final int xp = entry.xp();
         final float priceMultiplier = entry.priceMultiplier();

         levelTrades.add((trader, rand) -> {
            ItemStack baseCost = new ItemStack(fCost, costCount.sample(rand));
            ItemStack secondCost = fCost2 == null ? ItemStack.EMPTY : new ItemStack(fCost2, cost2Count.sample(rand));
            ItemStack result = new ItemStack(fResult, resultCount.sample(rand));
            resultTag.ifPresent(t -> result.setTag(t.copy()));
            return new MerchantOffer(baseCost, secondCost, result, maxUses, xp, priceMultiplier);
         });
         added++;
      }

      LOGGER.info("[DynamicVillage] Added {} trade(s) to profession {}", added, professionId);
   }

   /**
    * {@code components} is only applied to a trade's {@code result}. Costs are matched by item and
    * count alone, so components there would silently do nothing — say so rather than leaving a pack
    * author wondering why their component-matched cost accepts any plain item.
    */
   private static void warnIfComponentsIgnored(VillageTradeEntry entry, ResourceLocation professionId) {
      if (entry.cost().components().isPresent() || entry.cost2().map(c -> c.components().isPresent()).orElse(false)) {
         LOGGER.warn(
            "[DynamicVillage] Trade for {}: 'components' on a cost is ignored (costs match by item and count only); "
               + "it only applies to 'result'.",
            professionId
         );
      }
   }

   /**
    * Resolves a trade item spec to a concrete {@link Item}, or {@code null} (with a warning) if it is
    * malformed or references something not present — the trade is then skipped, never fatal.
    */
   private static Item resolve(TradeItem spec, ResourceLocation professionId) {
      if (!spec.isValid()) {
         LOGGER.warn("[DynamicVillage] Skipping trade for {}: each cost/result must set exactly one of 'item' or 'tag'.", professionId);
         return null;
      }

      if (spec.item().isPresent()) {
         ResourceLocation id = spec.item().get();
         Optional<Item> item = BuiltInRegistries.ITEM.getOptional(id);
         if (item.isEmpty()) {
            LOGGER.warn("[DynamicVillage] Skipping trade for {}: item {} is not registered (mod not installed?)", professionId, id);
            return null;
         }
         return item.get();
      }

      ResourceLocation tagId = spec.tag().get();
      TagKey<Item> tag = TagKey.create(Registries.ITEM, tagId);
      Optional<Item> first = BuiltInRegistries.ITEM.getTag(tag)
         .flatMap(named -> named.stream().findFirst())
         .map(Holder::value);
      if (first.isEmpty()) {
         LOGGER.warn("[DynamicVillage] Skipping trade for {}: item tag {} is empty or absent (mod not installed?)", professionId, tagId);
         return null;
      }
      return first.get();
   }
}
