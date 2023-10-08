package com.sudolev.dynamicvillage.event;


import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.sudolev.dynamicvillage.VillageLife;
import com.sudolev.dynamicvillage.villager.ModVillagers;
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
public class MechanicalEngineerTrades {
    @SubscribeEvent
    public static void addCustomTrades(VillagerTradesEvent event) {
        if(event.getType() == ModVillagers.MECHANICAL_ENGINEER.get()) {
            Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = event.getTrades();
            int villagerLevel1 = 1;
            int villagerLevel2 = 2;
            int villagerLevel3 = 3;
            int villagerLevel4 = 4;
            int villagerLevel5 = 5;
            trades.get(villagerLevel1).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 2),
                    new ItemStack(AllItems.ANDESITE_ALLOY.get(),
                            8),8,8,0.02F));
            trades.get(villagerLevel1).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.ANDESITE, 20),
                    new ItemStack(Items.EMERALD, 1),
                    8,8,0.01F));
            trades.get(villagerLevel1).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 1),
                    new ItemStack(AllItems.IRON_SHEET.get(),
                            4),10,8,0.02F));
            trades.get(villagerLevel1).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(AllItems.RAW_ZINC.get(), 6),
                    new ItemStack(Items.EMERALD, 1),
                    10,8,0.02F));
            trades.get(villagerLevel1).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 5),
                    new ItemStack(AllItems.WRENCH.get(),
                            1),3,24,0.04F));


            trades.get(villagerLevel2).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 3),
                    new ItemStack(AllBlocks.MECHANICAL_BEARING.get(),
                            1),8,8,0.02F));
            trades.get(villagerLevel2).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 6),
                    new ItemStack(AllItems.GOGGLES.get(),
                            1),3,32,0.1F));
            trades.get(villagerLevel2).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(AllBlocks.COGWHEEL.get(), 14),
                    new ItemStack(Items.EMERALD, 1),
                    10,8,0.02F));
            trades.get(villagerLevel2).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 3),
                    new ItemStack(AllBlocks.WATER_WHEEL.get(),
                            4),10,12,0.1F));
            trades.get(villagerLevel2).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 2),
                    new ItemStack(AllBlocks.BASIN.get(),
                            1),10,12,0.1F));
            trades.get(villagerLevel2).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 2),
                    new ItemStack(AllBlocks.DEPOT.get(),
                            1),10,10,0.1F));

            trades.get(villagerLevel3).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(AllBlocks.BELT.get(), 10),
                    new ItemStack(Items.EMERALD, 3),
                    10,10,0.01F));

            trades.get(villagerLevel3).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 6),
                    new ItemStack(AllBlocks.STRESSOMETER.get(), 1),
                    3,16,0.01F));

            trades.get(villagerLevel4).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 8),
                    new ItemStack(AllBlocks.SPEEDOMETER.get(), 1),
                    4,38,0.01F));
            trades.get(villagerLevel4).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(AllBlocks.LARGE_WATER_WHEEL.get(), 4),
                    new ItemStack(Items.EMERALD, 1),
                    6,10,0.01F));
            trades.get(villagerLevel4).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(AllBlocks.CHUTE.get(), 3),
                    new ItemStack(Items.EMERALD, 1),
                    10,10,0.01F));
            trades.get(villagerLevel5).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 5),
                    new ItemStack(AllBlocks.CRUSHING_WHEEL.get(), 1),
                    8,28,0.01F));
            trades.get(villagerLevel5).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 9),
                    new ItemStack(AllBlocks.MECHANICAL_ROLLER.get(), 2),
                    2,24,0.01F));
            trades.get(villagerLevel5).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 10),
                    new ItemStack(AllBlocks.MECHANICAL_ARM.get(), 1),
                    3,40,0.01F));
            trades.get(villagerLevel5).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 3),
                    new ItemStack(AllBlocks.MECHANICAL_CRAFTER.get(), 2),
                    8,12,0.01F));
        }
    }
}





