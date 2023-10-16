package com.mojang.realmsclient.gui.screens;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.RealmsServer;
import javax.annotation.Nullable;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsInviteScreen extends RealmsScreen {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Component NAME_LABEL = Component.translatable("mco.configure.world.invite.profile.name");
   private static final Component NO_SUCH_PLAYER_ERROR_TEXT = Component.translatable("mco.configure.world.players.error");
   private EditBox profileName;
   private final RealmsServer serverData;
   private final RealmsConfigureWorldScreen configureScreen;
   private final Screen lastScreen;
   @Nullable
   private Component errorMsg;

   public RealmsInviteScreen(RealmsConfigureWorldScreen pConfigureScreen, Screen pLastScreen, RealmsServer pServerData) {
      super(GameNarrator.NO_TITLE);
      this.configureScreen = pConfigureScreen;
      this.lastScreen = pLastScreen;
      this.serverData = pServerData;
   }

   public void tick() {
      this.profileName.tick();
   }

   public void init() {
      this.minecraft.keyboardHandler.setSendRepeatsToGui(true);
      this.profileName = new EditBox(this.minecraft.font, this.width / 2 - 100, row(2), 200, 20, (EditBox)null, Component.translatable("mco.configure.world.invite.profile.name"));
      this.addWidget(this.profileName);
      this.setInitialFocus(this.profileName);
      this.addRenderableWidget(new Button(this.width / 2 - 100, row(10), 200, 20, Component.translatable("mco.configure.world.buttons.invite"), (p_88721_) -> {
         this.onInvite();
      }));
      this.addRenderableWidget(new Button(this.width / 2 - 100, row(12), 200, 20, CommonComponents.GUI_CANCEL, (p_88716_) -> {
         this.minecraft.setScreen(this.lastScreen);
      }));
   }

   public void removed() {
      this.minecraft.keyboardHandler.setSendRepeatsToGui(false);
   }

   private void onInvite() {
      RealmsClient realmsclient = RealmsClient.create();
      if (this.profileName.getValue() != null && !this.profileName.getValue().isEmpty()) {
         try {
            RealmsServer realmsserver = realmsclient.invite(this.serverData.id, this.profileName.getValue().trim());
            if (realmsserver != null) {
               this.serverData.players = realmsserver.players;
               this.minecraft.setScreen(new RealmsPlayerScreen(this.configureScreen, this.serverData));
            } else {
               this.showError(NO_SUCH_PLAYER_ERROR_TEXT);
            }
         } catch (Exception exception) {
            LOGGER.error("Couldn't invite user");
            this.showError(NO_SUCH_PLAYER_ERROR_TEXT);
         }

      } else {
         this.showError(NO_SUCH_PLAYER_ERROR_TEXT);
      }
   }

   private void showError(Component pErrorMsg) {
      this.errorMsg = pErrorMsg;
      this.minecraft.getNarrator().sayNow(pErrorMsg);
   }

   public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
      if (pKeyCode == 256) {
         this.minecraft.setScreen(this.lastScreen);
         return true;
      } else {
         return super.keyPressed(pKeyCode, pScanCode, pModifiers);
      }
   }

   public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
      this.renderBackground(pPoseStack);
      this.font.draw(pPoseStack, NAME_LABEL, (float)(this.width / 2 - 100), (float)row(1), 10526880);
      if (this.errorMsg != null) {
         drawCenteredString(pPoseStack, this.font, this.errorMsg, this.width / 2, row(5), 16711680);
      }

      this.profileName.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
      super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
   }
}