package net.minecraft.server.packs.resources;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

public abstract class SimpleJsonResourceReloadListener extends SimplePreparableReloadListener<Map<ResourceLocation, JsonElement>> {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String PATH_SUFFIX = ".json";
   private static final int PATH_SUFFIX_LENGTH = ".json".length();
   private final Gson gson;
   private final String directory;

   public SimpleJsonResourceReloadListener(Gson p_10768_, String p_10769_) {
      this.gson = p_10768_;
      this.directory = p_10769_;
   }

   /**
    * Performs any reloading that can be done off-thread, such as file IO
    */
   protected Map<ResourceLocation, JsonElement> prepare(ResourceManager pResourceManager, ProfilerFiller pProfiler) {
      Map<ResourceLocation, JsonElement> map = Maps.newHashMap();
      int i = this.directory.length() + 1;

      for(Map.Entry<ResourceLocation, Resource> entry : pResourceManager.listResources(this.directory, (p_215600_) -> {
         return p_215600_.getPath().endsWith(".json");
      }).entrySet()) {
         ResourceLocation resourcelocation = entry.getKey();
         String s = resourcelocation.getPath();
         ResourceLocation resourcelocation1 = new ResourceLocation(resourcelocation.getNamespace(), s.substring(i, s.length() - PATH_SUFFIX_LENGTH));

         try {
            Reader reader = entry.getValue().openAsReader();

            try {
               JsonElement jsonelement = GsonHelper.fromJson(this.gson, reader, JsonElement.class);
               if (jsonelement != null) {
                  JsonElement jsonelement1 = map.put(resourcelocation1, jsonelement);
                  if (jsonelement1 != null) {
                     throw new IllegalStateException("Duplicate data file ignored with ID " + resourcelocation1);
                  }
               } else {
                  LOGGER.error("Couldn't load data file {} from {} as it's null or empty", resourcelocation1, resourcelocation);
               }
            } catch (Throwable throwable1) {
               if (reader != null) {
                  try {
                     reader.close();
                  } catch (Throwable throwable) {
                     throwable1.addSuppressed(throwable);
                  }
               }

               throw throwable1;
            }

            if (reader != null) {
               reader.close();
            }
         } catch (IllegalArgumentException | IOException | JsonParseException jsonparseexception) {
            LOGGER.error("Couldn't parse data file {} from {}", resourcelocation1, resourcelocation, jsonparseexception);
         }
      }

      return map;
   }

   protected ResourceLocation getPreparedPath(ResourceLocation rl) {
      return new ResourceLocation(rl.getNamespace(), this.directory + "/" + rl.getPath() + ".json");
   }
}
