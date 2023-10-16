package net.minecraft.world.item.crafting;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public class MapExtendingRecipe extends ShapedRecipe {
   public MapExtendingRecipe(ResourceLocation pId) {
      super(pId, "", 3, 3, NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.PAPER), Ingredient.of(Items.PAPER), Ingredient.of(Items.PAPER), Ingredient.of(Items.PAPER), Ingredient.of(Items.FILLED_MAP), Ingredient.of(Items.PAPER), Ingredient.of(Items.PAPER), Ingredient.of(Items.PAPER), Ingredient.of(Items.PAPER)), new ItemStack(Items.MAP));
   }

   /**
    * Used to check if a recipe matches current crafting inventory
    */
   public boolean matches(CraftingContainer pInv, Level pLevel) {
      if (!super.matches(pInv, pLevel)) {
         return false;
      } else {
         ItemStack itemstack = ItemStack.EMPTY;

         for(int i = 0; i < pInv.getContainerSize() && itemstack.isEmpty(); ++i) {
            ItemStack itemstack1 = pInv.getItem(i);
            if (itemstack1.is(Items.FILLED_MAP)) {
               itemstack = itemstack1;
            }
         }

         if (itemstack.isEmpty()) {
            return false;
         } else {
            MapItemSavedData mapitemsaveddata = MapItem.getSavedData(itemstack, pLevel);
            if (mapitemsaveddata == null) {
               return false;
            } else if (mapitemsaveddata.isExplorationMap()) {
               return false;
            } else {
               return mapitemsaveddata.scale < 4;
            }
         }
      }
   }

   /**
    * Returns an Item that is the result of this recipe
    */
   public ItemStack assemble(CraftingContainer pInv) {
      ItemStack itemstack = ItemStack.EMPTY;

      for(int i = 0; i < pInv.getContainerSize() && itemstack.isEmpty(); ++i) {
         ItemStack itemstack1 = pInv.getItem(i);
         if (itemstack1.is(Items.FILLED_MAP)) {
            itemstack = itemstack1;
         }
      }

      itemstack = itemstack.copy();
      itemstack.setCount(1);
      itemstack.getOrCreateTag().putInt("map_scale_direction", 1);
      return itemstack;
   }

   /**
    * If true, this recipe does not appear in the recipe book and does not respect recipe unlocking (and the
    * doLimitedCrafting gamerule)
    */
   public boolean isSpecial() {
      return true;
   }

   public RecipeSerializer<?> getSerializer() {
      return RecipeSerializer.MAP_EXTENDING;
   }
}