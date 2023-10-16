package net.minecraft.client.gui.screens.multiplayer;

import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.chat.ChatPreviewStatus;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ChatPreviewWarningScreen extends WarningScreen {
   private static final Component TITLE = Component.translatable("chatPreview.warning.title").withStyle(ChatFormatting.BOLD);
   private static final Component CHECK = Component.translatable("chatPreview.warning.check");
   private final ServerData serverData;
   @Nullable
   private final Screen lastScreen;

   private static Component content() {
      ChatPreviewStatus chatpreviewstatus = Minecraft.getInstance().options.chatPreview().get();
      return Component.translatable("chatPreview.warning.content", chatpreviewstatus.getCaption());
   }

   public ChatPreviewWarningScreen(@Nullable Screen p_232837_, ServerData p_232838_) {
      super(TITLE, content(), CHECK, CommonComponents.joinForNarration(TITLE, content()));
      this.serverData = p_232838_;
      this.lastScreen = p_232837_;
   }

   protected void initButtons(int p_232840_) {
      this.addRenderableWidget(new Button(this.width / 2 - 155, 100 + p_232840_, 150, 20, Component.translatable("menu.disconnect"), (p_232846_) -> {
         this.minecraft.level.disconnect();
         this.minecraft.clearLevel();
         this.minecraft.setScreen(new JoinMultiplayerScreen(new TitleScreen()));
      }));
      this.addRenderableWidget(new Button(this.width / 2 + 5, 100 + p_232840_, 150, 20, CommonComponents.GUI_PROCEED, (p_232842_) -> {
         this.updateOptions();
         this.onClose();
      }));
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }

   private void updateOptions() {
      if (this.stopShowing != null && this.stopShowing.selected()) {
         ServerData.ChatPreview serverdata$chatpreview = this.serverData.getChatPreview();
         if (serverdata$chatpreview != null) {
            serverdata$chatpreview.acknowledge();
            ServerList.saveSingleServer(this.serverData);
         }
      }

   }

   protected int getLineHeight() {
      return 9 * 3 / 2;
   }

   public void onClose() {
      this.minecraft.setScreen(this.lastScreen);
   }
}