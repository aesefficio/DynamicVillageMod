package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

public class UpdateActivityFromSchedule extends Behavior<LivingEntity> {
   public UpdateActivityFromSchedule() {
      super(ImmutableMap.of());
   }

   protected void start(ServerLevel pLevel, LivingEntity pEntity, long pGameTime) {
      pEntity.getBrain().updateActivityFromSchedule(pLevel.getDayTime(), pLevel.getGameTime());
   }
}