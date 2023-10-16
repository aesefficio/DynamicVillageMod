package net.minecraft.client.gui.screens.recipebook;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.RecipeBook;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RecipeButton extends AbstractWidget {
   private static final ResourceLocation RECIPE_BOOK_LOCATION = new ResourceLocation("textures/gui/recipe_book.png");
   private static final float ANIMATION_TIME = 15.0F;
   private static final int BACKGROUND_SIZE = 25;
   public static final int TICKS_TO_SWAP = 30;
   private static final Component MORE_RECIPES_TOOLTIP = Component.translatable("gui.recipebook.moreRecipes");
   private RecipeBookMenu<?> menu;
   private RecipeBook book;
   private RecipeCollection collection;
   private float time;
   private float animationTime;
   private int currentIndex;

   public RecipeButton() {
      super(0, 0, 25, 25, CommonComponents.EMPTY);
   }

   public void init(RecipeCollection pCollection, RecipeBookPage pRecipeBookPage) {
      this.collection = pCollection;
      this.menu = (RecipeBookMenu)pRecipeBookPage.getMinecraft().player.containerMenu;
      this.book = pRecipeBookPage.getRecipeBook();
      List<Recipe<?>> list = pCollection.getRecipes(this.book.isFiltering(this.menu));

      for(Recipe<?> recipe : list) {
         if (this.book.willHighlight(recipe)) {
            pRecipeBookPage.recipesShown(list);
            this.animationTime = 15.0F;
            break;
         }
      }

   }

   public RecipeCollection getCollection() {
      return this.collection;
   }

   public void setPosition(int pX, int pY) {
      this.x = pX;
      this.y = pY;
   }

   public void renderButton(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
      if (!Screen.hasControlDown()) {
         this.time += pPartialTick;
      }

      Minecraft minecraft = Minecraft.getInstance();
      RenderSystem.setShader(GameRenderer::getPositionTexShader);
      RenderSystem.setShaderTexture(0, RECIPE_BOOK_LOCATION);
      int i = 29;
      if (!this.collection.hasCraftable()) {
         i += 25;
      }

      int j = 206;
      if (this.collection.getRecipes(this.book.isFiltering(this.menu)).size() > 1) {
         j += 25;
      }

      boolean flag = this.animationTime > 0.0F;
      PoseStack posestack = RenderSystem.getModelViewStack();
      if (flag) {
         float f = 1.0F + 0.1F * (float)Math.sin((double)(this.animationTime / 15.0F * (float)Math.PI));
         posestack.pushPose();
         posestack.translate((double)(this.x + 8), (double)(this.y + 12), 0.0D);
         posestack.scale(f, f, 1.0F);
         posestack.translate((double)(-(this.x + 8)), (double)(-(this.y + 12)), 0.0D);
         RenderSystem.applyModelViewMatrix();
         this.animationTime -= pPartialTick;
      }

      this.blit(pPoseStack, this.x, this.y, i, j, this.width, this.height);
      List<Recipe<?>> list = this.getOrderedRecipes();
      this.currentIndex = Mth.floor(this.time / 30.0F) % list.size();
      ItemStack itemstack = list.get(this.currentIndex).getResultItem();
      int k = 4;
      if (this.collection.hasSingleResultItem() && this.getOrderedRecipes().size() > 1) {
         minecraft.getItemRenderer().renderAndDecorateItem(itemstack, this.x + k + 1, this.y + k + 1, 0, 10);
         --k;
      }

      minecraft.getItemRenderer().renderAndDecorateFakeItem(itemstack, this.x + k, this.y + k);
      if (flag) {
         posestack.popPose();
         RenderSystem.applyModelViewMatrix();
      }

   }

   private List<Recipe<?>> getOrderedRecipes() {
      List<Recipe<?>> list = this.collection.getDisplayRecipes(true);
      if (!this.book.isFiltering(this.menu)) {
         list.addAll(this.collection.getDisplayRecipes(false));
      }

      return list;
   }

   public boolean isOnlyOption() {
      return this.getOrderedRecipes().size() == 1;
   }

   public Recipe<?> getRecipe() {
      List<Recipe<?>> list = this.getOrderedRecipes();
      return list.get(this.currentIndex);
   }

   public List<Component> getTooltipText(Screen pScreen) {
      ItemStack itemstack = this.getOrderedRecipes().get(this.currentIndex).getResultItem();
      List<Component> list = Lists.newArrayList(pScreen.getTooltipFromItem(itemstack));
      if (this.collection.getRecipes(this.book.isFiltering(this.menu)).size() > 1) {
         list.add(MORE_RECIPES_TOOLTIP);
      }

      return list;
   }

   public void updateNarration(NarrationElementOutput pNarrationElementOutput) {
      ItemStack itemstack = this.getOrderedRecipes().get(this.currentIndex).getResultItem();
      pNarrationElementOutput.add(NarratedElementType.TITLE, Component.translatable("narration.recipe", itemstack.getHoverName()));
      if (this.collection.getRecipes(this.book.isFiltering(this.menu)).size() > 1) {
         pNarrationElementOutput.add(NarratedElementType.USAGE, Component.translatable("narration.button.usage.hovered"), Component.translatable("narration.recipe.usage.more"));
      } else {
         pNarrationElementOutput.add(NarratedElementType.USAGE, Component.translatable("narration.button.usage.hovered"));
      }

   }

   public int getWidth() {
      return 25;
   }

   protected boolean isValidClickButton(int pButton) {
      return pButton == 0 || pButton == 1;
   }
}