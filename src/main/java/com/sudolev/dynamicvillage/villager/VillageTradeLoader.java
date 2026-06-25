package com.sudolev.dynamicvillage.villager;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
 * Loads data-driven villager trades from every data pack and mod on the server.
 *
 * <p>Scans {@code data/<any-namespace>/dynamicvillage/trades/*.json}. Each file targets one
 * profession and lists its trades:
 * <pre>
 * { "profession": "dynamicvillage:mechanical_engineer", "trades": [ ... ] }
 * </pre>
 *
 * <p>Files in different namespaces/paths are merged (additive). To change or remove the mod's
 * standard trades, override the file at the same path -- a higher-priority data pack fully replaces
 * that profession's default list (standard data pack behaviour).
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
      int skipped = 0;

      for (Map.Entry<ResourceLocation, JsonElement> file : files.entrySet()) {
         var parsed = TradeSet.CODEC.parse(JsonOps.INSTANCE, file.getValue());
         if (parsed.error().isPresent()) {
            LOGGER.warn("[DynamicVillage] Skipping invalid trade file {}: {}", file.getKey(), parsed.error().get().message());
            skipped++;
            continue;
         }
         TradeSet set = parsed.result().orElseThrow();
         result.computeIfAbsent(set.profession(), key -> new ArrayList<>()).addAll(set.trades());
         trades += set.trades().size();
      }

      byProfession = result;
      LOGGER.info(
         "[DynamicVillage] Loaded {} villager trade(s) across {} profession(s){}",
         trades,
         result.size(),
         skipped > 0 ? " (" + skipped + " invalid file(s) skipped)" : ""
      );
   }

   /** Trades for the given profession id; empty list if none are defined. */
   public static List<VillageTradeEntry> forProfession(ResourceLocation profession) {
      return byProfession.getOrDefault(profession, List.of());
   }

   /** One trade JSON file: a profession id plus its list of trades. */
   private record TradeSet(ResourceLocation profession, List<VillageTradeEntry> trades) {
      static final Codec<TradeSet> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
               ResourceLocation.CODEC.fieldOf("profession").forGetter(TradeSet::profession),
               VillageTradeEntry.CODEC.listOf().fieldOf("trades").forGetter(TradeSet::trades)
            )
            .apply(instance, TradeSet::new)
      );
   }
}
