package net.minecraft.client.model.geom.builders;

import com.mojang.math.Vector3f;
import javax.annotation.Nullable;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class CubeDefinition {
   @Nullable
   private final String comment;
   private final Vector3f origin;
   private final Vector3f dimensions;
   private final CubeDeformation grow;
   private final boolean mirror;
   private final UVPair texCoord;
   private final UVPair texScale;

   protected CubeDefinition(@Nullable String pComment, float pTexCoordU, float pTexCoordV, float pOriginX, float pOriginY, float pOriginZ, float pDimensionX, float pDimensionY, float pDimensionZ, CubeDeformation pGrow, boolean pMirror, float pTexScaleU, float pTexScaleV) {
      this.comment = pComment;
      this.texCoord = new UVPair(pTexCoordU, pTexCoordV);
      this.origin = new Vector3f(pOriginX, pOriginY, pOriginZ);
      this.dimensions = new Vector3f(pDimensionX, pDimensionY, pDimensionZ);
      this.grow = pGrow;
      this.mirror = pMirror;
      this.texScale = new UVPair(pTexScaleU, pTexScaleV);
   }

   public ModelPart.Cube bake(int pTexWidth, int pTexHeight) {
      return new ModelPart.Cube((int)this.texCoord.u(), (int)this.texCoord.v(), this.origin.x(), this.origin.y(), this.origin.z(), this.dimensions.x(), this.dimensions.y(), this.dimensions.z(), this.grow.growX, this.grow.growY, this.grow.growZ, this.mirror, (float)pTexWidth * this.texScale.u(), (float)pTexHeight * this.texScale.v());
   }
}