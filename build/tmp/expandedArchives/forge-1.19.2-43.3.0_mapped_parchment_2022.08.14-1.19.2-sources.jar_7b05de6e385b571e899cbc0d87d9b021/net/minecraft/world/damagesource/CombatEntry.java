package net.minecraft.world.damagesource;

import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class CombatEntry {
   private final DamageSource source;
   private final int time;
   private final float damage;
   private final float health;
   @Nullable
   private final String location;
   private final float fallDistance;

   public CombatEntry(DamageSource pSource, int pTime, float pHealth, float pDamage, @Nullable String pLocation, float pFallDistance) {
      this.source = pSource;
      this.time = pTime;
      this.damage = pDamage;
      this.health = pHealth;
      this.location = pLocation;
      this.fallDistance = pFallDistance;
   }

   /**
    * Get the DamageSource of the CombatEntry instance.
    */
   public DamageSource getSource() {
      return this.source;
   }

   public int getTime() {
      return this.time;
   }

   public float getDamage() {
      return this.damage;
   }

   public float getHealthBeforeDamage() {
      return this.health;
   }

   public float getHealthAfterDamage() {
      return this.health - this.damage;
   }

   /**
    * Returns true if {@link net.minecraft.util.DamageSource#getEntity() damage source} is a living entity
    */
   public boolean isCombatRelated() {
      return this.source.getEntity() instanceof LivingEntity;
   }

   @Nullable
   public String getLocation() {
      return this.location;
   }

   @Nullable
   public Component getAttackerName() {
      return this.getSource().getEntity() == null ? null : this.getSource().getEntity().getDisplayName();
   }

   @Nullable
   public Entity getAttacker() {
      return this.getSource().getEntity();
   }

   public float getFallDistance() {
      return this.source == DamageSource.OUT_OF_WORLD ? Float.MAX_VALUE : this.fallDistance;
   }
}