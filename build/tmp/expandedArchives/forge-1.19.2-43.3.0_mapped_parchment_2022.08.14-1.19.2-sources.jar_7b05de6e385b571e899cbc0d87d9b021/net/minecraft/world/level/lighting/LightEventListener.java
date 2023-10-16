package net.minecraft.world.level.lighting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;

public interface LightEventListener {
   void checkBlock(BlockPos pPos);

   void onBlockEmissionIncrease(BlockPos pPos, int pEmissionLevel);

   boolean hasLightWork();

   int runUpdates(int pPos, boolean pIsQueueEmpty, boolean pUpdateBlockLight);

   default void updateSectionStatus(BlockPos pPos, boolean pIsQueueEmpty) {
      this.updateSectionStatus(SectionPos.of(pPos), pIsQueueEmpty);
   }

   void updateSectionStatus(SectionPos pPos, boolean pIsQueueEmpty);

   void enableLightSources(ChunkPos pChunkPos, boolean pIsQueueEmpty);
}