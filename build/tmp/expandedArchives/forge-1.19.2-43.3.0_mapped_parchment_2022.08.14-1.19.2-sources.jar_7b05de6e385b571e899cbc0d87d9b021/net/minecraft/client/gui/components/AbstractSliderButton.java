package net.minecraft.client.gui.components;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractSliderButton extends AbstractWidget {
   protected double value;

   public AbstractSliderButton(int pX, int pY, int pWidth, int pHeight, Component pMessage, double pValue) {
      super(pX, pY, pWidth, pHeight, pMessage);
      this.value = pValue;
   }

   protected int getYImage(boolean pIsHovered) {
      return 0;
   }

   protected MutableComponent createNarrationMessage() {
      return Component.translatable("gui.narrate.slider", this.getMessage());
   }

   public void updateNarration(NarrationElementOutput pNarrationElementOutput) {
      pNarrationElementOutput.add(NarratedElementType.TITLE, this.createNarrationMessage());
      if (this.active) {
         if (this.isFocused()) {
            pNarrationElementOutput.add(NarratedElementType.USAGE, Component.translatable("narration.slider.usage.focused"));
         } else {
            pNarrationElementOutput.add(NarratedElementType.USAGE, Component.translatable("narration.slider.usage.hovered"));
         }
      }

   }

   protected void renderBg(PoseStack pPoseStack, Minecraft pMinecraft, int pMouseX, int pMouseY) {
      RenderSystem.setShaderTexture(0, WIDGETS_LOCATION);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      int i = (this.isHoveredOrFocused() ? 2 : 1) * 20;
      this.blit(pPoseStack, this.x + (int)(this.value * (double)(this.width - 8)), this.y, 0, 46 + i, 4, 20);
      this.blit(pPoseStack, this.x + (int)(this.value * (double)(this.width - 8)) + 4, this.y, 196, 46 + i, 4, 20);
   }

   public void onClick(double pMouseX, double pMouseY) {
      this.setValueFromMouse(pMouseX);
   }

   public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
      boolean flag = pKeyCode == 263;
      if (flag || pKeyCode == 262) {
         float f = flag ? -1.0F : 1.0F;
         this.setValue(this.value + (double)(f / (float)(this.width - 8)));
      }

      return false;
   }

   private void setValueFromMouse(double pMouseX) {
      this.setValue((pMouseX - (double)(this.x + 4)) / (double)(this.width - 8));
   }

   private void setValue(double pValue) {
      double d0 = this.value;
      this.value = Mth.clamp(pValue, 0.0D, 1.0D);
      if (d0 != this.value) {
         this.applyValue();
      }

      this.updateMessage();
   }

   protected void onDrag(double pMouseX, double pMouseY, double pDragX, double pDragY) {
      this.setValueFromMouse(pMouseX);
      super.onDrag(pMouseX, pMouseY, pDragX, pDragY);
   }

   public void playDownSound(SoundManager pHandler) {
   }

   public void onRelease(double pMouseX, double pMouseY) {
      super.playDownSound(Minecraft.getInstance().getSoundManager());
   }

   protected abstract void updateMessage();

   protected abstract void applyValue();
}