package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.minecraft.client.model.SalmonModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Salmon;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SalmonRenderer extends MobRenderer<Salmon, SalmonModel<Salmon>> {
   private static final ResourceLocation SALMON_LOCATION = new ResourceLocation("textures/entity/fish/salmon.png");

   public SalmonRenderer(EntityRendererProvider.Context p_174364_) {
      super(p_174364_, new SalmonModel<>(p_174364_.bakeLayer(ModelLayers.SALMON)), 0.4F);
   }

   /**
    * Returns the location of an entity's texture.
    */
   public ResourceLocation getTextureLocation(Salmon pEntity) {
      return SALMON_LOCATION;
   }

   protected void setupRotations(Salmon pEntityLiving, PoseStack pMatrixStack, float pAgeInTicks, float pRotationYaw, float pPartialTicks) {
      super.setupRotations(pEntityLiving, pMatrixStack, pAgeInTicks, pRotationYaw, pPartialTicks);
      float f = 1.0F;
      float f1 = 1.0F;
      if (!pEntityLiving.isInWater()) {
         f = 1.3F;
         f1 = 1.7F;
      }

      float f2 = f * 4.3F * Mth.sin(f1 * 0.6F * pAgeInTicks);
      pMatrixStack.mulPose(Vector3f.YP.rotationDegrees(f2));
      pMatrixStack.translate(0.0D, 0.0D, (double)-0.4F);
      if (!pEntityLiving.isInWater()) {
         pMatrixStack.translate((double)0.2F, (double)0.1F, 0.0D);
         pMatrixStack.mulPose(Vector3f.ZP.rotationDegrees(90.0F));
      }

   }
}