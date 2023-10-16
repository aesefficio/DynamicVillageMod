package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.schedule.Activity;

public class ResetRaidStatus extends Behavior<LivingEntity> {
   public ResetRaidStatus() {
      super(ImmutableMap.of());
   }

   protected boolean checkExtraStartConditions(ServerLevel pLevel, LivingEntity pOwner) {
      return pLevel.random.nextInt(20) == 0;
   }

   protected void start(ServerLevel pLevel, LivingEntity pEntity, long pGameTime) {
      Brain<?> brain = pEntity.getBrain();
      Raid raid = pLevel.getRaidAt(pEntity.blockPosition());
      if (raid == null || raid.isStopped() || raid.isLoss()) {
         brain.setDefaultActivity(Activity.IDLE);
         brain.updateActivityFromSchedule(pLevel.getDayTime(), pLevel.getGameTime());
      }

   }
}