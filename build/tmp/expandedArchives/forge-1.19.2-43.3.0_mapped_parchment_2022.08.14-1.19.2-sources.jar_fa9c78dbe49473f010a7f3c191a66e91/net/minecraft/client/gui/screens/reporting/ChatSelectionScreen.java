package net.minecraft.client.gui.screens.reporting;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.report.AbuseReportLimits;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.chat.ChatTrustLevel;
import net.minecraft.client.multiplayer.chat.LoggedChatMessage;
import net.minecraft.client.multiplayer.chat.report.ChatReportBuilder;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ChatSelectionScreen extends Screen {
   private static final Component TITLE = Component.translatable("gui.chatSelection.title");
   private static final Component CONTEXT_INFO = Component.translatable("gui.chatSelection.context").withStyle(ChatFormatting.GRAY);
   @Nullable
   private final Screen lastScreen;
   private final ReportingContext reportingContext;
   private Button confirmSelectedButton;
   private MultiLineLabel contextInfoLabel;
   @Nullable
   private ChatSelectionScreen.ChatSelectionList chatSelectionList;
   final ChatReportBuilder report;
   private final Consumer<ChatReportBuilder> onSelected;
   private ChatSelectionLogFiller<LoggedChatMessage.Player> chatLogFiller;
   @Nullable
   private List<FormattedCharSequence> tooltip;

   public ChatSelectionScreen(@Nullable Screen pLastScreen, ReportingContext pReportingContext, ChatReportBuilder pReport, Consumer<ChatReportBuilder> pOnSelected) {
      super(TITLE);
      this.lastScreen = pLastScreen;
      this.reportingContext = pReportingContext;
      this.report = pReport.copy();
      this.onSelected = pOnSelected;
   }

   protected void init() {
      this.chatLogFiller = new ChatSelectionLogFiller<>(this.reportingContext.chatLog(), this::canReport, LoggedChatMessage.Player.class);
      this.contextInfoLabel = MultiLineLabel.create(this.font, CONTEXT_INFO, this.width - 16);
      this.chatSelectionList = new ChatSelectionScreen.ChatSelectionList(this.minecraft, (this.contextInfoLabel.getLineCount() + 1) * 9);
      this.chatSelectionList.setRenderBackground(false);
      this.addWidget(this.chatSelectionList);
      this.addRenderableWidget(new Button(this.width / 2 - 155, this.height - 32, 150, 20, CommonComponents.GUI_BACK, (p_239860_) -> {
         this.onClose();
      }));
      this.confirmSelectedButton = this.addRenderableWidget(new Button(this.width / 2 - 155 + 160, this.height - 32, 150, 20, CommonComponents.GUI_DONE, (p_239591_) -> {
         this.onSelected.accept(this.report);
         this.onClose();
      }));
      this.updateConfirmSelectedButton();
      this.extendLog();
      this.chatSelectionList.setScrollAmount((double)this.chatSelectionList.getMaxScroll());
   }

   private boolean canReport(LoggedChatMessage p_242240_) {
      return p_242240_.canReport(this.report.reportedProfileId());
   }

   private void extendLog() {
      int i = this.chatSelectionList.getMaxVisibleEntries();
      this.chatLogFiller.fillNextPage(i, this.chatSelectionList);
   }

   void onReachedScrollTop() {
      this.extendLog();
   }

   void updateConfirmSelectedButton() {
      this.confirmSelectedButton.active = !this.report.reportedMessages().isEmpty();
   }

   public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
      this.renderBackground(pPoseStack);
      this.chatSelectionList.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
      drawCenteredString(pPoseStack, this.font, this.title, this.width / 2, 16, 16777215);
      AbuseReportLimits abusereportlimits = this.reportingContext.sender().reportLimits();
      int i = this.report.reportedMessages().size();
      int j = abusereportlimits.maxReportedMessageCount();
      Component component = Component.translatable("gui.chatSelection.selected", i, j);
      drawCenteredString(pPoseStack, this.font, component, this.width / 2, 16 + 9 * 3 / 2, 10526880);
      this.contextInfoLabel.renderCentered(pPoseStack, this.width / 2, this.chatSelectionList.getFooterTop());
      super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
      if (this.tooltip != null) {
         this.renderTooltip(pPoseStack, this.tooltip, pMouseX, pMouseY);
         this.tooltip = null;
      }

   }

   public void onClose() {
      this.minecraft.setScreen(this.lastScreen);
   }

   public Component getNarrationMessage() {
      return CommonComponents.joinForNarration(super.getNarrationMessage(), CONTEXT_INFO);
   }

   void setTooltip(@Nullable List<FormattedCharSequence> pTooltip) {
      this.tooltip = pTooltip;
   }

   @OnlyIn(Dist.CLIENT)
   public class ChatSelectionList extends ObjectSelectionList<ChatSelectionScreen.ChatSelectionList.Entry> implements ChatSelectionLogFiller.Output<LoggedChatMessage.Player> {
      @Nullable
      private ChatSelectionScreen.ChatSelectionList.Heading previousHeading;

      public ChatSelectionList(Minecraft pMinecraft, int p_239061_) {
         super(pMinecraft, ChatSelectionScreen.this.width, ChatSelectionScreen.this.height, 40, ChatSelectionScreen.this.height - 40 - p_239061_, 16);
      }

      public void setScrollAmount(double pScroll) {
         double d0 = this.getScrollAmount();
         super.setScrollAmount(pScroll);
         if ((float)this.getMaxScroll() > 1.0E-5F && pScroll <= (double)1.0E-5F && !Mth.equal(pScroll, d0)) {
            ChatSelectionScreen.this.onReachedScrollTop();
         }

      }

      public void acceptMessage(int pChatId, LoggedChatMessage.Player pPlayer) {
         boolean flag = pPlayer.canReport(ChatSelectionScreen.this.report.reportedProfileId());
         ChatTrustLevel chattrustlevel = pPlayer.trustLevel();
         GuiMessageTag guimessagetag = chattrustlevel.createTag(pPlayer.message());
         ChatSelectionScreen.ChatSelectionList.Entry chatselectionscreen$chatselectionlist$entry = new ChatSelectionScreen.ChatSelectionList.MessageEntry(pChatId, pPlayer.toContentComponent(), pPlayer.toNarrationComponent(), guimessagetag, flag, true);
         this.addEntryToTop(chatselectionscreen$chatselectionlist$entry);
         this.updateHeading(pPlayer, flag);
      }

      private void updateHeading(LoggedChatMessage.Player pPlayer, boolean pCanReport) {
         ChatSelectionScreen.ChatSelectionList.Entry chatselectionscreen$chatselectionlist$entry = new ChatSelectionScreen.ChatSelectionList.MessageHeadingEntry(pPlayer.profile(), pPlayer.toHeadingComponent(), pCanReport);
         this.addEntryToTop(chatselectionscreen$chatselectionlist$entry);
         ChatSelectionScreen.ChatSelectionList.Heading chatselectionscreen$chatselectionlist$heading = new ChatSelectionScreen.ChatSelectionList.Heading(pPlayer.profileId(), chatselectionscreen$chatselectionlist$entry);
         if (this.previousHeading != null && this.previousHeading.canCombine(chatselectionscreen$chatselectionlist$heading)) {
            this.removeEntryFromTop(this.previousHeading.entry());
         }

         this.previousHeading = chatselectionscreen$chatselectionlist$heading;
      }

      public void acceptDivider(Component pText) {
         this.addEntryToTop(new ChatSelectionScreen.ChatSelectionList.PaddingEntry());
         this.addEntryToTop(new ChatSelectionScreen.ChatSelectionList.DividerEntry(pText));
         this.addEntryToTop(new ChatSelectionScreen.ChatSelectionList.PaddingEntry());
         this.previousHeading = null;
      }

      protected int getScrollbarPosition() {
         return (this.width + this.getRowWidth()) / 2;
      }

      public int getRowWidth() {
         return Math.min(350, this.width - 50);
      }

      public int getMaxVisibleEntries() {
         return Mth.positiveCeilDiv(this.y1 - this.y0, this.itemHeight);
      }

      protected void renderItem(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick, int pIndex, int pLeft, int pTop, int pWidth, int pHeight) {
         ChatSelectionScreen.ChatSelectionList.Entry chatselectionscreen$chatselectionlist$entry = this.getEntry(pIndex);
         if (this.shouldHighlightEntry(chatselectionscreen$chatselectionlist$entry)) {
            boolean flag = this.getSelected() == chatselectionscreen$chatselectionlist$entry;
            int i = this.isFocused() && flag ? -1 : -8355712;
            this.renderSelection(pPoseStack, pTop, pWidth, pHeight, i, -16777216);
         }

         chatselectionscreen$chatselectionlist$entry.render(pPoseStack, pIndex, pTop, pLeft, pWidth, pHeight, pMouseX, pMouseY, this.getHovered() == chatselectionscreen$chatselectionlist$entry, pPartialTick);
      }

      private boolean shouldHighlightEntry(ChatSelectionScreen.ChatSelectionList.Entry pEntry) {
         if (pEntry.canSelect()) {
            boolean flag = this.getSelected() == pEntry;
            boolean flag1 = this.getSelected() == null;
            boolean flag2 = this.getHovered() == pEntry;
            return flag || flag1 && flag2 && pEntry.canReport();
         } else {
            return false;
         }
      }

      protected void moveSelection(AbstractSelectionList.SelectionDirection pOrdering) {
         if (!this.moveSelectableSelection(pOrdering) && pOrdering == AbstractSelectionList.SelectionDirection.UP) {
            ChatSelectionScreen.this.onReachedScrollTop();
            this.moveSelectableSelection(pOrdering);
         }

      }

      private boolean moveSelectableSelection(AbstractSelectionList.SelectionDirection pDirection) {
         return this.moveSelection(pDirection, ChatSelectionScreen.ChatSelectionList.Entry::canSelect);
      }

      public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
         ChatSelectionScreen.ChatSelectionList.Entry chatselectionscreen$chatselectionlist$entry = this.getSelected();
         if (chatselectionscreen$chatselectionlist$entry != null && chatselectionscreen$chatselectionlist$entry.keyPressed(pKeyCode, pScanCode, pModifiers)) {
            return true;
         } else {
            this.setFocused((GuiEventListener)null);
            return super.keyPressed(pKeyCode, pScanCode, pModifiers);
         }
      }

      public int getFooterTop() {
         return this.y1 + 9;
      }

      protected boolean isFocused() {
         return ChatSelectionScreen.this.getFocused() == this;
      }

      @OnlyIn(Dist.CLIENT)
      public class DividerEntry extends ChatSelectionScreen.ChatSelectionList.Entry {
         private static final int COLOR = -6250336;
         private final Component text;

         public DividerEntry(Component pText) {
            this.text = pText;
         }

         public void render(PoseStack pPoseStack, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pIsMouseOver, float pPartialTick) {
            int i = pTop + pHeight / 2;
            int j = pLeft + pWidth - 8;
            int k = ChatSelectionScreen.this.font.width(this.text);
            int l = (pLeft + j - k) / 2;
            int i1 = i - 9 / 2;
            GuiComponent.drawString(pPoseStack, ChatSelectionScreen.this.font, this.text, l, i1, -6250336);
         }

         public Component getNarration() {
            return this.text;
         }
      }

      @OnlyIn(Dist.CLIENT)
      public abstract class Entry extends ObjectSelectionList.Entry<ChatSelectionScreen.ChatSelectionList.Entry> {
         public Component getNarration() {
            return CommonComponents.EMPTY;
         }

         public boolean isSelected() {
            return false;
         }

         public boolean canSelect() {
            return false;
         }

         public boolean canReport() {
            return this.canSelect();
         }
      }

      @OnlyIn(Dist.CLIENT)
      static record Heading(UUID sender, ChatSelectionScreen.ChatSelectionList.Entry entry) {
         public boolean canCombine(ChatSelectionScreen.ChatSelectionList.Heading pOther) {
            return pOther.sender.equals(this.sender);
         }
      }

      @OnlyIn(Dist.CLIENT)
      public class MessageEntry extends ChatSelectionScreen.ChatSelectionList.Entry {
         private static final ResourceLocation CHECKMARK_TEXTURE = new ResourceLocation("realms", "textures/gui/realms/checkmark.png");
         private static final int CHECKMARK_WIDTH = 9;
         private static final int CHECKMARK_HEIGHT = 8;
         private static final int INDENT_AMOUNT = 11;
         private static final int TAG_MARGIN_LEFT = 4;
         private final int chatId;
         private final FormattedText text;
         private final Component narration;
         @Nullable
         private final List<FormattedCharSequence> hoverText;
         @Nullable
         private final GuiMessageTag.Icon tagIcon;
         @Nullable
         private final List<FormattedCharSequence> tagHoverText;
         private final boolean canReport;
         private final boolean playerMessage;

         public MessageEntry(int pChatId, Component pText, @Nullable Component pNarration, GuiMessageTag p_240551_, boolean pCanReport, boolean pPlayerMessage) {
            this.chatId = pChatId;
            this.tagIcon = Util.mapNullable(p_240551_, GuiMessageTag::icon);
            this.tagHoverText = p_240551_ != null && p_240551_.text() != null ? ChatSelectionScreen.this.font.split(p_240551_.text(), ChatSelectionList.this.getRowWidth()) : null;
            this.canReport = pCanReport;
            this.playerMessage = pPlayerMessage;
            FormattedText formattedtext = ChatSelectionScreen.this.font.substrByWidth(pText, this.getMaximumTextWidth() - ChatSelectionScreen.this.font.width(CommonComponents.ELLIPSIS));
            if (pText != formattedtext) {
               this.text = FormattedText.composite(formattedtext, CommonComponents.ELLIPSIS);
               this.hoverText = ChatSelectionScreen.this.font.split(pText, ChatSelectionList.this.getRowWidth());
            } else {
               this.text = pText;
               this.hoverText = null;
            }

            this.narration = pNarration;
         }

         public void render(PoseStack pPoseStack, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pIsMouseOver, float pPartialTick) {
            if (this.isSelected() && this.canReport) {
               this.renderSelectedCheckmark(pPoseStack, pTop, pLeft, pHeight);
            }

            int i = pLeft + this.getTextIndent();
            int j = pTop + 1 + (pHeight - 9) / 2;
            GuiComponent.drawString(pPoseStack, ChatSelectionScreen.this.font, Language.getInstance().getVisualOrder(this.text), i, j, this.canReport ? -1 : -1593835521);
            if (this.hoverText != null && pIsMouseOver) {
               ChatSelectionScreen.this.setTooltip(this.hoverText);
            }

            int k = ChatSelectionScreen.this.font.width(this.text);
            this.renderTag(pPoseStack, i + k + 4, pTop, pHeight, pMouseX, pMouseY);
         }

         private void renderTag(PoseStack pPoseStack, int pX, int pY, int p_240581_, int p_240614_, int p_240612_) {
            if (this.tagIcon != null) {
               int i = pY + (p_240581_ - this.tagIcon.height) / 2;
               this.tagIcon.draw(pPoseStack, pX, i);
               if (this.tagHoverText != null && p_240614_ >= pX && p_240614_ <= pX + this.tagIcon.width && p_240612_ >= i && p_240612_ <= i + this.tagIcon.height) {
                  ChatSelectionScreen.this.setTooltip(this.tagHoverText);
               }
            }

         }

         private void renderSelectedCheckmark(PoseStack pPoseStack, int p_240275_, int p_240276_, int p_240277_) {
            int i = p_240275_ + (p_240277_ - 8) / 2;
            RenderSystem.setShaderTexture(0, CHECKMARK_TEXTURE);
            RenderSystem.enableBlend();
            GuiComponent.blit(pPoseStack, p_240276_, i, 0.0F, 0.0F, 9, 8, 9, 8);
            RenderSystem.disableBlend();
         }

         private int getMaximumTextWidth() {
            int i = this.tagIcon != null ? this.tagIcon.width + 4 : 0;
            return ChatSelectionList.this.getRowWidth() - this.getTextIndent() - 4 - i;
         }

         private int getTextIndent() {
            return this.playerMessage ? 11 : 0;
         }

         public Component getNarration() {
            return (Component)(this.isSelected() ? Component.translatable("narrator.select", this.narration) : this.narration);
         }

         public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
            if (pButton == 0) {
               ChatSelectionList.this.setSelected((ChatSelectionScreen.ChatSelectionList.Entry)null);
               return this.toggleReport();
            } else {
               return false;
            }
         }

         public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
            return pKeyCode != 257 && pKeyCode != 32 && pKeyCode != 335 ? false : this.toggleReport();
         }

         public boolean isSelected() {
            return ChatSelectionScreen.this.report.isReported(this.chatId);
         }

         public boolean canSelect() {
            return true;
         }

         public boolean canReport() {
            return this.canReport;
         }

         private boolean toggleReport() {
            if (this.canReport) {
               ChatSelectionScreen.this.report.toggleReported(this.chatId);
               ChatSelectionScreen.this.updateConfirmSelectedButton();
               return true;
            } else {
               return false;
            }
         }
      }

      @OnlyIn(Dist.CLIENT)
      public class MessageHeadingEntry extends ChatSelectionScreen.ChatSelectionList.Entry {
         private static final int FACE_SIZE = 12;
         private final Component heading;
         private final ResourceLocation skin;
         private final boolean canReport;

         public MessageHeadingEntry(GameProfile pProfile, Component pHeading, boolean pCanReport) {
            this.heading = pHeading;
            this.canReport = pCanReport;
            this.skin = ChatSelectionList.this.minecraft.getSkinManager().getInsecureSkinLocation(pProfile);
         }

         public void render(PoseStack pPoseStack, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pIsMouseOver, float pPartialTick) {
            int i = pLeft - 12 - 4;
            int j = pTop + (pHeight - 12) / 2;
            this.renderFace(pPoseStack, i, j, this.skin);
            int k = pTop + 1 + (pHeight - 9) / 2;
            GuiComponent.drawString(pPoseStack, ChatSelectionScreen.this.font, this.heading, pLeft, k, this.canReport ? -1 : -1593835521);
         }

         private void renderFace(PoseStack pPoseStack, int pX, int pY, ResourceLocation pTextureId) {
            RenderSystem.setShaderTexture(0, pTextureId);
            PlayerFaceRenderer.draw(pPoseStack, pX, pY, 12);
         }
      }

      @OnlyIn(Dist.CLIENT)
      public class PaddingEntry extends ChatSelectionScreen.ChatSelectionList.Entry {
         public void render(PoseStack p_240109_, int p_240110_, int p_240111_, int p_240112_, int p_240113_, int p_240114_, int p_240115_, int p_240116_, boolean p_240117_, float p_240118_) {
         }
      }
   }
}