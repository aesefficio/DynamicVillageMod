package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.Villager;

public class StrollToPoiList extends Behavior<Villager> {
   private final MemoryModuleType<List<GlobalPos>> strollToMemoryType;
   private final MemoryModuleType<GlobalPos> mustBeCloseToMemoryType;
   private final float speedModifier;
   private final int closeEnoughDist;
   private final int maxDistanceFromPoi;
   private long nextOkStartTime;
   @Nullable
   private GlobalPos targetPos;

   public StrollToPoiList(MemoryModuleType<List<GlobalPos>> pStrollMemoryType, float pSpeedModifier, int pCloseEnoughDist, int pMaxDistanceFromPoi, MemoryModuleType<GlobalPos> pMustBeCloseToMemoryType) {
      super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED, pStrollMemoryType, MemoryStatus.VALUE_PRESENT, pMustBeCloseToMemoryType, MemoryStatus.VALUE_PRESENT));
      this.strollToMemoryType = pStrollMemoryType;
      this.speedModifier = pSpeedModifier;
      this.closeEnoughDist = pCloseEnoughDist;
      this.maxDistanceFromPoi = pMaxDistanceFromPoi;
      this.mustBeCloseToMemoryType = pMustBeCloseToMemoryType;
   }

   protected boolean checkExtraStartConditions(ServerLevel pLevel, Villager pOwner) {
      Optional<List<GlobalPos>> optional = pOwner.getBrain().getMemory(this.strollToMemoryType);
      Optional<GlobalPos> optional1 = pOwner.getBrain().getMemory(this.mustBeCloseToMemoryType);
      if (optional.isPresent() && optional1.isPresent()) {
         List<GlobalPos> list = optional.get();
         if (!list.isEmpty()) {
            this.targetPos = list.get(pLevel.getRandom().nextInt(list.size()));
            return this.targetPos != null && pLevel.dimension() == this.targetPos.dimension() && optional1.get().pos().closerToCenterThan(pOwner.position(), (double)this.maxDistanceFromPoi);
         }
      }

      return false;
   }

   protected void start(ServerLevel pLevel, Villager pEntity, long pGameTime) {
      if (pGameTime > this.nextOkStartTime && this.targetPos != null) {
         pEntity.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(this.targetPos.pos(), this.speedModifier, this.closeEnoughDist));
         this.nextOkStartTime = pGameTime + 100L;
      }

   }
}