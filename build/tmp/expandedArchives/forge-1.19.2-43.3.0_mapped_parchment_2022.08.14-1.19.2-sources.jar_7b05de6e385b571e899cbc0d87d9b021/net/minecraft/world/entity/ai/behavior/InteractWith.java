package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public class InteractWith<E extends LivingEntity, T extends LivingEntity> extends Behavior<E> {
   private final int maxDist;
   private final float speedModifier;
   private final EntityType<? extends T> type;
   private final int interactionRangeSqr;
   private final Predicate<T> targetFilter;
   private final Predicate<E> selfFilter;
   private final MemoryModuleType<T> memory;

   public InteractWith(EntityType<? extends T> pType, int pInteractionRange, Predicate<E> pSelfFilter, Predicate<T> pTargetFilter, MemoryModuleType<T> pMemory, float pSpeedModifier, int pMaxDist) {
      super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT));
      this.type = pType;
      this.speedModifier = pSpeedModifier;
      this.interactionRangeSqr = pInteractionRange * pInteractionRange;
      this.maxDist = pMaxDist;
      this.targetFilter = pTargetFilter;
      this.selfFilter = pSelfFilter;
      this.memory = pMemory;
   }

   public static <T extends LivingEntity> InteractWith<LivingEntity, T> of(EntityType<? extends T> pType, int pInteractionRange, MemoryModuleType<T> pMemory, float pSpeedModifier, int pMaxDist) {
      return new InteractWith<>(pType, pInteractionRange, (p_23287_) -> {
         return true;
      }, (p_23285_) -> {
         return true;
      }, pMemory, pSpeedModifier, pMaxDist);
   }

   public static <T extends LivingEntity> InteractWith<LivingEntity, T> of(EntityType<? extends T> pType, int pInteractionRange, Predicate<T> pTargerFilter, MemoryModuleType<T> pMemory, float pSpeedModifier, int pMaxDist) {
      return new InteractWith<>(pType, pInteractionRange, (p_147584_) -> {
         return true;
      }, pTargerFilter, pMemory, pSpeedModifier, pMaxDist);
   }

   protected boolean checkExtraStartConditions(ServerLevel pLevel, E pOwner) {
      return this.selfFilter.test(pOwner) && this.seesAtLeastOneValidTarget(pOwner);
   }

   private boolean seesAtLeastOneValidTarget(E p_23267_) {
      NearestVisibleLivingEntities nearestvisiblelivingentities = p_23267_.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).get();
      return nearestvisiblelivingentities.contains(this::isTargetValid);
   }

   private boolean isTargetValid(LivingEntity p_23279_) {
      return this.type.equals(p_23279_.getType()) && this.targetFilter.test((T)p_23279_);
   }

   protected void start(ServerLevel pLevel, E pEntity, long pGameTime) {
      Brain<?> brain = pEntity.getBrain();
      Optional<NearestVisibleLivingEntities> optional = brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
      if (!optional.isEmpty()) {
         NearestVisibleLivingEntities nearestvisiblelivingentities = optional.get();
         nearestvisiblelivingentities.findClosest((p_186046_) -> {
            return this.canInteract(pEntity, p_186046_);
         }).ifPresent((p_186043_) -> {
            brain.setMemory(this.memory, (T)p_186043_);
            brain.setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(p_186043_, true));
            brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(new EntityTracker(p_186043_, false), this.speedModifier, this.maxDist));
         });
      }
   }

   private boolean canInteract(E p_186039_, LivingEntity p_186040_) {
      return this.type.equals(p_186040_.getType()) && p_186040_.distanceToSqr(p_186039_) <= (double)this.interactionRangeSqr && this.targetFilter.test((T)p_186040_);
   }
}