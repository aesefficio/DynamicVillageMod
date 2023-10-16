package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PandaModel;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Panda;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PandaHoldsItemLayer extends RenderLayer<Panda, PandaModel<Panda>> {
   private final ItemInHandRenderer itemInHandRenderer;

   public PandaHoldsItemLayer(RenderLayerParent<Panda, PandaModel<Panda>> p_234862_, ItemInHandRenderer p_234863_) {
      super(p_234862_);
      this.itemInHandRenderer = p_234863_;
   }

   public void render(PoseStack pMatrixStack, MultiBufferSource pBuffer, int pPackedLight, Panda pLivingEntity, float pLimbSwing, float pLimbSwingAmount, float pPartialTicks, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch) {
      ItemStack itemstack = pLivingEntity.getItemBySlot(EquipmentSlot.MAINHAND);
      if (pLivingEntity.isSitting() && !pLivingEntity.isScared()) {
         float f = -0.6F;
         float f1 = 1.4F;
         if (pLivingEntity.isEating()) {
            f -= 0.2F * Mth.sin(pAgeInTicks * 0.6F) + 0.2F;
            f1 -= 0.09F * Mth.sin(pAgeInTicks * 0.6F);
         }

         pMatrixStack.pushPose();
         pMatrixStack.translate((double)0.1F, (double)f1, (double)f);
         this.itemInHandRenderer.renderItem(pLivingEntity, itemstack, ItemTransforms.TransformType.GROUND, false, pMatrixStack, pBuffer, pPackedLight);
         pMatrixStack.popPose();
      }
   }
}