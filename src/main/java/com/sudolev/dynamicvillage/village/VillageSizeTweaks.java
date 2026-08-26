package com.sudolev.dynamicvillage.village;

import com.mojang.logging.LogUtils;
import com.sudolev.dynamicvillage.VillageLife;
import com.sudolev.dynamicvillage.config.VillageConfig;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import org.slf4j.Logger;

/**
 * Applies the configured village size and reach to the vanilla village structures when the world
 * starts.
 *
 * <p>A jigsaw structure's {@code size} is how many times its layout expands outward from the town
 * centre, and {@code maxDistanceFromCenter} is how far a piece may end up from that centre. Vanilla
 * ships every village at 6 and 80 respectively — identical across biomes — so this is the knob for
 * "make desert villages bigger". Both fields are normally final, hence the access transformer.
 *
 * <p>Like the building injection, this edits the per-world structure registry at load, so it affects
 * villages generated from now on and leaves existing ones untouched.
 */
@Mod.EventBusSubscriber(
   modid = VillageLife.MODID
)
public class VillageSizeTweaks {
   private static final Logger LOGGER = LogUtils.getLogger();

   @SubscribeEvent
   public static void onServerAboutToStart(ServerAboutToStartEvent event) {
      Registry<Structure> structures = event.getServer().registryAccess().registryOrThrow(Registries.STRUCTURE);

      for (Map.Entry<String, VillageConfig.VillageSize> entry : VillageConfig.VILLAGE_SIZES.entrySet()) {
         ResourceLocation id = new ResourceLocation(entry.getKey());
         Structure structure = structures.get(id);
         if (!(structure instanceof JigsawStructure village)) {
            // Another mod may have replaced or removed the structure; leave it alone rather than guess.
            LOGGER.debug("[DynamicVillage] {} is missing or not a jigsaw structure; leaving its size alone.", id);
            continue;
         }

         int size = entry.getValue().size().get();
         int distance = entry.getValue().maxDistance().get();
         if (village.maxDepth == size && village.maxDistanceFromCenter == distance) {
            continue; // already at the configured values, nothing to report
         }

         LOGGER.info(
            "[DynamicVillage] {}: size {} -> {}, max distance {} -> {}",
            id, village.maxDepth, size, village.maxDistanceFromCenter, distance
         );
         village.maxDepth = size;
         village.maxDistanceFromCenter = distance;
      }
   }
}
