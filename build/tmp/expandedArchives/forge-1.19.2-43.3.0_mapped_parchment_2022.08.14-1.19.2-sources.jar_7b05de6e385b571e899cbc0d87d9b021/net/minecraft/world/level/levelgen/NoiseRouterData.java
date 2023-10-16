package net.minecraft.world.level.levelgen;

import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.data.worldgen.TerrainProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public class NoiseRouterData {
   public static final float GLOBAL_OFFSET = -0.50375F;
   private static final float ORE_THICKNESS = 0.08F;
   private static final double VEININESS_FREQUENCY = 1.5D;
   private static final double NOODLE_SPACING_AND_STRAIGHTNESS = 1.5D;
   private static final double SURFACE_DENSITY_THRESHOLD = 1.5625D;
   private static final double CHEESE_NOISE_TARGET = -0.703125D;
   public static final int ISLAND_CHUNK_DISTANCE = 64;
   public static final long ISLAND_CHUNK_DISTANCE_SQR = 4096L;
   private static final DensityFunction BLENDING_FACTOR = DensityFunctions.constant(10.0D);
   private static final DensityFunction BLENDING_JAGGEDNESS = DensityFunctions.zero();
   private static final ResourceKey<DensityFunction> ZERO = createKey("zero");
   private static final ResourceKey<DensityFunction> Y = createKey("y");
   private static final ResourceKey<DensityFunction> SHIFT_X = createKey("shift_x");
   private static final ResourceKey<DensityFunction> SHIFT_Z = createKey("shift_z");
   private static final ResourceKey<DensityFunction> BASE_3D_NOISE_OVERWORLD = createKey("overworld/base_3d_noise");
   private static final ResourceKey<DensityFunction> BASE_3D_NOISE_NETHER = createKey("nether/base_3d_noise");
   private static final ResourceKey<DensityFunction> BASE_3D_NOISE_END = createKey("end/base_3d_noise");
   public static final ResourceKey<DensityFunction> CONTINENTS = createKey("overworld/continents");
   public static final ResourceKey<DensityFunction> EROSION = createKey("overworld/erosion");
   public static final ResourceKey<DensityFunction> RIDGES = createKey("overworld/ridges");
   public static final ResourceKey<DensityFunction> RIDGES_FOLDED = createKey("overworld/ridges_folded");
   public static final ResourceKey<DensityFunction> OFFSET = createKey("overworld/offset");
   public static final ResourceKey<DensityFunction> FACTOR = createKey("overworld/factor");
   public static final ResourceKey<DensityFunction> JAGGEDNESS = createKey("overworld/jaggedness");
   public static final ResourceKey<DensityFunction> DEPTH = createKey("overworld/depth");
   private static final ResourceKey<DensityFunction> SLOPED_CHEESE = createKey("overworld/sloped_cheese");
   public static final ResourceKey<DensityFunction> CONTINENTS_LARGE = createKey("overworld_large_biomes/continents");
   public static final ResourceKey<DensityFunction> EROSION_LARGE = createKey("overworld_large_biomes/erosion");
   private static final ResourceKey<DensityFunction> OFFSET_LARGE = createKey("overworld_large_biomes/offset");
   private static final ResourceKey<DensityFunction> FACTOR_LARGE = createKey("overworld_large_biomes/factor");
   private static final ResourceKey<DensityFunction> JAGGEDNESS_LARGE = createKey("overworld_large_biomes/jaggedness");
   private static final ResourceKey<DensityFunction> DEPTH_LARGE = createKey("overworld_large_biomes/depth");
   private static final ResourceKey<DensityFunction> SLOPED_CHEESE_LARGE = createKey("overworld_large_biomes/sloped_cheese");
   private static final ResourceKey<DensityFunction> OFFSET_AMPLIFIED = createKey("overworld_amplified/offset");
   private static final ResourceKey<DensityFunction> FACTOR_AMPLIFIED = createKey("overworld_amplified/factor");
   private static final ResourceKey<DensityFunction> JAGGEDNESS_AMPLIFIED = createKey("overworld_amplified/jaggedness");
   private static final ResourceKey<DensityFunction> DEPTH_AMPLIFIED = createKey("overworld_amplified/depth");
   private static final ResourceKey<DensityFunction> SLOPED_CHEESE_AMPLIFIED = createKey("overworld_amplified/sloped_cheese");
   private static final ResourceKey<DensityFunction> SLOPED_CHEESE_END = createKey("end/sloped_cheese");
   private static final ResourceKey<DensityFunction> SPAGHETTI_ROUGHNESS_FUNCTION = createKey("overworld/caves/spaghetti_roughness_function");
   private static final ResourceKey<DensityFunction> ENTRANCES = createKey("overworld/caves/entrances");
   private static final ResourceKey<DensityFunction> NOODLE = createKey("overworld/caves/noodle");
   private static final ResourceKey<DensityFunction> PILLARS = createKey("overworld/caves/pillars");
   private static final ResourceKey<DensityFunction> SPAGHETTI_2D_THICKNESS_MODULATOR = createKey("overworld/caves/spaghetti_2d_thickness_modulator");
   private static final ResourceKey<DensityFunction> SPAGHETTI_2D = createKey("overworld/caves/spaghetti_2d");

   private static ResourceKey<DensityFunction> createKey(String pLocation) {
      return ResourceKey.create(Registry.DENSITY_FUNCTION_REGISTRY, new ResourceLocation(pLocation));
   }

   public static Holder<? extends DensityFunction> bootstrap(Registry<DensityFunction> pRegistry) {
      register(pRegistry, ZERO, DensityFunctions.zero());
      int i = DimensionType.MIN_Y * 2;
      int j = DimensionType.MAX_Y * 2;
      register(pRegistry, Y, DensityFunctions.yClampedGradient(i, j, (double)i, (double)j));
      DensityFunction densityfunction = registerAndWrap(pRegistry, SHIFT_X, DensityFunctions.flatCache(DensityFunctions.cache2d(DensityFunctions.shiftA(getNoise(Noises.SHIFT)))));
      DensityFunction densityfunction1 = registerAndWrap(pRegistry, SHIFT_Z, DensityFunctions.flatCache(DensityFunctions.cache2d(DensityFunctions.shiftB(getNoise(Noises.SHIFT)))));
      register(pRegistry, BASE_3D_NOISE_OVERWORLD, BlendedNoise.createUnseeded(0.25D, 0.125D, 80.0D, 160.0D, 8.0D));
      register(pRegistry, BASE_3D_NOISE_NETHER, BlendedNoise.createUnseeded(0.25D, 0.375D, 80.0D, 60.0D, 8.0D));
      register(pRegistry, BASE_3D_NOISE_END, BlendedNoise.createUnseeded(0.25D, 0.25D, 80.0D, 160.0D, 4.0D));
      Holder<DensityFunction> holder = register(pRegistry, CONTINENTS, DensityFunctions.flatCache(DensityFunctions.shiftedNoise2d(densityfunction, densityfunction1, 0.25D, getNoise(Noises.CONTINENTALNESS))));
      Holder<DensityFunction> holder1 = register(pRegistry, EROSION, DensityFunctions.flatCache(DensityFunctions.shiftedNoise2d(densityfunction, densityfunction1, 0.25D, getNoise(Noises.EROSION))));
      DensityFunction densityfunction2 = registerAndWrap(pRegistry, RIDGES, DensityFunctions.flatCache(DensityFunctions.shiftedNoise2d(densityfunction, densityfunction1, 0.25D, getNoise(Noises.RIDGE))));
      register(pRegistry, RIDGES_FOLDED, peaksAndValleys(densityfunction2));
      DensityFunction densityfunction3 = DensityFunctions.noise(getNoise(Noises.JAGGED), 1500.0D, 0.0D);
      registerTerrainNoises(pRegistry, densityfunction3, holder, holder1, OFFSET, FACTOR, JAGGEDNESS, DEPTH, SLOPED_CHEESE, false);
      Holder<DensityFunction> holder2 = register(pRegistry, CONTINENTS_LARGE, DensityFunctions.flatCache(DensityFunctions.shiftedNoise2d(densityfunction, densityfunction1, 0.25D, getNoise(Noises.CONTINENTALNESS_LARGE))));
      Holder<DensityFunction> holder3 = register(pRegistry, EROSION_LARGE, DensityFunctions.flatCache(DensityFunctions.shiftedNoise2d(densityfunction, densityfunction1, 0.25D, getNoise(Noises.EROSION_LARGE))));
      registerTerrainNoises(pRegistry, densityfunction3, holder2, holder3, OFFSET_LARGE, FACTOR_LARGE, JAGGEDNESS_LARGE, DEPTH_LARGE, SLOPED_CHEESE_LARGE, false);
      registerTerrainNoises(pRegistry, densityfunction3, holder, holder1, OFFSET_AMPLIFIED, FACTOR_AMPLIFIED, JAGGEDNESS_AMPLIFIED, DEPTH_AMPLIFIED, SLOPED_CHEESE_AMPLIFIED, true);
      register(pRegistry, SLOPED_CHEESE_END, DensityFunctions.add(DensityFunctions.endIslands(0L), getFunction(pRegistry, BASE_3D_NOISE_END)));
      register(pRegistry, SPAGHETTI_ROUGHNESS_FUNCTION, spaghettiRoughnessFunction());
      register(pRegistry, SPAGHETTI_2D_THICKNESS_MODULATOR, DensityFunctions.cacheOnce(DensityFunctions.mappedNoise(getNoise(Noises.SPAGHETTI_2D_THICKNESS), 2.0D, 1.0D, -0.6D, -1.3D)));
      register(pRegistry, SPAGHETTI_2D, spaghetti2D(pRegistry));
      register(pRegistry, ENTRANCES, entrances(pRegistry));
      register(pRegistry, NOODLE, noodle(pRegistry));
      return register(pRegistry, PILLARS, pillars());
   }

   private static void registerTerrainNoises(Registry<DensityFunction> p_224475_, DensityFunction p_224476_, Holder<DensityFunction> p_224477_, Holder<DensityFunction> p_224478_, ResourceKey<DensityFunction> p_224479_, ResourceKey<DensityFunction> p_224480_, ResourceKey<DensityFunction> p_224481_, ResourceKey<DensityFunction> p_224482_, ResourceKey<DensityFunction> p_224483_, boolean p_224484_) {
      DensityFunctions.Spline.Coordinate densityfunctions$spline$coordinate = new DensityFunctions.Spline.Coordinate(p_224477_);
      DensityFunctions.Spline.Coordinate densityfunctions$spline$coordinate1 = new DensityFunctions.Spline.Coordinate(p_224478_);
      DensityFunctions.Spline.Coordinate densityfunctions$spline$coordinate2 = new DensityFunctions.Spline.Coordinate(p_224475_.getHolderOrThrow(RIDGES));
      DensityFunctions.Spline.Coordinate densityfunctions$spline$coordinate3 = new DensityFunctions.Spline.Coordinate(p_224475_.getHolderOrThrow(RIDGES_FOLDED));
      DensityFunction densityfunction = registerAndWrap(p_224475_, p_224479_, splineWithBlending(DensityFunctions.add(DensityFunctions.constant((double)-0.50375F), DensityFunctions.spline(TerrainProvider.overworldOffset(densityfunctions$spline$coordinate, densityfunctions$spline$coordinate1, densityfunctions$spline$coordinate3, p_224484_))), DensityFunctions.blendOffset()));
      DensityFunction densityfunction1 = registerAndWrap(p_224475_, p_224480_, splineWithBlending(DensityFunctions.spline(TerrainProvider.overworldFactor(densityfunctions$spline$coordinate, densityfunctions$spline$coordinate1, densityfunctions$spline$coordinate2, densityfunctions$spline$coordinate3, p_224484_)), BLENDING_FACTOR));
      DensityFunction densityfunction2 = registerAndWrap(p_224475_, p_224482_, DensityFunctions.add(DensityFunctions.yClampedGradient(-64, 320, 1.5D, -1.5D), densityfunction));
      DensityFunction densityfunction3 = registerAndWrap(p_224475_, p_224481_, splineWithBlending(DensityFunctions.spline(TerrainProvider.overworldJaggedness(densityfunctions$spline$coordinate, densityfunctions$spline$coordinate1, densityfunctions$spline$coordinate2, densityfunctions$spline$coordinate3, p_224484_)), BLENDING_JAGGEDNESS));
      DensityFunction densityfunction4 = DensityFunctions.mul(densityfunction3, p_224476_.halfNegative());
      DensityFunction densityfunction5 = noiseGradientDensity(densityfunction1, DensityFunctions.add(densityfunction2, densityfunction4));
      register(p_224475_, p_224483_, DensityFunctions.add(densityfunction5, getFunction(p_224475_, BASE_3D_NOISE_OVERWORLD)));
   }

   private static DensityFunction registerAndWrap(Registry<DensityFunction> pRegistry, ResourceKey<DensityFunction> pKey, DensityFunction pDensityFunction) {
      return new DensityFunctions.HolderHolder(BuiltinRegistries.register(pRegistry, pKey, pDensityFunction));
   }

   private static Holder<DensityFunction> register(Registry<DensityFunction> pRegistry, ResourceKey<DensityFunction> pKey, DensityFunction pDensityFunction) {
      return BuiltinRegistries.register(pRegistry, pKey, pDensityFunction);
   }

   private static Holder<NormalNoise.NoiseParameters> getNoise(ResourceKey<NormalNoise.NoiseParameters> p_209543_) {
      return BuiltinRegistries.NOISE.getHolderOrThrow(p_209543_);
   }

   private static DensityFunction getFunction(Registry<DensityFunction> p_224465_, ResourceKey<DensityFunction> p_224466_) {
      return new DensityFunctions.HolderHolder(p_224465_.getHolderOrThrow(p_224466_));
   }

   private static DensityFunction peaksAndValleys(DensityFunction p_224438_) {
      return DensityFunctions.mul(DensityFunctions.add(DensityFunctions.add(p_224438_.abs(), DensityFunctions.constant(-0.6666666666666666D)).abs(), DensityFunctions.constant(-0.3333333333333333D)), DensityFunctions.constant(-3.0D));
   }

   public static float peaksAndValleys(float p_224436_) {
      return -(Math.abs(Math.abs(p_224436_) - 0.6666667F) - 0.33333334F) * 3.0F;
   }

   private static DensityFunction spaghettiRoughnessFunction() {
      DensityFunction densityfunction = DensityFunctions.noise(getNoise(Noises.SPAGHETTI_ROUGHNESS));
      DensityFunction densityfunction1 = DensityFunctions.mappedNoise(getNoise(Noises.SPAGHETTI_ROUGHNESS_MODULATOR), 0.0D, -0.1D);
      return DensityFunctions.cacheOnce(DensityFunctions.mul(densityfunction1, DensityFunctions.add(densityfunction.abs(), DensityFunctions.constant(-0.4D))));
   }

   private static DensityFunction entrances(Registry<DensityFunction> p_224514_) {
      DensityFunction densityfunction = DensityFunctions.cacheOnce(DensityFunctions.noise(getNoise(Noises.SPAGHETTI_3D_RARITY), 2.0D, 1.0D));
      DensityFunction densityfunction1 = DensityFunctions.mappedNoise(getNoise(Noises.SPAGHETTI_3D_THICKNESS), -0.065D, -0.088D);
      DensityFunction densityfunction2 = DensityFunctions.weirdScaledSampler(densityfunction, getNoise(Noises.SPAGHETTI_3D_1), DensityFunctions.WeirdScaledSampler.RarityValueMapper.TYPE1);
      DensityFunction densityfunction3 = DensityFunctions.weirdScaledSampler(densityfunction, getNoise(Noises.SPAGHETTI_3D_2), DensityFunctions.WeirdScaledSampler.RarityValueMapper.TYPE1);
      DensityFunction densityfunction4 = DensityFunctions.add(DensityFunctions.max(densityfunction2, densityfunction3), densityfunction1).clamp(-1.0D, 1.0D);
      DensityFunction densityfunction5 = getFunction(p_224514_, SPAGHETTI_ROUGHNESS_FUNCTION);
      DensityFunction densityfunction6 = DensityFunctions.noise(getNoise(Noises.CAVE_ENTRANCE), 0.75D, 0.5D);
      DensityFunction densityfunction7 = DensityFunctions.add(DensityFunctions.add(densityfunction6, DensityFunctions.constant(0.37D)), DensityFunctions.yClampedGradient(-10, 30, 0.3D, 0.0D));
      return DensityFunctions.cacheOnce(DensityFunctions.min(densityfunction7, DensityFunctions.add(densityfunction5, densityfunction4)));
   }

   private static DensityFunction noodle(Registry<DensityFunction> p_224516_) {
      DensityFunction densityfunction = getFunction(p_224516_, Y);
      int i = -64;
      int j = -60;
      int k = 320;
      DensityFunction densityfunction1 = yLimitedInterpolatable(densityfunction, DensityFunctions.noise(getNoise(Noises.NOODLE), 1.0D, 1.0D), -60, 320, -1);
      DensityFunction densityfunction2 = yLimitedInterpolatable(densityfunction, DensityFunctions.mappedNoise(getNoise(Noises.NOODLE_THICKNESS), 1.0D, 1.0D, -0.05D, -0.1D), -60, 320, 0);
      double d0 = 2.6666666666666665D;
      DensityFunction densityfunction3 = yLimitedInterpolatable(densityfunction, DensityFunctions.noise(getNoise(Noises.NOODLE_RIDGE_A), 2.6666666666666665D, 2.6666666666666665D), -60, 320, 0);
      DensityFunction densityfunction4 = yLimitedInterpolatable(densityfunction, DensityFunctions.noise(getNoise(Noises.NOODLE_RIDGE_B), 2.6666666666666665D, 2.6666666666666665D), -60, 320, 0);
      DensityFunction densityfunction5 = DensityFunctions.mul(DensityFunctions.constant(1.5D), DensityFunctions.max(densityfunction3.abs(), densityfunction4.abs()));
      return DensityFunctions.rangeChoice(densityfunction1, -1000000.0D, 0.0D, DensityFunctions.constant(64.0D), DensityFunctions.add(densityfunction2, densityfunction5));
   }

   private static DensityFunction pillars() {
      double d0 = 25.0D;
      double d1 = 0.3D;
      DensityFunction densityfunction = DensityFunctions.noise(getNoise(Noises.PILLAR), 25.0D, 0.3D);
      DensityFunction densityfunction1 = DensityFunctions.mappedNoise(getNoise(Noises.PILLAR_RARENESS), 0.0D, -2.0D);
      DensityFunction densityfunction2 = DensityFunctions.mappedNoise(getNoise(Noises.PILLAR_THICKNESS), 0.0D, 1.1D);
      DensityFunction densityfunction3 = DensityFunctions.add(DensityFunctions.mul(densityfunction, DensityFunctions.constant(2.0D)), densityfunction1);
      return DensityFunctions.cacheOnce(DensityFunctions.mul(densityfunction3, densityfunction2.cube()));
   }

   private static DensityFunction spaghetti2D(Registry<DensityFunction> p_224518_) {
      DensityFunction densityfunction = DensityFunctions.noise(getNoise(Noises.SPAGHETTI_2D_MODULATOR), 2.0D, 1.0D);
      DensityFunction densityfunction1 = DensityFunctions.weirdScaledSampler(densityfunction, getNoise(Noises.SPAGHETTI_2D), DensityFunctions.WeirdScaledSampler.RarityValueMapper.TYPE2);
      DensityFunction densityfunction2 = DensityFunctions.mappedNoise(getNoise(Noises.SPAGHETTI_2D_ELEVATION), 0.0D, (double)Math.floorDiv(-64, 8), 8.0D);
      DensityFunction densityfunction3 = getFunction(p_224518_, SPAGHETTI_2D_THICKNESS_MODULATOR);
      DensityFunction densityfunction4 = DensityFunctions.add(densityfunction2, DensityFunctions.yClampedGradient(-64, 320, 8.0D, -40.0D)).abs();
      DensityFunction densityfunction5 = DensityFunctions.add(densityfunction4, densityfunction3).cube();
      double d0 = 0.083D;
      DensityFunction densityfunction6 = DensityFunctions.add(densityfunction1, DensityFunctions.mul(DensityFunctions.constant(0.083D), densityfunction3));
      return DensityFunctions.max(densityfunction6, densityfunction5).clamp(-1.0D, 1.0D);
   }

   private static DensityFunction underground(Registry<DensityFunction> p_224472_, DensityFunction p_224473_) {
      DensityFunction densityfunction = getFunction(p_224472_, SPAGHETTI_2D);
      DensityFunction densityfunction1 = getFunction(p_224472_, SPAGHETTI_ROUGHNESS_FUNCTION);
      DensityFunction densityfunction2 = DensityFunctions.noise(getNoise(Noises.CAVE_LAYER), 8.0D);
      DensityFunction densityfunction3 = DensityFunctions.mul(DensityFunctions.constant(4.0D), densityfunction2.square());
      DensityFunction densityfunction4 = DensityFunctions.noise(getNoise(Noises.CAVE_CHEESE), 0.6666666666666666D);
      DensityFunction densityfunction5 = DensityFunctions.add(DensityFunctions.add(DensityFunctions.constant(0.27D), densityfunction4).clamp(-1.0D, 1.0D), DensityFunctions.add(DensityFunctions.constant(1.5D), DensityFunctions.mul(DensityFunctions.constant(-0.64D), p_224473_)).clamp(0.0D, 0.5D));
      DensityFunction densityfunction6 = DensityFunctions.add(densityfunction3, densityfunction5);
      DensityFunction densityfunction7 = DensityFunctions.min(DensityFunctions.min(densityfunction6, getFunction(p_224472_, ENTRANCES)), DensityFunctions.add(densityfunction, densityfunction1));
      DensityFunction densityfunction8 = getFunction(p_224472_, PILLARS);
      DensityFunction densityfunction9 = DensityFunctions.rangeChoice(densityfunction8, -1000000.0D, 0.03D, DensityFunctions.constant(-1000000.0D), densityfunction8);
      return DensityFunctions.max(densityfunction7, densityfunction9);
   }

   private static DensityFunction postProcess(DensityFunction p_224493_) {
      DensityFunction densityfunction = DensityFunctions.blendDensity(p_224493_);
      return DensityFunctions.mul(DensityFunctions.interpolated(densityfunction), DensityFunctions.constant(0.64D)).squeeze();
   }

   protected static NoiseRouter overworld(Registry<DensityFunction> p_224486_, boolean p_224487_, boolean p_224488_) {
      DensityFunction densityfunction = DensityFunctions.noise(getNoise(Noises.AQUIFER_BARRIER), 0.5D);
      DensityFunction densityfunction1 = DensityFunctions.noise(getNoise(Noises.AQUIFER_FLUID_LEVEL_FLOODEDNESS), 0.67D);
      DensityFunction densityfunction2 = DensityFunctions.noise(getNoise(Noises.AQUIFER_FLUID_LEVEL_SPREAD), 0.7142857142857143D);
      DensityFunction densityfunction3 = DensityFunctions.noise(getNoise(Noises.AQUIFER_LAVA));
      DensityFunction densityfunction4 = getFunction(p_224486_, SHIFT_X);
      DensityFunction densityfunction5 = getFunction(p_224486_, SHIFT_Z);
      DensityFunction densityfunction6 = DensityFunctions.shiftedNoise2d(densityfunction4, densityfunction5, 0.25D, getNoise(p_224487_ ? Noises.TEMPERATURE_LARGE : Noises.TEMPERATURE));
      DensityFunction densityfunction7 = DensityFunctions.shiftedNoise2d(densityfunction4, densityfunction5, 0.25D, getNoise(p_224487_ ? Noises.VEGETATION_LARGE : Noises.VEGETATION));
      DensityFunction densityfunction8 = getFunction(p_224486_, p_224487_ ? FACTOR_LARGE : (p_224488_ ? FACTOR_AMPLIFIED : FACTOR));
      DensityFunction densityfunction9 = getFunction(p_224486_, p_224487_ ? DEPTH_LARGE : (p_224488_ ? DEPTH_AMPLIFIED : DEPTH));
      DensityFunction densityfunction10 = noiseGradientDensity(DensityFunctions.cache2d(densityfunction8), densityfunction9);
      DensityFunction densityfunction11 = getFunction(p_224486_, p_224487_ ? SLOPED_CHEESE_LARGE : (p_224488_ ? SLOPED_CHEESE_AMPLIFIED : SLOPED_CHEESE));
      DensityFunction densityfunction12 = DensityFunctions.min(densityfunction11, DensityFunctions.mul(DensityFunctions.constant(5.0D), getFunction(p_224486_, ENTRANCES)));
      DensityFunction densityfunction13 = DensityFunctions.rangeChoice(densityfunction11, -1000000.0D, 1.5625D, densityfunction12, underground(p_224486_, densityfunction11));
      DensityFunction densityfunction14 = DensityFunctions.min(postProcess(slideOverworld(p_224488_, densityfunction13)), getFunction(p_224486_, NOODLE));
      DensityFunction densityfunction15 = getFunction(p_224486_, Y);
      int i = Stream.of(OreVeinifier.VeinType.values()).mapToInt((p_224495_) -> {
         return p_224495_.minY;
      }).min().orElse(-DimensionType.MIN_Y * 2);
      int j = Stream.of(OreVeinifier.VeinType.values()).mapToInt((p_224457_) -> {
         return p_224457_.maxY;
      }).max().orElse(-DimensionType.MIN_Y * 2);
      DensityFunction densityfunction16 = yLimitedInterpolatable(densityfunction15, DensityFunctions.noise(getNoise(Noises.ORE_VEININESS), 1.5D, 1.5D), i, j, 0);
      float f = 4.0F;
      DensityFunction densityfunction17 = yLimitedInterpolatable(densityfunction15, DensityFunctions.noise(getNoise(Noises.ORE_VEIN_A), 4.0D, 4.0D), i, j, 0).abs();
      DensityFunction densityfunction18 = yLimitedInterpolatable(densityfunction15, DensityFunctions.noise(getNoise(Noises.ORE_VEIN_B), 4.0D, 4.0D), i, j, 0).abs();
      DensityFunction densityfunction19 = DensityFunctions.add(DensityFunctions.constant((double)-0.08F), DensityFunctions.max(densityfunction17, densityfunction18));
      DensityFunction densityfunction20 = DensityFunctions.noise(getNoise(Noises.ORE_GAP));
      return new NoiseRouter(densityfunction, densityfunction1, densityfunction2, densityfunction3, densityfunction6, densityfunction7, getFunction(p_224486_, p_224487_ ? CONTINENTS_LARGE : CONTINENTS), getFunction(p_224486_, p_224487_ ? EROSION_LARGE : EROSION), densityfunction9, getFunction(p_224486_, RIDGES), slideOverworld(p_224488_, DensityFunctions.add(densityfunction10, DensityFunctions.constant(-0.703125D)).clamp(-64.0D, 64.0D)), densityfunction14, densityfunction16, densityfunction19, densityfunction20);
   }

   private static NoiseRouter noNewCaves(Registry<DensityFunction> p_224503_, DensityFunction p_224504_) {
      DensityFunction densityfunction = getFunction(p_224503_, SHIFT_X);
      DensityFunction densityfunction1 = getFunction(p_224503_, SHIFT_Z);
      DensityFunction densityfunction2 = DensityFunctions.shiftedNoise2d(densityfunction, densityfunction1, 0.25D, getNoise(Noises.TEMPERATURE));
      DensityFunction densityfunction3 = DensityFunctions.shiftedNoise2d(densityfunction, densityfunction1, 0.25D, getNoise(Noises.VEGETATION));
      DensityFunction densityfunction4 = postProcess(p_224504_);
      return new NoiseRouter(DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), densityfunction2, densityfunction3, DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), densityfunction4, DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero());
   }

   private static DensityFunction slideOverworld(boolean p_224490_, DensityFunction p_224491_) {
      return slide(p_224491_, -64, 384, p_224490_ ? 16 : 80, p_224490_ ? 0 : 64, -0.078125D, 0, 24, p_224490_ ? 0.4D : 0.1171875D);
   }

   private static DensityFunction slideNetherLike(Registry<DensityFunction> p_224461_, int p_224462_, int p_224463_) {
      return slide(getFunction(p_224461_, BASE_3D_NOISE_NETHER), p_224462_, p_224463_, 24, 0, 0.9375D, -8, 24, 2.5D);
   }

   private static DensityFunction slideEndLike(DensityFunction p_224440_, int p_224441_, int p_224442_) {
      return slide(p_224440_, p_224441_, p_224442_, 72, -184, -23.4375D, 4, 32, -0.234375D);
   }

   protected static NoiseRouter nether(Registry<DensityFunction> p_224497_) {
      return noNewCaves(p_224497_, slideNetherLike(p_224497_, 0, 128));
   }

   protected static NoiseRouter caves(Registry<DensityFunction> p_224508_) {
      return noNewCaves(p_224508_, slideNetherLike(p_224508_, -64, 192));
   }

   protected static NoiseRouter floatingIslands(Registry<DensityFunction> p_224510_) {
      return noNewCaves(p_224510_, slideEndLike(getFunction(p_224510_, BASE_3D_NOISE_END), 0, 256));
   }

   private static DensityFunction slideEnd(DensityFunction p_224506_) {
      return slideEndLike(p_224506_, 0, 128);
   }

   protected static NoiseRouter end(Registry<DensityFunction> p_224512_) {
      DensityFunction densityfunction = DensityFunctions.cache2d(DensityFunctions.endIslands(0L));
      DensityFunction densityfunction1 = postProcess(slideEnd(getFunction(p_224512_, SLOPED_CHEESE_END)));
      return new NoiseRouter(DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), densityfunction, DensityFunctions.zero(), DensityFunctions.zero(), slideEnd(DensityFunctions.add(densityfunction, DensityFunctions.constant(-0.703125D))), densityfunction1, DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero());
   }

   protected static NoiseRouter none() {
      return new NoiseRouter(DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero(), DensityFunctions.zero());
   }

   private static DensityFunction splineWithBlending(DensityFunction p_224454_, DensityFunction p_224455_) {
      DensityFunction densityfunction = DensityFunctions.lerp(DensityFunctions.blendAlpha(), p_224455_, p_224454_);
      return DensityFunctions.flatCache(DensityFunctions.cache2d(densityfunction));
   }

   private static DensityFunction noiseGradientDensity(DensityFunction p_212272_, DensityFunction p_212273_) {
      DensityFunction densityfunction = DensityFunctions.mul(p_212273_, p_212272_);
      return DensityFunctions.mul(DensityFunctions.constant(4.0D), densityfunction.quarterNegative());
   }

   private static DensityFunction yLimitedInterpolatable(DensityFunction p_209472_, DensityFunction p_209473_, int p_209474_, int p_209475_, int p_209476_) {
      return DensityFunctions.interpolated(DensityFunctions.rangeChoice(p_209472_, (double)p_209474_, (double)(p_209475_ + 1), p_209473_, DensityFunctions.constant((double)p_209476_)));
   }

   private static DensityFunction slide(DensityFunction p_224444_, int p_224445_, int p_224446_, int p_224447_, int p_224448_, double p_224449_, int p_224450_, int p_224451_, double p_224452_) {
      DensityFunction densityfunction1 = DensityFunctions.yClampedGradient(p_224445_ + p_224446_ - p_224447_, p_224445_ + p_224446_ - p_224448_, 1.0D, 0.0D);
      DensityFunction $$9 = DensityFunctions.lerp(densityfunction1, p_224449_, p_224444_);
      DensityFunction densityfunction2 = DensityFunctions.yClampedGradient(p_224445_ + p_224450_, p_224445_ + p_224451_, 0.0D, 1.0D);
      return DensityFunctions.lerp(densityfunction2, p_224452_, $$9);
   }

   protected static final class QuantizedSpaghettiRarity {
      protected static double getSphaghettiRarity2D(double p_209564_) {
         if (p_209564_ < -0.75D) {
            return 0.5D;
         } else if (p_209564_ < -0.5D) {
            return 0.75D;
         } else if (p_209564_ < 0.5D) {
            return 1.0D;
         } else {
            return p_209564_ < 0.75D ? 2.0D : 3.0D;
         }
      }

      protected static double getSpaghettiRarity3D(double p_209566_) {
         if (p_209566_ < -0.5D) {
            return 0.75D;
         } else if (p_209566_ < 0.0D) {
            return 1.0D;
         } else {
            return p_209566_ < 0.5D ? 1.5D : 2.0D;
         }
      }
   }
}