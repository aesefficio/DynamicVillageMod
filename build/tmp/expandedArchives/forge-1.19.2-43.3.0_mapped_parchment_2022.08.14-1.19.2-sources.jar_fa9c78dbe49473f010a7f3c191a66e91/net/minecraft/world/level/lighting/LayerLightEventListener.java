package net.minecraft.world.level.lighting;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.DataLayer;

public interface LayerLightEventListener extends LightEventListener {
   @Nullable
   DataLayer getDataLayerData(SectionPos pSectionPos);

   int getLightValue(BlockPos pLevelPos);

   public static enum DummyLightLayerEventListener implements LayerLightEventListener {
      INSTANCE;

      @Nullable
      public DataLayer getDataLayerData(SectionPos pSectionPos) {
         return null;
      }

      public int getLightValue(BlockPos pLevelPos) {
         return 0;
      }

      public void checkBlock(BlockPos pPos) {
      }

      public void onBlockEmissionIncrease(BlockPos pPos, int pEmissionLevel) {
      }

      public boolean hasLightWork() {
         return false;
      }

      public int runUpdates(int pPos, boolean pIsQueueEmpty, boolean pUpdateBlockLight) {
         return pPos;
      }

      public void updateSectionStatus(SectionPos pPos, boolean pIsEmpty) {
      }

      public void enableLightSources(ChunkPos pChunkPos, boolean pIsQueueEmpty) {
      }
   }
}