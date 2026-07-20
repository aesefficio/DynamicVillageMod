package com.sudolev.dynamicvillage.villager;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sudolev.dynamicvillage.condition.LoadCondition;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

/**
 * Loads data-driven villager trades from every data pack and mod on the server.
 *
 * <p>Scans {@code data/<any-namespace>/dynamicvillage/trades/*.json}. Each file targets one
 * profession and lists its trades:
 * <pre>
 * { "profession": "dynamicvillage:mechanical_engineer", "trades": [ ... ] }
 * </pre>
 *
 * <p><b>Merging.</b> By default every file's trades are <em>appended</em> to their profession's pool,
 * so any number of packs/mods contribute at once without clobbering each other. A file may set
 * {@code "replace": true} to first clear everything accumulated for that profession, then add its
 * own — use this to fully redefine a profession's trades. Files are processed in a deterministic
 * order (sorted by file id) so {@code replace} behaves predictably when several packs are present.
 *
 * <p><b>Conditions.</b> A file may carry a {@code "conditions"} array (see {@link LoadCondition});
 * if the conditions are not met the whole file is skipped. This lets a pack ship, e.g., trades that
 * only load when another mod is present.
 */
public class VillageTradeLoader extends SimpleJsonResourceReloadListener {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Gson GSON = new Gson();

   /** Folder (under {@code data/<namespace>/}) that data pack authors drop trade JSON files into. */
   public static final String DIRECTORY = "dynamicvillage/trades";

   private static volatile Map<ResourceLocation, List<VillageTradeEntry>> byProfession = Map.of();

   public VillageTradeLoader() {
      super(GSON, DIRECTORY);
   }

   @Override
   protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
      Map<ResourceLocation, List<VillageTradeEntry>> result = new HashMap<>();
      int trades = 0;
      int invalid = 0;
      int conditioned = 0;

      // Sort by file id so `replace` and append order are deterministic regardless of pack scan order.
      for (Map.Entry<ResourceLocation, JsonElement> file : new TreeMap<>(files).entrySet()) {
         var parsed = TradeSet.CODEC.parse(JsonOps.INSTANCE, file.getValue());
         if (parsed.error().isPresent()) {
            LOGGER.warn("[DynamicVillage] Skipping invalid trade file {}: {}", file.getKey(), parsed.error().get().message());
            invalid++;
            continue;
         }

         TradeSet set = parsed.result().orElseThrow();
         if (!LoadCondition.allMet(set.conditions())) {
            LOGGER.info("[DynamicVillage] Trade file {} skipped: its conditions are not met.", file.getKey());
            conditioned++;
            continue;
         }

         if (set.replace()) {
            result.put(set.profession(), new ArrayList<>(set.trades()));
            LOGGER.info("[DynamicVillage] Trade file {} replaces trades for profession {}.", file.getKey(), set.profession());
         } else {
            result.computeIfAbsent(set.profession(), key -> new ArrayList<>()).addAll(set.trades());
         }
         trades += set.trades().size();
      }

      byProfession = result;
      StringBuilder note = new StringBuilder();
      if (invalid > 0) {
         note.append(" (").append(invalid).append(" invalid file(s) skipped)");
      }
      if (conditioned > 0) {
         note.append(" (").append(conditioned).append(" file(s) skipped by conditions)");
      }
      LOGGER.info(
         "[DynamicVillage] Loaded {} villager trade(s) across {} profession(s){}",
         trades,
         result.size(),
         note.toString()
      );
   }

   /** Trades for the given profession id; empty list if none are defined. */
   public static List<VillageTradeEntry> forProfession(ResourceLocation profession) {
      return byProfession.getOrDefault(profession, List.of());
   }

   /** One trade JSON file: a profession id, its list of trades, and optional conditions/replace flag. */
   private record TradeSet(ResourceLocation profession, List<VillageTradeEntry> trades, List<LoadCondition> conditions, boolean replace) {
      static final Codec<TradeSet> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
               ResourceLocation.CODEC.fieldOf("profession").forGetter(TradeSet::profession),
               VillageTradeEntry.CODEC.listOf().fieldOf("trades").forGetter(TradeSet::trades),
               LoadCondition.CODEC.listOf().optionalFieldOf("conditions", List.of()).forGetter(TradeSet::conditions),
               Codec.BOOL.optionalFieldOf("replace", false).forGetter(TradeSet::replace)
            )
            .apply(instance, TradeSet::new)
      );
   }
}
