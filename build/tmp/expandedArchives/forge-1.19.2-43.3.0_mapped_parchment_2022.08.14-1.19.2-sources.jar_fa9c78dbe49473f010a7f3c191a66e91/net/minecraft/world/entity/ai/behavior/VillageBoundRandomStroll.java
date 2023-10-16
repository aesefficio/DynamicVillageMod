package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

public class VillageBoundRandomStroll extends Behavior<PathfinderMob> {
   private static final int MAX_XZ_DIST = 10;
   private static final int MAX_Y_DIST = 7;
   private final float speedModifier;
   private final int maxXyDist;
   private final int maxYDist;

   public VillageBoundRandomStroll(float pSpeedModifier) {
      this(pSpeedModifier, 10, 7);
   }

   public VillageBoundRandomStroll(float pSpeedModifier, int pMaxXyDist, int pMaxYDist) {
      super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT));
      this.speedModifier = pSpeedModifier;
      this.maxXyDist = pMaxXyDist;
      this.maxYDist = pMaxYDist;
   }

   protected void start(ServerLevel pLevel, PathfinderMob pEntity, long pGameTime) {
      BlockPos blockpos = pEntity.blockPosition();
      if (pLevel.isVillage(blockpos)) {
         this.setRandomPos(pEntity);
      } else {
         SectionPos sectionpos = SectionPos.of(blockpos);
         SectionPos sectionpos1 = BehaviorUtils.findSectionClosestToVillage(pLevel, sectionpos, 2);
         if (sectionpos1 != sectionpos) {
            this.setTargetedPos(pEntity, sectionpos1);
         } else {
            this.setRandomPos(pEntity);
         }
      }

   }

   private void setTargetedPos(PathfinderMob pPathfinder, SectionPos pSectionPos) {
      Optional<Vec3> optional = Optional.ofNullable(DefaultRandomPos.getPosTowards(pPathfinder, this.maxXyDist, this.maxYDist, Vec3.atBottomCenterOf(pSectionPos.center()), (double)((float)Math.PI / 2F)));
      pPathfinder.getBrain().setMemory(MemoryModuleType.WALK_TARGET, optional.map((p_24567_) -> {
         return new WalkTarget(p_24567_, this.speedModifier, 0);
      }));
   }

   private void setRandomPos(PathfinderMob pPathfinder) {
      Optional<Vec3> optional = Optional.ofNullable(LandRandomPos.getPos(pPathfinder, this.maxXyDist, this.maxYDist));
      pPathfinder.getBrain().setMemory(MemoryModuleType.WALK_TARGET, optional.map((p_24565_) -> {
         return new WalkTarget(p_24565_, this.speedModifier, 0);
      }));
   }
}