package net.minecraft.world.entity;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public interface ItemSteerable {
   boolean boost();

   void travelWithInput(Vec3 pTravelVector);

   float getSteeringSpeed();

   default boolean travel(Mob pVehicle, ItemBasedSteering pHelper, Vec3 pTravelVec) {
      if (!pVehicle.isAlive()) {
         return false;
      } else {
         Entity entity = pVehicle.getControllingPassenger();
         if (pVehicle.isVehicle() && entity instanceof Player) {
            pVehicle.setYRot(entity.getYRot());
            pVehicle.yRotO = pVehicle.getYRot();
            pVehicle.setXRot(entity.getXRot() * 0.5F);
            pVehicle.setRot(pVehicle.getYRot(), pVehicle.getXRot());
            pVehicle.yBodyRot = pVehicle.getYRot();
            pVehicle.yHeadRot = pVehicle.getYRot();
            pVehicle.maxUpStep = 1.0F;
            pVehicle.flyingSpeed = pVehicle.getSpeed() * 0.1F;
            if (pHelper.boosting && pHelper.boostTime++ > pHelper.boostTimeTotal) {
               pHelper.boosting = false;
            }

            if (pVehicle.isControlledByLocalInstance()) {
               float f = this.getSteeringSpeed();
               if (pHelper.boosting) {
                  f += f * 1.15F * Mth.sin((float)pHelper.boostTime / (float)pHelper.boostTimeTotal * (float)Math.PI);
               }

               pVehicle.setSpeed(f);
               this.travelWithInput(new Vec3(0.0D, 0.0D, 1.0D));
               pVehicle.lerpSteps = 0;
            } else {
               pVehicle.calculateEntityAnimation(pVehicle, false);
               pVehicle.setDeltaMovement(Vec3.ZERO);
            }

            pVehicle.tryCheckInsideBlocks();
            return true;
         } else {
            pVehicle.maxUpStep = 0.5F;
            pVehicle.flyingSpeed = 0.02F;
            this.travelWithInput(pTravelVec);
            return false;
         }
      }
   }
}