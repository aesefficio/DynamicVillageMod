package com.mojang.blaze3d.vertex;

import com.google.common.collect.Queues;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Quaternion;
import java.util.Deque;
import net.minecraft.Util;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PoseStack implements net.minecraftforge.client.extensions.IForgePoseStack {
   private final Deque<PoseStack.Pose> poseStack = Util.make(Queues.newArrayDeque(), (p_85848_) -> {
      Matrix4f matrix4f = new Matrix4f();
      matrix4f.setIdentity();
      Matrix3f matrix3f = new Matrix3f();
      matrix3f.setIdentity();
      p_85848_.add(new PoseStack.Pose(matrix4f, matrix3f));
   });

   public void translate(double pX, double pY, double pZ) {
      PoseStack.Pose posestack$pose = this.poseStack.getLast();
      posestack$pose.pose.multiplyWithTranslation((float)pX, (float)pY, (float)pZ);
   }

   public void scale(float pX, float pY, float pZ) {
      PoseStack.Pose posestack$pose = this.poseStack.getLast();
      posestack$pose.pose.multiply(Matrix4f.createScaleMatrix(pX, pY, pZ));
      if (pX == pY && pY == pZ) {
         if (pX > 0.0F) {
            return;
         }

         posestack$pose.normal.mul(-1.0F);
      }

      float f = 1.0F / pX;
      float f1 = 1.0F / pY;
      float f2 = 1.0F / pZ;
      float f3 = Mth.fastInvCubeRoot(f * f1 * f2);
      posestack$pose.normal.mul(Matrix3f.createScaleMatrix(f3 * f, f3 * f1, f3 * f2));
   }

   public void mulPose(Quaternion pQuaternion) {
      PoseStack.Pose posestack$pose = this.poseStack.getLast();
      posestack$pose.pose.multiply(pQuaternion);
      posestack$pose.normal.mul(pQuaternion);
   }

   public void pushPose() {
      PoseStack.Pose posestack$pose = this.poseStack.getLast();
      this.poseStack.addLast(new PoseStack.Pose(posestack$pose.pose.copy(), posestack$pose.normal.copy()));
   }

   public void popPose() {
      this.poseStack.removeLast();
   }

   public PoseStack.Pose last() {
      return this.poseStack.getLast();
   }

   public boolean clear() {
      return this.poseStack.size() == 1;
   }

   public void setIdentity() {
      PoseStack.Pose posestack$pose = this.poseStack.getLast();
      posestack$pose.pose.setIdentity();
      posestack$pose.normal.setIdentity();
   }

   public void mulPoseMatrix(Matrix4f pMatrix) {
      (this.poseStack.getLast()).pose.multiply(pMatrix);
   }

   @OnlyIn(Dist.CLIENT)
   public static final class Pose {
      final Matrix4f pose;
      final Matrix3f normal;

      Pose(Matrix4f pPose, Matrix3f pNormal) {
         this.pose = pPose;
         this.normal = pNormal;
      }

      public Matrix4f pose() {
         return this.pose;
      }

      public Matrix3f normal() {
         return this.normal;
      }
   }
}
