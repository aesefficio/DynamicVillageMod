package net.minecraft.world.level.dimension;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.TheEndBiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

public final class LevelStem {
   public static final Codec<LevelStem> CODEC = RecordCodecBuilder.create((p_63986_) -> {
      return p_63986_.group(DimensionType.CODEC.fieldOf("type").forGetter(LevelStem::typeHolder), ChunkGenerator.CODEC.fieldOf("generator").forGetter(LevelStem::generator)).apply(p_63986_, p_63986_.stable(LevelStem::new));
   });
   public static final ResourceKey<LevelStem> OVERWORLD = ResourceKey.create(Registry.LEVEL_STEM_REGISTRY, new ResourceLocation("overworld"));
   public static final ResourceKey<LevelStem> NETHER = ResourceKey.create(Registry.LEVEL_STEM_REGISTRY, new ResourceLocation("the_nether"));
   public static final ResourceKey<LevelStem> END = ResourceKey.create(Registry.LEVEL_STEM_REGISTRY, new ResourceLocation("the_end"));
   private static final Set<ResourceKey<LevelStem>> BUILTIN_ORDER = ImmutableSet.of(OVERWORLD, NETHER, END);
   private final Holder<DimensionType> type;
   private final ChunkGenerator generator;

   public LevelStem(Holder<DimensionType> p_204519_, ChunkGenerator p_204520_) {
      this.type = p_204519_;
      this.generator = p_204520_;
   }

   public Holder<DimensionType> typeHolder() {
      return this.type;
   }

   public ChunkGenerator generator() {
      return this.generator;
   }

   public static Stream<ResourceKey<LevelStem>> keysInOrder(Stream<ResourceKey<LevelStem>> p_223606_) {
      return Stream.concat(BUILTIN_ORDER.stream(), p_223606_.filter((p_223600_) -> {
         return !BUILTIN_ORDER.contains(p_223600_);
      }));
   }

   public static Registry<LevelStem> sortMap(Registry<LevelStem> pRegistry) {
      WritableRegistry<LevelStem> writableregistry = new MappedRegistry<>(Registry.LEVEL_STEM_REGISTRY, Lifecycle.experimental(), (Function<LevelStem, Holder.Reference<LevelStem>>)null);
      keysInOrder(pRegistry.registryKeySet().stream()).forEach((p_223604_) -> {
         LevelStem levelstem = pRegistry.get(p_223604_);
         if (levelstem != null) {
            writableregistry.register(p_223604_, levelstem, pRegistry.lifecycle(levelstem));
         }

      });
      return writableregistry;
   }

   public static boolean stable(Registry<LevelStem> pRegistry) {
      if (pRegistry.size() != BUILTIN_ORDER.size()) {
         return false;
      } else {
         Optional<LevelStem> optional = pRegistry.getOptional(OVERWORLD);
         Optional<LevelStem> optional1 = pRegistry.getOptional(NETHER);
         Optional<LevelStem> optional2 = pRegistry.getOptional(END);
         if (!optional.isEmpty() && !optional1.isEmpty() && !optional2.isEmpty()) {
            if (!optional.get().typeHolder().is(BuiltinDimensionTypes.OVERWORLD) && !optional.get().typeHolder().is(BuiltinDimensionTypes.OVERWORLD_CAVES)) {
               return false;
            } else if (!optional1.get().typeHolder().is(BuiltinDimensionTypes.NETHER)) {
               return false;
            } else if (!optional2.get().typeHolder().is(BuiltinDimensionTypes.END)) {
               return false;
            } else if (optional1.get().generator() instanceof NoiseBasedChunkGenerator && optional2.get().generator() instanceof NoiseBasedChunkGenerator) {
               NoiseBasedChunkGenerator noisebasedchunkgenerator = (NoiseBasedChunkGenerator)optional1.get().generator();
               NoiseBasedChunkGenerator noisebasedchunkgenerator1 = (NoiseBasedChunkGenerator)optional2.get().generator();
               if (!noisebasedchunkgenerator.stable(NoiseGeneratorSettings.NETHER)) {
                  return false;
               } else if (!noisebasedchunkgenerator1.stable(NoiseGeneratorSettings.END)) {
                  return false;
               } else if (!(noisebasedchunkgenerator.getBiomeSource() instanceof MultiNoiseBiomeSource)) {
                  return false;
               } else {
                  MultiNoiseBiomeSource multinoisebiomesource = (MultiNoiseBiomeSource)noisebasedchunkgenerator.getBiomeSource();
                  if (!multinoisebiomesource.stable(MultiNoiseBiomeSource.Preset.NETHER)) {
                     return false;
                  } else {
                     BiomeSource biomesource = optional.get().generator().getBiomeSource();
                     if (biomesource instanceof MultiNoiseBiomeSource && !((MultiNoiseBiomeSource)biomesource).stable(MultiNoiseBiomeSource.Preset.OVERWORLD)) {
                        return false;
                     } else {
                        return noisebasedchunkgenerator1.getBiomeSource() instanceof TheEndBiomeSource;
                     }
                  }
               }
            } else {
               return false;
            }
         } else {
            return false;
         }
      }
   }
}