package com.mojang.realmsclient.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.realms.RealmsObjectSelectionList;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class RowButton {
   public final int width;
   public final int height;
   public final int xOffset;
   public final int yOffset;

   public RowButton(int pWidth, int pHeight, int pXOffset, int pYOffset) {
      this.width = pWidth;
      this.height = pHeight;
      this.xOffset = pXOffset;
      this.yOffset = pYOffset;
   }

   public void drawForRowAt(PoseStack pPoseStack, int pX, int pY, int p_88022_, int p_88023_) {
      int i = pX + this.xOffset;
      int j = pY + this.yOffset;
      boolean flag = p_88022_ >= i && p_88022_ <= i + this.width && p_88023_ >= j && p_88023_ <= j + this.height;
      this.draw(pPoseStack, i, j, flag);
   }

   protected abstract void draw(PoseStack pPoseStack, int pX, int pY, boolean pShowTooltip);

   public int getRight() {
      return this.xOffset + this.width;
   }

   public int getBottom() {
      return this.yOffset + this.height;
   }

   public abstract void onClick(int p_88017_);

   public static void drawButtonsInRow(PoseStack pPoseStack, List<RowButton> pButtons, RealmsObjectSelectionList<?> pPendingInvitations, int pX, int pY, int p_88034_, int p_88035_) {
      for(RowButton rowbutton : pButtons) {
         if (pPendingInvitations.getRowWidth() > rowbutton.getRight()) {
            rowbutton.drawForRowAt(pPoseStack, pX, pY, p_88034_, p_88035_);
         }
      }

   }

   public static void rowButtonMouseClicked(RealmsObjectSelectionList<?> p_88037_, ObjectSelectionList.Entry<?> p_88038_, List<RowButton> p_88039_, int p_88040_, double p_88041_, double p_88042_) {
      if (p_88040_ == 0) {
         int i = p_88037_.children().indexOf(p_88038_);
         if (i > -1) {
            p_88037_.selectItem(i);
            int j = p_88037_.getRowLeft();
            int k = p_88037_.getRowTop(i);
            int l = (int)(p_88041_ - (double)j);
            int i1 = (int)(p_88042_ - (double)k);

            for(RowButton rowbutton : p_88039_) {
               if (l >= rowbutton.xOffset && l <= rowbutton.getRight() && i1 >= rowbutton.yOffset && i1 <= rowbutton.getBottom()) {
                  rowbutton.onClick(i);
               }
            }
         }
      }

   }
}