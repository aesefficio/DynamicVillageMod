package net.minecraft.world.level.lighting;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunkGetter;

public class LevelLightEngine implements LightEventListener {
   public static final int MAX_SOURCE_LEVEL = 15;
   public static final int LIGHT_SECTION_PADDING = 1;
   protected final LevelHeightAccessor levelHeightAccessor;
   @Nullable
   private final LayerLightEngine<?, ?> blockEngine;
   @Nullable
   private final LayerLightEngine<?, ?> skyEngine;

   public LevelLightEngine(LightChunkGetter pLightChunkGetter, boolean p_75806_, boolean p_75807_) {
      this.levelHeightAccessor = pLightChunkGetter.getLevel();
      this.blockEngine = p_75806_ ? new BlockLightEngine(pLightChunkGetter) : null;
      this.skyEngine = p_75807_ ? new SkyLightEngine(pLightChunkGetter) : null;
   }

   public void checkBlock(BlockPos pPos) {
      if (this.blockEngine != null) {
         this.blockEngine.checkBlock(pPos);
      }

      if (this.skyEngine != null) {
         this.skyEngine.checkBlock(pPos);
      }

   }

   public void onBlockEmissionIncrease(BlockPos pPos, int pEmissionLevel) {
      if (this.blockEngine != null) {
         this.blockEngine.onBlockEmissionIncrease(pPos, pEmissionLevel);
      }

   }

   public boolean hasLightWork() {
      if (this.skyEngine != null && this.skyEngine.hasLightWork()) {
         return true;
      } else {
         return this.blockEngine != null && this.blockEngine.hasLightWork();
      }
   }

   public int runUpdates(int pPos, boolean pIsQueueEmpty, boolean pUpdateBlockLight) {
      if (this.blockEngine != null && this.skyEngine != null) {
         int i = pPos / 2;
         int j = this.blockEngine.runUpdates(i, pIsQueueEmpty, pUpdateBlockLight);
         int k = pPos - i + j;
         int l = this.skyEngine.runUpdates(k, pIsQueueEmpty, pUpdateBlockLight);
         return j == 0 && l > 0 ? this.blockEngine.runUpdates(l, pIsQueueEmpty, pUpdateBlockLight) : l;
      } else if (this.blockEngine != null) {
         return this.blockEngine.runUpdates(pPos, pIsQueueEmpty, pUpdateBlockLight);
      } else {
         return this.skyEngine != null ? this.skyEngine.runUpdates(pPos, pIsQueueEmpty, pUpdateBlockLight) : pPos;
      }
   }

   public void updateSectionStatus(SectionPos pPos, boolean pIsEmpty) {
      if (this.blockEngine != null) {
         this.blockEngine.updateSectionStatus(pPos, pIsEmpty);
      }

      if (this.skyEngine != null) {
         this.skyEngine.updateSectionStatus(pPos, pIsEmpty);
      }

   }

   public void enableLightSources(ChunkPos pChunkPos, boolean pIsQueueEmpty) {
      if (this.blockEngine != null) {
         this.blockEngine.enableLightSources(pChunkPos, pIsQueueEmpty);
      }

      if (this.skyEngine != null) {
         this.skyEngine.enableLightSources(pChunkPos, pIsQueueEmpty);
      }

   }

   public LayerLightEventListener getLayerListener(LightLayer pType) {
      if (pType == LightLayer.BLOCK) {
         return (LayerLightEventListener)(this.blockEngine == null ? LayerLightEventListener.DummyLightLayerEventListener.INSTANCE : this.blockEngine);
      } else {
         return (LayerLightEventListener)(this.skyEngine == null ? LayerLightEventListener.DummyLightLayerEventListener.INSTANCE : this.skyEngine);
      }
   }

   public String getDebugData(LightLayer p_75817_, SectionPos p_75818_) {
      if (p_75817_ == LightLayer.BLOCK) {
         if (this.blockEngine != null) {
            return this.blockEngine.getDebugData(p_75818_.asLong());
         }
      } else if (this.skyEngine != null) {
         return this.skyEngine.getDebugData(p_75818_.asLong());
      }

      return "n/a";
   }

   public void queueSectionData(LightLayer pType, SectionPos pPos, @Nullable DataLayer pArray, boolean p_75822_) {
      if (pType == LightLayer.BLOCK) {
         if (this.blockEngine != null) {
            this.blockEngine.queueSectionData(pPos.asLong(), pArray, p_75822_);
         }
      } else if (this.skyEngine != null) {
         this.skyEngine.queueSectionData(pPos.asLong(), pArray, p_75822_);
      }

   }

   public void retainData(ChunkPos pPos, boolean pRetain) {
      if (this.blockEngine != null) {
         this.blockEngine.retainData(pPos, pRetain);
      }

      if (this.skyEngine != null) {
         this.skyEngine.retainData(pPos, pRetain);
      }

   }

   public int getRawBrightness(BlockPos pBlockPos, int pAmount) {
      int i = this.skyEngine == null ? 0 : this.skyEngine.getLightValue(pBlockPos) - pAmount;
      int j = this.blockEngine == null ? 0 : this.blockEngine.getLightValue(pBlockPos);
      return Math.max(j, i);
   }

   public int getLightSectionCount() {
      return this.levelHeightAccessor.getSectionsCount() + 2;
   }

   public int getMinLightSection() {
      return this.levelHeightAccessor.getMinSection() - 1;
   }

   public int getMaxLightSection() {
      return this.getMinLightSection() + this.getLightSectionCount();
   }
}