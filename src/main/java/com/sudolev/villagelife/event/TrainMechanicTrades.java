package com.sudolev.villagelife.event;


import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.sudolev.villagelife.VillageLife;
import com.sudolev.villagelife.villager.ModVillagers;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = VillageLife.MODID)
public class TrainMechanicTrades {
    @SubscribeEvent
    public static void addCustomTrades(VillagerTradesEvent event) {
        if(event.getType() == ModVillagers.TRAIN_MECHANIC.get()) {
            Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = event.getTrades();
            int villagerLevel1 = 1;
            int villagerLevel2 = 2;
            int villagerLevel3 = 3;
            int villagerLevel4 = 4;
            int villagerLevel5 = 5;
            trades.get(villagerLevel1).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 3),
                    new ItemStack(Items.IRON_INGOT,
                            7),8,8,0.02F));
            trades.get(villagerLevel1).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 3),
                    new ItemStack(AllBlocks.TRAIN_DOOR.get(),
                            2),10,12,0.02F));
            trades.get(villagerLevel1).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 4),
                    new ItemStack(AllBlocks.STEAM_WHISTLE.get(), 5),
                    8,8,0.02F));

            trades.get(villagerLevel2).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 3),
                    new ItemStack(AllBlocks.TRACK.get(),
                            12),8,8,0.02F));
            trades.get(villagerLevel2).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 3),
                    new ItemStack(AllBlocks.TRACK_STATION.get(),
                            1),8,8,0.1F));
            trades.get(villagerLevel2).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 3),
                    new ItemStack(AllBlocks.DISPLAY_BOARD.get(),
                            8),10,12,0.1F));

            trades.get(villagerLevel3).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(AllBlocks.TRACK.get(), 6),
                    new ItemStack(Items.EMERALD, 1),
                    8,8,0.01F));
            trades.get(villagerLevel3).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 2),
                    new ItemStack(AllBlocks.DISPLAY_LINK.get(), 1),
                    10,10,0.01F));

            trades.get(villagerLevel3).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 5),
                    new ItemStack(AllItems.SCHEDULE.get(), 1),
                    4,16,0.01F));

            trades.get(villagerLevel3).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 3),
                    new ItemStack(AllBlocks.PLACARD.get(), 4),
                    3,16,0.01F));

            trades.get(villagerLevel4).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 7),
                    new ItemStack(AllBlocks.TRAIN_CONTROLS.get(), 1),
                    4,12,0.01F));
            trades.get(villagerLevel4).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 5),
                    new ItemStack(AllBlocks.RAILWAY_CASING.get(), 3),
                    8,10,0.01F));
            trades.get(villagerLevel5).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 6),
                    new ItemStack(AllBlocks.PORTABLE_STORAGE_INTERFACE.get(), 2),
                    8,12,0.01F));
            trades.get(villagerLevel5).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 9),
                    new ItemStack(AllBlocks.ITEM_VAULT.get(), 8),
                    2,24,0.01F));
            trades.get(villagerLevel5).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 4),
                    new ItemStack(AllItems.STURDY_SHEET.get(), 3),
                    8,12,0.01F));
        }
    }
}





