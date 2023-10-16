package net.minecraft.world.level.lighting;

import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.mutable.MutableInt;

public final class SkyLightEngine extends LayerLightEngine<SkyLightSectionStorage.SkyDataLayerStorageMap, SkyLightSectionStorage> {
   private static final Direction[] DIRECTIONS = Direction.values();
   private static final Direction[] HORIZONTALS = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

   public SkyLightEngine(LightChunkGetter pChunkSource) {
      super(pChunkSource, LightLayer.SKY, new SkyLightSectionStorage(pChunkSource));
   }

   /**
    * Returns level propagated from start position with specified level to the neighboring end position.
    */
   protected int computeLevelFromNeighbor(long pStartPos, long pEndPos, int pStartLevel) {
      if (pEndPos != Long.MAX_VALUE && pStartPos != Long.MAX_VALUE) {
         if (pStartLevel >= 15) {
            return pStartLevel;
         } else {
            MutableInt mutableint = new MutableInt();
            BlockState blockstate = this.getStateAndOpacity(pEndPos, mutableint);
            if (mutableint.getValue() >= 15) {
               return 15;
            } else {
               int i = BlockPos.getX(pStartPos);
               int j = BlockPos.getY(pStartPos);
               int k = BlockPos.getZ(pStartPos);
               int l = BlockPos.getX(pEndPos);
               int i1 = BlockPos.getY(pEndPos);
               int j1 = BlockPos.getZ(pEndPos);
               int k1 = Integer.signum(l - i);
               int l1 = Integer.signum(i1 - j);
               int i2 = Integer.signum(j1 - k);
               Direction direction = Direction.fromNormal(k1, l1, i2);
               if (direction == null) {
                  throw new IllegalStateException(String.format(Locale.ROOT, "Light was spread in illegal direction %d, %d, %d", k1, l1, i2));
               } else {
                  BlockState blockstate1 = this.getStateAndOpacity(pStartPos, (MutableInt)null);
                  VoxelShape voxelshape = this.getShape(blockstate1, pStartPos, direction);
                  VoxelShape voxelshape1 = this.getShape(blockstate, pEndPos, direction.getOpposite());
                  if (Shapes.faceShapeOccludes(voxelshape, voxelshape1)) {
                     return 15;
                  } else {
                     boolean flag = i == l && k == j1;
                     boolean flag1 = flag && j > i1;
                     return flag1 && pStartLevel == 0 && mutableint.getValue() == 0 ? 0 : pStartLevel + Math.max(1, mutableint.getValue());
                  }
               }
            }
         }
      } else {
         return 15;
      }
   }

   protected void checkNeighborsAfterUpdate(long pPos, int pLevel, boolean pIsDecreasing) {
      long i = SectionPos.blockToSection(pPos);
      int j = BlockPos.getY(pPos);
      int k = SectionPos.sectionRelative(j);
      int l = SectionPos.blockToSectionCoord(j);
      int i1;
      if (k != 0) {
         i1 = 0;
      } else {
         int j1;
         for(j1 = 0; !this.storage.storingLightForSection(SectionPos.offset(i, 0, -j1 - 1, 0)) && this.storage.hasSectionsBelow(l - j1 - 1); ++j1) {
         }

         i1 = j1;
      }

      long j3 = BlockPos.offset(pPos, 0, -1 - i1 * 16, 0);
      long k1 = SectionPos.blockToSection(j3);
      if (i == k1 || this.storage.storingLightForSection(k1)) {
         this.checkNeighbor(pPos, j3, pLevel, pIsDecreasing);
      }

      long l1 = BlockPos.offset(pPos, Direction.UP);
      long i2 = SectionPos.blockToSection(l1);
      if (i == i2 || this.storage.storingLightForSection(i2)) {
         this.checkNeighbor(pPos, l1, pLevel, pIsDecreasing);
      }

      for(Direction direction : HORIZONTALS) {
         int j2 = 0;

         while(true) {
            long k2 = BlockPos.offset(pPos, direction.getStepX(), -j2, direction.getStepZ());
            long l2 = SectionPos.blockToSection(k2);
            if (i == l2) {
               this.checkNeighbor(pPos, k2, pLevel, pIsDecreasing);
               break;
            }

            if (this.storage.storingLightForSection(l2)) {
               long i3 = BlockPos.offset(pPos, 0, -j2, 0);
               this.checkNeighbor(i3, k2, pLevel, pIsDecreasing);
            }

            ++j2;
            if (j2 > i1 * 16) {
               break;
            }
         }
      }

   }

   /**
    * Computes level propagated from neighbors of specified position with given existing level, excluding the given
    * source position.
    */
   protected int getComputedLevel(long pPos, long pExcludedSourcePos, int pLevel) {
      int i = pLevel;
      long j = SectionPos.blockToSection(pPos);
      DataLayer datalayer = this.storage.getDataLayer(j, true);

      for(Direction direction : DIRECTIONS) {
         long k = BlockPos.offset(pPos, direction);
         if (k != pExcludedSourcePos) {
            long l = SectionPos.blockToSection(k);
            DataLayer datalayer1;
            if (j == l) {
               datalayer1 = datalayer;
            } else {
               datalayer1 = this.storage.getDataLayer(l, true);
            }

            int i1;
            if (datalayer1 != null) {
               i1 = this.getLevel(datalayer1, k);
            } else {
               if (direction == Direction.DOWN) {
                  continue;
               }

               i1 = 15 - this.storage.getLightValue(k, true);
            }

            int j1 = this.computeLevelFromNeighbor(k, pPos, i1);
            if (i > j1) {
               i = j1;
            }

            if (i == 0) {
               return i;
            }
         }
      }

      return i;
   }

   protected void checkNode(long pLevelPos) {
      this.storage.runAllUpdates();
      long i = SectionPos.blockToSection(pLevelPos);
      if (this.storage.storingLightForSection(i)) {
         super.checkNode(pLevelPos);
      } else {
         for(pLevelPos = BlockPos.getFlatIndex(pLevelPos); !this.storage.storingLightForSection(i) && !this.storage.isAboveData(i); pLevelPos = BlockPos.offset(pLevelPos, 0, 16, 0)) {
            i = SectionPos.offset(i, Direction.UP);
         }

         if (this.storage.storingLightForSection(i)) {
            super.checkNode(pLevelPos);
         }
      }

   }

   public String getDebugData(long pSectionPos) {
      return super.getDebugData(pSectionPos) + (this.storage.isAboveData(pSectionPos) ? "*" : "");
   }

   @Override
   public int queuedUpdateSize() {
      return 0;
   }
}
