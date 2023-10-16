package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.minecraft.client.model.WitchModel;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WitchItemLayer<T extends LivingEntity> extends CrossedArmsItemLayer<T, WitchModel<T>> {
   public WitchItemLayer(RenderLayerParent<T, WitchModel<T>> p_234926_, ItemInHandRenderer p_234927_) {
      super(p_234926_, p_234927_);
   }

   public void render(PoseStack pMatrixStack, MultiBufferSource pBuffer, int pPackedLight, T pLivingEntity, float pLimbSwing, float pLimbSwingAmount, float pPartialTicks, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch) {
      ItemStack itemstack = pLivingEntity.getMainHandItem();
      pMatrixStack.pushPose();
      if (itemstack.is(Items.POTION)) {
         this.getParentModel().getHead().translateAndRotate(pMatrixStack);
         this.getParentModel().getNose().translateAndRotate(pMatrixStack);
         pMatrixStack.translate(0.0625D, 0.25D, 0.0D);
         pMatrixStack.mulPose(Vector3f.ZP.rotationDegrees(180.0F));
         pMatrixStack.mulPose(Vector3f.XP.rotationDegrees(140.0F));
         pMatrixStack.mulPose(Vector3f.ZP.rotationDegrees(10.0F));
         pMatrixStack.translate(0.0D, (double)-0.4F, (double)0.4F);
      }

      super.render(pMatrixStack, pBuffer, pPackedLight, pLivingEntity, pLimbSwing, pLimbSwingAmount, pPartialTicks, pAgeInTicks, pNetHeadYaw, pHeadPitch);
      pMatrixStack.popPose();
   }
}