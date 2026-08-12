package com.sudolev.dynamicvillage.village;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import com.sudolev.dynamicvillage.condition.LoadCondition;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

/**
 * Loads chest loot-table assignments from every data pack and mod on the server.
 *
 * <p>Scans {@code data/<any-namespace>/dynamicvillage/loot/*.json}. Each file's resource id mirrors
 * the structure it targets, so the assignment for structure {@code ns:path} is the file
 * {@code data/ns/dynamicvillage/loot/path.json}. {@link VillageAddition} looks up a building's
 * structure id here at village-generation time and, if an assignment exists, attaches a
 * {@link com.sudolev.dynamicvillage.structure.ChestLootProcessor} carrying that table.
 */
public class LootAssignmentLoader extends SimpleJsonResourceReloadListener {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Gson GSON = new Gson();

   /** Folder (under {@code data/<namespace>/}) that data pack authors drop loot-assignment files into. */
   public static final String DIRECTORY = "dynamicvillage/loot";

   /** structure id -> assignment. Replaced wholesale on each (re)load. */
   private static volatile Map<ResourceLocation, LootAssignment> byStructure = Map.of();

   public LootAssignmentLoader() {
      super(GSON, DIRECTORY);
   }

   @Override
   protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
      Map<ResourceLocation, LootAssignment> result = new HashMap<>();
      int invalid = 0;
      int conditioned = 0;

      for (Map.Entry<ResourceLocation, JsonElement> file : files.entrySet()) {
         var parsed = LootAssignment.CODEC.parse(JsonOps.INSTANCE, file.getValue());
         if (parsed.error().isPresent()) {
            LOGGER.warn("[DynamicVillage] Skipping invalid loot assignment {}: {}", file.getKey(), parsed.error().get().message());
            invalid++;
            continue;
         }

         LootAssignment assignment = parsed.result().orElseThrow();
         if (!LoadCondition.allMet(assignment.conditions())) {
            LOGGER.info("[DynamicVillage] Loot assignment {} skipped: its conditions are not met.", file.getKey());
            conditioned++;
            continue;
         }

         warnIfLootTableMissing(resourceManager, file.getKey(), assignment.lootTable());
         // The file id IS the structure id it applies to.
         result.put(file.getKey(), assignment);
      }

      byStructure = result;
      StringBuilder note = new StringBuilder();
      if (invalid > 0) {
         note.append(" (").append(invalid).append(" invalid skipped)");
      }
      if (conditioned > 0) {
         note.append(" (").append(conditioned).append(" skipped by conditions)");
      }
      LOGGER.info("[DynamicVillage] Loaded {} chest loot assignment(s){}", result.size(), note.toString());
   }

   /**
    * Warns when a referenced loot table has no matching file in any loaded pack. Without this a typo'd
    * id silently produces empty chests, which is exactly the failure this system exists to prevent.
    * Both the 1.21+ ({@code loot_table}) and pre-1.21 ({@code loot_tables}) folder names are accepted so
    * the same check works on every version.
    */
   private static void warnIfLootTableMissing(ResourceManager resourceManager, ResourceLocation file, ResourceLocation lootTable) {
      ResourceLocation singular = new ResourceLocation(lootTable.getNamespace(), "loot_table/" + lootTable.getPath() + ".json");
      ResourceLocation plural = new ResourceLocation(lootTable.getNamespace(), "loot_tables/" + lootTable.getPath() + ".json");
      if (resourceManager.getResource(singular).isEmpty() && resourceManager.getResource(plural).isEmpty()) {
         LOGGER.warn(
            "[DynamicVillage] Loot assignment {} points at loot table {}, which no loaded pack provides; those chests will be empty.",
            file, lootTable
         );
      }
   }

   /** The loot assignment for the given structure id, or {@code null} if none is defined. */
   @Nullable
   public static LootAssignment forStructure(ResourceLocation structure) {
      return byStructure.get(structure);
   }
}
