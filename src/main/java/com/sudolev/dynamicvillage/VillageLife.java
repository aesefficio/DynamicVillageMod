package com.sudolev.dynamicvillage;

import com.sudolev.dynamicvillage.client.ModConfigScreen;
import com.sudolev.dynamicvillage.config.VillageConfig;
import com.sudolev.dynamicvillage.profession.ProfessionRegistrar;
import com.sudolev.dynamicvillage.structure.ModProcessors;
import com.sudolev.dynamicvillage.villager.ModVillagers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(VillageLife.MODID)
public class VillageLife {
   public static final String MODID = "dynamicvillage";

   public VillageLife(IEventBus modEventBus, ModContainer modContainer) {
      ModVillagers.register(modEventBus);
      ProfessionRegistrar.register(modEventBus);
      ModProcessors.register(modEventBus);
      modContainer.registerConfig(ModConfig.Type.COMMON, VillageConfig.SPEC);

      // Expose the config in-game via Mods -> Create: Dynamic Village -> Config (client only).
      if (FMLEnvironment.dist == Dist.CLIENT) {
         ModConfigScreen.register(modContainer);
      }
   }
}
