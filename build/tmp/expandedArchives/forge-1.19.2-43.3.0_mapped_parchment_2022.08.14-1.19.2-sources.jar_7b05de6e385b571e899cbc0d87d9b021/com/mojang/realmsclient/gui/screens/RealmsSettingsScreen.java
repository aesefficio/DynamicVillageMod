package com.mojang.realmsclient.gui.screens;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.realmsclient.dto.RealmsServer;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RealmsSettingsScreen extends RealmsScreen {
   private static final int COMPONENT_WIDTH = 212;
   private static final Component NAME_LABEL = Component.translatable("mco.configure.world.name");
   private static final Component DESCRIPTION_LABEL = Component.translatable("mco.configure.world.description");
   private final RealmsConfigureWorldScreen configureWorldScreen;
   private final RealmsServer serverData;
   private Button doneButton;
   private EditBox descEdit;
   private EditBox nameEdit;

   public RealmsSettingsScreen(RealmsConfigureWorldScreen pConfigureWorldScreen, RealmsServer pServerData) {
      super(Component.translatable("mco.configure.world.settings.title"));
      this.configureWorldScreen = pConfigureWorldScreen;
      this.serverData = pServerData;
   }

   public void tick() {
      this.nameEdit.tick();
      this.descEdit.tick();
      this.doneButton.active = !this.nameEdit.getValue().trim().isEmpty();
   }

   public void init() {
      this.minecraft.keyboardHandler.setSendRepeatsToGui(true);
      int i = this.width / 2 - 106;
      this.doneButton = this.addRenderableWidget(new Button(i - 2, row(12), 106, 20, Component.translatable("mco.configure.world.buttons.done"), (p_89847_) -> {
         this.save();
      }));
      this.addRenderableWidget(new Button(this.width / 2 + 2, row(12), 106, 20, CommonComponents.GUI_CANCEL, (p_89845_) -> {
         this.minecraft.setScreen(this.configureWorldScreen);
      }));
      String s = this.serverData.state == RealmsServer.State.OPEN ? "mco.configure.world.buttons.close" : "mco.configure.world.buttons.open";
      Button button = new Button(this.width / 2 - 53, row(0), 106, 20, Component.translatable(s), (p_89842_) -> {
         if (this.serverData.state == RealmsServer.State.OPEN) {
            Component component = Component.translatable("mco.configure.world.close.question.line1");
            Component component1 = Component.translatable("mco.configure.world.close.question.line2");
            this.minecraft.setScreen(new RealmsLongConfirmationScreen((p_167510_) -> {
               if (p_167510_) {
                  this.configureWorldScreen.closeTheWorld(this);
               } else {
                  this.minecraft.setScreen(this);
               }

            }, RealmsLongConfirmationScreen.Type.Info, component, component1, true));
         } else {
            this.configureWorldScreen.openTheWorld(false, this);
         }

      });
      this.addRenderableWidget(button);
      this.nameEdit = new EditBox(this.minecraft.font, i, row(4), 212, 20, (EditBox)null, Component.translatable("mco.configure.world.name"));
      this.nameEdit.setMaxLength(32);
      this.nameEdit.setValue(this.serverData.getName());
      this.addWidget(this.nameEdit);
      this.magicalSpecialHackyFocus(this.nameEdit);
      this.descEdit = new EditBox(this.minecraft.font, i, row(8), 212, 20, (EditBox)null, Component.translatable("mco.configure.world.description"));
      this.descEdit.setMaxLength(32);
      this.descEdit.setValue(this.serverData.getDescription());
      this.addWidget(this.descEdit);
   }

   public void removed() {
      this.minecraft.keyboardHandler.setSendRepeatsToGui(false);
   }

   public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
      if (pKeyCode == 256) {
         this.minecraft.setScreen(this.configureWorldScreen);
         return true;
      } else {
         return super.keyPressed(pKeyCode, pScanCode, pModifiers);
      }
   }

   public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
      this.renderBackground(pPoseStack);
      drawCenteredString(pPoseStack, this.font, this.title, this.width / 2, 17, 16777215);
      this.font.draw(pPoseStack, NAME_LABEL, (float)(this.width / 2 - 106), (float)row(3), 10526880);
      this.font.draw(pPoseStack, DESCRIPTION_LABEL, (float)(this.width / 2 - 106), (float)row(7), 10526880);
      this.nameEdit.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
      this.descEdit.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
      super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
   }

   public void save() {
      this.configureWorldScreen.saveSettings(this.nameEdit.getValue(), this.descEdit.getValue());
   }
}