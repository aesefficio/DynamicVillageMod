package net.minecraft.world.level.levelgen.feature.foliageplacers;

import com.mojang.datafixers.Products;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.material.Fluids;

public abstract class FoliagePlacer {
   public static final Codec<FoliagePlacer> CODEC = Registry.FOLIAGE_PLACER_TYPES.byNameCodec().dispatch(FoliagePlacer::type, FoliagePlacerType::codec);
   protected final IntProvider radius;
   protected final IntProvider offset;

   protected static <P extends FoliagePlacer> Products.P2<RecordCodecBuilder.Mu<P>, IntProvider, IntProvider> foliagePlacerParts(RecordCodecBuilder.Instance<P> pInstance) {
      return pInstance.group(IntProvider.codec(0, 16).fieldOf("radius").forGetter((p_161449_) -> {
         return p_161449_.radius;
      }), IntProvider.codec(0, 16).fieldOf("offset").forGetter((p_161447_) -> {
         return p_161447_.offset;
      }));
   }

   public FoliagePlacer(IntProvider pRadius, IntProvider pOffset) {
      this.radius = pRadius;
      this.offset = pOffset;
   }

   protected abstract FoliagePlacerType<?> type();

   public void createFoliage(LevelSimulatedReader pLevel, BiConsumer<BlockPos, BlockState> pBlockSetter, RandomSource pRandom, TreeConfiguration pConfig, int pMaxFreeTreeHeight, FoliagePlacer.FoliageAttachment pAttachment, int pFoliageHeight, int pFoliageRadius) {
      this.createFoliage(pLevel, pBlockSetter, pRandom, pConfig, pMaxFreeTreeHeight, pAttachment, pFoliageHeight, pFoliageRadius, this.offset(pRandom));
   }

   protected abstract void createFoliage(LevelSimulatedReader pLevel, BiConsumer<BlockPos, BlockState> pBlockSetter, RandomSource pRandom, TreeConfiguration pConfig, int pMaxFreeTreeHeight, FoliagePlacer.FoliageAttachment pAttachment, int pFoliageHeight, int pFoliageRadius, int pOffset);

   public abstract int foliageHeight(RandomSource pRandom, int pHeight, TreeConfiguration pConfig);

   public int foliageRadius(RandomSource pRandom, int pRadius) {
      return this.radius.sample(pRandom);
   }

   private int offset(RandomSource pRandom) {
      return this.offset.sample(pRandom);
   }

   /**
    * Skips certain positions based on the provided shape, such as rounding corners randomly.
    * The coordinates are passed in as absolute value, and should be within [0, {@code range}].
    */
   protected abstract boolean shouldSkipLocation(RandomSource pRandom, int pLocalX, int pLocalY, int pLocalZ, int pRange, boolean pLarge);

   protected boolean shouldSkipLocationSigned(RandomSource pRandom, int pLocalX, int pLocalY, int pLocalZ, int pRange, boolean pLarge) {
      int i;
      int j;
      if (pLarge) {
         i = Math.min(Math.abs(pLocalX), Math.abs(pLocalX - 1));
         j = Math.min(Math.abs(pLocalZ), Math.abs(pLocalZ - 1));
      } else {
         i = Math.abs(pLocalX);
         j = Math.abs(pLocalZ);
      }

      return this.shouldSkipLocation(pRandom, i, pLocalY, j, pRange, pLarge);
   }

   /**
    * Places leaves in a shape within radius {@code range}, and a y offset of {@code yOffset} from the provided block
    * position.
    * @param pLarge When {@code true}, the leaf placement extends an additional one block outside the {@code range} and
    * skip checking is adjusted to fit the larger area.
    */
   protected void placeLeavesRow(LevelSimulatedReader pLevel, BiConsumer<BlockPos, BlockState> pBlockSetter, RandomSource pRandom, TreeConfiguration pConfig, BlockPos pPos, int pRange, int pYOffset, boolean pLarge) {
      int i = pLarge ? 1 : 0;
      BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

      for(int j = -pRange; j <= pRange + i; ++j) {
         for(int k = -pRange; k <= pRange + i; ++k) {
            if (!this.shouldSkipLocationSigned(pRandom, j, pYOffset, k, pRange, pLarge)) {
               blockpos$mutableblockpos.setWithOffset(pPos, j, pYOffset, k);
               tryPlaceLeaf(pLevel, pBlockSetter, pRandom, pConfig, blockpos$mutableblockpos);
            }
         }
      }

   }

   /**
    * Attempts to place a leaf block at the given position, if possible.
    */
   protected static void tryPlaceLeaf(LevelSimulatedReader pLevel, BiConsumer<BlockPos, BlockState> pBlockSetter, RandomSource pRandom, TreeConfiguration pConfig, BlockPos pPos) {
      if (TreeFeature.validTreePos(pLevel, pPos)) {
         BlockState blockstate = pConfig.foliageProvider.getState(pRandom, pPos);
         if (blockstate.hasProperty(BlockStateProperties.WATERLOGGED)) {
            blockstate = blockstate.setValue(BlockStateProperties.WATERLOGGED, Boolean.valueOf(pLevel.isFluidAtPosition(pPos, (p_225638_) -> {
               return p_225638_.isSourceOfType(Fluids.WATER);
            })));
         }

         pBlockSetter.accept(pPos, blockstate);
      }

   }

   public static final class FoliageAttachment {
      private final BlockPos pos;
      private final int radiusOffset;
      private final boolean doubleTrunk;

      public FoliageAttachment(BlockPos pPos, int pRadiusOffset, boolean pDoubleTrunk) {
         this.pos = pPos;
         this.radiusOffset = pRadiusOffset;
         this.doubleTrunk = pDoubleTrunk;
      }

      public BlockPos pos() {
         return this.pos;
      }

      public int radiusOffset() {
         return this.radiusOffset;
      }

      public boolean doubleTrunk() {
         return this.doubleTrunk;
      }
   }
}