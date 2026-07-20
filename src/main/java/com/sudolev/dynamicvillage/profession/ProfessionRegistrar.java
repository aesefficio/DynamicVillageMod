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
import java.util.LinkedHashSet;
import java.util.List;
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
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.RegisterEvent;
import org.slf4j.Logger;

/**
 * Registers data-defined villager professions (see {@link ProfessionDefinition}) from
 * {@code config/dynamicvillage/professions/*.json} during the registry phase.
 *
 * <p>Professions and POIs live in frozen built-in registries populated at mod load, so definitions
 * are read from the config directory (available that early) rather than from world data packs. Each
 * definition registers one POI type and one profession that uses it; a definition's trades and
 * buildings then use the normal data-pack systems.
 */
public final class ProfessionRegistrar {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String CONFIG_SUBDIR = "dynamicvillage/professions";

   /** Parsed once, lazily, on the first registry callback; reused for POIs and professions. */
   private static List<ProfessionDefinition> definitions;

   private ProfessionRegistrar() {
   }

   /** Wire this registrar onto the mod event bus (called from the mod constructor). */
   public static void register(IEventBus modEventBus) {
      modEventBus.addListener(ProfessionRegistrar::onRegister);
   }

   private static void onRegister(RegisterEvent event) {
      // POIs first (they exist before professions reference them).
      event.register(Registries.POINT_OF_INTEREST_TYPE, helper -> {
         for (ProfessionDefinition def : usableDefinitions()) {
            Set<BlockState> states = resolveStates(def);
            if (states.isEmpty()) {
               LOGGER.warn("[DynamicVillage] Profession {}: job site has no valid block states; skipping.", def.id());
               continue;
            }
            helper.register(def.id(), new PoiType(ImmutableSet.copyOf(states), def.maxTickets(), def.searchDistance()));
            LOGGER.info("[DynamicVillage] Registered POI {} ({} state(s)) for profession {}", def.id(), states.size(), def.id());
         }
      });

      event.register(Registries.VILLAGER_PROFESSION, helper -> {
         int registered = 0;
         for (ProfessionDefinition def : usableDefinitions()) {
            if (resolveStates(def).isEmpty()) {
               continue; // POI was not registered above; keep them consistent.
            }
            ResourceKey<PoiType> poiKey = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, def.id());
            Predicate<Holder<PoiType>> isJobSite = holder -> holder.is(poiKey);
            SoundEvent workSound = resolveSound(def.workSound());
            helper.register(
               def.id(),
               new VillagerProfession(def.id().getPath(), isJobSite, isJobSite, ImmutableSet.of(), ImmutableSet.of(), workSound)
            );
            registered++;
            LOGGER.info("[DynamicVillage] Registered profession {}", def.id());
         }
         if (registered > 0) {
            LOGGER.info("[DynamicVillage] Registered {} data-defined profession(s) from config.", registered);
         }
      });
   }

   /** Definitions that are structurally valid and whose conditions are met. */
   private static List<ProfessionDefinition> usableDefinitions() {
      List<ProfessionDefinition> usable = new ArrayList<>();
      for (ProfessionDefinition def : definitions()) {
         if (!def.hasValidJobSite()) {
            LOGGER.warn("[DynamicVillage] Profession {}: set exactly one of 'job_site_block' or 'job_site_tag'; skipping.", def.id());
            continue;
         }
         if (!LoadCondition.allMet(def.conditions())) {
            LOGGER.info("[DynamicVillage] Profession {} skipped: its conditions are not met.", def.id());
            continue;
         }
         usable.add(def);
      }
      return usable;
   }

   private static Set<BlockState> resolveStates(ProfessionDefinition def) {
      Set<BlockState> states = new LinkedHashSet<>();
      if (def.jobSiteBlock().isPresent()) {
         BuiltInRegistries.BLOCK.getOptional(def.jobSiteBlock().get())
            .ifPresent(block -> states.addAll(block.getStateDefinition().getPossibleStates()));
      } else if (def.jobSiteTag().isPresent()) {
         TagKey<Block> tag = TagKey.create(Registries.BLOCK, def.jobSiteTag().get());
         BuiltInRegistries.BLOCK.getTag(tag)
            .ifPresent(named -> named.forEach(holder -> states.addAll(holder.value().getStateDefinition().getPossibleStates())));
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
