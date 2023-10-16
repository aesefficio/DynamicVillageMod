package net.minecraft.server.packs;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.util.GsonHelper;
import org.slf4j.Logger;

public abstract class AbstractPackResources implements PackResources {
   private static final Logger LOGGER = LogUtils.getLogger();
   public final File file;

   public AbstractPackResources(File pFile) {
      this.file = pFile;
   }

   private static String getPathFromLocation(PackType pType, ResourceLocation pLocation) {
      return String.format(Locale.ROOT, "%s/%s/%s", pType.getDirectory(), pLocation.getNamespace(), pLocation.getPath());
   }

   protected static String getRelativePath(File pFile1, File pFile2) {
      return pFile1.toURI().relativize(pFile2.toURI()).getPath();
   }

   public InputStream getResource(PackType pType, ResourceLocation pLocation) throws IOException {
      return this.getResource(getPathFromLocation(pType, pLocation));
   }

   public boolean hasResource(PackType pType, ResourceLocation pLocation) {
      return this.hasResource(getPathFromLocation(pType, pLocation));
   }

   protected abstract InputStream getResource(String pResourcePath) throws IOException;

   public InputStream getRootResource(String pFileName) throws IOException {
      if (!pFileName.contains("/") && !pFileName.contains("\\")) {
         return this.getResource(pFileName);
      } else {
         throw new IllegalArgumentException("Root resources can only be filenames, not paths (no / allowed!)");
      }
   }

   protected abstract boolean hasResource(String pResourcePath);

   protected void logWarning(String pNamespace) {
      LOGGER.warn("ResourcePack: ignored non-lowercase namespace: {} in {}", pNamespace, this.file);
   }

   @Nullable
   public <T> T getMetadataSection(MetadataSectionSerializer<T> pDeserializer) throws IOException {
      InputStream inputstream = this.getResource("pack.mcmeta");

      Object object;
      try {
         object = getMetadataFromStream(pDeserializer, inputstream);
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

      return (T)object;
   }

   @Nullable
   public static <T> T getMetadataFromStream(MetadataSectionSerializer<T> pDeserializer, InputStream pInputStream) {
      JsonObject jsonobject;
      try {
         BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(pInputStream, StandardCharsets.UTF_8));

         try {
            jsonobject = GsonHelper.parse(bufferedreader);
         } catch (Throwable throwable1) {
            try {
               bufferedreader.close();
            } catch (Throwable throwable) {
               throwable1.addSuppressed(throwable);
            }

            throw throwable1;
         }

         bufferedreader.close();
      } catch (Exception exception1) {
         LOGGER.error("Couldn't load {} metadata", pDeserializer.getMetadataSectionName(), exception1);
         return (T)null;
      }

      if (!jsonobject.has(pDeserializer.getMetadataSectionName())) {
         return (T)null;
      } else {
         try {
            return pDeserializer.fromJson(GsonHelper.getAsJsonObject(jsonobject, pDeserializer.getMetadataSectionName()));
         } catch (Exception exception) {
            LOGGER.error("Couldn't load {} metadata", pDeserializer.getMetadataSectionName(), exception);
            return (T)null;
         }
      }
   }

   public String getName() {
      return this.file.getName();
   }

   @Override
   public String toString()
   {
      return String.format(java.util.Locale.ROOT, "%s: %s", getClass().getName(), file.getPath());
   }
}
