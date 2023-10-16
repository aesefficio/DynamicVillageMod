package net.minecraft.world.entity.animal.horse;

import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;

public class Donkey extends AbstractChestedHorse {
   public Donkey(EntityType<? extends Donkey> pEntityType, Level pLevel) {
      super(pEntityType, pLevel);
   }

   protected SoundEvent getAmbientSound() {
      super.getAmbientSound();
      return SoundEvents.DONKEY_AMBIENT;
   }

   protected SoundEvent getAngrySound() {
      super.getAngrySound();
      return SoundEvents.DONKEY_ANGRY;
   }

   protected SoundEvent getDeathSound() {
      super.getDeathSound();
      return SoundEvents.DONKEY_DEATH;
   }

   @Nullable
   protected SoundEvent getEatingSound() {
      return SoundEvents.DONKEY_EAT;
   }

   protected SoundEvent getHurtSound(DamageSource pDamageSource) {
      super.getHurtSound(pDamageSource);
      return SoundEvents.DONKEY_HURT;
   }

   /**
    * Returns true if the mob is currently able to mate with the specified mob.
    */
   public boolean canMate(Animal pOtherAnimal) {
      if (pOtherAnimal == this) {
         return false;
      } else if (!(pOtherAnimal instanceof Donkey) && !(pOtherAnimal instanceof Horse)) {
         return false;
      } else {
         return this.canParent() && ((AbstractHorse)pOtherAnimal).canParent();
      }
   }

   public AgeableMob getBreedOffspring(ServerLevel pLevel, AgeableMob pOtherParent) {
      EntityType<? extends AbstractHorse> entitytype = pOtherParent instanceof Horse ? EntityType.MULE : EntityType.DONKEY;
      AbstractHorse abstracthorse = entitytype.create(pLevel);
      this.setOffspringAttributes(pOtherParent, abstracthorse);
      return abstracthorse;
   }
}