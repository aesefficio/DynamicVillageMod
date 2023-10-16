package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.schedule.Activity;

public class WakeUp extends Behavior<LivingEntity> {
   public WakeUp() {
      super(ImmutableMap.of());
   }

   protected boolean checkExtraStartConditions(ServerLevel pLevel, LivingEntity pOwner) {
      return !pOwner.getBrain().isActive(Activity.REST) && pOwner.isSleeping();
   }

   protected void start(ServerLevel pLevel, LivingEntity pEntity, long pGameTime) {
      pEntity.stopSleeping();
   }
}