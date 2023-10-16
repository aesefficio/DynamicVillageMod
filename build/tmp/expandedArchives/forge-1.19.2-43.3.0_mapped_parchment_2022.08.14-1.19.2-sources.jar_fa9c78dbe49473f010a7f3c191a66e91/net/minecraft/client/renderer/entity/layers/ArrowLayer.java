package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ArrowLayer<T extends LivingEntity, M extends PlayerModel<T>> extends StuckInBodyLayer<T, M> {
   private final EntityRenderDispatcher dispatcher;

   public ArrowLayer(EntityRendererProvider.Context pContext, LivingEntityRenderer<T, M> pRenderer) {
      super(pRenderer);
      this.dispatcher = pContext.getEntityRenderDispatcher();
   }

   protected int numStuck(T pEntity) {
      return pEntity.getArrowCount();
   }

   protected void renderStuckItem(PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, Entity pEntity, float p_116573_, float pPartialTick, float pAgeInTicks, float p_116576_) {
      float f = Mth.sqrt(p_116573_ * p_116573_ + pAgeInTicks * pAgeInTicks);
      Arrow arrow = new Arrow(pEntity.level, pEntity.getX(), pEntity.getY(), pEntity.getZ());
      arrow.setYRot((float)(Math.atan2((double)p_116573_, (double)pAgeInTicks) * (double)(180F / (float)Math.PI)));
      arrow.setXRot((float)(Math.atan2((double)pPartialTick, (double)f) * (double)(180F / (float)Math.PI)));
      arrow.yRotO = arrow.getYRot();
      arrow.xRotO = arrow.getXRot();
      this.dispatcher.render(arrow, 0.0D, 0.0D, 0.0D, 0.0F, p_116576_, pPoseStack, pBuffer, pPackedLight);
   }
}