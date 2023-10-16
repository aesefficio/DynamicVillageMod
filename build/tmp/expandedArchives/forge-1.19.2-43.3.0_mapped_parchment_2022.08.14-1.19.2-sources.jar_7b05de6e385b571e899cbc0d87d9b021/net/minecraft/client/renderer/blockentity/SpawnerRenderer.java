package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SpawnerRenderer implements BlockEntityRenderer<SpawnerBlockEntity> {
   private final EntityRenderDispatcher entityRenderer;

   public SpawnerRenderer(BlockEntityRendererProvider.Context pContext) {
      this.entityRenderer = pContext.getEntityRenderer();
   }

   public void render(SpawnerBlockEntity pBlockEntity, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pPackedOverlay) {
      pPoseStack.pushPose();
      pPoseStack.translate(0.5D, 0.0D, 0.5D);
      BaseSpawner basespawner = pBlockEntity.getSpawner();
      Entity entity = basespawner.getOrCreateDisplayEntity(pBlockEntity.getLevel());
      if (entity != null) {
         float f = 0.53125F;
         float f1 = Math.max(entity.getBbWidth(), entity.getBbHeight());
         if ((double)f1 > 1.0D) {
            f /= f1;
         }

         pPoseStack.translate(0.0D, (double)0.4F, 0.0D);
         pPoseStack.mulPose(Vector3f.YP.rotationDegrees((float)Mth.lerp((double)pPartialTick, basespawner.getoSpin(), basespawner.getSpin()) * 10.0F));
         pPoseStack.translate(0.0D, (double)-0.2F, 0.0D);
         pPoseStack.mulPose(Vector3f.XP.rotationDegrees(-30.0F));
         pPoseStack.scale(f, f, f);
         this.entityRenderer.render(entity, 0.0D, 0.0D, 0.0D, 0.0F, pPartialTick, pPoseStack, pBufferSource, pPackedLight);
      }

      pPoseStack.popPose();
   }
}