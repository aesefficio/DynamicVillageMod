package net.minecraft.world.entity.ai.behavior;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class StayCloseToTarget<E extends LivingEntity> extends Behavior<E> {
   private final Function<LivingEntity, Optional<PositionTracker>> targetPositionGetter;
   private final int closeEnough;
   private final int tooFar;
   private final float speedModifier;

   public StayCloseToTarget(Function<LivingEntity, Optional<PositionTracker>> p_217386_, int p_217387_, int p_217388_, float p_217389_) {
      super(Map.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT));
      this.targetPositionGetter = p_217386_;
      this.closeEnough = p_217387_;
      this.tooFar = p_217388_;
      this.speedModifier = p_217389_;
   }

   protected boolean checkExtraStartConditions(ServerLevel p_217391_, E p_217392_) {
      Optional<PositionTracker> optional = this.targetPositionGetter.apply(p_217392_);
      if (optional.isEmpty()) {
         return false;
      } else {
         PositionTracker positiontracker = optional.get();
         return !p_217392_.position().closerThan(positiontracker.currentPosition(), (double)this.tooFar);
      }
   }

   protected void start(ServerLevel p_217394_, E p_217395_, long p_217396_) {
      BehaviorUtils.setWalkAndLookTargetMemories(p_217395_, this.targetPositionGetter.apply(p_217395_).get(), this.speedModifier, this.closeEnough);
   }
}