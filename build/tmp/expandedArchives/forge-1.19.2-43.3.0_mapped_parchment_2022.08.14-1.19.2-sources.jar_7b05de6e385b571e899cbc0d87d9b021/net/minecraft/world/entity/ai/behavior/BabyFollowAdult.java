package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.function.Function;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class BabyFollowAdult<E extends AgeableMob> extends Behavior<E> {
   private final UniformInt followRange;
   private final Function<LivingEntity, Float> speedModifier;

   public BabyFollowAdult(UniformInt pFollowRange, float pSpeedModifier) {
      this(pFollowRange, (p_147421_) -> {
         return pSpeedModifier;
      });
   }

   public BabyFollowAdult(UniformInt pFollowRange, Function<LivingEntity, Float> pSpeedModifier) {
      super(ImmutableMap.of(MemoryModuleType.NEAREST_VISIBLE_ADULT, MemoryStatus.VALUE_PRESENT, MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT));
      this.followRange = pFollowRange;
      this.speedModifier = pSpeedModifier;
   }

   protected boolean checkExtraStartConditions(ServerLevel pLevel, E pOwner) {
      if (!pOwner.isBaby()) {
         return false;
      } else {
         AgeableMob ageablemob = this.getNearestAdult(pOwner);
         return pOwner.closerThan(ageablemob, (double)(this.followRange.getMaxValue() + 1)) && !pOwner.closerThan(ageablemob, (double)this.followRange.getMinValue());
      }
   }

   protected void start(ServerLevel pLevel, E pEntity, long pGameTime) {
      BehaviorUtils.setWalkAndLookTargetMemories(pEntity, this.getNearestAdult(pEntity), this.speedModifier.apply(pEntity), this.followRange.getMinValue() - 1);
   }

   private AgeableMob getNearestAdult(E pEntity) {
      return pEntity.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_ADULT).get();
   }
}