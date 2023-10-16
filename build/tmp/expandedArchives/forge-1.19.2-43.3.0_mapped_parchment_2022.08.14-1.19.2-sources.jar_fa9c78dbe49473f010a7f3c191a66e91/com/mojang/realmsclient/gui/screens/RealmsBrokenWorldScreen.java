package com.mojang.realmsclient.gui.screens;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsWorldOptions;
import com.mojang.realmsclient.dto.WorldDownload;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.RealmsWorldSlotButton;
import com.mojang.realmsclient.util.RealmsTextureManager;
import com.mojang.realmsclient.util.task.OpenServerTask;
import com.mojang.realmsclient.util.task.SwitchSlotTask;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsBrokenWorldScreen extends RealmsScreen {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int DEFAULT_BUTTON_WIDTH = 80;
   private final Screen lastScreen;
   private final RealmsMainScreen mainScreen;
   @Nullable
   private RealmsServer serverData;
   private final long serverId;
   private final Component[] message = new Component[]{Component.translatable("mco.brokenworld.message.line1"), Component.translatable("mco.brokenworld.message.line2")};
   private int leftX;
   private int rightX;
   private final List<Integer> slotsThatHasBeenDownloaded = Lists.newArrayList();
   private int animTick;

   public RealmsBrokenWorldScreen(Screen pLastScreen, RealmsMainScreen pMainScreen, long pServerId, boolean pIsMinigame) {
      super(pIsMinigame ? Component.translatable("mco.brokenworld.minigame.title") : Component.translatable("mco.brokenworld.title"));
      this.lastScreen = pLastScreen;
      this.mainScreen = pMainScreen;
      this.serverId = pServerId;
   }

   public void init() {
      this.leftX = this.width / 2 - 150;
      this.rightX = this.width / 2 + 190;
      this.addRenderableWidget(new Button(this.rightX - 80 + 8, row(13) - 5, 70, 20, CommonComponents.GUI_BACK, (p_88333_) -> {
         this.backButtonClicked();
      }));
      if (this.serverData == null) {
         this.fetchServerData(this.serverId);
      } else {
         this.addButtons();
      }

      this.minecraft.keyboardHandler.setSendRepeatsToGui(true);
   }

   public Component getNarrationMessage() {
      return ComponentUtils.formatList(Stream.concat(Stream.of(this.title), Stream.of(this.message)).collect(Collectors.toList()), Component.literal(" "));
   }

   private void addButtons() {
      for(Map.Entry<Integer, RealmsWorldOptions> entry : this.serverData.slots.entrySet()) {
         int i = entry.getKey();
         boolean flag = i != this.serverData.activeSlot || this.serverData.worldType == RealmsServer.WorldType.MINIGAME;
         Button button;
         if (flag) {
            button = new Button(this.getFramePositionX(i), row(8), 80, 20, Component.translatable("mco.brokenworld.play"), (p_88347_) -> {
               if ((this.serverData.slots.get(i)).empty) {
                  RealmsResetWorldScreen realmsresetworldscreen = new RealmsResetWorldScreen(this, this.serverData, Component.translatable("mco.configure.world.switch.slot"), Component.translatable("mco.configure.world.switch.slot.subtitle"), 10526880, CommonComponents.GUI_CANCEL, this::doSwitchOrReset, () -> {
                     this.minecraft.setScreen(this);
                     this.doSwitchOrReset();
                  });
                  realmsresetworldscreen.setSlot(i);
                  realmsresetworldscreen.setResetTitle(Component.translatable("mco.create.world.reset.title"));
                  this.minecraft.setScreen(realmsresetworldscreen);
               } else {
                  this.minecraft.setScreen(new RealmsLongRunningMcoTaskScreen(this.lastScreen, new SwitchSlotTask(this.serverData.id, i, this::doSwitchOrReset)));
               }

            });
         } else {
            button = new Button(this.getFramePositionX(i), row(8), 80, 20, Component.translatable("mco.brokenworld.download"), (p_88339_) -> {
               Component component = Component.translatable("mco.configure.world.restore.download.question.line1");
               Component component1 = Component.translatable("mco.configure.world.restore.download.question.line2");
               this.minecraft.setScreen(new RealmsLongConfirmationScreen((p_167370_) -> {
                  if (p_167370_) {
                     this.downloadWorld(i);
                  } else {
                     this.minecraft.setScreen(this);
                  }

               }, RealmsLongConfirmationScreen.Type.Info, component, component1, true));
            });
         }

         if (this.slotsThatHasBeenDownloaded.contains(i)) {
            button.active = false;
            button.setMessage(Component.translatable("mco.brokenworld.downloaded"));
         }

         this.addRenderableWidget(button);
         this.addRenderableWidget(new Button(this.getFramePositionX(i), row(10), 80, 20, Component.translatable("mco.brokenworld.reset"), (p_88309_) -> {
            RealmsResetWorldScreen realmsresetworldscreen = new RealmsResetWorldScreen(this, this.serverData, this::doSwitchOrReset, () -> {
               this.minecraft.setScreen(this);
               this.doSwitchOrReset();
            });
            if (i != this.serverData.activeSlot || this.serverData.worldType == RealmsServer.WorldType.MINIGAME) {
               realmsresetworldscreen.setSlot(i);
            }

            this.minecraft.setScreen(realmsresetworldscreen);
         }));
      }

   }

   public void tick() {
      ++this.animTick;
   }

   public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
      this.renderBackground(pPoseStack);
      super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
      drawCenteredString(pPoseStack, this.font, this.title, this.width / 2, 17, 16777215);

      for(int i = 0; i < this.message.length; ++i) {
         drawCenteredString(pPoseStack, this.font, this.message[i], this.width / 2, row(-1) + 3 + i * 12, 10526880);
      }

      if (this.serverData != null) {
         for(Map.Entry<Integer, RealmsWorldOptions> entry : this.serverData.slots.entrySet()) {
            if ((entry.getValue()).templateImage != null && (entry.getValue()).templateId != -1L) {
               this.drawSlotFrame(pPoseStack, this.getFramePositionX(entry.getKey()), row(1) + 5, pMouseX, pMouseY, this.serverData.activeSlot == entry.getKey() && !this.isMinigame(), entry.getValue().getSlotName(entry.getKey()), entry.getKey(), (entry.getValue()).templateId, (entry.getValue()).templateImage, (entry.getValue()).empty);
            } else {
               this.drawSlotFrame(pPoseStack, this.getFramePositionX(entry.getKey()), row(1) + 5, pMouseX, pMouseY, this.serverData.activeSlot == entry.getKey() && !this.isMinigame(), entry.getValue().getSlotName(entry.getKey()), entry.getKey(), -1L, (String)null, (entry.getValue()).empty);
            }
         }

      }
   }

   private int getFramePositionX(int p_88302_) {
      return this.leftX + (p_88302_ - 1) * 110;
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
      this.minecraft.setScreen(this.lastScreen);
   }

   private void fetchServerData(long pServerId) {
      (new Thread(() -> {
         RealmsClient realmsclient = RealmsClient.create();

         try {
            this.serverData = realmsclient.getOwnWorld(pServerId);
            this.addButtons();
         } catch (RealmsServiceException realmsserviceexception) {
            LOGGER.error("Couldn't get own world");
            this.minecraft.setScreen(new RealmsGenericErrorScreen(Component.nullToEmpty(realmsserviceexception.getMessage()), this.lastScreen));
         }

      })).start();
   }

   public void doSwitchOrReset() {
      (new Thread(() -> {
         RealmsClient realmsclient = RealmsClient.create();
         if (this.serverData.state == RealmsServer.State.CLOSED) {
            this.minecraft.execute(() -> {
               this.minecraft.setScreen(new RealmsLongRunningMcoTaskScreen(this, new OpenServerTask(this.serverData, this, this.mainScreen, true, this.minecraft)));
            });
         } else {
            try {
               RealmsServer realmsserver = realmsclient.getOwnWorld(this.serverId);
               this.minecraft.execute(() -> {
                  this.mainScreen.newScreen().play(realmsserver, this);
               });
            } catch (RealmsServiceException realmsserviceexception) {
               LOGGER.error("Couldn't get own world");
               this.minecraft.execute(() -> {
                  this.minecraft.setScreen(this.lastScreen);
               });
            }
         }

      })).start();
   }

   private void downloadWorld(int pSlotIndex) {
      RealmsClient realmsclient = RealmsClient.create();

      try {
         WorldDownload worlddownload = realmsclient.requestDownloadInfo(this.serverData.id, pSlotIndex);
         RealmsDownloadLatestWorldScreen realmsdownloadlatestworldscreen = new RealmsDownloadLatestWorldScreen(this, worlddownload, this.serverData.getWorldName(pSlotIndex), (p_88312_) -> {
            if (p_88312_) {
               this.slotsThatHasBeenDownloaded.add(pSlotIndex);
               this.clearWidgets();
               this.addButtons();
            } else {
               this.minecraft.setScreen(this);
            }

         });
         this.minecraft.setScreen(realmsdownloadlatestworldscreen);
      } catch (RealmsServiceException realmsserviceexception) {
         LOGGER.error("Couldn't download world data");
         this.minecraft.setScreen(new RealmsGenericErrorScreen(realmsserviceexception, this));
      }

   }

   private boolean isMinigame() {
      return this.serverData != null && this.serverData.worldType == RealmsServer.WorldType.MINIGAME;
   }

   private void drawSlotFrame(PoseStack pPoseStack, int pX, int pY, int pMouseX, int pMouseY, boolean pIsActiveNonMinigame, String pText, int pSlotIndex, long pSlotName, @Nullable String pTemplateId, boolean pHastemplateImage) {
      if (pHastemplateImage) {
         RenderSystem.setShaderTexture(0, RealmsWorldSlotButton.EMPTY_SLOT_LOCATION);
      } else if (pTemplateId != null && pSlotName != -1L) {
         RealmsTextureManager.bindWorldTemplate(String.valueOf(pSlotName), pTemplateId);
      } else if (pSlotIndex == 1) {
         RenderSystem.setShaderTexture(0, RealmsWorldSlotButton.DEFAULT_WORLD_SLOT_1);
      } else if (pSlotIndex == 2) {
         RenderSystem.setShaderTexture(0, RealmsWorldSlotButton.DEFAULT_WORLD_SLOT_2);
      } else if (pSlotIndex == 3) {
         RenderSystem.setShaderTexture(0, RealmsWorldSlotButton.DEFAULT_WORLD_SLOT_3);
      } else {
         RealmsTextureManager.bindWorldTemplate(String.valueOf(this.serverData.minigameId), this.serverData.minigameImage);
      }

      if (!pIsActiveNonMinigame) {
         RenderSystem.setShaderColor(0.56F, 0.56F, 0.56F, 1.0F);
      } else if (pIsActiveNonMinigame) {
         float f = 0.9F + 0.1F * Mth.cos((float)this.animTick * 0.2F);
         RenderSystem.setShaderColor(f, f, f, 1.0F);
      }

      GuiComponent.blit(pPoseStack, pX + 3, pY + 3, 0.0F, 0.0F, 74, 74, 74, 74);
      RenderSystem.setShaderTexture(0, RealmsWorldSlotButton.SLOT_FRAME_LOCATION);
      if (pIsActiveNonMinigame) {
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      } else {
         RenderSystem.setShaderColor(0.56F, 0.56F, 0.56F, 1.0F);
      }

      GuiComponent.blit(pPoseStack, pX, pY, 0.0F, 0.0F, 80, 80, 80, 80);
      drawCenteredString(pPoseStack, this.font, pText, pX + 40, pY + 66, 16777215);
   }
}