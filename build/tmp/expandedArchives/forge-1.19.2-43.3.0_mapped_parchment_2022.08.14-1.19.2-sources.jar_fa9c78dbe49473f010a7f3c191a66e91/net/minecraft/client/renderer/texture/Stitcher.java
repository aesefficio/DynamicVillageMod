package net.minecraft.client.renderer.texture;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class Stitcher {
   private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

   private static final Comparator<Stitcher.Holder> HOLDER_COMPARATOR = Comparator.<Stitcher.Holder, Integer>comparing((p_118201_) -> {
      return -p_118201_.height;
   }).thenComparing((p_118199_) -> {
      return -p_118199_.width;
   }).thenComparing((p_118197_) -> {
      return p_118197_.spriteInfo.name();
   });
   private final int mipLevel;
   private final Set<Stitcher.Holder> texturesToBeStitched = Sets.newHashSetWithExpectedSize(256);
   private final List<Stitcher.Region> storage = Lists.newArrayListWithCapacity(256);
   private int storageX;
   private int storageY;
   private final int maxWidth;
   private final int maxHeight;

   public Stitcher(int pMaxWidth, int pMaxHeight, int pMipLevel) {
      this.mipLevel = pMipLevel;
      this.maxWidth = pMaxWidth;
      this.maxHeight = pMaxHeight;
   }

   public int getWidth() {
      return this.storageX;
   }

   public int getHeight() {
      return this.storageY;
   }

   public void registerSprite(TextureAtlasSprite.Info pSpriteInfo) {
      Stitcher.Holder stitcher$holder = new Stitcher.Holder(pSpriteInfo, this.mipLevel);
      this.texturesToBeStitched.add(stitcher$holder);
   }

   public void stitch() {
      List<Stitcher.Holder> list = Lists.newArrayList(this.texturesToBeStitched);
      list.sort(HOLDER_COMPARATOR);

      for(Stitcher.Holder stitcher$holder : list) {
         if (!this.addToStorage(stitcher$holder)) {
            if (LOGGER.isInfoEnabled()) {
               StringBuilder sb = new StringBuilder();
               sb.append("Unable to fit: ").append(stitcher$holder.spriteInfo.name());
               sb.append(" - size: ").append(stitcher$holder.spriteInfo.width()).append("x").append(stitcher$holder.spriteInfo.height());
               sb.append(" - Maybe try a lower resolution resourcepack?\n");
               list.forEach(h -> sb.append("\t").append(h).append("\n"));
               LOGGER.info(sb.toString());
            }
            throw new StitcherException(stitcher$holder.spriteInfo, list.stream().map((p_118195_) -> {
               return p_118195_.spriteInfo;
            }).collect(ImmutableList.toImmutableList()));
         }
      }

      this.storageX = Mth.smallestEncompassingPowerOfTwo(this.storageX);
      this.storageY = Mth.smallestEncompassingPowerOfTwo(this.storageY);
   }

   public void gatherSprites(Stitcher.SpriteLoader pLoader) {
      for(Stitcher.Region stitcher$region : this.storage) {
         stitcher$region.walk((p_118184_) -> {
            Stitcher.Holder stitcher$holder = p_118184_.getHolder();
            TextureAtlasSprite.Info textureatlassprite$info = stitcher$holder.spriteInfo;
            pLoader.load(textureatlassprite$info, this.storageX, this.storageY, p_118184_.getX(), p_118184_.getY());
         });
      }

   }

   static int smallestFittingMinTexel(int pDimension, int pMipLevel) {
      return (pDimension >> pMipLevel) + ((pDimension & (1 << pMipLevel) - 1) == 0 ? 0 : 1) << pMipLevel;
   }

   /**
    * Attempts to find space for specified {@code holder}.
    * 
    * @return {@code true} if there was space; {@code false} otherwise
    */
   private boolean addToStorage(Stitcher.Holder pHolder) {
      for(Stitcher.Region stitcher$region : this.storage) {
         if (stitcher$region.add(pHolder)) {
            return true;
         }
      }

      return this.expand(pHolder);
   }

   /**
    * Attempts to expand stitched texture in order to make space for specified {@code holder}.
    * 
    * @return {@code true} if there was enough space to expand the texture; {@code false} otherwise
    */
   private boolean expand(Stitcher.Holder pHolder) {
      int i = Mth.smallestEncompassingPowerOfTwo(this.storageX);
      int j = Mth.smallestEncompassingPowerOfTwo(this.storageY);
      int k = Mth.smallestEncompassingPowerOfTwo(this.storageX + pHolder.width);
      int l = Mth.smallestEncompassingPowerOfTwo(this.storageY + pHolder.height);
      boolean flag1 = k <= this.maxWidth;
      boolean flag2 = l <= this.maxHeight;
      if (!flag1 && !flag2) {
         return false;
      } else {
         boolean flag3 = flag1 && i != k;
         boolean flag4 = flag2 && j != l;
         boolean flag;
         if (flag3 ^ flag4) {
            flag = !flag3 && flag1; // Forge: Fix stitcher not expanding entire height before growing width, and (potentially) growing larger then the max size.
         } else {
            flag = flag1 && i <= j;
         }

         Stitcher.Region stitcher$region;
         if (flag) {
            if (this.storageY == 0) {
               this.storageY = pHolder.height;
            }

            stitcher$region = new Stitcher.Region(this.storageX, 0, pHolder.width, this.storageY);
            this.storageX += pHolder.width;
         } else {
            stitcher$region = new Stitcher.Region(0, this.storageY, this.storageX, pHolder.height);
            this.storageY += pHolder.height;
         }

         stitcher$region.add(pHolder);
         this.storage.add(stitcher$region);
         return true;
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class Holder {
      public final TextureAtlasSprite.Info spriteInfo;
      public final int width;
      public final int height;

      public Holder(TextureAtlasSprite.Info pSpriteInfo, int pMipLevel) {
         this.spriteInfo = pSpriteInfo;
         this.width = Stitcher.smallestFittingMinTexel(pSpriteInfo.width(), pMipLevel);
         this.height = Stitcher.smallestFittingMinTexel(pSpriteInfo.height(), pMipLevel);
      }

      public String toString() {
         return "Holder{width=" + this.width + ", height=" + this.height + ", name=" + this.spriteInfo.name() + '}';
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class Region {
      private final int originX;
      private final int originY;
      private final int width;
      private final int height;
      private List<Stitcher.Region> subSlots;
      private Stitcher.Holder holder;

      public Region(int pOriginX, int pOriginY, int pWidth, int pHeight) {
         this.originX = pOriginX;
         this.originY = pOriginY;
         this.width = pWidth;
         this.height = pHeight;
      }

      public Stitcher.Holder getHolder() {
         return this.holder;
      }

      public int getX() {
         return this.originX;
      }

      public int getY() {
         return this.originY;
      }

      public boolean add(Stitcher.Holder pHolder) {
         if (this.holder != null) {
            return false;
         } else {
            int i = pHolder.width;
            int j = pHolder.height;
            if (i <= this.width && j <= this.height) {
               if (i == this.width && j == this.height) {
                  this.holder = pHolder;
                  return true;
               } else {
                  if (this.subSlots == null) {
                     this.subSlots = Lists.newArrayListWithCapacity(1);
                     this.subSlots.add(new Stitcher.Region(this.originX, this.originY, i, j));
                     int k = this.width - i;
                     int l = this.height - j;
                     if (l > 0 && k > 0) {
                        int i1 = Math.max(this.height, k);
                        int j1 = Math.max(this.width, l);
                        if (i1 >= j1) {
                           this.subSlots.add(new Stitcher.Region(this.originX, this.originY + j, i, l));
                           this.subSlots.add(new Stitcher.Region(this.originX + i, this.originY, k, this.height));
                        } else {
                           this.subSlots.add(new Stitcher.Region(this.originX + i, this.originY, k, j));
                           this.subSlots.add(new Stitcher.Region(this.originX, this.originY + j, this.width, l));
                        }
                     } else if (k == 0) {
                        this.subSlots.add(new Stitcher.Region(this.originX, this.originY + j, i, l));
                     } else if (l == 0) {
                        this.subSlots.add(new Stitcher.Region(this.originX + i, this.originY, k, j));
                     }
                  }

                  for(Stitcher.Region stitcher$region : this.subSlots) {
                     if (stitcher$region.add(pHolder)) {
                        return true;
                     }
                  }

                  return false;
               }
            } else {
               return false;
            }
         }
      }

      public void walk(Consumer<Stitcher.Region> pConsumer) {
         if (this.holder != null) {
            pConsumer.accept(this);
         } else if (this.subSlots != null) {
            for(Stitcher.Region stitcher$region : this.subSlots) {
               stitcher$region.walk(pConsumer);
            }
         }

      }

      public String toString() {
         return "Slot{originX=" + this.originX + ", originY=" + this.originY + ", width=" + this.width + ", height=" + this.height + ", texture=" + this.holder + ", subSlots=" + this.subSlots + "}";
      }
   }

   @OnlyIn(Dist.CLIENT)
   public interface SpriteLoader {
      void load(TextureAtlasSprite.Info pSpriteInfo, int pStorageX, int pStorageY, int pX, int pY);
   }
}
