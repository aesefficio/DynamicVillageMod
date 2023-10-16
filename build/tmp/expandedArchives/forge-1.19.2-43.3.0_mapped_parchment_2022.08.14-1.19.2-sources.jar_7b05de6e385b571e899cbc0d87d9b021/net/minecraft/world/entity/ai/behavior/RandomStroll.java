package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

public class RandomStroll extends Behavior<PathfinderMob> {
   private static final int MAX_XZ_DIST = 10;
   private static final int MAX_Y_DIST = 7;
   private final float speedModifier;
   protected final int maxHorizontalDistance;
   protected final int maxVerticalDistance;
   private final boolean mayStrollFromWater;

   public RandomStroll(float pSpeedModifier) {
      this(pSpeedModifier, true);
   }

   public RandomStroll(float pSpeedModifier, boolean pMayStrollFromWater) {
      this(pSpeedModifier, 10, 7, pMayStrollFromWater);
   }

   public RandomStroll(float pSpeedModifier, int pMaxHorizontalDistance, int pMaxVerticalDistance) {
      this(pSpeedModifier, pMaxHorizontalDistance, pMaxVerticalDistance, true);
   }

   public RandomStroll(float pSpeedModifier, int pMaxHorizontalDistance, int pMaxVerticalDistance, boolean pMayStrollFromWater) {
      super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT));
      this.speedModifier = pSpeedModifier;
      this.maxHorizontalDistance = pMaxHorizontalDistance;
      this.maxVerticalDistance = pMaxVerticalDistance;
      this.mayStrollFromWater = pMayStrollFromWater;
   }

   protected boolean checkExtraStartConditions(ServerLevel pLevel, PathfinderMob pOwner) {
      return this.mayStrollFromWater || !pOwner.isInWaterOrBubble();
   }

   protected void start(ServerLevel pLevel, PathfinderMob pEntity, long pGameTime) {
      Optional<Vec3> optional = Optional.ofNullable(this.getTargetPos(pEntity));
      pEntity.getBrain().setMemory(MemoryModuleType.WALK_TARGET, optional.map((p_23758_) -> {
         return new WalkTarget(p_23758_, this.speedModifier, 0);
      }));
   }

   @Nullable
   protected Vec3 getTargetPos(PathfinderMob pPathfinder) {
      return LandRandomPos.getPos(pPathfinder, this.maxHorizontalDistance, this.maxVerticalDistance);
   }
}