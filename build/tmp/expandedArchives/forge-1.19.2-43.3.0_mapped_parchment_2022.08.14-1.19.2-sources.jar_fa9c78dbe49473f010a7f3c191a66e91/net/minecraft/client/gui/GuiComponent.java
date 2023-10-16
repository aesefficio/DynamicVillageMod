package net.minecraft.client.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;
import java.util.function.BiConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class GuiComponent {
   public static final ResourceLocation BACKGROUND_LOCATION = new ResourceLocation("textures/gui/options_background.png");
   public static final ResourceLocation STATS_ICON_LOCATION = new ResourceLocation("textures/gui/container/stats_icons.png");
   public static final ResourceLocation GUI_ICONS_LOCATION = new ResourceLocation("textures/gui/icons.png");
   private int blitOffset;

   protected void hLine(PoseStack pPoseStack, int pMinX, int pMaxX, int pY, int pColor) {
      if (pMaxX < pMinX) {
         int i = pMinX;
         pMinX = pMaxX;
         pMaxX = i;
      }

      fill(pPoseStack, pMinX, pY, pMaxX + 1, pY + 1, pColor);
   }

   protected void vLine(PoseStack pPoseStack, int pX, int pMinY, int pMaxY, int pColor) {
      if (pMaxY < pMinY) {
         int i = pMinY;
         pMinY = pMaxY;
         pMaxY = i;
      }

      fill(pPoseStack, pX, pMinY + 1, pX + 1, pMaxY, pColor);
   }

   public static void enableScissor(int p_239261_, int p_239262_, int p_239263_, int p_239264_) {
      Window window = Minecraft.getInstance().getWindow();
      int i = window.getHeight();
      double d0 = window.getGuiScale();
      double d1 = (double)p_239261_ * d0;
      double d2 = (double)i - (double)p_239264_ * d0;
      double d3 = (double)(p_239263_ - p_239261_) * d0;
      double d4 = (double)(p_239264_ - p_239262_) * d0;
      RenderSystem.enableScissor((int)d1, (int)d2, Math.max(0, (int)d3), Math.max(0, (int)d4));
   }

   public static void disableScissor() {
      RenderSystem.disableScissor();
   }

   public static void fill(PoseStack pPoseStack, int pMinX, int pMinY, int pMaxX, int pMaxY, int pColor) {
      innerFill(pPoseStack.last().pose(), pMinX, pMinY, pMaxX, pMaxY, pColor);
   }

   private static void innerFill(Matrix4f pMatrix, int pMinX, int pMinY, int pMaxX, int pMaxY, int pColor) {
      if (pMinX < pMaxX) {
         int i = pMinX;
         pMinX = pMaxX;
         pMaxX = i;
      }

      if (pMinY < pMaxY) {
         int j = pMinY;
         pMinY = pMaxY;
         pMaxY = j;
      }

      float f3 = (float)(pColor >> 24 & 255) / 255.0F;
      float f = (float)(pColor >> 16 & 255) / 255.0F;
      float f1 = (float)(pColor >> 8 & 255) / 255.0F;
      float f2 = (float)(pColor & 255) / 255.0F;
      BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
      RenderSystem.enableBlend();
      RenderSystem.disableTexture();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      bufferbuilder.vertex(pMatrix, (float)pMinX, (float)pMaxY, 0.0F).color(f, f1, f2, f3).endVertex();
      bufferbuilder.vertex(pMatrix, (float)pMaxX, (float)pMaxY, 0.0F).color(f, f1, f2, f3).endVertex();
      bufferbuilder.vertex(pMatrix, (float)pMaxX, (float)pMinY, 0.0F).color(f, f1, f2, f3).endVertex();
      bufferbuilder.vertex(pMatrix, (float)pMinX, (float)pMinY, 0.0F).color(f, f1, f2, f3).endVertex();
      BufferUploader.drawWithShader(bufferbuilder.end());
      RenderSystem.enableTexture();
      RenderSystem.disableBlend();
   }

   protected void fillGradient(PoseStack pPoseStack, int pX1, int pY1, int pX2, int pY2, int pColorFrom, int pColorTo) {
      fillGradient(pPoseStack, pX1, pY1, pX2, pY2, pColorFrom, pColorTo, this.blitOffset);
   }

   protected static void fillGradient(PoseStack pPoseStack, int pX1, int pY1, int pX2, int pY2, int pColorFrom, int pColorTo, int pBlitOffset) {
      RenderSystem.disableTexture();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      Tesselator tesselator = Tesselator.getInstance();
      BufferBuilder bufferbuilder = tesselator.getBuilder();
      bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      fillGradient(pPoseStack.last().pose(), bufferbuilder, pX1, pY1, pX2, pY2, pBlitOffset, pColorFrom, pColorTo);
      tesselator.end();
      RenderSystem.disableBlend();
      RenderSystem.enableTexture();
   }

   protected static void fillGradient(Matrix4f pMatrix, BufferBuilder pBuilder, int pX1, int pY1, int pX2, int pY2, int pBlitOffset, int pColorA, int pColorB) {
      float f = (float)(pColorA >> 24 & 255) / 255.0F;
      float f1 = (float)(pColorA >> 16 & 255) / 255.0F;
      float f2 = (float)(pColorA >> 8 & 255) / 255.0F;
      float f3 = (float)(pColorA & 255) / 255.0F;
      float f4 = (float)(pColorB >> 24 & 255) / 255.0F;
      float f5 = (float)(pColorB >> 16 & 255) / 255.0F;
      float f6 = (float)(pColorB >> 8 & 255) / 255.0F;
      float f7 = (float)(pColorB & 255) / 255.0F;
      pBuilder.vertex(pMatrix, (float)pX2, (float)pY1, (float)pBlitOffset).color(f1, f2, f3, f).endVertex();
      pBuilder.vertex(pMatrix, (float)pX1, (float)pY1, (float)pBlitOffset).color(f1, f2, f3, f).endVertex();
      pBuilder.vertex(pMatrix, (float)pX1, (float)pY2, (float)pBlitOffset).color(f5, f6, f7, f4).endVertex();
      pBuilder.vertex(pMatrix, (float)pX2, (float)pY2, (float)pBlitOffset).color(f5, f6, f7, f4).endVertex();
   }

   public static void drawCenteredString(PoseStack pPoseStack, Font pFont, String pText, int pX, int pY, int pColor) {
      pFont.drawShadow(pPoseStack, pText, (float)(pX - pFont.width(pText) / 2), (float)pY, pColor);
   }

   public static void drawCenteredString(PoseStack pPoseStack, Font pFont, Component pText, int pX, int pY, int pColor) {
      FormattedCharSequence formattedcharsequence = pText.getVisualOrderText();
      pFont.drawShadow(pPoseStack, formattedcharsequence, (float)(pX - pFont.width(formattedcharsequence) / 2), (float)pY, pColor);
   }

   public static void drawCenteredString(PoseStack pPoseStack, Font pFont, FormattedCharSequence pText, int pX, int pY, int pColor) {
      pFont.drawShadow(pPoseStack, pText, (float)(pX - pFont.width(pText) / 2), (float)pY, pColor);
   }

   public static void drawString(PoseStack pPoseStack, Font pFont, String pText, int pX, int pY, int pColor) {
      pFont.drawShadow(pPoseStack, pText, (float)pX, (float)pY, pColor);
   }

   public static void drawString(PoseStack pPoseStack, Font pFont, FormattedCharSequence pText, int pX, int pY, int pColor) {
      pFont.drawShadow(pPoseStack, pText, (float)pX, (float)pY, pColor);
   }

   public static void drawString(PoseStack pPoseStack, Font pFont, Component pText, int pX, int pY, int pColor) {
      pFont.drawShadow(pPoseStack, pText, (float)pX, (float)pY, pColor);
   }

   public void blitOutlineBlack(int pWidth, int pHeight, BiConsumer<Integer, Integer> pBoxXYConsumer) {
      RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
      pBoxXYConsumer.accept(pWidth + 1, pHeight);
      pBoxXYConsumer.accept(pWidth - 1, pHeight);
      pBoxXYConsumer.accept(pWidth, pHeight + 1);
      pBoxXYConsumer.accept(pWidth, pHeight - 1);
      RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
      pBoxXYConsumer.accept(pWidth, pHeight);
   }

   public static void blit(PoseStack pPoseStack, int pX, int pY, int pBlitOffset, int pWidth, int pHeight, TextureAtlasSprite pSprite) {
      innerBlit(pPoseStack.last().pose(), pX, pX + pWidth, pY, pY + pHeight, pBlitOffset, pSprite.getU0(), pSprite.getU1(), pSprite.getV0(), pSprite.getV1());
   }

   public void blit(PoseStack pPoseStack, int pX, int pY, int pUOffset, int pVOffset, int pUWidth, int pVHeight) {
      blit(pPoseStack, pX, pY, this.blitOffset, (float)pUOffset, (float)pVOffset, pUWidth, pVHeight, 256, 256);
   }

   public static void blit(PoseStack pPoseStack, int pX, int pY, int pBlitOffset, float pUOffset, float pVOffset, int pUWidth, int pVHeight, int pTextureHeight, int pTextureWidth) {
      innerBlit(pPoseStack, pX, pX + pUWidth, pY, pY + pVHeight, pBlitOffset, pUWidth, pVHeight, pUOffset, pVOffset, pTextureHeight, pTextureWidth);
   }

   public static void blit(PoseStack pPoseStack, int pX, int pY, int pWidth, int pHeight, float pUOffset, float pVOffset, int pUWidth, int pVHeight, int pTextureWidth, int pTextureHeight) {
      innerBlit(pPoseStack, pX, pX + pWidth, pY, pY + pHeight, 0, pUWidth, pVHeight, pUOffset, pVOffset, pTextureWidth, pTextureHeight);
   }

   public static void blit(PoseStack pPoseStack, int pX, int pY, float pUOffset, float pVOffset, int pWidth, int pHeight, int pTextureWidth, int pTextureHeight) {
      blit(pPoseStack, pX, pY, pWidth, pHeight, pUOffset, pVOffset, pWidth, pHeight, pTextureWidth, pTextureHeight);
   }

   private static void innerBlit(PoseStack pPoseStack, int pX1, int pX2, int pY1, int pY2, int pBlitOffset, int pUWidth, int pVHeight, float pUOffset, float pVOffset, int pTextureWidth, int pTextureHeight) {
      innerBlit(pPoseStack.last().pose(), pX1, pX2, pY1, pY2, pBlitOffset, (pUOffset + 0.0F) / (float)pTextureWidth, (pUOffset + (float)pUWidth) / (float)pTextureWidth, (pVOffset + 0.0F) / (float)pTextureHeight, (pVOffset + (float)pVHeight) / (float)pTextureHeight);
   }

   private static void innerBlit(Matrix4f pMatrix, int pX1, int pX2, int pY1, int pY2, int pBlitOffset, float pMinU, float pMaxU, float pMinV, float pMaxV) {
      RenderSystem.setShader(GameRenderer::getPositionTexShader);
      BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
      bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
      bufferbuilder.vertex(pMatrix, (float)pX1, (float)pY2, (float)pBlitOffset).uv(pMinU, pMaxV).endVertex();
      bufferbuilder.vertex(pMatrix, (float)pX2, (float)pY2, (float)pBlitOffset).uv(pMaxU, pMaxV).endVertex();
      bufferbuilder.vertex(pMatrix, (float)pX2, (float)pY1, (float)pBlitOffset).uv(pMaxU, pMinV).endVertex();
      bufferbuilder.vertex(pMatrix, (float)pX1, (float)pY1, (float)pBlitOffset).uv(pMinU, pMinV).endVertex();
      BufferUploader.drawWithShader(bufferbuilder.end());
   }

   public int getBlitOffset() {
      return this.blitOffset;
   }

   public void setBlitOffset(int pBlitOffset) {
      this.blitOffset = pBlitOffset;
   }
}