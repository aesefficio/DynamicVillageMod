package com.sudolev.villagelife.villager;

import com.google.common.collect.ImmutableSet;
import com.simibubi.create.AllBlocks;
import com.sudolev.villagelife.VillageLife;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.lang.reflect.InvocationTargetException;

public class ModVillagers {
    public static final DeferredRegister<PoiType> POI_TYPES =
            DeferredRegister.create(ForgeRegistries.POI_TYPES, VillageLife.MODID);
    public static final DeferredRegister<VillagerProfession> VILLAGER_PROFESSIONS =
            DeferredRegister.create(ForgeRegistries.VILLAGER_PROFESSIONS, VillageLife.MODID);

    public static final RegistryObject<PoiType> CREATE_ENGINEER_POI = POI_TYPES.register("create_engineer_poi",
            () -> new PoiType(ImmutableSet.copyOf(AllBlocks.LARGE_WATER_WHEEL.get().getStateDefinition().getPossibleStates()),
                    1, 1));
    public static final RegistryObject<PoiType> CREATE_HYDRAULIC_ENGINEER_POI = POI_TYPES.register("create_hydraulic_engineer_poi",
            () -> new PoiType(ImmutableSet.copyOf(AllBlocks.SPOUT.get().getStateDefinition().getPossibleStates()),
                    1, 1));
    public static final RegistryObject<PoiType> CREATE_MINER_POI = POI_TYPES.register("create_miner_poi",
            () -> new PoiType(ImmutableSet.copyOf(AllBlocks.MECHANICAL_DRILL.get().getStateDefinition().getPossibleStates()),
                    1, 1));

    public static final RegistryObject<PoiType> CREATE_MECHANIC_POI = POI_TYPES.register("create_mechanic_poi",
            () -> new PoiType(ImmutableSet.copyOf(AllBlocks.TRACK_STATION.get().getStateDefinition().getPossibleStates()),
                    1, 1));

    public static final RegistryObject<VillagerProfession> MECHANICAL_ENGINEER = VILLAGER_PROFESSIONS.register("mechanical_engineer",
            () -> new VillagerProfession("mechanical_engineer", x -> x.get() == CREATE_ENGINEER_POI.get(),
                    x -> x.get() == CREATE_ENGINEER_POI.get(), ImmutableSet.of(), ImmutableSet.of(),
                    SoundEvents.VILLAGER_WORK_ARMORER));
    public static final RegistryObject<VillagerProfession> HYDRAULIC_ENGINEER = VILLAGER_PROFESSIONS.register("hydraulic_engineer",
            () -> new VillagerProfession("hydraulic_engineer", x -> x.get() == CREATE_HYDRAULIC_ENGINEER_POI.get(),
                    x -> x.get() == CREATE_HYDRAULIC_ENGINEER_POI.get(), ImmutableSet.of(), ImmutableSet.of(),
                    SoundEvents.VILLAGER_WORK_LEATHERWORKER));
    public static final RegistryObject<VillagerProfession> TRAIN_MECHANIC = VILLAGER_PROFESSIONS.register("train_mechanic",
            () -> new VillagerProfession("train_mechanic", x -> x.get() == CREATE_MECHANIC_POI.get(),
                    x -> x.get() == CREATE_MECHANIC_POI.get(), ImmutableSet.of(), ImmutableSet.of(),
                    SoundEvents.VILLAGER_WORK_TOOLSMITH));
    public static final RegistryObject<VillagerProfession> MINER = VILLAGER_PROFESSIONS.register("miner",
            () -> new VillagerProfession("miner", x -> x.get() == CREATE_MINER_POI.get(),
                    x -> x.get() == CREATE_MINER_POI.get(), ImmutableSet.of(), ImmutableSet.of(),
                    SoundEvents.VILLAGER_WORK_MASON));




    public static void registerPOIs() {
        try {
            ObfuscationReflectionHelper.findMethod(PoiType.class,
                    "registerBlockStates", PoiType.class).invoke(null, CREATE_ENGINEER_POI.get());
            ObfuscationReflectionHelper.findMethod(PoiType.class,
                    "registerBlockStates", PoiType.class).invoke(null, CREATE_HYDRAULIC_ENGINEER_POI.get());
            ObfuscationReflectionHelper.findMethod(PoiType.class,
                    "registerBlockStates", PoiType.class).invoke(null, CREATE_MECHANIC_POI.get());
            ObfuscationReflectionHelper.findMethod(PoiType.class,
                    "registerBlockStates", PoiType.class).invoke(null, CREATE_MINER_POI.get());


        } catch (InvocationTargetException | IllegalAccessException exception) {
            exception.printStackTrace();
        }
    }

    public static void register(IEventBus eventBus) {
        POI_TYPES.register(eventBus);
        VILLAGER_PROFESSIONS.register(eventBus);
    }
}