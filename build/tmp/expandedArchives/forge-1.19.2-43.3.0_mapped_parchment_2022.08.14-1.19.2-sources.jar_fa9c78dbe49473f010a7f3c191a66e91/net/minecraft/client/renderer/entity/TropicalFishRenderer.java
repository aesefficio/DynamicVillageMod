package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.minecraft.client.model.ColorableHierarchicalModel;
import net.minecraft.client.model.TropicalFishModelA;
import net.minecraft.client.model.TropicalFishModelB;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.TropicalFishPatternLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.TropicalFish;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TropicalFishRenderer extends MobRenderer<TropicalFish, ColorableHierarchicalModel<TropicalFish>> {
   /** Breaking recompile intentionally since modelA/B incorrectly mapped. */
   private final ColorableHierarchicalModel<TropicalFish> modelA = this.getModel();
   /** Breaking recompile intentionally since modelA/B incorrectly mapped. */
   private final ColorableHierarchicalModel<TropicalFish> modelB;

   public TropicalFishRenderer(EntityRendererProvider.Context p_174428_) {
      super(p_174428_, new TropicalFishModelA<>(p_174428_.bakeLayer(ModelLayers.TROPICAL_FISH_SMALL)), 0.15F);
      this.modelB = new TropicalFishModelB<>(p_174428_.bakeLayer(ModelLayers.TROPICAL_FISH_LARGE));
      this.addLayer(new TropicalFishPatternLayer(this, p_174428_.getModelSet()));
   }

   /**
    * Returns the location of an entity's texture.
    */
   public ResourceLocation getTextureLocation(TropicalFish pEntity) {
      return pEntity.getBaseTextureLocation();
   }

   public void render(TropicalFish pEntity, float pEntityYaw, float pPartialTicks, PoseStack pMatrixStack, MultiBufferSource pBuffer, int pPackedLight) {
      ColorableHierarchicalModel<TropicalFish> colorablehierarchicalmodel = pEntity.getBaseVariant() == 0 ? this.modelA : this.modelB;
      this.model = colorablehierarchicalmodel;
      float[] afloat = pEntity.getBaseColor();
      colorablehierarchicalmodel.setColor(afloat[0], afloat[1], afloat[2]);
      super.render(pEntity, pEntityYaw, pPartialTicks, pMatrixStack, pBuffer, pPackedLight);
      colorablehierarchicalmodel.setColor(1.0F, 1.0F, 1.0F);
   }

   protected void setupRotations(TropicalFish pEntityLiving, PoseStack pMatrixStack, float pAgeInTicks, float pRotationYaw, float pPartialTicks) {
      super.setupRotations(pEntityLiving, pMatrixStack, pAgeInTicks, pRotationYaw, pPartialTicks);
      float f = 4.3F * Mth.sin(0.6F * pAgeInTicks);
      pMatrixStack.mulPose(Vector3f.YP.rotationDegrees(f));
      if (!pEntityLiving.isInWater()) {
         pMatrixStack.translate((double)0.2F, (double)0.1F, 0.0D);
         pMatrixStack.mulPose(Vector3f.ZP.rotationDegrees(90.0F));
      }

   }
}