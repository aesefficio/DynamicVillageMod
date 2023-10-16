package com.mojang.math;

/**
 * A mutable, three dimensional vector with double floating point precision.
 */
public class Vector3d {
   public double x;
   public double y;
   public double z;

   public Vector3d(double pX, double pY, double pZ) {
      this.x = pX;
      this.y = pY;
      this.z = pZ;
   }

   public void set(Vector3d pOther) {
      this.x = pOther.x;
      this.y = pOther.y;
      this.z = pOther.z;
   }

   public void set(double pX, double pY, double pZ) {
      this.x = pX;
      this.y = pY;
      this.z = pZ;
   }

   public void scale(double pScale) {
      this.x *= pScale;
      this.y *= pScale;
      this.z *= pScale;
   }

   public void add(Vector3d pOther) {
      this.x += pOther.x;
      this.y += pOther.y;
      this.z += pOther.z;
   }
}