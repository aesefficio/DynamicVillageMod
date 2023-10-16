package net.minecraft.client.renderer.texture;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.renderer.SpriteCoordinateExpander;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class TextureAtlasSprite implements AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final TextureAtlas atlas;
   private final ResourceLocation name;
   final int width;
   final int height;
   protected final NativeImage[] mainImage;
   @Nullable
   private final TextureAtlasSprite.AnimatedTexture animatedTexture;
   private final int x;
   private final int y;
   private final float u0;
   private final float u1;
   private final float v0;
   private final float v1;

   protected TextureAtlasSprite(TextureAtlas pAtlas, TextureAtlasSprite.Info pSpriteInfo, int pMipLevel, int pStorageX, int pStorageY, int pX, int pY, NativeImage pImage) {
      this.atlas = pAtlas;
      this.width = pSpriteInfo.width;
      this.height = pSpriteInfo.height;
      this.name = pSpriteInfo.name;
      this.x = pX;
      this.y = pY;
      this.u0 = (float)pX / (float)pStorageX;
      this.u1 = (float)(pX + this.width) / (float)pStorageX;
      this.v0 = (float)pY / (float)pStorageY;
      this.v1 = (float)(pY + this.height) / (float)pStorageY;
      this.animatedTexture = this.createTicker(pSpriteInfo, pImage.getWidth(), pImage.getHeight(), pMipLevel);

      try {
         try {
            this.mainImage = MipmapGenerator.generateMipLevels(pImage, pMipLevel);
         } catch (Throwable throwable) {
            CrashReport crashreport1 = CrashReport.forThrowable(throwable, "Generating mipmaps for frame");
            CrashReportCategory crashreportcategory1 = crashreport1.addCategory("Frame being iterated");
            crashreportcategory1.setDetail("First frame", () -> {
               StringBuilder stringbuilder = new StringBuilder();
               if (stringbuilder.length() > 0) {
                  stringbuilder.append(", ");
               }

               stringbuilder.append(pImage.getWidth()).append("x").append(pImage.getHeight());
               return stringbuilder.toString();
            });
            throw new ReportedException(crashreport1);
         }
      } catch (Throwable throwable1) {
         CrashReport crashreport = CrashReport.forThrowable(throwable1, "Applying mipmap");
         CrashReportCategory crashreportcategory = crashreport.addCategory("Sprite being mipmapped");
         crashreportcategory.setDetail("Sprite name", this.name::toString);
         crashreportcategory.setDetail("Sprite size", () -> {
            return this.width + " x " + this.height;
         });
         crashreportcategory.setDetail("Sprite frames", () -> {
            return this.getFrameCount() + " frames";
         });
         crashreportcategory.setDetail("Mipmap levels", pMipLevel);
         throw new ReportedException(crashreport);
      }
   }

   public int getFrameCount() {
      return this.animatedTexture != null ? this.animatedTexture.frames.size() : 1;
   }

   @Nullable
   private TextureAtlasSprite.AnimatedTexture createTicker(TextureAtlasSprite.Info pSpriteInfo, int pImageWidth, int pImageHeight, int pMipLevel) {
      AnimationMetadataSection animationmetadatasection = pSpriteInfo.metadata;
      int i = pImageWidth / animationmetadatasection.getFrameWidth(pSpriteInfo.width);
      int j = pImageHeight / animationmetadatasection.getFrameHeight(pSpriteInfo.height);
      int k = i * j;
      List<TextureAtlasSprite.FrameInfo> list = Lists.newArrayList();
      animationmetadatasection.forEachFrame((p_174739_, p_174740_) -> {
         list.add(new TextureAtlasSprite.FrameInfo(p_174739_, p_174740_));
      });
      if (list.isEmpty()) {
         for(int l = 0; l < k; ++l) {
            list.add(new TextureAtlasSprite.FrameInfo(l, animationmetadatasection.getDefaultFrameTime()));
         }
      } else {
         int i1 = 0;
         IntSet intset = new IntOpenHashSet();

         for(Iterator<TextureAtlasSprite.FrameInfo> iterator = list.iterator(); iterator.hasNext(); ++i1) {
            TextureAtlasSprite.FrameInfo textureatlassprite$frameinfo = iterator.next();
            boolean flag = true;
            if (textureatlassprite$frameinfo.time <= 0) {
               LOGGER.warn("Invalid frame duration on sprite {} frame {}: {}", this.name, i1, textureatlassprite$frameinfo.time);
               flag = false;
            }

            if (textureatlassprite$frameinfo.index < 0 || textureatlassprite$frameinfo.index >= k) {
               LOGGER.warn("Invalid frame index on sprite {} frame {}: {}", this.name, i1, textureatlassprite$frameinfo.index);
               flag = false;
            }

            if (flag) {
               intset.add(textureatlassprite$frameinfo.index);
            } else {
               iterator.remove();
            }
         }

         int[] aint = IntStream.range(0, k).filter((p_174736_) -> {
            return !intset.contains(p_174736_);
         }).toArray();
         if (aint.length > 0) {
            LOGGER.warn("Unused frames in sprite {}: {}", this.name, Arrays.toString(aint));
         }
      }

      if (list.size() <= 1) {
         return null;
      } else {
         TextureAtlasSprite.InterpolationData textureatlassprite$interpolationdata = animationmetadatasection.isInterpolatedFrames() ? new TextureAtlasSprite.InterpolationData(pSpriteInfo, pMipLevel) : null;
         return new TextureAtlasSprite.AnimatedTexture(ImmutableList.copyOf(list), i, textureatlassprite$interpolationdata);
      }
   }

   void upload(int pXOffset, int pYOffset, NativeImage[] pFrames) {
      for(int i = 0; i < this.mainImage.length; ++i) {
         if ((this.width >> i <= 0) || (this.height >> i <= 0)) break;
         pFrames[i].upload(i, this.x >> i, this.y >> i, pXOffset >> i, pYOffset >> i, this.width >> i, this.height >> i, this.mainImage.length > 1, false);
      }

   }

   public int getX() {
      return this.x;
   }

   public int getY() {
      return this.y;
   }

   /**
    * @return the width of this sprite, in pixels
    */
   public int getWidth() {
      return this.width;
   }

   /**
    * @return the height of this sprite, in pixels
    */
   public int getHeight() {
      return this.height;
   }

   /**
    * @return the minimum U coordinate to use when rendering this sprite
    */
   public float getU0() {
      return this.u0;
   }

   /**
    * @return the maximum U coordinate to use when rendering this sprite
    */
   public float getU1() {
      return this.u1;
   }

   /**
    * @return the specified {@code u} coordinate relative to this sprite
    */
   public float getU(double pU) {
      float f = this.u1 - this.u0;
      return this.u0 + f * (float)pU / 16.0F;
   }

   public float getUOffset(float pOffset) {
      float f = this.u1 - this.u0;
      return (pOffset - this.u0) / f * 16.0F;
   }

   /**
    * @return the minimum V coordinate to use when rendering this sprite
    */
   public float getV0() {
      return this.v0;
   }

   /**
    * @return the maximum V coordinate to use when rendering this sprite
    */
   public float getV1() {
      return this.v1;
   }

   /**
    * @return the specified {@code v} coordinate relative to this sprite
    */
   public float getV(double pV) {
      float f = this.v1 - this.v0;
      return this.v0 + f * (float)pV / 16.0F;
   }

   public float getVOffset(float pOffset) {
      float f = this.v1 - this.v0;
      return (pOffset - this.v0) / f * 16.0F;
   }

   public ResourceLocation getName() {
      return this.name;
   }

   public TextureAtlas atlas() {
      return this.atlas;
   }

   public IntStream getUniqueFrames() {
      return this.animatedTexture != null ? this.animatedTexture.getUniqueFrames() : IntStream.of(1);
   }

   public void close() {
      for(NativeImage nativeimage : this.mainImage) {
         if (nativeimage != null) {
            nativeimage.close();
         }
      }

      if (this.animatedTexture != null) {
         this.animatedTexture.close();
      }

   }

   public String toString() {
      return "TextureAtlasSprite{name='" + this.name + "', frameCount=" + this.getFrameCount() + ", x=" + this.x + ", y=" + this.y + ", height=" + this.height + ", width=" + this.width + ", u0=" + this.u0 + ", u1=" + this.u1 + ", v0=" + this.v0 + ", v1=" + this.v1 + "}";
   }

   public boolean isTransparent(int pFrameIndex, int pPixelX, int pPixelY) {
      int i = pPixelX;
      int j = pPixelY;
      if (this.animatedTexture != null) {
         i = pPixelX + this.animatedTexture.getFrameX(pFrameIndex) * this.width;
         j = pPixelY + this.animatedTexture.getFrameY(pFrameIndex) * this.height;
      }

      return (this.mainImage[0].getPixelRGBA(i, j) >> 24 & 255) == 0;
   }

   public void uploadFirstFrame() {
      if (this.animatedTexture != null) {
         this.animatedTexture.uploadFirstFrame();
      } else {
         this.upload(0, 0, this.mainImage);
      }

   }

   private float atlasSize() {
      float f = (float)this.width / (this.u1 - this.u0);
      float f1 = (float)this.height / (this.v1 - this.v0);
      return Math.max(f1, f);
   }

   public float uvShrinkRatio() {
      return 4.0F / this.atlasSize();
   }

   @Nullable
   public Tickable getAnimationTicker() {
      return this.animatedTexture;
   }

   public VertexConsumer wrap(VertexConsumer pConsumer) {
      return new SpriteCoordinateExpander(pConsumer, this);
   }

   @OnlyIn(Dist.CLIENT)
   class AnimatedTexture implements Tickable, AutoCloseable {
      int frame;
      int subFrame;
      final List<TextureAtlasSprite.FrameInfo> frames;
      private final int frameRowSize;
      @Nullable
      private final TextureAtlasSprite.InterpolationData interpolationData;

      AnimatedTexture(List<TextureAtlasSprite.FrameInfo> pFrames, @Nullable int pFrameRowSize, TextureAtlasSprite.InterpolationData pInterpolationData) {
         this.frames = pFrames;
         this.frameRowSize = pFrameRowSize;
         this.interpolationData = pInterpolationData;
      }

      int getFrameX(int pFrameIndex) {
         return pFrameIndex % this.frameRowSize;
      }

      int getFrameY(int pFrameIndex) {
         return pFrameIndex / this.frameRowSize;
      }

      private void uploadFrame(int pFrameIndex) {
         int i = this.getFrameX(pFrameIndex) * TextureAtlasSprite.this.width;
         int j = this.getFrameY(pFrameIndex) * TextureAtlasSprite.this.height;
         TextureAtlasSprite.this.upload(i, j, TextureAtlasSprite.this.mainImage);
      }

      public void close() {
         if (this.interpolationData != null) {
            this.interpolationData.close();
         }

      }

      public void tick() {
         ++this.subFrame;
         TextureAtlasSprite.FrameInfo textureatlassprite$frameinfo = this.frames.get(this.frame);
         if (this.subFrame >= textureatlassprite$frameinfo.time) {
            int i = textureatlassprite$frameinfo.index;
            this.frame = (this.frame + 1) % this.frames.size();
            this.subFrame = 0;
            int j = (this.frames.get(this.frame)).index;
            if (i != j) {
               this.uploadFrame(j);
            }
         } else if (this.interpolationData != null) {
            if (!RenderSystem.isOnRenderThread()) {
               RenderSystem.recordRenderCall(() -> {
                  this.interpolationData.uploadInterpolatedFrame(this);
               });
            } else {
               this.interpolationData.uploadInterpolatedFrame(this);
            }
         }

      }

      public void uploadFirstFrame() {
         this.uploadFrame((this.frames.get(0)).index);
      }

      public IntStream getUniqueFrames() {
         return this.frames.stream().mapToInt((p_174762_) -> {
            return p_174762_.index;
         }).distinct();
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class FrameInfo {
      final int index;
      final int time;

      FrameInfo(int pIndex, int pTime) {
         this.index = pIndex;
         this.time = pTime;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static final class Info {
      final ResourceLocation name;
      final int width;
      final int height;
      final AnimationMetadataSection metadata;

      public Info(ResourceLocation pName, int pWidth, int pHeight, AnimationMetadataSection pMetadata) {
         this.name = pName;
         this.width = pWidth;
         this.height = pHeight;
         this.metadata = pMetadata;
      }

      public ResourceLocation name() {
         return this.name;
      }

      public int width() {
         return this.width;
      }

      public int height() {
         return this.height;
      }
   }

   @OnlyIn(Dist.CLIENT)
   final class InterpolationData implements AutoCloseable {
      private final NativeImage[] activeFrame;

      InterpolationData(TextureAtlasSprite.Info pSpriteInfo, int pMipLevel) {
         this.activeFrame = new NativeImage[pMipLevel + 1];

         for(int i = 0; i < this.activeFrame.length; ++i) {
            int j = pSpriteInfo.width >> i;
            int k = pSpriteInfo.height >> i;
            if (this.activeFrame[i] == null) {
               // Forge: guard against invalid texture size, because we allow generating mipmaps regardless of texture sizes
               this.activeFrame[i] = new NativeImage(Math.max(1, j), Math.max(1, k), false);
            }
         }

      }

      void uploadInterpolatedFrame(TextureAtlasSprite.AnimatedTexture pTexture) {
         TextureAtlasSprite.FrameInfo textureatlassprite$frameinfo = pTexture.frames.get(pTexture.frame);
         double d0 = 1.0D - (double)pTexture.subFrame / (double)textureatlassprite$frameinfo.time;
         int i = textureatlassprite$frameinfo.index;
         int j = (pTexture.frames.get((pTexture.frame + 1) % pTexture.frames.size())).index;
         if (i != j) {
            for(int k = 0; k < this.activeFrame.length; ++k) {
               int l = TextureAtlasSprite.this.width >> k;
               int i1 = TextureAtlasSprite.this.height >> k;
               // Forge: guard against invalid texture size, because we allow generating mipmaps regardless of texture sizes
               if (l == 0 || i1 == 0) continue;

               for(int j1 = 0; j1 < i1; ++j1) {
                  for(int k1 = 0; k1 < l; ++k1) {
                     int l1 = this.getPixel(pTexture, i, k, k1, j1);
                     int i2 = this.getPixel(pTexture, j, k, k1, j1);
                     int j2 = this.mix(d0, l1 >> 16 & 255, i2 >> 16 & 255);
                     int k2 = this.mix(d0, l1 >> 8 & 255, i2 >> 8 & 255);
                     int l2 = this.mix(d0, l1 & 255, i2 & 255);
                     this.activeFrame[k].setPixelRGBA(k1, j1, l1 & -16777216 | j2 << 16 | k2 << 8 | l2);
                  }
               }
            }

            TextureAtlasSprite.this.upload(0, 0, this.activeFrame);
         }

      }

      private int getPixel(TextureAtlasSprite.AnimatedTexture pTexture, int pFrameIndex, int pMipLevel, int pX, int pY) {
         return TextureAtlasSprite.this.mainImage[pMipLevel].getPixelRGBA(pX + (pTexture.getFrameX(pFrameIndex) * TextureAtlasSprite.this.width >> pMipLevel), pY + (pTexture.getFrameY(pFrameIndex) * TextureAtlasSprite.this.height >> pMipLevel));
      }

      /**
       * Mixes the {@code first} and {@code second} numbers in the {@code ratio}. Used to mix the colors of two pixels
       * from adjacent frames in order to calculate the interpolated color value.
       * This is done by multiplying {@code first} by the ratio, {@code second} by the inverse of the ratio, and summing
       * the results.
       */
      private int mix(double pRatio, int pFirst, int pSecond) {
         return (int)(pRatio * (double)pFirst + (1.0D - pRatio) * (double)pSecond);
      }

      public void close() {
         for(NativeImage nativeimage : this.activeFrame) {
            if (nativeimage != null) {
               nativeimage.close();
            }
         }

      }
   }

   // Forge Start
   public int getPixelRGBA(int frameIndex, int x, int y) {
       if (this.animatedTexture != null) {
           x += this.animatedTexture.getFrameX(frameIndex) * this.width;
           y += this.animatedTexture.getFrameY(frameIndex) * this.height;
       }

       return this.mainImage[0].getPixelRGBA(x, y);
   }
}
