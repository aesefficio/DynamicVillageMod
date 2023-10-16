package net.minecraft.world.level.levelgen.presets;

import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.TheEndBiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.DebugLevelSource;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public class WorldPresets {
   public static final ResourceKey<WorldPreset> NORMAL = register("normal");
   public static final ResourceKey<WorldPreset> FLAT = register("flat");
   public static final ResourceKey<WorldPreset> LARGE_BIOMES = register("large_biomes");
   public static final ResourceKey<WorldPreset> AMPLIFIED = register("amplified");
   public static final ResourceKey<WorldPreset> SINGLE_BIOME_SURFACE = register("single_biome_surface");
   public static final ResourceKey<WorldPreset> DEBUG = register("debug_all_block_states");

   public static Holder<WorldPreset> bootstrap(Registry<WorldPreset> p_226448_) {
      return (new WorldPresets.Bootstrap(p_226448_)).run();
   }

   private static ResourceKey<WorldPreset> register(String p_226460_) {
      return ResourceKey.create(Registry.WORLD_PRESET_REGISTRY, new ResourceLocation(p_226460_));
   }

   public static Optional<ResourceKey<WorldPreset>> fromSettings(WorldGenSettings p_226446_) {
      ChunkGenerator chunkgenerator = p_226446_.overworld();
      if (chunkgenerator instanceof FlatLevelSource) {
         return Optional.of(FLAT);
      } else {
         return chunkgenerator instanceof DebugLevelSource ? Optional.of(DEBUG) : Optional.empty();
      }
   }

   public static WorldGenSettings createNormalWorldFromPreset(RegistryAccess p_226455_, long p_226456_, boolean p_226457_, boolean p_226458_) {
      return p_226455_.registryOrThrow(Registry.WORLD_PRESET_REGISTRY).getHolderOrThrow(NORMAL).value().createWorldGenSettings(p_226456_, p_226457_, p_226458_);
   }

   public static WorldGenSettings createNormalWorldFromPreset(RegistryAccess p_226452_, long p_226453_) {
      return createNormalWorldFromPreset(p_226452_, p_226453_, true, false);
   }

   public static WorldGenSettings createNormalWorldFromPreset(RegistryAccess p_226450_) {
      return createNormalWorldFromPreset(p_226450_, RandomSource.create().nextLong());
   }

   public static WorldGenSettings demoSettings(RegistryAccess p_226462_) {
      return createNormalWorldFromPreset(p_226462_, (long)"North Carolina".hashCode(), true, true);
   }

   public static LevelStem getNormalOverworld(RegistryAccess p_226464_) {
      return p_226464_.registryOrThrow(Registry.WORLD_PRESET_REGISTRY).getHolderOrThrow(NORMAL).value().overworldOrThrow();
   }

   static class Bootstrap {
      private final Registry<WorldPreset> presets;
      private final Registry<DimensionType> dimensionTypes = BuiltinRegistries.DIMENSION_TYPE;
      private final Registry<Biome> biomes = BuiltinRegistries.BIOME;
      private final Registry<StructureSet> structureSets = BuiltinRegistries.STRUCTURE_SETS;
      private final Registry<NoiseGeneratorSettings> noiseSettings = BuiltinRegistries.NOISE_GENERATOR_SETTINGS;
      private final Registry<NormalNoise.NoiseParameters> noises = BuiltinRegistries.NOISE;
      private final Holder<DimensionType> overworldDimensionType = this.dimensionTypes.getOrCreateHolderOrThrow(BuiltinDimensionTypes.OVERWORLD);
      private final Holder<DimensionType> netherDimensionType = this.dimensionTypes.getOrCreateHolderOrThrow(BuiltinDimensionTypes.NETHER);
      private final Holder<NoiseGeneratorSettings> netherNoiseSettings = this.noiseSettings.getOrCreateHolderOrThrow(NoiseGeneratorSettings.NETHER);
      private final LevelStem netherStem = new LevelStem(this.netherDimensionType, new NoiseBasedChunkGenerator(this.structureSets, this.noises, MultiNoiseBiomeSource.Preset.NETHER.biomeSource(this.biomes), this.netherNoiseSettings));
      private final Holder<DimensionType> endDimensionType = this.dimensionTypes.getOrCreateHolderOrThrow(BuiltinDimensionTypes.END);
      private final Holder<NoiseGeneratorSettings> endNoiseSettings = this.noiseSettings.getOrCreateHolderOrThrow(NoiseGeneratorSettings.END);
      private final LevelStem endStem = new LevelStem(this.endDimensionType, new NoiseBasedChunkGenerator(this.structureSets, this.noises, new TheEndBiomeSource(this.biomes), this.endNoiseSettings));

      Bootstrap(Registry<WorldPreset> p_226479_) {
         this.presets = p_226479_;
      }

      private LevelStem makeOverworld(ChunkGenerator p_226488_) {
         return new LevelStem(this.overworldDimensionType, p_226488_);
      }

      private LevelStem makeNoiseBasedOverworld(BiomeSource p_226485_, Holder<NoiseGeneratorSettings> p_226486_) {
         return this.makeOverworld(new NoiseBasedChunkGenerator(this.structureSets, this.noises, p_226485_, p_226486_));
      }

      private WorldPreset createPresetWithCustomOverworld(LevelStem p_226490_) {
         return new WorldPreset(Map.of(LevelStem.OVERWORLD, p_226490_, LevelStem.NETHER, this.netherStem, LevelStem.END, this.endStem));
      }

      private Holder<WorldPreset> registerCustomOverworldPreset(ResourceKey<WorldPreset> p_226482_, LevelStem p_226483_) {
         return BuiltinRegistries.register(this.presets, p_226482_, this.createPresetWithCustomOverworld(p_226483_));
      }

      public Holder<WorldPreset> run() {
         MultiNoiseBiomeSource multinoisebiomesource = MultiNoiseBiomeSource.Preset.OVERWORLD.biomeSource(this.biomes);
         Holder<NoiseGeneratorSettings> holder = this.noiseSettings.getOrCreateHolderOrThrow(NoiseGeneratorSettings.OVERWORLD);
         this.registerCustomOverworldPreset(WorldPresets.NORMAL, this.makeNoiseBasedOverworld(multinoisebiomesource, holder));
         Holder<NoiseGeneratorSettings> holder1 = this.noiseSettings.getOrCreateHolderOrThrow(NoiseGeneratorSettings.LARGE_BIOMES);
         this.registerCustomOverworldPreset(WorldPresets.LARGE_BIOMES, this.makeNoiseBasedOverworld(multinoisebiomesource, holder1));
         Holder<NoiseGeneratorSettings> holder2 = this.noiseSettings.getOrCreateHolderOrThrow(NoiseGeneratorSettings.AMPLIFIED);
         this.registerCustomOverworldPreset(WorldPresets.AMPLIFIED, this.makeNoiseBasedOverworld(multinoisebiomesource, holder2));
         this.registerCustomOverworldPreset(WorldPresets.SINGLE_BIOME_SURFACE, this.makeNoiseBasedOverworld(new FixedBiomeSource(this.biomes.getOrCreateHolderOrThrow(Biomes.PLAINS)), holder));
         this.registerCustomOverworldPreset(WorldPresets.FLAT, this.makeOverworld(new FlatLevelSource(this.structureSets, FlatLevelGeneratorSettings.getDefault(this.biomes, this.structureSets))));
         return this.registerCustomOverworldPreset(WorldPresets.DEBUG, this.makeOverworld(new DebugLevelSource(this.structureSets, this.biomes)));
      }
   }
}