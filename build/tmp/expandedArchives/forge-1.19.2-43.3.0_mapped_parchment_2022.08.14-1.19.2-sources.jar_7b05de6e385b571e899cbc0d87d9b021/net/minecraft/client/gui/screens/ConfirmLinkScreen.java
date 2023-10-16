package net.minecraft.client.gui.screens;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ConfirmLinkScreen extends ConfirmScreen {
   private static final Component COPY_BUTTON_TEXT = Component.translatable("chat.copy");
   private static final Component WARNING_TEXT = Component.translatable("chat.link.warning");
   private final String url;
   private final boolean showWarning;

   public ConfirmLinkScreen(BooleanConsumer pCallback, String pUrl, boolean pTrusted) {
      this(pCallback, confirmMessage(pTrusted), Component.literal(pUrl), pUrl, pTrusted ? CommonComponents.GUI_CANCEL : CommonComponents.GUI_NO, pTrusted);
   }

   public ConfirmLinkScreen(BooleanConsumer pCallback, Component pTitle, String pUrl, boolean pTrusted) {
      this(pCallback, pTitle, pUrl, pTrusted ? CommonComponents.GUI_CANCEL : CommonComponents.GUI_NO, pTrusted);
   }

   public ConfirmLinkScreen(BooleanConsumer pCallback, Component pTitle, String pUrl, Component pNoButton, boolean pTrusted) {
      this(pCallback, pTitle, confirmMessage(pTrusted, pUrl), pUrl, pNoButton, pTrusted);
   }

   public ConfirmLinkScreen(BooleanConsumer pCallback, Component pTitle, Component pMessage, String pUrl, Component pNoButton, boolean pTrusted) {
      super(pCallback, pTitle, pMessage);
      this.yesButton = (Component)(pTrusted ? Component.translatable("chat.link.open") : CommonComponents.GUI_YES);
      this.noButton = pNoButton;
      this.showWarning = !pTrusted;
      this.url = pUrl;
   }

   protected static MutableComponent confirmMessage(boolean pTrusted, String pCallback) {
      return confirmMessage(pTrusted).append(" ").append(Component.literal(pCallback));
   }

   protected static MutableComponent confirmMessage(boolean pTrusted) {
      return Component.translatable(pTrusted ? "chat.link.confirmTrusted" : "chat.link.confirm");
   }

   protected void addButtons(int pY) {
      this.addRenderableWidget(new Button(this.width / 2 - 50 - 105, pY, 100, 20, this.yesButton, (p_169249_) -> {
         this.callback.accept(true);
      }));
      this.addRenderableWidget(new Button(this.width / 2 - 50, pY, 100, 20, COPY_BUTTON_TEXT, (p_169247_) -> {
         this.copyToClipboard();
         this.callback.accept(false);
      }));
      this.addRenderableWidget(new Button(this.width / 2 - 50 + 105, pY, 100, 20, this.noButton, (p_169245_) -> {
         this.callback.accept(false);
      }));
   }

   /**
    * Copies the link to the system clipboard.
    */
   public void copyToClipboard() {
      this.minecraft.keyboardHandler.setClipboard(this.url);
   }

   public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
      super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
      if (this.showWarning) {
         drawCenteredString(pPoseStack, this.font, WARNING_TEXT, this.width / 2, 110, 16764108);
      }

   }
}