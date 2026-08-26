package com.sudolev.dynamicvillage.village;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import com.sudolev.dynamicvillage.condition.LoadCondition;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

/**
 * Loads per-pool building-density settings from every data pack and mod on the server.
 *
 * <p>Scans {@code data/<any-namespace>/dynamicvillage/pools/*.json}. Each file's resource id mirrors
 * the template pool it tunes, so settings for {@code minecraft:village/desert/houses} are the file
 * {@code data/minecraft/dynamicvillage/pools/village/desert/houses.json}. {@link VillageAddition}
 * reads these when injecting buildings; a pool with no file uses {@link PoolSettings#DEFAULT}.
 */
public class PoolSettingsLoader extends SimpleJsonResourceReloadListener {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Gson GSON = new Gson();

   /** Folder (under {@code data/<namespace>/}) that data pack authors drop pool settings into. */
   public static final String DIRECTORY = "dynamicvillage/pools";

   /** pool id -> settings. Replaced wholesale on each (re)load. */
   private static volatile Map<ResourceLocation, PoolSettings> byPool = Map.of();

   public PoolSettingsLoader() {
      super(GSON, DIRECTORY);
   }

   @Override
   protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
      Map<ResourceLocation, PoolSettings> result = new HashMap<>();
      int invalid = 0;
      int conditioned = 0;

      for (Map.Entry<ResourceLocation, JsonElement> file : files.entrySet()) {
         var parsed = PoolSettings.CODEC.parse(JsonOps.INSTANCE, file.getValue());
         if (parsed.error().isPresent()) {
            LOGGER.warn("[DynamicVillage] Skipping invalid pool settings {}: {}", file.getKey(), parsed.error().get().message());
            invalid++;
            continue;
         }

         PoolSettings settings = parsed.result().orElseThrow();
         if (!LoadCondition.allMet(settings.conditions())) {
            LOGGER.info("[DynamicVillage] Pool settings {} skipped: its conditions are not met.", file.getKey());
            conditioned++;
            continue;
         }
         if (settings.effectiveMultiplier() != settings.weightMultiplier()) {
            LOGGER.warn(
               "[DynamicVillage] Pool settings {}: weight_multiplier {} is out of range; using {}.",
               file.getKey(), settings.weightMultiplier(), settings.effectiveMultiplier()
            );
         }
         // The file id IS the pool id it applies to.
         result.put(file.getKey(), settings);
      }

      byPool = result;
      StringBuilder note = new StringBuilder();
      if (invalid > 0) {
         note.append(" (").append(invalid).append(" invalid skipped)");
      }
      if (conditioned > 0) {
         note.append(" (").append(conditioned).append(" skipped by conditions)");
      }
      LOGGER.info("[DynamicVillage] Loaded {} pool setting(s){}", result.size(), note.toString());
   }

   /** Settings for the given pool, or {@link PoolSettings#DEFAULT} when the pool has no file. */
   public static PoolSettings forPool(ResourceLocation pool) {
      return byPool.getOrDefault(pool, PoolSettings.DEFAULT);
   }
}
