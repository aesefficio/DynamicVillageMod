package net.minecraft.world.damagesource;

import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class EntityDamageSource extends DamageSource {
   protected final Entity entity;
   /** Whether this EntityDamageSource is from an entity wearing Thorns-enchanted armor. */
   private boolean isThorns;

   public EntityDamageSource(String pDamageTypeId, Entity pEntity) {
      super(pDamageTypeId);
      this.entity = pEntity;
   }

   /**
    * Sets this EntityDamageSource as originating from Thorns armor
    */
   public EntityDamageSource setThorns() {
      this.isThorns = true;
      return this;
   }

   public boolean isThorns() {
      return this.isThorns;
   }

   /**
    * Retrieves the true causer of the damage, e.g. the player who fired an arrow, the shulker who fired the bullet,
    * etc.
    */
   public Entity getEntity() {
      return this.entity;
   }

   /**
    * Gets the death message that is displayed when the player dies
    */
   public Component getLocalizedDeathMessage(LivingEntity pLivingEntity) {
      ItemStack itemstack = this.entity instanceof LivingEntity ? ((LivingEntity)this.entity).getMainHandItem() : ItemStack.EMPTY;
      String s = "death.attack." + this.msgId;
      return !itemstack.isEmpty() && itemstack.hasCustomHoverName() ? Component.translatable(s + ".item", pLivingEntity.getDisplayName(), this.entity.getDisplayName(), itemstack.getDisplayName()) : Component.translatable(s, pLivingEntity.getDisplayName(), this.entity.getDisplayName());
   }

   /**
    * Return whether this damage source will have its damage amount scaled based on the current difficulty.
    */
   public boolean scalesWithDifficulty() {
      return this.entity instanceof LivingEntity && !(this.entity instanceof Player);
   }

   /**
    * Gets the location from which the damage originates.
    */
   @Nullable
   public Vec3 getSourcePosition() {
      return this.entity.position();
   }

   public String toString() {
      return "EntityDamageSource (" + this.entity + ")";
   }
}