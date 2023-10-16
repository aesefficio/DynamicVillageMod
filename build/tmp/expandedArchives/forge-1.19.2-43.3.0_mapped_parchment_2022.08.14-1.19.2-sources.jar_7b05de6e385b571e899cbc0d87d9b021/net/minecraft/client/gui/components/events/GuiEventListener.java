package net.minecraft.client.gui.components.events;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface GuiEventListener {
   long DOUBLE_CLICK_THRESHOLD_MS = 250L;

   default void mouseMoved(double pMouseX, double pMouseY) {
   }

   default boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
      return false;
   }

   default boolean mouseReleased(double pMouseX, double pMouseY, int pButton) {
      return false;
   }

   default boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
      return false;
   }

   default boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
      return false;
   }

   default boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
      return false;
   }

   default boolean keyReleased(int pKeyCode, int pScanCode, int pModifiers) {
      return false;
   }

   default boolean charTyped(char pCodePoint, int pModifiers) {
      return false;
   }

   default boolean changeFocus(boolean pFocus) {
      return false;
   }

   default boolean isMouseOver(double pMouseX, double pMouseY) {
      return false;
   }
}