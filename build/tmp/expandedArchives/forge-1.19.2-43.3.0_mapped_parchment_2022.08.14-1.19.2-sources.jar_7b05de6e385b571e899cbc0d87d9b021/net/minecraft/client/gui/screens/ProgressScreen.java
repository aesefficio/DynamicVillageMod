package net.minecraft.client.gui.screens;

import com.mojang.blaze3d.vertex.PoseStack;
import javax.annotation.Nullable;
import net.minecraft.client.GameNarrator;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ProgressListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ProgressScreen extends Screen implements ProgressListener {
   @Nullable
   private Component header;
   @Nullable
   private Component stage;
   private int progress;
   private boolean stop;
   private final boolean clearScreenAfterStop;

   public ProgressScreen(boolean pClearScreenAfterStop) {
      super(GameNarrator.NO_TITLE);
      this.clearScreenAfterStop = pClearScreenAfterStop;
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }

   public void progressStartNoAbort(Component pComponent) {
      this.progressStart(pComponent);
   }

   public void progressStart(Component pComponent) {
      this.header = pComponent;
      this.progressStage(Component.translatable("progress.working"));
   }

   public void progressStage(Component pComponent) {
      this.stage = pComponent;
      this.progressStagePercentage(0);
   }

   /**
    * Updates the progress bar on the loading screen to the specified amount.
    */
   public void progressStagePercentage(int pProgress) {
      this.progress = pProgress;
   }

   public void stop() {
      this.stop = true;
   }

   public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
      if (this.stop) {
         if (this.clearScreenAfterStop) {
            this.minecraft.setScreen((Screen)null);
         }

      } else {
         this.renderBackground(pPoseStack);
         if (this.header != null) {
            drawCenteredString(pPoseStack, this.font, this.header, this.width / 2, 70, 16777215);
         }

         if (this.stage != null && this.progress != 0) {
            drawCenteredString(pPoseStack, this.font, Component.empty().append(this.stage).append(" " + this.progress + "%"), this.width / 2, 90, 16777215);
         }

         super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
      }
   }
}