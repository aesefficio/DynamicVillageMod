package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class StartAttacking<E extends Mob> extends Behavior<E> {
   private final Predicate<E> canAttackPredicate;
   private final Function<E, Optional<? extends LivingEntity>> targetFinderFunction;

   public StartAttacking(Predicate<E> pCanAttackPredicate, Function<E, Optional<? extends LivingEntity>> pTargetFinderFunction) {
      this(pCanAttackPredicate, pTargetFinderFunction, 60);
   }

   public StartAttacking(Predicate<E> p_217378_, Function<E, Optional<? extends LivingEntity>> p_217379_, int p_217380_) {
      super(ImmutableMap.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, MemoryStatus.REGISTERED), p_217380_);
      this.canAttackPredicate = p_217378_;
      this.targetFinderFunction = p_217379_;
   }

   public StartAttacking(Function<E, Optional<? extends LivingEntity>> pTargetFinderFunction) {
      this((p_24212_) -> {
         return true;
      }, pTargetFinderFunction);
   }

   protected boolean checkExtraStartConditions(ServerLevel pLevel, E pOwner) {
      if (!this.canAttackPredicate.test(pOwner)) {
         return false;
      } else {
         Optional<? extends LivingEntity> optional = this.targetFinderFunction.apply(pOwner);
         return optional.isPresent() ? pOwner.canAttack(optional.get()) : false;
      }
   }

   protected void start(ServerLevel pLevel, E pEntity, long pGameTime) {
      this.targetFinderFunction.apply(pEntity).ifPresent((p_24218_) -> {
         setAttackTarget(pEntity, p_24218_);
      });
   }

   public static <E extends Mob> void setAttackTarget(E p_24214_, LivingEntity pAttackTarget) {
      net.minecraftforge.event.entity.living.LivingChangeTargetEvent changeTargetEvent = net.minecraftforge.common.ForgeHooks.onLivingChangeTarget(p_24214_, pAttackTarget, net.minecraftforge.event.entity.living.LivingChangeTargetEvent.LivingTargetType.BEHAVIOR_TARGET);
      if(!changeTargetEvent.isCanceled()) {
          p_24214_.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, changeTargetEvent.getNewTarget());
          p_24214_.getBrain().eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
          net.minecraftforge.common.ForgeHooks.onLivingSetAttackTarget(p_24214_, changeTargetEvent.getNewTarget(), net.minecraftforge.event.entity.living.LivingChangeTargetEvent.LivingTargetType.BEHAVIOR_TARGET); // TODO: Remove in 1.20
      }
   }
}
