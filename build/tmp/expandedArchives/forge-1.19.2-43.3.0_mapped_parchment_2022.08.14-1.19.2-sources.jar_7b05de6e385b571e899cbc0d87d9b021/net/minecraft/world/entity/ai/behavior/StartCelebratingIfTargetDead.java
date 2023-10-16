package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.function.BiPredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.level.GameRules;

public class StartCelebratingIfTargetDead extends Behavior<LivingEntity> {
   private final int celebrateDuration;
   private final BiPredicate<LivingEntity, LivingEntity> dancePredicate;

   public StartCelebratingIfTargetDead(int pCelebrateDuration, BiPredicate<LivingEntity, LivingEntity> pDancePredicate) {
      super(ImmutableMap.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT, MemoryModuleType.ANGRY_AT, MemoryStatus.REGISTERED, MemoryModuleType.CELEBRATE_LOCATION, MemoryStatus.VALUE_ABSENT, MemoryModuleType.DANCING, MemoryStatus.REGISTERED));
      this.celebrateDuration = pCelebrateDuration;
      this.dancePredicate = pDancePredicate;
   }

   protected boolean checkExtraStartConditions(ServerLevel pLevel, LivingEntity pOwner) {
      return this.getAttackTarget(pOwner).isDeadOrDying();
   }

   protected void start(ServerLevel pLevel, LivingEntity pEntity, long pGameTime) {
      LivingEntity livingentity = this.getAttackTarget(pEntity);
      if (this.dancePredicate.test(pEntity, livingentity)) {
         pEntity.getBrain().setMemoryWithExpiry(MemoryModuleType.DANCING, true, (long)this.celebrateDuration);
      }

      pEntity.getBrain().setMemoryWithExpiry(MemoryModuleType.CELEBRATE_LOCATION, livingentity.blockPosition(), (long)this.celebrateDuration);
      if (livingentity.getType() != EntityType.PLAYER || pLevel.getGameRules().getBoolean(GameRules.RULE_FORGIVE_DEAD_PLAYERS)) {
         pEntity.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
         pEntity.getBrain().eraseMemory(MemoryModuleType.ANGRY_AT);
      }

   }

   private LivingEntity getAttackTarget(LivingEntity pLivingEntity) {
      return pLivingEntity.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).get();
   }
}