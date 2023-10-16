package net.minecraft.world.level.biome;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.level.levelgen.NoiseRouterData;

public class MultiNoiseBiomeSource extends BiomeSource {
   public static final MapCodec<MultiNoiseBiomeSource> DIRECT_CODEC = RecordCodecBuilder.mapCodec((p_187070_) -> {
      return p_187070_.group(ExtraCodecs.<Pair<Climate.ParameterPoint, Holder<Biome>>>nonEmptyList(RecordCodecBuilder.<Pair<Climate.ParameterPoint, Holder<Biome>>>create((p_187078_) -> {
         return p_187078_.group(Climate.ParameterPoint.CODEC.fieldOf("parameters").forGetter(Pair::getFirst), Biome.CODEC.fieldOf("biome").forGetter(Pair::getSecond)).apply(p_187078_, Pair::of);
      }).listOf()).xmap(Climate.ParameterList::new, (Function<Climate.ParameterList<Holder<Biome>>, List<Pair<Climate.ParameterPoint, Holder<Biome>>>>) Climate.ParameterList::values).fieldOf("biomes").forGetter((p_187080_) -> {
         return p_187080_.parameters;
      })).apply(p_187070_, MultiNoiseBiomeSource::new);
   });
   public static final Codec<MultiNoiseBiomeSource> CODEC = Codec.mapEither(MultiNoiseBiomeSource.PresetInstance.CODEC, DIRECT_CODEC).xmap((p_187068_) -> {
      return p_187068_.map(MultiNoiseBiomeSource.PresetInstance::biomeSource, Function.identity());
   }, (p_187066_) -> {
      return p_187066_.preset().map(Either::<MultiNoiseBiomeSource.PresetInstance, MultiNoiseBiomeSource>left).orElseGet(() -> {
         return Either.right(p_187066_);
      });
   }).codec();
   private final Climate.ParameterList<Holder<Biome>> parameters;
   private final Optional<MultiNoiseBiomeSource.PresetInstance> preset;

   private MultiNoiseBiomeSource(Climate.ParameterList<Holder<Biome>> p_187057_) {
      this(p_187057_, Optional.empty());
   }

   MultiNoiseBiomeSource(Climate.ParameterList<Holder<Biome>> pParameters, Optional<MultiNoiseBiomeSource.PresetInstance> pPreset) {
      super(pParameters.values().stream().map(Pair::getSecond));
      this.preset = pPreset;
      this.parameters = pParameters;
   }

   protected Codec<? extends BiomeSource> codec() {
      return CODEC;
   }

   private Optional<MultiNoiseBiomeSource.PresetInstance> preset() {
      return this.preset;
   }

   public boolean stable(MultiNoiseBiomeSource.Preset pPreset) {
      return this.preset.isPresent() && Objects.equals(this.preset.get().preset(), pPreset);
   }

   public Holder<Biome> getNoiseBiome(int pX, int pY, int pZ, Climate.Sampler pSampler) {
      return this.getNoiseBiome(pSampler.sample(pX, pY, pZ));
   }

   @VisibleForDebug
   public Holder<Biome> getNoiseBiome(Climate.TargetPoint pTargetPoint) {
      return this.parameters.findValue(pTargetPoint);
   }

   public void addDebugInfo(List<String> pInfo, BlockPos pPos, Climate.Sampler pSampler) {
      int i = QuartPos.fromBlock(pPos.getX());
      int j = QuartPos.fromBlock(pPos.getY());
      int k = QuartPos.fromBlock(pPos.getZ());
      Climate.TargetPoint climate$targetpoint = pSampler.sample(i, j, k);
      float f = Climate.unquantizeCoord(climate$targetpoint.continentalness());
      float f1 = Climate.unquantizeCoord(climate$targetpoint.erosion());
      float f2 = Climate.unquantizeCoord(climate$targetpoint.temperature());
      float f3 = Climate.unquantizeCoord(climate$targetpoint.humidity());
      float f4 = Climate.unquantizeCoord(climate$targetpoint.weirdness());
      double d0 = (double)NoiseRouterData.peaksAndValleys(f4);
      OverworldBiomeBuilder overworldbiomebuilder = new OverworldBiomeBuilder();
      pInfo.add("Biome builder PV: " + OverworldBiomeBuilder.getDebugStringForPeaksAndValleys(d0) + " C: " + overworldbiomebuilder.getDebugStringForContinentalness((double)f) + " E: " + overworldbiomebuilder.getDebugStringForErosion((double)f1) + " T: " + overworldbiomebuilder.getDebugStringForTemperature((double)f2) + " H: " + overworldbiomebuilder.getDebugStringForHumidity((double)f3));
   }

   public static class Preset {
      static final Map<ResourceLocation, MultiNoiseBiomeSource.Preset> BY_NAME = Maps.newHashMap();
      public static final MultiNoiseBiomeSource.Preset NETHER = new MultiNoiseBiomeSource.Preset(new ResourceLocation("nether"), (p_204283_) -> {
         return new Climate.ParameterList<>(ImmutableList.of(Pair.of(Climate.parameters(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F), p_204283_.getOrCreateHolderOrThrow(Biomes.NETHER_WASTES)), Pair.of(Climate.parameters(0.0F, -0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F), p_204283_.getOrCreateHolderOrThrow(Biomes.SOUL_SAND_VALLEY)), Pair.of(Climate.parameters(0.4F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F), p_204283_.getOrCreateHolderOrThrow(Biomes.CRIMSON_FOREST)), Pair.of(Climate.parameters(0.0F, 0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.375F), p_204283_.getOrCreateHolderOrThrow(Biomes.WARPED_FOREST)), Pair.of(Climate.parameters(-0.5F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.175F), p_204283_.getOrCreateHolderOrThrow(Biomes.BASALT_DELTAS))));
      });
      public static final MultiNoiseBiomeSource.Preset OVERWORLD = new MultiNoiseBiomeSource.Preset(new ResourceLocation("overworld"), (p_204281_) -> {
         ImmutableList.Builder<Pair<Climate.ParameterPoint, Holder<Biome>>> builder = ImmutableList.builder();
         (new OverworldBiomeBuilder()).addBiomes((p_204279_) -> {
            builder.add(p_204279_.mapSecond(p_204281_::getOrCreateHolderOrThrow));
         });
         return new Climate.ParameterList<>(builder.build());
      });
      final ResourceLocation name;
      private final Function<Registry<Biome>, Climate.ParameterList<Holder<Biome>>> parameterSource;

      public Preset(ResourceLocation pName, Function<Registry<Biome>, Climate.ParameterList<Holder<Biome>>> pParameterSource) {
         this.name = pName;
         this.parameterSource = pParameterSource;
         BY_NAME.put(pName, this);
      }

      @VisibleForDebug
      public static Stream<Pair<ResourceLocation, MultiNoiseBiomeSource.Preset>> getPresets() {
         return BY_NAME.entrySet().stream().map((p_220661_) -> {
            return Pair.of(p_220661_.getKey(), p_220661_.getValue());
         });
      }

      MultiNoiseBiomeSource biomeSource(MultiNoiseBiomeSource.PresetInstance pPresetInstance, boolean pHasParams) {
         Climate.ParameterList<Holder<Biome>> parameterlist = this.parameterSource.apply(pPresetInstance.biomes());
         return new MultiNoiseBiomeSource(parameterlist, pHasParams ? Optional.of(pPresetInstance) : Optional.empty());
      }

      public MultiNoiseBiomeSource biomeSource(Registry<Biome> pBiomes, boolean pHasParams) {
         return this.biomeSource(new MultiNoiseBiomeSource.PresetInstance(this, pBiomes), pHasParams);
      }

      public MultiNoiseBiomeSource biomeSource(Registry<Biome> pRegistry) {
         return this.biomeSource(pRegistry, true);
      }

      public Stream<ResourceKey<Biome>> possibleBiomes() {
         return this.biomeSource(BuiltinRegistries.BIOME).possibleBiomes().stream().flatMap((p_220659_) -> {
            return p_220659_.unwrapKey().stream();
         });
      }
   }

   static record PresetInstance(MultiNoiseBiomeSource.Preset preset, Registry<Biome> biomes) {
      public static final MapCodec<MultiNoiseBiomeSource.PresetInstance> CODEC = RecordCodecBuilder.mapCodec((p_48558_) -> {
         return p_48558_.group(ResourceLocation.CODEC.flatXmap((p_151869_) -> {
            return Optional.ofNullable(MultiNoiseBiomeSource.Preset.BY_NAME.get(p_151869_)).map(DataResult::success).orElseGet(() -> {
               return DataResult.error("Unknown preset: " + p_151869_);
            });
         }, (p_151867_) -> {
            return DataResult.success(p_151867_.name);
         }).fieldOf("preset").stable().forGetter(MultiNoiseBiomeSource.PresetInstance::preset), RegistryOps.retrieveRegistry(Registry.BIOME_REGISTRY).forGetter(MultiNoiseBiomeSource.PresetInstance::biomes)).apply(p_48558_, p_48558_.stable(MultiNoiseBiomeSource.PresetInstance::new));
      });

      public MultiNoiseBiomeSource biomeSource() {
         return this.preset.biomeSource(this, true);
      }
   }
}