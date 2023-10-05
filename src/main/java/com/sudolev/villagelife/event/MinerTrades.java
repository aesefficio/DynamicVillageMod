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
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = VillageLife.MODID)
public class MinerTrades {
    @SubscribeEvent
    public static void addCustomTrades(VillagerTradesEvent event) {
        if(event.getType() == ModVillagers.MINER.get()) {
            Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = event.getTrades();
            int villagerLevel1 = 1;
            int villagerLevel2 = 2;
            int villagerLevel3 = 3;
            int villagerLevel4 = 4;
            int villagerLevel5 = 5;
            trades.get(villagerLevel1).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 1),
                    new ItemStack(Items.ANDESITE,
                            14),8,8,0.02F));
            trades.get(villagerLevel1).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.IRON_PICKAXE, 1),
                    new ItemStack(Items.EMERALD, 2),
                    6,8,0.01F));
            trades.get(villagerLevel1).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 1),
                    new ItemStack(Items.IRON_INGOT,
                            4),10,8,0.02F));
            trades.get(villagerLevel1).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 1),
                    new ItemStack(AllItems.RAW_ZINC.get(), 5),
                    10,8,0.02F));

            trades.get(villagerLevel2).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 5),
                    new ItemStack(AllBlocks.MECHANICAL_DRILL.get(),
                            1),8,8,0.02F));
            trades.get(villagerLevel2).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 1),
                    new ItemStack(Items.TORCH,
                            12),8,8,0.1F));
            trades.get(villagerLevel2).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 4),
                    new ItemStack(Blocks.BLAST_FURNACE,
                            1),10,12,0.1F));

            trades.get(villagerLevel3).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 2),
                    new ItemStack(AllItems.BRASS_NUGGET.get(), 35),
                    10,10,0.01F));

            trades.get(villagerLevel3).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 4),
                    new ItemStack(AllBlocks.BLAZE_BURNER.get(), 1),
                    4,16,0.01F));

            trades.get(villagerLevel3).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 3),
                    new ItemStack(AllBlocks.ENCASED_FAN.get(), 1),
                    3,16,0.01F));

            trades.get(villagerLevel4).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 7),
                    new ItemStack(AllBlocks.ZINC_BLOCK.get(), 1),
                    4,12,0.01F));
            trades.get(villagerLevel4).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 5),
                    new ItemStack(AllItems.BRASS_INGOT.get(), 3),
                    6,10,0.01F));
            trades.get(villagerLevel4).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(AllBlocks.ELEVATOR_PULLEY.get(), 1),
                    new ItemStack(Items.EMERALD, 3),
                    4,10,0.01F));
            trades.get(villagerLevel5).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 5),
                    new ItemStack(AllBlocks.CRUSHING_WHEEL.get(), 2),
                    8,12,0.01F));
            trades.get(villagerLevel5).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 9),
                    new ItemStack(AllBlocks.MECHANICAL_ROLLER.get(), 2),
                    2,24,0.01F));
            trades.get(villagerLevel5).add((trader, rand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 3),
                    new ItemStack(AllBlocks.COPPER_CASING.get(), 2),
                    8,12,0.01F));
        }
    }
}





