package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class Deadmau5EarsLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
   public Deadmau5EarsLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> pRenderer) {
      super(pRenderer);
   }

   public void render(PoseStack pMatrixStack, MultiBufferSource pBuffer, int pPackedLight, AbstractClientPlayer pLivingEntity, float pLimbSwing, float pLimbSwingAmount, float pPartialTicks, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch) {
      if ("deadmau5".equals(pLivingEntity.getName().getString()) && pLivingEntity.isSkinLoaded() && !pLivingEntity.isInvisible()) {
         VertexConsumer vertexconsumer = pBuffer.getBuffer(RenderType.entitySolid(pLivingEntity.getSkinTextureLocation()));
         int i = LivingEntityRenderer.getOverlayCoords(pLivingEntity, 0.0F);

         for(int j = 0; j < 2; ++j) {
            float f = Mth.lerp(pPartialTicks, pLivingEntity.yRotO, pLivingEntity.getYRot()) - Mth.lerp(pPartialTicks, pLivingEntity.yBodyRotO, pLivingEntity.yBodyRot);
            float f1 = Mth.lerp(pPartialTicks, pLivingEntity.xRotO, pLivingEntity.getXRot());
            pMatrixStack.pushPose();
            pMatrixStack.mulPose(Vector3f.YP.rotationDegrees(f));
            pMatrixStack.mulPose(Vector3f.XP.rotationDegrees(f1));
            pMatrixStack.translate((double)(0.375F * (float)(j * 2 - 1)), 0.0D, 0.0D);
            pMatrixStack.translate(0.0D, -0.375D, 0.0D);
            pMatrixStack.mulPose(Vector3f.XP.rotationDegrees(-f1));
            pMatrixStack.mulPose(Vector3f.YP.rotationDegrees(-f));
            float f2 = 1.3333334F;
            pMatrixStack.scale(1.3333334F, 1.3333334F, 1.3333334F);
            this.getParentModel().renderEars(pMatrixStack, vertexconsumer, pPackedLight, i);
            pMatrixStack.popPose();
         }

      }
   }
}