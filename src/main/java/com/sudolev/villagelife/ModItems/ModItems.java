package com.sudolev.villagelife.ModItems;

import com.sudolev.villagelife.VillageLife;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, VillageLife.MODID);

    public static final RegistryObject<Item> MECHANICAL_ENGINEER_SPAWN_EGG = ITEMS.register("mechanical_engineer_spawn_egg",
            () -> new Item(new Item.Properties().tab(ModCreativeModeTabs.VILLAGE_LIFE_TAB)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}






