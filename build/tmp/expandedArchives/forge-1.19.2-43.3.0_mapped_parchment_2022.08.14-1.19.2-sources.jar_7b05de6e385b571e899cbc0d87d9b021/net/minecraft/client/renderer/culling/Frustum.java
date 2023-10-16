package net.minecraft.client.renderer.culling;

import com.mojang.math.Matrix4f;
import com.mojang.math.Vector4f;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class Frustum {
   public static final int OFFSET_STEP = 4;
   private final Vector4f[] frustumData = new Vector4f[6];
   private Vector4f viewVector;
   private double camX;
   private double camY;
   private double camZ;

   public Frustum(Matrix4f pProjection, Matrix4f pFrustrum) {
      this.calculateFrustum(pProjection, pFrustrum);
   }

   public Frustum(Frustum pOther) {
      System.arraycopy(pOther.frustumData, 0, this.frustumData, 0, pOther.frustumData.length);
      this.camX = pOther.camX;
      this.camY = pOther.camY;
      this.camZ = pOther.camZ;
      this.viewVector = pOther.viewVector;
   }

   public Frustum offsetToFullyIncludeCameraCube(int p_194442_) {
      double d0 = Math.floor(this.camX / (double)p_194442_) * (double)p_194442_;
      double d1 = Math.floor(this.camY / (double)p_194442_) * (double)p_194442_;
      double d2 = Math.floor(this.camZ / (double)p_194442_) * (double)p_194442_;
      double d3 = Math.ceil(this.camX / (double)p_194442_) * (double)p_194442_;
      double d4 = Math.ceil(this.camY / (double)p_194442_) * (double)p_194442_;

      for(double d5 = Math.ceil(this.camZ / (double)p_194442_) * (double)p_194442_; !this.cubeCompletelyInFrustum((float)(d0 - this.camX), (float)(d1 - this.camY), (float)(d2 - this.camZ), (float)(d3 - this.camX), (float)(d4 - this.camY), (float)(d5 - this.camZ)); this.camZ -= (double)(this.viewVector.z() * 4.0F)) {
         this.camX -= (double)(this.viewVector.x() * 4.0F);
         this.camY -= (double)(this.viewVector.y() * 4.0F);
      }

      return this;
   }

   public void prepare(double pCamX, double pCamY, double pCamZ) {
      this.camX = pCamX;
      this.camY = pCamY;
      this.camZ = pCamZ;
   }

   private void calculateFrustum(Matrix4f pProjection, Matrix4f pFrustrumMatrix) {
      Matrix4f matrix4f = pFrustrumMatrix.copy();
      matrix4f.multiply(pProjection);
      matrix4f.transpose();
      this.viewVector = new Vector4f(0.0F, 0.0F, 1.0F, 0.0F);
      this.viewVector.transform(matrix4f);
      this.getPlane(matrix4f, -1, 0, 0, 0);
      this.getPlane(matrix4f, 1, 0, 0, 1);
      this.getPlane(matrix4f, 0, -1, 0, 2);
      this.getPlane(matrix4f, 0, 1, 0, 3);
      this.getPlane(matrix4f, 0, 0, -1, 4);
      this.getPlane(matrix4f, 0, 0, 1, 5);
   }

   private void getPlane(Matrix4f pFrustrumMatrix, int pX, int pY, int pZ, int pId) {
      Vector4f vector4f = new Vector4f((float)pX, (float)pY, (float)pZ, 1.0F);
      vector4f.transform(pFrustrumMatrix);
      vector4f.normalize();
      this.frustumData[pId] = vector4f;
   }

   public boolean isVisible(AABB pAabb) {
      return this.cubeInFrustum(pAabb.minX, pAabb.minY, pAabb.minZ, pAabb.maxX, pAabb.maxY, pAabb.maxZ);
   }

   private boolean cubeInFrustum(double pMinX, double pMinY, double pMinZ, double pMaxX, double pMaxY, double pMaxZ) {
      float f = (float)(pMinX - this.camX);
      float f1 = (float)(pMinY - this.camY);
      float f2 = (float)(pMinZ - this.camZ);
      float f3 = (float)(pMaxX - this.camX);
      float f4 = (float)(pMaxY - this.camY);
      float f5 = (float)(pMaxZ - this.camZ);
      return this.cubeInFrustum(f, f1, f2, f3, f4, f5);
   }

   private boolean cubeInFrustum(float pMinX, float pMinY, float pMinZ, float pMaxX, float pMaxY, float pMaxZ) {
      for(int i = 0; i < 6; ++i) {
         Vector4f vector4f = this.frustumData[i];
         if (!(vector4f.dot(new Vector4f(pMinX, pMinY, pMinZ, 1.0F)) > 0.0F) && !(vector4f.dot(new Vector4f(pMaxX, pMinY, pMinZ, 1.0F)) > 0.0F) && !(vector4f.dot(new Vector4f(pMinX, pMaxY, pMinZ, 1.0F)) > 0.0F) && !(vector4f.dot(new Vector4f(pMaxX, pMaxY, pMinZ, 1.0F)) > 0.0F) && !(vector4f.dot(new Vector4f(pMinX, pMinY, pMaxZ, 1.0F)) > 0.0F) && !(vector4f.dot(new Vector4f(pMaxX, pMinY, pMaxZ, 1.0F)) > 0.0F) && !(vector4f.dot(new Vector4f(pMinX, pMaxY, pMaxZ, 1.0F)) > 0.0F) && !(vector4f.dot(new Vector4f(pMaxX, pMaxY, pMaxZ, 1.0F)) > 0.0F)) {
            return false;
         }
      }

      return true;
   }

   private boolean cubeCompletelyInFrustum(float p_194444_, float p_194445_, float p_194446_, float p_194447_, float p_194448_, float p_194449_) {
      for(int i = 0; i < 6; ++i) {
         Vector4f vector4f = this.frustumData[i];
         if (vector4f.dot(new Vector4f(p_194444_, p_194445_, p_194446_, 1.0F)) <= 0.0F) {
            return false;
         }

         if (vector4f.dot(new Vector4f(p_194447_, p_194445_, p_194446_, 1.0F)) <= 0.0F) {
            return false;
         }

         if (vector4f.dot(new Vector4f(p_194444_, p_194448_, p_194446_, 1.0F)) <= 0.0F) {
            return false;
         }

         if (vector4f.dot(new Vector4f(p_194447_, p_194448_, p_194446_, 1.0F)) <= 0.0F) {
            return false;
         }

         if (vector4f.dot(new Vector4f(p_194444_, p_194445_, p_194449_, 1.0F)) <= 0.0F) {
            return false;
         }

         if (vector4f.dot(new Vector4f(p_194447_, p_194445_, p_194449_, 1.0F)) <= 0.0F) {
            return false;
         }

         if (vector4f.dot(new Vector4f(p_194444_, p_194448_, p_194449_, 1.0F)) <= 0.0F) {
            return false;
         }

         if (vector4f.dot(new Vector4f(p_194447_, p_194448_, p_194449_, 1.0F)) <= 0.0F) {
            return false;
         }
      }

      return true;
   }
}