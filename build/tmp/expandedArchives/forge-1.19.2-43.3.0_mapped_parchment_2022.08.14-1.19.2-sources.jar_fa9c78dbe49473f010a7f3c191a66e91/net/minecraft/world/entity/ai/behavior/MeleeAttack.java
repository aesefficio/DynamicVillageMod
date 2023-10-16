package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ProjectileWeaponItem;

public class MeleeAttack extends Behavior<Mob> {
   private final int cooldownBetweenAttacks;

   public MeleeAttack(int pCooldownBetweenAttacks) {
      super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT, MemoryModuleType.ATTACK_COOLING_DOWN, MemoryStatus.VALUE_ABSENT));
      this.cooldownBetweenAttacks = pCooldownBetweenAttacks;
   }

   protected boolean checkExtraStartConditions(ServerLevel pLevel, Mob pOwner) {
      LivingEntity livingentity = this.getAttackTarget(pOwner);
      return !this.isHoldingUsableProjectileWeapon(pOwner) && BehaviorUtils.canSee(pOwner, livingentity) && pOwner.isWithinMeleeAttackRange(livingentity);
   }

   private boolean isHoldingUsableProjectileWeapon(Mob pMob) {
      return pMob.isHolding((p_147697_) -> {
         Item item = p_147697_.getItem();
         return item instanceof ProjectileWeaponItem && pMob.canFireProjectileWeapon((ProjectileWeaponItem)item);
      });
   }

   protected void start(ServerLevel pLevel, Mob pEntity, long pGameTime) {
      LivingEntity livingentity = this.getAttackTarget(pEntity);
      BehaviorUtils.lookAtEntity(pEntity, livingentity);
      pEntity.swing(InteractionHand.MAIN_HAND);
      pEntity.doHurtTarget(livingentity);
      pEntity.getBrain().setMemoryWithExpiry(MemoryModuleType.ATTACK_COOLING_DOWN, true, (long)this.cooldownBetweenAttacks);
   }

   private LivingEntity getAttackTarget(Mob pMob) {
      return pMob.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).get();
   }
}