package net.minecraft.world.entity.ai.behavior.warden;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.monster.warden.Warden;

public class SetWardenLookTarget extends Behavior<Warden> {
   public SetWardenLookTarget() {
      super(ImmutableMap.of(MemoryModuleType.DISTURBANCE_LOCATION, MemoryStatus.REGISTERED, MemoryModuleType.ROAR_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_ABSENT));
   }

   protected boolean checkExtraStartConditions(ServerLevel p_217636_, Warden p_217637_) {
      return p_217637_.getBrain().hasMemoryValue(MemoryModuleType.DISTURBANCE_LOCATION) || p_217637_.getBrain().hasMemoryValue(MemoryModuleType.ROAR_TARGET);
   }

   protected void start(ServerLevel p_217639_, Warden p_217640_, long p_217641_) {
      BlockPos blockpos = p_217640_.getBrain().getMemory(MemoryModuleType.ROAR_TARGET).map(Entity::blockPosition).or(() -> {
         return p_217640_.getBrain().getMemory(MemoryModuleType.DISTURBANCE_LOCATION);
      }).get();
      p_217640_.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(blockpos));
   }
}