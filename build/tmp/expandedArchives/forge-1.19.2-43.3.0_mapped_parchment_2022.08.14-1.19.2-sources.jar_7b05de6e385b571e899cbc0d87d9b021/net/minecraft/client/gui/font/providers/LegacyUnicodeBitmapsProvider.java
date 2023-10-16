package net.minecraft.client.gui.font.providers;

import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.SheetGlyphInfo;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.IllegalFormatException;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class LegacyUnicodeBitmapsProvider implements GlyphProvider {
   static final Logger LOGGER = LogUtils.getLogger();
   private static final int UNICODE_SHEETS = 256;
   private static final int CODEPOINTS_PER_SHEET = 256;
   private static final int TEXTURE_SIZE = 256;
   private static final byte NO_GLYPH = 0;
   private final ResourceManager resourceManager;
   private final byte[] sizes;
   private final String texturePattern;
   private final Map<ResourceLocation, NativeImage> textures = Maps.newHashMap();

   public LegacyUnicodeBitmapsProvider(ResourceManager pResourceManager, byte[] pSizes, String pTexturePattern) {
      this.resourceManager = pResourceManager;
      this.sizes = pSizes;
      this.texturePattern = pTexturePattern;

      for(int i = 0; i < 256; ++i) {
         int j = i * 256;
         ResourceLocation resourcelocation = this.getSheetLocation(j);

         try {
            InputStream inputstream = this.resourceManager.open(resourcelocation);

            label90: {
               try {
                  NativeImage nativeimage;
                  label104: {
                     nativeimage = NativeImage.read(NativeImage.Format.RGBA, inputstream);

                     try {
                        if (nativeimage.getWidth() == 256 && nativeimage.getHeight() == 256) {
                           int k = 0;

                           while(true) {
                              if (k >= 256) {
                                 break label104;
                              }

                              byte b0 = pSizes[j + k];
                              if (b0 != 0 && getLeft(b0) > getRight(b0)) {
                                 pSizes[j + k] = 0;
                              }

                              ++k;
                           }
                        }
                     } catch (Throwable throwable2) {
                        if (nativeimage != null) {
                           try {
                              nativeimage.close();
                           } catch (Throwable throwable1) {
                              throwable2.addSuppressed(throwable1);
                           }
                        }

                        throw throwable2;
                     }

                     if (nativeimage != null) {
                        nativeimage.close();
                     }
                     break label90;
                  }

                  if (nativeimage != null) {
                     nativeimage.close();
                  }
               } catch (Throwable throwable3) {
                  if (inputstream != null) {
                     try {
                        inputstream.close();
                     } catch (Throwable throwable) {
                        throwable3.addSuppressed(throwable);
                     }
                  }

                  throw throwable3;
               }

               if (inputstream != null) {
                  inputstream.close();
               }
               continue;
            }

            if (inputstream != null) {
               inputstream.close();
            }
         } catch (IOException ioexception) {
         }

         Arrays.fill(pSizes, j, j + 256, (byte)0);
      }

   }

   public void close() {
      this.textures.values().forEach(NativeImage::close);
   }

   private ResourceLocation getSheetLocation(int p_95443_) {
      ResourceLocation resourcelocation = new ResourceLocation(String.format(Locale.ROOT, this.texturePattern, String.format(Locale.ROOT, "%02x", p_95443_ / 256)));
      return new ResourceLocation(resourcelocation.getNamespace(), "textures/" + resourcelocation.getPath());
   }

   @Nullable
   public GlyphInfo getGlyph(int pCharacter) {
      if (pCharacter >= 0 && pCharacter < this.sizes.length) {
         byte b0 = this.sizes[pCharacter];
         if (b0 != 0) {
            NativeImage nativeimage = this.textures.computeIfAbsent(this.getSheetLocation(pCharacter), this::loadTexture);
            if (nativeimage != null) {
               int i = getLeft(b0);
               return new LegacyUnicodeBitmapsProvider.Glyph(pCharacter % 16 * 16 + i, (pCharacter & 255) / 16 * 16, getRight(b0) - i, 16, nativeimage);
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public IntSet getSupportedGlyphs() {
      IntSet intset = new IntOpenHashSet();

      for(int i = 0; i < this.sizes.length; ++i) {
         if (this.sizes[i] != 0) {
            intset.add(i);
         }
      }

      return intset;
   }

   @Nullable
   private NativeImage loadTexture(ResourceLocation p_95438_) {
      try {
         InputStream inputstream = this.resourceManager.open(p_95438_);

         NativeImage nativeimage;
         try {
            nativeimage = NativeImage.read(NativeImage.Format.RGBA, inputstream);
         } catch (Throwable throwable1) {
            if (inputstream != null) {
               try {
                  inputstream.close();
               } catch (Throwable throwable) {
                  throwable1.addSuppressed(throwable);
               }
            }

            throw throwable1;
         }

         if (inputstream != null) {
            inputstream.close();
         }

         return nativeimage;
      } catch (IOException ioexception) {
         LOGGER.error("Couldn't load texture {}", p_95438_, ioexception);
         return null;
      }
   }

   private static int getLeft(byte p_95434_) {
      return p_95434_ >> 4 & 15;
   }

   private static int getRight(byte p_95441_) {
      return (p_95441_ & 15) + 1;
   }

   @OnlyIn(Dist.CLIENT)
   public static class Builder implements GlyphProviderBuilder {
      private final ResourceLocation metadata;
      private final String texturePattern;

      public Builder(ResourceLocation pMetadata, String pTexturePattern) {
         this.metadata = pMetadata;
         this.texturePattern = pTexturePattern;
      }

      public static GlyphProviderBuilder fromJson(JsonObject pJson) {
         return new LegacyUnicodeBitmapsProvider.Builder(new ResourceLocation(GsonHelper.getAsString(pJson, "sizes")), getTemplate(pJson));
      }

      private static String getTemplate(JsonObject pJson) {
         String s = GsonHelper.getAsString(pJson, "template");

         try {
            String.format(Locale.ROOT, s, "");
            return s;
         } catch (IllegalFormatException illegalformatexception) {
            throw new JsonParseException("Invalid legacy unicode template supplied, expected single '%s': " + s);
         }
      }

      @Nullable
      public GlyphProvider create(ResourceManager pResourceManager) {
         try {
            InputStream inputstream = Minecraft.getInstance().getResourceManager().open(this.metadata);

            LegacyUnicodeBitmapsProvider legacyunicodebitmapsprovider;
            try {
               byte[] abyte = inputstream.readNBytes(65536);
               legacyunicodebitmapsprovider = new LegacyUnicodeBitmapsProvider(pResourceManager, abyte, this.texturePattern);
            } catch (Throwable throwable1) {
               if (inputstream != null) {
                  try {
                     inputstream.close();
                  } catch (Throwable throwable) {
                     throwable1.addSuppressed(throwable);
                  }
               }

               throw throwable1;
            }

            if (inputstream != null) {
               inputstream.close();
            }

            return legacyunicodebitmapsprovider;
         } catch (IOException ioexception) {
            LegacyUnicodeBitmapsProvider.LOGGER.error("Cannot load {}, unicode glyphs will not render correctly", (Object)this.metadata);
            return null;
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   static record Glyph(int sourceX, int sourceY, int width, int height, NativeImage source) implements GlyphInfo {
      public float getAdvance() {
         return (float)(this.width / 2 + 1);
      }

      public float getShadowOffset() {
         return 0.5F;
      }

      public float getBoldOffset() {
         return 0.5F;
      }

      public BakedGlyph bake(Function<SheetGlyphInfo, BakedGlyph> p_232670_) {
         return p_232670_.apply(new SheetGlyphInfo() {
            public float getOversample() {
               return 2.0F;
            }

            public int getPixelWidth() {
               return Glyph.this.width;
            }

            public int getPixelHeight() {
               return Glyph.this.height;
            }

            public void upload(int p_232685_, int p_232686_) {
               Glyph.this.source.upload(0, p_232685_, p_232686_, Glyph.this.sourceX, Glyph.this.sourceY, Glyph.this.width, Glyph.this.height, false, false);
            }

            public boolean isColored() {
               return Glyph.this.source.format().components() > 1;
            }
         });
      }
   }
}