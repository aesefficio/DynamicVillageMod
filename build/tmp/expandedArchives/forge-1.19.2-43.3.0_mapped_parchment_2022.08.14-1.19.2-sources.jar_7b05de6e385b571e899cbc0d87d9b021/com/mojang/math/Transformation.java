package com.mojang.math;

import com.mojang.datafixers.util.Pair;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.Util;
import org.apache.commons.lang3.tuple.Triple;

public final class Transformation implements net.minecraftforge.client.extensions.IForgeTransformation {
   private final Matrix4f matrix;
   private boolean decomposed;
   @Nullable
   private Vector3f translation;
   @Nullable
   private Quaternion leftRotation;
   @Nullable
   private Vector3f scale;
   @Nullable
   private Quaternion rightRotation;
   private static final Transformation IDENTITY = Util.make(() -> {
      Matrix4f matrix4f = new Matrix4f();
      matrix4f.setIdentity();
      Transformation transformation = new Transformation(matrix4f);
      transformation.getLeftRotation();
      return transformation;
   });

   public Transformation(@Nullable Matrix4f pMatrix) {
      if (pMatrix == null) {
         this.matrix = IDENTITY.matrix;
      } else {
         this.matrix = pMatrix;
      }

   }

   public Transformation(@Nullable Vector3f pTranslation, @Nullable Quaternion pLeftRotation, @Nullable Vector3f pScale, @Nullable Quaternion pRightRotation) {
      this.matrix = compose(pTranslation, pLeftRotation, pScale, pRightRotation);
      this.translation = pTranslation != null ? pTranslation : new Vector3f();
      this.leftRotation = pLeftRotation != null ? pLeftRotation : Quaternion.ONE.copy();
      this.scale = pScale != null ? pScale : new Vector3f(1.0F, 1.0F, 1.0F);
      this.rightRotation = pRightRotation != null ? pRightRotation : Quaternion.ONE.copy();
      this.decomposed = true;
   }

   public static Transformation identity() {
      return IDENTITY;
   }

   public Transformation compose(Transformation pOther) {
      Matrix4f matrix4f = this.getMatrix();
      matrix4f.multiply(pOther.getMatrix());
      return new Transformation(matrix4f);
   }

   @Nullable
   public Transformation inverse() {
      if (this == IDENTITY) {
         return this;
      } else {
         Matrix4f matrix4f = this.getMatrix();
         return matrix4f.invert() ? new Transformation(matrix4f) : null;
      }
   }

   private void ensureDecomposed() {
      if (!this.decomposed) {
         Pair<Matrix3f, Vector3f> pair = toAffine(this.matrix);
         Triple<Quaternion, Vector3f, Quaternion> triple = pair.getFirst().svdDecompose();
         this.translation = pair.getSecond();
         this.leftRotation = triple.getLeft();
         this.scale = triple.getMiddle();
         this.rightRotation = triple.getRight();
         this.decomposed = true;
      }

   }

   private static Matrix4f compose(@Nullable Vector3f pTranslation, @Nullable Quaternion pLeftRotation, @Nullable Vector3f pScale, @Nullable Quaternion pRightRotation) {
      Matrix4f matrix4f = new Matrix4f();
      matrix4f.setIdentity();
      if (pLeftRotation != null) {
         matrix4f.multiply(new Matrix4f(pLeftRotation));
      }

      if (pScale != null) {
         matrix4f.multiply(Matrix4f.createScaleMatrix(pScale.x(), pScale.y(), pScale.z()));
      }

      if (pRightRotation != null) {
         matrix4f.multiply(new Matrix4f(pRightRotation));
      }

      if (pTranslation != null) {
         matrix4f.m03 = pTranslation.x();
         matrix4f.m13 = pTranslation.y();
         matrix4f.m23 = pTranslation.z();
      }

      return matrix4f;
   }

   public static Pair<Matrix3f, Vector3f> toAffine(Matrix4f pMatrix) {
      pMatrix.multiply(1.0F / pMatrix.m33);
      Vector3f vector3f = new Vector3f(pMatrix.m03, pMatrix.m13, pMatrix.m23);
      Matrix3f matrix3f = new Matrix3f(pMatrix);
      return Pair.of(matrix3f, vector3f);
   }

   public Matrix4f getMatrix() {
      return this.matrix.copy();
   }

   public Vector3f getTranslation() {
      this.ensureDecomposed();
      return this.translation.copy();
   }

   public Quaternion getLeftRotation() {
      this.ensureDecomposed();
      return this.leftRotation.copy();
   }

   public Vector3f getScale() {
      this.ensureDecomposed();
      return this.scale.copy();
   }

   public Quaternion getRightRotation() {
      this.ensureDecomposed();
      return this.rightRotation.copy();
   }

   public boolean equals(Object pOther) {
      if (this == pOther) {
         return true;
      } else if (pOther != null && this.getClass() == pOther.getClass()) {
         Transformation transformation = (Transformation)pOther;
         return Objects.equals(this.matrix, transformation.matrix);
      } else {
         return false;
      }
   }

   public int hashCode() {
      return Objects.hash(this.matrix);
   }

    private Matrix3f normalTransform = null;
    public Matrix3f getNormalMatrix() {
        checkNormalTransform();
        return normalTransform;
    }
    private void checkNormalTransform() {
        if (normalTransform == null) {
            normalTransform = new Matrix3f(matrix);
            normalTransform.invert();
            normalTransform.transpose();
        }
    }

   public Transformation slerp(Transformation pTransformation, float pDelta) {
      Vector3f vector3f = this.getTranslation();
      Quaternion quaternion = this.getLeftRotation();
      Vector3f vector3f1 = this.getScale();
      Quaternion quaternion1 = this.getRightRotation();
      vector3f.lerp(pTransformation.getTranslation(), pDelta);
      quaternion.slerp(pTransformation.getLeftRotation(), pDelta);
      vector3f1.lerp(pTransformation.getScale(), pDelta);
      quaternion1.slerp(pTransformation.getRightRotation(), pDelta);
      return new Transformation(vector3f, quaternion, vector3f1, quaternion1);
   }
}
