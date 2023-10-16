package net.minecraft.client.gui.components.toasts;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RecipeToast implements Toast {
   private static final long DISPLAY_TIME = 5000L;
   private static final Component TITLE_TEXT = Component.translatable("recipe.toast.title");
   private static final Component DESCRIPTION_TEXT = Component.translatable("recipe.toast.description");
   private final List<Recipe<?>> recipes = Lists.newArrayList();
   private long lastChanged;
   private boolean changed;

   public RecipeToast(Recipe<?> pRecipe) {
      this.recipes.add(pRecipe);
   }

   /**
    * 
    * @param pTimeSinceLastVisible time in milliseconds
    */
   public Toast.Visibility render(PoseStack pPoseStack, ToastComponent pToastComponent, long pTimeSinceLastVisible) {
      if (this.changed) {
         this.lastChanged = pTimeSinceLastVisible;
         this.changed = false;
      }

      if (this.recipes.isEmpty()) {
         return Toast.Visibility.HIDE;
      } else {
         RenderSystem.setShader(GameRenderer::getPositionTexShader);
         RenderSystem.setShaderTexture(0, TEXTURE);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         pToastComponent.blit(pPoseStack, 0, 0, 0, 32, this.width(), this.height());
         pToastComponent.getMinecraft().font.draw(pPoseStack, TITLE_TEXT, 30.0F, 7.0F, -11534256);
         pToastComponent.getMinecraft().font.draw(pPoseStack, DESCRIPTION_TEXT, 30.0F, 18.0F, -16777216);
         Recipe<?> recipe = this.recipes.get((int)(pTimeSinceLastVisible / Math.max(1L, 5000L / (long)this.recipes.size()) % (long)this.recipes.size()));
         ItemStack itemstack = recipe.getToastSymbol();
         PoseStack posestack = RenderSystem.getModelViewStack();
         posestack.pushPose();
         posestack.scale(0.6F, 0.6F, 1.0F);
         RenderSystem.applyModelViewMatrix();
         pToastComponent.getMinecraft().getItemRenderer().renderAndDecorateFakeItem(itemstack, 3, 3);
         posestack.popPose();
         RenderSystem.applyModelViewMatrix();
         pToastComponent.getMinecraft().getItemRenderer().renderAndDecorateFakeItem(recipe.getResultItem(), 8, 8);
         return pTimeSinceLastVisible - this.lastChanged >= 5000L ? Toast.Visibility.HIDE : Toast.Visibility.SHOW;
      }
   }

   private void addItem(Recipe<?> pRecipe) {
      this.recipes.add(pRecipe);
      this.changed = true;
   }

   public static void addOrUpdate(ToastComponent pToastGui, Recipe<?> pRecipe) {
      RecipeToast recipetoast = pToastGui.getToast(RecipeToast.class, NO_TOKEN);
      if (recipetoast == null) {
         pToastGui.addToast(new RecipeToast(pRecipe));
      } else {
         recipetoast.addItem(pRecipe);
      }

   }
}