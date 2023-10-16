package net.minecraft.data.worldgen.features;

import java.util.List;
import net.minecraft.Util;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class FeatureUtils {
   public static Holder<? extends ConfiguredFeature<?, ?>> bootstrap(Registry<ConfiguredFeature<?, ?>> p_236678_) {
      List<Holder<? extends ConfiguredFeature<?, ?>>> list = List.of(AquaticFeatures.KELP, CaveFeatures.MOSS_PATCH_BONEMEAL, EndFeatures.CHORUS_PLANT, MiscOverworldFeatures.SPRING_LAVA_OVERWORLD, NetherFeatures.BASALT_BLOBS, OreFeatures.ORE_ANCIENT_DEBRIS_LARGE, PileFeatures.PILE_HAY, TreeFeatures.AZALEA_TREE, VegetationFeatures.TREES_OLD_GROWTH_PINE_TAIGA);
      return Util.getRandom(list, RandomSource.create());
   }

   private static BlockPredicate simplePatchPredicate(List<Block> pBlocks) {
      BlockPredicate blockpredicate;
      if (!pBlocks.isEmpty()) {
         blockpredicate = BlockPredicate.allOf(BlockPredicate.ONLY_IN_AIR_PREDICATE, BlockPredicate.matchesBlocks(Direction.DOWN.getNormal(), pBlocks));
      } else {
         blockpredicate = BlockPredicate.ONLY_IN_AIR_PREDICATE;
      }

      return blockpredicate;
   }

   public static RandomPatchConfiguration simpleRandomPatchConfiguration(int pTries, Holder<PlacedFeature> pFeature) {
      return new RandomPatchConfiguration(pTries, 7, 3, pFeature);
   }

   public static <FC extends FeatureConfiguration, F extends Feature<FC>> RandomPatchConfiguration simplePatchConfiguration(F pFeature, FC pConfig, List<Block> p_206483_, int pTries) {
      return simpleRandomPatchConfiguration(pTries, PlacementUtils.filtered(pFeature, pConfig, simplePatchPredicate(p_206483_)));
   }

   public static <FC extends FeatureConfiguration, F extends Feature<FC>> RandomPatchConfiguration simplePatchConfiguration(F pFeature, FC pConfig, List<Block> p_206479_) {
      return simplePatchConfiguration(pFeature, pConfig, p_206479_, 96);
   }

   public static <FC extends FeatureConfiguration, F extends Feature<FC>> RandomPatchConfiguration simplePatchConfiguration(F pFeature, FC pConfig) {
      return simplePatchConfiguration(pFeature, pConfig, List.of(), 96);
   }

   public static Holder<ConfiguredFeature<NoneFeatureConfiguration, ?>> register(String pName, Feature<NoneFeatureConfiguration> pFeature) {
      return register(pName, pFeature, FeatureConfiguration.NONE);
   }

   public static <FC extends FeatureConfiguration, F extends Feature<FC>> Holder<ConfiguredFeature<FC, ?>> register(String pName, F pFeature, FC pConfig) {
      return BuiltinRegistries.registerExact(BuiltinRegistries.CONFIGURED_FEATURE, pName, new ConfiguredFeature<>(pFeature, pConfig));
   }
}