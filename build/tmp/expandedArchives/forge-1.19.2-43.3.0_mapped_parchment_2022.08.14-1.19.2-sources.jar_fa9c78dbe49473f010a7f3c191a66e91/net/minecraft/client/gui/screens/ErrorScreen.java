package net.minecraft.client.gui.screens;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ErrorScreen extends Screen {
   private final Component message;

   public ErrorScreen(Component pTitle, Component pMessage) {
      super(pTitle);
      this.message = pMessage;
   }

   protected void init() {
      super.init();
      this.addRenderableWidget(new Button(this.width / 2 - 100, 140, 200, 20, CommonComponents.GUI_CANCEL, (p_96057_) -> {
         this.minecraft.setScreen((Screen)null);
      }));
   }

   public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
      this.fillGradient(pPoseStack, 0, 0, this.width, this.height, -12574688, -11530224);
      drawCenteredString(pPoseStack, this.font, this.title, this.width / 2, 90, 16777215);
      drawCenteredString(pPoseStack, this.font, this.message, this.width / 2, 110, 16777215);
      super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }
}