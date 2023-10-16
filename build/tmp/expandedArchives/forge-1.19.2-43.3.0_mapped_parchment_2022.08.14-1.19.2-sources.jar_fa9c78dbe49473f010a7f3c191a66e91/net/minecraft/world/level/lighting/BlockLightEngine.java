package net.minecraft.world.level.lighting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.mutable.MutableInt;

public final class BlockLightEngine extends LayerLightEngine<BlockLightSectionStorage.BlockDataLayerStorageMap, BlockLightSectionStorage> {
   private static final Direction[] DIRECTIONS = Direction.values();
   private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

   public BlockLightEngine(LightChunkGetter pChunkSource) {
      super(pChunkSource, LightLayer.BLOCK, new BlockLightSectionStorage(pChunkSource));
   }

   private int getLightEmission(long pLevelPos) {
      int i = BlockPos.getX(pLevelPos);
      int j = BlockPos.getY(pLevelPos);
      int k = BlockPos.getZ(pLevelPos);
      BlockGetter blockgetter = this.chunkSource.getChunkForLighting(SectionPos.blockToSectionCoord(i), SectionPos.blockToSectionCoord(k));
      return blockgetter != null ? blockgetter.getLightEmission(this.pos.set(i, j, k)) : 0;
   }

   /**
    * Returns level propagated from start position with specified level to the neighboring end position.
    */
   protected int computeLevelFromNeighbor(long pStartPos, long pEndPos, int pStartLevel) {
      if (pEndPos == Long.MAX_VALUE) {
         return 15;
      } else if (pStartPos == Long.MAX_VALUE) {
         return pStartLevel + 15 - this.getLightEmission(pEndPos);
      } else if (pStartLevel >= 15) {
         return pStartLevel;
      } else {
         int i = Integer.signum(BlockPos.getX(pEndPos) - BlockPos.getX(pStartPos));
         int j = Integer.signum(BlockPos.getY(pEndPos) - BlockPos.getY(pStartPos));
         int k = Integer.signum(BlockPos.getZ(pEndPos) - BlockPos.getZ(pStartPos));
         Direction direction = Direction.fromNormal(i, j, k);
         if (direction == null) {
            return 15;
         } else {
            MutableInt mutableint = new MutableInt();
            BlockState blockstate = this.getStateAndOpacity(pEndPos, mutableint);
            if (mutableint.getValue() >= 15) {
               return 15;
            } else {
               BlockState blockstate1 = this.getStateAndOpacity(pStartPos, (MutableInt)null);
               VoxelShape voxelshape = this.getShape(blockstate1, pStartPos, direction);
               VoxelShape voxelshape1 = this.getShape(blockstate, pEndPos, direction.getOpposite());
               return Shapes.faceShapeOccludes(voxelshape, voxelshape1) ? 15 : pStartLevel + Math.max(1, mutableint.getValue());
            }
         }
      }
   }

   protected void checkNeighborsAfterUpdate(long pPos, int pLevel, boolean pIsDecreasing) {
      long i = SectionPos.blockToSection(pPos);

      for(Direction direction : DIRECTIONS) {
         long j = BlockPos.offset(pPos, direction);
         long k = SectionPos.blockToSection(j);
         if (i == k || this.storage.storingLightForSection(k)) {
            this.checkNeighbor(pPos, j, pLevel, pIsDecreasing);
         }
      }

   }

   /**
    * Computes level propagated from neighbors of specified position with given existing level, excluding the given
    * source position.
    */
   protected int getComputedLevel(long pPos, long pExcludedSourcePos, int pLevel) {
      int i = pLevel;
      if (Long.MAX_VALUE != pExcludedSourcePos) {
         int j = this.computeLevelFromNeighbor(Long.MAX_VALUE, pPos, 0);
         if (pLevel > j) {
            i = j;
         }

         if (i == 0) {
            return i;
         }
      }

      long j1 = SectionPos.blockToSection(pPos);
      DataLayer datalayer = this.storage.getDataLayer(j1, true);

      for(Direction direction : DIRECTIONS) {
         long k = BlockPos.offset(pPos, direction);
         if (k != pExcludedSourcePos) {
            long l = SectionPos.blockToSection(k);
            DataLayer datalayer1;
            if (j1 == l) {
               datalayer1 = datalayer;
            } else {
               datalayer1 = this.storage.getDataLayer(l, true);
            }

            if (datalayer1 != null) {
               int i1 = this.computeLevelFromNeighbor(k, pPos, this.getLevel(datalayer1, k));
               if (i > i1) {
                  i = i1;
               }

               if (i == 0) {
                  return i;
               }
            }
         }
      }

      return i;
   }

   public void onBlockEmissionIncrease(BlockPos pPos, int pEmissionLevel) {
      this.storage.runAllUpdates();
      this.checkEdge(Long.MAX_VALUE, pPos.asLong(), 15 - pEmissionLevel, true);
   }

   @Override
   public int queuedUpdateSize() {
      return storage.queuedUpdateSize();
   }
}
