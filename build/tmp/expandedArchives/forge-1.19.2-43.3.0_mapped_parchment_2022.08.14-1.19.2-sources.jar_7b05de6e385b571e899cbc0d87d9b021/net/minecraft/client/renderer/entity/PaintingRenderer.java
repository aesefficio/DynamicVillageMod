package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.PaintingTextureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PaintingRenderer extends EntityRenderer<Painting> {
   public PaintingRenderer(EntityRendererProvider.Context pContext) {
      super(pContext);
   }

   public void render(Painting pEntity, float pEntityYaw, float pPartialTicks, PoseStack pMatrixStack, MultiBufferSource pBuffer, int pPackedLight) {
      pMatrixStack.pushPose();
      pMatrixStack.mulPose(Vector3f.YP.rotationDegrees(180.0F - pEntityYaw));
      PaintingVariant paintingvariant = pEntity.getVariant().value();
      float f = 0.0625F;
      pMatrixStack.scale(0.0625F, 0.0625F, 0.0625F);
      VertexConsumer vertexconsumer = pBuffer.getBuffer(RenderType.entitySolid(this.getTextureLocation(pEntity)));
      PaintingTextureManager paintingtexturemanager = Minecraft.getInstance().getPaintingTextures();
      this.renderPainting(pMatrixStack, vertexconsumer, pEntity, paintingvariant.getWidth(), paintingvariant.getHeight(), paintingtexturemanager.get(paintingvariant), paintingtexturemanager.getBackSprite());
      pMatrixStack.popPose();
      super.render(pEntity, pEntityYaw, pPartialTicks, pMatrixStack, pBuffer, pPackedLight);
   }

   /**
    * Returns the location of an entity's texture.
    */
   public ResourceLocation getTextureLocation(Painting pEntity) {
      return Minecraft.getInstance().getPaintingTextures().getBackSprite().atlas().location();
   }

   private void renderPainting(PoseStack pPoseStack, VertexConsumer p_115560_, Painting pPainting, int p_115562_, int p_115563_, TextureAtlasSprite p_115564_, TextureAtlasSprite p_115565_) {
      PoseStack.Pose posestack$pose = pPoseStack.last();
      Matrix4f matrix4f = posestack$pose.pose();
      Matrix3f matrix3f = posestack$pose.normal();
      float f = (float)(-p_115562_) / 2.0F;
      float f1 = (float)(-p_115563_) / 2.0F;
      float f2 = 0.5F;
      float f3 = p_115565_.getU0();
      float f4 = p_115565_.getU1();
      float f5 = p_115565_.getV0();
      float f6 = p_115565_.getV1();
      float f7 = p_115565_.getU0();
      float f8 = p_115565_.getU1();
      float f9 = p_115565_.getV0();
      float f10 = p_115565_.getV(1.0D);
      float f11 = p_115565_.getU0();
      float f12 = p_115565_.getU(1.0D);
      float f13 = p_115565_.getV0();
      float f14 = p_115565_.getV1();
      int i = p_115562_ / 16;
      int j = p_115563_ / 16;
      double d0 = 16.0D / (double)i;
      double d1 = 16.0D / (double)j;

      for(int k = 0; k < i; ++k) {
         for(int l = 0; l < j; ++l) {
            float f15 = f + (float)((k + 1) * 16);
            float f16 = f + (float)(k * 16);
            float f17 = f1 + (float)((l + 1) * 16);
            float f18 = f1 + (float)(l * 16);
            int i1 = pPainting.getBlockX();
            int j1 = Mth.floor(pPainting.getY() + (double)((f17 + f18) / 2.0F / 16.0F));
            int k1 = pPainting.getBlockZ();
            Direction direction = pPainting.getDirection();
            if (direction == Direction.NORTH) {
               i1 = Mth.floor(pPainting.getX() + (double)((f15 + f16) / 2.0F / 16.0F));
            }

            if (direction == Direction.WEST) {
               k1 = Mth.floor(pPainting.getZ() - (double)((f15 + f16) / 2.0F / 16.0F));
            }

            if (direction == Direction.SOUTH) {
               i1 = Mth.floor(pPainting.getX() - (double)((f15 + f16) / 2.0F / 16.0F));
            }

            if (direction == Direction.EAST) {
               k1 = Mth.floor(pPainting.getZ() + (double)((f15 + f16) / 2.0F / 16.0F));
            }

            int l1 = LevelRenderer.getLightColor(pPainting.level, new BlockPos(i1, j1, k1));
            float f19 = p_115564_.getU(d0 * (double)(i - k));
            float f20 = p_115564_.getU(d0 * (double)(i - (k + 1)));
            float f21 = p_115564_.getV(d1 * (double)(j - l));
            float f22 = p_115564_.getV(d1 * (double)(j - (l + 1)));
            this.vertex(matrix4f, matrix3f, p_115560_, f15, f18, f20, f21, -0.5F, 0, 0, -1, l1);
            this.vertex(matrix4f, matrix3f, p_115560_, f16, f18, f19, f21, -0.5F, 0, 0, -1, l1);
            this.vertex(matrix4f, matrix3f, p_115560_, f16, f17, f19, f22, -0.5F, 0, 0, -1, l1);
            this.vertex(matrix4f, matrix3f, p_115560_, f15, f17, f20, f22, -0.5F, 0, 0, -1, l1);
            this.vertex(matrix4f, matrix3f, p_115560_, f15, f17, f4, f5, 0.5F, 0, 0, 1, l1);
            this.vertex(matrix4f, matrix3f, p_115560_, f16, f17, f3, f5, 0.5F, 0, 0, 1, l1);
            this.vertex(matrix4f, matrix3f, p_115560_, f16, f18, f3, f6, 0.5F, 0, 0, 1, l1);
            this.vertex(matrix4f, matrix3f, p_115560_, f15, f18, f4, f6, 0.5F, 0, 0, 1, l1);
            this.vertex(matrix4f, matrix3f, p_115560_, f15, f17, f7, f9, -0.5F, 0, 1, 0, l1);
            this.vertex(matrix4f, matrix3f, p_115560_, f16, f17, f8, f9, -0.5F, 0, 1, 0, l1);
            this.vertex(matrix4f, matrix3f, p_115560_, f16, f17, f8, f10, 0.5F, 0, 1, 0, l1);
            this.vertex(matrix4f, matrix3f, p_115560_, f15, f17, f7, f10, 0.5F, 0, 1, 0, l1);
            this.vertex(matrix4f, matrix3f, p_115560_, f15, f18, f7, f9, 0.5F, 0, -1, 0, l1);
            this.vertex(matrix4f, matrix3f, p_115560_, f16, f18, f8, f9, 0.5F, 0, -1, 0, l1);
            this.vertex(matrix4f, matrix3f, p_115560_, f16, f18, f8, f10, -0.5F, 0, -1, 0, l1);
            this.vertex(matrix4f, matrix3f, p_115560_, f15, f18, f7, f10, -0.5F, 0, -1, 0, l1);
            this.vertex(matrix4f, matrix3f, p_115560_, f15, f17, f12, f13, 0.5F, -1, 0, 0, l1);
            this.vertex(matrix4f, matrix3f, p_115560_, f15, f18, f12, f14, 0.5F, -1, 0, 0, l1);
            this.vertex(matrix4f, matrix3f, p_115560_, f15, f18, f11, f14, -0.5F, -1, 0, 0, l1);
            this.vertex(matrix4f, matrix3f, p_115560_, f15, f17, f11, f13, -0.5F, -1, 0, 0, l1);
            this.vertex(matrix4f, matrix3f, p_115560_, f16, f17, f12, f13, -0.5F, 1, 0, 0, l1);
            this.vertex(matrix4f, matrix3f, p_115560_, f16, f18, f12, f14, -0.5F, 1, 0, 0, l1);
            this.vertex(matrix4f, matrix3f, p_115560_, f16, f18, f11, f14, 0.5F, 1, 0, 0, l1);
            this.vertex(matrix4f, matrix3f, p_115560_, f16, f17, f11, f13, 0.5F, 1, 0, 0, l1);
         }
      }

   }

   private void vertex(Matrix4f p_115537_, Matrix3f p_115538_, VertexConsumer p_115539_, float p_115540_, float p_115541_, float p_115542_, float p_115543_, float p_115544_, int p_115545_, int p_115546_, int p_115547_, int p_115548_) {
      p_115539_.vertex(p_115537_, p_115540_, p_115541_, p_115544_).color(255, 255, 255, 255).uv(p_115542_, p_115543_).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(p_115548_).normal(p_115538_, (float)p_115545_, (float)p_115546_, (float)p_115547_).endVertex();
   }
}