package net.minecraft.client.gui.screens;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BackupConfirmScreen extends Screen {
   private final Screen lastScreen;
   protected final BackupConfirmScreen.Listener listener;
   private final Component description;
   private final boolean promptForCacheErase;
   private MultiLineLabel message = MultiLineLabel.EMPTY;
   protected int id;
   private Checkbox eraseCache;

   public BackupConfirmScreen(Screen pLastScreen, BackupConfirmScreen.Listener pListener, Component pTitle, Component pDescription, boolean pPromptForCacheErase) {
      super(pTitle);
      this.lastScreen = pLastScreen;
      this.listener = pListener;
      this.description = pDescription;
      this.promptForCacheErase = pPromptForCacheErase;
   }

   protected void init() {
      super.init();
      this.message = MultiLineLabel.create(this.font, this.description, this.width - 50);
      int i = (this.message.getLineCount() + 1) * 9;
      this.addRenderableWidget(new Button(this.width / 2 - 155, 100 + i, 150, 20, Component.translatable("selectWorld.backupJoinConfirmButton"), (p_95564_) -> {
         this.listener.proceed(true, this.eraseCache.selected());
      }));
      this.addRenderableWidget(new Button(this.width / 2 - 155 + 160, 100 + i, 150, 20, Component.translatable("selectWorld.backupJoinSkipButton"), (p_95562_) -> {
         this.listener.proceed(false, this.eraseCache.selected());
      }));
      this.addRenderableWidget(new Button(this.width / 2 - 155 + 80, 124 + i, 150, 20, CommonComponents.GUI_CANCEL, (p_95558_) -> {
         this.minecraft.setScreen(this.lastScreen);
      }));
      this.eraseCache = new Checkbox(this.width / 2 - 155 + 80, 76 + i, 150, 20, Component.translatable("selectWorld.backupEraseCache"), false);
      if (this.promptForCacheErase) {
         this.addRenderableWidget(this.eraseCache);
      }

   }

   public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
      this.renderBackground(pPoseStack);
      drawCenteredString(pPoseStack, this.font, this.title, this.width / 2, 50, 16777215);
      this.message.renderCentered(pPoseStack, this.width / 2, 70);
      super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }

   public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
      if (pKeyCode == 256) {
         this.minecraft.setScreen(this.lastScreen);
         return true;
      } else {
         return super.keyPressed(pKeyCode, pScanCode, pModifiers);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public interface Listener {
      void proceed(boolean pConfirmed, boolean pEraseCache);
   }
}