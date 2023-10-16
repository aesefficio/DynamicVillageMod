package net.minecraft.world.level.levelgen;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import org.apache.commons.lang3.StringUtils;

public class WorldGenSettings {
   public static final Codec<WorldGenSettings> CODEC = RecordCodecBuilder.<WorldGenSettings>create((p_64626_) -> {
      return p_64626_.group(Codec.LONG.fieldOf("seed").stable().forGetter(WorldGenSettings::seed), Codec.BOOL.fieldOf("generate_features").orElse(true).stable().forGetter(WorldGenSettings::generateStructures), Codec.BOOL.fieldOf("bonus_chest").orElse(false).stable().forGetter(WorldGenSettings::generateBonusChest), RegistryCodecs.dataPackAwareCodec(Registry.LEVEL_STEM_REGISTRY, Lifecycle.stable(), LevelStem.CODEC).xmap(LevelStem::sortMap, Function.identity()).fieldOf("dimensions").forGetter(WorldGenSettings::dimensions), Codec.STRING.optionalFieldOf("legacy_custom_options").stable().forGetter((p_158959_) -> {
         return p_158959_.legacyCustomOptions;
      })).apply(p_64626_, p_64626_.stable(WorldGenSettings::new));
   }).comapFlatMap(WorldGenSettings::guardExperimental, Function.identity());
   private final long seed;
   private final boolean generateStructures;
   private final boolean generateBonusChest;
   private final Registry<LevelStem> dimensions;
   public final Optional<String> legacyCustomOptions;

   private DataResult<WorldGenSettings> guardExperimental() {
      LevelStem levelstem = this.dimensions.get(LevelStem.OVERWORLD);
      if (levelstem == null) {
         return DataResult.error("Overworld settings missing");
      } else {
         return this.stable() ? DataResult.success(this, Lifecycle.stable()) : DataResult.success(this);
      }
   }

   private boolean stable() {
      return LevelStem.stable(this.dimensions);
   }

   public WorldGenSettings(long pSeed, boolean pGenerateFeatures, boolean pGenerateBonusChest, Registry<LevelStem> pDimensions) {
      this(pSeed, pGenerateFeatures, pGenerateBonusChest, pDimensions, Optional.empty());
      LevelStem levelstem = pDimensions.get(LevelStem.OVERWORLD);
      if (levelstem == null) {
         throw new IllegalStateException("Overworld settings missing");
      }
   }

   public WorldGenSettings(long p_204638_, boolean p_204639_, boolean p_204640_, Registry<LevelStem> p_204641_, Optional<String> p_204642_) {
      this.seed = p_204638_;
      this.generateStructures = p_204639_;
      this.generateBonusChest = p_204640_;
      this.dimensions = p_204641_;
      this.legacyCustomOptions = p_204642_;
   }

   public long seed() {
      return this.seed;
   }

   public boolean generateStructures() {
      return this.generateStructures;
   }

   public boolean generateBonusChest() {
      return this.generateBonusChest;
   }

   public static WorldGenSettings replaceOverworldGenerator(RegistryAccess pRegistryAccess, WorldGenSettings pWorldGenSettings, ChunkGenerator pGenerator) {
      Registry<DimensionType> registry = pRegistryAccess.registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY);
      Registry<LevelStem> registry1 = withOverworld(registry, pWorldGenSettings.dimensions(), pGenerator);
      return new WorldGenSettings(pWorldGenSettings.seed(), pWorldGenSettings.generateStructures(), pWorldGenSettings.generateBonusChest(), registry1);
   }

   public static Registry<LevelStem> withOverworld(Registry<DimensionType> pDimensionTypes, Registry<LevelStem> pDimensions, ChunkGenerator pChunkGenerator) {
      LevelStem levelstem = pDimensions.get(LevelStem.OVERWORLD);
      Holder<DimensionType> holder = levelstem == null ? pDimensionTypes.getOrCreateHolderOrThrow(BuiltinDimensionTypes.OVERWORLD) : levelstem.typeHolder();
      return withOverworld(pDimensions, holder, pChunkGenerator);
   }

   public static Registry<LevelStem> withOverworld(Registry<LevelStem> pDimensions, Holder<DimensionType> pDimensionType, ChunkGenerator pChunkGenerator) {
      WritableRegistry<LevelStem> writableregistry = new MappedRegistry<>(Registry.LEVEL_STEM_REGISTRY, Lifecycle.experimental(), (Function<LevelStem, Holder.Reference<LevelStem>>)null);
      writableregistry.register(LevelStem.OVERWORLD, new LevelStem(pDimensionType, pChunkGenerator), Lifecycle.stable());

      for(Map.Entry<ResourceKey<LevelStem>, LevelStem> entry : pDimensions.entrySet()) {
         ResourceKey<LevelStem> resourcekey = entry.getKey();
         if (resourcekey != LevelStem.OVERWORLD) {
            writableregistry.register(resourcekey, entry.getValue(), pDimensions.lifecycle(entry.getValue()));
         }
      }

      return writableregistry;
   }

   public Registry<LevelStem> dimensions() {
      return this.dimensions;
   }

   public ChunkGenerator overworld() {
      LevelStem levelstem = this.dimensions.get(LevelStem.OVERWORLD);
      if (levelstem == null) {
         throw new IllegalStateException("Overworld settings missing");
      } else {
         return levelstem.generator();
      }
   }

   public ImmutableSet<ResourceKey<Level>> levels() {
      return this.dimensions().entrySet().stream().map(Map.Entry::getKey).map(WorldGenSettings::levelStemToLevel).collect(ImmutableSet.toImmutableSet());
   }

   public static ResourceKey<Level> levelStemToLevel(ResourceKey<LevelStem> p_190049_) {
      return ResourceKey.create(Registry.DIMENSION_REGISTRY, p_190049_.location());
   }

   public static ResourceKey<LevelStem> levelToLevelStem(ResourceKey<Level> p_190053_) {
      return ResourceKey.create(Registry.LEVEL_STEM_REGISTRY, p_190053_.location());
   }

   public boolean isDebug() {
      return this.overworld() instanceof DebugLevelSource;
   }

   public boolean isFlatWorld() {
      return this.overworld() instanceof FlatLevelSource;
   }

   public boolean isOldCustomizedWorld() {
      return this.legacyCustomOptions.isPresent();
   }

   public WorldGenSettings withBonusChest() {
      return new WorldGenSettings(this.seed, this.generateStructures, true, this.dimensions, this.legacyCustomOptions);
   }

   public WorldGenSettings withStructuresToggled() {
      return new WorldGenSettings(this.seed, !this.generateStructures, this.generateBonusChest, this.dimensions);
   }

   public WorldGenSettings withBonusChestToggled() {
      return new WorldGenSettings(this.seed, this.generateStructures, !this.generateBonusChest, this.dimensions);
   }

   public WorldGenSettings withSeed(boolean pHardcore, OptionalLong pLevelSeed) {
      long i = pLevelSeed.orElse(this.seed);
      Registry<LevelStem> registry;
      if (pLevelSeed.isPresent()) {
         WritableRegistry<LevelStem> writableregistry = new MappedRegistry<>(Registry.LEVEL_STEM_REGISTRY, Lifecycle.experimental(), (Function<LevelStem, Holder.Reference<LevelStem>>)null);

         for(Map.Entry<ResourceKey<LevelStem>, LevelStem> entry : this.dimensions.entrySet()) {
            ResourceKey<LevelStem> resourcekey = entry.getKey();
            writableregistry.register(resourcekey, new LevelStem(entry.getValue().typeHolder(), entry.getValue().generator()), this.dimensions.lifecycle(entry.getValue()));
         }

         registry = writableregistry;
      } else {
         registry = this.dimensions;
      }

      WorldGenSettings worldgensettings;
      if (this.isDebug()) {
         worldgensettings = new WorldGenSettings(i, false, false, registry);
      } else {
         worldgensettings = new WorldGenSettings(i, this.generateStructures(), this.generateBonusChest() && !pHardcore, registry);
      }

      return worldgensettings;
   }

   public static OptionalLong parseSeed(String pSeed) {
      pSeed = pSeed.trim();
      if (StringUtils.isEmpty(pSeed)) {
         return OptionalLong.empty();
      } else {
         try {
            return OptionalLong.of(Long.parseLong(pSeed));
         } catch (NumberFormatException numberformatexception) {
            return OptionalLong.of((long)pSeed.hashCode());
         }
      }
   }
}