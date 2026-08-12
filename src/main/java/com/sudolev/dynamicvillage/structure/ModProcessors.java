package com.sudolev.dynamicvillage.structure;

import com.sudolev.dynamicvillage.VillageLife;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** Registers the mod's structure processor type(s). */
public class ModProcessors {
   public static final DeferredRegister<StructureProcessorType<?>> PROCESSORS =
      DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, VillageLife.MODID);

   public static final RegistryObject<StructureProcessorType<ChestLootProcessor>> CHEST_LOOT =
      PROCESSORS.register("chest_loot", () -> () -> ChestLootProcessor.CODEC);

   public static void register(IEventBus modEventBus) {
      PROCESSORS.register(modEventBus);
   }
}
