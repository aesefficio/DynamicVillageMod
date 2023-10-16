package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class TryFindWater extends Behavior<PathfinderMob> {
   private final int range;
   private final float speedModifier;
   private long nextOkStartTime;

   public TryFindWater(int pRange, float pSpeedModifier) {
      super(ImmutableMap.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED));
      this.range = pRange;
      this.speedModifier = pSpeedModifier;
   }

   protected void stop(ServerLevel pLevel, PathfinderMob pEntity, long pGameTime) {
      this.nextOkStartTime = pGameTime + 20L + 2L;
   }

   protected boolean checkExtraStartConditions(ServerLevel pLevel, PathfinderMob pOwner) {
      return !pOwner.level.getFluidState(pOwner.blockPosition()).is(FluidTags.WATER);
   }

   protected void start(ServerLevel pLevel, PathfinderMob pEntity, long pGameTime) {
      if (pGameTime >= this.nextOkStartTime) {
         BlockPos blockpos = null;
         BlockPos blockpos1 = null;
         BlockPos blockpos2 = pEntity.blockPosition();

         for(BlockPos blockpos3 : BlockPos.withinManhattan(blockpos2, this.range, this.range, this.range)) {
            if (blockpos3.getX() != blockpos2.getX() || blockpos3.getZ() != blockpos2.getZ()) {
               BlockState blockstate = pEntity.level.getBlockState(blockpos3.above());
               BlockState blockstate1 = pEntity.level.getBlockState(blockpos3);
               if (blockstate1.is(Blocks.WATER)) {
                  if (blockstate.isAir()) {
                     blockpos = blockpos3.immutable();
                     break;
                  }

                  if (blockpos1 == null && !blockpos3.closerToCenterThan(pEntity.position(), 1.5D)) {
                     blockpos1 = blockpos3.immutable();
                  }
               }
            }
         }

         if (blockpos == null) {
            blockpos = blockpos1;
         }

         if (blockpos != null) {
            this.nextOkStartTime = pGameTime + 40L;
            BehaviorUtils.setWalkAndLookTargetMemories(pEntity, blockpos, this.speedModifier, 0);
         }

      }
   }
}