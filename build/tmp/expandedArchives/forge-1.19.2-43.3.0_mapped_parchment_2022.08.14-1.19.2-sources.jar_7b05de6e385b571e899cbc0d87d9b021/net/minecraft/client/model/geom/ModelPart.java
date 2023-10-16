package net.minecraft.client.model.geom;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import com.mojang.math.Vector4f;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Stream;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ModelPart {
   public static final float DEFAULT_SCALE = 1.0F;
   public float x;
   public float y;
   public float z;
   public float xRot;
   public float yRot;
   public float zRot;
   public float xScale = 1.0F;
   public float yScale = 1.0F;
   public float zScale = 1.0F;
   public boolean visible = true;
   public boolean skipDraw;
   private final List<ModelPart.Cube> cubes;
   private final Map<String, ModelPart> children;
   private PartPose initialPose = PartPose.ZERO;

   public ModelPart(List<ModelPart.Cube> pCubes, Map<String, ModelPart> pChildren) {
      this.cubes = pCubes;
      this.children = pChildren;
   }

   public PartPose storePose() {
      return PartPose.offsetAndRotation(this.x, this.y, this.z, this.xRot, this.yRot, this.zRot);
   }

   public PartPose getInitialPose() {
      return this.initialPose;
   }

   public void setInitialPose(PartPose pInitialPose) {
      this.initialPose = pInitialPose;
   }

   public void resetPose() {
      this.loadPose(this.initialPose);
   }

   public void loadPose(PartPose pPartPose) {
      this.x = pPartPose.x;
      this.y = pPartPose.y;
      this.z = pPartPose.z;
      this.xRot = pPartPose.xRot;
      this.yRot = pPartPose.yRot;
      this.zRot = pPartPose.zRot;
      this.xScale = 1.0F;
      this.yScale = 1.0F;
      this.zScale = 1.0F;
   }

   public void copyFrom(ModelPart pModelPart) {
      this.xScale = pModelPart.xScale;
      this.yScale = pModelPart.yScale;
      this.zScale = pModelPart.zScale;
      this.xRot = pModelPart.xRot;
      this.yRot = pModelPart.yRot;
      this.zRot = pModelPart.zRot;
      this.x = pModelPart.x;
      this.y = pModelPart.y;
      this.z = pModelPart.z;
   }

   public boolean hasChild(String p_233563_) {
      return this.children.containsKey(p_233563_);
   }

   public ModelPart getChild(String pName) {
      ModelPart modelpart = this.children.get(pName);
      if (modelpart == null) {
         throw new NoSuchElementException("Can't find part " + pName);
      } else {
         return modelpart;
      }
   }

   public void setPos(float pX, float pY, float pZ) {
      this.x = pX;
      this.y = pY;
      this.z = pZ;
   }

   public void setRotation(float pXRot, float pYRot, float pZRot) {
      this.xRot = pXRot;
      this.yRot = pYRot;
      this.zRot = pZRot;
   }

   public void render(PoseStack pPoseStack, VertexConsumer pVertexConsumer, int pPackedLight, int pPackedOverlay) {
      this.render(pPoseStack, pVertexConsumer, pPackedLight, pPackedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
   }

   public void render(PoseStack pPoseStack, VertexConsumer pVertexConsumer, int pPackedLight, int pPackedOverlay, float pRed, float pGreen, float pBlue, float pAlpha) {
      if (this.visible) {
         if (!this.cubes.isEmpty() || !this.children.isEmpty()) {
            pPoseStack.pushPose();
            this.translateAndRotate(pPoseStack);
            if (!this.skipDraw) {
               this.compile(pPoseStack.last(), pVertexConsumer, pPackedLight, pPackedOverlay, pRed, pGreen, pBlue, pAlpha);
            }

            for(ModelPart modelpart : this.children.values()) {
               modelpart.render(pPoseStack, pVertexConsumer, pPackedLight, pPackedOverlay, pRed, pGreen, pBlue, pAlpha);
            }

            pPoseStack.popPose();
         }
      }
   }

   public void visit(PoseStack pPoseStack, ModelPart.Visitor pVisitor) {
      this.visit(pPoseStack, pVisitor, "");
   }

   private void visit(PoseStack pPoseStack, ModelPart.Visitor pVisitor, String p_171315_) {
      if (!this.cubes.isEmpty() || !this.children.isEmpty()) {
         pPoseStack.pushPose();
         this.translateAndRotate(pPoseStack);
         PoseStack.Pose posestack$pose = pPoseStack.last();

         for(int i = 0; i < this.cubes.size(); ++i) {
            pVisitor.visit(posestack$pose, p_171315_, i, this.cubes.get(i));
         }

         String s = p_171315_ + "/";
         this.children.forEach((p_171320_, p_171321_) -> {
            p_171321_.visit(pPoseStack, pVisitor, s + p_171320_);
         });
         pPoseStack.popPose();
      }
   }

   public void translateAndRotate(PoseStack pPoseStack) {
      pPoseStack.translate((double)(this.x / 16.0F), (double)(this.y / 16.0F), (double)(this.z / 16.0F));
      if (this.zRot != 0.0F) {
         pPoseStack.mulPose(Vector3f.ZP.rotation(this.zRot));
      }

      if (this.yRot != 0.0F) {
         pPoseStack.mulPose(Vector3f.YP.rotation(this.yRot));
      }

      if (this.xRot != 0.0F) {
         pPoseStack.mulPose(Vector3f.XP.rotation(this.xRot));
      }

      if (this.xScale != 1.0F || this.yScale != 1.0F || this.zScale != 1.0F) {
         pPoseStack.scale(this.xScale, this.yScale, this.zScale);
      }

   }

   private void compile(PoseStack.Pose pPose, VertexConsumer pVertexConsumer, int pPackedLight, int pPackedOverlay, float pRed, float pGreen, float pBlue, float pAlpha) {
      for(ModelPart.Cube modelpart$cube : this.cubes) {
         modelpart$cube.compile(pPose, pVertexConsumer, pPackedLight, pPackedOverlay, pRed, pGreen, pBlue, pAlpha);
      }

   }

   public ModelPart.Cube getRandomCube(RandomSource pRandom) {
      return this.cubes.get(pRandom.nextInt(this.cubes.size()));
   }

   public boolean isEmpty() {
      return this.cubes.isEmpty();
   }

   public void offsetPos(Vector3f p_233565_) {
      this.x += p_233565_.x();
      this.y += p_233565_.y();
      this.z += p_233565_.z();
   }

   public void offsetRotation(Vector3f p_233568_) {
      this.xRot += p_233568_.x();
      this.yRot += p_233568_.y();
      this.zRot += p_233568_.z();
   }

   public void offsetScale(Vector3f p_233571_) {
      this.xScale += p_233571_.x();
      this.yScale += p_233571_.y();
      this.zScale += p_233571_.z();
   }

   public Stream<ModelPart> getAllParts() {
      return Stream.concat(Stream.of(this), this.children.values().stream().flatMap(ModelPart::getAllParts));
   }

   @OnlyIn(Dist.CLIENT)
   public static class Cube {
      private final ModelPart.Polygon[] polygons;
      public final float minX;
      public final float minY;
      public final float minZ;
      public final float maxX;
      public final float maxY;
      public final float maxZ;

      public Cube(int pTexCoordU, int pTexCoordV, float pMinX, float pMinY, float pMinZ, float pDimensionX, float pDimensionY, float pDimensionZ, float pGrowX, float pGrowY, float pGrowZ, boolean pMirror, float pTexWidthScaled, float pTexHeightScaled) {
         this.minX = pMinX;
         this.minY = pMinY;
         this.minZ = pMinZ;
         this.maxX = pMinX + pDimensionX;
         this.maxY = pMinY + pDimensionY;
         this.maxZ = pMinZ + pDimensionZ;
         this.polygons = new ModelPart.Polygon[6];
         float f = pMinX + pDimensionX;
         float f1 = pMinY + pDimensionY;
         float f2 = pMinZ + pDimensionZ;
         pMinX -= pGrowX;
         pMinY -= pGrowY;
         pMinZ -= pGrowZ;
         f += pGrowX;
         f1 += pGrowY;
         f2 += pGrowZ;
         if (pMirror) {
            float f3 = f;
            f = pMinX;
            pMinX = f3;
         }

         ModelPart.Vertex modelpart$vertex7 = new ModelPart.Vertex(pMinX, pMinY, pMinZ, 0.0F, 0.0F);
         ModelPart.Vertex modelpart$vertex = new ModelPart.Vertex(f, pMinY, pMinZ, 0.0F, 8.0F);
         ModelPart.Vertex modelpart$vertex1 = new ModelPart.Vertex(f, f1, pMinZ, 8.0F, 8.0F);
         ModelPart.Vertex modelpart$vertex2 = new ModelPart.Vertex(pMinX, f1, pMinZ, 8.0F, 0.0F);
         ModelPart.Vertex modelpart$vertex3 = new ModelPart.Vertex(pMinX, pMinY, f2, 0.0F, 0.0F);
         ModelPart.Vertex modelpart$vertex4 = new ModelPart.Vertex(f, pMinY, f2, 0.0F, 8.0F);
         ModelPart.Vertex modelpart$vertex5 = new ModelPart.Vertex(f, f1, f2, 8.0F, 8.0F);
         ModelPart.Vertex modelpart$vertex6 = new ModelPart.Vertex(pMinX, f1, f2, 8.0F, 0.0F);
         float f4 = (float)pTexCoordU;
         float f5 = (float)pTexCoordU + pDimensionZ;
         float f6 = (float)pTexCoordU + pDimensionZ + pDimensionX;
         float f7 = (float)pTexCoordU + pDimensionZ + pDimensionX + pDimensionX;
         float f8 = (float)pTexCoordU + pDimensionZ + pDimensionX + pDimensionZ;
         float f9 = (float)pTexCoordU + pDimensionZ + pDimensionX + pDimensionZ + pDimensionX;
         float f10 = (float)pTexCoordV;
         float f11 = (float)pTexCoordV + pDimensionZ;
         float f12 = (float)pTexCoordV + pDimensionZ + pDimensionY;
         this.polygons[2] = new ModelPart.Polygon(new ModelPart.Vertex[]{modelpart$vertex4, modelpart$vertex3, modelpart$vertex7, modelpart$vertex}, f5, f10, f6, f11, pTexWidthScaled, pTexHeightScaled, pMirror, Direction.DOWN);
         this.polygons[3] = new ModelPart.Polygon(new ModelPart.Vertex[]{modelpart$vertex1, modelpart$vertex2, modelpart$vertex6, modelpart$vertex5}, f6, f11, f7, f10, pTexWidthScaled, pTexHeightScaled, pMirror, Direction.UP);
         this.polygons[1] = new ModelPart.Polygon(new ModelPart.Vertex[]{modelpart$vertex7, modelpart$vertex3, modelpart$vertex6, modelpart$vertex2}, f4, f11, f5, f12, pTexWidthScaled, pTexHeightScaled, pMirror, Direction.WEST);
         this.polygons[4] = new ModelPart.Polygon(new ModelPart.Vertex[]{modelpart$vertex, modelpart$vertex7, modelpart$vertex2, modelpart$vertex1}, f5, f11, f6, f12, pTexWidthScaled, pTexHeightScaled, pMirror, Direction.NORTH);
         this.polygons[0] = new ModelPart.Polygon(new ModelPart.Vertex[]{modelpart$vertex4, modelpart$vertex, modelpart$vertex1, modelpart$vertex5}, f6, f11, f8, f12, pTexWidthScaled, pTexHeightScaled, pMirror, Direction.EAST);
         this.polygons[5] = new ModelPart.Polygon(new ModelPart.Vertex[]{modelpart$vertex3, modelpart$vertex4, modelpart$vertex5, modelpart$vertex6}, f8, f11, f9, f12, pTexWidthScaled, pTexHeightScaled, pMirror, Direction.SOUTH);
      }

      public void compile(PoseStack.Pose pPose, VertexConsumer pVertexConsumer, int pPackedLight, int pPackedOverlay, float pRed, float pGreen, float pBlue, float pAlpha) {
         Matrix4f matrix4f = pPose.pose();
         Matrix3f matrix3f = pPose.normal();

         for(ModelPart.Polygon modelpart$polygon : this.polygons) {
            Vector3f vector3f = modelpart$polygon.normal.copy();
            vector3f.transform(matrix3f);
            float f = vector3f.x();
            float f1 = vector3f.y();
            float f2 = vector3f.z();

            for(ModelPart.Vertex modelpart$vertex : modelpart$polygon.vertices) {
               float f3 = modelpart$vertex.pos.x() / 16.0F;
               float f4 = modelpart$vertex.pos.y() / 16.0F;
               float f5 = modelpart$vertex.pos.z() / 16.0F;
               Vector4f vector4f = new Vector4f(f3, f4, f5, 1.0F);
               vector4f.transform(matrix4f);
               pVertexConsumer.vertex(vector4f.x(), vector4f.y(), vector4f.z(), pRed, pGreen, pBlue, pAlpha, modelpart$vertex.u, modelpart$vertex.v, pPackedOverlay, pPackedLight, f, f1, f2);
            }
         }

      }
   }

   @OnlyIn(Dist.CLIENT)
   static class Polygon {
      public final ModelPart.Vertex[] vertices;
      public final Vector3f normal;

      public Polygon(ModelPart.Vertex[] pVertices, float p_104363_, float p_104364_, float p_104365_, float p_104366_, float p_104367_, float p_104368_, boolean p_104369_, Direction pDirection) {
         this.vertices = pVertices;
         float f = 0.0F / p_104367_;
         float f1 = 0.0F / p_104368_;
         pVertices[0] = pVertices[0].remap(p_104365_ / p_104367_ - f, p_104364_ / p_104368_ + f1);
         pVertices[1] = pVertices[1].remap(p_104363_ / p_104367_ + f, p_104364_ / p_104368_ + f1);
         pVertices[2] = pVertices[2].remap(p_104363_ / p_104367_ + f, p_104366_ / p_104368_ - f1);
         pVertices[3] = pVertices[3].remap(p_104365_ / p_104367_ - f, p_104366_ / p_104368_ - f1);
         if (p_104369_) {
            int i = pVertices.length;

            for(int j = 0; j < i / 2; ++j) {
               ModelPart.Vertex modelpart$vertex = pVertices[j];
               pVertices[j] = pVertices[i - 1 - j];
               pVertices[i - 1 - j] = modelpart$vertex;
            }
         }

         this.normal = pDirection.step();
         if (p_104369_) {
            this.normal.mul(-1.0F, 1.0F, 1.0F);
         }

      }
   }

   @OnlyIn(Dist.CLIENT)
   static class Vertex {
      public final Vector3f pos;
      public final float u;
      public final float v;

      public Vertex(float pX, float pY, float pZ, float pU, float pV) {
         this(new Vector3f(pX, pY, pZ), pU, pV);
      }

      public ModelPart.Vertex remap(float pU, float pV) {
         return new ModelPart.Vertex(this.pos, pU, pV);
      }

      public Vertex(Vector3f pPos, float pU, float pV) {
         this.pos = pPos;
         this.u = pU;
         this.v = pV;
      }
   }

   @FunctionalInterface
   @OnlyIn(Dist.CLIENT)
   public interface Visitor {
      void visit(PoseStack.Pose pPose, String pPath, int pIndex, ModelPart.Cube pCube);
   }
}