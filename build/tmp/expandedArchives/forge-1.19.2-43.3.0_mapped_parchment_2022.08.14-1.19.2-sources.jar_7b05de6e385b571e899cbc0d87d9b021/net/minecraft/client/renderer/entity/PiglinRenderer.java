package net.minecraft.client.renderer.entity;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PiglinModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PiglinRenderer extends HumanoidMobRenderer<Mob, PiglinModel<Mob>> {
   private static final Map<EntityType<?>, ResourceLocation> TEXTURES = ImmutableMap.of(EntityType.PIGLIN, new ResourceLocation("textures/entity/piglin/piglin.png"), EntityType.ZOMBIFIED_PIGLIN, new ResourceLocation("textures/entity/piglin/zombified_piglin.png"), EntityType.PIGLIN_BRUTE, new ResourceLocation("textures/entity/piglin/piglin_brute.png"));
   private static final float PIGLIN_CUSTOM_HEAD_SCALE = 1.0019531F;

   public PiglinRenderer(EntityRendererProvider.Context pContext, ModelLayerLocation p_174345_, ModelLayerLocation p_174346_, ModelLayerLocation p_174347_, boolean p_174348_) {
      super(pContext, createModel(pContext.getModelSet(), p_174345_, p_174348_), 0.5F, 1.0019531F, 1.0F, 1.0019531F);
      this.addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel(pContext.bakeLayer(p_174346_)), new HumanoidModel(pContext.bakeLayer(p_174347_))));
   }

   private static PiglinModel<Mob> createModel(EntityModelSet p_174350_, ModelLayerLocation pLayer, boolean p_174352_) {
      PiglinModel<Mob> piglinmodel = new PiglinModel<>(p_174350_.bakeLayer(pLayer));
      if (p_174352_) {
         piglinmodel.rightEar.visible = false;
      }

      return piglinmodel;
   }

   /**
    * Returns the location of an entity's texture.
    */
   public ResourceLocation getTextureLocation(Mob pEntity) {
      ResourceLocation resourcelocation = TEXTURES.get(pEntity.getType());
      if (resourcelocation == null) {
         throw new IllegalArgumentException("I don't know what texture to use for " + pEntity.getType());
      } else {
         return resourcelocation;
      }
   }

   protected boolean isShaking(Mob pEntity) {
      return super.isShaking(pEntity) || pEntity instanceof AbstractPiglin && ((AbstractPiglin)pEntity).isConverting();
   }
}