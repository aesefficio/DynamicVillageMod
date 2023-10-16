package net.minecraft.world.level.lighting;

import java.util.Arrays;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.mutable.MutableInt;

public abstract class LayerLightEngine<M extends DataLayerStorageMap<M>, S extends LayerLightSectionStorage<M>> extends DynamicGraphMinFixedPoint implements LayerLightEventListener {
   public static final long SELF_SOURCE = Long.MAX_VALUE;
   private static final Direction[] DIRECTIONS = Direction.values();
   protected final LightChunkGetter chunkSource;
   protected final LightLayer layer;
   protected final S storage;
   private boolean runningLightUpdates;
   protected final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
   private static final int CACHE_SIZE = 2;
   private final long[] lastChunkPos = new long[2];
   private final BlockGetter[] lastChunk = new BlockGetter[2];

   public LayerLightEngine(LightChunkGetter pChunkSource, LightLayer pLayer, S pStorage) {
      super(16, 256, 8192);
      this.chunkSource = pChunkSource;
      this.layer = pLayer;
      this.storage = pStorage;
      this.clearCache();
   }

   protected void checkNode(long pLevelPos) {
      this.storage.runAllUpdates();
      if (this.storage.storingLightForSection(SectionPos.blockToSection(pLevelPos))) {
         super.checkNode(pLevelPos);
      }

   }

   @Nullable
   private BlockGetter getChunk(int pChunkX, int pChunkZ) {
      long i = ChunkPos.asLong(pChunkX, pChunkZ);

      for(int j = 0; j < 2; ++j) {
         if (i == this.lastChunkPos[j]) {
            return this.lastChunk[j];
         }
      }

      BlockGetter blockgetter = this.chunkSource.getChunkForLighting(pChunkX, pChunkZ);

      for(int k = 1; k > 0; --k) {
         this.lastChunkPos[k] = this.lastChunkPos[k - 1];
         this.lastChunk[k] = this.lastChunk[k - 1];
      }

      this.lastChunkPos[0] = i;
      this.lastChunk[0] = blockgetter;
      return blockgetter;
   }

   private void clearCache() {
      Arrays.fill(this.lastChunkPos, ChunkPos.INVALID_CHUNK_POS);
      Arrays.fill(this.lastChunk, (Object)null);
   }

   protected BlockState getStateAndOpacity(long pPos, @Nullable MutableInt pOpacityOut) {
      if (pPos == Long.MAX_VALUE) {
         if (pOpacityOut != null) {
            pOpacityOut.setValue(0);
         }

         return Blocks.AIR.defaultBlockState();
      } else {
         int i = SectionPos.blockToSectionCoord(BlockPos.getX(pPos));
         int j = SectionPos.blockToSectionCoord(BlockPos.getZ(pPos));
         BlockGetter blockgetter = this.getChunk(i, j);
         if (blockgetter == null) {
            if (pOpacityOut != null) {
               pOpacityOut.setValue(16);
            }

            return Blocks.BEDROCK.defaultBlockState();
         } else {
            this.pos.set(pPos);
            BlockState blockstate = blockgetter.getBlockState(this.pos);
            boolean flag = blockstate.canOcclude() && blockstate.useShapeForLightOcclusion();
            if (pOpacityOut != null) {
               pOpacityOut.setValue(blockstate.getLightBlock(this.chunkSource.getLevel(), this.pos));
            }

            return flag ? blockstate : Blocks.AIR.defaultBlockState();
         }
      }
   }

   protected VoxelShape getShape(BlockState pBlockState, long pLevelPos, Direction pDirection) {
      return pBlockState.canOcclude() ? pBlockState.getFaceOcclusionShape(this.chunkSource.getLevel(), this.pos.set(pLevelPos), pDirection) : Shapes.empty();
   }

   public static int getLightBlockInto(BlockGetter pLevel, BlockState pState, BlockPos pPos, BlockState pStateAbove, BlockPos pPosAbove, Direction pDir, int pDefaultValue) {
      boolean flag = pState.canOcclude() && pState.useShapeForLightOcclusion();
      boolean flag1 = pStateAbove.canOcclude() && pStateAbove.useShapeForLightOcclusion();
      if (!flag && !flag1) {
         return pDefaultValue;
      } else {
         VoxelShape voxelshape = flag ? pState.getOcclusionShape(pLevel, pPos) : Shapes.empty();
         VoxelShape voxelshape1 = flag1 ? pStateAbove.getOcclusionShape(pLevel, pPosAbove) : Shapes.empty();
         return Shapes.mergedFaceOccludes(voxelshape, voxelshape1, pDir) ? 16 : pDefaultValue;
      }
   }

   protected boolean isSource(long pPos) {
      return pPos == Long.MAX_VALUE;
   }

   /**
    * Computes level propagated from neighbors of specified position with given existing level, excluding the given
    * source position.
    */
   protected int getComputedLevel(long pPos, long pExcludedSourcePos, int pLevel) {
      return 0;
   }

   protected int getLevel(long pSectionPos) {
      return pSectionPos == Long.MAX_VALUE ? 0 : 15 - this.storage.getStoredLevel(pSectionPos);
   }

   protected int getLevel(DataLayer pArray, long pLevelPos) {
      return 15 - pArray.get(SectionPos.sectionRelative(BlockPos.getX(pLevelPos)), SectionPos.sectionRelative(BlockPos.getY(pLevelPos)), SectionPos.sectionRelative(BlockPos.getZ(pLevelPos)));
   }

   protected void setLevel(long pSectionPos, int pLevel) {
      this.storage.setStoredLevel(pSectionPos, Math.min(15, 15 - pLevel));
   }

   /**
    * Returns level propagated from start position with specified level to the neighboring end position.
    */
   protected int computeLevelFromNeighbor(long pStartPos, long pEndPos, int pStartLevel) {
      return 0;
   }

   public boolean hasLightWork() {
      return this.hasWork() || this.storage.hasWork() || this.storage.hasInconsistencies();
   }

   public int runUpdates(int pPos, boolean pIsQueueEmpty, boolean pUpdateBlockLight) {
      if (!this.runningLightUpdates) {
         if (this.storage.hasWork()) {
            pPos = this.storage.runUpdates(pPos);
            if (pPos == 0) {
               return pPos;
            }
         }

         this.storage.markNewInconsistencies(this, pIsQueueEmpty, pUpdateBlockLight);
      }

      this.runningLightUpdates = true;
      if (this.hasWork()) {
         pPos = this.runUpdates(pPos);
         this.clearCache();
         if (pPos == 0) {
            return pPos;
         }
      }

      this.runningLightUpdates = false;
      this.storage.swapSectionMap();
      return pPos;
   }

   protected void queueSectionData(long pSectionPos, @Nullable DataLayer pArray, boolean pIsQueueEmpty) {
      this.storage.queueSectionData(pSectionPos, pArray, pIsQueueEmpty);
   }

   @Nullable
   public DataLayer getDataLayerData(SectionPos pSectionPos) {
      return this.storage.getDataLayerData(pSectionPos.asLong());
   }

   public int getLightValue(BlockPos pLevelPos) {
      return this.storage.getLightValue(pLevelPos.asLong());
   }

   public String getDebugData(long pSectionPos) {
      return "" + this.storage.getLevel(pSectionPos);
   }

   public void checkBlock(BlockPos pPos) {
      long i = pPos.asLong();
      this.checkNode(i);

      for(Direction direction : DIRECTIONS) {
         this.checkNode(BlockPos.offset(i, direction));
      }

   }

   public void onBlockEmissionIncrease(BlockPos pPos, int pEmissionLevel) {
   }

   public void updateSectionStatus(SectionPos pPos, boolean pIsEmpty) {
      this.storage.updateSectionStatus(pPos.asLong(), pIsEmpty);
   }

   public void enableLightSources(ChunkPos pChunkPos, boolean pIsQueueEmpty) {
      long i = SectionPos.getZeroNode(SectionPos.asLong(pChunkPos.x, 0, pChunkPos.z));
      this.storage.enableLightSources(i, pIsQueueEmpty);
   }

   public void retainData(ChunkPos pPos, boolean pRetain) {
      long i = SectionPos.getZeroNode(SectionPos.asLong(pPos.x, 0, pPos.z));
      this.storage.retainData(i, pRetain);
   }

   public abstract int queuedUpdateSize();
}
