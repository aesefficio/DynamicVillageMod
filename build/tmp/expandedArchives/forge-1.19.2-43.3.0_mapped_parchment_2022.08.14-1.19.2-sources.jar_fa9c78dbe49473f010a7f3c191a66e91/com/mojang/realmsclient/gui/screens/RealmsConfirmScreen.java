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
public class RealmsConfirmScreen extends RealmsScreen {
   protected BooleanConsumer callback;
   private final Component title1;
   private final Component title2;

   public RealmsConfirmScreen(BooleanConsumer pCallback, Component pTitle1, Component pTitle2) {
      super(GameNarrator.NO_TITLE);
      this.callback = pCallback;
      this.title1 = pTitle1;
      this.title2 = pTitle2;
   }

   public void init() {
      this.addRenderableWidget(new Button(this.width / 2 - 105, row(9), 100, 20, CommonComponents.GUI_YES, (p_88562_) -> {
         this.callback.accept(true);
      }));
      this.addRenderableWidget(new Button(this.width / 2 + 5, row(9), 100, 20, CommonComponents.GUI_NO, (p_88559_) -> {
         this.callback.accept(false);
      }));
   }

   public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
      this.renderBackground(pPoseStack);
      drawCenteredString(pPoseStack, this.font, this.title1, this.width / 2, row(3), 16777215);
      drawCenteredString(pPoseStack, this.font, this.title2, this.width / 2, row(5), 16777215);
      super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
   }
}