package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.BatModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BatRenderer extends MobRenderer<Bat, BatModel> {
   private static final ResourceLocation BAT_LOCATION = new ResourceLocation("textures/entity/bat.png");

   public BatRenderer(EntityRendererProvider.Context p_173929_) {
      super(p_173929_, new BatModel(p_173929_.bakeLayer(ModelLayers.BAT)), 0.25F);
   }

   /**
    * Returns the location of an entity's texture.
    */
   public ResourceLocation getTextureLocation(Bat pEntity) {
      return BAT_LOCATION;
   }

   protected void scale(Bat pLivingEntity, PoseStack pMatrixStack, float pPartialTickTime) {
      pMatrixStack.scale(0.35F, 0.35F, 0.35F);
   }

   protected void setupRotations(Bat pEntityLiving, PoseStack pMatrixStack, float pAgeInTicks, float pRotationYaw, float pPartialTicks) {
      if (pEntityLiving.isResting()) {
         pMatrixStack.translate(0.0D, (double)-0.1F, 0.0D);
      } else {
         pMatrixStack.translate(0.0D, (double)(Mth.cos(pAgeInTicks * 0.3F) * 0.1F), 0.0D);
      }

      super.setupRotations(pEntityLiving, pMatrixStack, pAgeInTicks, pRotationYaw, pPartialTicks);
   }
}