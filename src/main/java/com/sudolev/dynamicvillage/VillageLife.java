package com.sudolev.dynamicvillage;

import com.sudolev.dynamicvillage.config.VillageConfig;
import com.sudolev.dynamicvillage.villager.ModVillagers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

@Mod(VillageLife.MODID)
public class VillageLife {
   public static final String MODID = "dynamicvillage";

   public VillageLife(IEventBus modEventBus, ModContainer modContainer) {
      modEventBus.addListener(this::commonSetup);
      ModVillagers.register(modEventBus);
      com.sudolev.dynamicvillage.profession.ProfessionRegistrar.register(modEventBus);
      NeoForge.EVENT_BUS.register(this);
      modContainer.registerConfig(ModConfig.Type.COMMON, VillageConfig.SPEC);

      // Expose the config in-game via Mods -> Create: Dynamic Village -> Config (client only).
      if (FMLEnvironment.dist == Dist.CLIENT) {
         com.sudolev.dynamicvillage.client.ModConfigScreen.register(modContainer);
      }
   }

   private void commonSetup(FMLCommonSetupEvent event) {
   }

   @SubscribeEvent
   public void onServerStarting(ServerStartingEvent event) {
   }

   @EventBusSubscriber(
      modid = MODID,
      value = {Dist.CLIENT}
   )
   public static class ClientModEvents {
      @SubscribeEvent
      public static void onClientSetup(FMLClientSetupEvent event) {
      }
   }
}
