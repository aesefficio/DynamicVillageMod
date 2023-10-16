package net.minecraft.world.ticks;

import net.minecraft.core.BlockPos;

public interface TickAccess<T> {
   void schedule(ScheduledTick<T> pTick);

   boolean hasScheduledTick(BlockPos p_193429_, T p_193430_);

   int count();
}