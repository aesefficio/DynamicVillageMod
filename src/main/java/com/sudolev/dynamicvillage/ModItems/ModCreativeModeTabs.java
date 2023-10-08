package com.sudolev.dynamicvillage.ModItems;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ModCreativeModeTabs {
    public static final CreativeModeTab VILLAGE_LIFE_TAB = new CreativeModeTab("village_life_tab") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(Items.VILLAGER_SPAWN_EGG);
        }
    };
}




