package com.mojang.blaze3d.vertex;

import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import com.mojang.math.Vector4f;
import net.minecraft.core.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SheetedDecalTextureGenerator extends DefaultedVertexConsumer {
   private final VertexConsumer delegate;
   private final Matrix4f cameraInversePose;
   private final Matrix3f normalInversePose;
   private float x;
   private float y;
   private float z;
   private int overlayU;
   private int overlayV;
   private int lightCoords;
   private float nx;
   private float ny;
   private float nz;

   public SheetedDecalTextureGenerator(VertexConsumer pDelegate, Matrix4f pCameraPose, Matrix3f pNormalPose) {
      this.delegate = pDelegate;
      this.cameraInversePose = pCameraPose.copy();
      this.cameraInversePose.invert();
      this.normalInversePose = pNormalPose.copy();
      this.normalInversePose.invert();
      this.resetState();
   }

   private void resetState() {
      this.x = 0.0F;
      this.y = 0.0F;
      this.z = 0.0F;
      this.overlayU = 0;
      this.overlayV = 10;
      this.lightCoords = 15728880;
      this.nx = 0.0F;
      this.ny = 1.0F;
      this.nz = 0.0F;
   }

   public void endVertex() {
      Vector3f vector3f = new Vector3f(this.nx, this.ny, this.nz);
      vector3f.transform(this.normalInversePose);
      Direction direction = Direction.getNearest(vector3f.x(), vector3f.y(), vector3f.z());
      Vector4f vector4f = new Vector4f(this.x, this.y, this.z, 1.0F);
      vector4f.transform(this.cameraInversePose);
      vector4f.transform(Vector3f.YP.rotationDegrees(180.0F));
      vector4f.transform(Vector3f.XP.rotationDegrees(-90.0F));
      vector4f.transform(direction.getRotation());
      float f = -vector4f.x();
      float f1 = -vector4f.y();
      this.delegate.vertex((double)this.x, (double)this.y, (double)this.z).color(1.0F, 1.0F, 1.0F, 1.0F).uv(f, f1).overlayCoords(this.overlayU, this.overlayV).uv2(this.lightCoords).normal(this.nx, this.ny, this.nz).endVertex();
      this.resetState();
   }

   public VertexConsumer vertex(double pX, double pY, double pZ) {
      this.x = (float)pX;
      this.y = (float)pY;
      this.z = (float)pZ;
      return this;
   }

   public VertexConsumer color(int pRed, int pGreen, int pBlue, int pAlpha) {
      return this;
   }

   public VertexConsumer uv(float pU, float pV) {
      return this;
   }

   public VertexConsumer overlayCoords(int pU, int pV) {
      this.overlayU = pU;
      this.overlayV = pV;
      return this;
   }

   public VertexConsumer uv2(int pU, int pV) {
      this.lightCoords = pU | pV << 16;
      return this;
   }

   public VertexConsumer normal(float pX, float pY, float pZ) {
      this.nx = pX;
      this.ny = pY;
      this.nz = pZ;
      return this;
   }
}