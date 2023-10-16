package net.minecraft.client.resources;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LegacyStuffWrapper {
   /** @deprecated */
   @Deprecated
   public static int[] getPixels(ResourceManager pManager, ResourceLocation pLocation) throws IOException {
      InputStream inputstream = pManager.open(pLocation);

      int[] aint;
      try {
         NativeImage nativeimage = NativeImage.read(inputstream);

         try {
            aint = nativeimage.makePixelArray();
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

      return aint;
   }
}