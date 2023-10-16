package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class StopAttackingIfTargetInvalid<E extends Mob> extends Behavior<E> {
   private static final int TIMEOUT_TO_GET_WITHIN_ATTACK_RANGE = 200;
   private final Predicate<LivingEntity> stopAttackingWhen;
   private final BiConsumer<E, LivingEntity> onTargetErased;
   private final boolean canGrowTiredOfTryingToReachTarget;

   public StopAttackingIfTargetInvalid(Predicate<LivingEntity> pStopAttackingWhen, BiConsumer<E, LivingEntity> pOnTargetErased, boolean pCanGrowTiredOfTryingToReachTarget) {
      super(ImmutableMap.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT, MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, MemoryStatus.REGISTERED));
      this.stopAttackingWhen = pStopAttackingWhen;
      this.onTargetErased = pOnTargetErased;
      this.canGrowTiredOfTryingToReachTarget = pCanGrowTiredOfTryingToReachTarget;
   }

   public StopAttackingIfTargetInvalid(Predicate<LivingEntity> pStopAttackingWhen, BiConsumer<E, LivingEntity> pOnTargetErased) {
      this(pStopAttackingWhen, pOnTargetErased, true);
   }

   public StopAttackingIfTargetInvalid(Predicate<LivingEntity> pStopAttackingWhen) {
      this(pStopAttackingWhen, (p_217411_, p_217412_) -> {
      });
   }

   public StopAttackingIfTargetInvalid(BiConsumer<E, LivingEntity> pOnTargetErased) {
      this((p_147988_) -> {
         return false;
      }, pOnTargetErased);
   }

   public StopAttackingIfTargetInvalid() {
      this((p_147986_) -> {
         return false;
      }, (p_217408_, p_217409_) -> {
      });
   }

   protected void start(ServerLevel pLevel, E pEntity, long pGameTime) {
      LivingEntity livingentity = this.getAttackTarget(pEntity);
      if (!pEntity.canAttack(livingentity)) {
         this.clearAttackTarget(pEntity);
      } else if (this.canGrowTiredOfTryingToReachTarget && isTiredOfTryingToReachTarget(pEntity)) {
         this.clearAttackTarget(pEntity);
      } else if (this.isCurrentTargetDeadOrRemoved(pEntity)) {
         this.clearAttackTarget(pEntity);
      } else if (this.isCurrentTargetInDifferentLevel(pEntity)) {
         this.clearAttackTarget(pEntity);
      } else if (this.stopAttackingWhen.test(this.getAttackTarget(pEntity))) {
         this.clearAttackTarget(pEntity);
      }
   }

   private boolean isCurrentTargetInDifferentLevel(E pMemoryHolder) {
      return this.getAttackTarget(pMemoryHolder).level != pMemoryHolder.level;
   }

   private LivingEntity getAttackTarget(E pMemoryHolder) {
      return pMemoryHolder.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).get();
   }

   private static <E extends LivingEntity> boolean isTiredOfTryingToReachTarget(E pMemoryHolder) {
      Optional<Long> optional = pMemoryHolder.getBrain().getMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
      return optional.isPresent() && pMemoryHolder.level.getGameTime() - optional.get() > 200L;
   }

   private boolean isCurrentTargetDeadOrRemoved(E pMemoryHolder) {
      Optional<LivingEntity> optional = pMemoryHolder.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
      return optional.isPresent() && !optional.get().isAlive();
   }

   protected void clearAttackTarget(E pMemoryHolder) {
      this.onTargetErased.accept(pMemoryHolder, this.getAttackTarget(pMemoryHolder));
      pMemoryHolder.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
   }
}