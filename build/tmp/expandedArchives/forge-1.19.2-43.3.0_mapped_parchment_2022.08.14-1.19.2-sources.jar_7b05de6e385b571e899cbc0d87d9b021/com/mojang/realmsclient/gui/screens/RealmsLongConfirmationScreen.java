package com.mojang.realmsclient.gui.screens;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RealmsLongConfirmationScreen extends RealmsScreen {
   private final RealmsLongConfirmationScreen.Type type;
   private final Component line2;
   private final Component line3;
   protected final BooleanConsumer callback;
   private final boolean yesNoQuestion;

   public RealmsLongConfirmationScreen(BooleanConsumer pConsumer, RealmsLongConfirmationScreen.Type pType, Component pLine2, Component pLine3, boolean pYesNoQuestion) {
      super(GameNarrator.NO_TITLE);
      this.callback = pConsumer;
      this.type = pType;
      this.line2 = pLine2;
      this.line3 = pLine3;
      this.yesNoQuestion = pYesNoQuestion;
   }

   public void init() {
      if (this.yesNoQuestion) {
         this.addRenderableWidget(new Button(this.width / 2 - 105, row(8), 100, 20, CommonComponents.GUI_YES, (p_88751_) -> {
            this.callback.accept(true);
         }));
         this.addRenderableWidget(new Button(this.width / 2 + 5, row(8), 100, 20, CommonComponents.GUI_NO, (p_88749_) -> {
            this.callback.accept(false);
         }));
      } else {
         this.addRenderableWidget(new Button(this.width / 2 - 50, row(8), 100, 20, Component.translatable("mco.gui.ok"), (p_88746_) -> {
            this.callback.accept(true);
         }));
      }

   }

   public Component getNarrationMessage() {
      return CommonComponents.joinLines(this.type.text, this.line2, this.line3);
   }

   public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
      if (pKeyCode == 256) {
         this.callback.accept(false);
         return true;
      } else {
         return super.keyPressed(pKeyCode, pScanCode, pModifiers);
      }
   }

   public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
      this.renderBackground(pPoseStack);
      drawCenteredString(pPoseStack, this.font, this.type.text, this.width / 2, row(2), this.type.colorCode);
      drawCenteredString(pPoseStack, this.font, this.line2, this.width / 2, row(4), 16777215);
      drawCenteredString(pPoseStack, this.font, this.line3, this.width / 2, row(6), 16777215);
      super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
   }

   @OnlyIn(Dist.CLIENT)
   public static enum Type {
      Warning("Warning!", 16711680),
      Info("Info!", 8226750);

      public final int colorCode;
      public final Component text;

      private Type(String pText, int pColorCode) {
         this.text = Component.literal(pText);
         this.colorCode = pColorCode;
      }
   }
}