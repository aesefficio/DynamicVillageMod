package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class HumanoidMobRenderer<T extends Mob, M extends HumanoidModel<T>> extends MobRenderer<T, M> {
   private static final ResourceLocation DEFAULT_LOCATION = new ResourceLocation("textures/entity/steve.png");

   public HumanoidMobRenderer(EntityRendererProvider.Context pContext, M pModel, float pShadowRadius) {
      this(pContext, pModel, pShadowRadius, 1.0F, 1.0F, 1.0F);
   }

   public HumanoidMobRenderer(EntityRendererProvider.Context pContext, M pModel, float pShadowRadius, float pScaleX, float pScaleY, float pScaleZ) {
      super(pContext, pModel, pShadowRadius);
      this.addLayer(new CustomHeadLayer<>(this, pContext.getModelSet(), pScaleX, pScaleY, pScaleZ, pContext.getItemInHandRenderer()));
      this.addLayer(new ElytraLayer<>(this, pContext.getModelSet()));
      this.addLayer(new ItemInHandLayer<>(this, pContext.getItemInHandRenderer()));
   }

   /**
    * Returns the location of an entity's texture.
    */
   public ResourceLocation getTextureLocation(T pEntity) {
      return DEFAULT_LOCATION;
   }
}