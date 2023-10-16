package net.minecraft.client.renderer.debug;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GameTestDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
   private static final float PADDING = 0.02F;
   private final Map<BlockPos, GameTestDebugRenderer.Marker> markers = Maps.newHashMap();

   /**
    * 
    * @param pRemoveAfter how long after the current time to remove this marker, in milliseconds
    */
   public void addMarker(BlockPos pPos, int pColor, String pText, int pRemoveAfter) {
      this.markers.put(pPos, new GameTestDebugRenderer.Marker(pColor, pText, Util.getMillis() + (long)pRemoveAfter));
   }

   public void clear() {
      this.markers.clear();
   }

   public void render(PoseStack pPoseStack, MultiBufferSource pBufferSource, double pCamX, double pCamY, double pCamZ) {
      long i = Util.getMillis();
      this.markers.entrySet().removeIf((p_113517_) -> {
         return i > (p_113517_.getValue()).removeAtTime;
      });
      this.markers.forEach(this::renderMarker);
   }

   private void renderMarker(BlockPos p_113530_, GameTestDebugRenderer.Marker p_113531_) {
      RenderSystem.enableBlend();
      RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
      RenderSystem.setShaderColor(0.0F, 1.0F, 0.0F, 0.75F);
      RenderSystem.disableTexture();
      DebugRenderer.renderFilledBox(p_113530_, 0.02F, p_113531_.getR(), p_113531_.getG(), p_113531_.getB(), p_113531_.getA());
      if (!p_113531_.text.isEmpty()) {
         double d0 = (double)p_113530_.getX() + 0.5D;
         double d1 = (double)p_113530_.getY() + 1.2D;
         double d2 = (double)p_113530_.getZ() + 0.5D;
         DebugRenderer.renderFloatingText(p_113531_.text, d0, d1, d2, -1, 0.01F, true, 0.0F, true);
      }

      RenderSystem.enableTexture();
      RenderSystem.disableBlend();
   }

   @OnlyIn(Dist.CLIENT)
   static class Marker {
      public int color;
      public String text;
      public long removeAtTime;

      public Marker(int pColor, String pText, long pRemoveAtTime) {
         this.color = pColor;
         this.text = pText;
         this.removeAtTime = pRemoveAtTime;
      }

      public float getR() {
         return (float)(this.color >> 16 & 255) / 255.0F;
      }

      public float getG() {
         return (float)(this.color >> 8 & 255) / 255.0F;
      }

      public float getB() {
         return (float)(this.color & 255) / 255.0F;
      }

      public float getA() {
         return (float)(this.color >> 24 & 255) / 255.0F;
      }
   }
}