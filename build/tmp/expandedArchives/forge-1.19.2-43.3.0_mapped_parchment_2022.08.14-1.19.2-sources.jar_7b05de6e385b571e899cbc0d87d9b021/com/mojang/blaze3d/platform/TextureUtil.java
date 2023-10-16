package com.mojang.blaze3d.platform;

import com.mojang.blaze3d.DontObfuscate;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.SharedConstants;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
public class TextureUtil {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final int MIN_MIPMAP_LEVEL = 0;
   private static final int DEFAULT_IMAGE_BUFFER_SIZE = 8192;

   public static int generateTextureId() {
      RenderSystem.assertOnRenderThreadOrInit();
      if (SharedConstants.IS_RUNNING_IN_IDE) {
         int[] aint = new int[ThreadLocalRandom.current().nextInt(15) + 1];
         GlStateManager._genTextures(aint);
         int i = GlStateManager._genTexture();
         GlStateManager._deleteTextures(aint);
         return i;
      } else {
         return GlStateManager._genTexture();
      }
   }

   public static void releaseTextureId(int pTextureId) {
      RenderSystem.assertOnRenderThreadOrInit();
      GlStateManager._deleteTexture(pTextureId);
   }

   public static void prepareImage(int pTextureId, int pWidth, int pHeight) {
      prepareImage(NativeImage.InternalGlFormat.RGBA, pTextureId, 0, pWidth, pHeight);
   }

   public static void prepareImage(NativeImage.InternalGlFormat pPixelFormat, int pTextureId, int pWidth, int pHeight) {
      prepareImage(pPixelFormat, pTextureId, 0, pWidth, pHeight);
   }

   public static void prepareImage(int pTextureId, int pMipmapLevel, int pWidth, int pHeight) {
      prepareImage(NativeImage.InternalGlFormat.RGBA, pTextureId, pMipmapLevel, pWidth, pHeight);
   }

   public static void prepareImage(NativeImage.InternalGlFormat pPixelFormat, int pTextureId, int pMipmapLevel, int pWidth, int pHeight) {
      RenderSystem.assertOnRenderThreadOrInit();
      bind(pTextureId);
      if (pMipmapLevel >= 0) {
         GlStateManager._texParameter(3553, 33085, pMipmapLevel);
         GlStateManager._texParameter(3553, 33082, 0);
         GlStateManager._texParameter(3553, 33083, pMipmapLevel);
         GlStateManager._texParameter(3553, 34049, 0.0F);
      }

      for(int i = 0; i <= pMipmapLevel; ++i) {
         GlStateManager._texImage2D(3553, i, pPixelFormat.glFormat(), pWidth >> i, pHeight >> i, 0, 6408, 5121, (IntBuffer)null);
      }

   }

   private static void bind(int pTextureId) {
      RenderSystem.assertOnRenderThreadOrInit();
      GlStateManager._bindTexture(pTextureId);
   }

   public static ByteBuffer readResource(InputStream pInputStream) throws IOException {
      ByteBuffer bytebuffer;
      if (pInputStream instanceof FileInputStream fileinputstream) {
         FileChannel filechannel = fileinputstream.getChannel();
         bytebuffer = MemoryUtil.memAlloc((int)filechannel.size() + 1);

         while(filechannel.read(bytebuffer) != -1) {
         }
      } else {
         bytebuffer = MemoryUtil.memAlloc(8192);
         ReadableByteChannel readablebytechannel = Channels.newChannel(pInputStream);

         while(readablebytechannel.read(bytebuffer) != -1) {
            if (bytebuffer.remaining() == 0) {
               bytebuffer = MemoryUtil.memRealloc(bytebuffer, bytebuffer.capacity() * 2);
            }
         }
      }

      return bytebuffer;
   }

   public static void writeAsPNG(String pTextureName, int pTextureId, int pAmount, int pWidth, int pHeight) {
      RenderSystem.assertOnRenderThread();
      bind(pTextureId);

      for(int i = 0; i <= pAmount; ++i) {
         String s = pTextureName + "_" + i + ".png";
         int j = pWidth >> i;
         int k = pHeight >> i;

         try {
            NativeImage nativeimage = new NativeImage(j, k, false);

            try {
               nativeimage.downloadTexture(i, false);
               nativeimage.writeToFile(s);
               LOGGER.debug("Exported png to: {}", (Object)(new File(s)).getAbsolutePath());
            } catch (Throwable throwable1) {
               try {
                  nativeimage.close();
               } catch (Throwable throwable) {
                  throwable1.addSuppressed(throwable);
               }

               throw throwable1;
            }

            nativeimage.close();
         } catch (IOException ioexception) {
            LOGGER.debug("Unable to write: ", (Throwable)ioexception);
         }
      }

   }

   public static void initTexture(IntBuffer pBuffer, int pWidth, int pHeight) {
      RenderSystem.assertOnRenderThread();
      GL11.glPixelStorei(3312, 0);
      GL11.glPixelStorei(3313, 0);
      GL11.glPixelStorei(3314, 0);
      GL11.glPixelStorei(3315, 0);
      GL11.glPixelStorei(3316, 0);
      GL11.glPixelStorei(3317, 4);
      GL11.glTexImage2D(3553, 0, 6408, pWidth, pHeight, 0, 32993, 33639, pBuffer);
      GL11.glTexParameteri(3553, 10240, 9728);
      GL11.glTexParameteri(3553, 10241, 9729);
   }
}