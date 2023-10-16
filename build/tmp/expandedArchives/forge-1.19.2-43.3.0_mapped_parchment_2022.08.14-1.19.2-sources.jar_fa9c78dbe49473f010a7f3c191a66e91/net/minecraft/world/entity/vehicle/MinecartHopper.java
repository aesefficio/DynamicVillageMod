package net.minecraft.world.entity.vehicle;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class MinecartHopper extends AbstractMinecartContainer implements Hopper {
   public static final int MOVE_ITEM_SPEED = 4;
   private boolean enabled = true;
   private int cooldownTime = -1;
   private final BlockPos lastPosition = BlockPos.ZERO;

   public MinecartHopper(EntityType<? extends MinecartHopper> pEntityType, Level pLevel) {
      super(pEntityType, pLevel);
   }

   public MinecartHopper(Level pLevel, double pX, double pY, double pZ) {
      super(EntityType.HOPPER_MINECART, pX, pY, pZ, pLevel);
   }

   public AbstractMinecart.Type getMinecartType() {
      return AbstractMinecart.Type.HOPPER;
   }

   public BlockState getDefaultDisplayBlockState() {
      return Blocks.HOPPER.defaultBlockState();
   }

   public int getDefaultDisplayOffset() {
      return 1;
   }

   /**
    * Returns the number of slots in the inventory.
    */
   public int getContainerSize() {
      return 5;
   }

   /**
    * Called every tick the minecart is on an activator rail.
    */
   public void activateMinecart(int pX, int pY, int pZ, boolean pReceivingPower) {
      boolean flag = !pReceivingPower;
      if (flag != this.isEnabled()) {
         this.setEnabled(flag);
      }

   }

   /**
    * Get whether this hopper minecart is being blocked by an activator rail.
    */
   public boolean isEnabled() {
      return this.enabled;
   }

   /**
    * Set whether this hopper minecart is being blocked by an activator rail.
    */
   public void setEnabled(boolean pEnabled) {
      this.enabled = pEnabled;
   }

   /**
    * Gets the world X position for this hopper entity.
    */
   public double getLevelX() {
      return this.getX();
   }

   /**
    * Gets the world Y position for this hopper entity.
    */
   public double getLevelY() {
      return this.getY() + 0.5D;
   }

   /**
    * Gets the world Z position for this hopper entity.
    */
   public double getLevelZ() {
      return this.getZ();
   }

   /**
    * Called to update the entity's position/logic.
    */
   public void tick() {
      super.tick();
      if (!this.level.isClientSide && this.isAlive() && this.isEnabled()) {
         BlockPos blockpos = this.blockPosition();
         if (blockpos.equals(this.lastPosition)) {
            --this.cooldownTime;
         } else {
            this.setCooldown(0);
         }

         if (!this.isOnCooldown()) {
            this.setCooldown(0);
            if (this.suckInItems()) {
               this.setCooldown(4);
               this.setChanged();
            }
         }
      }

   }

   public boolean suckInItems() {
      if (HopperBlockEntity.suckInItems(this.level, this)) {
         return true;
      } else {
         List<ItemEntity> list = this.level.getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate(0.25D, 0.0D, 0.25D), EntitySelector.ENTITY_STILL_ALIVE);
         if (!list.isEmpty()) {
            HopperBlockEntity.addItem(this, list.get(0));
         }

         return false;
      }
   }

   protected Item getDropItem() {
      return Items.HOPPER_MINECART;
   }

   protected void addAdditionalSaveData(CompoundTag pCompound) {
      super.addAdditionalSaveData(pCompound);
      pCompound.putInt("TransferCooldown", this.cooldownTime);
      pCompound.putBoolean("Enabled", this.enabled);
   }

   /**
    * (abstract) Protected helper method to read subclass entity data from NBT.
    */
   protected void readAdditionalSaveData(CompoundTag pCompound) {
      super.readAdditionalSaveData(pCompound);
      this.cooldownTime = pCompound.getInt("TransferCooldown");
      this.enabled = pCompound.contains("Enabled") ? pCompound.getBoolean("Enabled") : true;
   }

   /**
    * Sets the transfer ticker, used to determine the delay between transfers.
    */
   public void setCooldown(int pCooldownTime) {
      this.cooldownTime = pCooldownTime;
   }

   /**
    * Returns whether the hopper cart can currently transfer an item.
    */
   public boolean isOnCooldown() {
      return this.cooldownTime > 0;
   }

   public AbstractContainerMenu createMenu(int pId, Inventory pPlayerInventory) {
      return new HopperMenu(pId, pPlayerInventory, this);
   }
}