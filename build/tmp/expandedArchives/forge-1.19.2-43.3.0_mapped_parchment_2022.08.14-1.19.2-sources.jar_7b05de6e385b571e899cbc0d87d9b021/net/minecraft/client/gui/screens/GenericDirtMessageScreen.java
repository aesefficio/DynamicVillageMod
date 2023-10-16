package net.minecraft.client.gui.screens;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GenericDirtMessageScreen extends Screen {
   public GenericDirtMessageScreen(Component pTitle) {
      super(pTitle);
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }

   public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
      this.renderDirtBackground(0);
      drawCenteredString(pPoseStack, this.font, this.title, this.width / 2, 70, 16777215);
      super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
   }
}