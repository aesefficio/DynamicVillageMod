package com.sudolev.dynamicvillage.profession;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import com.sudolev.dynamicvillage.condition.LoadCondition;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.slf4j.Logger;

/**
 * Registers data-defined villager professions (see {@link ProfessionDefinition}) from
 * {@code config/dynamicvillage/professions/*.json} during the registry phase.
 *
 * <p>Professions and POIs live in frozen built-in registries populated at mod load, so definitions
 * are read from the config directory (available that early) rather than from world data packs. Each
 * definition registers one POI type and one profession that uses it; a definition's trades and
 * buildings then use the normal data-pack systems.
 *
 * <p>Bad definitions never crash the game: duplicates, ids that clash with an already-registered
 * profession, missing blocks and unusable fields are all logged and skipped.
 */
public final class ProfessionRegistrar {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String CONFIG_SUBDIR = "dynamicvillage/professions";

   /** Parsed once, lazily, on the first registry callback; reused for POIs and professions. */
   private static List<ProfessionDefinition> definitions;

   /**
    * Definitions that passed validation, with their resolved block states — computed once and reused
    * by both registry passes, so nothing is validated, resolved, or warned about twice.
    */
   private static Map<ProfessionDefinition, Set<BlockState>> usableDefinitions;

   /*
    * Professions and POIs live in separate registries whose RegisterEvents fire one after the other
    * (currently professions first). Each pass can only check clashes against its own registry, so a
    * definition refused by the first pass must also be skipped by the second. Tracking it this way keeps
    * the pair consistent without depending on which event happens to fire first.
    */
   private static final Set<ResourceLocation> REJECTED = new HashSet<>();
   private static final Set<ResourceLocation> REGISTERED_PROFESSIONS = new HashSet<>();
   private static final Set<ResourceLocation> REGISTERED_POIS = new HashSet<>();

   private ProfessionRegistrar() {
   }

   /**
    * Wire this registrar onto the mod event bus (called from the mod constructor). It listens at
    * {@link EventPriority#LOWEST} so every other mod has already registered its entries for the same
    * registry by the time the clash checks read it; checking any earlier misses them, and the real
    * duplicate then crashes the game later in the same event.
    */
   public static void register(IEventBus modEventBus) {
      modEventBus.addListener(EventPriority.LOWEST, ProfessionRegistrar::onRegister);
   }

   private static void onRegister(RegisterEvent event) {
      // Clashes are checked here, against each live registry, not in the cached validate(): only inside
      // a registry's own event (at LOWEST) is every other mod's entry for it guaranteed to be present.
      event.register(Registries.VILLAGER_PROFESSION, helper -> {
         // Best-effort early check against POIs that already exist. Vanilla's are bootstrapped before any
         // RegisterEvent, so the common case (a vanilla job-site block) skips the whole pair here instead of
         // leaving a profession no villager can take. Mod POIs register later; the POI pass catches those.
         Set<BlockState> existingClaims = new HashSet<>();
         BuiltInRegistries.POINT_OF_INTEREST_TYPE.forEach(poi -> existingClaims.addAll(poi.matchingStates()));
         int registered = 0;
         for (ProfessionDefinition def : validate().keySet()) {
            if (REJECTED.contains(def.id())) {
               continue;
            }
            if (BuiltInRegistries.VILLAGER_PROFESSION.containsKey(def.id())) {
               reject(def, "its id clashes with an already-registered profession");
               continue;
            }
            BlockState earlyClash = firstAlreadyClaimed(validate().get(def), existingClaims);
            if (earlyClash != null) {
               reject(def, blockClashReason(earlyClash));
               continue;
            }
            ResourceKey<PoiType> poiKey = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, def.id());
            Predicate<Holder<PoiType>> isJobSite = holder -> holder.is(poiKey);
            helper.register(
               def.id(),
               new VillagerProfession(def.id().getPath(), isJobSite, isJobSite, ImmutableSet.of(), ImmutableSet.of(), resolveSound(def.workSound()))
            );
            REGISTERED_PROFESSIONS.add(def.id());
            registered++;
            LOGGER.info("[DynamicVillage] Registered profession {}", def.id());
         }
         if (registered > 0) {
            LOGGER.info("[DynamicVillage] Registered {} data-defined profession(s) from config.", registered);
         }
      });

      event.register(Registries.POINT_OF_INTEREST_TYPE, helper -> {
         Set<BlockState> claimed = new HashSet<>();
         BuiltInRegistries.POINT_OF_INTEREST_TYPE.forEach(poi -> claimed.addAll(poi.matchingStates()));

         validate().forEach((def, states) -> {
            if (REJECTED.contains(def.id())) {
               return;
            }
            if (BuiltInRegistries.POINT_OF_INTEREST_TYPE.containsKey(def.id())) {
               reject(def, "its id clashes with an already-registered point-of-interest type");
               return;
            }
            // Minecraft allows a block state to belong to exactly one POI type; registering a duplicate
            // throws during the registry event and takes the whole game down, so refuse it here instead.
            BlockState clash = firstAlreadyClaimed(states, claimed);
            if (clash != null) {
               reject(def, blockClashReason(clash));
               return;
            }

            helper.register(def.id(), new PoiType(ImmutableSet.copyOf(states), def.effectiveMaxTickets(), def.effectiveSearchDistance()));
            claimed.addAll(states); // so two config professions can't claim the same block either
            REGISTERED_POIS.add(def.id());
            LOGGER.info("[DynamicVillage] Registered POI {} ({} block state(s))", def.id(), states.size());
         });
      });
   }

   private static String blockClashReason(BlockState clash) {
      return "block " + BuiltInRegistries.BLOCK.getKey(clash.getBlock())
         + " already belongs to another point-of-interest type (a block state may only have one)";
   }

   /** Refuses a definition in both passes, explaining any half of the pair that was already registered. */
   private static void reject(ProfessionDefinition def, String reason) {
      REJECTED.add(def.id());
      if (REGISTERED_PROFESSIONS.contains(def.id())) {
         LOGGER.error(
            "[DynamicVillage] Profession {}: {}. The profession itself was already registered, so it exists but no villager "
               + "can ever take the job. Fix its job-site definition.",
            def.id(), reason
         );
      } else if (REGISTERED_POIS.contains(def.id())) {
         LOGGER.error("[DynamicVillage] Profession {}: {}; skipping. Its job site was already registered and stays unused.", def.id(), reason);
      } else {
         LOGGER.error("[DynamicVillage] Profession {}: {}; skipping.", def.id(), reason);
      }
   }

   /**
    * Config-level validation, done exactly once and cached so each problem is reported a single time.
    * Registry clash checks are deliberately NOT here — they need the live registries, see onRegister.
    */
   private static synchronized Map<ProfessionDefinition, Set<BlockState>> validate() {
      if (usableDefinitions != null) {
         return usableDefinitions;
      }

      Map<ProfessionDefinition, Set<BlockState>> usable = new LinkedHashMap<>();
      Set<ResourceLocation> seen = new HashSet<>();

      for (ProfessionDefinition def : definitions()) {
         if (def.jobSiteTag().isPresent()) {
            LOGGER.error(
               "[DynamicVillage] Profession {}: 'job_site_tag' is not supported — block tags come from data packs and are not "
                  + "loaded yet when professions must be registered. List the blocks with 'job_site_blocks' instead. Skipping.",
               def.id()
            );
            continue;
         }
         if (!def.hasValidJobSite()) {
            LOGGER.error("[DynamicVillage] Profession {}: set 'job_site_block' or 'job_site_blocks'; skipping.", def.id());
            continue;
         }
         if (!seen.add(def.id())) {
            LOGGER.error("[DynamicVillage] Profession {} is defined more than once in config; keeping the first and skipping this one.", def.id());
            continue;
         }
         if (!LoadCondition.allMet(def.conditions())) {
            LOGGER.info("[DynamicVillage] Profession {} skipped: its conditions are not met.", def.id());
            continue;
         }

         Set<BlockState> states = resolveStates(def);
         if (states.isEmpty()) {
            LOGGER.warn("[DynamicVillage] Profession {}: none of its job-site blocks are registered (mod not installed?); skipping.", def.id());
            continue;
         }
         usable.put(def, states);
      }

      usableDefinitions = usable;
      return usableDefinitions;
   }

   /** The first state already owned by another POI type, or {@code null} when none of them clash. */
   private static BlockState firstAlreadyClaimed(Set<BlockState> states, Set<BlockState> claimed) {
      for (BlockState state : states) {
         if (claimed.contains(state)) {
            return state;
         }
      }
      return null;
   }

   private static Set<BlockState> resolveStates(ProfessionDefinition def) {
      Set<BlockState> states = new LinkedHashSet<>();
      for (ResourceLocation blockId : def.allJobSiteBlocks()) {
         Optional<Block> block = BuiltInRegistries.BLOCK.getOptional(blockId);
         if (block.isEmpty()) {
            LOGGER.warn("[DynamicVillage] Profession {}: job-site block {} is not registered; ignoring it.", def.id(), blockId);
            continue;
         }
         states.addAll(block.get().getStateDefinition().getPossibleStates());
      }
      return states;
   }

   private static SoundEvent resolveSound(Optional<ResourceLocation> soundId) {
      if (soundId.isPresent()) {
         SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(soundId.get());
         if (sound != null) {
            return sound;
         }
         LOGGER.warn("[DynamicVillage] Work sound {} not found; using the default villager work sound.", soundId.get());
      }
      return SoundEvents.VILLAGER_WORK_TOOLSMITH;
   }

   private static synchronized List<ProfessionDefinition> definitions() {
      if (definitions != null) {
         return definitions;
      }
      List<ProfessionDefinition> list = new ArrayList<>();
      Path dir = FMLPaths.CONFIGDIR.get().resolve(CONFIG_SUBDIR);
      if (Files.isDirectory(dir)) {
         try (Stream<Path> paths = Files.list(dir)) {
            List<Path> files = paths.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().toList();
            for (Path file : files) {
               try (Reader reader = Files.newBufferedReader(file)) {
                  JsonElement json = JsonParser.parseReader(reader);
                  var parsed = ProfessionDefinition.CODEC.parse(JsonOps.INSTANCE, json);
                  if (parsed.error().isPresent()) {
                     LOGGER.warn("[DynamicVillage] Skipping invalid profession file {}: {}", file.getFileName(), parsed.error().get().message());
                     continue;
                  }
                  list.add(parsed.result().orElseThrow());
               } catch (Exception e) {
                  LOGGER.warn("[DynamicVillage] Could not read profession file {}: {}", file.getFileName(), e.toString());
               }
            }
         } catch (IOException e) {
            LOGGER.warn("[DynamicVillage] Could not scan profession config directory {}: {}", dir, e.toString());
         }
      }
      definitions = list;
      LOGGER.info("[DynamicVillage] Loaded {} profession definition(s) from config ({}).", list.size(), dir);
      return definitions;
   }
}
