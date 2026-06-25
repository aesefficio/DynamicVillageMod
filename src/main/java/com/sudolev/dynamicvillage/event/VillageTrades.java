package com.sudolev.dynamicvillage.event;

import com.mojang.logging.LogUtils;
import com.sudolev.dynamicvillage.VillageLife;
import com.sudolev.dynamicvillage.villager.VillageTradeEntry;
import com.sudolev.dynamicvillage.villager.VillageTradeLoader;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
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
 * being set up. Replaces the old per-profession hardcoded trade classes; all trades now come from
 * data packs, so packs can add, change, or remove them.
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
         List<ItemListing> levelTrades = trades.get(level);
         if (levelTrades == null) {
            continue;
         }

         Item costItem = resolve(entry.cost().item(), professionId);
         Item resultItem = resolve(entry.result().item(), professionId);
         if (costItem == null || resultItem == null) {
            continue;
         }

         ItemStack baseCost = new ItemStack(costItem, entry.cost().count());
         ItemStack result = new ItemStack(resultItem, entry.result().count());

         ItemStack secondCost = ItemStack.EMPTY;
         if (entry.cost2().isPresent()) {
            Item cost2Item = resolve(entry.cost2().get().item(), professionId);
            if (cost2Item == null) {
               continue;
            }
            secondCost = new ItemStack(cost2Item, entry.cost2().get().count());
         }

         ItemStack finalSecondCost = secondCost;
         levelTrades.add(
            (trader, rand) -> new MerchantOffer(baseCost, finalSecondCost, result, entry.maxUses(), entry.xp(), entry.priceMultiplier())
         );
         added++;
      }

      LOGGER.info("[DynamicVillage] Added {} trade(s) to profession {}", added, professionId);
   }

   private static Item resolve(ResourceLocation itemId, ResourceLocation professionId) {
      Optional<Item> item = BuiltInRegistries.ITEM.getOptional(itemId);
      if (item.isEmpty()) {
         LOGGER.warn("[DynamicVillage] Skipping trade for {}: item {} is not registered (mod not installed?)", professionId, itemId);
         return null;
      }
      return item.get();
   }
}
