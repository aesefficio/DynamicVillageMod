package com.mojang.realmsclient;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.RateLimiter;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.mojang.math.Vector3f;
import com.mojang.realmsclient.client.Ping;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.PingResult;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsServerPlayerList;
import com.mojang.realmsclient.dto.RegionPingResult;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.RealmsDataFetcher;
import com.mojang.realmsclient.gui.RealmsNewsManager;
import com.mojang.realmsclient.gui.RealmsServerList;
import com.mojang.realmsclient.gui.screens.RealmsClientOutdatedScreen;
import com.mojang.realmsclient.gui.screens.RealmsConfigureWorldScreen;
import com.mojang.realmsclient.gui.screens.RealmsCreateRealmScreen;
import com.mojang.realmsclient.gui.screens.RealmsGenericErrorScreen;
import com.mojang.realmsclient.gui.screens.RealmsLongConfirmationScreen;
import com.mojang.realmsclient.gui.screens.RealmsLongRunningMcoTaskScreen;
import com.mojang.realmsclient.gui.screens.RealmsParentalConsentScreen;
import com.mojang.realmsclient.gui.screens.RealmsPendingInvitesScreen;
import com.mojang.realmsclient.gui.task.DataFetcher;
import com.mojang.realmsclient.util.RealmsPersistence;
import com.mojang.realmsclient.util.RealmsTextureManager;
import com.mojang.realmsclient.util.task.GetServerDetailsTask;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsObjectSelectionList;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsMainScreen extends RealmsScreen {
   static final Logger LOGGER = LogUtils.getLogger();
   private static final ResourceLocation ON_ICON_LOCATION = new ResourceLocation("realms", "textures/gui/realms/on_icon.png");
   private static final ResourceLocation OFF_ICON_LOCATION = new ResourceLocation("realms", "textures/gui/realms/off_icon.png");
   private static final ResourceLocation EXPIRED_ICON_LOCATION = new ResourceLocation("realms", "textures/gui/realms/expired_icon.png");
   private static final ResourceLocation EXPIRES_SOON_ICON_LOCATION = new ResourceLocation("realms", "textures/gui/realms/expires_soon_icon.png");
   private static final ResourceLocation LEAVE_ICON_LOCATION = new ResourceLocation("realms", "textures/gui/realms/leave_icon.png");
   private static final ResourceLocation INVITATION_ICONS_LOCATION = new ResourceLocation("realms", "textures/gui/realms/invitation_icons.png");
   private static final ResourceLocation INVITE_ICON_LOCATION = new ResourceLocation("realms", "textures/gui/realms/invite_icon.png");
   static final ResourceLocation WORLDICON_LOCATION = new ResourceLocation("realms", "textures/gui/realms/world_icon.png");
   private static final ResourceLocation LOGO_LOCATION = new ResourceLocation("realms", "textures/gui/title/realms.png");
   private static final ResourceLocation CONFIGURE_LOCATION = new ResourceLocation("realms", "textures/gui/realms/configure_icon.png");
   private static final ResourceLocation NEWS_LOCATION = new ResourceLocation("realms", "textures/gui/realms/news_icon.png");
   private static final ResourceLocation POPUP_LOCATION = new ResourceLocation("realms", "textures/gui/realms/popup.png");
   private static final ResourceLocation DARKEN_LOCATION = new ResourceLocation("realms", "textures/gui/realms/darken.png");
   static final ResourceLocation CROSS_ICON_LOCATION = new ResourceLocation("realms", "textures/gui/realms/cross_icon.png");
   private static final ResourceLocation TRIAL_ICON_LOCATION = new ResourceLocation("realms", "textures/gui/realms/trial_icon.png");
   static final ResourceLocation BUTTON_LOCATION = new ResourceLocation("minecraft", "textures/gui/widgets.png");
   static final Component NO_PENDING_INVITES_TEXT = Component.translatable("mco.invites.nopending");
   static final Component PENDING_INVITES_TEXT = Component.translatable("mco.invites.pending");
   static final List<Component> TRIAL_MESSAGE_LINES = ImmutableList.of(Component.translatable("mco.trial.message.line1"), Component.translatable("mco.trial.message.line2"));
   static final Component SERVER_UNITIALIZED_TEXT = Component.translatable("mco.selectServer.uninitialized");
   static final Component SUBSCRIPTION_EXPIRED_TEXT = Component.translatable("mco.selectServer.expiredList");
   static final Component SUBSCRIPTION_RENEW_TEXT = Component.translatable("mco.selectServer.expiredRenew");
   static final Component TRIAL_EXPIRED_TEXT = Component.translatable("mco.selectServer.expiredTrial");
   static final Component SUBSCRIPTION_CREATE_TEXT = Component.translatable("mco.selectServer.expiredSubscribe");
   static final Component SELECT_MINIGAME_PREFIX = Component.translatable("mco.selectServer.minigame").append(" ");
   private static final Component POPUP_TEXT = Component.translatable("mco.selectServer.popup");
   private static final Component SERVER_EXPIRED_TOOLTIP = Component.translatable("mco.selectServer.expired");
   private static final Component SERVER_EXPIRES_SOON_TOOLTIP = Component.translatable("mco.selectServer.expires.soon");
   private static final Component SERVER_EXPIRES_IN_DAY_TOOLTIP = Component.translatable("mco.selectServer.expires.day");
   private static final Component SERVER_OPEN_TOOLTIP = Component.translatable("mco.selectServer.open");
   private static final Component SERVER_CLOSED_TOOLTIP = Component.translatable("mco.selectServer.closed");
   private static final Component LEAVE_SERVER_TOOLTIP = Component.translatable("mco.selectServer.leave");
   private static final Component CONFIGURE_SERVER_TOOLTIP = Component.translatable("mco.selectServer.configure");
   private static final Component NEWS_TOOLTIP = Component.translatable("mco.news");
   static final Component UNITIALIZED_WORLD_NARRATION = Component.translatable("gui.narrate.button", SERVER_UNITIALIZED_TEXT);
   static final Component TRIAL_TEXT = CommonComponents.joinLines(TRIAL_MESSAGE_LINES);
   private static List<ResourceLocation> teaserImages = ImmutableList.of();
   @Nullable
   private DataFetcher.Subscription dataSubscription;
   private RealmsServerList serverList;
   static boolean overrideConfigure;
   private static int lastScrollYPosition = -1;
   static volatile boolean hasParentalConsent;
   static volatile boolean checkedParentalConsent;
   static volatile boolean checkedClientCompatability;
   @Nullable
   static Screen realmsGenericErrorScreen;
   private static boolean regionsPinged;
   private final RateLimiter inviteNarrationLimiter;
   private boolean dontSetConnectedToRealms;
   final Screen lastScreen;
   RealmsMainScreen.RealmSelectionList realmSelectionList;
   private boolean realmsSelectionListAdded;
   private Button playButton;
   private Button backButton;
   private Button renewButton;
   private Button configureButton;
   private Button leaveButton;
   @Nullable
   private List<Component> toolTip;
   private List<RealmsServer> realmsServers = ImmutableList.of();
   volatile int numberOfPendingInvites;
   int animTick;
   private boolean hasFetchedServers;
   boolean popupOpenedByUser;
   private boolean justClosedPopup;
   private volatile boolean trialsAvailable;
   private volatile boolean createdTrial;
   private volatile boolean showingPopup;
   volatile boolean hasUnreadNews;
   @Nullable
   volatile String newsLink;
   private int carouselIndex;
   private int carouselTick;
   private boolean hasSwitchedCarouselImage;
   private List<KeyCombo> keyCombos;
   long lastClickTime;
   private ReentrantLock connectLock = new ReentrantLock();
   private MultiLineLabel formattedPopup = MultiLineLabel.EMPTY;
   RealmsMainScreen.HoveredElement hoveredElement;
   private Button showPopupButton;
   private RealmsMainScreen.PendingInvitesButton pendingInvitesButton;
   private Button newsButton;
   private Button createTrialButton;
   private Button buyARealmButton;
   private Button closeButton;

   public RealmsMainScreen(Screen pLastScreen) {
      super(GameNarrator.NO_TITLE);
      this.lastScreen = pLastScreen;
      this.inviteNarrationLimiter = RateLimiter.create((double)0.016666668F);
   }

   private boolean shouldShowMessageInList() {
      if (hasParentalConsent() && this.hasFetchedServers) {
         if (this.trialsAvailable && !this.createdTrial) {
            return true;
         } else {
            for(RealmsServer realmsserver : this.realmsServers) {
               if (realmsserver.ownerUUID.equals(this.minecraft.getUser().getUuid())) {
                  return false;
               }
            }

            return true;
         }
      } else {
         return false;
      }
   }

   public boolean shouldShowPopup() {
      if (hasParentalConsent() && this.hasFetchedServers) {
         return this.popupOpenedByUser ? true : this.realmsServers.isEmpty();
      } else {
         return false;
      }
   }

   public void init() {
      this.keyCombos = Lists.newArrayList(new KeyCombo(new char[]{'3', '2', '1', '4', '5', '6'}, () -> {
         overrideConfigure = !overrideConfigure;
      }), new KeyCombo(new char[]{'9', '8', '7', '1', '2', '3'}, () -> {
         if (RealmsClient.currentEnvironment == RealmsClient.Environment.STAGE) {
            this.switchToProd();
         } else {
            this.switchToStage();
         }

      }), new KeyCombo(new char[]{'9', '8', '7', '4', '5', '6'}, () -> {
         if (RealmsClient.currentEnvironment == RealmsClient.Environment.LOCAL) {
            this.switchToProd();
         } else {
            this.switchToLocal();
         }

      }));
      if (realmsGenericErrorScreen != null) {
         this.minecraft.setScreen(realmsGenericErrorScreen);
      } else {
         this.connectLock = new ReentrantLock();
         if (checkedClientCompatability && !hasParentalConsent()) {
            this.checkParentalConsent();
         }

         this.checkClientCompatability();
         if (!this.dontSetConnectedToRealms) {
            this.minecraft.setConnectedToRealms(false);
         }

         this.minecraft.keyboardHandler.setSendRepeatsToGui(true);
         this.showingPopup = false;
         this.addButtons();
         this.realmSelectionList = new RealmsMainScreen.RealmSelectionList();
         if (lastScrollYPosition != -1) {
            this.realmSelectionList.setScrollAmount((double)lastScrollYPosition);
         }

         this.addWidget(this.realmSelectionList);
         this.realmsSelectionListAdded = true;
         this.magicalSpecialHackyFocus(this.realmSelectionList);
         this.formattedPopup = MultiLineLabel.create(this.font, POPUP_TEXT, 100);
         RealmsNewsManager realmsnewsmanager = this.minecraft.realmsDataFetcher().newsManager;
         this.hasUnreadNews = realmsnewsmanager.hasUnreadNews();
         this.newsLink = realmsnewsmanager.newsLink();
         if (this.serverList == null) {
            this.serverList = new RealmsServerList(this.minecraft);
         }

         if (this.dataSubscription != null) {
            this.dataSubscription.forceUpdate();
         }

      }
   }

   private static boolean hasParentalConsent() {
      return checkedParentalConsent && hasParentalConsent;
   }

   public void addButtons() {
      this.leaveButton = this.addRenderableWidget(new Button(this.width / 2 - 202, this.height - 32, 90, 20, Component.translatable("mco.selectServer.leave"), (p_86679_) -> {
         this.leaveClicked(this.getSelectedServer());
      }));
      this.configureButton = this.addRenderableWidget(new Button(this.width / 2 - 190, this.height - 32, 90, 20, Component.translatable("mco.selectServer.configure"), (p_86672_) -> {
         this.configureClicked(this.getSelectedServer());
      }));
      this.playButton = this.addRenderableWidget(new Button(this.width / 2 - 93, this.height - 32, 90, 20, Component.translatable("mco.selectServer.play"), (p_86659_) -> {
         this.play(this.getSelectedServer(), this);
      }));
      this.backButton = this.addRenderableWidget(new Button(this.width / 2 + 4, this.height - 32, 90, 20, CommonComponents.GUI_BACK, (p_86647_) -> {
         if (!this.justClosedPopup) {
            this.minecraft.setScreen(this.lastScreen);
         }

      }));
      this.renewButton = this.addRenderableWidget(new Button(this.width / 2 + 100, this.height - 32, 90, 20, Component.translatable("mco.selectServer.expiredRenew"), (p_86622_) -> {
         this.onRenew(this.getSelectedServer());
      }));
      this.newsButton = this.addRenderableWidget(new RealmsMainScreen.NewsButton());
      this.showPopupButton = this.addRenderableWidget(new Button(this.width - 90, 6, 80, 20, Component.translatable("mco.selectServer.purchase"), (p_86597_) -> {
         this.popupOpenedByUser = !this.popupOpenedByUser;
      }));
      this.pendingInvitesButton = this.addRenderableWidget(new RealmsMainScreen.PendingInvitesButton());
      this.closeButton = this.addRenderableWidget(new RealmsMainScreen.CloseButton());
      this.createTrialButton = this.addRenderableWidget(new Button(this.width / 2 + 52, this.popupY0() + 137 - 20, 98, 20, Component.translatable("mco.selectServer.trial"), (p_86565_) -> {
         if (this.trialsAvailable && !this.createdTrial) {
            Util.getPlatform().openUri("https://aka.ms/startjavarealmstrial");
            this.minecraft.setScreen(this.lastScreen);
         }
      }));
      this.buyARealmButton = this.addRenderableWidget(new Button(this.width / 2 + 52, this.popupY0() + 160 - 20, 98, 20, Component.translatable("mco.selectServer.buy"), (p_231255_) -> {
         Util.getPlatform().openUri("https://aka.ms/BuyJavaRealms");
      }));
      this.updateButtonStates((RealmsServer)null);
   }

   void updateButtonStates(@Nullable RealmsServer pRealmsServer) {
      this.backButton.active = true;
      if (hasParentalConsent() && this.hasFetchedServers) {
         this.playButton.visible = true;
         this.playButton.active = this.shouldPlayButtonBeActive(pRealmsServer) && !this.shouldShowPopup();
         this.renewButton.visible = this.shouldRenewButtonBeActive(pRealmsServer);
         this.configureButton.visible = this.shouldConfigureButtonBeVisible(pRealmsServer);
         this.leaveButton.visible = this.shouldLeaveButtonBeVisible(pRealmsServer);
         boolean flag = this.shouldShowPopup() && this.trialsAvailable && !this.createdTrial;
         this.createTrialButton.visible = flag;
         this.createTrialButton.active = flag;
         this.buyARealmButton.visible = this.shouldShowPopup();
         this.closeButton.visible = this.shouldShowPopup() && this.popupOpenedByUser;
         this.renewButton.active = !this.shouldShowPopup();
         this.configureButton.active = !this.shouldShowPopup();
         this.leaveButton.active = !this.shouldShowPopup();
         this.newsButton.active = true;
         this.newsButton.visible = this.newsLink != null;
         this.pendingInvitesButton.active = true;
         this.pendingInvitesButton.visible = true;
         this.showPopupButton.active = !this.shouldShowPopup();
      } else {
         hideWidgets(new AbstractWidget[]{this.playButton, this.renewButton, this.configureButton, this.createTrialButton, this.buyARealmButton, this.closeButton, this.newsButton, this.pendingInvitesButton, this.showPopupButton, this.leaveButton});
      }
   }

   private boolean shouldShowPopupButton() {
      return (!this.shouldShowPopup() || this.popupOpenedByUser) && hasParentalConsent() && this.hasFetchedServers;
   }

   boolean shouldPlayButtonBeActive(@Nullable RealmsServer pRealmsServer) {
      return pRealmsServer != null && !pRealmsServer.expired && pRealmsServer.state == RealmsServer.State.OPEN;
   }

   private boolean shouldRenewButtonBeActive(@Nullable RealmsServer pRealmsServer) {
      return pRealmsServer != null && pRealmsServer.expired && this.isSelfOwnedServer(pRealmsServer);
   }

   private boolean shouldConfigureButtonBeVisible(@Nullable RealmsServer pRealmsServer) {
      return pRealmsServer != null && this.isSelfOwnedServer(pRealmsServer);
   }

   private boolean shouldLeaveButtonBeVisible(@Nullable RealmsServer pRealmsServer) {
      return pRealmsServer != null && !this.isSelfOwnedServer(pRealmsServer);
   }

   public void tick() {
      super.tick();
      if (this.pendingInvitesButton != null) {
         this.pendingInvitesButton.tick();
      }

      this.justClosedPopup = false;
      ++this.animTick;
      boolean flag = hasParentalConsent();
      if (this.dataSubscription == null && flag) {
         this.dataSubscription = this.initDataFetcher(this.minecraft.realmsDataFetcher());
      } else if (this.dataSubscription != null && !flag) {
         this.dataSubscription = null;
      }

      if (this.dataSubscription != null) {
         this.dataSubscription.tick();
      }

      if (this.shouldShowPopup()) {
         ++this.carouselTick;
      }

      if (this.showPopupButton != null) {
         this.showPopupButton.visible = this.shouldShowPopupButton();
         this.showPopupButton.active = this.showPopupButton.visible;
      }

   }

   private DataFetcher.Subscription initDataFetcher(RealmsDataFetcher p_238836_) {
      DataFetcher.Subscription datafetcher$subscription = p_238836_.dataFetcher.createSubscription();
      datafetcher$subscription.subscribe(p_238836_.serverListUpdateTask, (p_238837_) -> {
         List<RealmsServer> list = this.serverList.updateServersList(p_238837_);
         RealmsServer realmsserver = this.getSelectedServer();
         RealmsMainScreen.Entry realmsmainscreen$entry = null;
         this.realmSelectionList.clear();
         boolean flag = !this.hasFetchedServers;
         if (flag) {
            this.hasFetchedServers = true;
         }

         boolean flag1 = false;

         for(RealmsServer realmsserver1 : list) {
            if (this.isSelfOwnedNonExpiredServer(realmsserver1)) {
               flag1 = true;
            }
         }

         this.realmsServers = list;
         if (this.shouldShowMessageInList()) {
            this.realmSelectionList.addEntry(new RealmsMainScreen.TrialEntry());
         }

         for(RealmsServer realmsserver2 : this.realmsServers) {
            RealmsMainScreen.ServerEntry realmsmainscreen$serverentry = new RealmsMainScreen.ServerEntry(realmsserver2);
            this.realmSelectionList.addEntry(realmsmainscreen$serverentry);
            if (realmsserver != null && realmsserver.id == realmsserver2.id) {
               realmsmainscreen$entry = realmsmainscreen$serverentry;
            }
         }

         if (!regionsPinged && flag1) {
            regionsPinged = true;
            this.pingRegions();
         }

         if (flag) {
            this.updateButtonStates((RealmsServer)null);
         } else {
            this.realmSelectionList.setSelected(realmsmainscreen$entry);
         }

      });
      datafetcher$subscription.subscribe(p_238836_.pendingInvitesTask, (p_240510_) -> {
         this.numberOfPendingInvites = p_240510_;
         if (this.numberOfPendingInvites > 0 && this.inviteNarrationLimiter.tryAcquire(1)) {
            this.minecraft.getNarrator().sayNow(Component.translatable("mco.configure.world.invite.narration", this.numberOfPendingInvites));
         }

      });
      datafetcher$subscription.subscribe(p_238836_.trialAvailabilityTask, (p_238839_) -> {
         if (!this.createdTrial) {
            if (p_238839_ != this.trialsAvailable && this.shouldShowPopup()) {
               this.trialsAvailable = p_238839_;
               this.showingPopup = false;
            } else {
               this.trialsAvailable = p_238839_;
            }

         }
      });
      datafetcher$subscription.subscribe(p_238836_.liveStatsTask, (p_238847_) -> {
         for(RealmsServerPlayerList realmsserverplayerlist : p_238847_.servers) {
            for(RealmsServer realmsserver : this.realmsServers) {
               if (realmsserver.id == realmsserverplayerlist.serverId) {
                  realmsserver.updateServerPing(realmsserverplayerlist);
                  break;
               }
            }
         }

      });
      datafetcher$subscription.subscribe(p_238836_.newsTask, (p_231355_) -> {
         p_238836_.newsManager.updateUnreadNews(p_231355_);
         this.hasUnreadNews = p_238836_.newsManager.hasUnreadNews();
         this.newsLink = p_238836_.newsManager.newsLink();
         this.updateButtonStates((RealmsServer)null);
      });
      return datafetcher$subscription;
   }

   void refreshFetcher() {
      if (this.dataSubscription != null) {
         this.dataSubscription.reset();
      }

   }

   private void pingRegions() {
      (new Thread(() -> {
         List<RegionPingResult> list = Ping.pingAllRegions();
         RealmsClient realmsclient = RealmsClient.create();
         PingResult pingresult = new PingResult();
         pingresult.pingResults = list;
         pingresult.worldIds = this.getOwnedNonExpiredWorldIds();

         try {
            realmsclient.sendPingResults(pingresult);
         } catch (Throwable throwable) {
            LOGGER.warn("Could not send ping result to Realms: ", throwable);
         }

      })).start();
   }

   private List<Long> getOwnedNonExpiredWorldIds() {
      List<Long> list = Lists.newArrayList();

      for(RealmsServer realmsserver : this.realmsServers) {
         if (this.isSelfOwnedNonExpiredServer(realmsserver)) {
            list.add(realmsserver.id);
         }
      }

      return list;
   }

   public void removed() {
      this.minecraft.keyboardHandler.setSendRepeatsToGui(false);
   }

   public void setCreatedTrial(boolean pCreatedTrial) {
      this.createdTrial = pCreatedTrial;
   }

   void onRenew(@Nullable RealmsServer pRealmsServer) {
      if (pRealmsServer != null) {
         String s = "https://aka.ms/ExtendJavaRealms?subscriptionId=" + pRealmsServer.remoteSubscriptionId + "&profileId=" + this.minecraft.getUser().getUuid() + "&ref=" + (pRealmsServer.expiredTrial ? "expiredTrial" : "expiredRealm");
         this.minecraft.keyboardHandler.setClipboard(s);
         Util.getPlatform().openUri(s);
      }

   }

   private void checkClientCompatability() {
      if (!checkedClientCompatability) {
         checkedClientCompatability = true;
         (new Thread("MCO Compatability Checker #1") {
            public void run() {
               RealmsClient realmsclient = RealmsClient.create();

               try {
                  RealmsClient.CompatibleVersionResponse realmsclient$compatibleversionresponse = realmsclient.clientCompatible();
                  if (realmsclient$compatibleversionresponse != RealmsClient.CompatibleVersionResponse.COMPATIBLE) {
                     RealmsMainScreen.realmsGenericErrorScreen = new RealmsClientOutdatedScreen(RealmsMainScreen.this.lastScreen);
                     RealmsMainScreen.this.minecraft.execute(() -> {
                        RealmsMainScreen.this.minecraft.setScreen(RealmsMainScreen.realmsGenericErrorScreen);
                     });
                     return;
                  }

                  RealmsMainScreen.this.checkParentalConsent();
               } catch (RealmsServiceException realmsserviceexception) {
                  RealmsMainScreen.checkedClientCompatability = false;
                  RealmsMainScreen.LOGGER.error("Couldn't connect to realms", (Throwable)realmsserviceexception);
                  if (realmsserviceexception.httpResultCode == 401) {
                     RealmsMainScreen.realmsGenericErrorScreen = new RealmsGenericErrorScreen(Component.translatable("mco.error.invalid.session.title"), Component.translatable("mco.error.invalid.session.message"), RealmsMainScreen.this.lastScreen);
                     RealmsMainScreen.this.minecraft.execute(() -> {
                        RealmsMainScreen.this.minecraft.setScreen(RealmsMainScreen.realmsGenericErrorScreen);
                     });
                  } else {
                     RealmsMainScreen.this.minecraft.execute(() -> {
                        RealmsMainScreen.this.minecraft.setScreen(new RealmsGenericErrorScreen(realmsserviceexception, RealmsMainScreen.this.lastScreen));
                     });
                  }
               }

            }
         }).start();
      }

   }

   void checkParentalConsent() {
      (new Thread("MCO Compatability Checker #1") {
         public void run() {
            RealmsClient realmsclient = RealmsClient.create();

            try {
               Boolean obool = realmsclient.mcoEnabled();
               if (obool) {
                  RealmsMainScreen.LOGGER.info("Realms is available for this user");
                  RealmsMainScreen.hasParentalConsent = true;
               } else {
                  RealmsMainScreen.LOGGER.info("Realms is not available for this user");
                  RealmsMainScreen.hasParentalConsent = false;
                  RealmsMainScreen.this.minecraft.execute(() -> {
                     RealmsMainScreen.this.minecraft.setScreen(new RealmsParentalConsentScreen(RealmsMainScreen.this.lastScreen));
                  });
               }

               RealmsMainScreen.checkedParentalConsent = true;
            } catch (RealmsServiceException realmsserviceexception) {
               RealmsMainScreen.LOGGER.error("Couldn't connect to realms", (Throwable)realmsserviceexception);
               RealmsMainScreen.this.minecraft.execute(() -> {
                  RealmsMainScreen.this.minecraft.setScreen(new RealmsGenericErrorScreen(realmsserviceexception, RealmsMainScreen.this.lastScreen));
               });
            }

         }
      }).start();
   }

   private void switchToStage() {
      if (RealmsClient.currentEnvironment != RealmsClient.Environment.STAGE) {
         (new Thread("MCO Stage Availability Checker #1") {
            public void run() {
               RealmsClient realmsclient = RealmsClient.create();

               try {
                  Boolean obool = realmsclient.stageAvailable();
                  if (obool) {
                     RealmsClient.switchToStage();
                     RealmsMainScreen.LOGGER.info("Switched to stage");
                     RealmsMainScreen.this.refreshFetcher();
                  }
               } catch (RealmsServiceException realmsserviceexception) {
                  RealmsMainScreen.LOGGER.error("Couldn't connect to Realms: {}", (Object)realmsserviceexception.toString());
               }

            }
         }).start();
      }

   }

   private void switchToLocal() {
      if (RealmsClient.currentEnvironment != RealmsClient.Environment.LOCAL) {
         (new Thread("MCO Local Availability Checker #1") {
            public void run() {
               RealmsClient realmsclient = RealmsClient.create();

               try {
                  Boolean obool = realmsclient.stageAvailable();
                  if (obool) {
                     RealmsClient.switchToLocal();
                     RealmsMainScreen.LOGGER.info("Switched to local");
                     RealmsMainScreen.this.refreshFetcher();
                  }
               } catch (RealmsServiceException realmsserviceexception) {
                  RealmsMainScreen.LOGGER.error("Couldn't connect to Realms: {}", (Object)realmsserviceexception.toString());
               }

            }
         }).start();
      }

   }

   private void switchToProd() {
      RealmsClient.switchToProd();
      this.refreshFetcher();
   }

   void configureClicked(@Nullable RealmsServer pRealmsServer) {
      if (pRealmsServer != null && (this.minecraft.getUser().getUuid().equals(pRealmsServer.ownerUUID) || overrideConfigure)) {
         this.saveListScrollPosition();
         this.minecraft.setScreen(new RealmsConfigureWorldScreen(this, pRealmsServer.id));
      }

   }

   void leaveClicked(@Nullable RealmsServer pRealmsServer) {
      if (pRealmsServer != null && !this.minecraft.getUser().getUuid().equals(pRealmsServer.ownerUUID)) {
         this.saveListScrollPosition();
         Component component = Component.translatable("mco.configure.world.leave.question.line1");
         Component component1 = Component.translatable("mco.configure.world.leave.question.line2");
         this.minecraft.setScreen(new RealmsLongConfirmationScreen((p_231253_) -> {
            this.leaveServer(p_231253_, pRealmsServer);
         }, RealmsLongConfirmationScreen.Type.Info, component, component1, true));
      }

   }

   private void saveListScrollPosition() {
      lastScrollYPosition = (int)this.realmSelectionList.getScrollAmount();
   }

   @Nullable
   private RealmsServer getSelectedServer() {
      if (this.realmSelectionList == null) {
         return null;
      } else {
         RealmsMainScreen.Entry realmsmainscreen$entry = this.realmSelectionList.getSelected();
         return realmsmainscreen$entry != null ? realmsmainscreen$entry.getServer() : null;
      }
   }

   private void leaveServer(boolean p_193494_, final RealmsServer p_193495_) {
      if (p_193494_) {
         (new Thread("Realms-leave-server") {
            public void run() {
               try {
                  RealmsClient realmsclient = RealmsClient.create();
                  realmsclient.uninviteMyselfFrom(p_193495_.id);
                  RealmsMainScreen.this.minecraft.execute(() -> {
                     RealmsMainScreen.this.removeServer(p_193495_);
                  });
               } catch (RealmsServiceException realmsserviceexception) {
                  RealmsMainScreen.LOGGER.error("Couldn't configure world");
                  RealmsMainScreen.this.minecraft.execute(() -> {
                     RealmsMainScreen.this.minecraft.setScreen(new RealmsGenericErrorScreen(realmsserviceexception, RealmsMainScreen.this));
                  });
               }

            }
         }).start();
      }

      this.minecraft.setScreen(this);
   }

   void removeServer(RealmsServer pRealmsServer) {
      this.realmsServers = this.serverList.removeItem(pRealmsServer);
      this.realmSelectionList.children().removeIf((p_231250_) -> {
         RealmsServer realmsserver = p_231250_.getServer();
         return realmsserver != null && realmsserver.id == pRealmsServer.id;
      });
      this.realmSelectionList.setSelected((RealmsMainScreen.Entry)null);
      this.updateButtonStates((RealmsServer)null);
      this.playButton.active = false;
   }

   public void resetScreen() {
      if (this.realmSelectionList != null) {
         this.realmSelectionList.setSelected((RealmsMainScreen.Entry)null);
      }

   }

   public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
      if (pKeyCode == 256) {
         this.keyCombos.forEach(KeyCombo::reset);
         this.onClosePopup();
         return true;
      } else {
         return super.keyPressed(pKeyCode, pScanCode, pModifiers);
      }
   }

   void onClosePopup() {
      if (this.shouldShowPopup() && this.popupOpenedByUser) {
         this.popupOpenedByUser = false;
      } else {
         this.minecraft.setScreen(this.lastScreen);
      }

   }

   public boolean charTyped(char pCodePoint, int pModifiers) {
      this.keyCombos.forEach((p_231245_) -> {
         p_231245_.keyPressed(pCodePoint);
      });
      return true;
   }

   public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
      this.hoveredElement = RealmsMainScreen.HoveredElement.NONE;
      this.toolTip = null;
      this.renderBackground(pPoseStack);
      this.realmSelectionList.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
      this.drawRealmsLogo(pPoseStack, this.width / 2 - 50, 7);
      if (RealmsClient.currentEnvironment == RealmsClient.Environment.STAGE) {
         this.renderStage(pPoseStack);
      }

      if (RealmsClient.currentEnvironment == RealmsClient.Environment.LOCAL) {
         this.renderLocal(pPoseStack);
      }

      if (this.shouldShowPopup()) {
         this.drawPopup(pPoseStack);
      } else {
         if (this.showingPopup) {
            this.updateButtonStates((RealmsServer)null);
            if (!this.realmsSelectionListAdded) {
               this.addWidget(this.realmSelectionList);
               this.realmsSelectionListAdded = true;
            }

            this.playButton.active = this.shouldPlayButtonBeActive(this.getSelectedServer());
         }

         this.showingPopup = false;
      }

      super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
      if (this.toolTip != null) {
         this.renderMousehoverTooltip(pPoseStack, this.toolTip, pMouseX, pMouseY);
      }

      if (this.trialsAvailable && !this.createdTrial && this.shouldShowPopup()) {
         RenderSystem.setShaderTexture(0, TRIAL_ICON_LOCATION);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         int i = 8;
         int j = 8;
         int k = 0;
         if ((Util.getMillis() / 800L & 1L) == 1L) {
            k = 8;
         }

         GuiComponent.blit(pPoseStack, this.createTrialButton.x + this.createTrialButton.getWidth() - 8 - 4, this.createTrialButton.y + this.createTrialButton.getHeight() / 2 - 4, 0.0F, (float)k, 8, 8, 8, 16);
      }

   }

   private void drawRealmsLogo(PoseStack pPoseStack, int pX, int pY) {
      RenderSystem.setShader(GameRenderer::getPositionTexShader);
      RenderSystem.setShaderTexture(0, LOGO_LOCATION);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      pPoseStack.pushPose();
      pPoseStack.scale(0.5F, 0.5F, 0.5F);
      GuiComponent.blit(pPoseStack, pX * 2, pY * 2 - 5, 0.0F, 0.0F, 200, 50, 200, 50);
      pPoseStack.popPose();
   }

   public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
      if (this.isOutsidePopup(pMouseX, pMouseY) && this.popupOpenedByUser) {
         this.popupOpenedByUser = false;
         this.justClosedPopup = true;
         return true;
      } else {
         return super.mouseClicked(pMouseX, pMouseY, pButton);
      }
   }

   private boolean isOutsidePopup(double p_86394_, double p_86395_) {
      int i = this.popupX0();
      int j = this.popupY0();
      return p_86394_ < (double)(i - 5) || p_86394_ > (double)(i + 315) || p_86395_ < (double)(j - 5) || p_86395_ > (double)(j + 171);
   }

   private void drawPopup(PoseStack pPoseStack) {
      int i = this.popupX0();
      int j = this.popupY0();
      if (!this.showingPopup) {
         this.carouselIndex = 0;
         this.carouselTick = 0;
         this.hasSwitchedCarouselImage = true;
         this.updateButtonStates((RealmsServer)null);
         if (this.realmsSelectionListAdded) {
            this.removeWidget(this.realmSelectionList);
            this.realmsSelectionListAdded = false;
         }

         this.minecraft.getNarrator().sayNow(POPUP_TEXT);
      }

      if (this.hasFetchedServers) {
         this.showingPopup = true;
      }

      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.7F);
      RenderSystem.enableBlend();
      RenderSystem.setShaderTexture(0, DARKEN_LOCATION);
      int k = 0;
      int l = 32;
      GuiComponent.blit(pPoseStack, 0, 32, 0.0F, 0.0F, this.width, this.height - 40 - 32, 310, 166);
      RenderSystem.disableBlend();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.setShaderTexture(0, POPUP_LOCATION);
      GuiComponent.blit(pPoseStack, i, j, 0.0F, 0.0F, 310, 166, 310, 166);
      if (!teaserImages.isEmpty()) {
         RenderSystem.setShaderTexture(0, teaserImages.get(this.carouselIndex));
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         GuiComponent.blit(pPoseStack, i + 7, j + 7, 0.0F, 0.0F, 195, 152, 195, 152);
         if (this.carouselTick % 95 < 5) {
            if (!this.hasSwitchedCarouselImage) {
               this.carouselIndex = (this.carouselIndex + 1) % teaserImages.size();
               this.hasSwitchedCarouselImage = true;
            }
         } else {
            this.hasSwitchedCarouselImage = false;
         }
      }

      this.formattedPopup.renderLeftAlignedNoShadow(pPoseStack, this.width / 2 + 52, j + 7, 10, 5000268);
   }

   int popupX0() {
      return (this.width - 310) / 2;
   }

   int popupY0() {
      return this.height / 2 - 80;
   }

   void drawInvitationPendingIcon(PoseStack pPoseStack, int p_86426_, int p_86427_, int p_86428_, int p_86429_, boolean p_86430_, boolean p_86431_) {
      int i = this.numberOfPendingInvites;
      boolean flag = this.inPendingInvitationArea((double)p_86426_, (double)p_86427_);
      boolean flag1 = p_86431_ && p_86430_;
      if (flag1) {
         float f = 0.25F + (1.0F + Mth.sin((float)this.animTick * 0.5F)) * 0.25F;
         int j = -16777216 | (int)(f * 64.0F) << 16 | (int)(f * 64.0F) << 8 | (int)(f * 64.0F) << 0;
         this.fillGradient(pPoseStack, p_86428_ - 2, p_86429_ - 2, p_86428_ + 18, p_86429_ + 18, j, j);
         j = -16777216 | (int)(f * 255.0F) << 16 | (int)(f * 255.0F) << 8 | (int)(f * 255.0F) << 0;
         this.fillGradient(pPoseStack, p_86428_ - 2, p_86429_ - 2, p_86428_ + 18, p_86429_ - 1, j, j);
         this.fillGradient(pPoseStack, p_86428_ - 2, p_86429_ - 2, p_86428_ - 1, p_86429_ + 18, j, j);
         this.fillGradient(pPoseStack, p_86428_ + 17, p_86429_ - 2, p_86428_ + 18, p_86429_ + 18, j, j);
         this.fillGradient(pPoseStack, p_86428_ - 2, p_86429_ + 17, p_86428_ + 18, p_86429_ + 18, j, j);
      }

      RenderSystem.setShaderTexture(0, INVITE_ICON_LOCATION);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      boolean flag3 = p_86431_ && p_86430_;
      float f2 = flag3 ? 16.0F : 0.0F;
      GuiComponent.blit(pPoseStack, p_86428_, p_86429_ - 6, f2, 0.0F, 15, 25, 31, 25);
      boolean flag2 = p_86431_ && i != 0;
      if (flag2) {
         int k = (Math.min(i, 6) - 1) * 8;
         int l = (int)(Math.max(0.0F, Math.max(Mth.sin((float)(10 + this.animTick) * 0.57F), Mth.cos((float)this.animTick * 0.35F))) * -6.0F);
         RenderSystem.setShaderTexture(0, INVITATION_ICONS_LOCATION);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         float f1 = flag ? 8.0F : 0.0F;
         GuiComponent.blit(pPoseStack, p_86428_ + 4, p_86429_ + 4 + l, (float)k, f1, 8, 8, 48, 16);
      }

      int j1 = p_86426_ + 12;
      boolean flag4 = p_86431_ && flag;
      if (flag4) {
         Component component = i == 0 ? NO_PENDING_INVITES_TEXT : PENDING_INVITES_TEXT;
         int i1 = this.font.width(component);
         this.fillGradient(pPoseStack, j1 - 3, p_86427_ - 3, j1 + i1 + 3, p_86427_ + 8 + 3, -1073741824, -1073741824);
         this.font.drawShadow(pPoseStack, component, (float)j1, (float)p_86427_, -1);
      }

   }

   private boolean inPendingInvitationArea(double p_86572_, double p_86573_) {
      int i = this.width / 2 + 50;
      int j = this.width / 2 + 66;
      int k = 11;
      int l = 23;
      if (this.numberOfPendingInvites != 0) {
         i -= 3;
         j += 3;
         k -= 5;
         l += 5;
      }

      return (double)i <= p_86572_ && p_86572_ <= (double)j && (double)k <= p_86573_ && p_86573_ <= (double)l;
   }

   public void play(@Nullable RealmsServer pRealmsServer, Screen pLastScreen) {
      if (pRealmsServer != null) {
         try {
            if (!this.connectLock.tryLock(1L, TimeUnit.SECONDS)) {
               return;
            }

            if (this.connectLock.getHoldCount() > 1) {
               return;
            }
         } catch (InterruptedException interruptedexception) {
            return;
         }

         this.dontSetConnectedToRealms = true;
         this.minecraft.setScreen(new RealmsLongRunningMcoTaskScreen(pLastScreen, new GetServerDetailsTask(this, pLastScreen, pRealmsServer, this.connectLock)));
      }

   }

   boolean isSelfOwnedServer(RealmsServer pServer) {
      return pServer.ownerUUID != null && pServer.ownerUUID.equals(this.minecraft.getUser().getUuid());
   }

   private boolean isSelfOwnedNonExpiredServer(RealmsServer pServer) {
      return this.isSelfOwnedServer(pServer) && !pServer.expired;
   }

   void drawExpired(PoseStack pPoseStack, int p_86578_, int p_86579_, int p_86580_, int p_86581_) {
      RenderSystem.setShaderTexture(0, EXPIRED_ICON_LOCATION);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      GuiComponent.blit(pPoseStack, p_86578_, p_86579_, 0.0F, 0.0F, 10, 28, 10, 28);
      if (p_86580_ >= p_86578_ && p_86580_ <= p_86578_ + 9 && p_86581_ >= p_86579_ && p_86581_ <= p_86579_ + 27 && p_86581_ < this.height - 40 && p_86581_ > 32 && !this.shouldShowPopup()) {
         this.setTooltip(SERVER_EXPIRED_TOOLTIP);
      }

   }

   void drawExpiring(PoseStack pPoseStack, int p_86539_, int p_86540_, int p_86541_, int p_86542_, int p_86543_) {
      RenderSystem.setShaderTexture(0, EXPIRES_SOON_ICON_LOCATION);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      if (this.animTick % 20 < 10) {
         GuiComponent.blit(pPoseStack, p_86539_, p_86540_, 0.0F, 0.0F, 10, 28, 20, 28);
      } else {
         GuiComponent.blit(pPoseStack, p_86539_, p_86540_, 10.0F, 0.0F, 10, 28, 20, 28);
      }

      if (p_86541_ >= p_86539_ && p_86541_ <= p_86539_ + 9 && p_86542_ >= p_86540_ && p_86542_ <= p_86540_ + 27 && p_86542_ < this.height - 40 && p_86542_ > 32 && !this.shouldShowPopup()) {
         if (p_86543_ <= 0) {
            this.setTooltip(SERVER_EXPIRES_SOON_TOOLTIP);
         } else if (p_86543_ == 1) {
            this.setTooltip(SERVER_EXPIRES_IN_DAY_TOOLTIP);
         } else {
            this.setTooltip(Component.translatable("mco.selectServer.expires.days", p_86543_));
         }
      }

   }

   void drawOpen(PoseStack pPoseStack, int p_86603_, int p_86604_, int p_86605_, int p_86606_) {
      RenderSystem.setShaderTexture(0, ON_ICON_LOCATION);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      GuiComponent.blit(pPoseStack, p_86603_, p_86604_, 0.0F, 0.0F, 10, 28, 10, 28);
      if (p_86605_ >= p_86603_ && p_86605_ <= p_86603_ + 9 && p_86606_ >= p_86604_ && p_86606_ <= p_86604_ + 27 && p_86606_ < this.height - 40 && p_86606_ > 32 && !this.shouldShowPopup()) {
         this.setTooltip(SERVER_OPEN_TOOLTIP);
      }

   }

   void drawClose(PoseStack pPoseStack, int p_86628_, int p_86629_, int p_86630_, int p_86631_) {
      RenderSystem.setShaderTexture(0, OFF_ICON_LOCATION);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      GuiComponent.blit(pPoseStack, p_86628_, p_86629_, 0.0F, 0.0F, 10, 28, 10, 28);
      if (p_86630_ >= p_86628_ && p_86630_ <= p_86628_ + 9 && p_86631_ >= p_86629_ && p_86631_ <= p_86629_ + 27 && p_86631_ < this.height - 40 && p_86631_ > 32 && !this.shouldShowPopup()) {
         this.setTooltip(SERVER_CLOSED_TOOLTIP);
      }

   }

   void drawLeave(PoseStack pPoseStack, int p_86650_, int p_86651_, int p_86652_, int p_86653_) {
      boolean flag = false;
      if (p_86652_ >= p_86650_ && p_86652_ <= p_86650_ + 28 && p_86653_ >= p_86651_ && p_86653_ <= p_86651_ + 28 && p_86653_ < this.height - 40 && p_86653_ > 32 && !this.shouldShowPopup()) {
         flag = true;
      }

      RenderSystem.setShaderTexture(0, LEAVE_ICON_LOCATION);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      float f = flag ? 28.0F : 0.0F;
      GuiComponent.blit(pPoseStack, p_86650_, p_86651_, f, 0.0F, 28, 28, 56, 28);
      if (flag) {
         this.setTooltip(LEAVE_SERVER_TOOLTIP);
         this.hoveredElement = RealmsMainScreen.HoveredElement.LEAVE;
      }

   }

   void drawConfigure(PoseStack pPoseStack, int p_86663_, int p_86664_, int p_86665_, int p_86666_) {
      boolean flag = false;
      if (p_86665_ >= p_86663_ && p_86665_ <= p_86663_ + 28 && p_86666_ >= p_86664_ && p_86666_ <= p_86664_ + 28 && p_86666_ < this.height - 40 && p_86666_ > 32 && !this.shouldShowPopup()) {
         flag = true;
      }

      RenderSystem.setShaderTexture(0, CONFIGURE_LOCATION);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      float f = flag ? 28.0F : 0.0F;
      GuiComponent.blit(pPoseStack, p_86663_, p_86664_, f, 0.0F, 28, 28, 56, 28);
      if (flag) {
         this.setTooltip(CONFIGURE_SERVER_TOOLTIP);
         this.hoveredElement = RealmsMainScreen.HoveredElement.CONFIGURE;
      }

   }

   protected void renderMousehoverTooltip(PoseStack pPoseStack, List<Component> pTooltip, int pMouseX, int pMouseY) {
      if (!pTooltip.isEmpty()) {
         int i = 0;
         int j = 0;

         for(Component component : pTooltip) {
            int k = this.font.width(component);
            if (k > j) {
               j = k;
            }
         }

         int i1 = pMouseX - j - 5;
         int j1 = pMouseY;
         if (i1 < 0) {
            i1 = pMouseX + 12;
         }

         for(Component component1 : pTooltip) {
            int l = j1 - (i == 0 ? 3 : 0) + i;
            this.fillGradient(pPoseStack, i1 - 3, l, i1 + j + 3, j1 + 8 + 3 + i, -1073741824, -1073741824);
            this.font.drawShadow(pPoseStack, component1, (float)i1, (float)(j1 + i), 16777215);
            i += 10;
         }

      }
   }

   void renderNews(PoseStack pPoseStack, int p_86434_, int p_86435_, boolean p_86436_, int p_86437_, int p_86438_, boolean p_86439_, boolean p_86440_) {
      boolean flag = false;
      if (p_86434_ >= p_86437_ && p_86434_ <= p_86437_ + 20 && p_86435_ >= p_86438_ && p_86435_ <= p_86438_ + 20) {
         flag = true;
      }

      RenderSystem.setShaderTexture(0, NEWS_LOCATION);
      if (p_86440_) {
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      } else {
         RenderSystem.setShaderColor(0.5F, 0.5F, 0.5F, 1.0F);
      }

      boolean flag1 = p_86440_ && p_86439_;
      float f = flag1 ? 20.0F : 0.0F;
      GuiComponent.blit(pPoseStack, p_86437_, p_86438_, f, 0.0F, 20, 20, 40, 20);
      if (flag && p_86440_) {
         this.setTooltip(NEWS_TOOLTIP);
      }

      if (p_86436_ && p_86440_) {
         int i = flag ? 0 : (int)(Math.max(0.0F, Math.max(Mth.sin((float)(10 + this.animTick) * 0.57F), Mth.cos((float)this.animTick * 0.35F))) * -6.0F);
         RenderSystem.setShaderTexture(0, INVITATION_ICONS_LOCATION);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         GuiComponent.blit(pPoseStack, p_86437_ + 10, p_86438_ + 2 + i, 40.0F, 0.0F, 8, 8, 48, 16);
      }

   }

   private void renderLocal(PoseStack pPoseStack) {
      String s = "LOCAL!";
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      pPoseStack.pushPose();
      pPoseStack.translate((double)(this.width / 2 - 25), 20.0D, 0.0D);
      pPoseStack.mulPose(Vector3f.ZP.rotationDegrees(-20.0F));
      pPoseStack.scale(1.5F, 1.5F, 1.5F);
      this.font.draw(pPoseStack, "LOCAL!", 0.0F, 0.0F, 8388479);
      pPoseStack.popPose();
   }

   private void renderStage(PoseStack pPoseStack) {
      String s = "STAGE!";
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      pPoseStack.pushPose();
      pPoseStack.translate((double)(this.width / 2 - 25), 20.0D, 0.0D);
      pPoseStack.mulPose(Vector3f.ZP.rotationDegrees(-20.0F));
      pPoseStack.scale(1.5F, 1.5F, 1.5F);
      this.font.draw(pPoseStack, "STAGE!", 0.0F, 0.0F, -256);
      pPoseStack.popPose();
   }

   public RealmsMainScreen newScreen() {
      RealmsMainScreen realmsmainscreen = new RealmsMainScreen(this.lastScreen);
      realmsmainscreen.init(this.minecraft, this.width, this.height);
      return realmsmainscreen;
   }

   public static void updateTeaserImages(ResourceManager p_86407_) {
      Collection<ResourceLocation> collection = p_86407_.listResources("textures/gui/images", (p_193492_) -> {
         return p_193492_.getPath().endsWith(".png");
      }).keySet();
      teaserImages = collection.stream().filter((p_231247_) -> {
         return p_231247_.getNamespace().equals("realms");
      }).toList();
   }

   void setTooltip(Component... pTooltip) {
      this.toolTip = Arrays.asList(pTooltip);
   }

   private void pendingButtonPress(Button pButton) {
      this.minecraft.setScreen(new RealmsPendingInvitesScreen(this.lastScreen));
   }

   @OnlyIn(Dist.CLIENT)
   class CloseButton extends Button {
      public CloseButton() {
         super(RealmsMainScreen.this.popupX0() + 4, RealmsMainScreen.this.popupY0() + 4, 12, 12, Component.translatable("mco.selectServer.close"), null);
      }

      @Override
      public void onPress() {
            RealmsMainScreen.this.onClosePopup();
      }

      public void renderButton(PoseStack p_86777_, int p_86778_, int p_86779_, float p_86780_) {
         RenderSystem.setShaderTexture(0, RealmsMainScreen.CROSS_ICON_LOCATION);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         float f = this.isHoveredOrFocused() ? 12.0F : 0.0F;
         blit(p_86777_, this.x, this.y, 0.0F, f, 12, 12, 12, 24);
         if (this.isMouseOver((double)p_86778_, (double)p_86779_)) {
            RealmsMainScreen.this.setTooltip(this.getMessage());
         }

      }
   }

   @OnlyIn(Dist.CLIENT)
   abstract class Entry extends ObjectSelectionList.Entry<RealmsMainScreen.Entry> {
      @Nullable
      public abstract RealmsServer getServer();
   }

   @OnlyIn(Dist.CLIENT)
   static enum HoveredElement {
      NONE,
      EXPIRED,
      LEAVE,
      CONFIGURE;
   }

   @OnlyIn(Dist.CLIENT)
   class NewsButton extends Button {
      public NewsButton() {
         super(RealmsMainScreen.this.width - 115, 6, 20, 20, Component.translatable("mco.news"), null);
      }

      @Override
      public void onPress() {
            if (RealmsMainScreen.this.newsLink != null) {
               Util.getPlatform().openUri(RealmsMainScreen.this.newsLink);
               if (RealmsMainScreen.this.hasUnreadNews) {
                  RealmsPersistence.RealmsPersistenceData realmspersistence$realmspersistencedata = RealmsPersistence.readFile();
                  realmspersistence$realmspersistencedata.hasUnreadNews = false;
                  RealmsMainScreen.this.hasUnreadNews = false;
                  RealmsPersistence.writeFile(realmspersistence$realmspersistencedata);
               }

            }
      }

      public void renderButton(PoseStack p_86806_, int p_86807_, int p_86808_, float p_86809_) {
         RealmsMainScreen.this.renderNews(p_86806_, p_86807_, p_86808_, RealmsMainScreen.this.hasUnreadNews, this.x, this.y, this.isHoveredOrFocused(), this.active);
      }
   }

   @OnlyIn(Dist.CLIENT)
   class PendingInvitesButton extends Button {
      public PendingInvitesButton() {
         super(RealmsMainScreen.this.width / 2 + 47, 6, 22, 22, CommonComponents.EMPTY, RealmsMainScreen.this::pendingButtonPress);
      }

      public void tick() {
         this.setMessage(RealmsMainScreen.this.numberOfPendingInvites == 0 ? RealmsMainScreen.NO_PENDING_INVITES_TEXT : RealmsMainScreen.PENDING_INVITES_TEXT);
      }

      public void renderButton(PoseStack p_86817_, int p_86818_, int p_86819_, float p_86820_) {
         RealmsMainScreen.this.drawInvitationPendingIcon(p_86817_, p_86818_, p_86819_, this.x, this.y, this.isHoveredOrFocused(), this.active);
      }
   }

   @OnlyIn(Dist.CLIENT)
   class RealmSelectionList extends RealmsObjectSelectionList<RealmsMainScreen.Entry> {
      public RealmSelectionList() {
         super(RealmsMainScreen.this.width, RealmsMainScreen.this.height, 32, RealmsMainScreen.this.height - 40, 36);
      }

      public boolean isFocused() {
         return RealmsMainScreen.this.getFocused() == this;
      }

      public boolean keyPressed(int p_86840_, int p_86841_, int p_86842_) {
         if (p_86840_ != 257 && p_86840_ != 32 && p_86840_ != 335) {
            return super.keyPressed(p_86840_, p_86841_, p_86842_);
         } else {
            RealmsMainScreen.Entry realmsmainscreen$entry = this.getSelected();
            return realmsmainscreen$entry == null ? super.keyPressed(p_86840_, p_86841_, p_86842_) : realmsmainscreen$entry.mouseClicked(0.0D, 0.0D, 0);
         }
      }

      public boolean mouseClicked(double p_86828_, double p_86829_, int p_86830_) {
         if (p_86830_ == 0 && p_86828_ < (double)this.getScrollbarPosition() && p_86829_ >= (double)this.y0 && p_86829_ <= (double)this.y1) {
            int i = RealmsMainScreen.this.realmSelectionList.getRowLeft();
            int j = this.getScrollbarPosition();
            int k = (int)Math.floor(p_86829_ - (double)this.y0) - this.headerHeight + (int)this.getScrollAmount() - 4;
            int l = k / this.itemHeight;
            if (p_86828_ >= (double)i && p_86828_ <= (double)j && l >= 0 && k >= 0 && l < this.getItemCount()) {
               this.itemClicked(k, l, p_86828_, p_86829_, this.width);
               this.selectItem(l);
            }

            return true;
         } else {
            return super.mouseClicked(p_86828_, p_86829_, p_86830_);
         }
      }

      public void setSelected(@Nullable RealmsMainScreen.Entry p_86849_) {
         super.setSelected(p_86849_);
         if (p_86849_ != null) {
            RealmsMainScreen.this.updateButtonStates(p_86849_.getServer());
         } else {
            RealmsMainScreen.this.updateButtonStates((RealmsServer)null);
         }

      }

      public void itemClicked(int p_86834_, int p_86835_, double p_86836_, double p_86837_, int p_86838_) {
         RealmsMainScreen.Entry realmsmainscreen$entry = this.getEntry(p_86835_);
         if (realmsmainscreen$entry instanceof RealmsMainScreen.TrialEntry) {
            RealmsMainScreen.this.popupOpenedByUser = true;
         } else {
            RealmsServer realmsserver = realmsmainscreen$entry.getServer();
            if (realmsserver != null) {
               if (realmsserver.state == RealmsServer.State.UNINITIALIZED) {
                  Minecraft.getInstance().setScreen(new RealmsCreateRealmScreen(realmsserver, RealmsMainScreen.this));
               } else {
                  if (RealmsMainScreen.this.hoveredElement == RealmsMainScreen.HoveredElement.CONFIGURE) {
                     RealmsMainScreen.this.configureClicked(realmsserver);
                  } else if (RealmsMainScreen.this.hoveredElement == RealmsMainScreen.HoveredElement.LEAVE) {
                     RealmsMainScreen.this.leaveClicked(realmsserver);
                  } else if (RealmsMainScreen.this.hoveredElement == RealmsMainScreen.HoveredElement.EXPIRED) {
                     RealmsMainScreen.this.onRenew(realmsserver);
                  } else if (RealmsMainScreen.this.shouldPlayButtonBeActive(realmsserver)) {
                     if (Util.getMillis() - RealmsMainScreen.this.lastClickTime < 250L && this.isSelectedItem(p_86835_)) {
                        RealmsMainScreen.this.play(realmsserver, RealmsMainScreen.this);
                     }

                     RealmsMainScreen.this.lastClickTime = Util.getMillis();
                  }

               }
            }
         }
      }

      public int getMaxPosition() {
         return this.getItemCount() * 36;
      }

      public int getRowWidth() {
         return 300;
      }
   }

   @OnlyIn(Dist.CLIENT)
   class ServerEntry extends RealmsMainScreen.Entry {
      private static final int SKIN_HEAD_LARGE_WIDTH = 36;
      private final RealmsServer serverData;

      public ServerEntry(RealmsServer pServerData) {
         this.serverData = pServerData;
      }

      public void render(PoseStack pPoseStack, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pIsMouseOver, float pPartialTick) {
         this.renderMcoServerItem(this.serverData, pPoseStack, pLeft, pTop, pMouseX, pMouseY);
      }

      public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
         if (this.serverData.state == RealmsServer.State.UNINITIALIZED) {
            RealmsMainScreen.this.minecraft.setScreen(new RealmsCreateRealmScreen(this.serverData, RealmsMainScreen.this));
         }

         return true;
      }

      private void renderMcoServerItem(RealmsServer pRealmsServer, PoseStack pPoseStack, int p_86881_, int p_86882_, int p_86883_, int p_86884_) {
         this.renderLegacy(pRealmsServer, pPoseStack, p_86881_ + 36, p_86882_, p_86883_, p_86884_);
      }

      private void renderLegacy(RealmsServer pRealmsServer, PoseStack pPoseStack, int p_86888_, int p_86889_, int p_86890_, int p_86891_) {
         if (pRealmsServer.state == RealmsServer.State.UNINITIALIZED) {
            RenderSystem.setShaderTexture(0, RealmsMainScreen.WORLDICON_LOCATION);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            GuiComponent.blit(pPoseStack, p_86888_ + 10, p_86889_ + 6, 0.0F, 0.0F, 40, 20, 40, 20);
            float f = 0.5F + (1.0F + Mth.sin((float)RealmsMainScreen.this.animTick * 0.25F)) * 0.25F;
            int k2 = -16777216 | (int)(127.0F * f) << 16 | (int)(255.0F * f) << 8 | (int)(127.0F * f);
            GuiComponent.drawCenteredString(pPoseStack, RealmsMainScreen.this.font, RealmsMainScreen.SERVER_UNITIALIZED_TEXT, p_86888_ + 10 + 40 + 75, p_86889_ + 12, k2);
         } else {
            int i = 225;
            int j = 2;
            if (pRealmsServer.expired) {
               RealmsMainScreen.this.drawExpired(pPoseStack, p_86888_ + 225 - 14, p_86889_ + 2, p_86890_, p_86891_);
            } else if (pRealmsServer.state == RealmsServer.State.CLOSED) {
               RealmsMainScreen.this.drawClose(pPoseStack, p_86888_ + 225 - 14, p_86889_ + 2, p_86890_, p_86891_);
            } else if (RealmsMainScreen.this.isSelfOwnedServer(pRealmsServer) && pRealmsServer.daysLeft < 7) {
               RealmsMainScreen.this.drawExpiring(pPoseStack, p_86888_ + 225 - 14, p_86889_ + 2, p_86890_, p_86891_, pRealmsServer.daysLeft);
            } else if (pRealmsServer.state == RealmsServer.State.OPEN) {
               RealmsMainScreen.this.drawOpen(pPoseStack, p_86888_ + 225 - 14, p_86889_ + 2, p_86890_, p_86891_);
            }

            if (!RealmsMainScreen.this.isSelfOwnedServer(pRealmsServer) && !RealmsMainScreen.overrideConfigure) {
               RealmsMainScreen.this.drawLeave(pPoseStack, p_86888_ + 225, p_86889_ + 2, p_86890_, p_86891_);
            } else {
               RealmsMainScreen.this.drawConfigure(pPoseStack, p_86888_ + 225, p_86889_ + 2, p_86890_, p_86891_);
            }

            if (!"0".equals(pRealmsServer.serverPing.nrOfPlayers)) {
               String s = ChatFormatting.GRAY + pRealmsServer.serverPing.nrOfPlayers;
               RealmsMainScreen.this.font.draw(pPoseStack, s, (float)(p_86888_ + 207 - RealmsMainScreen.this.font.width(s)), (float)(p_86889_ + 3), 8421504);
               if (p_86890_ >= p_86888_ + 207 - RealmsMainScreen.this.font.width(s) && p_86890_ <= p_86888_ + 207 && p_86891_ >= p_86889_ + 1 && p_86891_ <= p_86889_ + 10 && p_86891_ < RealmsMainScreen.this.height - 40 && p_86891_ > 32 && !RealmsMainScreen.this.shouldShowPopup()) {
                  RealmsMainScreen.this.setTooltip(Component.literal(pRealmsServer.serverPing.playerList));
               }
            }

            if (RealmsMainScreen.this.isSelfOwnedServer(pRealmsServer) && pRealmsServer.expired) {
               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
               RenderSystem.enableBlend();
               RenderSystem.setShaderTexture(0, RealmsMainScreen.BUTTON_LOCATION);
               RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
               Component component;
               Component component1;
               if (pRealmsServer.expiredTrial) {
                  component = RealmsMainScreen.TRIAL_EXPIRED_TEXT;
                  component1 = RealmsMainScreen.SUBSCRIPTION_CREATE_TEXT;
               } else {
                  component = RealmsMainScreen.SUBSCRIPTION_EXPIRED_TEXT;
                  component1 = RealmsMainScreen.SUBSCRIPTION_RENEW_TEXT;
               }

               int l = RealmsMainScreen.this.font.width(component1) + 17;
               int i1 = 16;
               int j1 = p_86888_ + RealmsMainScreen.this.font.width(component) + 8;
               int k1 = p_86889_ + 13;
               boolean flag = false;
               if (p_86890_ >= j1 && p_86890_ < j1 + l && p_86891_ > k1 && p_86891_ <= k1 + 16 && p_86891_ < RealmsMainScreen.this.height - 40 && p_86891_ > 32 && !RealmsMainScreen.this.shouldShowPopup()) {
                  flag = true;
                  RealmsMainScreen.this.hoveredElement = RealmsMainScreen.HoveredElement.EXPIRED;
               }

               int l1 = flag ? 2 : 1;
               GuiComponent.blit(pPoseStack, j1, k1, 0.0F, (float)(46 + l1 * 20), l / 2, 8, 256, 256);
               GuiComponent.blit(pPoseStack, j1 + l / 2, k1, (float)(200 - l / 2), (float)(46 + l1 * 20), l / 2, 8, 256, 256);
               GuiComponent.blit(pPoseStack, j1, k1 + 8, 0.0F, (float)(46 + l1 * 20 + 12), l / 2, 8, 256, 256);
               GuiComponent.blit(pPoseStack, j1 + l / 2, k1 + 8, (float)(200 - l / 2), (float)(46 + l1 * 20 + 12), l / 2, 8, 256, 256);
               RenderSystem.disableBlend();
               int i2 = p_86889_ + 11 + 5;
               int j2 = flag ? 16777120 : 16777215;
               RealmsMainScreen.this.font.draw(pPoseStack, component, (float)(p_86888_ + 2), (float)(i2 + 1), 15553363);
               GuiComponent.drawCenteredString(pPoseStack, RealmsMainScreen.this.font, component1, j1 + l / 2, i2 + 1, j2);
            } else {
               if (pRealmsServer.worldType == RealmsServer.WorldType.MINIGAME) {
                  int l2 = 13413468;
                  int k = RealmsMainScreen.this.font.width(RealmsMainScreen.SELECT_MINIGAME_PREFIX);
                  RealmsMainScreen.this.font.draw(pPoseStack, RealmsMainScreen.SELECT_MINIGAME_PREFIX, (float)(p_86888_ + 2), (float)(p_86889_ + 12), 13413468);
                  RealmsMainScreen.this.font.draw(pPoseStack, pRealmsServer.getMinigameName(), (float)(p_86888_ + 2 + k), (float)(p_86889_ + 12), 7105644);
               } else {
                  RealmsMainScreen.this.font.draw(pPoseStack, pRealmsServer.getDescription(), (float)(p_86888_ + 2), (float)(p_86889_ + 12), 7105644);
               }

               if (!RealmsMainScreen.this.isSelfOwnedServer(pRealmsServer)) {
                  RealmsMainScreen.this.font.draw(pPoseStack, pRealmsServer.owner, (float)(p_86888_ + 2), (float)(p_86889_ + 12 + 11), 5000268);
               }
            }

            RealmsMainScreen.this.font.draw(pPoseStack, pRealmsServer.getName(), (float)(p_86888_ + 2), (float)(p_86889_ + 1), 16777215);
            RealmsTextureManager.withBoundFace(pRealmsServer.ownerUUID, () -> {
               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
               PlayerFaceRenderer.draw(pPoseStack, p_86888_ - 36, p_86889_, 32);
            });
         }
      }

      public Component getNarration() {
         return (Component)(this.serverData.state == RealmsServer.State.UNINITIALIZED ? RealmsMainScreen.UNITIALIZED_WORLD_NARRATION : Component.translatable("narrator.select", this.serverData.name));
      }

      @Nullable
      public RealmsServer getServer() {
         return this.serverData;
      }
   }

   @OnlyIn(Dist.CLIENT)
   class TrialEntry extends RealmsMainScreen.Entry {
      public void render(PoseStack pPoseStack, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pIsMouseOver, float pPartialTick) {
         this.renderTrialItem(pPoseStack, pIndex, pLeft, pTop, pMouseX, pMouseY);
      }

      public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
         RealmsMainScreen.this.popupOpenedByUser = true;
         return true;
      }

      private void renderTrialItem(PoseStack pPoseStack, int p_86915_, int p_86916_, int p_86917_, int p_86918_, int p_86919_) {
         int i = p_86917_ + 8;
         int j = 0;
         boolean flag = false;
         if (p_86916_ <= p_86918_ && p_86918_ <= (int)RealmsMainScreen.this.realmSelectionList.getScrollAmount() && p_86917_ <= p_86919_ && p_86919_ <= p_86917_ + 32) {
            flag = true;
         }

         int k = 8388479;
         if (flag && !RealmsMainScreen.this.shouldShowPopup()) {
            k = 6077788;
         }

         for(Component component : RealmsMainScreen.TRIAL_MESSAGE_LINES) {
            GuiComponent.drawCenteredString(pPoseStack, RealmsMainScreen.this.font, component, RealmsMainScreen.this.width / 2, i + j, k);
            j += 10;
         }

      }

      public Component getNarration() {
         return RealmsMainScreen.TRIAL_TEXT;
      }

      @Nullable
      public RealmsServer getServer() {
         return null;
      }
   }
}