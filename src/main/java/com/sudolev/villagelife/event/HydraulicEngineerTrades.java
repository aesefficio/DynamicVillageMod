package com.sudolev.villagelife.event;


import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.sudolev.villagelife.VillageLife;
import com.sudolev.villagelife.villager.ModVillagers;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = VillageLife.MODID)
public class HydraulicEngineerTrades {
    @SubscribeEvent
    public static void addCustomTrades(VillagerTradesEvent event) {
        if(event.getType() == ModVillagers.HYDRAULIC_ENGINEER.get()) {
            Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = event.getTrades();
            int villagerLevel1 = 1;
            int villagerLevel2 = 2;
            int villagerLevel3 = 3;
            int villagerLevel4 = 4;
            int villagerLevel5 = 5;
            trades.get(villagerLevel1).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 2),
                    new ItemStack(AllItems.COPPER_SHEET.get(),
                            8),8,8,0.02F));
            trades.get(villagerLevel1).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.DRIED_KELP, 20),
                    new ItemStack(Items.EMERALD, 1),
                    8,8,0.01F));
            trades.get(villagerLevel1).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 2),
                    new ItemStack(AllBlocks.FLUID_PIPE.get(),
                            6),10,8,0.02F));
            trades.get(villagerLevel1).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.COPPER_INGOT, 6),
                    new ItemStack(Items.EMERALD, 1),
                    10,8,0.02F));
            trades.get(villagerLevel1).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 3),
                    new ItemStack(AllItems.COPPER_DIVING_HELMET.get(),
                            1),3,24,0.04F));
            trades.get(villagerLevel2).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 2),
                    new ItemStack(AllBlocks.COPPER_CASING.get(),
                            1),8,8,0.1F));
            trades.get(villagerLevel2).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 4),
                    new ItemStack(AllBlocks.SPOUT.get(),
                            1),10,12,0.1F));
            trades.get(villagerLevel2).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 2),
                    new ItemStack(AllBlocks.FLUID_TANK.get(),
                            1),10,10,0.1F));
            trades.get(villagerLevel2).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 3),
                    new ItemStack(AllBlocks.MECHANICAL_PUMP.get(),
                            2),10,10,0.1F));


            trades.get(villagerLevel3).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(AllBlocks.COPPER_CASING.get(), 4),
                    new ItemStack(Items.EMERALD, 1),
                    10,8,0.02F));
            trades.get(villagerLevel3).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 1),
                    new ItemStack(AllBlocks.FLUID_TANK.get(), 3),
                    10,10,0.01F));
            trades.get(villagerLevel3).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 4),
                    new ItemStack(AllBlocks.MECHANICAL_MIXER.get(), 1),
                    8,12,0.01F));
            trades.get(villagerLevel4).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 2),
                    new ItemStack(AllBlocks.FLUID_VALVE.get(), 3),
                    4,16,0.01F));
            trades.get(villagerLevel4).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 1),
                    new ItemStack(AllBlocks.LARGE_WATER_WHEEL.get(), 3),
                    6,10,0.01F));
            trades.get(villagerLevel5).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 1),
                    new ItemStack(AllItems.HONEYED_APPLE.get(), 8),
                    3,16,0.01F));
            trades.get(villagerLevel5).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 4),
                    new ItemStack(AllItems.COPPER_DIVING_BOOTS.get(),
                            1),3,16,0.02F));
            trades.get(villagerLevel5).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 7),
                    new ItemStack(AllBlocks.COPPER_BACKTANK.get(),
                            1),3,16,0.02F));


        }
    }
}





