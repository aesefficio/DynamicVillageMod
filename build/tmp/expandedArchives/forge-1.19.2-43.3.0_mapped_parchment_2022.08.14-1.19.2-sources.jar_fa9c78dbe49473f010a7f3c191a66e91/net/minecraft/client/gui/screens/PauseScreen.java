package net.minecraft.client.gui.screens;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.realmsclient.RealmsMainScreen;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.social.SocialInteractionsScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PauseScreen extends Screen {
   private static final String URL_FEEDBACK_SNAPSHOT = "https://aka.ms/snapshotfeedback?ref=game";
   private static final String URL_FEEDBACK_RELEASE = "https://aka.ms/javafeedback?ref=game";
   private static final String URL_BUGS = "https://aka.ms/snapshotbugs?ref=game";
   private final boolean showPauseMenu;

   public PauseScreen(boolean pShowPauseMenu) {
      super(pShowPauseMenu ? Component.translatable("menu.game") : Component.translatable("menu.paused"));
      this.showPauseMenu = pShowPauseMenu;
   }

   protected void init() {
      if (this.showPauseMenu) {
         this.createPauseMenu();
      }

   }

   private void createPauseMenu() {
      int i = -16;
      int j = 98;
      this.addRenderableWidget(new Button(this.width / 2 - 102, this.height / 4 + 24 + -16, 204, 20, Component.translatable("menu.returnToGame"), (p_96337_) -> {
         this.minecraft.setScreen((Screen)null);
         this.minecraft.mouseHandler.grabMouse();
      }));
      this.addRenderableWidget(new Button(this.width / 2 - 102, this.height / 4 + 48 + -16, 98, 20, Component.translatable("gui.advancements"), (p_96335_) -> {
         this.minecraft.setScreen(new AdvancementsScreen(this.minecraft.player.connection.getAdvancements()));
      }));
      this.addRenderableWidget(new Button(this.width / 2 + 4, this.height / 4 + 48 + -16, 98, 20, Component.translatable("gui.stats"), (p_96333_) -> {
         this.minecraft.setScreen(new StatsScreen(this, this.minecraft.player.getStats()));
      }));
      String s = SharedConstants.getCurrentVersion().isStable() ? "https://aka.ms/javafeedback?ref=game" : "https://aka.ms/snapshotfeedback?ref=game";
      this.addRenderableWidget(new Button(this.width / 2 - 102, this.height / 4 + 72 + -16, 98, 20, Component.translatable("menu.sendFeedback"), (p_96318_) -> {
         this.minecraft.setScreen(new ConfirmLinkScreen((p_169337_) -> {
            if (p_169337_) {
               Util.getPlatform().openUri(s);
            }

            this.minecraft.setScreen(this);
         }, s, true));
      }));
      Button button = this.addRenderableWidget(new Button(this.width / 2 + 4, this.height / 4 + 72 + -16, 98, 20, Component.translatable("menu.reportBugs"), (p_96331_) -> {
         this.minecraft.setScreen(new ConfirmLinkScreen((p_169339_) -> {
            if (p_169339_) {
               Util.getPlatform().openUri("https://aka.ms/snapshotbugs?ref=game");
            }

            this.minecraft.setScreen(this);
         }, "https://aka.ms/snapshotbugs?ref=game", true));
      }));
      button.active = !SharedConstants.getCurrentVersion().getDataVersion().isSideSeries();
      this.addRenderableWidget(new Button(this.width / 2 - 102, this.height / 4 + 96 + -16, 98, 20, Component.translatable("menu.options"), (p_96323_) -> {
         this.minecraft.setScreen(new OptionsScreen(this, this.minecraft.options));
      }));
      if (this.minecraft.hasSingleplayerServer() && !this.minecraft.getSingleplayerServer().isPublished()) {
         this.addRenderableWidget(new Button(this.width / 2 + 4, this.height / 4 + 96 + -16, 98, 20, Component.translatable("menu.shareToLan"), (p_96321_) -> {
            this.minecraft.setScreen(new ShareToLanScreen(this));
         }));
      } else {
         this.addRenderableWidget(new Button(this.width / 2 + 4, this.height / 4 + 96 + -16, 98, 20, Component.translatable("menu.playerReporting"), (p_238870_) -> {
            this.minecraft.setScreen(new SocialInteractionsScreen());
         }));
      }
      this.addRenderableWidget(new Button(this.width / 2 - 102, this.height / 4 + 120 - 16, 204, 20, Component.translatable("fml.menu.mods"), modButton -> this.minecraft.setScreen(new net.minecraftforge.client.gui.ModListScreen(this))));

      Component component = this.minecraft.isLocalServer() ? Component.translatable("menu.returnToMenu") : Component.translatable("menu.disconnect");
      this.addRenderableWidget(new Button(this.width / 2 - 102, this.height / 4 + 144 + -16, 204, 20, component, (p_96315_) -> {
         boolean flag = this.minecraft.isLocalServer();
         boolean flag1 = this.minecraft.isConnectedToRealms();
         p_96315_.active = false;
         this.minecraft.level.disconnect();
         if (flag) {
            this.minecraft.clearLevel(new GenericDirtMessageScreen(Component.translatable("menu.savingLevel")));
         } else {
            this.minecraft.clearLevel();
         }

         TitleScreen titlescreen = new TitleScreen();
         if (flag) {
            this.minecraft.setScreen(titlescreen);
         } else if (flag1) {
            this.minecraft.setScreen(new RealmsMainScreen(titlescreen));
         } else {
            this.minecraft.setScreen(new JoinMultiplayerScreen(titlescreen));
         }

      }));
   }

   public void tick() {
      super.tick();
   }

   public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
      if (this.showPauseMenu) {
         this.renderBackground(pPoseStack);
         drawCenteredString(pPoseStack, this.font, this.title, this.width / 2, 40, 16777215);
      } else {
         drawCenteredString(pPoseStack, this.font, this.title, this.width / 2, 10, 16777215);
      }

      super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
   }
}
