package net.minecraft.client.gui.components.events;

import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface ContainerEventHandler extends GuiEventListener {
   List<? extends GuiEventListener> children();

   /**
    * Returns the first event listener that intersects with the mouse coordinates.
    */
   default Optional<GuiEventListener> getChildAt(double pMouseX, double pMouseY) {
      for(GuiEventListener guieventlistener : this.children()) {
         if (guieventlistener.isMouseOver(pMouseX, pMouseY)) {
            return Optional.of(guieventlistener);
         }
      }

      return Optional.empty();
   }

   default boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
      for(GuiEventListener guieventlistener : this.children()) {
         if (guieventlistener.mouseClicked(pMouseX, pMouseY, pButton)) {
            this.setFocused(guieventlistener);
            if (pButton == 0) {
               this.setDragging(true);
            }

            return true;
         }
      }

      return false;
   }

   default boolean mouseReleased(double pMouseX, double pMouseY, int pButton) {
      this.setDragging(false);
      return this.getChildAt(pMouseX, pMouseY).filter((p_94708_) -> {
         return p_94708_.mouseReleased(pMouseX, pMouseY, pButton);
      }).isPresent();
   }

   default boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
      return this.getFocused() != null && this.isDragging() && pButton == 0 ? this.getFocused().mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY) : false;
   }

   boolean isDragging();

   void setDragging(boolean pIsDragging);

   default boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
      return this.getChildAt(pMouseX, pMouseY).filter((p_94693_) -> {
         return p_94693_.mouseScrolled(pMouseX, pMouseY, pDelta);
      }).isPresent();
   }

   default boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
      return this.getFocused() != null && this.getFocused().keyPressed(pKeyCode, pScanCode, pModifiers);
   }

   default boolean keyReleased(int pKeyCode, int pScanCode, int pModifiers) {
      return this.getFocused() != null && this.getFocused().keyReleased(pKeyCode, pScanCode, pModifiers);
   }

   default boolean charTyped(char pCodePoint, int pModifiers) {
      return this.getFocused() != null && this.getFocused().charTyped(pCodePoint, pModifiers);
   }

   @Nullable
   GuiEventListener getFocused();

   void setFocused(@Nullable GuiEventListener pFocused);

   default void setInitialFocus(@Nullable GuiEventListener pEventListener) {
      this.setFocused(pEventListener);
      pEventListener.changeFocus(true);
   }

   default void magicalSpecialHackyFocus(@Nullable GuiEventListener pEventListener) {
      this.setFocused(pEventListener);
   }

   default boolean changeFocus(boolean pFocus) {
      GuiEventListener guieventlistener = this.getFocused();
      boolean flag = guieventlistener != null;
      if (flag && guieventlistener.changeFocus(pFocus)) {
         return true;
      } else {
         List<? extends GuiEventListener> list = this.children();
         int j = list.indexOf(guieventlistener);
         int i;
         if (flag && j >= 0) {
            i = j + (pFocus ? 1 : 0);
         } else if (pFocus) {
            i = 0;
         } else {
            i = list.size();
         }

         ListIterator<? extends GuiEventListener> listiterator = list.listIterator(i);
         BooleanSupplier booleansupplier = pFocus ? listiterator::hasNext : listiterator::hasPrevious;
         Supplier<? extends GuiEventListener> supplier = pFocus ? listiterator::next : listiterator::previous;

         while(booleansupplier.getAsBoolean()) {
            GuiEventListener guieventlistener1 = supplier.get();
            if (guieventlistener1.changeFocus(pFocus)) {
               this.setFocused(guieventlistener1);
               return true;
            }
         }

         this.setFocused((GuiEventListener)null);
         return false;
      }
   }
}