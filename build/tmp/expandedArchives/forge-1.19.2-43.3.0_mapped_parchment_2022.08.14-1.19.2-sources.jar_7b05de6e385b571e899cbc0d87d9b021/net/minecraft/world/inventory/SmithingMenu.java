package net.minecraft.world.inventory;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.UpgradeRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class SmithingMenu extends ItemCombinerMenu {
   private final Level level;
   @Nullable
   private UpgradeRecipe selectedRecipe;
   private final List<UpgradeRecipe> recipes;

   public SmithingMenu(int pContainerId, Inventory pPlayerInventory) {
      this(pContainerId, pPlayerInventory, ContainerLevelAccess.NULL);
   }

   public SmithingMenu(int pContainerId, Inventory pPlayerInventory, ContainerLevelAccess pAccess) {
      super(MenuType.SMITHING, pContainerId, pPlayerInventory, pAccess);
      this.level = pPlayerInventory.player.level;
      this.recipes = this.level.getRecipeManager().getAllRecipesFor(RecipeType.SMITHING);
   }

   protected boolean isValidBlock(BlockState pState) {
      return pState.is(Blocks.SMITHING_TABLE);
   }

   protected boolean mayPickup(Player pPlayer, boolean pHasStack) {
      return this.selectedRecipe != null && this.selectedRecipe.matches(this.inputSlots, this.level);
   }

   protected void onTake(Player p_150663_, ItemStack p_150664_) {
      p_150664_.onCraftedBy(p_150663_.level, p_150663_, p_150664_.getCount());
      this.resultSlots.awardUsedRecipes(p_150663_);
      this.shrinkStackInSlot(0);
      this.shrinkStackInSlot(1);
      this.access.execute((p_40263_, p_40264_) -> {
         p_40263_.levelEvent(1044, p_40264_, 0);
      });
   }

   private void shrinkStackInSlot(int pIndex) {
      ItemStack itemstack = this.inputSlots.getItem(pIndex);
      itemstack.shrink(1);
      this.inputSlots.setItem(pIndex, itemstack);
   }

   /**
    * called when the Anvil Input Slot changes, calculates the new result and puts it in the output slot
    */
   public void createResult() {
      List<UpgradeRecipe> list = this.level.getRecipeManager().getRecipesFor(RecipeType.SMITHING, this.inputSlots, this.level);
      if (list.isEmpty()) {
         this.resultSlots.setItem(0, ItemStack.EMPTY);
      } else {
         this.selectedRecipe = list.get(0);
         ItemStack itemstack = this.selectedRecipe.assemble(this.inputSlots);
         this.resultSlots.setRecipeUsed(this.selectedRecipe);
         this.resultSlots.setItem(0, itemstack);
      }

   }

   protected boolean shouldQuickMoveToAdditionalSlot(ItemStack pStack) {
      return this.recipes.stream().anyMatch((p_40261_) -> {
         return p_40261_.isAdditionIngredient(pStack);
      });
   }

   /**
    * Called to determine if the current slot is valid for the stack merging (double-click) code. The stack passed in is
    * null for the initial slot that was double-clicked.
    */
   public boolean canTakeItemForPickAll(ItemStack pStack, Slot pSlot) {
      return pSlot.container != this.resultSlots && super.canTakeItemForPickAll(pStack, pSlot);
   }
}