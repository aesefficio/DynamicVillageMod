package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class GoToTargetLocation<E extends Mob> extends Behavior<E> {
   private final MemoryModuleType<BlockPos> locationMemory;
   private final int closeEnoughDist;
   private final float speedModifier;

   public GoToTargetLocation(MemoryModuleType<BlockPos> p_217235_, int p_217236_, float p_217237_) {
      super(ImmutableMap.of(p_217235_, MemoryStatus.VALUE_PRESENT, MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED));
      this.locationMemory = p_217235_;
      this.closeEnoughDist = p_217236_;
      this.speedModifier = p_217237_;
   }

   protected void start(ServerLevel p_217243_, Mob p_217244_, long p_217245_) {
      BlockPos blockpos = this.getTargetLocation(p_217244_);
      boolean flag = blockpos.closerThan(p_217244_.blockPosition(), (double)this.closeEnoughDist);
      if (!flag) {
         BehaviorUtils.setWalkAndLookTargetMemories(p_217244_, getNearbyPos(p_217244_, blockpos), this.speedModifier, this.closeEnoughDist);
      }

   }

   private static BlockPos getNearbyPos(Mob p_217251_, BlockPos p_217252_) {
      RandomSource randomsource = p_217251_.level.random;
      return p_217252_.offset(getRandomOffset(randomsource), 0, getRandomOffset(randomsource));
   }

   private static int getRandomOffset(RandomSource p_217247_) {
      return p_217247_.nextInt(3) - 1;
   }

   private BlockPos getTargetLocation(Mob p_217249_) {
      return p_217249_.getBrain().getMemory(this.locationMemory).get();
   }
}