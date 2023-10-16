package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public class StrollToPoi extends Behavior<PathfinderMob> {
   private final MemoryModuleType<GlobalPos> memoryType;
   private final int closeEnoughDist;
   private final int maxDistanceFromPoi;
   private final float speedModifier;
   private long nextOkStartTime;

   public StrollToPoi(MemoryModuleType<GlobalPos> pMemoryType, float pSpeedModifier, int pCloseEnoughDist, int pMaxDistanceFromPoi) {
      super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED, pMemoryType, MemoryStatus.VALUE_PRESENT));
      this.memoryType = pMemoryType;
      this.speedModifier = pSpeedModifier;
      this.closeEnoughDist = pCloseEnoughDist;
      this.maxDistanceFromPoi = pMaxDistanceFromPoi;
   }

   protected boolean checkExtraStartConditions(ServerLevel pLevel, PathfinderMob pOwner) {
      Optional<GlobalPos> optional = pOwner.getBrain().getMemory(this.memoryType);
      return optional.isPresent() && pLevel.dimension() == optional.get().dimension() && optional.get().pos().closerToCenterThan(pOwner.position(), (double)this.maxDistanceFromPoi);
   }

   protected void start(ServerLevel pLevel, PathfinderMob pEntity, long pGameTime) {
      if (pGameTime > this.nextOkStartTime) {
         Brain<?> brain = pEntity.getBrain();
         Optional<GlobalPos> optional = brain.getMemory(this.memoryType);
         optional.ifPresent((p_24353_) -> {
            brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(p_24353_.pos(), this.speedModifier, this.closeEnoughDist));
         });
         this.nextOkStartTime = pGameTime + 80L;
      }

   }
}