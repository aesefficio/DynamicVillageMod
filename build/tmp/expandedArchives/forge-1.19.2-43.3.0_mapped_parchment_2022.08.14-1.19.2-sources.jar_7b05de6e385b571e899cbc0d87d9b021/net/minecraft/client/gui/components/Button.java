package net.minecraft.client.gui.components;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.Consumer;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class Button extends AbstractButton {
   public static final Button.OnTooltip NO_TOOLTIP = (p_93740_, p_93741_, p_93742_, p_93743_) -> {
   };
   public static final int SMALL_WIDTH = 120;
   public static final int DEFAULT_WIDTH = 150;
   public static final int DEFAULT_HEIGHT = 20;
   protected final Button.OnPress onPress;
   protected final Button.OnTooltip onTooltip;

   public Button(int pX, int pY, int pWidth, int pHeight, Component pMessage, Button.OnPress pOnPress) {
      this(pX, pY, pWidth, pHeight, pMessage, pOnPress, NO_TOOLTIP);
   }

   public Button(int pX, int pY, int pWidth, int pHeight, Component pMessage, Button.OnPress pOnPress, Button.OnTooltip pOnTooltip) {
      super(pX, pY, pWidth, pHeight, pMessage);
      this.onPress = pOnPress;
      this.onTooltip = pOnTooltip;
   }

   public void onPress() {
      this.onPress.onPress(this);
   }

   public void renderButton(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
      super.renderButton(pPoseStack, pMouseX, pMouseY, pPartialTick);
      if (this.isHoveredOrFocused()) {
         this.renderToolTip(pPoseStack, pMouseX, pMouseY);
      }

   }

   public void renderToolTip(PoseStack pPoseStack, int pMouseX, int pMouseY) {
      this.onTooltip.onTooltip(this, pPoseStack, pMouseX, pMouseY);
   }

   public void updateNarration(NarrationElementOutput pNarrationElementOutput) {
      this.defaultButtonNarrationText(pNarrationElementOutput);
      this.onTooltip.narrateTooltip((p_168841_) -> {
         pNarrationElementOutput.add(NarratedElementType.HINT, p_168841_);
      });
   }

   @OnlyIn(Dist.CLIENT)
   public interface OnPress {
      void onPress(Button pButton);
   }

   @OnlyIn(Dist.CLIENT)
   public interface OnTooltip {
      void onTooltip(Button pButton, PoseStack pPoseStack, int pMouseX, int pMouseY);

      default void narrateTooltip(Consumer<Component> pContents) {
      }
   }
}