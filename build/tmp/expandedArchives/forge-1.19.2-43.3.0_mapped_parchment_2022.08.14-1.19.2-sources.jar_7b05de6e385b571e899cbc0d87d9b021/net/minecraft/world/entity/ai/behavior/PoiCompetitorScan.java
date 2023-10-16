package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;

public class PoiCompetitorScan extends Behavior<Villager> {
   final VillagerProfession profession;

   public PoiCompetitorScan(VillagerProfession pProfession) {
      super(ImmutableMap.of(MemoryModuleType.JOB_SITE, MemoryStatus.VALUE_PRESENT, MemoryModuleType.NEAREST_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT));
      this.profession = pProfession;
   }

   protected void start(ServerLevel pLevel, Villager pEntity, long pGameTime) {
      GlobalPos globalpos = pEntity.getBrain().getMemory(MemoryModuleType.JOB_SITE).get();
      pLevel.getPoiManager().getType(globalpos.pos()).ifPresent((p_217328_) -> {
         BehaviorUtils.getNearbyVillagersWithCondition(pEntity, (p_217339_) -> {
            return this.competesForSameJobsite(globalpos, p_217328_, p_217339_);
         }).reduce(pEntity, PoiCompetitorScan::selectWinner);
      });
   }

   private static Villager selectWinner(Villager p_23725_, Villager p_23726_) {
      Villager villager;
      Villager villager1;
      if (p_23725_.getVillagerXp() > p_23726_.getVillagerXp()) {
         villager = p_23725_;
         villager1 = p_23726_;
      } else {
         villager = p_23726_;
         villager1 = p_23725_;
      }

      villager1.getBrain().eraseMemory(MemoryModuleType.JOB_SITE);
      return villager;
   }

   private boolean competesForSameJobsite(GlobalPos pJobSitePos, Holder<PoiType> pPoiType, Villager pVillager) {
      return this.hasJobSite(pVillager) && pJobSitePos.equals(pVillager.getBrain().getMemory(MemoryModuleType.JOB_SITE).get()) && this.hasMatchingProfession(pPoiType, pVillager.getVillagerData().getProfession());
   }

   private boolean hasMatchingProfession(Holder<PoiType> pPoiType, VillagerProfession pProfession) {
      return pProfession.heldJobSite().test(pPoiType);
   }

   private boolean hasJobSite(Villager pVillager) {
      return pVillager.getBrain().getMemory(MemoryModuleType.JOB_SITE).isPresent();
   }
}