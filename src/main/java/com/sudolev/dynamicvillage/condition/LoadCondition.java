package com.sudolev.dynamicvillage.condition;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

/**
 * A loader-neutral load condition used to gate data-driven trade and building files on the runtime
 * environment (e.g. "only load if another mod is present"). Evaluated once at data (re)load time.
 *
 * <p>This is intentionally <em>not</em> NeoForge's {@code neoforge:conditions} / Forge's
 * {@code forge:conditions}: those use different JSON keys and APIs on the two loaders, which would
 * break the promise that a data pack's JSON is identical across every Minecraft version. This mod
 * evaluates a small, self-contained {@code conditions} block itself, so one file works everywhere.
 *
 * <p>JSON shape (a {@code "conditions"} array on the file/entry):
 * <pre>
 * "conditions": [
 *   { "type": "mod_loaded", "mod": "immersiveengineering" },
 *   { "type": "item_exists", "id": "create:packager" },
 *   { "type": "block_exists", "id": "create:track_station", "negate": true }
 * ]
 * </pre>
 *
 * <p>Supported {@code type}s:
 * <ul>
 *   <li>{@code mod_loaded} — requires {@code "mod"}: true when that mod id is loaded.</li>
 *   <li>{@code item_exists} — requires {@code "id"}: true when that item is registered.</li>
 *   <li>{@code block_exists} — requires {@code "id"}: true when that block is registered.</li>
 * </ul>
 * {@code "negate": true} inverts a single condition. All conditions in the array must pass
 * (logical AND). An unrecognised {@code type} fails closed (treated as not met) and is logged, so a
 * file that relies on a condition this version doesn't understand is skipped rather than loaded by
 * accident.
 *
 * @param type   the condition kind (see above)
 * @param mod    mod id, for {@code mod_loaded}
 * @param id     item/block id, for {@code item_exists} / {@code block_exists}
 * @param negate invert this single condition (defaults to {@code false})
 */
public record LoadCondition(String type, Optional<String> mod, Optional<ResourceLocation> id, boolean negate) {
   private static final Logger LOGGER = LogUtils.getLogger();

   public static final Codec<LoadCondition> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            Codec.STRING.fieldOf("type").forGetter(LoadCondition::type),
            Codec.STRING.optionalFieldOf("mod").forGetter(LoadCondition::mod),
            ResourceLocation.CODEC.optionalFieldOf("id").forGetter(LoadCondition::id),
            Codec.BOOL.optionalFieldOf("negate", false).forGetter(LoadCondition::negate)
         )
         .apply(instance, LoadCondition::new)
   );

   /** True when this single condition is satisfied (after applying {@link #negate()}). */
   public boolean test() {
      boolean raw;
      switch (type) {
         case "mod_loaded" -> {
            if (mod.isEmpty()) {
               LOGGER.warn("[DynamicVillage] Condition 'mod_loaded' is missing the 'mod' field; treating as not met.");
               return false;
            }
            raw = ModList.get().isLoaded(mod.get());
         }
         case "item_exists" -> raw = id.isPresent() && BuiltInRegistries.ITEM.containsKey(id.get());
         case "block_exists" -> raw = id.isPresent() && BuiltInRegistries.BLOCK.containsKey(id.get());
         default -> {
            LOGGER.warn("[DynamicVillage] Unknown condition type '{}'; treating as not met (skipping the file).", type);
            return false;
         }
      }
      return negate != raw;
   }

   /** True when every condition in the list is met (logical AND). An empty/absent list is always met. */
   public static boolean allMet(List<LoadCondition> conditions) {
      for (LoadCondition condition : conditions) {
         if (!condition.test()) {
            return false;
         }
      }
      return true;
   }
}
