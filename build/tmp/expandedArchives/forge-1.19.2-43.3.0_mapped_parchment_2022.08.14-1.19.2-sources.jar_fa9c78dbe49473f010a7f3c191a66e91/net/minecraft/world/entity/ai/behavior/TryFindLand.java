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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;

public class TryFindLand extends Behavior<PathfinderMob> {
   private static final int COOLDOWN_TICKS = 60;
   private final int range;
   private final float speedModifier;
   private long nextOkStartTime;

   public TryFindLand(int p_217418_, float p_217419_) {
      super(ImmutableMap.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED));
      this.range = p_217418_;
      this.speedModifier = p_217419_;
   }

   protected void stop(ServerLevel p_217431_, PathfinderMob p_217432_, long p_217433_) {
      this.nextOkStartTime = p_217433_ + 60L;
   }

   protected boolean checkExtraStartConditions(ServerLevel p_217428_, PathfinderMob p_217429_) {
      return p_217429_.level.getFluidState(p_217429_.blockPosition()).is(FluidTags.WATER);
   }

   protected void start(ServerLevel p_217435_, PathfinderMob p_217436_, long p_217437_) {
      if (p_217437_ >= this.nextOkStartTime) {
         BlockPos blockpos = p_217436_.blockPosition();
         BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
         CollisionContext collisioncontext = CollisionContext.of(p_217436_);

         for(BlockPos blockpos1 : BlockPos.withinManhattan(blockpos, this.range, this.range, this.range)) {
            if (blockpos1.getX() != blockpos.getX() || blockpos1.getZ() != blockpos.getZ()) {
               BlockState blockstate = p_217435_.getBlockState(blockpos1);
               BlockState blockstate1 = p_217435_.getBlockState(blockpos$mutableblockpos.setWithOffset(blockpos1, Direction.DOWN));
               if (!blockstate.is(Blocks.WATER) && p_217435_.getFluidState(blockpos1).isEmpty() && blockstate.getCollisionShape(p_217435_, blockpos1, collisioncontext).isEmpty() && blockstate1.isFaceSturdy(p_217435_, blockpos$mutableblockpos, Direction.UP)) {
                  this.nextOkStartTime = p_217437_ + 60L;
                  BehaviorUtils.setWalkAndLookTargetMemories(p_217436_, blockpos1.immutable(), this.speedModifier, 1);
                  return;
               }
            }
         }

      }
   }
}