package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;

public class TryLaySpawnOnWaterNearLand extends Behavior<Frog> {
   private final Block spawnBlock;
   private final MemoryModuleType<?> memoryModule;

   public TryLaySpawnOnWaterNearLand(Block p_217473_, MemoryModuleType<?> p_217474_) {
      super(ImmutableMap.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_PRESENT, MemoryModuleType.IS_PREGNANT, MemoryStatus.VALUE_PRESENT));
      this.spawnBlock = p_217473_;
      this.memoryModule = p_217474_;
   }

   protected boolean checkExtraStartConditions(ServerLevel p_217483_, Frog p_217484_) {
      return !p_217484_.isInWater() && p_217484_.isOnGround();
   }

   protected void start(ServerLevel p_217486_, Frog p_217487_, long p_217488_) {
      BlockPos blockpos = p_217487_.blockPosition().below();

      for(Direction direction : Direction.Plane.HORIZONTAL) {
         BlockPos blockpos1 = blockpos.relative(direction);
         if (p_217486_.getBlockState(blockpos1).getCollisionShape(p_217486_, blockpos1).getFaceShape(Direction.UP).isEmpty() && p_217486_.getFluidState(blockpos1).is(Fluids.WATER)) {
            BlockPos blockpos2 = blockpos1.above();
            if (p_217486_.getBlockState(blockpos2).isAir()) {
               p_217486_.setBlock(blockpos2, this.spawnBlock.defaultBlockState(), 3);
               p_217486_.playSound((Player)null, p_217487_, SoundEvents.FROG_LAY_SPAWN, SoundSource.BLOCKS, 1.0F, 1.0F);
               p_217487_.getBrain().eraseMemory(this.memoryModule);
               return;
            }
         }
      }

   }
}