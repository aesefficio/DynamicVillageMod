package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;

public class VillagerCalmDown extends Behavior<Villager> {
   private static final int SAFE_DISTANCE_FROM_DANGER = 36;

   public VillagerCalmDown() {
      super(ImmutableMap.of());
   }

   protected void start(ServerLevel pLevel, Villager pEntity, long pGameTime) {
      boolean flag = VillagerPanicTrigger.isHurt(pEntity) || VillagerPanicTrigger.hasHostile(pEntity) || isCloseToEntityThatHurtMe(pEntity);
      if (!flag) {
         pEntity.getBrain().eraseMemory(MemoryModuleType.HURT_BY);
         pEntity.getBrain().eraseMemory(MemoryModuleType.HURT_BY_ENTITY);
         pEntity.getBrain().updateActivityFromSchedule(pLevel.getDayTime(), pLevel.getGameTime());
      }

   }

   private static boolean isCloseToEntityThatHurtMe(Villager pVillager) {
      return pVillager.getBrain().getMemory(MemoryModuleType.HURT_BY_ENTITY).filter((p_24581_) -> {
         return p_24581_.distanceToSqr(pVillager) <= 36.0D;
      }).isPresent();
   }
}