package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;

public class ResetProfession extends Behavior<Villager> {
   public ResetProfession() {
      super(ImmutableMap.of(MemoryModuleType.JOB_SITE, MemoryStatus.VALUE_ABSENT));
   }

   protected boolean checkExtraStartConditions(ServerLevel pLevel, Villager pOwner) {
      VillagerData villagerdata = pOwner.getVillagerData();
      return villagerdata.getProfession() != VillagerProfession.NONE && villagerdata.getProfession() != VillagerProfession.NITWIT && pOwner.getVillagerXp() == 0 && villagerdata.getLevel() <= 1;
   }

   protected void start(ServerLevel pLevel, Villager pEntity, long pGameTime) {
      pEntity.setVillagerData(pEntity.getVillagerData().setProfession(VillagerProfession.NONE));
      pEntity.refreshBrain(pLevel);
   }
}