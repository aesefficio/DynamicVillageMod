package net.minecraft.client.gui.screens;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DeathScreen extends Screen {
   /** The integer value containing the number of ticks that have passed since the player's death */
   private int delayTicker;
   private final Component causeOfDeath;
   private final boolean hardcore;
   private Component deathScore;
   private final List<Button> exitButtons = Lists.newArrayList();

   public DeathScreen(@Nullable Component pCauseOfDeath, boolean pHardcore) {
      super(Component.translatable(pHardcore ? "deathScreen.title.hardcore" : "deathScreen.title"));
      this.causeOfDeath = pCauseOfDeath;
      this.hardcore = pHardcore;
   }

   protected void init() {
      this.delayTicker = 0;
      this.exitButtons.clear();
      this.exitButtons.add(this.addRenderableWidget(new Button(this.width / 2 - 100, this.height / 4 + 72, 200, 20, this.hardcore ? Component.translatable("deathScreen.spectate") : Component.translatable("deathScreen.respawn"), (p_95930_) -> {
         this.minecraft.player.respawn();
         this.minecraft.setScreen((Screen)null);
      })));
      this.exitButtons.add(this.addRenderableWidget(new Button(this.width / 2 - 100, this.height / 4 + 96, 200, 20, Component.translatable("deathScreen.titleScreen"), (p_95925_) -> {
         if (this.hardcore) {
            confirmResult(true);
            this.exitToTitleScreen();
         } else {
            ConfirmScreen confirmscreen = new ConfirmScreen(this::confirmResult, Component.translatable("deathScreen.quit.confirm"), CommonComponents.EMPTY, Component.translatable("deathScreen.titleScreen"), Component.translatable("deathScreen.respawn"));
            this.minecraft.setScreen(confirmscreen);
            confirmscreen.setDelay(20);
         }
      })));

      for(Button button : this.exitButtons) {
         button.active = false;
      }

      this.deathScore = Component.translatable("deathScreen.score").append(": ").append(Component.literal(Integer.toString(this.minecraft.player.getScore())).withStyle(ChatFormatting.YELLOW));
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }

   private void confirmResult(boolean p_95932_) {
      if (p_95932_) {
         this.exitToTitleScreen();
      } else {
         this.minecraft.player.respawn();
         this.minecraft.setScreen((Screen)null);
      }

   }

   private void exitToTitleScreen() {
      if (this.minecraft.level != null) {
         this.minecraft.level.disconnect();
      }

      this.minecraft.clearLevel(new GenericDirtMessageScreen(Component.translatable("menu.savingLevel")));
      this.minecraft.setScreen(new TitleScreen());
   }

   public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
      this.fillGradient(pPoseStack, 0, 0, this.width, this.height, 1615855616, -1602211792);
      pPoseStack.pushPose();
      pPoseStack.scale(2.0F, 2.0F, 2.0F);
      drawCenteredString(pPoseStack, this.font, this.title, this.width / 2 / 2, 30, 16777215);
      pPoseStack.popPose();
      if (this.causeOfDeath != null) {
         drawCenteredString(pPoseStack, this.font, this.causeOfDeath, this.width / 2, 85, 16777215);
      }

      drawCenteredString(pPoseStack, this.font, this.deathScore, this.width / 2, 100, 16777215);
      if (this.causeOfDeath != null && pMouseY > 85 && pMouseY < 85 + 9) {
         Style style = this.getClickedComponentStyleAt(pMouseX);
         this.renderComponentHoverEffect(pPoseStack, style, pMouseX, pMouseY);
      }

      super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
   }

   @Nullable
   private Style getClickedComponentStyleAt(int p_95918_) {
      if (this.causeOfDeath == null) {
         return null;
      } else {
         int i = this.minecraft.font.width(this.causeOfDeath);
         int j = this.width / 2 - i / 2;
         int k = this.width / 2 + i / 2;
         return p_95918_ >= j && p_95918_ <= k ? this.minecraft.font.getSplitter().componentStyleAtWidth(this.causeOfDeath, p_95918_ - j) : null;
      }
   }

   public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
      if (this.causeOfDeath != null && pMouseY > 85.0D && pMouseY < (double)(85 + 9)) {
         Style style = this.getClickedComponentStyleAt((int)pMouseX);
         if (style != null && style.getClickEvent() != null && style.getClickEvent().getAction() == ClickEvent.Action.OPEN_URL) {
            this.handleComponentClicked(style);
            return false;
         }
      }

      return super.mouseClicked(pMouseX, pMouseY, pButton);
   }

   public boolean isPauseScreen() {
      return false;
   }

   public void tick() {
      super.tick();
      ++this.delayTicker;
      if (this.delayTicker == 20) {
         for(Button button : this.exitButtons) {
            button.active = true;
         }
      }

   }
}
