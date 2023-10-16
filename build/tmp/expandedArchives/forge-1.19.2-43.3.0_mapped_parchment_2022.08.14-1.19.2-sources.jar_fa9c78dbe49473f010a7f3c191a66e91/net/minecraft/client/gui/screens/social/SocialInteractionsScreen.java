package net.minecraft.client.gui.screens.social;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SocialInteractionsScreen extends Screen {
   protected static final ResourceLocation SOCIAL_INTERACTIONS_LOCATION = new ResourceLocation("textures/gui/social_interactions.png");
   private static final Component TAB_ALL = Component.translatable("gui.socialInteractions.tab_all");
   private static final Component TAB_HIDDEN = Component.translatable("gui.socialInteractions.tab_hidden");
   private static final Component TAB_BLOCKED = Component.translatable("gui.socialInteractions.tab_blocked");
   private static final Component TAB_ALL_SELECTED = TAB_ALL.plainCopy().withStyle(ChatFormatting.UNDERLINE);
   private static final Component TAB_HIDDEN_SELECTED = TAB_HIDDEN.plainCopy().withStyle(ChatFormatting.UNDERLINE);
   private static final Component TAB_BLOCKED_SELECTED = TAB_BLOCKED.plainCopy().withStyle(ChatFormatting.UNDERLINE);
   private static final Component SEARCH_HINT = Component.translatable("gui.socialInteractions.search_hint").withStyle(ChatFormatting.ITALIC).withStyle(ChatFormatting.GRAY);
   static final Component EMPTY_SEARCH = Component.translatable("gui.socialInteractions.search_empty").withStyle(ChatFormatting.GRAY);
   private static final Component EMPTY_HIDDEN = Component.translatable("gui.socialInteractions.empty_hidden").withStyle(ChatFormatting.GRAY);
   private static final Component EMPTY_BLOCKED = Component.translatable("gui.socialInteractions.empty_blocked").withStyle(ChatFormatting.GRAY);
   private static final Component BLOCKING_HINT = Component.translatable("gui.socialInteractions.blocking_hint");
   private static final String BLOCK_LINK = "https://aka.ms/javablocking";
   private static final int BG_BORDER_SIZE = 8;
   private static final int BG_UNITS = 16;
   private static final int BG_WIDTH = 236;
   private static final int SEARCH_HEIGHT = 16;
   private static final int MARGIN_Y = 64;
   public static final int LIST_START = 88;
   public static final int SEARCH_START = 78;
   private static final int IMAGE_WIDTH = 238;
   private static final int BUTTON_HEIGHT = 20;
   private static final int ITEM_HEIGHT = 36;
   SocialInteractionsPlayerList socialInteractionsPlayerList;
   EditBox searchBox;
   private String lastSearch = "";
   private SocialInteractionsScreen.Page page = SocialInteractionsScreen.Page.ALL;
   private Button allButton;
   private Button hiddenButton;
   private Button blockedButton;
   private Button blockingHintButton;
   @Nullable
   private Component serverLabel;
   private int playerCount;
   private boolean initialized;
   @Nullable
   private Runnable postRenderRunnable;

   public SocialInteractionsScreen() {
      super(Component.translatable("gui.socialInteractions.title"));
      this.updateServerLabel(Minecraft.getInstance());
   }

   private int windowHeight() {
      return Math.max(52, this.height - 128 - 16);
   }

   private int backgroundUnits() {
      return this.windowHeight() / 16;
   }

   private int listEnd() {
      return 80 + this.backgroundUnits() * 16 - 8;
   }

   private int marginX() {
      return (this.width - 238) / 2;
   }

   public Component getNarrationMessage() {
      return (Component)(this.serverLabel != null ? CommonComponents.joinForNarration(super.getNarrationMessage(), this.serverLabel) : super.getNarrationMessage());
   }

   public void tick() {
      super.tick();
      this.searchBox.tick();
   }

   protected void init() {
      this.minecraft.keyboardHandler.setSendRepeatsToGui(true);
      if (this.initialized) {
         this.socialInteractionsPlayerList.updateSize(this.width, this.height, 88, this.listEnd());
      } else {
         this.socialInteractionsPlayerList = new SocialInteractionsPlayerList(this, this.minecraft, this.width, this.height, 88, this.listEnd(), 36);
      }

      int i = this.socialInteractionsPlayerList.getRowWidth() / 3;
      int j = this.socialInteractionsPlayerList.getRowLeft();
      int k = this.socialInteractionsPlayerList.getRowRight();
      int l = this.font.width(BLOCKING_HINT) + 40;
      int i1 = 64 + 16 * this.backgroundUnits();
      int j1 = (this.width - l) / 2 + 3;
      this.allButton = this.addRenderableWidget(new Button(j, 45, i, 20, TAB_ALL, (p_240243_) -> {
         this.showPage(SocialInteractionsScreen.Page.ALL);
      }));
      this.hiddenButton = this.addRenderableWidget(new Button((j + k - i) / 2 + 1, 45, i, 20, TAB_HIDDEN, (p_100791_) -> {
         this.showPage(SocialInteractionsScreen.Page.HIDDEN);
      }));
      this.blockedButton = this.addRenderableWidget(new Button(k - i + 1, 45, i, 20, TAB_BLOCKED, (p_100785_) -> {
         this.showPage(SocialInteractionsScreen.Page.BLOCKED);
      }));
      String s = this.searchBox != null ? this.searchBox.getValue() : "";
      this.searchBox = new EditBox(this.font, this.marginX() + 28, 78, 196, 16, SEARCH_HINT) {
         protected MutableComponent createNarrationMessage() {
            return !SocialInteractionsScreen.this.searchBox.getValue().isEmpty() && SocialInteractionsScreen.this.socialInteractionsPlayerList.isEmpty() ? super.createNarrationMessage().append(", ").append(SocialInteractionsScreen.EMPTY_SEARCH) : super.createNarrationMessage();
         }
      };
      this.searchBox.setMaxLength(16);
      this.searchBox.setBordered(false);
      this.searchBox.setVisible(true);
      this.searchBox.setTextColor(16777215);
      this.searchBox.setValue(s);
      this.searchBox.setResponder(this::checkSearchStringUpdate);
      this.addWidget(this.searchBox);
      this.addWidget(this.socialInteractionsPlayerList);
      this.blockingHintButton = this.addRenderableWidget(new Button(j1, i1, l, 20, BLOCKING_HINT, (p_100770_) -> {
         this.minecraft.setScreen(new ConfirmLinkScreen((p_170143_) -> {
            if (p_170143_) {
               Util.getPlatform().openUri("https://aka.ms/javablocking");
            }

            this.minecraft.setScreen(this);
         }, "https://aka.ms/javablocking", true));
      }));
      this.initialized = true;
      this.showPage(this.page);
   }

   private void showPage(SocialInteractionsScreen.Page pPage) {
      this.page = pPage;
      this.allButton.setMessage(TAB_ALL);
      this.hiddenButton.setMessage(TAB_HIDDEN);
      this.blockedButton.setMessage(TAB_BLOCKED);
      boolean flag = false;
      switch (pPage) {
         case ALL:
            this.allButton.setMessage(TAB_ALL_SELECTED);
            Collection<UUID> collection = this.minecraft.player.connection.getOnlinePlayerIds();
            this.socialInteractionsPlayerList.updatePlayerList(collection, this.socialInteractionsPlayerList.getScrollAmount(), true);
            break;
         case HIDDEN:
            this.hiddenButton.setMessage(TAB_HIDDEN_SELECTED);
            Set<UUID> set1 = this.minecraft.getPlayerSocialManager().getHiddenPlayers();
            flag = set1.isEmpty();
            this.socialInteractionsPlayerList.updatePlayerList(set1, this.socialInteractionsPlayerList.getScrollAmount(), false);
            break;
         case BLOCKED:
            this.blockedButton.setMessage(TAB_BLOCKED_SELECTED);
            PlayerSocialManager playersocialmanager = this.minecraft.getPlayerSocialManager();
            Set<UUID> set = this.minecraft.player.connection.getOnlinePlayerIds().stream().filter(playersocialmanager::isBlocked).collect(Collectors.toSet());
            flag = set.isEmpty();
            this.socialInteractionsPlayerList.updatePlayerList(set, this.socialInteractionsPlayerList.getScrollAmount(), false);
      }

      GameNarrator gamenarrator = this.minecraft.getNarrator();
      if (!this.searchBox.getValue().isEmpty() && this.socialInteractionsPlayerList.isEmpty() && !this.searchBox.isFocused()) {
         gamenarrator.sayNow(EMPTY_SEARCH);
      } else if (flag) {
         if (pPage == SocialInteractionsScreen.Page.HIDDEN) {
            gamenarrator.sayNow(EMPTY_HIDDEN);
         } else if (pPage == SocialInteractionsScreen.Page.BLOCKED) {
            gamenarrator.sayNow(EMPTY_BLOCKED);
         }
      }

   }

   public void removed() {
      this.minecraft.keyboardHandler.setSendRepeatsToGui(false);
   }

   public void renderBackground(PoseStack pPoseStack) {
      int i = this.marginX() + 3;
      super.renderBackground(pPoseStack);
      RenderSystem.setShaderTexture(0, SOCIAL_INTERACTIONS_LOCATION);
      this.blit(pPoseStack, i, 64, 1, 1, 236, 8);
      int j = this.backgroundUnits();

      for(int k = 0; k < j; ++k) {
         this.blit(pPoseStack, i, 72 + 16 * k, 1, 10, 236, 16);
      }

      this.blit(pPoseStack, i, 72 + 16 * j, 1, 27, 236, 8);
      this.blit(pPoseStack, i + 10, 76, 243, 1, 12, 12);
   }

   public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
      this.updateServerLabel(this.minecraft);
      this.renderBackground(pPoseStack);
      if (this.serverLabel != null) {
         drawString(pPoseStack, this.minecraft.font, this.serverLabel, this.marginX() + 8, 35, -1);
      }

      if (!this.socialInteractionsPlayerList.isEmpty()) {
         this.socialInteractionsPlayerList.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
      } else if (!this.searchBox.getValue().isEmpty()) {
         drawCenteredString(pPoseStack, this.minecraft.font, EMPTY_SEARCH, this.width / 2, (78 + this.listEnd()) / 2, -1);
      } else if (this.page == SocialInteractionsScreen.Page.HIDDEN) {
         drawCenteredString(pPoseStack, this.minecraft.font, EMPTY_HIDDEN, this.width / 2, (78 + this.listEnd()) / 2, -1);
      } else if (this.page == SocialInteractionsScreen.Page.BLOCKED) {
         drawCenteredString(pPoseStack, this.minecraft.font, EMPTY_BLOCKED, this.width / 2, (78 + this.listEnd()) / 2, -1);
      }

      if (!this.searchBox.isFocused() && this.searchBox.getValue().isEmpty()) {
         drawString(pPoseStack, this.minecraft.font, SEARCH_HINT, this.searchBox.x, this.searchBox.y, -1);
      } else {
         this.searchBox.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
      }

      this.blockingHintButton.visible = this.page == SocialInteractionsScreen.Page.BLOCKED;
      super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
      if (this.postRenderRunnable != null) {
         this.postRenderRunnable.run();
      }

   }

   public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
      if (this.searchBox.isFocused()) {
         this.searchBox.mouseClicked(pMouseX, pMouseY, pButton);
      }

      return super.mouseClicked(pMouseX, pMouseY, pButton) || this.socialInteractionsPlayerList.mouseClicked(pMouseX, pMouseY, pButton);
   }

   public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
      if (!this.searchBox.isFocused() && this.minecraft.options.keySocialInteractions.matches(pKeyCode, pScanCode)) {
         this.minecraft.setScreen((Screen)null);
         return true;
      } else {
         return super.keyPressed(pKeyCode, pScanCode, pModifiers);
      }
   }

   public boolean isPauseScreen() {
      return false;
   }

   private void checkSearchStringUpdate(String p_100789_) {
      p_100789_ = p_100789_.toLowerCase(Locale.ROOT);
      if (!p_100789_.equals(this.lastSearch)) {
         this.socialInteractionsPlayerList.setFilter(p_100789_);
         this.lastSearch = p_100789_;
         this.showPage(this.page);
      }

   }

   private void updateServerLabel(Minecraft pMinecraft) {
      int i = pMinecraft.getConnection().getOnlinePlayers().size();
      if (this.playerCount != i) {
         String s = "";
         ServerData serverdata = pMinecraft.getCurrentServer();
         if (pMinecraft.isLocalServer()) {
            s = pMinecraft.getSingleplayerServer().getMotd();
         } else if (serverdata != null) {
            s = serverdata.name;
         }

         if (i > 1) {
            this.serverLabel = Component.translatable("gui.socialInteractions.server_label.multiple", s, i);
         } else {
            this.serverLabel = Component.translatable("gui.socialInteractions.server_label.single", s, i);
         }

         this.playerCount = i;
      }

   }

   public void onAddPlayer(PlayerInfo pPlayerInfo) {
      this.socialInteractionsPlayerList.addPlayer(pPlayerInfo, this.page);
   }

   public void onRemovePlayer(UUID pId) {
      this.socialInteractionsPlayerList.removePlayer(pId);
   }

   public void setPostRenderRunnable(@Nullable Runnable pPoseRenderRunnable) {
      this.postRenderRunnable = pPoseRenderRunnable;
   }

   @OnlyIn(Dist.CLIENT)
   public static enum Page {
      ALL,
      HIDDEN,
      BLOCKED;
   }
}