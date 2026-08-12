package com.sudolev.dynamicvillage.villager;

import com.google.common.collect.ImmutableSet;
import com.simibubi.create.AllBlocks;
import com.sudolev.dynamicvillage.VillageLife;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModVillagers {
   public static final DeferredRegister<PoiType> POI_TYPES = DeferredRegister.create(ForgeRegistries.POI_TYPES, VillageLife.MODID);
   public static final DeferredRegister<VillagerProfession> VILLAGER_PROFESSIONS = DeferredRegister.create(
      ForgeRegistries.VILLAGER_PROFESSIONS, VillageLife.MODID
   );

   public static final RegistryObject<PoiType> CREATE_ENGINEER_POI = POI_TYPES.register(
      "create_engineer_poi",
      () -> new PoiType(ImmutableSet.copyOf(AllBlocks.SCHEMATIC_TABLE.get().getStateDefinition().getPossibleStates()), 1, 3)
   );
   public static final RegistryObject<PoiType> CREATE_HYDRAULIC_ENGINEER_POI = POI_TYPES.register(
      "create_hydraulic_engineer_poi",
      () -> new PoiType(ImmutableSet.copyOf(AllBlocks.ITEM_DRAIN.get().getStateDefinition().getPossibleStates()), 1, 2)
   );
   public static final RegistryObject<PoiType> CREATE_MINER_POI = POI_TYPES.register(
      "create_miner_poi",
      () -> new PoiType(ImmutableSet.copyOf(AllBlocks.MECHANICAL_DRILL.get().getStateDefinition().getPossibleStates()), 1, 1)
   );
   public static final RegistryObject<PoiType> CREATE_MECHANIC_POI = POI_TYPES.register(
      "create_mechanic_poi",
      () -> new PoiType(ImmutableSet.copyOf(AllBlocks.TRACK_STATION.get().getStateDefinition().getPossibleStates()), 1, 2)
   );

   public static final RegistryObject<VillagerProfession> MECHANICAL_ENGINEER = VILLAGER_PROFESSIONS.register(
      "mechanical_engineer",
      () -> new VillagerProfession(
            "mechanical_engineer",
            holder -> holder.value() == CREATE_ENGINEER_POI.get(),
            holder -> holder.value() == CREATE_ENGINEER_POI.get(),
            ImmutableSet.of(),
            ImmutableSet.of(),
            SoundEvents.VILLAGER_WORK_TOOLSMITH
         )
   );
   public static final RegistryObject<VillagerProfession> HYDRAULIC_ENGINEER = VILLAGER_PROFESSIONS.register(
      "hydraulic_engineer",
      () -> new VillagerProfession(
            "hydraulic_engineer",
            holder -> holder.value() == CREATE_HYDRAULIC_ENGINEER_POI.get(),
            holder -> holder.value() == CREATE_HYDRAULIC_ENGINEER_POI.get(),
            ImmutableSet.of(),
            ImmutableSet.of(),
            SoundEvents.VILLAGER_WORK_FISHERMAN
         )
   );
   public static final RegistryObject<VillagerProfession> TRAIN_MECHANIC = VILLAGER_PROFESSIONS.register(
      "train_mechanic",
      () -> new VillagerProfession(
            "train_mechanic",
            holder -> holder.value() == CREATE_MECHANIC_POI.get(),
            holder -> holder.value() == CREATE_MECHANIC_POI.get(),
            ImmutableSet.of(),
            ImmutableSet.of(),
            SoundEvents.VILLAGER_WORK_WEAPONSMITH
         )
   );
   public static final RegistryObject<VillagerProfession> MINER = VILLAGER_PROFESSIONS.register(
      "miner",
      () -> new VillagerProfession(
            "miner",
            holder -> holder.value() == CREATE_MINER_POI.get(),
            holder -> holder.value() == CREATE_MINER_POI.get(),
            ImmutableSet.of(),
            ImmutableSet.of(),
            SoundEvents.VILLAGER_WORK_MASON
         )
   );

   public static void register(IEventBus eventBus) {
      POI_TYPES.register(eventBus);
      VILLAGER_PROFESSIONS.register(eventBus);
   }
}
