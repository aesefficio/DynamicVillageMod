package net.minecraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class HierarchicalModel<E extends Entity> extends EntityModel<E> {
   private static final Vector3f ANIMATION_VECTOR_CACHE = new Vector3f();

   public HierarchicalModel() {
      this(RenderType::entityCutoutNoCull);
   }

   public HierarchicalModel(Function<ResourceLocation, RenderType> p_170623_) {
      super(p_170623_);
   }

   public void renderToBuffer(PoseStack p_170625_, VertexConsumer p_170626_, int p_170627_, int p_170628_, float p_170629_, float p_170630_, float p_170631_, float p_170632_) {
      this.root().render(p_170625_, p_170626_, p_170627_, p_170628_, p_170629_, p_170630_, p_170631_, p_170632_);
   }

   public abstract ModelPart root();

   public Optional<ModelPart> getAnyDescendantWithName(String p_233394_) {
      return this.root().getAllParts().filter((p_233400_) -> {
         return p_233400_.hasChild(p_233394_);
      }).findFirst().map((p_233397_) -> {
         return p_233397_.getChild(p_233394_);
      });
   }

   protected void animate(AnimationState p_233382_, AnimationDefinition p_233383_, float p_233384_) {
      this.animate(p_233382_, p_233383_, p_233384_, 1.0F);
   }

   protected void animate(AnimationState p_233386_, AnimationDefinition p_233387_, float p_233388_, float p_233389_) {
      p_233386_.updateTime(p_233388_, p_233389_);
      p_233386_.ifStarted((p_233392_) -> {
         KeyframeAnimations.animate(this, p_233387_, p_233392_.getAccumulatedTime(), 1.0F, ANIMATION_VECTOR_CACHE);
      });
   }
}