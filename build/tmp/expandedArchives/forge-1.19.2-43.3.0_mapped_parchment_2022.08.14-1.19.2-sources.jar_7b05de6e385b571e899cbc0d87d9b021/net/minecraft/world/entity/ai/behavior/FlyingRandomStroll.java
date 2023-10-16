package net.minecraft.world.entity.ai.behavior;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.AirAndWaterRandomPos;
import net.minecraft.world.phys.Vec3;

public class FlyingRandomStroll extends RandomStroll {
   public FlyingRandomStroll(float p_217182_) {
      this(p_217182_, true);
   }

   public FlyingRandomStroll(float p_217184_, boolean p_217185_) {
      super(p_217184_, p_217185_);
   }

   protected Vec3 getTargetPos(PathfinderMob p_217187_) {
      Vec3 vec3 = p_217187_.getViewVector(0.0F);
      return AirAndWaterRandomPos.getPos(p_217187_, this.maxHorizontalDistance, this.maxVerticalDistance, -2, vec3.x, vec3.z, (double)((float)Math.PI / 2F));
   }
}