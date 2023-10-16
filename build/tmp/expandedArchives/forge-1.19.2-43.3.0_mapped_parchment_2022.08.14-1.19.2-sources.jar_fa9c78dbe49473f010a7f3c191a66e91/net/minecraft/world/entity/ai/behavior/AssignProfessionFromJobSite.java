package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;

public class AssignProfessionFromJobSite extends Behavior<Villager> {
   public AssignProfessionFromJobSite() {
      super(ImmutableMap.of(MemoryModuleType.POTENTIAL_JOB_SITE, MemoryStatus.VALUE_PRESENT));
   }

   protected boolean checkExtraStartConditions(ServerLevel pLevel, Villager pOwner) {
      BlockPos blockpos = pOwner.getBrain().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE).get().pos();
      return blockpos.closerToCenterThan(pOwner.position(), 2.0D) || pOwner.assignProfessionWhenSpawned();
   }

   protected void start(ServerLevel pLevel, Villager pEntity, long pGameTime) {
      GlobalPos globalpos = pEntity.getBrain().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE).get();
      pEntity.getBrain().eraseMemory(MemoryModuleType.POTENTIAL_JOB_SITE);
      pEntity.getBrain().setMemory(MemoryModuleType.JOB_SITE, globalpos);
      pLevel.broadcastEntityEvent(pEntity, (byte)14);
      if (pEntity.getVillagerData().getProfession() == VillagerProfession.NONE) {
         MinecraftServer minecraftserver = pLevel.getServer();
         Optional.ofNullable(minecraftserver.getLevel(globalpos.dimension())).flatMap((p_22467_) -> {
            return p_22467_.getPoiManager().getType(globalpos.pos());
         }).flatMap((p_217122_) -> {
            return Registry.VILLAGER_PROFESSION.stream().filter((p_217125_) -> {
               return p_217125_.heldJobSite().test(p_217122_);
            }).findFirst();
         }).ifPresent((p_22464_) -> {
            pEntity.setVillagerData(pEntity.getVillagerData().setProfession(p_22464_));
            pEntity.refreshBrain(pLevel);
         });
      }
   }
}