package net.minecraft.world.ticks;

import java.util.function.Function;
import net.minecraft.core.BlockPos;

public class WorldGenTickAccess<T> implements LevelTickAccess<T> {
   private final Function<BlockPos, TickContainerAccess<T>> containerGetter;

   public WorldGenTickAccess(Function<BlockPos, TickContainerAccess<T>> pContainerGetter) {
      this.containerGetter = pContainerGetter;
   }

   public boolean hasScheduledTick(BlockPos p_193459_, T p_193460_) {
      return this.containerGetter.apply(p_193459_).hasScheduledTick(p_193459_, p_193460_);
   }

   public void schedule(ScheduledTick<T> pTick) {
      this.containerGetter.apply(pTick.pos()).schedule(pTick);
   }

   public boolean willTickThisTick(BlockPos p_193462_, T p_193463_) {
      return false;
   }

   public int count() {
      return 0;
   }
}