package net.minecraft.client.renderer.texture;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.PngInfo;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class TextureAtlas extends AbstractTexture implements Tickable {
   private static final Logger LOGGER = LogUtils.getLogger();
   /** @deprecated */
   @Deprecated
   public static final ResourceLocation LOCATION_BLOCKS = InventoryMenu.BLOCK_ATLAS;
   /** @deprecated */
   @Deprecated
   public static final ResourceLocation LOCATION_PARTICLES = new ResourceLocation("textures/atlas/particles.png");
   private static final String FILE_EXTENSION = ".png";
   private final List<Tickable> animatedTextures = Lists.newArrayList();
   private final Set<ResourceLocation> sprites = Sets.newHashSet();
   private final Map<ResourceLocation, TextureAtlasSprite> texturesByName = Maps.newHashMap();
   private final ResourceLocation location;
   private final int maxSupportedTextureSize;

   public TextureAtlas(ResourceLocation pLocation) {
      this.location = pLocation;
      this.maxSupportedTextureSize = RenderSystem.maxSupportedTextureSize();
   }

   public void load(ResourceManager pResourceManager) {
   }

   public void reload(TextureAtlas.Preparations pPreparations) {
      this.sprites.clear();
      this.sprites.addAll(pPreparations.sprites);
      LOGGER.info("Created: {}x{}x{} {}-atlas", pPreparations.width, pPreparations.height, pPreparations.mipLevel, this.location);
      TextureUtil.prepareImage(this.getId(), pPreparations.mipLevel, pPreparations.width, pPreparations.height);
      this.clearTextureData();

      for(TextureAtlasSprite textureatlassprite : pPreparations.regions) {
         this.texturesByName.put(textureatlassprite.getName(), textureatlassprite);

         try {
            textureatlassprite.uploadFirstFrame();
         } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Stitching texture atlas");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Texture being stitched together");
            crashreportcategory.setDetail("Atlas path", this.location);
            crashreportcategory.setDetail("Sprite", textureatlassprite);
            throw new ReportedException(crashreport);
         }

         Tickable tickable = textureatlassprite.getAnimationTicker();
         if (tickable != null) {
            this.animatedTextures.add(tickable);
         }
      }

      net.minecraftforge.client.ForgeHooksClient.onTextureStitchedPost(this);
   }

   public TextureAtlas.Preparations prepareToStitch(ResourceManager pResourceManager, Stream<ResourceLocation> pSpriteNames, ProfilerFiller pProfiler, int pMipLevel) {
      pProfiler.push("preparing");
      Set<ResourceLocation> set = pSpriteNames.peek((p_118327_) -> {
         if (p_118327_ == null) {
            throw new IllegalArgumentException("Location cannot be null!");
         }
      }).collect(Collectors.toSet());
      int i = this.maxSupportedTextureSize;
      Stitcher stitcher = new Stitcher(i, i, pMipLevel);
      int j = Integer.MAX_VALUE;
      int k = 1 << pMipLevel;
      pProfiler.popPush("extracting_frames");
      net.minecraftforge.client.ForgeHooksClient.onTextureStitchedPre(this, set);

      for(TextureAtlasSprite.Info textureatlassprite$info : this.getBasicSpriteInfos(pResourceManager, set)) {
         j = Math.min(j, Math.min(textureatlassprite$info.width(), textureatlassprite$info.height()));
         int l = Math.min(Integer.lowestOneBit(textureatlassprite$info.width()), Integer.lowestOneBit(textureatlassprite$info.height()));
         if (l < k) {
            LOGGER.warn("Texture {} with size {}x{} limits mip level from {} to {}", textureatlassprite$info.name(), textureatlassprite$info.width(), textureatlassprite$info.height(), Mth.log2(k), Mth.log2(l));
            k = l;
         }

         stitcher.registerSprite(textureatlassprite$info);
      }

      int i1 = Math.min(j, k);
      int j1 = Mth.log2(i1);
      int k1 = pMipLevel;
      if (false) // FORGE: do not lower the mipmap level
      if (j1 < pMipLevel) {
         LOGGER.warn("{}: dropping miplevel from {} to {}, because of minimum power of two: {}", this.location, pMipLevel, j1, i1);
         k1 = j1;
      } else {
         k1 = pMipLevel;
      }

      pProfiler.popPush("register");
      stitcher.registerSprite(MissingTextureAtlasSprite.info());
      pProfiler.popPush("stitching");

      try {
         stitcher.stitch();
      } catch (StitcherException stitcherexception) {
         CrashReport crashreport = CrashReport.forThrowable(stitcherexception, "Stitching");
         CrashReportCategory crashreportcategory = crashreport.addCategory("Stitcher");
         crashreportcategory.setDetail("Sprites", stitcherexception.getAllSprites().stream().map((p_118315_) -> {
            return String.format(Locale.ROOT, "%s[%dx%d]", p_118315_.name(), p_118315_.width(), p_118315_.height());
         }).collect(Collectors.joining(",")));
         crashreportcategory.setDetail("Max Texture Size", i);
         throw new ReportedException(crashreport);
      }

      pProfiler.popPush("loading");
      List<TextureAtlasSprite> list = this.getLoadedSprites(pResourceManager, stitcher, k1);
      pProfiler.pop();
      return new TextureAtlas.Preparations(set, stitcher.getWidth(), stitcher.getHeight(), k1, list);
   }

   private Collection<TextureAtlasSprite.Info> getBasicSpriteInfos(ResourceManager pResourceManager, Set<ResourceLocation> pSpriteNames) {
      List<CompletableFuture<?>> list = Lists.newArrayList();
      Queue<TextureAtlasSprite.Info> queue = new ConcurrentLinkedQueue<>();

      for(ResourceLocation resourcelocation : pSpriteNames) {
         if (!MissingTextureAtlasSprite.getLocation().equals(resourcelocation)) {
            list.add(CompletableFuture.runAsync(() -> {
               ResourceLocation resourcelocation1 = this.getResourceLocation(resourcelocation);
               Optional<Resource> optional = pResourceManager.getResource(resourcelocation1);
               if (optional.isEmpty()) {
                  LOGGER.error("Using missing texture, file {} not found", (Object)resourcelocation1);
               } else {
                  Resource resource = optional.get();

                  PngInfo pnginfo;
                  try {
                     InputStream inputstream = resource.open();

                     try {
                        pnginfo = new PngInfo(resourcelocation1::toString, inputstream);
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
                  } catch (IOException ioexception) {
                     LOGGER.error("Using missing texture, unable to load {} : {}", resourcelocation1, ioexception);
                     return;
                  }

                  AnimationMetadataSection animationmetadatasection;
                  try {
                     animationmetadatasection = resource.metadata().getSection(AnimationMetadataSection.SERIALIZER).orElse(AnimationMetadataSection.EMPTY);
                  } catch (Exception exception) {
                     LOGGER.error("Unable to parse metadata from {} : {}", resourcelocation1, exception);
                     return;
                  }

                  Pair<Integer, Integer> pair = animationmetadatasection.getFrameSize(pnginfo.width, pnginfo.height);
                  TextureAtlasSprite.Info textureatlassprite$info = new TextureAtlasSprite.Info(resourcelocation, pair.getFirst(), pair.getSecond(), animationmetadatasection);
                  queue.add(textureatlassprite$info);
               }
            }, Util.backgroundExecutor()));
         }
      }

      CompletableFuture.allOf(list.toArray(new CompletableFuture[0])).join();
      return queue;
   }

   private List<TextureAtlasSprite> getLoadedSprites(ResourceManager pResourceManager, Stitcher pStitcher, int pMipLevel) {
      Queue<TextureAtlasSprite> queue = new ConcurrentLinkedQueue<>();
      List<CompletableFuture<?>> list = Lists.newArrayList();
      pStitcher.gatherSprites((p_174703_, p_174704_, p_174705_, p_174706_, p_174707_) -> {
         if (p_174703_ == MissingTextureAtlasSprite.info()) {
            MissingTextureAtlasSprite missingtextureatlassprite = MissingTextureAtlasSprite.newInstance(this, pMipLevel, p_174704_, p_174705_, p_174706_, p_174707_);
            queue.add(missingtextureatlassprite);
         } else {
            list.add(CompletableFuture.runAsync(() -> {
               TextureAtlasSprite textureatlassprite = this.load(pResourceManager, p_174703_, p_174704_, p_174705_, pMipLevel, p_174706_, p_174707_);
               if (textureatlassprite != null) {
                  queue.add(textureatlassprite);
               }

            }, Util.backgroundExecutor()));
         }

      });
      CompletableFuture.allOf(list.toArray(new CompletableFuture[0])).join();
      return Lists.newArrayList(queue);
   }

   @Nullable
   private TextureAtlasSprite load(ResourceManager pResourceManager, TextureAtlasSprite.Info pSpriteInfo, int pStorageX, int pStorageY, int pMipLevel, int pX, int pY) {
      ResourceLocation resourcelocation = this.getResourceLocation(pSpriteInfo.name());

      try {
         Resource resource = pResourceManager.getResourceOrThrow(resourcelocation);
         InputStream inputstream = resource.open();

         TextureAtlasSprite textureatlassprite;
         try {
            NativeImage nativeimage = NativeImage.read(inputstream);
            textureatlassprite = net.minecraftforge.client.ForgeHooksClient.loadTextureAtlasSprite(this, pResourceManager, pSpriteInfo, resource, pStorageX, pStorageY, pX, pY, pMipLevel, nativeimage);
            if (textureatlassprite == null)
            textureatlassprite = new TextureAtlasSprite(this, pSpriteInfo, pMipLevel, pStorageX, pStorageY, pX, pY, nativeimage);
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

         return textureatlassprite;
      } catch (RuntimeException runtimeexception) {
         LOGGER.error("Unable to parse metadata from {}", resourcelocation, runtimeexception);
         return null;
      } catch (IOException ioexception) {
         LOGGER.error("Using missing texture, unable to load {}", resourcelocation, ioexception);
         return null;
      }
   }

   private ResourceLocation getResourceLocation(ResourceLocation pSpriteName) {
      return new ResourceLocation(pSpriteName.getNamespace(), String.format(Locale.ROOT, "textures/%s%s", pSpriteName.getPath(), ".png"));
   }

   public void cycleAnimationFrames() {
      this.bind();

      for(Tickable tickable : this.animatedTextures) {
         tickable.tick();
      }

   }

   public void tick() {
      if (!RenderSystem.isOnRenderThread()) {
         RenderSystem.recordRenderCall(this::cycleAnimationFrames);
      } else {
         this.cycleAnimationFrames();
      }

   }

   public TextureAtlasSprite getSprite(ResourceLocation pName) {
      TextureAtlasSprite textureatlassprite = this.texturesByName.get(pName);
      return textureatlassprite == null ? this.texturesByName.get(MissingTextureAtlasSprite.getLocation()) : textureatlassprite;
   }

   public void clearTextureData() {
      for(TextureAtlasSprite textureatlassprite : this.texturesByName.values()) {
         textureatlassprite.close();
      }

      this.texturesByName.clear();
      this.animatedTextures.clear();
   }

   public ResourceLocation location() {
      return this.location;
   }

   public void updateFilter(TextureAtlas.Preparations pPreparations) {
      this.setFilter(false, pPreparations.mipLevel > 0);
   }

   @OnlyIn(Dist.CLIENT)
   public static class Preparations {
      final Set<ResourceLocation> sprites;
      final int width;
      final int height;
      final int mipLevel;
      final List<TextureAtlasSprite> regions;

      public Preparations(Set<ResourceLocation> pSprites, int pWidth, int pHeight, int pMipLevel, List<TextureAtlasSprite> pRegions) {
         this.sprites = pSprites;
         this.width = pWidth;
         this.height = pHeight;
         this.mipLevel = pMipLevel;
         this.regions = pRegions;
      }
   }
}
