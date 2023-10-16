package net.minecraft.world.level.levelgen;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public final class RandomState {
   final PositionalRandomFactory random;
   private final long legacyLevelSeed;
   private final Registry<NormalNoise.NoiseParameters> noises;
   private final NoiseRouter router;
   private final Climate.Sampler sampler;
   private final SurfaceSystem surfaceSystem;
   private final PositionalRandomFactory aquiferRandom;
   private final PositionalRandomFactory oreRandom;
   private final Map<ResourceKey<NormalNoise.NoiseParameters>, NormalNoise> noiseIntances;
   private final Map<ResourceLocation, PositionalRandomFactory> positionalRandoms;

   public static RandomState create(RegistryAccess p_224575_, ResourceKey<NoiseGeneratorSettings> p_224576_, long p_224577_) {
      return create(p_224575_.registryOrThrow(Registry.NOISE_GENERATOR_SETTINGS_REGISTRY).getOrThrow(p_224576_), p_224575_.registryOrThrow(Registry.NOISE_REGISTRY), p_224577_);
   }

   public static RandomState create(NoiseGeneratorSettings p_224571_, Registry<NormalNoise.NoiseParameters> p_224572_, long p_224573_) {
      return new RandomState(p_224571_, p_224572_, p_224573_);
   }

   private RandomState(NoiseGeneratorSettings p_224556_, Registry<NormalNoise.NoiseParameters> p_224557_, final long p_224558_) {
      this.random = p_224556_.getRandomSource().newInstance(p_224558_).forkPositional();
      this.legacyLevelSeed = p_224558_;
      this.noises = p_224557_;
      this.aquiferRandom = this.random.fromHashOf(new ResourceLocation("aquifer")).forkPositional();
      this.oreRandom = this.random.fromHashOf(new ResourceLocation("ore")).forkPositional();
      this.noiseIntances = new ConcurrentHashMap<>();
      this.positionalRandoms = new ConcurrentHashMap<>();
      this.surfaceSystem = new SurfaceSystem(this, p_224556_.defaultBlock(), p_224556_.seaLevel(), this.random);
      final boolean flag = p_224556_.useLegacyRandomSource();

      class NoiseWiringHelper implements DensityFunction.Visitor {
         private final Map<DensityFunction, DensityFunction> wrapped = new HashMap<>();

         private RandomSource newLegacyInstance(long p_224592_) {
            return new LegacyRandomSource(p_224558_ + p_224592_);
         }

         public DensityFunction.NoiseHolder visitNoise(DensityFunction.NoiseHolder p_224594_) {
            Holder<NormalNoise.NoiseParameters> holder = p_224594_.noiseData();
            if (flag) {
               if (Objects.equals(holder.unwrapKey(), Optional.of(Noises.TEMPERATURE))) {
                  NormalNoise normalnoise3 = NormalNoise.createLegacyNetherBiome(this.newLegacyInstance(0L), new NormalNoise.NoiseParameters(-7, 1.0D, 1.0D));
                  return new DensityFunction.NoiseHolder(holder, normalnoise3);
               }

               if (Objects.equals(holder.unwrapKey(), Optional.of(Noises.VEGETATION))) {
                  NormalNoise normalnoise2 = NormalNoise.createLegacyNetherBiome(this.newLegacyInstance(1L), new NormalNoise.NoiseParameters(-7, 1.0D, 1.0D));
                  return new DensityFunction.NoiseHolder(holder, normalnoise2);
               }

               if (Objects.equals(holder.unwrapKey(), Optional.of(Noises.SHIFT))) {
                  NormalNoise normalnoise1 = NormalNoise.create(RandomState.this.random.fromHashOf(Noises.SHIFT.location()), new NormalNoise.NoiseParameters(0, 0.0D));
                  return new DensityFunction.NoiseHolder(holder, normalnoise1);
               }
            }

            NormalNoise normalnoise = RandomState.this.getOrCreateNoise(holder.unwrapKey().orElseThrow());
            return new DensityFunction.NoiseHolder(holder, normalnoise);
         }

         private DensityFunction wrapNew(DensityFunction p_224596_) {
            if (p_224596_ instanceof BlendedNoise blendednoise) {
               RandomSource randomsource = flag ? this.newLegacyInstance(0L) : RandomState.this.random.fromHashOf(new ResourceLocation("terrain"));
               return blendednoise.withNewRandom(randomsource);
            } else {
               return (DensityFunction)(p_224596_ instanceof DensityFunctions.EndIslandDensityFunction ? new DensityFunctions.EndIslandDensityFunction(p_224558_) : p_224596_);
            }
         }

         public DensityFunction apply(DensityFunction p_224598_) {
            return this.wrapped.computeIfAbsent(p_224598_, this::wrapNew);
         }
      }

      this.router = p_224556_.noiseRouter().mapAll(new NoiseWiringHelper());
      this.sampler = new Climate.Sampler(this.router.temperature(), this.router.vegetation(), this.router.continents(), this.router.erosion(), this.router.depth(), this.router.ridges(), p_224556_.spawnTarget());
   }

   public NormalNoise getOrCreateNoise(ResourceKey<NormalNoise.NoiseParameters> p_224561_) {
      return this.noiseIntances.computeIfAbsent(p_224561_, (p_224564_) -> {
         return Noises.instantiate(this.noises, this.random, p_224561_);
      });
   }

   public PositionalRandomFactory getOrCreateRandomFactory(ResourceLocation p_224566_) {
      return this.positionalRandoms.computeIfAbsent(p_224566_, (p_224569_) -> {
         return this.random.fromHashOf(p_224566_).forkPositional();
      });
   }

   public long legacyLevelSeed() {
      return this.legacyLevelSeed;
   }

   public NoiseRouter router() {
      return this.router;
   }

   public Climate.Sampler sampler() {
      return this.sampler;
   }

   public SurfaceSystem surfaceSystem() {
      return this.surfaceSystem;
   }

   public PositionalRandomFactory aquiferRandom() {
      return this.aquiferRandom;
   }

   public PositionalRandomFactory oreRandom() {
      return this.oreRandom;
   }
}