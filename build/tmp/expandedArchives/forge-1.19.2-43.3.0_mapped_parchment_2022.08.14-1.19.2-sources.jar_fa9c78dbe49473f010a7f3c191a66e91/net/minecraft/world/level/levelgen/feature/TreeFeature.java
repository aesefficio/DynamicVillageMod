package net.minecraft.world.level.levelgen.feature;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.LevelWriter;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;

public class TreeFeature extends Feature<TreeConfiguration> {
   private static final int BLOCK_UPDATE_FLAGS = 19;

   public TreeFeature(Codec<TreeConfiguration> pCodec) {
      super(pCodec);
   }

   private static boolean isVine(LevelSimulatedReader pLevel, BlockPos pPos) {
      return pLevel.isStateAtPosition(pPos, (p_225299_) -> {
         return p_225299_.is(Blocks.VINE);
      });
   }

   public static boolean isBlockWater(LevelSimulatedReader pLevel, BlockPos pPos) {
      return pLevel.isStateAtPosition(pPos, (p_225297_) -> {
         return p_225297_.is(Blocks.WATER);
      });
   }

   public static boolean isAirOrLeaves(LevelSimulatedReader pLevel, BlockPos pPos) {
      return pLevel.isStateAtPosition(pPos, (p_225295_) -> {
         return p_225295_.isAir() || p_225295_.is(BlockTags.LEAVES);
      });
   }

   private static boolean isReplaceablePlant(LevelSimulatedReader pLevel, BlockPos pPos) {
      return pLevel.isStateAtPosition(pPos, (p_225293_) -> {
         Material material = p_225293_.getMaterial();
         return material == Material.REPLACEABLE_PLANT || material == Material.REPLACEABLE_WATER_PLANT || material == Material.REPLACEABLE_FIREPROOF_PLANT;
      });
   }

   private static void setBlockKnownShape(LevelWriter pLevel, BlockPos pPos, BlockState pState) {
      pLevel.setBlock(pPos, pState, 19);
   }

   public static boolean validTreePos(LevelSimulatedReader pLevel, BlockPos pPos) {
      return isAirOrLeaves(pLevel, pPos) || isReplaceablePlant(pLevel, pPos) || isBlockWater(pLevel, pPos);
   }

   private boolean doPlace(WorldGenLevel pLevel, RandomSource pRandom, BlockPos pPos, BiConsumer<BlockPos, BlockState> pRootBlockSetter, BiConsumer<BlockPos, BlockState> pTrunkBlockSetter, BiConsumer<BlockPos, BlockState> pFoliageBlockSetter, TreeConfiguration pConfig) {
      int i = pConfig.trunkPlacer.getTreeHeight(pRandom);
      int j = pConfig.foliagePlacer.foliageHeight(pRandom, i, pConfig);
      int k = i - j;
      int l = pConfig.foliagePlacer.foliageRadius(pRandom, k);
      BlockPos blockpos = pConfig.rootPlacer.map((p_225286_) -> {
         return p_225286_.getTrunkOrigin(pPos, pRandom);
      }).orElse(pPos);
      int i1 = Math.min(pPos.getY(), blockpos.getY());
      int j1 = Math.max(pPos.getY(), blockpos.getY()) + i + 1;
      if (i1 >= pLevel.getMinBuildHeight() + 1 && j1 <= pLevel.getMaxBuildHeight()) {
         OptionalInt optionalint = pConfig.minimumSize.minClippedHeight();
         int k1 = this.getMaxFreeTreeHeight(pLevel, i, blockpos, pConfig);
         if (k1 >= i || !optionalint.isEmpty() && k1 >= optionalint.getAsInt()) {
            if (pConfig.rootPlacer.isPresent() && !pConfig.rootPlacer.get().placeRoots(pLevel, pRootBlockSetter, pRandom, pPos, blockpos, pConfig)) {
               return false;
            } else {
               List<FoliagePlacer.FoliageAttachment> list = pConfig.trunkPlacer.placeTrunk(pLevel, pTrunkBlockSetter, pRandom, k1, blockpos, pConfig);
               list.forEach((p_225279_) -> {
                  pConfig.foliagePlacer.createFoliage(pLevel, pFoliageBlockSetter, pRandom, pConfig, k1, p_225279_, j, l);
               });
               return true;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private int getMaxFreeTreeHeight(LevelSimulatedReader pLevel, int pTrunkHeight, BlockPos pTopPosition, TreeConfiguration pConfig) {
      BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

      for(int i = 0; i <= pTrunkHeight + 1; ++i) {
         int j = pConfig.minimumSize.getSizeAtHeight(pTrunkHeight, i);

         for(int k = -j; k <= j; ++k) {
            for(int l = -j; l <= j; ++l) {
               blockpos$mutableblockpos.setWithOffset(pTopPosition, k, i, l);
               if (!pConfig.trunkPlacer.isFree(pLevel, blockpos$mutableblockpos) || !pConfig.ignoreVines && isVine(pLevel, blockpos$mutableblockpos)) {
                  return i - 2;
               }
            }
         }
      }

      return pTrunkHeight;
   }

   protected void setBlock(LevelWriter pLevel, BlockPos pPos, BlockState pState) {
      setBlockKnownShape(pLevel, pPos, pState);
   }

   /**
    * Places the given feature at the given location.
    * During world generation, features are provided with a 3x3 region of chunks, centered on the chunk being generated,
    * that they can safely generate into.
    * @param pContext A context object with a reference to the level and the position the feature is being placed at
    */
   public final boolean place(FeaturePlaceContext<TreeConfiguration> pContext) {
      WorldGenLevel worldgenlevel = pContext.level();
      RandomSource randomsource = pContext.random();
      BlockPos blockpos = pContext.origin();
      TreeConfiguration treeconfiguration = pContext.config();
      Set<BlockPos> set = Sets.newHashSet();
      Set<BlockPos> set1 = Sets.newHashSet();
      Set<BlockPos> set2 = Sets.newHashSet();
      Set<BlockPos> set3 = Sets.newHashSet();
      BiConsumer<BlockPos, BlockState> biconsumer = (p_160555_, p_160556_) -> {
         set.add(p_160555_.immutable());
         worldgenlevel.setBlock(p_160555_, p_160556_, 19);
      };
      BiConsumer<BlockPos, BlockState> biconsumer1 = (p_160548_, p_160549_) -> {
         set1.add(p_160548_.immutable());
         worldgenlevel.setBlock(p_160548_, p_160549_, 19);
      };
      BiConsumer<BlockPos, BlockState> biconsumer2 = (p_160543_, p_160544_) -> {
         set2.add(p_160543_.immutable());
         worldgenlevel.setBlock(p_160543_, p_160544_, 19);
      };
      BiConsumer<BlockPos, BlockState> biconsumer3 = (p_225290_, p_225291_) -> {
         set3.add(p_225290_.immutable());
         worldgenlevel.setBlock(p_225290_, p_225291_, 19);
      };
      boolean flag = this.doPlace(worldgenlevel, randomsource, blockpos, biconsumer, biconsumer1, biconsumer2, treeconfiguration);
      if (flag && (!set1.isEmpty() || !set2.isEmpty())) {
         if (!treeconfiguration.decorators.isEmpty()) {
            TreeDecorator.Context treedecorator$context = new TreeDecorator.Context(worldgenlevel, biconsumer3, randomsource, set1, set2, set);
            treeconfiguration.decorators.forEach((p_225282_) -> {
               p_225282_.place(treedecorator$context);
            });
         }

         return BoundingBox.encapsulatingPositions(Iterables.concat(set, set1, set2, set3)).map((p_225270_) -> {
            DiscreteVoxelShape discretevoxelshape = updateLeaves(worldgenlevel, p_225270_, set1, set3, set);
            StructureTemplate.updateShapeAtEdge(worldgenlevel, 3, discretevoxelshape, p_225270_.minX(), p_225270_.minY(), p_225270_.minZ());
            return true;
         }).orElse(false);
      } else {
         return false;
      }
   }

   private static DiscreteVoxelShape updateLeaves(LevelAccessor pLevel, BoundingBox pBox, Set<BlockPos> pRootPositions, Set<BlockPos> pTrunkPositions, Set<BlockPos> pFoliagePositions) {
      List<Set<BlockPos>> list = Lists.newArrayList();
      DiscreteVoxelShape discretevoxelshape = new BitSetDiscreteVoxelShape(pBox.getXSpan(), pBox.getYSpan(), pBox.getZSpan());
      int i = 6;

      for(int j = 0; j < 6; ++j) {
         list.add(Sets.newHashSet());
      }

      BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

      for(BlockPos blockpos : Lists.newArrayList(Sets.union(pTrunkPositions, pFoliagePositions))) {
         if (pBox.isInside(blockpos)) {
            discretevoxelshape.fill(blockpos.getX() - pBox.minX(), blockpos.getY() - pBox.minY(), blockpos.getZ() - pBox.minZ());
         }
      }

      for(BlockPos blockpos1 : Lists.newArrayList(pRootPositions)) {
         if (pBox.isInside(blockpos1)) {
            discretevoxelshape.fill(blockpos1.getX() - pBox.minX(), blockpos1.getY() - pBox.minY(), blockpos1.getZ() - pBox.minZ());
         }

         for(Direction direction : Direction.values()) {
            blockpos$mutableblockpos.setWithOffset(blockpos1, direction);
            if (!pRootPositions.contains(blockpos$mutableblockpos)) {
               BlockState blockstate = pLevel.getBlockState(blockpos$mutableblockpos);
               if (blockstate.hasProperty(BlockStateProperties.DISTANCE)) {
                  list.get(0).add(blockpos$mutableblockpos.immutable());
                  setBlockKnownShape(pLevel, blockpos$mutableblockpos, blockstate.setValue(BlockStateProperties.DISTANCE, Integer.valueOf(1)));
                  if (pBox.isInside(blockpos$mutableblockpos)) {
                     discretevoxelshape.fill(blockpos$mutableblockpos.getX() - pBox.minX(), blockpos$mutableblockpos.getY() - pBox.minY(), blockpos$mutableblockpos.getZ() - pBox.minZ());
                  }
               }
            }
         }
      }

      for(int l = 1; l < 6; ++l) {
         Set<BlockPos> set = list.get(l - 1);
         Set<BlockPos> set1 = list.get(l);

         for(BlockPos blockpos2 : set) {
            if (pBox.isInside(blockpos2)) {
               discretevoxelshape.fill(blockpos2.getX() - pBox.minX(), blockpos2.getY() - pBox.minY(), blockpos2.getZ() - pBox.minZ());
            }

            for(Direction direction1 : Direction.values()) {
               blockpos$mutableblockpos.setWithOffset(blockpos2, direction1);
               if (!set.contains(blockpos$mutableblockpos) && !set1.contains(blockpos$mutableblockpos)) {
                  BlockState blockstate1 = pLevel.getBlockState(blockpos$mutableblockpos);
                  if (blockstate1.hasProperty(BlockStateProperties.DISTANCE)) {
                     int k = blockstate1.getValue(BlockStateProperties.DISTANCE);
                     if (k > l + 1) {
                        BlockState blockstate2 = blockstate1.setValue(BlockStateProperties.DISTANCE, Integer.valueOf(l + 1));
                        setBlockKnownShape(pLevel, blockpos$mutableblockpos, blockstate2);
                        if (pBox.isInside(blockpos$mutableblockpos)) {
                           discretevoxelshape.fill(blockpos$mutableblockpos.getX() - pBox.minX(), blockpos$mutableblockpos.getY() - pBox.minY(), blockpos$mutableblockpos.getZ() - pBox.minZ());
                        }

                        set1.add(blockpos$mutableblockpos.immutable());
                     }
                  }
               }
            }
         }
      }

      return discretevoxelshape;
   }
}