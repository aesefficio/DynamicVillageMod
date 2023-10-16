package net.minecraft.world.damagesource;

import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class IndirectEntityDamageSource extends EntityDamageSource {
   @Nullable
   private final Entity owner;

   public IndirectEntityDamageSource(String pDamageTypeId, Entity pSource, @Nullable Entity pIndirectEntity) {
      super(pDamageTypeId, pSource);
      this.owner = pIndirectEntity;
   }

   /**
    * Retrieves the immediate causer of the damage, e.g. the arrow entity, not its shooter
    */
   @Nullable
   public Entity getDirectEntity() {
      return this.entity;
   }

   /**
    * Retrieves the true causer of the damage, e.g. the player who fired an arrow, the shulker who fired the bullet,
    * etc.
    */
   @Nullable
   public Entity getEntity() {
      return this.owner;
   }

   /**
    * Gets the death message that is displayed when the player dies
    */
   public Component getLocalizedDeathMessage(LivingEntity pLivingEntity) {
      Component component = this.owner == null ? this.entity.getDisplayName() : this.owner.getDisplayName();
      ItemStack itemstack = this.owner instanceof LivingEntity ? ((LivingEntity)this.owner).getMainHandItem() : ItemStack.EMPTY;
      String s = "death.attack." + this.msgId;
      String s1 = s + ".item";
      return !itemstack.isEmpty() && itemstack.hasCustomHoverName() ? Component.translatable(s1, pLivingEntity.getDisplayName(), component, itemstack.getDisplayName()) : Component.translatable(s, pLivingEntity.getDisplayName(), component);
   }
}