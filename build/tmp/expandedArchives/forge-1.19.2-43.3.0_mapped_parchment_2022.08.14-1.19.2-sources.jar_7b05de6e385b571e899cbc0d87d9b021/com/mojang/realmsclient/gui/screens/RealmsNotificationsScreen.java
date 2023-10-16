package com.mojang.realmsclient.gui.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.RealmsDataFetcher;
import com.mojang.realmsclient.gui.task.DataFetcher;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RealmsNotificationsScreen extends RealmsScreen {
   private static final ResourceLocation INVITE_ICON_LOCATION = new ResourceLocation("realms", "textures/gui/realms/invite_icon.png");
   private static final ResourceLocation TRIAL_ICON_LOCATION = new ResourceLocation("realms", "textures/gui/realms/trial_icon.png");
   private static final ResourceLocation NEWS_ICON_LOCATION = new ResourceLocation("realms", "textures/gui/realms/news_notification_mainscreen.png");
   @Nullable
   private DataFetcher.Subscription realmsDataSubscription;
   private volatile int numberOfPendingInvites;
   static boolean checkedMcoAvailability;
   private static boolean trialAvailable;
   static boolean validClient;
   private static boolean hasUnreadNews;

   public RealmsNotificationsScreen() {
      super(GameNarrator.NO_TITLE);
   }

   public void init() {
      this.checkIfMcoEnabled();
      this.minecraft.keyboardHandler.setSendRepeatsToGui(true);
      if (this.realmsDataSubscription != null) {
         this.realmsDataSubscription.forceUpdate();
      }

   }

   public void tick() {
      boolean flag = this.getRealmsNotificationsEnabled() && this.inTitleScreen() && validClient;
      if (this.realmsDataSubscription == null && flag) {
         this.realmsDataSubscription = this.initDataFetcher(this.minecraft.realmsDataFetcher());
      } else if (this.realmsDataSubscription != null && !flag) {
         this.realmsDataSubscription = null;
      }

      if (this.realmsDataSubscription != null) {
         this.realmsDataSubscription.tick();
      }

   }

   private DataFetcher.Subscription initDataFetcher(RealmsDataFetcher p_238855_) {
      DataFetcher.Subscription datafetcher$subscription = p_238855_.dataFetcher.createSubscription();
      datafetcher$subscription.subscribe(p_238855_.pendingInvitesTask, (p_239521_) -> {
         this.numberOfPendingInvites = p_239521_;
      });
      datafetcher$subscription.subscribe(p_238855_.trialAvailabilityTask, (p_239494_) -> {
         trialAvailable = p_239494_;
      });
      datafetcher$subscription.subscribe(p_238855_.newsTask, (p_238946_) -> {
         p_238855_.newsManager.updateUnreadNews(p_238946_);
         hasUnreadNews = p_238855_.newsManager.hasUnreadNews();
      });
      return datafetcher$subscription;
   }

   private boolean getRealmsNotificationsEnabled() {
      return this.minecraft.options.realmsNotifications().get();
   }

   private boolean inTitleScreen() {
      return this.minecraft.screen instanceof TitleScreen;
   }

   private void checkIfMcoEnabled() {
      if (!checkedMcoAvailability) {
         checkedMcoAvailability = true;
         (new Thread("Realms Notification Availability checker #1") {
            public void run() {
               RealmsClient realmsclient = RealmsClient.create();

               try {
                  RealmsClient.CompatibleVersionResponse realmsclient$compatibleversionresponse = realmsclient.clientCompatible();
                  if (realmsclient$compatibleversionresponse != RealmsClient.CompatibleVersionResponse.COMPATIBLE) {
                     return;
                  }
               } catch (RealmsServiceException realmsserviceexception) {
                  if (realmsserviceexception.httpResultCode != 401) {
                     RealmsNotificationsScreen.checkedMcoAvailability = false;
                  }

                  return;
               }

               RealmsNotificationsScreen.validClient = true;
            }
         }).start();
      }

   }

   public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
      if (validClient) {
         this.drawIcons(pPoseStack, pMouseX, pMouseY);
      }

      super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
   }

   private void drawIcons(PoseStack pPoseStack, int p_88834_, int p_88835_) {
      int i = this.numberOfPendingInvites;
      int j = 24;
      int k = this.height / 4 + 48;
      int l = this.width / 2 + 80;
      int i1 = k + 48 + 2;
      int j1 = 0;
      if (hasUnreadNews) {
         RenderSystem.setShaderTexture(0, NEWS_ICON_LOCATION);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         pPoseStack.pushPose();
         pPoseStack.scale(0.4F, 0.4F, 0.4F);
         GuiComponent.blit(pPoseStack, (int)((double)(l + 2 - j1) * 2.5D), (int)((double)i1 * 2.5D), 0.0F, 0.0F, 40, 40, 40, 40);
         pPoseStack.popPose();
         j1 += 14;
      }

      if (i != 0) {
         RenderSystem.setShaderTexture(0, INVITE_ICON_LOCATION);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         GuiComponent.blit(pPoseStack, l - j1, i1 - 6, 0.0F, 0.0F, 15, 25, 31, 25);
         j1 += 16;
      }

      if (trialAvailable) {
         RenderSystem.setShaderTexture(0, TRIAL_ICON_LOCATION);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         int k1 = 0;
         if ((Util.getMillis() / 800L & 1L) == 1L) {
            k1 = 8;
         }

         GuiComponent.blit(pPoseStack, l + 4 - j1, i1 + 4, 0.0F, (float)k1, 8, 8, 8, 16);
      }

   }
}