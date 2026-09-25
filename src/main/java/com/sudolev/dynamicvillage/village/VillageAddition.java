package com.sudolev.dynamicvillage.village;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.sudolev.dynamicvillage.VillageLife;
import com.sudolev.dynamicvillage.config.VillageConfig;
import com.sudolev.dynamicvillage.structure.ChestLootProcessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pools.EmptyPoolElement;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import org.slf4j.Logger;

/**
 * Injects data-driven custom buildings (see {@link VillageBuildingLoader}) into vanilla village
 * template pools when the world starts. The set of buildings is fully data pack driven: this class
 * holds no hardcoded building list.
 */
@EventBusSubscriber(
   modid = VillageLife.MODID
)
public class VillageAddition {
   private static final Logger LOGGER = LogUtils.getLogger();

   /** Vanilla per-biome village decoration pools whose density the config can boost. */
   private static final List<ResourceLocation> DECOR_POOLS = List.of(
      ResourceLocation.parse("minecraft:village/plains/decor"),
      ResourceLocation.parse("minecraft:village/desert/decor"),
      ResourceLocation.parse("minecraft:village/savanna/decor"),
      ResourceLocation.parse("minecraft:village/snowy/decor"),
      ResourceLocation.parse("minecraft:village/taiga/decor")
   );

   /**
    * Ceiling on one pool's total custom weight. The pool expands into one list entry per weight unit, so
    * this bounds memory; at this size even a large vanilla pool is over 99% custom, so nothing is lost.
    */
   private static final int MAX_CUSTOM_WEIGHT = 50_000;

   /** Register the data pack reload listener that loads building definitions. */
   @SubscribeEvent
   public static void onAddReloadListeners(AddReloadListenerEvent event) {
      event.addListener(new VillageBuildingLoader());
      event.addListener(new LootAssignmentLoader());
      event.addListener(new PoolSettingsLoader());
   }

   @SubscribeEvent
   public static void addNewVillageBuilding(ServerAboutToStartEvent event) {
      RegistryAccess registryAccess = event.getServer().registryAccess();
      Registry<StructureTemplatePool> templatePoolRegistry = registryAccess.registryOrThrow(Registries.TEMPLATE_POOL);
      Registry<StructureProcessorList> processorListRegistry = registryAccess.registryOrThrow(Registries.PROCESSOR_LIST);

      Map<ResourceLocation, List<VillageBuildingEntry>> byPool = VillageBuildingLoader.byPool();
      if (byPool.isEmpty()) {
         LOGGER.warn("[DynamicVillage] No village building definitions were loaded; no buildings will be added.");
         return;
      }

      int spawnChancePercent = VillageConfig.BUILDING_SPAWN_CHANCE.get();
      byPool.forEach((poolRL, entries) -> injectPool(templatePoolRegistry, processorListRegistry, poolRL, entries, spawnChancePercent));

      int decorMultiplier = VillageConfig.DECORATION_DENSITY.get();
      if (decorMultiplier > 1) {
         for (ResourceLocation decorPool : DECOR_POOLS) {
            boostDecorPool(templatePoolRegistry, decorPool, decorMultiplier);
         }
      }
   }

   /**
    * Increases vanilla decoration density by scaling the weight of every non-empty element in a decor
    * pool, leaving the weighted "empty" element as-is. This shifts each decoration spot away from
    * resolving to nothing, so more lamps/flowers/props appear.
    */
   private static void boostDecorPool(Registry<StructureTemplatePool> templatePoolRegistry, ResourceLocation poolRL, int multiplier) {
      StructureTemplatePool pool = templatePoolRegistry.get(poolRL);
      if (pool == null) {
         return;
      }

      int sizeBefore = pool.templates.size();
      List<Pair<StructurePoolElement, Integer>> newRaw = new ArrayList<>();
      List<StructurePoolElement> newTemplates = new ArrayList<>();
      for (Pair<StructurePoolElement, Integer> pair : pool.rawTemplates) {
         StructurePoolElement element = pair.getFirst();
         int weight = (element instanceof EmptyPoolElement) ? pair.getSecond() : pair.getSecond() * multiplier;
         newRaw.add(new Pair<>(element, weight));
         for (int i = 0; i < weight; i++) {
            newTemplates.add(element);
         }
      }

      pool.templates.clear();
      pool.templates.addAll(newTemplates);
      pool.rawTemplates = newRaw;
      LOGGER.info("[DynamicVillage] Decor pool {}: density x{} ({} -> {} weighted entries)", poolRL, multiplier, sizeBefore, pool.templates.size());
   }

   private static void injectPool(
      Registry<StructureTemplatePool> templatePoolRegistry,
      Registry<StructureProcessorList> processorListRegistry,
      ResourceLocation poolRL,
      List<VillageBuildingEntry> entries,
      int spawnChancePercent
   ) {
      StructureTemplatePool pool = templatePoolRegistry.get(poolRL);
      if (pool == null) {
         LOGGER.warn("[DynamicVillage] Pool {} not found, could not add {} custom building(s)", poolRL, entries.size());
         return;
      }

      if (spawnChancePercent <= 0) {
         LOGGER.info("[DynamicVillage] Pool {}: spawn chance 0%, skipping {} custom building(s)", poolRL, entries.size());
         return;
      }

      // Entries with weight <= 0 are intentionally excluded (lets a data pack disable a building).
      List<VillageBuildingEntry> weighted = entries.stream().filter(e -> e.weight() > 0).toList();
      if (weighted.isEmpty()) {
         LOGGER.info("[DynamicVillage] Pool {}: no custom building(s) with weight > 0, skipping", poolRL);
         return;
      }

      // Copy only once we know there is work to do.
      int sizeBefore = pool.templates.size();
      List<Pair<StructurePoolElement, Integer>> rawTemplates = new ArrayList<>(pool.rawTemplates);
      List<StructurePoolElement> templates = new ArrayList<>(pool.templates);

      // The config controls the TOTAL custom share vs vanilla; each entry's weight controls its
      // share of that custom budget (so data packs tune relative frequency per building).
      // Done in long/double: the multipliers compound (config density x data-pack weight_multiplier can
      // reach 10,000), and an int product silently wraps negative at high settings.
      long vanillaWeight = rawTemplates.stream().mapToLong(Pair::getSecond).sum();
      double customBudget;
      if (spawnChancePercent >= 100) {
         // Keep vanilla entries as a fallback for plot shapes our buildings don't fit, but make custom
         // buildings overwhelmingly likely wherever they do fit. The pool's `templates` list holds one
         // element per weight unit, so this multiplier is also an allocation: 50x is already ~98%
         // custom and costs a fraction of the memory a larger factor would.
         customBudget = Math.max(1L, vanillaWeight) * 50.0D;
      } else {
         customBudget = Math.max(1.0D, (double) vanillaWeight * spawnChancePercent / (100 - spawnChancePercent));
      }

      // Per-pool multiplier lets one biome carry more or fewer custom buildings than another. A
      // building's own weight is relative within the pool, so only this can shift a pool's total share.
      // Two sources compose: the in-game config (per vanilla biome, tunable without a restart of the
      // pack) and any data pack pool settings (works for modded biomes and non-house pools too).
      double poolMultiplier = PoolSettingsLoader.forPool(poolRL).effectiveMultiplier() * VillageConfig.densityFor(poolRL);
      if (poolMultiplier <= 0.0D) {
         LOGGER.info("[DynamicVillage] Pool {}: density multiplier 0, skipping {} custom building(s)", poolRL, weighted.size());
         return;
      }
      customBudget *= poolMultiplier;

      // The pool's `templates` list holds one element per weight unit, so the budget is also an allocation.
      if (customBudget > MAX_CUSTOM_WEIGHT) {
         LOGGER.warn(
            "[DynamicVillage] Pool {}: custom building weight {} capped at {} (the pool is already overwhelmingly custom there).",
            poolRL, Math.round(customBudget), MAX_CUSTOM_WEIGHT
         );
         customBudget = MAX_CUSTOM_WEIGHT;
      }
      int customTotalWeight = (int) Math.max(1L, Math.round(customBudget));

      long totalRelative = weighted.stream().mapToLong(VillageBuildingEntry::weight).sum();
      int added = 0;

      for (VillageBuildingEntry entry : weighted) {
         ResourceKey<StructureProcessorList> procKey = ResourceKey.create(Registries.PROCESSOR_LIST, entry.processors());
         Optional<Holder.Reference<StructureProcessorList>> procHolder = processorListRegistry.getHolder(procKey);
         if (procHolder.isEmpty()) {
            LOGGER.warn("[DynamicVillage] Skipping {} in {}: processor list {} not found", entry.structure(), poolRL, entry.processors());
            continue;
         }

         // Proportional split of the custom budget by relative weight (rounded), min 1 so each building can appear.
         int entryWeight = (int) Math.max(1L, Math.round((double) customTotalWeight * entry.weight() / totalRelative));

         // If this structure has a chest loot assignment, append our loot processor to its processor list
         // so chests get a loot table stamped on at placement.
         Holder<StructureProcessorList> effectiveProcessors = procHolder.get();
         LootAssignment loot = LootAssignmentLoader.forStructure(entry.structure());
         if (loot != null) {
            List<StructureProcessor> combined = new ArrayList<>(procHolder.get().value().list());
            combined.add(new ChestLootProcessor(loot.lootTable(), loot.overrideExisting()));
            effectiveProcessors = Holder.direct(new StructureProcessorList(combined));
         }

         SinglePoolElement piece = (SinglePoolElement) SinglePoolElement
            .single(entry.structure().toString(), effectiveProcessors)
            .apply(entry.projection());

         for (int i = 0; i < entryWeight; i++) {
            templates.add(piece);
         }
         rawTemplates.add(new Pair<>(piece, entryWeight));
         added++;
         LOGGER.debug("[DynamicVillage]   + {} (weight {}, projection {}) -> {}", entry.structure(), entryWeight, entry.projection(), poolRL);
      }

      pool.templates.clear();
      pool.templates.addAll(templates);
      pool.rawTemplates = rawTemplates;
      LOGGER.info(
         "[DynamicVillage] Pool {}: added {} custom building(s) at {}% spawn chance x{} multiplier ({} -> {} entries)",
         poolRL, added, spawnChancePercent, poolMultiplier, sizeBefore, pool.templates.size()
      );
   }
}
