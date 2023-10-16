package net.minecraft.client.gui.components.toasts;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TutorialToast implements Toast {
   public static final int PROGRESS_BAR_WIDTH = 154;
   public static final int PROGRESS_BAR_HEIGHT = 1;
   public static final int PROGRESS_BAR_X = 3;
   public static final int PROGRESS_BAR_Y = 28;
   private final TutorialToast.Icons icon;
   private final Component title;
   @Nullable
   private final Component message;
   private Toast.Visibility visibility = Toast.Visibility.SHOW;
   private long lastProgressTime;
   private float lastProgress;
   private float progress;
   private final boolean progressable;

   public TutorialToast(TutorialToast.Icons pIcon, Component pTitle, @Nullable Component pMessage, boolean pProgressable) {
      this.icon = pIcon;
      this.title = pTitle;
      this.message = pMessage;
      this.progressable = pProgressable;
   }

   /**
    * 
    * @param pTimeSinceLastVisible time in milliseconds
    */
   public Toast.Visibility render(PoseStack pPoseStack, ToastComponent pToastComponent, long pTimeSinceLastVisible) {
      RenderSystem.setShaderTexture(0, TEXTURE);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      pToastComponent.blit(pPoseStack, 0, 0, 0, 96, this.width(), this.height());
      this.icon.render(pPoseStack, pToastComponent, 6, 6);
      if (this.message == null) {
         pToastComponent.getMinecraft().font.draw(pPoseStack, this.title, 30.0F, 12.0F, -11534256);
      } else {
         pToastComponent.getMinecraft().font.draw(pPoseStack, this.title, 30.0F, 7.0F, -11534256);
         pToastComponent.getMinecraft().font.draw(pPoseStack, this.message, 30.0F, 18.0F, -16777216);
      }

      if (this.progressable) {
         GuiComponent.fill(pPoseStack, 3, 28, 157, 29, -1);
         float f = Mth.clampedLerp(this.lastProgress, this.progress, (float)(pTimeSinceLastVisible - this.lastProgressTime) / 100.0F);
         int i;
         if (this.progress >= this.lastProgress) {
            i = -16755456;
         } else {
            i = -11206656;
         }

         GuiComponent.fill(pPoseStack, 3, 28, (int)(3.0F + 154.0F * f), 29, i);
         this.lastProgress = f;
         this.lastProgressTime = pTimeSinceLastVisible;
      }

      return this.visibility;
   }

   public void hide() {
      this.visibility = Toast.Visibility.HIDE;
   }

   public void updateProgress(float pProgress) {
      this.progress = pProgress;
   }

   @OnlyIn(Dist.CLIENT)
   public static enum Icons {
      MOVEMENT_KEYS(0, 0),
      MOUSE(1, 0),
      TREE(2, 0),
      RECIPE_BOOK(0, 1),
      WOODEN_PLANKS(1, 1),
      SOCIAL_INTERACTIONS(2, 1),
      RIGHT_CLICK(3, 1);

      private final int x;
      private final int y;

      private Icons(int pX, int pY) {
         this.x = pX;
         this.y = pY;
      }

      public void render(PoseStack pPoseStack, GuiComponent pGuiComponent, int pX, int pY) {
         RenderSystem.enableBlend();
         pGuiComponent.blit(pPoseStack, pX, pY, 176 + this.x * 20, this.y * 20, 20, 20);
         RenderSystem.enableBlend();
      }
   }
}