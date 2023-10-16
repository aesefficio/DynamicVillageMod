package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.function.Function;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public class SetWalkTargetFromAttackTargetIfTargetOutOfReach extends Behavior<Mob> {
   private static final int PROJECTILE_ATTACK_RANGE_BUFFER = 1;
   private final Function<LivingEntity, Float> speedModifier;

   public SetWalkTargetFromAttackTargetIfTargetOutOfReach(float pSpeedModifier) {
      this((p_147908_) -> {
         return pSpeedModifier;
      });
   }

   public SetWalkTargetFromAttackTargetIfTargetOutOfReach(Function<LivingEntity, Float> pSpeedModifier) {
      super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryStatus.REGISTERED));
      this.speedModifier = pSpeedModifier;
   }

   protected void start(ServerLevel pLevel, Mob pEntity, long pGameTime) {
      LivingEntity livingentity = pEntity.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).get();
      if (BehaviorUtils.canSee(pEntity, livingentity) && BehaviorUtils.isWithinAttackRange(pEntity, livingentity, 1)) {
         this.clearWalkTarget(pEntity);
      } else {
         this.setWalkAndLookTarget(pEntity, livingentity);
      }

   }

   private void setWalkAndLookTarget(LivingEntity pEntity, LivingEntity pTarget) {
      Brain<?> brain = pEntity.getBrain();
      brain.setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(pTarget, true));
      WalkTarget walktarget = new WalkTarget(new EntityTracker(pTarget, false), this.speedModifier.apply(pEntity), 0);
      brain.setMemory(MemoryModuleType.WALK_TARGET, walktarget);
   }

   private void clearWalkTarget(LivingEntity p_24036_) {
      p_24036_.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
   }
}