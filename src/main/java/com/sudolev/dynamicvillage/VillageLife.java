package com.sudolev.dynamicvillage;

import com.sudolev.dynamicvillage.client.ModConfigScreen;
import com.sudolev.dynamicvillage.config.VillageConfig;
import com.sudolev.dynamicvillage.profession.ProfessionRegistrar;
import com.sudolev.dynamicvillage.structure.ModProcessors;
import com.sudolev.dynamicvillage.villager.ModVillagers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(VillageLife.MODID)
public class VillageLife {
   public static final String MODID = "dynamicvillage";

   public VillageLife() {
      IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
      ModVillagers.register(modEventBus);
      ProfessionRegistrar.register(modEventBus);
      ModProcessors.register(modEventBus);
      ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, VillageConfig.SPEC);

      // Hook our config into the Mods list "Config" button (client only).
      if (FMLEnvironment.dist == Dist.CLIENT) {
         ModConfigScreen.register();
      }
   }
}
