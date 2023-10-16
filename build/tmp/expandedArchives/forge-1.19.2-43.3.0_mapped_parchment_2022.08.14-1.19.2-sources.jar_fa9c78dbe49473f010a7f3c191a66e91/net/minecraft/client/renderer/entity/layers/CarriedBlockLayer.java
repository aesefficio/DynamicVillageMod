package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.minecraft.client.model.EndermanModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CarriedBlockLayer extends RenderLayer<EnderMan, EndermanModel<EnderMan>> {
   private final BlockRenderDispatcher blockRenderer;

   public CarriedBlockLayer(RenderLayerParent<EnderMan, EndermanModel<EnderMan>> p_234814_, BlockRenderDispatcher p_234815_) {
      super(p_234814_);
      this.blockRenderer = p_234815_;
   }

   public void render(PoseStack pMatrixStack, MultiBufferSource pBuffer, int pPackedLight, EnderMan pLivingEntity, float pLimbSwing, float pLimbSwingAmount, float pPartialTicks, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch) {
      BlockState blockstate = pLivingEntity.getCarriedBlock();
      if (blockstate != null) {
         pMatrixStack.pushPose();
         pMatrixStack.translate(0.0D, 0.6875D, -0.75D);
         pMatrixStack.mulPose(Vector3f.XP.rotationDegrees(20.0F));
         pMatrixStack.mulPose(Vector3f.YP.rotationDegrees(45.0F));
         pMatrixStack.translate(0.25D, 0.1875D, 0.25D);
         float f = 0.5F;
         pMatrixStack.scale(-0.5F, -0.5F, 0.5F);
         pMatrixStack.mulPose(Vector3f.YP.rotationDegrees(90.0F));
         this.blockRenderer.renderSingleBlock(blockstate, pMatrixStack, pBuffer, pPackedLight, OverlayTexture.NO_OVERLAY);
         pMatrixStack.popPose();
      }
   }
}