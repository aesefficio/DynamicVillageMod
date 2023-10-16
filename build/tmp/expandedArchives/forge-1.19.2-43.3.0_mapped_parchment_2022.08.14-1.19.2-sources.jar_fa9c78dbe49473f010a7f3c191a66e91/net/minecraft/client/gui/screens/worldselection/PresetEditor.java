package net.minecraft.client.gui.screens.worldselection;

import java.util.Map;
import java.util.Optional;
import net.minecraft.client.gui.screens.CreateBuffetWorldScreen;
import net.minecraft.client.gui.screens.CreateFlatWorldScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface PresetEditor {
   Map<Optional<ResourceKey<WorldPreset>>, PresetEditor> EDITORS = Map.of(Optional.of(WorldPresets.FLAT), (p_232974_, p_232975_) -> {
      ChunkGenerator chunkgenerator = p_232975_.worldGenSettings().overworld();
      RegistryAccess registryaccess = p_232975_.registryAccess();
      Registry<Biome> registry = registryaccess.registryOrThrow(Registry.BIOME_REGISTRY);
      Registry<StructureSet> registry1 = registryaccess.registryOrThrow(Registry.STRUCTURE_SET_REGISTRY);
      return new CreateFlatWorldScreen(p_232974_, (p_232960_) -> {
         p_232974_.worldGenSettingsComponent.updateSettings(flatWorldConfigurator(p_232960_));
      }, chunkgenerator instanceof FlatLevelSource ? ((FlatLevelSource)chunkgenerator).settings() : FlatLevelGeneratorSettings.getDefault(registry, registry1));
   }, Optional.of(WorldPresets.SINGLE_BIOME_SURFACE), (p_232962_, p_232963_) -> {
      return new CreateBuffetWorldScreen(p_232962_, p_232963_, (p_232966_) -> {
         p_232962_.worldGenSettingsComponent.updateSettings(fixedBiomeConfigurator(p_232966_));
      });
   });

   Screen createEditScreen(CreateWorldScreen p_232977_, WorldCreationContext p_232978_);

   private static WorldCreationContext.Updater flatWorldConfigurator(FlatLevelGeneratorSettings p_232953_) {
      return (p_232956_, p_232957_) -> {
         Registry<StructureSet> registry = p_232956_.registryOrThrow(Registry.STRUCTURE_SET_REGISTRY);
         ChunkGenerator chunkgenerator = new FlatLevelSource(registry, p_232953_);
         return WorldGenSettings.replaceOverworldGenerator(p_232956_, p_232957_, chunkgenerator);
      };
   }

   private static WorldCreationContext.Updater fixedBiomeConfigurator(Holder<Biome> p_232968_) {
      return (p_232971_, p_232972_) -> {
         Registry<StructureSet> registry = p_232971_.registryOrThrow(Registry.STRUCTURE_SET_REGISTRY);
         Registry<NoiseGeneratorSettings> registry1 = p_232971_.registryOrThrow(Registry.NOISE_GENERATOR_SETTINGS_REGISTRY);
         Registry<NormalNoise.NoiseParameters> registry2 = p_232971_.registryOrThrow(Registry.NOISE_REGISTRY);
         Holder<NoiseGeneratorSettings> holder = registry1.getOrCreateHolderOrThrow(NoiseGeneratorSettings.OVERWORLD);
         BiomeSource biomesource = new FixedBiomeSource(p_232968_);
         ChunkGenerator chunkgenerator = new NoiseBasedChunkGenerator(registry, registry2, biomesource, holder);
         return WorldGenSettings.replaceOverworldGenerator(p_232971_, p_232972_, chunkgenerator);
      };
   }
}