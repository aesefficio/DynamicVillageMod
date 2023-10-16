package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class BackUpIfTooClose<E extends Mob> extends Behavior<E> {
   private final int tooCloseDistance;
   private final float strafeSpeed;

   public BackUpIfTooClose(int pTooCloseDistance, float pStrafeSpeed) {
      super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT));
      this.tooCloseDistance = pTooCloseDistance;
      this.strafeSpeed = pStrafeSpeed;
   }

   protected boolean checkExtraStartConditions(ServerLevel pLevel, E pOwner) {
      return this.isTargetVisible(pOwner) && this.isTargetTooClose(pOwner);
   }

   protected void start(ServerLevel pLevel, E pEntity, long pGameTime) {
      pEntity.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(this.getTarget(pEntity), true));
      pEntity.getMoveControl().strafe(-this.strafeSpeed, 0.0F);
      pEntity.setYRot(Mth.rotateIfNecessary(pEntity.getYRot(), pEntity.yHeadRot, 0.0F));
   }

   private boolean isTargetVisible(E pMob) {
      return pMob.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).get().contains(this.getTarget(pMob));
   }

   private boolean isTargetTooClose(E pMob) {
      return this.getTarget(pMob).closerThan(pMob, (double)this.tooCloseDistance);
   }

   private LivingEntity getTarget(E pMob) {
      return pMob.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).get();
   }
}