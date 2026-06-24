package com.sudolev.dynamicvillage.village;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

/**
 * Loads data-driven village building definitions from every data pack and mod on the server.
 *
 * <p>Scans {@code data/<any-namespace>/dynamicvillage/buildings/*.json}, so any data pack adds
 * buildings under its OWN namespace and they all merge together -- no file conflicts, no overwriting.
 * The parsed entries are grouped by target pool and read by {@link VillageAddition} when the world
 * starts.
 *
 * <p>Invalid entries are logged and skipped; one bad file never breaks the rest.
 */
public class VillageBuildingLoader extends SimpleJsonResourceReloadListener {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Gson GSON = new Gson();

   /** Folder (under {@code data/<namespace>/}) that data pack authors drop building JSON files into. */
   public static final String DIRECTORY = "dynamicvillage/buildings";

   /** Latest loaded definitions, grouped by the pool they target. Replaced wholesale on each (re)load. */
   private static volatile Map<ResourceLocation, List<VillageBuildingEntry>> byPool = Map.of();

   public VillageBuildingLoader() {
      super(GSON, DIRECTORY);
   }

   @Override
   protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
      Map<ResourceLocation, List<VillageBuildingEntry>> result = new HashMap<>();
      int loaded = 0;
      int skipped = 0;

      for (Map.Entry<ResourceLocation, JsonElement> file : files.entrySet()) {
         var parsed = VillageBuildingEntry.CODEC.parse(JsonOps.INSTANCE, file.getValue());
         if (parsed.error().isPresent()) {
            LOGGER.warn("[DynamicVillage] Skipping invalid building definition {}: {}", file.getKey(), parsed.error().get().message());
            skipped++;
            continue;
         }

         VillageBuildingEntry entry = parsed.result().orElseThrow();
         result.computeIfAbsent(entry.pool(), key -> new ArrayList<>()).add(entry);
         loaded++;
      }

      byPool = result;
      LOGGER.info(
         "[DynamicVillage] Loaded {} village building definition(s) across {} pool(s){}",
         loaded,
         result.size(),
         skipped > 0 ? " (" + skipped + " invalid skipped)" : ""
      );
   }

   /** Definitions grouped by target pool. Never null; empty until the first data load completes. */
   public static Map<ResourceLocation, List<VillageBuildingEntry>> byPool() {
      return byPool;
   }
}
