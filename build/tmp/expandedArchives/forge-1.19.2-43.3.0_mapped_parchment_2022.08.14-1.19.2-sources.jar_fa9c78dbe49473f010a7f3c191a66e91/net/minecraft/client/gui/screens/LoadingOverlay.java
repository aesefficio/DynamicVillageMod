package net.minecraft.client.gui.screens;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.VanillaPackResources;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LoadingOverlay extends Overlay {
   static final ResourceLocation MOJANG_STUDIOS_LOGO_LOCATION = new ResourceLocation("textures/gui/title/mojangstudios.png");
   private static final int LOGO_BACKGROUND_COLOR = FastColor.ARGB32.color(255, 239, 50, 61);
   private static final int LOGO_BACKGROUND_COLOR_DARK = FastColor.ARGB32.color(255, 0, 0, 0);
   private static final IntSupplier BRAND_BACKGROUND = () -> {
      return Minecraft.getInstance().options.darkMojangStudiosBackground().get() ? LOGO_BACKGROUND_COLOR_DARK : LOGO_BACKGROUND_COLOR;
   };
   private static final int LOGO_SCALE = 240;
   private static final float LOGO_QUARTER_FLOAT = 60.0F;
   private static final int LOGO_QUARTER = 60;
   private static final int LOGO_HALF = 120;
   private static final float LOGO_OVERLAP = 0.0625F;
   private static final float SMOOTHING = 0.95F;
   public static final long FADE_OUT_TIME = 1000L;
   public static final long FADE_IN_TIME = 500L;
   private final Minecraft minecraft;
   private final ReloadInstance reload;
   private final Consumer<Optional<Throwable>> onFinish;
   private final boolean fadeIn;
   private float currentProgress;
   private long fadeOutStart = -1L;
   private long fadeInStart = -1L;

   public LoadingOverlay(Minecraft pMinecraft, ReloadInstance pReload, Consumer<Optional<Throwable>> pOnFinish, boolean pFadeIn) {
      this.minecraft = pMinecraft;
      this.reload = pReload;
      this.onFinish = pOnFinish;
      this.fadeIn = pFadeIn;
   }

   public static void registerTextures(Minecraft pMc) {
      pMc.getTextureManager().register(MOJANG_STUDIOS_LOGO_LOCATION, new LoadingOverlay.LogoTexture());
   }

   private static int replaceAlpha(int p_169325_, int p_169326_) {
      return p_169325_ & 16777215 | p_169326_ << 24;
   }

   public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
      int i = this.minecraft.getWindow().getGuiScaledWidth();
      int j = this.minecraft.getWindow().getGuiScaledHeight();
      long k = Util.getMillis();
      if (this.fadeIn && this.fadeInStart == -1L) {
         this.fadeInStart = k;
      }

      float f = this.fadeOutStart > -1L ? (float)(k - this.fadeOutStart) / 1000.0F : -1.0F;
      float f1 = this.fadeInStart > -1L ? (float)(k - this.fadeInStart) / 500.0F : -1.0F;
      float f2;
      if (f >= 1.0F) {
         if (this.minecraft.screen != null) {
            this.minecraft.screen.render(pPoseStack, 0, 0, pPartialTick);
         }

         int l = Mth.ceil((1.0F - Mth.clamp(f - 1.0F, 0.0F, 1.0F)) * 255.0F);
         fill(pPoseStack, 0, 0, i, j, replaceAlpha(BRAND_BACKGROUND.getAsInt(), l));
         f2 = 1.0F - Mth.clamp(f - 1.0F, 0.0F, 1.0F);
      } else if (this.fadeIn) {
         if (this.minecraft.screen != null && f1 < 1.0F) {
            this.minecraft.screen.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
         }

         int l1 = Mth.ceil(Mth.clamp((double)f1, 0.15D, 1.0D) * 255.0D);
         fill(pPoseStack, 0, 0, i, j, replaceAlpha(BRAND_BACKGROUND.getAsInt(), l1));
         f2 = Mth.clamp(f1, 0.0F, 1.0F);
      } else {
         int i2 = BRAND_BACKGROUND.getAsInt();
         float f3 = (float)(i2 >> 16 & 255) / 255.0F;
         float f4 = (float)(i2 >> 8 & 255) / 255.0F;
         float f5 = (float)(i2 & 255) / 255.0F;
         GlStateManager._clearColor(f3, f4, f5, 1.0F);
         GlStateManager._clear(16384, Minecraft.ON_OSX);
         f2 = 1.0F;
      }

      int j2 = (int)((double)this.minecraft.getWindow().getGuiScaledWidth() * 0.5D);
      int k2 = (int)((double)this.minecraft.getWindow().getGuiScaledHeight() * 0.5D);
      double d1 = Math.min((double)this.minecraft.getWindow().getGuiScaledWidth() * 0.75D, (double)this.minecraft.getWindow().getGuiScaledHeight()) * 0.25D;
      int i1 = (int)(d1 * 0.5D);
      double d0 = d1 * 4.0D;
      int j1 = (int)(d0 * 0.5D);
      RenderSystem.setShaderTexture(0, MOJANG_STUDIOS_LOGO_LOCATION);
      RenderSystem.enableBlend();
      RenderSystem.blendEquation(32774);
      RenderSystem.blendFunc(770, 1);
      RenderSystem.setShader(GameRenderer::getPositionTexShader);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, f2);
      blit(pPoseStack, j2 - j1, k2 - i1, j1, (int)d1, -0.0625F, 0.0F, 120, 60, 120, 120);
      blit(pPoseStack, j2, k2 - i1, j1, (int)d1, 0.0625F, 60.0F, 120, 60, 120, 120);
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableBlend();
      int k1 = (int)((double)this.minecraft.getWindow().getGuiScaledHeight() * 0.8325D);
      float f6 = this.reload.getActualProgress();
      this.currentProgress = Mth.clamp(this.currentProgress * 0.95F + f6 * 0.050000012F, 0.0F, 1.0F);
      net.minecraftforge.client.loading.ClientModLoader.renderProgressText();
      if (f < 1.0F) {
         this.drawProgressBar(pPoseStack, i / 2 - j1, k1 - 5, i / 2 + j1, k1 + 5, 1.0F - Mth.clamp(f, 0.0F, 1.0F));
      }

      if (f >= 2.0F) {
         this.minecraft.setOverlay((Overlay)null);
      }

      if (this.fadeOutStart == -1L && this.reload.isDone() && (!this.fadeIn || f1 >= 2.0F)) {
         this.fadeOutStart = Util.getMillis(); // Moved up to guard against inf loops caused by callback
         try {
            this.reload.checkExceptions();
            this.onFinish.accept(Optional.empty());
         } catch (Throwable throwable) {
            this.onFinish.accept(Optional.of(throwable));
         }

         if (this.minecraft.screen != null) {
            this.minecraft.screen.init(this.minecraft, this.minecraft.getWindow().getGuiScaledWidth(), this.minecraft.getWindow().getGuiScaledHeight());
         }
      }

   }

   private void drawProgressBar(PoseStack pPoseStack, int pMinX, int pMinY, int pMaxX, int pMaxY, float pPartialTick) {
      int i = Mth.ceil((float)(pMaxX - pMinX - 2) * this.currentProgress);
      int j = Math.round(pPartialTick * 255.0F);
      int k = FastColor.ARGB32.color(j, 255, 255, 255);
      fill(pPoseStack, pMinX + 2, pMinY + 2, pMinX + i, pMaxY - 2, k);
      fill(pPoseStack, pMinX + 1, pMinY, pMaxX - 1, pMinY + 1, k);
      fill(pPoseStack, pMinX + 1, pMaxY, pMaxX - 1, pMaxY - 1, k);
      fill(pPoseStack, pMinX, pMinY, pMinX + 1, pMaxY, k);
      fill(pPoseStack, pMaxX, pMinY, pMaxX - 1, pMaxY, k);
   }

   public boolean isPauseScreen() {
      return true;
   }

   @OnlyIn(Dist.CLIENT)
   static class LogoTexture extends SimpleTexture {
      public LogoTexture() {
         super(LoadingOverlay.MOJANG_STUDIOS_LOGO_LOCATION);
      }

      protected SimpleTexture.TextureImage getTextureImage(ResourceManager pResourceManager) {
         Minecraft minecraft = Minecraft.getInstance();
         VanillaPackResources vanillapackresources = minecraft.getClientPackSource().getVanillaPack();

         try {
            InputStream inputstream = vanillapackresources.getResource(PackType.CLIENT_RESOURCES, LoadingOverlay.MOJANG_STUDIOS_LOGO_LOCATION);

            SimpleTexture.TextureImage simpletexture$textureimage;
            try {
               simpletexture$textureimage = new SimpleTexture.TextureImage(new TextureMetadataSection(true, true), NativeImage.read(inputstream));
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

            return simpletexture$textureimage;
         } catch (IOException ioexception) {
            return new SimpleTexture.TextureImage(ioexception);
         }
      }
   }
}
