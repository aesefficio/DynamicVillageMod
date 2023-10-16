package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;

public class ShowTradesToPlayer extends Behavior<Villager> {
   private static final int MAX_LOOK_TIME = 900;
   private static final int STARTING_LOOK_TIME = 40;
   @Nullable
   private ItemStack playerItemStack;
   private final List<ItemStack> displayItems = Lists.newArrayList();
   private int cycleCounter;
   private int displayIndex;
   private int lookTime;

   public ShowTradesToPlayer(int pMinDuration, int pMaxDuration) {
      super(ImmutableMap.of(MemoryModuleType.INTERACTION_TARGET, MemoryStatus.VALUE_PRESENT), pMinDuration, pMaxDuration);
   }

   public boolean checkExtraStartConditions(ServerLevel pLevel, Villager pOwner) {
      Brain<?> brain = pOwner.getBrain();
      if (!brain.getMemory(MemoryModuleType.INTERACTION_TARGET).isPresent()) {
         return false;
      } else {
         LivingEntity livingentity = brain.getMemory(MemoryModuleType.INTERACTION_TARGET).get();
         return livingentity.getType() == EntityType.PLAYER && pOwner.isAlive() && livingentity.isAlive() && !pOwner.isBaby() && pOwner.distanceToSqr(livingentity) <= 17.0D;
      }
   }

   public boolean canStillUse(ServerLevel pLevel, Villager pEntity, long pGameTime) {
      return this.checkExtraStartConditions(pLevel, pEntity) && this.lookTime > 0 && pEntity.getBrain().getMemory(MemoryModuleType.INTERACTION_TARGET).isPresent();
   }

   public void start(ServerLevel pLevel, Villager pEntity, long pGameTime) {
      super.start(pLevel, pEntity, pGameTime);
      this.lookAtTarget(pEntity);
      this.cycleCounter = 0;
      this.displayIndex = 0;
      this.lookTime = 40;
   }

   public void tick(ServerLevel pLevel, Villager pOwner, long pGameTime) {
      LivingEntity livingentity = this.lookAtTarget(pOwner);
      this.findItemsToDisplay(livingentity, pOwner);
      if (!this.displayItems.isEmpty()) {
         this.displayCyclingItems(pOwner);
      } else {
         clearHeldItem(pOwner);
         this.lookTime = Math.min(this.lookTime, 40);
      }

      --this.lookTime;
   }

   public void stop(ServerLevel pLevel, Villager pEntity, long pGameTime) {
      super.stop(pLevel, pEntity, pGameTime);
      pEntity.getBrain().eraseMemory(MemoryModuleType.INTERACTION_TARGET);
      clearHeldItem(pEntity);
      this.playerItemStack = null;
   }

   private void findItemsToDisplay(LivingEntity p_24113_, Villager p_24114_) {
      boolean flag = false;
      ItemStack itemstack = p_24113_.getMainHandItem();
      if (this.playerItemStack == null || !ItemStack.isSame(this.playerItemStack, itemstack)) {
         this.playerItemStack = itemstack;
         flag = true;
         this.displayItems.clear();
      }

      if (flag && !this.playerItemStack.isEmpty()) {
         this.updateDisplayItems(p_24114_);
         if (!this.displayItems.isEmpty()) {
            this.lookTime = 900;
            this.displayFirstItem(p_24114_);
         }
      }

   }

   private void displayFirstItem(Villager p_24116_) {
      displayAsHeldItem(p_24116_, this.displayItems.get(0));
   }

   private void updateDisplayItems(Villager p_24128_) {
      for(MerchantOffer merchantoffer : p_24128_.getOffers()) {
         if (!merchantoffer.isOutOfStock() && this.playerItemStackMatchesCostOfOffer(merchantoffer)) {
            this.displayItems.add(merchantoffer.getResult());
         }
      }

   }

   private boolean playerItemStackMatchesCostOfOffer(MerchantOffer p_24118_) {
      return ItemStack.isSame(this.playerItemStack, p_24118_.getCostA()) || ItemStack.isSame(this.playerItemStack, p_24118_.getCostB());
   }

   private static void clearHeldItem(Villager p_182374_) {
      p_182374_.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
      p_182374_.setDropChance(EquipmentSlot.MAINHAND, 0.085F);
   }

   private static void displayAsHeldItem(Villager p_182371_, ItemStack p_182372_) {
      p_182371_.setItemSlot(EquipmentSlot.MAINHAND, p_182372_);
      p_182371_.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
   }

   private LivingEntity lookAtTarget(Villager p_24138_) {
      Brain<?> brain = p_24138_.getBrain();
      LivingEntity livingentity = brain.getMemory(MemoryModuleType.INTERACTION_TARGET).get();
      brain.setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(livingentity, true));
      return livingentity;
   }

   private void displayCyclingItems(Villager p_24148_) {
      if (this.displayItems.size() >= 2 && ++this.cycleCounter >= 40) {
         ++this.displayIndex;
         this.cycleCounter = 0;
         if (this.displayIndex > this.displayItems.size() - 1) {
            this.displayIndex = 0;
         }

         displayAsHeldItem(p_24148_, this.displayItems.get(this.displayIndex));
      }

   }
}