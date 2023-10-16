package net.minecraft.world.entity.ai.behavior.warden;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.monster.warden.Warden;

public class SetRoarTarget<E extends Warden> extends Behavior<E> {
   private final Function<E, Optional<? extends LivingEntity>> targetFinderFunction;

   public SetRoarTarget(Function<E, Optional<? extends LivingEntity>> pTargetFinderFunction) {
      super(ImmutableMap.of(MemoryModuleType.ROAR_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, MemoryStatus.REGISTERED));
      this.targetFinderFunction = pTargetFinderFunction;
   }

   protected boolean checkExtraStartConditions(ServerLevel pLevel, E pOwner) {
      return this.targetFinderFunction.apply(pOwner).filter(pOwner::canTargetEntity).isPresent();
   }

   protected void start(ServerLevel pLevel, E pEntity, long pGameTime) {
      this.targetFinderFunction.apply(pEntity).ifPresent((p_217626_) -> {
         pEntity.getBrain().setMemory(MemoryModuleType.ROAR_TARGET, p_217626_);
         pEntity.getBrain().eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
      });
   }
}