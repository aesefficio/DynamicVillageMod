package com.mojang.math;

import net.minecraft.util.Mth;

public final class Quaternion {
   public static final Quaternion ONE = new Quaternion(0.0F, 0.0F, 0.0F, 1.0F);
   private float i;
   private float j;
   private float k;
   private float r;

   public Quaternion(float pI, float pJ, float pK, float pR) {
      this.i = pI;
      this.j = pJ;
      this.k = pK;
      this.r = pR;
   }

   public Quaternion(Vector3f pRotationAxis, float pRotationAngle, boolean pDegrees) {
      if (pDegrees) {
         pRotationAngle *= ((float)Math.PI / 180F);
      }

      float f = sin(pRotationAngle / 2.0F);
      this.i = pRotationAxis.x() * f;
      this.j = pRotationAxis.y() * f;
      this.k = pRotationAxis.z() * f;
      this.r = cos(pRotationAngle / 2.0F);
   }

   public Quaternion(float pX, float pY, float pZ, boolean pDegrees) {
      if (pDegrees) {
         pX *= ((float)Math.PI / 180F);
         pY *= ((float)Math.PI / 180F);
         pZ *= ((float)Math.PI / 180F);
      }

      float f = sin(0.5F * pX);
      float f1 = cos(0.5F * pX);
      float f2 = sin(0.5F * pY);
      float f3 = cos(0.5F * pY);
      float f4 = sin(0.5F * pZ);
      float f5 = cos(0.5F * pZ);
      this.i = f * f3 * f5 + f1 * f2 * f4;
      this.j = f1 * f2 * f5 - f * f3 * f4;
      this.k = f * f2 * f5 + f1 * f3 * f4;
      this.r = f1 * f3 * f5 - f * f2 * f4;
   }

   public Quaternion(Quaternion pOther) {
      this.i = pOther.i;
      this.j = pOther.j;
      this.k = pOther.k;
      this.r = pOther.r;
   }

   public static Quaternion fromYXZ(float pY, float pX, float pZ) {
      Quaternion quaternion = ONE.copy();
      quaternion.mul(new Quaternion(0.0F, (float)Math.sin((double)(pY / 2.0F)), 0.0F, (float)Math.cos((double)(pY / 2.0F))));
      quaternion.mul(new Quaternion((float)Math.sin((double)(pX / 2.0F)), 0.0F, 0.0F, (float)Math.cos((double)(pX / 2.0F))));
      quaternion.mul(new Quaternion(0.0F, 0.0F, (float)Math.sin((double)(pZ / 2.0F)), (float)Math.cos((double)(pZ / 2.0F))));
      return quaternion;
   }

   public static Quaternion fromXYZDegrees(Vector3f pDegreesVector) {
      return fromXYZ((float)Math.toRadians((double)pDegreesVector.x()), (float)Math.toRadians((double)pDegreesVector.y()), (float)Math.toRadians((double)pDegreesVector.z()));
   }

   public static Quaternion fromXYZ(Vector3f pRadiansVector) {
      return fromXYZ(pRadiansVector.x(), pRadiansVector.y(), pRadiansVector.z());
   }

   public static Quaternion fromXYZ(float pX, float pY, float pZ) {
      Quaternion quaternion = ONE.copy();
      quaternion.mul(new Quaternion((float)Math.sin((double)(pX / 2.0F)), 0.0F, 0.0F, (float)Math.cos((double)(pX / 2.0F))));
      quaternion.mul(new Quaternion(0.0F, (float)Math.sin((double)(pY / 2.0F)), 0.0F, (float)Math.cos((double)(pY / 2.0F))));
      quaternion.mul(new Quaternion(0.0F, 0.0F, (float)Math.sin((double)(pZ / 2.0F)), (float)Math.cos((double)(pZ / 2.0F))));
      return quaternion;
   }

   public Vector3f toXYZ() {
      float f = this.r() * this.r();
      float f1 = this.i() * this.i();
      float f2 = this.j() * this.j();
      float f3 = this.k() * this.k();
      float f4 = f + f1 + f2 + f3;
      float f5 = 2.0F * this.r() * this.i() - 2.0F * this.j() * this.k();
      float f6 = (float)Math.asin((double)(f5 / f4));
      return Math.abs(f5) > 0.999F * f4 ? new Vector3f(2.0F * (float)Math.atan2((double)this.i(), (double)this.r()), f6, 0.0F) : new Vector3f((float)Math.atan2((double)(2.0F * this.j() * this.k() + 2.0F * this.i() * this.r()), (double)(f - f1 - f2 + f3)), f6, (float)Math.atan2((double)(2.0F * this.i() * this.j() + 2.0F * this.r() * this.k()), (double)(f + f1 - f2 - f3)));
   }

   public Vector3f toXYZDegrees() {
      Vector3f vector3f = this.toXYZ();
      return new Vector3f((float)Math.toDegrees((double)vector3f.x()), (float)Math.toDegrees((double)vector3f.y()), (float)Math.toDegrees((double)vector3f.z()));
   }

   public Vector3f toYXZ() {
      float f = this.r() * this.r();
      float f1 = this.i() * this.i();
      float f2 = this.j() * this.j();
      float f3 = this.k() * this.k();
      float f4 = f + f1 + f2 + f3;
      float f5 = 2.0F * this.r() * this.i() - 2.0F * this.j() * this.k();
      float f6 = (float)Math.asin((double)(f5 / f4));
      return Math.abs(f5) > 0.999F * f4 ? new Vector3f(f6, 2.0F * (float)Math.atan2((double)this.j(), (double)this.r()), 0.0F) : new Vector3f(f6, (float)Math.atan2((double)(2.0F * this.i() * this.k() + 2.0F * this.j() * this.r()), (double)(f - f1 - f2 + f3)), (float)Math.atan2((double)(2.0F * this.i() * this.j() + 2.0F * this.r() * this.k()), (double)(f - f1 + f2 - f3)));
   }

   public Vector3f toYXZDegrees() {
      Vector3f vector3f = this.toYXZ();
      return new Vector3f((float)Math.toDegrees((double)vector3f.x()), (float)Math.toDegrees((double)vector3f.y()), (float)Math.toDegrees((double)vector3f.z()));
   }

   public boolean equals(Object pOther) {
      if (this == pOther) {
         return true;
      } else if (pOther != null && this.getClass() == pOther.getClass()) {
         Quaternion quaternion = (Quaternion)pOther;
         if (Float.compare(quaternion.i, this.i) != 0) {
            return false;
         } else if (Float.compare(quaternion.j, this.j) != 0) {
            return false;
         } else if (Float.compare(quaternion.k, this.k) != 0) {
            return false;
         } else {
            return Float.compare(quaternion.r, this.r) == 0;
         }
      } else {
         return false;
      }
   }

   public int hashCode() {
      int i = Float.floatToIntBits(this.i);
      i = 31 * i + Float.floatToIntBits(this.j);
      i = 31 * i + Float.floatToIntBits(this.k);
      return 31 * i + Float.floatToIntBits(this.r);
   }

   public String toString() {
      StringBuilder stringbuilder = new StringBuilder();
      stringbuilder.append("Quaternion[").append(this.r()).append(" + ");
      stringbuilder.append(this.i()).append("i + ");
      stringbuilder.append(this.j()).append("j + ");
      stringbuilder.append(this.k()).append("k]");
      return stringbuilder.toString();
   }

   public float i() {
      return this.i;
   }

   public float j() {
      return this.j;
   }

   public float k() {
      return this.k;
   }

   public float r() {
      return this.r;
   }

   public void mul(Quaternion pOther) {
      float f = this.i();
      float f1 = this.j();
      float f2 = this.k();
      float f3 = this.r();
      float f4 = pOther.i();
      float f5 = pOther.j();
      float f6 = pOther.k();
      float f7 = pOther.r();
      this.i = f3 * f4 + f * f7 + f1 * f6 - f2 * f5;
      this.j = f3 * f5 - f * f6 + f1 * f7 + f2 * f4;
      this.k = f3 * f6 + f * f5 - f1 * f4 + f2 * f7;
      this.r = f3 * f7 - f * f4 - f1 * f5 - f2 * f6;
   }

   public void mul(float pMultiplier) {
      this.i *= pMultiplier;
      this.j *= pMultiplier;
      this.k *= pMultiplier;
      this.r *= pMultiplier;
   }

   public void conj() {
      this.i = -this.i;
      this.j = -this.j;
      this.k = -this.k;
   }

   public void set(float pI, float pJ, float pK, float pR) {
      this.i = pI;
      this.j = pJ;
      this.k = pK;
      this.r = pR;
   }

   private static float cos(float pAngle) {
      return (float)Math.cos((double)pAngle);
   }

   private static float sin(float pAngle) {
      return (float)Math.sin((double)pAngle);
   }

   public void normalize() {
      float f = this.i() * this.i() + this.j() * this.j() + this.k() * this.k() + this.r() * this.r();
      if (f > 1.0E-6F) {
         float f1 = Mth.fastInvSqrt(f);
         this.i *= f1;
         this.j *= f1;
         this.k *= f1;
         this.r *= f1;
      } else {
         this.i = 0.0F;
         this.j = 0.0F;
         this.k = 0.0F;
         this.r = 0.0F;
      }

   }

   public void slerp(Quaternion pOther, float pPercentage) {
      throw new UnsupportedOperationException();
   }

   public Quaternion copy() {
      return new Quaternion(this);
   }
}