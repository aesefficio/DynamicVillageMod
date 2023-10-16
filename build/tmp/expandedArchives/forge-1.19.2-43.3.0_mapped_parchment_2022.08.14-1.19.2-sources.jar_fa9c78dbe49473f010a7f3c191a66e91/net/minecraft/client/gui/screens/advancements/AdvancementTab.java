package net.minecraft.client.gui.screens.advancements;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AdvancementTab extends GuiComponent {
   private final Minecraft minecraft;
   private final AdvancementsScreen screen;
   private final AdvancementTabType type;
   private final int index;
   private final Advancement advancement;
   private final DisplayInfo display;
   private final ItemStack icon;
   private final Component title;
   private final AdvancementWidget root;
   private final Map<Advancement, AdvancementWidget> widgets = Maps.newLinkedHashMap();
   private double scrollX;
   private double scrollY;
   private int minX = Integer.MAX_VALUE;
   private int minY = Integer.MAX_VALUE;
   private int maxX = Integer.MIN_VALUE;
   private int maxY = Integer.MIN_VALUE;
   private float fade;
   private boolean centered;
   private int page;

   public AdvancementTab(Minecraft pMinecraft, AdvancementsScreen pScreen, AdvancementTabType pType, int pIndex, Advancement pAdvancement, DisplayInfo pDisplay) {
      this.minecraft = pMinecraft;
      this.screen = pScreen;
      this.type = pType;
      this.index = pIndex;
      this.advancement = pAdvancement;
      this.display = pDisplay;
      this.icon = pDisplay.getIcon();
      this.title = pDisplay.getTitle();
      this.root = new AdvancementWidget(this, pMinecraft, pAdvancement, pDisplay);
      this.addWidget(this.root, pAdvancement);
   }

   public AdvancementTab(Minecraft mc, AdvancementsScreen screen, AdvancementTabType type, int index, int page, Advancement adv, DisplayInfo info) {
      this(mc, screen, type, index, adv, info);
      this.page = page;
   }

   public int getPage() {
      return page;
   }

   public AdvancementTabType getType() {
      return this.type;
   }

   public int getIndex() {
      return this.index;
   }

   public Advancement getAdvancement() {
      return this.advancement;
   }

   public Component getTitle() {
      return this.title;
   }

   public DisplayInfo getDisplay() {
      return this.display;
   }

   public void drawTab(PoseStack pPoseStack, int pOffsetX, int pOffsetY, boolean pIsSelected) {
      this.type.draw(pPoseStack, this, pOffsetX, pOffsetY, pIsSelected, this.index);
   }

   public void drawIcon(int pOffsetX, int pOffsetY, ItemRenderer pRenderer) {
      this.type.drawIcon(pOffsetX, pOffsetY, this.index, pRenderer, this.icon);
   }

   public void drawContents(PoseStack pPoseStack) {
      if (!this.centered) {
         this.scrollX = (double)(117 - (this.maxX + this.minX) / 2);
         this.scrollY = (double)(56 - (this.maxY + this.minY) / 2);
         this.centered = true;
      }

      pPoseStack.pushPose();
      pPoseStack.translate(0.0D, 0.0D, 950.0D);
      RenderSystem.enableDepthTest();
      RenderSystem.colorMask(false, false, false, false);
      fill(pPoseStack, 4680, 2260, -4680, -2260, -16777216);
      RenderSystem.colorMask(true, true, true, true);
      pPoseStack.translate(0.0D, 0.0D, -950.0D);
      RenderSystem.depthFunc(518);
      fill(pPoseStack, 234, 113, 0, 0, -16777216);
      RenderSystem.depthFunc(515);
      ResourceLocation resourcelocation = this.display.getBackground();
      RenderSystem.setShader(GameRenderer::getPositionTexShader);
      if (resourcelocation != null) {
         RenderSystem.setShaderTexture(0, resourcelocation);
      } else {
         RenderSystem.setShaderTexture(0, TextureManager.INTENTIONAL_MISSING_TEXTURE);
      }

      int i = Mth.floor(this.scrollX);
      int j = Mth.floor(this.scrollY);
      int k = i % 16;
      int l = j % 16;

      for(int i1 = -1; i1 <= 15; ++i1) {
         for(int j1 = -1; j1 <= 8; ++j1) {
            blit(pPoseStack, k + 16 * i1, l + 16 * j1, 0.0F, 0.0F, 16, 16, 16, 16);
         }
      }

      this.root.drawConnectivity(pPoseStack, i, j, true);
      this.root.drawConnectivity(pPoseStack, i, j, false);
      this.root.draw(pPoseStack, i, j);
      RenderSystem.depthFunc(518);
      pPoseStack.translate(0.0D, 0.0D, -950.0D);
      RenderSystem.colorMask(false, false, false, false);
      fill(pPoseStack, 4680, 2260, -4680, -2260, -16777216);
      RenderSystem.colorMask(true, true, true, true);
      RenderSystem.depthFunc(515);
      pPoseStack.popPose();
   }

   public void drawTooltips(PoseStack pPoseStack, int pMouseX, int pMouseY, int pWidth, int pHeight) {
      pPoseStack.pushPose();
      pPoseStack.translate(0.0D, 0.0D, -200.0D);
      fill(pPoseStack, 0, 0, 234, 113, Mth.floor(this.fade * 255.0F) << 24);
      boolean flag = false;
      int i = Mth.floor(this.scrollX);
      int j = Mth.floor(this.scrollY);
      if (pMouseX > 0 && pMouseX < 234 && pMouseY > 0 && pMouseY < 113) {
         for(AdvancementWidget advancementwidget : this.widgets.values()) {
            if (advancementwidget.isMouseOver(i, j, pMouseX, pMouseY)) {
               flag = true;
               advancementwidget.drawHover(pPoseStack, i, j, this.fade, pWidth, pHeight);
               break;
            }
         }
      }

      pPoseStack.popPose();
      if (flag) {
         this.fade = Mth.clamp(this.fade + 0.02F, 0.0F, 0.3F);
      } else {
         this.fade = Mth.clamp(this.fade - 0.04F, 0.0F, 1.0F);
      }

   }

   public boolean isMouseOver(int pOffsetX, int pOffsetY, double pMouseX, double pMouseY) {
      return this.type.isMouseOver(pOffsetX, pOffsetY, this.index, pMouseX, pMouseY);
   }

   @Nullable
   public static AdvancementTab create(Minecraft pMinecraft, AdvancementsScreen pScreen, int pTabIndex, Advancement pAdvancement) {
      if (pAdvancement.getDisplay() == null) {
         return null;
      } else {
         for(AdvancementTabType advancementtabtype : AdvancementTabType.values()) {
            if ((pTabIndex % AdvancementTabType.MAX_TABS) < advancementtabtype.getMax()) {
               return new AdvancementTab(pMinecraft, pScreen, advancementtabtype, pTabIndex % AdvancementTabType.MAX_TABS, pTabIndex / AdvancementTabType.MAX_TABS, pAdvancement, pAdvancement.getDisplay());
            }

            pTabIndex -= advancementtabtype.getMax();
         }

         return null;
      }
   }

   public void scroll(double pDragX, double pDragY) {
      if (this.maxX - this.minX > 234) {
         this.scrollX = Mth.clamp(this.scrollX + pDragX, (double)(-(this.maxX - 234)), 0.0D);
      }

      if (this.maxY - this.minY > 113) {
         this.scrollY = Mth.clamp(this.scrollY + pDragY, (double)(-(this.maxY - 113)), 0.0D);
      }

   }

   public void addAdvancement(Advancement pAdvancement) {
      if (pAdvancement.getDisplay() != null) {
         AdvancementWidget advancementwidget = new AdvancementWidget(this, this.minecraft, pAdvancement, pAdvancement.getDisplay());
         this.addWidget(advancementwidget, pAdvancement);
      }
   }

   private void addWidget(AdvancementWidget pWidget, Advancement pAdvancement) {
      this.widgets.put(pAdvancement, pWidget);
      int i = pWidget.getX();
      int j = i + 28;
      int k = pWidget.getY();
      int l = k + 27;
      this.minX = Math.min(this.minX, i);
      this.maxX = Math.max(this.maxX, j);
      this.minY = Math.min(this.minY, k);
      this.maxY = Math.max(this.maxY, l);

      for(AdvancementWidget advancementwidget : this.widgets.values()) {
         advancementwidget.attachToParent();
      }

   }

   @Nullable
   public AdvancementWidget getWidget(Advancement pAdvancement) {
      return this.widgets.get(pAdvancement);
   }

   public AdvancementsScreen getScreen() {
      return this.screen;
   }
}
