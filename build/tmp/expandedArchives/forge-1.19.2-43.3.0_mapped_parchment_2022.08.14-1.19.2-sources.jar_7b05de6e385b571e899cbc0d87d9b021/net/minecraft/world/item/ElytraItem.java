package net.minecraft.world.item;

import javax.annotation.Nullable;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;

public class ElytraItem extends Item implements Wearable {
   public ElytraItem(Item.Properties pProperties) {
      super(pProperties);
      DispenserBlock.registerBehavior(this, ArmorItem.DISPENSE_ITEM_BEHAVIOR);
   }

   public static boolean isFlyEnabled(ItemStack pElytraStack) {
      return pElytraStack.getDamageValue() < pElytraStack.getMaxDamage() - 1;
   }

   /**
    * Return whether this item is repairable in an anvil.
    */
   public boolean isValidRepairItem(ItemStack pToRepair, ItemStack pRepair) {
      return pRepair.is(Items.PHANTOM_MEMBRANE);
   }

   /**
    * Called to trigger the item's "innate" right click behavior. To handle when this item is used on a Block, see
    * {@link #onItemUse}.
    */
   public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pHand) {
      ItemStack itemstack = pPlayer.getItemInHand(pHand);
      EquipmentSlot equipmentslot = Mob.getEquipmentSlotForItem(itemstack);
      ItemStack itemstack1 = pPlayer.getItemBySlot(equipmentslot);
      if (itemstack1.isEmpty()) {
         pPlayer.setItemSlot(equipmentslot, itemstack.copy());
         if (!pLevel.isClientSide()) {
            pPlayer.awardStat(Stats.ITEM_USED.get(this));
         }

         itemstack.setCount(0);
         return InteractionResultHolder.sidedSuccess(itemstack, pLevel.isClientSide());
      } else {
         return InteractionResultHolder.fail(itemstack);
      }
   }

   @Override
   public boolean canElytraFly(ItemStack stack, net.minecraft.world.entity.LivingEntity entity) {
      return ElytraItem.isFlyEnabled(stack);
   }

   @Override
   public boolean elytraFlightTick(ItemStack stack, net.minecraft.world.entity.LivingEntity entity, int flightTicks) {
      if (!entity.level.isClientSide) {
         int nextFlightTick = flightTicks + 1;
         if (nextFlightTick % 10 == 0) {
            if (nextFlightTick % 20 == 0) {
               stack.hurtAndBreak(1, entity, e -> e.broadcastBreakEvent(net.minecraft.world.entity.EquipmentSlot.CHEST));
            }
            entity.gameEvent(net.minecraft.world.level.gameevent.GameEvent.ELYTRA_GLIDE);
         }
      }
      return true;
   }

   @Nullable
   public SoundEvent getEquipSound() {
      return SoundEvents.ARMOR_EQUIP_ELYTRA;
   }
}
