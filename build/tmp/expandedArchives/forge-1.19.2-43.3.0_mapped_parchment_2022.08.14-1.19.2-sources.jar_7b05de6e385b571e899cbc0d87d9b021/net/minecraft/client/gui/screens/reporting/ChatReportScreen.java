package net.minecraft.client.gui.screens.reporting;

import com.mojang.authlib.minecraft.report.AbuseReportLimits;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.screens.GenericWaitingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.WarningScreen;
import net.minecraft.client.multiplayer.chat.report.ChatReportBuilder;
import net.minecraft.client.multiplayer.chat.report.ReportReason;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ThrowingComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ChatReportScreen extends Screen {
   private static final int BUTTON_WIDTH = 120;
   private static final int BUTTON_HEIGHT = 20;
   private static final int BUTTON_MARGIN = 20;
   private static final int BUTTON_MARGIN_HALF = 10;
   private static final int LABEL_HEIGHT = 25;
   private static final int SCREEN_WIDTH = 280;
   private static final int SCREEN_HEIGHT = 300;
   private static final Component OBSERVED_WHAT_LABEL = Component.translatable("gui.chatReport.observed_what");
   private static final Component SELECT_REASON = Component.translatable("gui.chatReport.select_reason");
   private static final Component MORE_COMMENTS_LABEL = Component.translatable("gui.chatReport.more_comments");
   private static final Component DESCRIBE_PLACEHOLDER = Component.translatable("gui.chatReport.describe");
   private static final Component REPORT_SENT_MESSAGE = Component.translatable("gui.chatReport.report_sent_msg");
   private static final Component SELECT_CHAT_MESSAGE = Component.translatable("gui.chatReport.select_chat");
   private static final Component REPORT_SENDING_TITLE = Component.translatable("gui.abuseReport.sending.title").withStyle(ChatFormatting.BOLD);
   private static final Component REPORT_SENT_TITLE = Component.translatable("gui.abuseReport.sent.title").withStyle(ChatFormatting.BOLD);
   private static final Component REPORT_ERROR_TITLE = Component.translatable("gui.abuseReport.error.title").withStyle(ChatFormatting.BOLD);
   private static final Component REPORT_SEND_GENERIC_ERROR = Component.translatable("gui.abuseReport.send.generic_error");
   private static final Logger LOGGER = LogUtils.getLogger();
   @Nullable
   final Screen lastScreen;
   private final ReportingContext reportingContext;
   @Nullable
   private MultiLineLabel reasonDescriptionLabel;
   @Nullable
   private MultiLineEditBox commentBox;
   private Button sendButton;
   private ChatReportBuilder report;
   @Nullable
   ChatReportBuilder.CannotBuildReason cannotBuildReason;

   public ChatReportScreen(Screen pLastScreen, ReportingContext pReportingContext, UUID pReportId) {
      super(Component.translatable("gui.chatReport.title"));
      this.lastScreen = pLastScreen;
      this.reportingContext = pReportingContext;
      this.report = new ChatReportBuilder(pReportId, pReportingContext.sender().reportLimits());
   }

   protected void init() {
      this.minecraft.keyboardHandler.setSendRepeatsToGui(true);
      AbuseReportLimits abusereportlimits = this.reportingContext.sender().reportLimits();
      int i = this.width / 2;
      ReportReason reportreason = this.report.reason();
      if (reportreason != null) {
         this.reasonDescriptionLabel = MultiLineLabel.create(this.font, reportreason.description(), 280);
      } else {
         this.reasonDescriptionLabel = null;
      }

      IntSet intset = this.report.reportedMessages();
      Component component;
      if (intset.isEmpty()) {
         component = SELECT_CHAT_MESSAGE;
      } else {
         component = Component.translatable("gui.chatReport.selected_chat", intset.size());
      }

      this.addRenderableWidget(new Button(this.contentLeft(), this.selectChatTop(), 280, 20, component, (p_239836_) -> {
         this.minecraft.setScreen(new ChatSelectionScreen(this, this.reportingContext, this.report, (p_239697_) -> {
            this.report = p_239697_;
            this.onReportChanged();
         }));
      }));
      Component component1 = Util.mapNullable(reportreason, ReportReason::title, SELECT_REASON);
      this.addRenderableWidget(new Button(this.contentLeft(), this.selectInfoTop(), 280, 20, component1, (p_239172_) -> {
         this.minecraft.setScreen(new ReportReasonSelectionScreen(this, this.report.reason(), (p_239513_) -> {
            this.report.setReason(p_239513_);
            this.onReportChanged();
         }));
      }));
      this.commentBox = this.addRenderableWidget(new MultiLineEditBox(this.minecraft.font, this.contentLeft(), this.commentBoxTop(), 280, this.commentBoxBottom() - this.commentBoxTop(), DESCRIBE_PLACEHOLDER, Component.translatable("gui.chatReport.comments")));
      this.commentBox.setValue(this.report.comments());
      this.commentBox.setCharacterLimit(abusereportlimits.maxOpinionCommentsLength());
      this.commentBox.setValueListener((p_240036_) -> {
         this.report.setComments(p_240036_);
         this.onReportChanged();
      });
      this.addRenderableWidget(new Button(i - 120, this.completeButtonTop(), 120, 20, CommonComponents.GUI_BACK, (p_239971_) -> {
         this.onClose();
      }));
      this.sendButton = this.addRenderableWidget(new Button(i + 10, this.completeButtonTop(), 120, 20, Component.translatable("gui.chatReport.send"), (p_239742_) -> {
         this.sendReport();
      }, new ChatReportScreen.SubmitButtonTooltip()));
      this.onReportChanged();
   }

   private void onReportChanged() {
      this.cannotBuildReason = this.report.checkBuildable();
      this.sendButton.active = this.cannotBuildReason == null;
   }

   private void sendReport() {
      this.report.build(this.reportingContext).ifLeft((p_240239_) -> {
         CompletableFuture<?> completablefuture = this.reportingContext.sender().send(p_240239_.id(), p_240239_.report());
         this.minecraft.setScreen(GenericWaitingScreen.createWaiting(REPORT_SENDING_TITLE, CommonComponents.GUI_CANCEL, () -> {
            this.minecraft.setScreen(this);
            completablefuture.cancel(true);
         }));
         completablefuture.handleAsync((p_240236_, p_240237_) -> {
            if (p_240237_ == null) {
               this.onReportSendSuccess();
            } else {
               if (p_240237_ instanceof CancellationException) {
                  return null;
               }

               this.onReportSendError(p_240237_);
            }

            return null;
         }, this.minecraft);
      }).ifRight((p_242967_) -> {
         this.displayReportSendError(p_242967_.message());
      });
   }

   private void onReportSendSuccess() {
      this.minecraft.setScreen(GenericWaitingScreen.createCompleted(REPORT_SENT_TITLE, REPORT_SENT_MESSAGE, CommonComponents.GUI_DONE, () -> {
         this.minecraft.setScreen((Screen)null);
      }));
   }

   private void onReportSendError(Throwable pThrowable) {
      LOGGER.error("Encountered error while sending abuse report", pThrowable);
      Throwable throwable = pThrowable.getCause();
      Component component;
      if (throwable instanceof ThrowingComponent throwingcomponent) {
         component = throwingcomponent.getComponent();
      } else {
         component = REPORT_SEND_GENERIC_ERROR;
      }

      this.displayReportSendError(component);
   }

   private void displayReportSendError(Component pMessage) {
      Component component = pMessage.copy().withStyle(ChatFormatting.RED);
      this.minecraft.setScreen(GenericWaitingScreen.createCompleted(REPORT_ERROR_TITLE, component, CommonComponents.GUI_BACK, () -> {
         this.minecraft.setScreen(this);
      }));
   }

   public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
      int i = this.width / 2;
      RenderSystem.disableDepthTest();
      this.renderBackground(pPoseStack);
      drawCenteredString(pPoseStack, this.font, this.title, i, 10, 16777215);
      drawCenteredString(pPoseStack, this.font, OBSERVED_WHAT_LABEL, i, this.selectChatTop() - 9 - 6, 16777215);
      if (this.reasonDescriptionLabel != null) {
         this.reasonDescriptionLabel.renderLeftAligned(pPoseStack, this.contentLeft(), this.selectInfoTop() + 20 + 5, 9, 16777215);
      }

      drawString(pPoseStack, this.font, MORE_COMMENTS_LABEL, this.contentLeft(), this.commentBoxTop() - 9 - 6, 16777215);
      super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
      RenderSystem.enableDepthTest();
   }

   public void tick() {
      this.commentBox.tick();
      super.tick();
   }

   public void onClose() {
      if (!this.commentBox.getValue().isEmpty()) {
         this.minecraft.setScreen(new ChatReportScreen.DiscardReportWarningScreen());
      } else {
         this.minecraft.setScreen(this.lastScreen);
      }

   }

   public boolean mouseReleased(double pMouseX, double pMouseY, int pButton) {
      return super.mouseReleased(pMouseX, pMouseY, pButton) ? true : this.commentBox.mouseReleased(pMouseX, pMouseY, pButton);
   }

   private int contentLeft() {
      return this.width / 2 - 140;
   }

   private int contentRight() {
      return this.width / 2 + 140;
   }

   private int contentTop() {
      return Math.max((this.height - 300) / 2, 0);
   }

   private int contentBottom() {
      return Math.min((this.height + 300) / 2, this.height);
   }

   private int selectChatTop() {
      return this.contentTop() + 40;
   }

   private int selectInfoTop() {
      return this.selectChatTop() + 10 + 20;
   }

   private int commentBoxTop() {
      int i = this.selectInfoTop() + 20 + 25;
      if (this.reasonDescriptionLabel != null) {
         i += (this.reasonDescriptionLabel.getLineCount() + 1) * 9;
      }

      return i;
   }

   private int commentBoxBottom() {
      return this.completeButtonTop() - 20;
   }

   private int completeButtonTop() {
      return this.contentBottom() - 20 - 10;
   }

   @OnlyIn(Dist.CLIENT)
   class DiscardReportWarningScreen extends WarningScreen {
      private static final Component TITLE = Component.translatable("gui.chatReport.discard.title").withStyle(ChatFormatting.BOLD);
      private static final Component MESSAGE = Component.translatable("gui.chatReport.discard.content");
      private static final Component RETURN = Component.translatable("gui.chatReport.discard.return");
      private static final Component DISCARD = Component.translatable("gui.chatReport.discard.discard");

      protected DiscardReportWarningScreen() {
         super(TITLE, MESSAGE, MESSAGE);
      }

      protected void initButtons(int p_239753_) {
         this.addRenderableWidget(new Button(this.width / 2 - 155, 100 + p_239753_, 150, 20, RETURN, (p_239525_) -> {
            this.onClose();
         }));
         this.addRenderableWidget(new Button(this.width / 2 + 5, 100 + p_239753_, 150, 20, DISCARD, (p_239170_) -> {
            this.minecraft.setScreen(ChatReportScreen.this.lastScreen);
         }));
      }

      public void onClose() {
         this.minecraft.setScreen(ChatReportScreen.this);
      }

      public boolean shouldCloseOnEsc() {
         return false;
      }

      protected void renderTitle(PoseStack p_239057_) {
         drawString(p_239057_, this.font, this.title, this.width / 2 - 155, 30, 16777215);
      }
   }

   @OnlyIn(Dist.CLIENT)
   class SubmitButtonTooltip implements Button.OnTooltip {
      public void onTooltip(Button p_240155_, PoseStack p_240156_, int p_240157_, int p_240158_) {
         if (ChatReportScreen.this.cannotBuildReason != null) {
            Component component = ChatReportScreen.this.cannotBuildReason.message();
            ChatReportScreen.this.renderTooltip(p_240156_, ChatReportScreen.this.font.split(component, Math.max(ChatReportScreen.this.width / 2 - 43, 170)), p_240157_, p_240158_);
         }

      }
   }
}