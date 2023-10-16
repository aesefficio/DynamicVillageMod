package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;

public class SetLookAndInteract extends Behavior<LivingEntity> {
   private final EntityType<?> type;
   private final int interactionRangeSqr;
   private final Predicate<LivingEntity> targetFilter;
   private final Predicate<LivingEntity> selfFilter;

   public SetLookAndInteract(EntityType<?> pType, int pInteractionRange, Predicate<LivingEntity> pSelfFilter, Predicate<LivingEntity> pTargetFilter) {
      super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.INTERACTION_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT));
      this.type = pType;
      this.interactionRangeSqr = pInteractionRange * pInteractionRange;
      this.targetFilter = pTargetFilter;
      this.selfFilter = pSelfFilter;
   }

   public SetLookAndInteract(EntityType<?> pType, int pInteractionRange) {
      this(pType, pInteractionRange, (p_23973_) -> {
         return true;
      }, (p_23971_) -> {
         return true;
      });
   }

   public boolean checkExtraStartConditions(ServerLevel pLevel, LivingEntity pOwner) {
      return this.selfFilter.test(pOwner) && this.getVisibleEntities(pOwner).contains(this::isMatchingTarget);
   }

   public void start(ServerLevel pLevel, LivingEntity pEntity, long pGameTime) {
      super.start(pLevel, pEntity, pGameTime);
      Brain<?> brain = pEntity.getBrain();
      brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).flatMap((p_186056_) -> {
         return p_186056_.findClosest((p_147899_) -> {
            return p_147899_.distanceToSqr(pEntity) <= (double)this.interactionRangeSqr && this.isMatchingTarget(p_147899_);
         });
      }).ifPresent((p_186059_) -> {
         brain.setMemory(MemoryModuleType.INTERACTION_TARGET, p_186059_);
         brain.setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(p_186059_, true));
      });
   }

   private boolean isMatchingTarget(LivingEntity p_23957_) {
      return this.type.equals(p_23957_.getType()) && this.targetFilter.test(p_23957_);
   }

   private NearestVisibleLivingEntities getVisibleEntities(LivingEntity pLivingEntity) {
      return pLivingEntity.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).get();
   }
}