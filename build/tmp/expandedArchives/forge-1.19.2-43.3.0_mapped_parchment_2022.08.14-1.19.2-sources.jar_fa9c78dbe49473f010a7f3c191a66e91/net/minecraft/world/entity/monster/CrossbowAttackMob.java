package net.minecraft.world.entity.monster;

import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import javax.annotation.Nullable;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public interface CrossbowAttackMob extends RangedAttackMob {
   void setChargingCrossbow(boolean pChargingCrossbow);

   void shootCrossbowProjectile(LivingEntity pTarget, ItemStack pCrossbowStack, Projectile pProjectile, float pProjectileAngle);

   /**
    * Gets the active target the Goal system uses for tracking
    */
   @Nullable
   LivingEntity getTarget();

   void onCrossbowAttackPerformed();

   default void performCrossbowAttack(LivingEntity pUser, float pVelocity) {
      InteractionHand interactionhand = ProjectileUtil.getWeaponHoldingHand(pUser, item -> item instanceof CrossbowItem);
      ItemStack itemstack = pUser.getItemInHand(interactionhand);
      if (pUser.isHolding(is -> is.getItem() instanceof CrossbowItem)) {
         CrossbowItem.performShooting(pUser.level, pUser, interactionhand, itemstack, pVelocity, (float)(14 - pUser.level.getDifficulty().getId() * 4));
      }

      this.onCrossbowAttackPerformed();
   }

   default void shootCrossbowProjectile(LivingEntity pUser, LivingEntity pTarget, Projectile pProjectile, float pProjectileAngle, float pVelocity) {
      double d0 = pTarget.getX() - pUser.getX();
      double d1 = pTarget.getZ() - pUser.getZ();
      double d2 = Math.sqrt(d0 * d0 + d1 * d1);
      double d3 = pTarget.getY(0.3333333333333333D) - pProjectile.getY() + d2 * (double)0.2F;
      Vector3f vector3f = this.getProjectileShotVector(pUser, new Vec3(d0, d3, d1), pProjectileAngle);
      pProjectile.shoot((double)vector3f.x(), (double)vector3f.y(), (double)vector3f.z(), pVelocity, (float)(14 - pUser.level.getDifficulty().getId() * 4));
      pUser.playSound(SoundEvents.CROSSBOW_SHOOT, 1.0F, 1.0F / (pUser.getRandom().nextFloat() * 0.4F + 0.8F));
   }

   default Vector3f getProjectileShotVector(LivingEntity pUser, Vec3 pVectorTowardTarget, float pProjectileAngle) {
      Vec3 vec3 = pVectorTowardTarget.normalize();
      Vec3 vec31 = vec3.cross(new Vec3(0.0D, 1.0D, 0.0D));
      if (vec31.lengthSqr() <= 1.0E-7D) {
         vec31 = vec3.cross(pUser.getUpVector(1.0F));
      }

      Quaternion quaternion = new Quaternion(new Vector3f(vec31), 90.0F, true);
      Vector3f vector3f = new Vector3f(vec3);
      vector3f.transform(quaternion);
      Quaternion quaternion1 = new Quaternion(vector3f, pProjectileAngle, true);
      Vector3f vector3f1 = new Vector3f(vec3);
      vector3f1.transform(quaternion1);
      return vector3f1;
   }
}
