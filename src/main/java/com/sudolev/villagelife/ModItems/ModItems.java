package com.sudolev.villagelife.ModItems;

import com.sudolev.villagelife.VillageLife;
import com.sudolev.villagelife.villager.ModVillagers;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, VillageLife.MODID);

    public static final RegistryObject<Item> MECHANICAL_ENGINEER_SPAWN_EGG = ITEMS.register("mechanical_engineer_spawn_egg",
            () -> new ForgeSpawnEggItem(([WHAT GOES HERE], 111111111, 111111111, new Item.Properties().tab(ModCreativeModeTabs.VILLAGE_LIFE_TAB)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}






