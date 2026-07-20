package com.sudolev.dynamicvillage;

import com.sudolev.dynamicvillage.config.VillageConfig;
import com.sudolev.dynamicvillage.villager.ModVillagers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(VillageLife.MODID)
public class VillageLife {
   public static final String MODID = "dynamicvillage";

   public VillageLife() {
      IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
      modEventBus.addListener(this::commonSetup);
      ModVillagers.register(modEventBus);
      com.sudolev.dynamicvillage.profession.ProfessionRegistrar.register(modEventBus);
      MinecraftForge.EVENT_BUS.register(this);
      ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, VillageConfig.SPEC);

      // Hook our config into the Mods list "Config" button (client only).
      if (FMLEnvironment.dist == Dist.CLIENT) {
         com.sudolev.dynamicvillage.client.ModConfigScreen.register();
      }
   }

   private void commonSetup(FMLCommonSetupEvent event) {
   }

   @SubscribeEvent
   public void onServerStarting(ServerStartingEvent event) {
   }

   @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
   public static class ClientModEvents {
      @SubscribeEvent
      public static void onClientSetup(FMLClientSetupEvent event) {
      }
   }
}
