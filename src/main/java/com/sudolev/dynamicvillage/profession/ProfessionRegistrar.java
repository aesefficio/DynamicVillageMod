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

   private ProfessionRegistrar() {
   }

   /** Wire this registrar onto the mod event bus (called from the mod constructor). */
   public static void register(IEventBus modEventBus) {
      modEventBus.addListener(ProfessionRegistrar::onRegister);
   }

   private static void onRegister(RegisterEvent event) {
      // POIs first (they exist before professions reference them).
      event.register(Registries.POINT_OF_INTEREST_TYPE, helper -> validate().forEach((def, states) -> {
         helper.register(def.id(), new PoiType(ImmutableSet.copyOf(states), def.effectiveMaxTickets(), def.effectiveSearchDistance()));
         LOGGER.info("[DynamicVillage] Registered POI {} ({} block state(s))", def.id(), states.size());
      }));

      event.register(Registries.VILLAGER_PROFESSION, helper -> {
         Map<ProfessionDefinition, Set<BlockState>> usable = validate();
         for (ProfessionDefinition def : usable.keySet()) {
            ResourceKey<PoiType> poiKey = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, def.id());
            Predicate<Holder<PoiType>> isJobSite = holder -> holder.is(poiKey);
            helper.register(
               def.id(),
               new VillagerProfession(def.id().getPath(), isJobSite, isJobSite, ImmutableSet.of(), ImmutableSet.of(), resolveSound(def.workSound()))
            );
            LOGGER.info("[DynamicVillage] Registered profession {}", def.id());
         }
         if (!usable.isEmpty()) {
            LOGGER.info("[DynamicVillage] Registered {} data-defined profession(s) from config.", usable.size());
         }
      });
   }

   /**
    * Validates every definition exactly once and caches the result, so both registry passes agree and
    * each problem is reported a single time. Insertion order is preserved for deterministic output.
    */
   private static synchronized Map<ProfessionDefinition, Set<BlockState>> validate() {
      if (usableDefinitions != null) {
         return usableDefinitions;
      }

      Map<ProfessionDefinition, Set<BlockState>> usable = new LinkedHashMap<>();
      Set<ResourceLocation> seen = new HashSet<>();
      // Every block state already spoken for by a registered POI type, plus the ones our own accepted
      // definitions claim as we go, so two config professions can't fight over the same block either.
      Set<BlockState> claimed = new HashSet<>();
      BuiltInRegistries.POINT_OF_INTEREST_TYPE.forEach(poi -> claimed.addAll(poi.matchingStates()));

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
         if (BuiltInRegistries.VILLAGER_PROFESSION.containsKey(def.id()) || BuiltInRegistries.POINT_OF_INTEREST_TYPE.containsKey(def.id())) {
            LOGGER.error("[DynamicVillage] Profession {} clashes with an already-registered profession/POI id; skipping.", def.id());
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

         // Minecraft allows a block state to belong to exactly one POI type; registering a duplicate
         // throws during the registry event and takes the whole game down, so refuse it here instead.
         BlockState clash = firstAlreadyClaimed(states, claimed);
         if (clash != null) {
            LOGGER.error(
               "[DynamicVillage] Profession {}: block {} already belongs to another point-of-interest type "
                  + "(a block state may only have one). Pick a different job-site block; skipping.",
               def.id(), BuiltInRegistries.BLOCK.getKey(clash.getBlock())
            );
            continue;
         }

         claimed.addAll(states);
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
