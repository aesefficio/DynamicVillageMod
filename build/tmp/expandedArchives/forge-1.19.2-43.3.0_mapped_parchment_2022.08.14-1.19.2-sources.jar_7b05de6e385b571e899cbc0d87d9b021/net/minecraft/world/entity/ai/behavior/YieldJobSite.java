package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.pathfinder.Path;

public class YieldJobSite extends Behavior<Villager> {
   private final float speedModifier;

   public YieldJobSite(float pSpeedModifier) {
      super(ImmutableMap.of(MemoryModuleType.POTENTIAL_JOB_SITE, MemoryStatus.VALUE_PRESENT, MemoryModuleType.JOB_SITE, MemoryStatus.VALUE_ABSENT, MemoryModuleType.NEAREST_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT));
      this.speedModifier = pSpeedModifier;
   }

   protected boolean checkExtraStartConditions(ServerLevel pLevel, Villager pOwner) {
      if (pOwner.isBaby()) {
         return false;
      } else {
         return pOwner.getVillagerData().getProfession() == VillagerProfession.NONE;
      }
   }

   protected void start(ServerLevel pLevel, Villager pEntity, long pGameTime) {
      BlockPos blockpos = pEntity.getBrain().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE).get().pos();
      Optional<Holder<PoiType>> optional = pLevel.getPoiManager().getType(blockpos);
      if (optional.isPresent()) {
         BehaviorUtils.getNearbyVillagersWithCondition(pEntity, (p_24874_) -> {
            return this.nearbyWantsJobsite(optional.get(), p_24874_, blockpos);
         }).findFirst().ifPresent((p_24860_) -> {
            this.yieldJobSite(pLevel, pEntity, p_24860_, blockpos, p_24860_.getBrain().getMemory(MemoryModuleType.JOB_SITE).isPresent());
         });
      }
   }

   private boolean nearbyWantsJobsite(Holder<PoiType> pPoiType, Villager pVillager, BlockPos pPotentialJobSitePos) {
      boolean flag = pVillager.getBrain().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE).isPresent();
      if (flag) {
         return false;
      } else {
         Optional<GlobalPos> optional = pVillager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
         VillagerProfession villagerprofession = pVillager.getVillagerData().getProfession();
         if (villagerprofession.heldJobSite().test(pPoiType)) {
            return !optional.isPresent() ? this.canReachPos(pVillager, pPotentialJobSitePos, pPoiType.value()) : optional.get().pos().equals(pPotentialJobSitePos);
         } else {
            return false;
         }
      }
   }

   private void yieldJobSite(ServerLevel pLevel, Villager pJobSiteLoser, Villager pJobSiteWinner, BlockPos pPotentialJobSitePos, boolean pHasJobSite) {
      this.eraseMemories(pJobSiteLoser);
      if (!pHasJobSite) {
         BehaviorUtils.setWalkAndLookTargetMemories(pJobSiteWinner, pPotentialJobSitePos, this.speedModifier, 1);
         pJobSiteWinner.getBrain().setMemory(MemoryModuleType.POTENTIAL_JOB_SITE, GlobalPos.of(pLevel.dimension(), pPotentialJobSitePos));
         DebugPackets.sendPoiTicketCountPacket(pLevel, pPotentialJobSitePos);
      }

   }

   private boolean canReachPos(Villager pVillager, BlockPos pPos, PoiType pPoiType) {
      Path path = pVillager.getNavigation().createPath(pPos, pPoiType.validRange());
      return path != null && path.canReach();
   }

   private void eraseMemories(Villager pVillager) {
      pVillager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
      pVillager.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
      pVillager.getBrain().eraseMemory(MemoryModuleType.POTENTIAL_JOB_SITE);
   }
}