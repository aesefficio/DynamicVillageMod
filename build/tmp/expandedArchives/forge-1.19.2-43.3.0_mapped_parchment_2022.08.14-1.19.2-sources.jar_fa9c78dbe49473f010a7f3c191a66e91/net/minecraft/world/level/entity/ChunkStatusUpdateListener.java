package net.minecraft.world.level.entity;

import net.minecraft.server.level.ChunkHolder;
import net.minecraft.world.level.ChunkPos;

@FunctionalInterface
public interface ChunkStatusUpdateListener {
   void onChunkStatusChange(ChunkPos pPos, ChunkHolder.FullChunkStatus pStatus);
}