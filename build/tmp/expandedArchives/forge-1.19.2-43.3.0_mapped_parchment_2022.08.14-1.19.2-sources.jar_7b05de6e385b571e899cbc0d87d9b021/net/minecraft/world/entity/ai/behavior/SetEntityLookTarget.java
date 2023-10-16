package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;

public class SetEntityLookTarget extends Behavior<LivingEntity> {
   private final Predicate<LivingEntity> predicate;
   private final float maxDistSqr;
   private Optional<LivingEntity> nearestEntityMatchingTest = Optional.empty();

   public SetEntityLookTarget(TagKey<EntityType<?>> pPredicateTag, float pMaxDist) {
      this((p_204051_) -> {
         return p_204051_.getType().is(pPredicateTag);
      }, pMaxDist);
   }

   public SetEntityLookTarget(MobCategory pPredicateCategory, float pMaxDist) {
      this((p_23923_) -> {
         return pPredicateCategory.equals(p_23923_.getType().getCategory());
      }, pMaxDist);
   }

   public SetEntityLookTarget(EntityType<?> pPredicateType, float pMaxDist) {
      this((p_23911_) -> {
         return pPredicateType.equals(p_23911_.getType());
      }, pMaxDist);
   }

   public SetEntityLookTarget(float pMaxDist) {
      this((p_23913_) -> {
         return true;
      }, pMaxDist);
   }

   public SetEntityLookTarget(Predicate<LivingEntity> pPredicate, float pMaxDist) {
      super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT));
      this.predicate = pPredicate;
      this.maxDistSqr = pMaxDist * pMaxDist;
   }

   protected boolean checkExtraStartConditions(ServerLevel pLevel, LivingEntity pOwner) {
      NearestVisibleLivingEntities nearestvisiblelivingentities = pOwner.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).get();
      this.nearestEntityMatchingTest = nearestvisiblelivingentities.findClosest(this.predicate.and((p_186053_) -> {
         return p_186053_.distanceToSqr(pOwner) <= (double)this.maxDistSqr;
      }));
      return this.nearestEntityMatchingTest.isPresent();
   }

   protected void start(ServerLevel pLevel, LivingEntity pEntity, long pGameTime) {
      pEntity.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(this.nearestEntityMatchingTest.get(), true));
      this.nearestEntityMatchingTest = Optional.empty();
   }
}