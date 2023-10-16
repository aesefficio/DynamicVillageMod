package net.minecraft.client.animation;

import com.mojang.math.Vector3f;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class KeyframeAnimations {
   public static void animate(HierarchicalModel<?> p_232320_, AnimationDefinition p_232321_, long p_232322_, float p_232323_, Vector3f p_232324_) {
      float f = getElapsedSeconds(p_232321_, p_232322_);

      for(Map.Entry<String, List<AnimationChannel>> entry : p_232321_.boneAnimations().entrySet()) {
         Optional<ModelPart> optional = p_232320_.getAnyDescendantWithName(entry.getKey());
         List<AnimationChannel> list = entry.getValue();
         optional.ifPresent((p_232330_) -> {
            list.forEach((p_232311_) -> {
               Keyframe[] akeyframe = p_232311_.keyframes();
               int i = Math.max(0, Mth.binarySearch(0, akeyframe.length, (p_232315_) -> {
                  return f <= akeyframe[p_232315_].timestamp();
               }) - 1);
               int j = Math.min(akeyframe.length - 1, i + 1);
               Keyframe keyframe = akeyframe[i];
               Keyframe keyframe1 = akeyframe[j];
               float f1 = f - keyframe.timestamp();
               float f2 = Mth.clamp(f1 / (keyframe1.timestamp() - keyframe.timestamp()), 0.0F, 1.0F);
               keyframe1.interpolation().apply(p_232324_, f2, akeyframe, i, j, p_232323_);
               p_232311_.target().apply(p_232330_, p_232324_);
            });
         });
      }

   }

   private static float getElapsedSeconds(AnimationDefinition p_232317_, long p_232318_) {
      float f = (float)p_232318_ / 1000.0F;
      return p_232317_.looping() ? f % p_232317_.lengthInSeconds() : f;
   }

   public static Vector3f posVec(float p_232303_, float p_232304_, float p_232305_) {
      return new Vector3f(p_232303_, -p_232304_, p_232305_);
   }

   public static Vector3f degreeVec(float p_232332_, float p_232333_, float p_232334_) {
      return new Vector3f(p_232332_ * ((float)Math.PI / 180F), p_232333_ * ((float)Math.PI / 180F), p_232334_ * ((float)Math.PI / 180F));
   }

   public static Vector3f scaleVec(double p_232299_, double p_232300_, double p_232301_) {
      return new Vector3f((float)(p_232299_ - 1.0D), (float)(p_232300_ - 1.0D), (float)(p_232301_ - 1.0D));
   }
}