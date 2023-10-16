package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

public class DoNothing extends Behavior<LivingEntity> {
   public DoNothing(int pMinDuration, int pMaxDuration) {
      super(ImmutableMap.of(), pMinDuration, pMaxDuration);
   }

   protected boolean canStillUse(ServerLevel pLevel, LivingEntity pEntity, long pGameTime) {
      return true;
   }
}