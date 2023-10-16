package com.mojang.math;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.floats.Float2FloatFunction;
import net.minecraft.Util;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * A mutable, three dimensional vector with single floating point precision.
 */
public final class Vector3f {
   public static final Codec<Vector3f> CODEC = Codec.FLOAT.listOf().comapFlatMap((p_176767_) -> {
      return Util.fixedSize(p_176767_, 3).map((p_176774_) -> {
         return new Vector3f(p_176774_.get(0), p_176774_.get(1), p_176774_.get(2));
      });
   }, (p_176776_) -> {
      return ImmutableList.of(p_176776_.x, p_176776_.y, p_176776_.z);
   });
   public static Vector3f XN = new Vector3f(-1.0F, 0.0F, 0.0F);
   public static Vector3f XP = new Vector3f(1.0F, 0.0F, 0.0F);
   public static Vector3f YN = new Vector3f(0.0F, -1.0F, 0.0F);
   public static Vector3f YP = new Vector3f(0.0F, 1.0F, 0.0F);
   public static Vector3f ZN = new Vector3f(0.0F, 0.0F, -1.0F);
   public static Vector3f ZP = new Vector3f(0.0F, 0.0F, 1.0F);
   public static Vector3f ZERO = new Vector3f(0.0F, 0.0F, 0.0F);
   private float x;
   private float y;
   private float z;

   public Vector3f() {
   }

   public Vector3f(float pX, float pY, float pZ) {
      this.x = pX;
      this.y = pY;
      this.z = pZ;
   }

   public Vector3f(Vector4f pVector) {
      this(pVector.x(), pVector.y(), pVector.z());
   }

   public Vector3f(Vec3 pVector) {
      this((float)pVector.x, (float)pVector.y, (float)pVector.z);
   }

   public boolean equals(Object pOther) {
      if (this == pOther) {
         return true;
      } else if (pOther != null && this.getClass() == pOther.getClass()) {
         Vector3f vector3f = (Vector3f)pOther;
         if (Float.compare(vector3f.x, this.x) != 0) {
            return false;
         } else if (Float.compare(vector3f.y, this.y) != 0) {
            return false;
         } else {
            return Float.compare(vector3f.z, this.z) == 0;
         }
      } else {
         return false;
      }
   }

   public int hashCode() {
      int i = Float.floatToIntBits(this.x);
      i = 31 * i + Float.floatToIntBits(this.y);
      return 31 * i + Float.floatToIntBits(this.z);
   }

   public float x() {
      return this.x;
   }

   public float y() {
      return this.y;
   }

   public float z() {
      return this.z;
   }

   public void mul(float pMultiplier) {
      this.x *= pMultiplier;
      this.y *= pMultiplier;
      this.z *= pMultiplier;
   }

   public void mul(float pMx, float pMy, float pMz) {
      this.x *= pMx;
      this.y *= pMy;
      this.z *= pMz;
   }

   public void clamp(Vector3f pMin, Vector3f pMax) {
      this.x = Mth.clamp(this.x, pMin.x(), pMax.x());
      this.y = Mth.clamp(this.y, pMin.x(), pMax.y());
      this.z = Mth.clamp(this.z, pMin.z(), pMax.z());
   }

   public void clamp(float pMin, float pMax) {
      this.x = Mth.clamp(this.x, pMin, pMax);
      this.y = Mth.clamp(this.y, pMin, pMax);
      this.z = Mth.clamp(this.z, pMin, pMax);
   }

   public void set(float pX, float pY, float pZ) {
      this.x = pX;
      this.y = pY;
      this.z = pZ;
   }

   public void load(Vector3f pOther) {
      this.x = pOther.x;
      this.y = pOther.y;
      this.z = pOther.z;
   }

   public void add(float pX, float pY, float pZ) {
      this.x += pX;
      this.y += pY;
      this.z += pZ;
   }

   public void add(Vector3f pOther) {
      this.x += pOther.x;
      this.y += pOther.y;
      this.z += pOther.z;
   }

   public void sub(Vector3f pOther) {
      this.x -= pOther.x;
      this.y -= pOther.y;
      this.z -= pOther.z;
   }

   public float dot(Vector3f pOther) {
      return this.x * pOther.x + this.y * pOther.y + this.z * pOther.z;
   }

   public boolean normalize() {
      float f = this.x * this.x + this.y * this.y + this.z * this.z;
      if (f < Float.MIN_NORMAL) { //Forge: Fix MC-239212
         return false;
      } else {
         float f1 = Mth.fastInvSqrt(f);
         this.x *= f1;
         this.y *= f1;
         this.z *= f1;
         return true;
      }
   }

   public void cross(Vector3f pOther) {
      float f = this.x;
      float f1 = this.y;
      float f2 = this.z;
      float f3 = pOther.x();
      float f4 = pOther.y();
      float f5 = pOther.z();
      this.x = f1 * f5 - f2 * f4;
      this.y = f2 * f3 - f * f5;
      this.z = f * f4 - f1 * f3;
   }

   public void transform(Matrix3f pMatrix) {
      float f = this.x;
      float f1 = this.y;
      float f2 = this.z;
      this.x = pMatrix.m00 * f + pMatrix.m01 * f1 + pMatrix.m02 * f2;
      this.y = pMatrix.m10 * f + pMatrix.m11 * f1 + pMatrix.m12 * f2;
      this.z = pMatrix.m20 * f + pMatrix.m21 * f1 + pMatrix.m22 * f2;
   }

   public void transform(Quaternion pQuaternion) {
      Quaternion quaternion = new Quaternion(pQuaternion);
      quaternion.mul(new Quaternion(this.x(), this.y(), this.z(), 0.0F));
      Quaternion quaternion1 = new Quaternion(pQuaternion);
      quaternion1.conj();
      quaternion.mul(quaternion1);
      this.set(quaternion.i(), quaternion.j(), quaternion.k());
   }

   public void lerp(Vector3f pVector, float pDelta) {
      float f = 1.0F - pDelta;
      this.x = this.x * f + pVector.x * pDelta;
      this.y = this.y * f + pVector.y * pDelta;
      this.z = this.z * f + pVector.z * pDelta;
   }

   public Quaternion rotation(float pValue) {
      return new Quaternion(this, pValue, false);
   }

   public Quaternion rotationDegrees(float pValue) {
      return new Quaternion(this, pValue, true);
   }

   public Vector3f copy() {
      return new Vector3f(this.x, this.y, this.z);
   }

   public void map(Float2FloatFunction pMapper) {
      this.x = pMapper.get(this.x);
      this.y = pMapper.get(this.y);
      this.z = pMapper.get(this.z);
   }

   public String toString() {
      return "[" + this.x + ", " + this.y + ", " + this.z + "]";
   }

    public Vector3f(float[] values) {
        set(values);
    }
    public void set(float[] values) {
        this.x = values[0];
        this.y = values[1];
        this.z = values[2];
    }
    public void setX(float x) { this.x = x; }
    public void setY(float y) { this.y = y; }
    public void setZ(float z) { this.z = z; }
}
