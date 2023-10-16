package com.mojang.math;

import com.mojang.datafixers.util.Pair;
import java.nio.FloatBuffer;
import net.minecraft.util.Mth;
import org.apache.commons.lang3.tuple.Triple;

public final class Matrix3f {
   private static final int ORDER = 3;
   private static final float G = 3.0F + 2.0F * (float)Math.sqrt(2.0D);
   private static final float CS = (float)Math.cos((Math.PI / 8D));
   private static final float SS = (float)Math.sin((Math.PI / 8D));
   private static final float SQ2 = 1.0F / (float)Math.sqrt(2.0D);
   protected float m00;
   protected float m01;
   protected float m02;
   protected float m10;
   protected float m11;
   protected float m12;
   protected float m20;
   protected float m21;
   protected float m22;

   public Matrix3f() {
   }

   public Matrix3f(Quaternion pQuaternion) {
      float f = pQuaternion.i();
      float f1 = pQuaternion.j();
      float f2 = pQuaternion.k();
      float f3 = pQuaternion.r();
      float f4 = 2.0F * f * f;
      float f5 = 2.0F * f1 * f1;
      float f6 = 2.0F * f2 * f2;
      this.m00 = 1.0F - f5 - f6;
      this.m11 = 1.0F - f6 - f4;
      this.m22 = 1.0F - f4 - f5;
      float f7 = f * f1;
      float f8 = f1 * f2;
      float f9 = f2 * f;
      float f10 = f * f3;
      float f11 = f1 * f3;
      float f12 = f2 * f3;
      this.m10 = 2.0F * (f7 + f12);
      this.m01 = 2.0F * (f7 - f12);
      this.m20 = 2.0F * (f9 - f11);
      this.m02 = 2.0F * (f9 + f11);
      this.m21 = 2.0F * (f8 + f10);
      this.m12 = 2.0F * (f8 - f10);
   }

   public static Matrix3f createScaleMatrix(float pM00, float pM11, float pM22) {
      Matrix3f matrix3f = new Matrix3f();
      matrix3f.m00 = pM00;
      matrix3f.m11 = pM11;
      matrix3f.m22 = pM22;
      return matrix3f;
   }

   public Matrix3f(Matrix4f pMatrix) {
      this.m00 = pMatrix.m00;
      this.m01 = pMatrix.m01;
      this.m02 = pMatrix.m02;
      this.m10 = pMatrix.m10;
      this.m11 = pMatrix.m11;
      this.m12 = pMatrix.m12;
      this.m20 = pMatrix.m20;
      this.m21 = pMatrix.m21;
      this.m22 = pMatrix.m22;
   }

   public Matrix3f(Matrix3f pOther) {
      this.m00 = pOther.m00;
      this.m01 = pOther.m01;
      this.m02 = pOther.m02;
      this.m10 = pOther.m10;
      this.m11 = pOther.m11;
      this.m12 = pOther.m12;
      this.m20 = pOther.m20;
      this.m21 = pOther.m21;
      this.m22 = pOther.m22;
   }

   private static Pair<Float, Float> approxGivensQuat(float pOrigin, float pMiddle, float pEnd) {
      float f = 2.0F * (pOrigin - pEnd);
      if (G * pMiddle * pMiddle < f * f) {
         float f1 = Mth.fastInvSqrt(pMiddle * pMiddle + f * f);
         return Pair.of(f1 * pMiddle, f1 * f);
      } else {
         return Pair.of(SS, CS);
      }
   }

   private static Pair<Float, Float> qrGivensQuat(float pOrigin, float pDelta) {
      float f = (float)Math.hypot((double)pOrigin, (double)pDelta);
      float f1 = f > 1.0E-6F ? pDelta : 0.0F;
      float f2 = Math.abs(pOrigin) + Math.max(f, 1.0E-6F);
      if (pOrigin < 0.0F) {
         float f3 = f1;
         f1 = f2;
         f2 = f3;
      }

      float f4 = Mth.fastInvSqrt(f2 * f2 + f1 * f1);
      f2 *= f4;
      f1 *= f4;
      return Pair.of(f1, f2);
   }

   private static Quaternion stepJacobi(Matrix3f pMatrix) {
      Matrix3f matrix3f = new Matrix3f();
      Quaternion quaternion = Quaternion.ONE.copy();
      if (pMatrix.m01 * pMatrix.m01 + pMatrix.m10 * pMatrix.m10 > 1.0E-6F) {
         Pair<Float, Float> pair = approxGivensQuat(pMatrix.m00, 0.5F * (pMatrix.m01 + pMatrix.m10), pMatrix.m11);
         Float f = pair.getFirst();
         Float f1 = pair.getSecond();
         Quaternion quaternion1 = new Quaternion(0.0F, 0.0F, f, f1);
         float f2 = f1 * f1 - f * f;
         float f3 = -2.0F * f * f1;
         float f4 = f1 * f1 + f * f;
         quaternion.mul(quaternion1);
         matrix3f.setIdentity();
         matrix3f.m00 = f2;
         matrix3f.m11 = f2;
         matrix3f.m10 = -f3;
         matrix3f.m01 = f3;
         matrix3f.m22 = f4;
         pMatrix.mul(matrix3f);
         matrix3f.transpose();
         matrix3f.mul(pMatrix);
         pMatrix.load(matrix3f);
      }

      if (pMatrix.m02 * pMatrix.m02 + pMatrix.m20 * pMatrix.m20 > 1.0E-6F) {
         Pair<Float, Float> pair1 = approxGivensQuat(pMatrix.m00, 0.5F * (pMatrix.m02 + pMatrix.m20), pMatrix.m22);
         float f5 = -pair1.getFirst();
         Float f7 = pair1.getSecond();
         Quaternion quaternion2 = new Quaternion(0.0F, f5, 0.0F, f7);
         float f9 = f7 * f7 - f5 * f5;
         float f11 = -2.0F * f5 * f7;
         float f13 = f7 * f7 + f5 * f5;
         quaternion.mul(quaternion2);
         matrix3f.setIdentity();
         matrix3f.m00 = f9;
         matrix3f.m22 = f9;
         matrix3f.m20 = f11;
         matrix3f.m02 = -f11;
         matrix3f.m11 = f13;
         pMatrix.mul(matrix3f);
         matrix3f.transpose();
         matrix3f.mul(pMatrix);
         pMatrix.load(matrix3f);
      }

      if (pMatrix.m12 * pMatrix.m12 + pMatrix.m21 * pMatrix.m21 > 1.0E-6F) {
         Pair<Float, Float> pair2 = approxGivensQuat(pMatrix.m11, 0.5F * (pMatrix.m12 + pMatrix.m21), pMatrix.m22);
         Float f6 = pair2.getFirst();
         Float f8 = pair2.getSecond();
         Quaternion quaternion3 = new Quaternion(f6, 0.0F, 0.0F, f8);
         float f10 = f8 * f8 - f6 * f6;
         float f12 = -2.0F * f6 * f8;
         float f14 = f8 * f8 + f6 * f6;
         quaternion.mul(quaternion3);
         matrix3f.setIdentity();
         matrix3f.m11 = f10;
         matrix3f.m22 = f10;
         matrix3f.m21 = -f12;
         matrix3f.m12 = f12;
         matrix3f.m00 = f14;
         pMatrix.mul(matrix3f);
         matrix3f.transpose();
         matrix3f.mul(pMatrix);
         pMatrix.load(matrix3f);
      }

      return quaternion;
   }

   private static void sortSingularValues(Matrix3f pMatrix, Quaternion pQuaternion) {
      float f1 = pMatrix.m00 * pMatrix.m00 + pMatrix.m10 * pMatrix.m10 + pMatrix.m20 * pMatrix.m20;
      float f2 = pMatrix.m01 * pMatrix.m01 + pMatrix.m11 * pMatrix.m11 + pMatrix.m21 * pMatrix.m21;
      float f3 = pMatrix.m02 * pMatrix.m02 + pMatrix.m12 * pMatrix.m12 + pMatrix.m22 * pMatrix.m22;
      if (f1 < f2) {
         float f = pMatrix.m10;
         pMatrix.m10 = -pMatrix.m00;
         pMatrix.m00 = f;
         f = pMatrix.m11;
         pMatrix.m11 = -pMatrix.m01;
         pMatrix.m01 = f;
         f = pMatrix.m12;
         pMatrix.m12 = -pMatrix.m02;
         pMatrix.m02 = f;
         Quaternion quaternion = new Quaternion(0.0F, 0.0F, SQ2, SQ2);
         pQuaternion.mul(quaternion);
         f = f1;
         f1 = f2;
         f2 = f;
      }

      if (f1 < f3) {
         float f4 = pMatrix.m20;
         pMatrix.m20 = -pMatrix.m00;
         pMatrix.m00 = f4;
         f4 = pMatrix.m21;
         pMatrix.m21 = -pMatrix.m01;
         pMatrix.m01 = f4;
         f4 = pMatrix.m22;
         pMatrix.m22 = -pMatrix.m02;
         pMatrix.m02 = f4;
         Quaternion quaternion1 = new Quaternion(0.0F, SQ2, 0.0F, SQ2);
         pQuaternion.mul(quaternion1);
         f3 = f1;
      }

      if (f2 < f3) {
         float f5 = pMatrix.m20;
         pMatrix.m20 = -pMatrix.m10;
         pMatrix.m10 = f5;
         f5 = pMatrix.m21;
         pMatrix.m21 = -pMatrix.m11;
         pMatrix.m11 = f5;
         f5 = pMatrix.m22;
         pMatrix.m22 = -pMatrix.m12;
         pMatrix.m12 = f5;
         Quaternion quaternion2 = new Quaternion(SQ2, 0.0F, 0.0F, SQ2);
         pQuaternion.mul(quaternion2);
      }

   }

   public void transpose() {
      float f = this.m01;
      this.m01 = this.m10;
      this.m10 = f;
      f = this.m02;
      this.m02 = this.m20;
      this.m20 = f;
      f = this.m12;
      this.m12 = this.m21;
      this.m21 = f;
   }

   public Triple<Quaternion, Vector3f, Quaternion> svdDecompose() {
      Quaternion quaternion = Quaternion.ONE.copy();
      Quaternion quaternion1 = Quaternion.ONE.copy();
      Matrix3f matrix3f = this.copy();
      matrix3f.transpose();
      matrix3f.mul(this);

      for(int i = 0; i < 5; ++i) {
         quaternion1.mul(stepJacobi(matrix3f));
      }

      quaternion1.normalize();
      Matrix3f matrix3f4 = new Matrix3f(this);
      matrix3f4.mul(new Matrix3f(quaternion1));
      float f = 1.0F;
      Pair<Float, Float> pair = qrGivensQuat(matrix3f4.m00, matrix3f4.m10);
      Float f1 = pair.getFirst();
      Float f2 = pair.getSecond();
      float f3 = f2 * f2 - f1 * f1;
      float f4 = -2.0F * f1 * f2;
      float f5 = f2 * f2 + f1 * f1;
      Quaternion quaternion2 = new Quaternion(0.0F, 0.0F, f1, f2);
      quaternion.mul(quaternion2);
      Matrix3f matrix3f1 = new Matrix3f();
      matrix3f1.setIdentity();
      matrix3f1.m00 = f3;
      matrix3f1.m11 = f3;
      matrix3f1.m10 = f4;
      matrix3f1.m01 = -f4;
      matrix3f1.m22 = f5;
      f *= f5;
      matrix3f1.mul(matrix3f4);
      pair = qrGivensQuat(matrix3f1.m00, matrix3f1.m20);
      float f6 = -pair.getFirst();
      Float f7 = pair.getSecond();
      float f8 = f7 * f7 - f6 * f6;
      float f9 = -2.0F * f6 * f7;
      float f10 = f7 * f7 + f6 * f6;
      Quaternion quaternion3 = new Quaternion(0.0F, f6, 0.0F, f7);
      quaternion.mul(quaternion3);
      Matrix3f matrix3f2 = new Matrix3f();
      matrix3f2.setIdentity();
      matrix3f2.m00 = f8;
      matrix3f2.m22 = f8;
      matrix3f2.m20 = -f9;
      matrix3f2.m02 = f9;
      matrix3f2.m11 = f10;
      f *= f10;
      matrix3f2.mul(matrix3f1);
      pair = qrGivensQuat(matrix3f2.m11, matrix3f2.m21);
      Float f11 = pair.getFirst();
      Float f12 = pair.getSecond();
      float f13 = f12 * f12 - f11 * f11;
      float f14 = -2.0F * f11 * f12;
      float f15 = f12 * f12 + f11 * f11;
      Quaternion quaternion4 = new Quaternion(f11, 0.0F, 0.0F, f12);
      quaternion.mul(quaternion4);
      Matrix3f matrix3f3 = new Matrix3f();
      matrix3f3.setIdentity();
      matrix3f3.m11 = f13;
      matrix3f3.m22 = f13;
      matrix3f3.m21 = f14;
      matrix3f3.m12 = -f14;
      matrix3f3.m00 = f15;
      f *= f15;
      matrix3f3.mul(matrix3f2);
      f = 1.0F / f;
      quaternion.mul((float)Math.sqrt((double)f));
      Vector3f vector3f = new Vector3f(matrix3f3.m00 * f, matrix3f3.m11 * f, matrix3f3.m22 * f);
      return Triple.of(quaternion, vector3f, quaternion1);
   }

   public boolean equals(Object pOther) {
      if (this == pOther) {
         return true;
      } else if (pOther != null && this.getClass() == pOther.getClass()) {
         Matrix3f matrix3f = (Matrix3f)pOther;
         return Float.compare(matrix3f.m00, this.m00) == 0 && Float.compare(matrix3f.m01, this.m01) == 0 && Float.compare(matrix3f.m02, this.m02) == 0 && Float.compare(matrix3f.m10, this.m10) == 0 && Float.compare(matrix3f.m11, this.m11) == 0 && Float.compare(matrix3f.m12, this.m12) == 0 && Float.compare(matrix3f.m20, this.m20) == 0 && Float.compare(matrix3f.m21, this.m21) == 0 && Float.compare(matrix3f.m22, this.m22) == 0;
      } else {
         return false;
      }
   }

   public int hashCode() {
      int i = this.m00 != 0.0F ? Float.floatToIntBits(this.m00) : 0;
      i = 31 * i + (this.m01 != 0.0F ? Float.floatToIntBits(this.m01) : 0);
      i = 31 * i + (this.m02 != 0.0F ? Float.floatToIntBits(this.m02) : 0);
      i = 31 * i + (this.m10 != 0.0F ? Float.floatToIntBits(this.m10) : 0);
      i = 31 * i + (this.m11 != 0.0F ? Float.floatToIntBits(this.m11) : 0);
      i = 31 * i + (this.m12 != 0.0F ? Float.floatToIntBits(this.m12) : 0);
      i = 31 * i + (this.m20 != 0.0F ? Float.floatToIntBits(this.m20) : 0);
      i = 31 * i + (this.m21 != 0.0F ? Float.floatToIntBits(this.m21) : 0);
      return 31 * i + (this.m22 != 0.0F ? Float.floatToIntBits(this.m22) : 0);
   }

   private static int bufferIndex(int pX, int pY) {
      return pY * 3 + pX;
   }

   public void load(FloatBuffer pBuffer) {
      this.m00 = pBuffer.get(bufferIndex(0, 0));
      this.m01 = pBuffer.get(bufferIndex(0, 1));
      this.m02 = pBuffer.get(bufferIndex(0, 2));
      this.m10 = pBuffer.get(bufferIndex(1, 0));
      this.m11 = pBuffer.get(bufferIndex(1, 1));
      this.m12 = pBuffer.get(bufferIndex(1, 2));
      this.m20 = pBuffer.get(bufferIndex(2, 0));
      this.m21 = pBuffer.get(bufferIndex(2, 1));
      this.m22 = pBuffer.get(bufferIndex(2, 2));
   }

   public void loadTransposed(FloatBuffer pBuffer) {
      this.m00 = pBuffer.get(bufferIndex(0, 0));
      this.m01 = pBuffer.get(bufferIndex(1, 0));
      this.m02 = pBuffer.get(bufferIndex(2, 0));
      this.m10 = pBuffer.get(bufferIndex(0, 1));
      this.m11 = pBuffer.get(bufferIndex(1, 1));
      this.m12 = pBuffer.get(bufferIndex(2, 1));
      this.m20 = pBuffer.get(bufferIndex(0, 2));
      this.m21 = pBuffer.get(bufferIndex(1, 2));
      this.m22 = pBuffer.get(bufferIndex(2, 2));
   }

   public void load(FloatBuffer pBuffer, boolean pTranspose) {
      if (pTranspose) {
         this.loadTransposed(pBuffer);
      } else {
         this.load(pBuffer);
      }

   }

   public void load(Matrix3f pOther) {
      this.m00 = pOther.m00;
      this.m01 = pOther.m01;
      this.m02 = pOther.m02;
      this.m10 = pOther.m10;
      this.m11 = pOther.m11;
      this.m12 = pOther.m12;
      this.m20 = pOther.m20;
      this.m21 = pOther.m21;
      this.m22 = pOther.m22;
   }

   public String toString() {
      StringBuilder stringbuilder = new StringBuilder();
      stringbuilder.append("Matrix3f:\n");
      stringbuilder.append(this.m00);
      stringbuilder.append(" ");
      stringbuilder.append(this.m01);
      stringbuilder.append(" ");
      stringbuilder.append(this.m02);
      stringbuilder.append("\n");
      stringbuilder.append(this.m10);
      stringbuilder.append(" ");
      stringbuilder.append(this.m11);
      stringbuilder.append(" ");
      stringbuilder.append(this.m12);
      stringbuilder.append("\n");
      stringbuilder.append(this.m20);
      stringbuilder.append(" ");
      stringbuilder.append(this.m21);
      stringbuilder.append(" ");
      stringbuilder.append(this.m22);
      stringbuilder.append("\n");
      return stringbuilder.toString();
   }

   public void store(FloatBuffer pBuffer) {
      pBuffer.put(bufferIndex(0, 0), this.m00);
      pBuffer.put(bufferIndex(0, 1), this.m01);
      pBuffer.put(bufferIndex(0, 2), this.m02);
      pBuffer.put(bufferIndex(1, 0), this.m10);
      pBuffer.put(bufferIndex(1, 1), this.m11);
      pBuffer.put(bufferIndex(1, 2), this.m12);
      pBuffer.put(bufferIndex(2, 0), this.m20);
      pBuffer.put(bufferIndex(2, 1), this.m21);
      pBuffer.put(bufferIndex(2, 2), this.m22);
   }

   public void storeTransposed(FloatBuffer pBuffer) {
      pBuffer.put(bufferIndex(0, 0), this.m00);
      pBuffer.put(bufferIndex(1, 0), this.m01);
      pBuffer.put(bufferIndex(2, 0), this.m02);
      pBuffer.put(bufferIndex(0, 1), this.m10);
      pBuffer.put(bufferIndex(1, 1), this.m11);
      pBuffer.put(bufferIndex(2, 1), this.m12);
      pBuffer.put(bufferIndex(0, 2), this.m20);
      pBuffer.put(bufferIndex(1, 2), this.m21);
      pBuffer.put(bufferIndex(2, 2), this.m22);
   }

   public void store(FloatBuffer pBuffer, boolean pTranspose) {
      if (pTranspose) {
         this.storeTransposed(pBuffer);
      } else {
         this.store(pBuffer);
      }

   }

   public void setIdentity() {
      this.m00 = 1.0F;
      this.m01 = 0.0F;
      this.m02 = 0.0F;
      this.m10 = 0.0F;
      this.m11 = 1.0F;
      this.m12 = 0.0F;
      this.m20 = 0.0F;
      this.m21 = 0.0F;
      this.m22 = 1.0F;
   }

   public float adjugateAndDet() {
      float f = this.m11 * this.m22 - this.m12 * this.m21;
      float f1 = -(this.m10 * this.m22 - this.m12 * this.m20);
      float f2 = this.m10 * this.m21 - this.m11 * this.m20;
      float f3 = -(this.m01 * this.m22 - this.m02 * this.m21);
      float f4 = this.m00 * this.m22 - this.m02 * this.m20;
      float f5 = -(this.m00 * this.m21 - this.m01 * this.m20);
      float f6 = this.m01 * this.m12 - this.m02 * this.m11;
      float f7 = -(this.m00 * this.m12 - this.m02 * this.m10);
      float f8 = this.m00 * this.m11 - this.m01 * this.m10;
      float f9 = this.m00 * f + this.m01 * f1 + this.m02 * f2;
      this.m00 = f;
      this.m10 = f1;
      this.m20 = f2;
      this.m01 = f3;
      this.m11 = f4;
      this.m21 = f5;
      this.m02 = f6;
      this.m12 = f7;
      this.m22 = f8;
      return f9;
   }

   public float determinant() {
      float f = this.m11 * this.m22 - this.m12 * this.m21;
      float f1 = -(this.m10 * this.m22 - this.m12 * this.m20);
      float f2 = this.m10 * this.m21 - this.m11 * this.m20;
      return this.m00 * f + this.m01 * f1 + this.m02 * f2;
   }

   public boolean invert() {
      float f = this.adjugateAndDet();
      if (Math.abs(f) > 1.0E-6F) {
         this.mul(f);
         return true;
      } else {
         return false;
      }
   }

   public void set(int pX, int pY, float pValue) {
      if (pX == 0) {
         if (pY == 0) {
            this.m00 = pValue;
         } else if (pY == 1) {
            this.m01 = pValue;
         } else {
            this.m02 = pValue;
         }
      } else if (pX == 1) {
         if (pY == 0) {
            this.m10 = pValue;
         } else if (pY == 1) {
            this.m11 = pValue;
         } else {
            this.m12 = pValue;
         }
      } else if (pY == 0) {
         this.m20 = pValue;
      } else if (pY == 1) {
         this.m21 = pValue;
      } else {
         this.m22 = pValue;
      }

   }

   public void mul(Matrix3f pOther) {
      float f = this.m00 * pOther.m00 + this.m01 * pOther.m10 + this.m02 * pOther.m20;
      float f1 = this.m00 * pOther.m01 + this.m01 * pOther.m11 + this.m02 * pOther.m21;
      float f2 = this.m00 * pOther.m02 + this.m01 * pOther.m12 + this.m02 * pOther.m22;
      float f3 = this.m10 * pOther.m00 + this.m11 * pOther.m10 + this.m12 * pOther.m20;
      float f4 = this.m10 * pOther.m01 + this.m11 * pOther.m11 + this.m12 * pOther.m21;
      float f5 = this.m10 * pOther.m02 + this.m11 * pOther.m12 + this.m12 * pOther.m22;
      float f6 = this.m20 * pOther.m00 + this.m21 * pOther.m10 + this.m22 * pOther.m20;
      float f7 = this.m20 * pOther.m01 + this.m21 * pOther.m11 + this.m22 * pOther.m21;
      float f8 = this.m20 * pOther.m02 + this.m21 * pOther.m12 + this.m22 * pOther.m22;
      this.m00 = f;
      this.m01 = f1;
      this.m02 = f2;
      this.m10 = f3;
      this.m11 = f4;
      this.m12 = f5;
      this.m20 = f6;
      this.m21 = f7;
      this.m22 = f8;
   }

   public void mul(Quaternion pQuaternion) {
      this.mul(new Matrix3f(pQuaternion));
   }

   public void mul(float pMultiplier) {
      this.m00 *= pMultiplier;
      this.m01 *= pMultiplier;
      this.m02 *= pMultiplier;
      this.m10 *= pMultiplier;
      this.m11 *= pMultiplier;
      this.m12 *= pMultiplier;
      this.m20 *= pMultiplier;
      this.m21 *= pMultiplier;
      this.m22 *= pMultiplier;
   }

   public void add(Matrix3f pOther) {
      this.m00 += pOther.m00;
      this.m01 += pOther.m01;
      this.m02 += pOther.m02;
      this.m10 += pOther.m10;
      this.m11 += pOther.m11;
      this.m12 += pOther.m12;
      this.m20 += pOther.m20;
      this.m21 += pOther.m21;
      this.m22 += pOther.m22;
   }

   public void sub(Matrix3f pOther) {
      this.m00 -= pOther.m00;
      this.m01 -= pOther.m01;
      this.m02 -= pOther.m02;
      this.m10 -= pOther.m10;
      this.m11 -= pOther.m11;
      this.m12 -= pOther.m12;
      this.m20 -= pOther.m20;
      this.m21 -= pOther.m21;
      this.m22 -= pOther.m22;
   }

   public float trace() {
      return this.m00 + this.m11 + this.m22;
   }

   public Matrix3f copy() {
      return new Matrix3f(this);
   }

    public void multiplyBackward(Matrix3f other) {
        Matrix3f copy = other.copy();
        copy.mul(this);
        this.load(copy);
    }
}
