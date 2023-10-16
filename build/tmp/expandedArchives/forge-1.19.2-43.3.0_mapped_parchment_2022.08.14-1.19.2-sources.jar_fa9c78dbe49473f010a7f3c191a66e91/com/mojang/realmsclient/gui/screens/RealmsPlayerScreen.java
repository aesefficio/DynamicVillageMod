package com.mojang.realmsclient.gui.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.Ops;
import com.mojang.realmsclient.dto.PlayerInfo;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.util.RealmsTextureManager;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsObjectSelectionList;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsPlayerScreen extends RealmsScreen {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final ResourceLocation OP_ICON_LOCATION = new ResourceLocation("realms", "textures/gui/realms/op_icon.png");
   private static final ResourceLocation USER_ICON_LOCATION = new ResourceLocation("realms", "textures/gui/realms/user_icon.png");
   private static final ResourceLocation CROSS_ICON_LOCATION = new ResourceLocation("realms", "textures/gui/realms/cross_player_icon.png");
   private static final ResourceLocation OPTIONS_BACKGROUND = new ResourceLocation("minecraft", "textures/gui/options_background.png");
   private static final Component NORMAL_USER_TOOLTIP = Component.translatable("mco.configure.world.invites.normal.tooltip");
   private static final Component OP_TOOLTIP = Component.translatable("mco.configure.world.invites.ops.tooltip");
   private static final Component REMOVE_ENTRY_TOOLTIP = Component.translatable("mco.configure.world.invites.remove.tooltip");
   private static final Component INVITED_LABEL = Component.translatable("mco.configure.world.invited");
   @Nullable
   private Component toolTip;
   private final RealmsConfigureWorldScreen lastScreen;
   final RealmsServer serverData;
   private RealmsPlayerScreen.InvitedObjectSelectionList invitedObjectSelectionList;
   int column1X;
   int columnWidth;
   private int column2X;
   private Button removeButton;
   private Button opdeopButton;
   private int selectedInvitedIndex = -1;
   private String selectedInvited;
   int player = -1;
   private boolean stateChanged;
   RealmsPlayerScreen.UserAction hoveredUserAction = RealmsPlayerScreen.UserAction.NONE;

   public RealmsPlayerScreen(RealmsConfigureWorldScreen pLastScreen, RealmsServer pServerData) {
      super(Component.translatable("mco.configure.world.players.title"));
      this.lastScreen = pLastScreen;
      this.serverData = pServerData;
   }

   public void init() {
      this.column1X = this.width / 2 - 160;
      this.columnWidth = 150;
      this.column2X = this.width / 2 + 12;
      this.minecraft.keyboardHandler.setSendRepeatsToGui(true);
      this.invitedObjectSelectionList = new RealmsPlayerScreen.InvitedObjectSelectionList();
      this.invitedObjectSelectionList.setLeftPos(this.column1X);
      this.addWidget(this.invitedObjectSelectionList);

      for(PlayerInfo playerinfo : this.serverData.players) {
         this.invitedObjectSelectionList.addEntry(playerinfo);
      }

      this.addRenderableWidget(new Button(this.column2X, row(1), this.columnWidth + 10, 20, Component.translatable("mco.configure.world.buttons.invite"), (p_89176_) -> {
         this.minecraft.setScreen(new RealmsInviteScreen(this.lastScreen, this, this.serverData));
      }));
      this.removeButton = this.addRenderableWidget(new Button(this.column2X, row(7), this.columnWidth + 10, 20, Component.translatable("mco.configure.world.invites.remove.tooltip"), (p_89161_) -> {
         this.uninvite(this.player);
      }));
      this.opdeopButton = this.addRenderableWidget(new Button(this.column2X, row(9), this.columnWidth + 10, 20, Component.translatable("mco.configure.world.invites.ops.tooltip"), (p_89139_) -> {
         if (this.serverData.players.get(this.player).isOperator()) {
            this.deop(this.player);
         } else {
            this.op(this.player);
         }

      }));
      this.addRenderableWidget(new Button(this.column2X + this.columnWidth / 2 + 2, row(12), this.columnWidth / 2 + 10 - 2, 20, CommonComponents.GUI_BACK, (p_89122_) -> {
         this.backButtonClicked();
      }));
      this.updateButtonStates();
   }

   void updateButtonStates() {
      this.removeButton.visible = this.shouldRemoveAndOpdeopButtonBeVisible(this.player);
      this.opdeopButton.visible = this.shouldRemoveAndOpdeopButtonBeVisible(this.player);
   }

   private boolean shouldRemoveAndOpdeopButtonBeVisible(int p_89191_) {
      return p_89191_ != -1;
   }

   public void removed() {
      this.minecraft.keyboardHandler.setSendRepeatsToGui(false);
   }

   public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
      if (pKeyCode == 256) {
         this.backButtonClicked();
         return true;
      } else {
         return super.keyPressed(pKeyCode, pScanCode, pModifiers);
      }
   }

   private void backButtonClicked() {
      if (this.stateChanged) {
         this.minecraft.setScreen(this.lastScreen.getNewScreen());
      } else {
         this.minecraft.setScreen(this.lastScreen);
      }

   }

   void op(int pIndex) {
      this.updateButtonStates();
      RealmsClient realmsclient = RealmsClient.create();
      String s = this.serverData.players.get(pIndex).getUuid();

      try {
         this.updateOps(realmsclient.op(this.serverData.id, s));
      } catch (RealmsServiceException realmsserviceexception) {
         LOGGER.error("Couldn't op the user");
      }

   }

   void deop(int pIndex) {
      this.updateButtonStates();
      RealmsClient realmsclient = RealmsClient.create();
      String s = this.serverData.players.get(pIndex).getUuid();

      try {
         this.updateOps(realmsclient.deop(this.serverData.id, s));
      } catch (RealmsServiceException realmsserviceexception) {
         LOGGER.error("Couldn't deop the user");
      }

   }

   private void updateOps(Ops pOps) {
      for(PlayerInfo playerinfo : this.serverData.players) {
         playerinfo.setOperator(pOps.ops.contains(playerinfo.getName()));
      }

   }

   void uninvite(int pIndex) {
      this.updateButtonStates();
      if (pIndex >= 0 && pIndex < this.serverData.players.size()) {
         PlayerInfo playerinfo = this.serverData.players.get(pIndex);
         this.selectedInvited = playerinfo.getUuid();
         this.selectedInvitedIndex = pIndex;
         RealmsConfirmScreen realmsconfirmscreen = new RealmsConfirmScreen((p_89163_) -> {
            if (p_89163_) {
               RealmsClient realmsclient = RealmsClient.create();

               try {
                  realmsclient.uninvite(this.serverData.id, this.selectedInvited);
               } catch (RealmsServiceException realmsserviceexception) {
                  LOGGER.error("Couldn't uninvite user");
               }

               this.deleteFromInvitedList(this.selectedInvitedIndex);
               this.player = -1;
               this.updateButtonStates();
            }

            this.stateChanged = true;
            this.minecraft.setScreen(this);
         }, Component.literal("Question"), Component.translatable("mco.configure.world.uninvite.question").append(" '").append(playerinfo.getName()).append("' ?"));
         this.minecraft.setScreen(realmsconfirmscreen);
      }

   }

   private void deleteFromInvitedList(int pIndex) {
      this.serverData.players.remove(pIndex);
   }

   public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
      this.toolTip = null;
      this.hoveredUserAction = RealmsPlayerScreen.UserAction.NONE;
      this.renderBackground(pPoseStack);
      if (this.invitedObjectSelectionList != null) {
         this.invitedObjectSelectionList.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
      }

      drawCenteredString(pPoseStack, this.font, this.title, this.width / 2, 17, 16777215);
      int i = row(12) + 20;
      Tesselator tesselator = Tesselator.getInstance();
      BufferBuilder bufferbuilder = tesselator.getBuilder();
      RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
      RenderSystem.setShaderTexture(0, OPTIONS_BACKGROUND);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      float f = 32.0F;
      bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
      bufferbuilder.vertex(0.0D, (double)this.height, 0.0D).uv(0.0F, (float)(this.height - i) / 32.0F + 0.0F).color(64, 64, 64, 255).endVertex();
      bufferbuilder.vertex((double)this.width, (double)this.height, 0.0D).uv((float)this.width / 32.0F, (float)(this.height - i) / 32.0F + 0.0F).color(64, 64, 64, 255).endVertex();
      bufferbuilder.vertex((double)this.width, (double)i, 0.0D).uv((float)this.width / 32.0F, 0.0F).color(64, 64, 64, 255).endVertex();
      bufferbuilder.vertex(0.0D, (double)i, 0.0D).uv(0.0F, 0.0F).color(64, 64, 64, 255).endVertex();
      tesselator.end();
      if (this.serverData != null && this.serverData.players != null) {
         this.font.draw(pPoseStack, Component.empty().append(INVITED_LABEL).append(" (").append(Integer.toString(this.serverData.players.size())).append(")"), (float)this.column1X, (float)row(0), 10526880);
      } else {
         this.font.draw(pPoseStack, INVITED_LABEL, (float)this.column1X, (float)row(0), 10526880);
      }

      super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
      if (this.serverData != null) {
         this.renderMousehoverTooltip(pPoseStack, this.toolTip, pMouseX, pMouseY);
      }
   }

   protected void renderMousehoverTooltip(PoseStack pPoseStack, @Nullable Component pTooltip, int pMouseX, int pMouseY) {
      if (pTooltip != null) {
         int i = pMouseX + 12;
         int j = pMouseY - 12;
         int k = this.font.width(pTooltip);
         this.fillGradient(pPoseStack, i - 3, j - 3, i + k + 3, j + 8 + 3, -1073741824, -1073741824);
         this.font.drawShadow(pPoseStack, pTooltip, (float)i, (float)j, 16777215);
      }
   }

   void drawRemoveIcon(PoseStack pPoseStack, int pX, int pY, int p_89146_, int p_89147_) {
      boolean flag = p_89146_ >= pX && p_89146_ <= pX + 9 && p_89147_ >= pY && p_89147_ <= pY + 9 && p_89147_ < row(12) + 20 && p_89147_ > row(1);
      RenderSystem.setShaderTexture(0, CROSS_ICON_LOCATION);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      float f = flag ? 7.0F : 0.0F;
      GuiComponent.blit(pPoseStack, pX, pY, 0.0F, f, 8, 7, 8, 14);
      if (flag) {
         this.toolTip = REMOVE_ENTRY_TOOLTIP;
         this.hoveredUserAction = RealmsPlayerScreen.UserAction.REMOVE;
      }

   }

   void drawOpped(PoseStack pPoseStack, int pX, int pY, int p_89168_, int p_89169_) {
      boolean flag = p_89168_ >= pX && p_89168_ <= pX + 9 && p_89169_ >= pY && p_89169_ <= pY + 9 && p_89169_ < row(12) + 20 && p_89169_ > row(1);
      RenderSystem.setShaderTexture(0, OP_ICON_LOCATION);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      float f = flag ? 8.0F : 0.0F;
      GuiComponent.blit(pPoseStack, pX, pY, 0.0F, f, 8, 8, 8, 16);
      if (flag) {
         this.toolTip = OP_TOOLTIP;
         this.hoveredUserAction = RealmsPlayerScreen.UserAction.TOGGLE_OP;
      }

   }

   void drawNormal(PoseStack pPoseStack, int pX, int pY, int p_89182_, int p_89183_) {
      boolean flag = p_89182_ >= pX && p_89182_ <= pX + 9 && p_89183_ >= pY && p_89183_ <= pY + 9 && p_89183_ < row(12) + 20 && p_89183_ > row(1);
      RenderSystem.setShaderTexture(0, USER_ICON_LOCATION);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      float f = flag ? 8.0F : 0.0F;
      GuiComponent.blit(pPoseStack, pX, pY, 0.0F, f, 8, 8, 8, 16);
      if (flag) {
         this.toolTip = NORMAL_USER_TOOLTIP;
         this.hoveredUserAction = RealmsPlayerScreen.UserAction.TOGGLE_OP;
      }

   }

   @OnlyIn(Dist.CLIENT)
   class Entry extends ObjectSelectionList.Entry<RealmsPlayerScreen.Entry> {
      private final PlayerInfo playerInfo;

      public Entry(PlayerInfo pPlayerInfo) {
         this.playerInfo = pPlayerInfo;
      }

      public void render(PoseStack pPoseStack, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pIsMouseOver, float pPartialTick) {
         this.renderInvitedItem(pPoseStack, this.playerInfo, pLeft, pTop, pMouseX, pMouseY);
      }

      private void renderInvitedItem(PoseStack pPoseStack, PlayerInfo pPlayerInfo, int pLeft, int pTop, int pMouseX, int pMouseY) {
         int i;
         if (!pPlayerInfo.getAccepted()) {
            i = 10526880;
         } else if (pPlayerInfo.getOnline()) {
            i = 8388479;
         } else {
            i = 16777215;
         }

         RealmsPlayerScreen.this.font.draw(pPoseStack, pPlayerInfo.getName(), (float)(RealmsPlayerScreen.this.column1X + 3 + 12), (float)(pTop + 1), i);
         if (pPlayerInfo.isOperator()) {
            RealmsPlayerScreen.this.drawOpped(pPoseStack, RealmsPlayerScreen.this.column1X + RealmsPlayerScreen.this.columnWidth - 10, pTop + 1, pMouseX, pMouseY);
         } else {
            RealmsPlayerScreen.this.drawNormal(pPoseStack, RealmsPlayerScreen.this.column1X + RealmsPlayerScreen.this.columnWidth - 10, pTop + 1, pMouseX, pMouseY);
         }

         RealmsPlayerScreen.this.drawRemoveIcon(pPoseStack, RealmsPlayerScreen.this.column1X + RealmsPlayerScreen.this.columnWidth - 22, pTop + 2, pMouseX, pMouseY);
         RealmsTextureManager.withBoundFace(pPlayerInfo.getUuid(), () -> {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            PlayerFaceRenderer.draw(pPoseStack, RealmsPlayerScreen.this.column1X + 2 + 2, pTop + 1, 8);
         });
      }

      public Component getNarration() {
         return Component.translatable("narrator.select", this.playerInfo.getName());
      }
   }

   @OnlyIn(Dist.CLIENT)
   class InvitedObjectSelectionList extends RealmsObjectSelectionList<RealmsPlayerScreen.Entry> {
      public InvitedObjectSelectionList() {
         super(RealmsPlayerScreen.this.columnWidth + 10, RealmsPlayerScreen.row(12) + 20, RealmsPlayerScreen.row(1), RealmsPlayerScreen.row(12) + 20, 13);
      }

      public void addEntry(PlayerInfo pPlayerInfo) {
         this.addEntry(RealmsPlayerScreen.this.new Entry(pPlayerInfo));
      }

      public int getRowWidth() {
         return (int)((double)this.width * 1.0D);
      }

      public boolean isFocused() {
         return RealmsPlayerScreen.this.getFocused() == this;
      }

      public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
         if (pButton == 0 && pMouseX < (double)this.getScrollbarPosition() && pMouseY >= (double)this.y0 && pMouseY <= (double)this.y1) {
            int i = RealmsPlayerScreen.this.column1X;
            int j = RealmsPlayerScreen.this.column1X + RealmsPlayerScreen.this.columnWidth;
            int k = (int)Math.floor(pMouseY - (double)this.y0) - this.headerHeight + (int)this.getScrollAmount() - 4;
            int l = k / this.itemHeight;
            if (pMouseX >= (double)i && pMouseX <= (double)j && l >= 0 && k >= 0 && l < this.getItemCount()) {
               this.selectItem(l);
               this.itemClicked(k, l, pMouseX, pMouseY, this.width);
            }

            return true;
         } else {
            return super.mouseClicked(pMouseX, pMouseY, pButton);
         }
      }

      public void itemClicked(int p_89236_, int p_89237_, double p_89238_, double p_89239_, int p_89240_) {
         if (p_89237_ >= 0 && p_89237_ <= RealmsPlayerScreen.this.serverData.players.size() && RealmsPlayerScreen.this.hoveredUserAction != RealmsPlayerScreen.UserAction.NONE) {
            if (RealmsPlayerScreen.this.hoveredUserAction == RealmsPlayerScreen.UserAction.TOGGLE_OP) {
               if (RealmsPlayerScreen.this.serverData.players.get(p_89237_).isOperator()) {
                  RealmsPlayerScreen.this.deop(p_89237_);
               } else {
                  RealmsPlayerScreen.this.op(p_89237_);
               }
            } else if (RealmsPlayerScreen.this.hoveredUserAction == RealmsPlayerScreen.UserAction.REMOVE) {
               RealmsPlayerScreen.this.uninvite(p_89237_);
            }

         }
      }

      public void selectItem(int pIndex) {
         super.selectItem(pIndex);
         this.selectInviteListItem(pIndex);
      }

      public void selectInviteListItem(int pIndex) {
         RealmsPlayerScreen.this.player = pIndex;
         RealmsPlayerScreen.this.updateButtonStates();
      }

      public void setSelected(@Nullable RealmsPlayerScreen.Entry pSelected) {
         super.setSelected(pSelected);
         RealmsPlayerScreen.this.player = this.children().indexOf(pSelected);
         RealmsPlayerScreen.this.updateButtonStates();
      }

      public void renderBackground(PoseStack pPoseStack) {
         RealmsPlayerScreen.this.renderBackground(pPoseStack);
      }

      public int getScrollbarPosition() {
         return RealmsPlayerScreen.this.column1X + this.width - 5;
      }

      public int getMaxPosition() {
         return this.getItemCount() * 13;
      }
   }

   @OnlyIn(Dist.CLIENT)
   static enum UserAction {
      TOGGLE_OP,
      REMOVE,
      NONE;
   }
}