package net.minecraft.client.gui.components;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PlainTextButton extends Button {
   private final Font font;
   private final Component message;
   private final Component underlinedMessage;

   public PlainTextButton(int pX, int pY, int pWidth, int pHeight, Component pMessage, Button.OnPress pOnPress, Font pFont) {
      super(pX, pY, pWidth, pHeight, pMessage, pOnPress);
      this.font = pFont;
      this.message = pMessage;
      this.underlinedMessage = ComponentUtils.mergeStyles(pMessage.copy(), Style.EMPTY.withUnderlined(true));
   }

   public void renderButton(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
      Component component = this.isHoveredOrFocused() ? this.underlinedMessage : this.message;
      drawString(pPoseStack, this.font, component, this.x, this.y, 16777215 | Mth.ceil(this.alpha * 255.0F) << 24);
   }
}