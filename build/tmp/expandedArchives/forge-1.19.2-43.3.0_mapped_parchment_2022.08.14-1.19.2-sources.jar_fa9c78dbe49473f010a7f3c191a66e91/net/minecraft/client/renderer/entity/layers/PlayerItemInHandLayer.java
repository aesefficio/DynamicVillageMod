package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PlayerItemInHandLayer<T extends Player, M extends EntityModel<T> & ArmedModel & HeadedModel> extends ItemInHandLayer<T, M> {
   private final ItemInHandRenderer itemInHandRenderer;
   private static final float X_ROT_MIN = (-(float)Math.PI / 6F);
   private static final float X_ROT_MAX = ((float)Math.PI / 2F);

   public PlayerItemInHandLayer(RenderLayerParent<T, M> p_234866_, ItemInHandRenderer p_234867_) {
      super(p_234866_, p_234867_);
      this.itemInHandRenderer = p_234867_;
   }

   protected void renderArmWithItem(LivingEntity pLivingEntity, ItemStack pItemStack, ItemTransforms.TransformType pTransformType, HumanoidArm pArm, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight) {
      if (pItemStack.is(Items.SPYGLASS) && pLivingEntity.getUseItem() == pItemStack && pLivingEntity.swingTime == 0) {
         this.renderArmWithSpyglass(pLivingEntity, pItemStack, pArm, pPoseStack, pBuffer, pPackedLight);
      } else {
         super.renderArmWithItem(pLivingEntity, pItemStack, pTransformType, pArm, pPoseStack, pBuffer, pPackedLight);
      }

   }

   private void renderArmWithSpyglass(LivingEntity p_174518_, ItemStack p_174519_, HumanoidArm p_174520_, PoseStack pPoseStack, MultiBufferSource pBuffer, int p_174523_) {
      pPoseStack.pushPose();
      ModelPart modelpart = this.getParentModel().getHead();
      float f = modelpart.xRot;
      modelpart.xRot = Mth.clamp(modelpart.xRot, (-(float)Math.PI / 6F), ((float)Math.PI / 2F));
      modelpart.translateAndRotate(pPoseStack);
      modelpart.xRot = f;
      CustomHeadLayer.translateToHead(pPoseStack, false);
      boolean flag = p_174520_ == HumanoidArm.LEFT;
      pPoseStack.translate((double)((flag ? -2.5F : 2.5F) / 16.0F), -0.0625D, 0.0D);
      this.itemInHandRenderer.renderItem(p_174518_, p_174519_, ItemTransforms.TransformType.HEAD, false, pPoseStack, pBuffer, p_174523_);
      pPoseStack.popPose();
   }
}