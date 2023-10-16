package com.mojang.realmsclient.gui.screens;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsWorldOptions;
import com.mojang.realmsclient.dto.WorldTemplate;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.RealmsWorldSlotButton;
import com.mojang.realmsclient.util.task.CloseServerTask;
import com.mojang.realmsclient.util.task.OpenServerTask;
import com.mojang.realmsclient.util.task.SwitchMinigameTask;
import com.mojang.realmsclient.util.task.SwitchSlotTask;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsConfigureWorldScreen extends RealmsScreen {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final ResourceLocation ON_ICON_LOCATION = new ResourceLocation("realms", "textures/gui/realms/on_icon.png");
   private static final ResourceLocation OFF_ICON_LOCATION = new ResourceLocation("realms", "textures/gui/realms/off_icon.png");
   private static final ResourceLocation EXPIRED_ICON_LOCATION = new ResourceLocation("realms", "textures/gui/realms/expired_icon.png");
   private static final ResourceLocation EXPIRES_SOON_ICON_LOCATION = new ResourceLocation("realms", "textures/gui/realms/expires_soon_icon.png");
   private static final Component WORLD_LIST_TITLE = Component.translatable("mco.configure.worlds.title");
   private static final Component TITLE = Component.translatable("mco.configure.world.title");
   private static final Component MINIGAME_PREFIX = Component.translatable("mco.configure.current.minigame").append(": ");
   private static final Component SERVER_EXPIRED_TOOLTIP = Component.translatable("mco.selectServer.expired");
   private static final Component SERVER_EXPIRING_SOON_TOOLTIP = Component.translatable("mco.selectServer.expires.soon");
   private static final Component SERVER_EXPIRING_IN_DAY_TOOLTIP = Component.translatable("mco.selectServer.expires.day");
   private static final Component SERVER_OPEN_TOOLTIP = Component.translatable("mco.selectServer.open");
   private static final Component SERVER_CLOSED_TOOLTIP = Component.translatable("mco.selectServer.closed");
   private static final int DEFAULT_BUTTON_WIDTH = 80;
   private static final int DEFAULT_BUTTON_OFFSET = 5;
   @Nullable
   private Component toolTip;
   private final RealmsMainScreen lastScreen;
   @Nullable
   private RealmsServer serverData;
   private final long serverId;
   private int leftX;
   private int rightX;
   private Button playersButton;
   private Button settingsButton;
   private Button subscriptionButton;
   private Button optionsButton;
   private Button backupButton;
   private Button resetWorldButton;
   private Button switchMinigameButton;
   private boolean stateChanged;
   private int animTick;
   private int clicks;
   private final List<RealmsWorldSlotButton> slotButtonList = Lists.newArrayList();

   public RealmsConfigureWorldScreen(RealmsMainScreen pLastScreen, long pServerId) {
      super(TITLE);
      this.lastScreen = pLastScreen;
      this.serverId = pServerId;
   }

   public void init() {
      if (this.serverData == null) {
         this.fetchServerData(this.serverId);
      }

      this.leftX = this.width / 2 - 187;
      this.rightX = this.width / 2 + 190;
      this.minecraft.keyboardHandler.setSendRepeatsToGui(true);
      this.playersButton = this.addRenderableWidget(new Button(this.centerButton(0, 3), row(0), 100, 20, Component.translatable("mco.configure.world.buttons.players"), (p_88532_) -> {
         this.minecraft.setScreen(new RealmsPlayerScreen(this, this.serverData));
      }));
      this.settingsButton = this.addRenderableWidget(new Button(this.centerButton(1, 3), row(0), 100, 20, Component.translatable("mco.configure.world.buttons.settings"), (p_88530_) -> {
         this.minecraft.setScreen(new RealmsSettingsScreen(this, this.serverData.clone()));
      }));
      this.subscriptionButton = this.addRenderableWidget(new Button(this.centerButton(2, 3), row(0), 100, 20, Component.translatable("mco.configure.world.buttons.subscription"), (p_88527_) -> {
         this.minecraft.setScreen(new RealmsSubscriptionInfoScreen(this, this.serverData.clone(), this.lastScreen));
      }));
      this.slotButtonList.clear();

      for(int i = 1; i < 5; ++i) {
         this.slotButtonList.add(this.addSlotButton(i));
      }

      this.switchMinigameButton = this.addRenderableWidget(new Button(this.leftButton(0), row(13) - 5, 100, 20, Component.translatable("mco.configure.world.buttons.switchminigame"), (p_88524_) -> {
         this.minecraft.setScreen(new RealmsSelectWorldTemplateScreen(Component.translatable("mco.template.title.minigame"), this::templateSelectionCallback, RealmsServer.WorldType.MINIGAME));
      }));
      this.optionsButton = this.addRenderableWidget(new Button(this.leftButton(0), row(13) - 5, 90, 20, Component.translatable("mco.configure.world.buttons.options"), (p_88522_) -> {
         this.minecraft.setScreen(new RealmsSlotOptionsScreen(this, this.serverData.slots.get(this.serverData.activeSlot).clone(), this.serverData.worldType, this.serverData.activeSlot));
      }));
      this.backupButton = this.addRenderableWidget(new Button(this.leftButton(1), row(13) - 5, 90, 20, Component.translatable("mco.configure.world.backup"), (p_88514_) -> {
         this.minecraft.setScreen(new RealmsBackupScreen(this, this.serverData.clone(), this.serverData.activeSlot));
      }));
      this.resetWorldButton = this.addRenderableWidget(new Button(this.leftButton(2), row(13) - 5, 90, 20, Component.translatable("mco.configure.world.buttons.resetworld"), (p_88496_) -> {
         this.minecraft.setScreen(new RealmsResetWorldScreen(this, this.serverData.clone(), () -> {
            this.minecraft.execute(() -> {
               this.minecraft.setScreen(this.getNewScreen());
            });
         }, () -> {
            this.minecraft.setScreen(this.getNewScreen());
         }));
      }));
      this.addRenderableWidget(new Button(this.rightX - 80 + 8, row(13) - 5, 70, 20, CommonComponents.GUI_BACK, (p_167407_) -> {
         this.backButtonClicked();
      }));
      this.backupButton.active = true;
      if (this.serverData == null) {
         this.hideMinigameButtons();
         this.hideRegularButtons();
         this.playersButton.active = false;
         this.settingsButton.active = false;
         this.subscriptionButton.active = false;
      } else {
         this.disableButtons();
         if (this.isMinigame()) {
            this.hideRegularButtons();
         } else {
            this.hideMinigameButtons();
         }
      }

   }

   private RealmsWorldSlotButton addSlotButton(int p_167386_) {
      int i = this.frame(p_167386_);
      int j = row(5) + 5;
      RealmsWorldSlotButton realmsworldslotbutton = new RealmsWorldSlotButton(i, j, 80, 80, () -> {
         return this.serverData;
      }, (p_167399_) -> {
         this.toolTip = p_167399_;
      }, p_167386_, (p_167389_) -> {
         RealmsWorldSlotButton.State realmsworldslotbutton$state = ((RealmsWorldSlotButton)p_167389_).getState();
         if (realmsworldslotbutton$state != null) {
            switch (realmsworldslotbutton$state.action) {
               case NOTHING:
                  break;
               case JOIN:
                  this.joinRealm(this.serverData);
                  break;
               case SWITCH_SLOT:
                  if (realmsworldslotbutton$state.minigame) {
                     this.switchToMinigame();
                  } else if (realmsworldslotbutton$state.empty) {
                     this.switchToEmptySlot(p_167386_, this.serverData);
                  } else {
                     this.switchToFullSlot(p_167386_, this.serverData);
                  }
                  break;
               default:
                  throw new IllegalStateException("Unknown action " + realmsworldslotbutton$state.action);
            }
         }

      });
      return this.addRenderableWidget(realmsworldslotbutton);
   }

   private int leftButton(int p_88464_) {
      return this.leftX + p_88464_ * 95;
   }

   private int centerButton(int p_88466_, int p_88467_) {
      return this.width / 2 - (p_88467_ * 105 - 5) / 2 + p_88466_ * 105;
   }

   public void tick() {
      super.tick();
      ++this.animTick;
      --this.clicks;
      if (this.clicks < 0) {
         this.clicks = 0;
      }

      this.slotButtonList.forEach(RealmsWorldSlotButton::tick);
   }

   public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
      this.toolTip = null;
      this.renderBackground(pPoseStack);
      drawCenteredString(pPoseStack, this.font, WORLD_LIST_TITLE, this.width / 2, row(4), 16777215);
      super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
      if (this.serverData == null) {
         drawCenteredString(pPoseStack, this.font, this.title, this.width / 2, 17, 16777215);
      } else {
         String s = this.serverData.getName();
         int i = this.font.width(s);
         int j = this.serverData.state == RealmsServer.State.CLOSED ? 10526880 : 8388479;
         int k = this.font.width(this.title);
         drawCenteredString(pPoseStack, this.font, this.title, this.width / 2, 12, 16777215);
         drawCenteredString(pPoseStack, this.font, s, this.width / 2, 24, j);
         int l = Math.min(this.centerButton(2, 3) + 80 - 11, this.width / 2 + i / 2 + k / 2 + 10);
         this.drawServerStatus(pPoseStack, l, 7, pMouseX, pMouseY);
         if (this.isMinigame()) {
            this.font.draw(pPoseStack, MINIGAME_PREFIX.copy().append(this.serverData.getMinigameName()), (float)(this.leftX + 80 + 20 + 10), (float)row(13), 16777215);
         }

         if (this.toolTip != null) {
            this.renderMousehoverTooltip(pPoseStack, this.toolTip, pMouseX, pMouseY);
         }

      }
   }

   private int frame(int p_88488_) {
      return this.leftX + (p_88488_ - 1) * 98;
   }

   public void removed() {
      this.minecraft.keyboardHandler.setSendRepeatsToGui(false);
   }

   public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
      if (pKeyCode == 256) {
         this.backButtonClicked();
         return true;
      } else {
         return super.keyPressed(pKeyCode, pScanCode, pModifiers);
      }
   }

   private void backButtonClicked() {
      if (this.stateChanged) {
         this.lastScreen.resetScreen();
      }

      this.minecraft.setScreen(this.lastScreen);
   }

   private void fetchServerData(long pServerId) {
      (new Thread(() -> {
         RealmsClient realmsclient = RealmsClient.create();

         try {
            RealmsServer realmsserver = realmsclient.getOwnWorld(pServerId);
            this.minecraft.execute(() -> {
               this.serverData = realmsserver;
               this.disableButtons();
               if (this.isMinigame()) {
                  this.show(this.switchMinigameButton);
               } else {
                  this.show(this.optionsButton);
                  this.show(this.backupButton);
                  this.show(this.resetWorldButton);
               }

            });
         } catch (RealmsServiceException realmsserviceexception) {
            LOGGER.error("Couldn't get own world");
            this.minecraft.execute(() -> {
               this.minecraft.setScreen(new RealmsGenericErrorScreen(Component.nullToEmpty(realmsserviceexception.getMessage()), this.lastScreen));
            });
         }

      })).start();
   }

   private void disableButtons() {
      this.playersButton.active = !this.serverData.expired;
      this.settingsButton.active = !this.serverData.expired;
      this.subscriptionButton.active = true;
      this.switchMinigameButton.active = !this.serverData.expired;
      this.optionsButton.active = !this.serverData.expired;
      this.resetWorldButton.active = !this.serverData.expired;
   }

   private void joinRealm(RealmsServer pServer) {
      if (this.serverData.state == RealmsServer.State.OPEN) {
         this.lastScreen.play(pServer, new RealmsConfigureWorldScreen(this.lastScreen.newScreen(), this.serverId));
      } else {
         this.openTheWorld(true, new RealmsConfigureWorldScreen(this.lastScreen.newScreen(), this.serverId));
      }

   }

   private void switchToMinigame() {
      RealmsSelectWorldTemplateScreen realmsselectworldtemplatescreen = new RealmsSelectWorldTemplateScreen(Component.translatable("mco.template.title.minigame"), this::templateSelectionCallback, RealmsServer.WorldType.MINIGAME);
      realmsselectworldtemplatescreen.setWarning(Component.translatable("mco.minigame.world.info.line1"), Component.translatable("mco.minigame.world.info.line2"));
      this.minecraft.setScreen(realmsselectworldtemplatescreen);
   }

   private void switchToFullSlot(int pSlot, RealmsServer pServer) {
      Component component = Component.translatable("mco.configure.world.slot.switch.question.line1");
      Component component1 = Component.translatable("mco.configure.world.slot.switch.question.line2");
      this.minecraft.setScreen(new RealmsLongConfirmationScreen((p_167405_) -> {
         if (p_167405_) {
            this.minecraft.setScreen(new RealmsLongRunningMcoTaskScreen(this.lastScreen, new SwitchSlotTask(pServer.id, pSlot, () -> {
               this.minecraft.execute(() -> {
                  this.minecraft.setScreen(this.getNewScreen());
               });
            })));
         } else {
            this.minecraft.setScreen(this);
         }

      }, RealmsLongConfirmationScreen.Type.Info, component, component1, true));
   }

   private void switchToEmptySlot(int pSlot, RealmsServer pServer) {
      Component component = Component.translatable("mco.configure.world.slot.switch.question.line1");
      Component component1 = Component.translatable("mco.configure.world.slot.switch.question.line2");
      this.minecraft.setScreen(new RealmsLongConfirmationScreen((p_167393_) -> {
         if (p_167393_) {
            RealmsResetWorldScreen realmsresetworldscreen = new RealmsResetWorldScreen(this, pServer, Component.translatable("mco.configure.world.switch.slot"), Component.translatable("mco.configure.world.switch.slot.subtitle"), 10526880, CommonComponents.GUI_CANCEL, () -> {
               this.minecraft.execute(() -> {
                  this.minecraft.setScreen(this.getNewScreen());
               });
            }, () -> {
               this.minecraft.setScreen(this.getNewScreen());
            });
            realmsresetworldscreen.setSlot(pSlot);
            realmsresetworldscreen.setResetTitle(Component.translatable("mco.create.world.reset.title"));
            this.minecraft.setScreen(realmsresetworldscreen);
         } else {
            this.minecraft.setScreen(this);
         }

      }, RealmsLongConfirmationScreen.Type.Info, component, component1, true));
   }

   protected void renderMousehoverTooltip(PoseStack pPoseStack, @Nullable Component pTooltip, int pMouseX, int pMouseY) {
      int i = pMouseX + 12;
      int j = pMouseY - 12;
      int k = this.font.width(pTooltip);
      if (i + k + 3 > this.rightX) {
         i = i - k - 20;
      }

      this.fillGradient(pPoseStack, i - 3, j - 3, i + k + 3, j + 8 + 3, -1073741824, -1073741824);
      this.font.drawShadow(pPoseStack, pTooltip, (float)i, (float)j, 16777215);
   }

   private void drawServerStatus(PoseStack pPoseStack, int pX, int pY, int p_88493_, int p_88494_) {
      if (this.serverData.expired) {
         this.drawExpired(pPoseStack, pX, pY, p_88493_, p_88494_);
      } else if (this.serverData.state == RealmsServer.State.CLOSED) {
         this.drawClose(pPoseStack, pX, pY, p_88493_, p_88494_);
      } else if (this.serverData.state == RealmsServer.State.OPEN) {
         if (this.serverData.daysLeft < 7) {
            this.drawExpiring(pPoseStack, pX, pY, p_88493_, p_88494_, this.serverData.daysLeft);
         } else {
            this.drawOpen(pPoseStack, pX, pY, p_88493_, p_88494_);
         }
      }

   }

   private void drawExpired(PoseStack pPoseStack, int pX, int pY, int p_88502_, int p_88503_) {
      RenderSystem.setShaderTexture(0, EXPIRED_ICON_LOCATION);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      GuiComponent.blit(pPoseStack, pX, pY, 0.0F, 0.0F, 10, 28, 10, 28);
      if (p_88502_ >= pX && p_88502_ <= pX + 9 && p_88503_ >= pY && p_88503_ <= pY + 27) {
         this.toolTip = SERVER_EXPIRED_TOOLTIP;
      }

   }

   private void drawExpiring(PoseStack pPoseStack, int pX, int pY, int p_88477_, int p_88478_, int p_88479_) {
      RenderSystem.setShaderTexture(0, EXPIRES_SOON_ICON_LOCATION);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      if (this.animTick % 20 < 10) {
         GuiComponent.blit(pPoseStack, pX, pY, 0.0F, 0.0F, 10, 28, 20, 28);
      } else {
         GuiComponent.blit(pPoseStack, pX, pY, 10.0F, 0.0F, 10, 28, 20, 28);
      }

      if (p_88477_ >= pX && p_88477_ <= pX + 9 && p_88478_ >= pY && p_88478_ <= pY + 27) {
         if (p_88479_ <= 0) {
            this.toolTip = SERVER_EXPIRING_SOON_TOOLTIP;
         } else if (p_88479_ == 1) {
            this.toolTip = SERVER_EXPIRING_IN_DAY_TOOLTIP;
         } else {
            this.toolTip = Component.translatable("mco.selectServer.expires.days", p_88479_);
         }
      }

   }

   private void drawOpen(PoseStack pPoseStack, int pX, int pY, int p_88511_, int p_88512_) {
      RenderSystem.setShaderTexture(0, ON_ICON_LOCATION);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      GuiComponent.blit(pPoseStack, pX, pY, 0.0F, 0.0F, 10, 28, 10, 28);
      if (p_88511_ >= pX && p_88511_ <= pX + 9 && p_88512_ >= pY && p_88512_ <= pY + 27) {
         this.toolTip = SERVER_OPEN_TOOLTIP;
      }

   }

   private void drawClose(PoseStack pPoseStack, int pX, int pY, int p_88519_, int p_88520_) {
      RenderSystem.setShaderTexture(0, OFF_ICON_LOCATION);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      GuiComponent.blit(pPoseStack, pX, pY, 0.0F, 0.0F, 10, 28, 10, 28);
      if (p_88519_ >= pX && p_88519_ <= pX + 9 && p_88520_ >= pY && p_88520_ <= pY + 27) {
         this.toolTip = SERVER_CLOSED_TOOLTIP;
      }

   }

   private boolean isMinigame() {
      return this.serverData != null && this.serverData.worldType == RealmsServer.WorldType.MINIGAME;
   }

   private void hideRegularButtons() {
      this.hide(this.optionsButton);
      this.hide(this.backupButton);
      this.hide(this.resetWorldButton);
   }

   private void hide(Button pButton) {
      pButton.visible = false;
      this.removeWidget(pButton);
   }

   private void show(Button pButton) {
      pButton.visible = true;
      this.addRenderableWidget(pButton);
   }

   private void hideMinigameButtons() {
      this.hide(this.switchMinigameButton);
   }

   public void saveSlotSettings(RealmsWorldOptions pWorldOptions) {
      RealmsWorldOptions realmsworldoptions = this.serverData.slots.get(this.serverData.activeSlot);
      pWorldOptions.templateId = realmsworldoptions.templateId;
      pWorldOptions.templateImage = realmsworldoptions.templateImage;
      RealmsClient realmsclient = RealmsClient.create();

      try {
         realmsclient.updateSlot(this.serverData.id, this.serverData.activeSlot, pWorldOptions);
         this.serverData.slots.put(this.serverData.activeSlot, pWorldOptions);
      } catch (RealmsServiceException realmsserviceexception) {
         LOGGER.error("Couldn't save slot settings");
         this.minecraft.setScreen(new RealmsGenericErrorScreen(realmsserviceexception, this));
         return;
      }

      this.minecraft.setScreen(this);
   }

   public void saveSettings(String pKey, String pValue) {
      String s = pValue.trim().isEmpty() ? null : pValue;
      RealmsClient realmsclient = RealmsClient.create();

      try {
         realmsclient.update(this.serverData.id, pKey, s);
         this.serverData.setName(pKey);
         this.serverData.setDescription(s);
      } catch (RealmsServiceException realmsserviceexception) {
         LOGGER.error("Couldn't save settings");
         this.minecraft.setScreen(new RealmsGenericErrorScreen(realmsserviceexception, this));
         return;
      }

      this.minecraft.setScreen(this);
   }

   public void openTheWorld(boolean pJoin, Screen pLastScreen) {
      this.minecraft.setScreen(new RealmsLongRunningMcoTaskScreen(pLastScreen, new OpenServerTask(this.serverData, this, this.lastScreen, pJoin, this.minecraft)));
   }

   public void closeTheWorld(Screen pLastScreen) {
      this.minecraft.setScreen(new RealmsLongRunningMcoTaskScreen(pLastScreen, new CloseServerTask(this.serverData, this)));
   }

   public void stateChanged() {
      this.stateChanged = true;
   }

   private void templateSelectionCallback(@Nullable WorldTemplate p_167395_) {
      if (p_167395_ != null && WorldTemplate.WorldTemplateType.MINIGAME == p_167395_.type) {
         this.minecraft.setScreen(new RealmsLongRunningMcoTaskScreen(this.lastScreen, new SwitchMinigameTask(this.serverData.id, p_167395_, this.getNewScreen())));
      } else {
         this.minecraft.setScreen(this);
      }

   }

   public RealmsConfigureWorldScreen getNewScreen() {
      return new RealmsConfigureWorldScreen(this.lastScreen, this.serverId);
   }
}