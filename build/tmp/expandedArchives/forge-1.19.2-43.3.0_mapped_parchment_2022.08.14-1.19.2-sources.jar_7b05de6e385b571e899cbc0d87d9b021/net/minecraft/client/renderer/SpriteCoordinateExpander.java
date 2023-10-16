package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SpriteCoordinateExpander implements VertexConsumer {
   private final VertexConsumer delegate;
   private final TextureAtlasSprite sprite;

   public SpriteCoordinateExpander(VertexConsumer pDelegate, TextureAtlasSprite pSprite) {
      this.delegate = pDelegate;
      this.sprite = pSprite;
   }

   public VertexConsumer vertex(double pX, double pY, double pZ) {
      return this.delegate.vertex(pX, pY, pZ);
   }

   public VertexConsumer color(int pRed, int pGreen, int pBlue, int pAlpha) {
      return this.delegate.color(pRed, pGreen, pBlue, pAlpha);
   }

   public VertexConsumer uv(float pU, float pV) {
      return this.delegate.uv(this.sprite.getU((double)(pU * 16.0F)), this.sprite.getV((double)(pV * 16.0F)));
   }

   public VertexConsumer overlayCoords(int pU, int pV) {
      return this.delegate.overlayCoords(pU, pV);
   }

   public VertexConsumer uv2(int pU, int pV) {
      return this.delegate.uv2(pU, pV);
   }

   public VertexConsumer normal(float pX, float pY, float pZ) {
      return this.delegate.normal(pX, pY, pZ);
   }

   public void endVertex() {
      this.delegate.endVertex();
   }

   public void defaultColor(int pDefaultR, int pDefaultG, int pDefaultB, int pDefaultA) {
      this.delegate.defaultColor(pDefaultR, pDefaultG, pDefaultB, pDefaultA);
   }

   public void unsetDefaultColor() {
      this.delegate.unsetDefaultColor();
   }

   public void vertex(float pX, float pY, float pZ, float pRed, float pGreen, float pBlue, float pAlpha, float pTexU, float pTexV, int pOverlayUV, int pLightmapUV, float pNormalX, float pNormalY, float pNormalZ) {
      this.delegate.vertex(pX, pY, pZ, pRed, pGreen, pBlue, pAlpha, this.sprite.getU((double)(pTexU * 16.0F)), this.sprite.getV((double)(pTexV * 16.0F)), pOverlayUV, pLightmapUV, pNormalX, pNormalY, pNormalZ);
   }
}