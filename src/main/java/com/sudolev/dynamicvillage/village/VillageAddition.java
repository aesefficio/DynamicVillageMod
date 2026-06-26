package com.sudolev.dynamicvillage.village;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.sudolev.dynamicvillage.VillageLife;
import com.sudolev.dynamicvillage.config.VillageConfig;
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

   /** Register the data pack reload listener that loads building definitions. */
   @SubscribeEvent
   public static void onAddReloadListeners(AddReloadListenerEvent event) {
      event.addListener(new VillageBuildingLoader());
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

      int sizeBefore = pool.templates.size();
      List<Pair<StructurePoolElement, Integer>> rawTemplates = new ArrayList<>(pool.rawTemplates);
      List<StructurePoolElement> templates = new ArrayList<>(pool.templates);

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

      // The config controls the TOTAL custom share vs vanilla; each entry's weight controls its
      // share of that custom budget (so data packs tune relative frequency per building).
      int vanillaWeight = rawTemplates.stream().mapToInt(Pair::getSecond).sum();
      int customTotalWeight;
      if (spawnChancePercent >= 100) {
         // Keep vanilla entries as a fallback for plot shapes our buildings don't fit,
         // but make custom buildings overwhelmingly likely wherever they do fit.
         customTotalWeight = Math.max(1, vanillaWeight) * 1000;
      } else {
         customTotalWeight = Math.max(1, vanillaWeight * spawnChancePercent / (100 - spawnChancePercent));
      }

      int totalRelative = weighted.stream().mapToInt(VillageBuildingEntry::weight).sum();
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

         SinglePoolElement piece = (SinglePoolElement) SinglePoolElement
            .single(entry.structure().toString(), procHolder.get())
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
         "[DynamicVillage] Pool {}: added {} custom building(s) at {}% spawn chance ({} -> {} entries)",
         poolRL, added, spawnChancePercent, sizeBefore, pool.templates.size()
      );
   }
}
