package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.minecraft.client.model.SquidModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Squid;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SquidRenderer<T extends Squid> extends MobRenderer<T, SquidModel<T>> {
   private static final ResourceLocation SQUID_LOCATION = new ResourceLocation("textures/entity/squid/squid.png");

   public SquidRenderer(EntityRendererProvider.Context pContext, SquidModel<T> pModel) {
      super(pContext, pModel, 0.7F);
   }

   /**
    * Returns the location of an entity's texture.
    */
   public ResourceLocation getTextureLocation(T pEntity) {
      return SQUID_LOCATION;
   }

   protected void setupRotations(T pEntityLiving, PoseStack pMatrixStack, float pAgeInTicks, float pRotationYaw, float pPartialTicks) {
      float f = Mth.lerp(pPartialTicks, pEntityLiving.xBodyRotO, pEntityLiving.xBodyRot);
      float f1 = Mth.lerp(pPartialTicks, pEntityLiving.zBodyRotO, pEntityLiving.zBodyRot);
      pMatrixStack.translate(0.0D, 0.5D, 0.0D);
      pMatrixStack.mulPose(Vector3f.YP.rotationDegrees(180.0F - pRotationYaw));
      pMatrixStack.mulPose(Vector3f.XP.rotationDegrees(f));
      pMatrixStack.mulPose(Vector3f.YP.rotationDegrees(f1));
      pMatrixStack.translate(0.0D, (double)-1.2F, 0.0D);
   }

   /**
    * Defines what float the third param in setRotationAngles of ModelBase is
    */
   protected float getBob(T pLivingBase, float pPartialTicks) {
      return Mth.lerp(pPartialTicks, pLivingBase.oldTentacleAngle, pLivingBase.tentacleAngle);
   }
}