package net.minecraft.client.resources;

import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class AssetIndex {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Map<String, File> rootFiles = Maps.newHashMap();
   private final Map<ResourceLocation, File> namespacedFiles = Maps.newHashMap();

   protected AssetIndex() {
   }

   public AssetIndex(File pAssetDirectory, String pIndex) {
      File file1 = new File(pAssetDirectory, "objects");
      File file2 = new File(pAssetDirectory, "indexes/" + pIndex + ".json");
      BufferedReader bufferedreader = null;

      try {
         bufferedreader = Files.newReader(file2, StandardCharsets.UTF_8);
         JsonObject jsonobject = GsonHelper.parse(bufferedreader);
         JsonObject jsonobject1 = GsonHelper.getAsJsonObject(jsonobject, "objects", (JsonObject)null);
         if (jsonobject1 != null) {
            for(Map.Entry<String, JsonElement> entry : jsonobject1.entrySet()) {
               JsonObject jsonobject2 = (JsonObject)entry.getValue();
               String s = entry.getKey();
               String[] astring = s.split("/", 2);
               String s1 = GsonHelper.getAsString(jsonobject2, "hash");
               File file3 = new File(file1, s1.substring(0, 2) + "/" + s1);
               if (astring.length == 1) {
                  this.rootFiles.put(astring[0], file3);
               } else {
                  this.namespacedFiles.put(new ResourceLocation(astring[0], astring[1]), file3);
               }
            }
         }
      } catch (JsonParseException jsonparseexception) {
         LOGGER.error("Unable to parse resource index file: {}", (Object)file2);
      } catch (FileNotFoundException filenotfoundexception) {
         LOGGER.error("Can't find the resource index file: {}", (Object)file2);
      } finally {
         IOUtils.closeQuietly((Reader)bufferedreader);
      }

   }

   @Nullable
   public File getFile(ResourceLocation pLocation) {
      return this.namespacedFiles.get(pLocation);
   }

   @Nullable
   public File getRootFile(String pPath) {
      return this.rootFiles.get(pPath);
   }

   public Collection<ResourceLocation> getFiles(String pPath, String pNamespace, Predicate<ResourceLocation> pFilter) {
      return this.namespacedFiles.keySet().stream().filter((p_118541_) -> {
         String s = p_118541_.getPath();
         return p_118541_.getNamespace().equals(pNamespace) && !s.endsWith(".mcmeta") && s.startsWith(pPath + "/") && pFilter.test(p_118541_);
      }).collect(Collectors.toList());
   }
}