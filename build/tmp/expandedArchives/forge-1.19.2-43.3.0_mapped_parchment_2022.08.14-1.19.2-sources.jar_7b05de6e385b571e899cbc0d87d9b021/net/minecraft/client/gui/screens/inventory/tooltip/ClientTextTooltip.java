package net.minecraft.client.gui.screens.inventory.tooltip;

import com.mojang.math.Matrix4f;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientTextTooltip implements ClientTooltipComponent {
   private final FormattedCharSequence text;

   public ClientTextTooltip(FormattedCharSequence pText) {
      this.text = pText;
   }

   public int getWidth(Font pFont) {
      return pFont.width(this.text);
   }

   public int getHeight() {
      return 10;
   }

   public void renderText(Font pFont, int pX, int pY, Matrix4f pMatrix4f, MultiBufferSource.BufferSource pBufferSource) {
      pFont.drawInBatch(this.text, (float)pX, (float)pY, -1, true, pMatrix4f, pBufferSource, false, 0, 15728880);
   }
}