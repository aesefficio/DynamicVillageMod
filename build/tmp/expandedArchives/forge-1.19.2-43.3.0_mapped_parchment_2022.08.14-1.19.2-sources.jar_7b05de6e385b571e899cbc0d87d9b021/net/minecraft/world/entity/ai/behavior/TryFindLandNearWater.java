package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.shapes.CollisionContext;

public class TryFindLandNearWater extends Behavior<PathfinderMob> {
   private final int range;
   private final float speedModifier;
   private long nextOkStartTime;

   public TryFindLandNearWater(int p_217446_, float p_217447_) {
      super(ImmutableMap.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED));
      this.range = p_217446_;
      this.speedModifier = p_217447_;
   }

   protected void stop(ServerLevel p_217459_, PathfinderMob p_217460_, long p_217461_) {
      this.nextOkStartTime = p_217461_ + 40L;
   }

   protected boolean checkExtraStartConditions(ServerLevel p_217456_, PathfinderMob p_217457_) {
      return !p_217457_.level.getFluidState(p_217457_.blockPosition()).is(FluidTags.WATER);
   }

   protected void start(ServerLevel p_217463_, PathfinderMob p_217464_, long p_217465_) {
      if (p_217465_ >= this.nextOkStartTime) {
         CollisionContext collisioncontext = CollisionContext.of(p_217464_);
         BlockPos blockpos = p_217464_.blockPosition();
         BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

         for(BlockPos blockpos1 : BlockPos.withinManhattan(blockpos, this.range, this.range, this.range)) {
            if ((blockpos1.getX() != blockpos.getX() || blockpos1.getZ() != blockpos.getZ()) && p_217463_.getBlockState(blockpos1).getCollisionShape(p_217463_, blockpos1, collisioncontext).isEmpty() && !p_217463_.getBlockState(blockpos$mutableblockpos.setWithOffset(blockpos1, Direction.DOWN)).getCollisionShape(p_217463_, blockpos1, collisioncontext).isEmpty()) {
               for(Direction direction : Direction.Plane.HORIZONTAL) {
                  blockpos$mutableblockpos.setWithOffset(blockpos1, direction);
                  if (p_217463_.getBlockState(blockpos$mutableblockpos).isAir() && p_217463_.getBlockState(blockpos$mutableblockpos.move(Direction.DOWN)).is(Blocks.WATER)) {
                     this.nextOkStartTime = p_217465_ + 40L;
                     BehaviorUtils.setWalkAndLookTargetMemories(p_217464_, blockpos1, this.speedModifier, 0);
                     return;
                  }
               }
            }
         }

      }
   }
}