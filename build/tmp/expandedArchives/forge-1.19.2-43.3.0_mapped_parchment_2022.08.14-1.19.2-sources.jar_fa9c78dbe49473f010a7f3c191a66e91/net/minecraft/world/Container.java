package net.minecraft.world;

import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public interface Container extends Clearable {
   int LARGE_MAX_STACK_SIZE = 64;

   /**
    * Returns the number of slots in the inventory.
    */
   int getContainerSize();

   boolean isEmpty();

   /**
    * Returns the stack in the given slot.
    */
   ItemStack getItem(int pSlot);

   /**
    * Removes up to a specified number of items from an inventory slot and returns them in a new stack.
    */
   ItemStack removeItem(int pSlot, int pAmount);

   /**
    * Removes a stack from the given slot and returns it.
    */
   ItemStack removeItemNoUpdate(int pSlot);

   /**
    * Sets the given item stack to the specified slot in the inventory (can be crafting or armor sections).
    */
   void setItem(int pSlot, ItemStack pStack);

   /**
    * Returns the maximum stack size for a inventory slot. Seems to always be 64, possibly will be extended.
    */
   default int getMaxStackSize() {
      return 64;
   }

   /**
    * For tile entities, ensures the chunk containing the tile entity is saved to disk later - the game won't think it
    * hasn't changed and skip it.
    */
   void setChanged();

   /**
    * Don't rename this method to canInteractWith due to conflicts with Container
    */
   boolean stillValid(Player pPlayer);

   default void startOpen(Player pPlayer) {
   }

   default void stopOpen(Player pPlayer) {
   }

   /**
    * Returns true if automation is allowed to insert the given stack (ignoring stack size) into the given slot. For
    * guis use Slot.isItemValid
    */
   default boolean canPlaceItem(int pIndex, ItemStack pStack) {
      return true;
   }

   /**
    * Returns the total amount of the specified item in this inventory. This method does not check for nbt.
    */
   default int countItem(Item pItem) {
      int i = 0;

      for(int j = 0; j < this.getContainerSize(); ++j) {
         ItemStack itemstack = this.getItem(j);
         if (itemstack.getItem().equals(pItem)) {
            i += itemstack.getCount();
         }
      }

      return i;
   }

   /**
    * Returns true if any item from the passed set exists in this inventory.
    */
   default boolean hasAnyOf(Set<Item> pSet) {
      return this.hasAnyMatching((p_216873_) -> {
         return !p_216873_.isEmpty() && pSet.contains(p_216873_.getItem());
      });
   }

   default boolean hasAnyMatching(Predicate<ItemStack> p_216875_) {
      for(int i = 0; i < this.getContainerSize(); ++i) {
         ItemStack itemstack = this.getItem(i);
         if (p_216875_.test(itemstack)) {
            return true;
         }
      }

      return false;
   }
}