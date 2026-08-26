package com.sudolev.dynamicvillage.config;

import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

public class VillageConfig {
   public static final ForgeConfigSpec SPEC;
   public static final ForgeConfigSpec.IntValue BUILDING_SPAWN_CHANCE;
   public static final ForgeConfigSpec.IntValue DECORATION_DENSITY;

   public static final ForgeConfigSpec.DoubleValue PLAINS_DENSITY;
   public static final ForgeConfigSpec.DoubleValue DESERT_DENSITY;
   public static final ForgeConfigSpec.DoubleValue SAVANNA_DENSITY;
   public static final ForgeConfigSpec.DoubleValue SNOWY_DENSITY;
   public static final ForgeConfigSpec.DoubleValue TAIGA_DENSITY;

   public static final ForgeConfigSpec.IntValue PLAINS_SIZE;
   public static final ForgeConfigSpec.IntValue PLAINS_DISTANCE;
   public static final ForgeConfigSpec.IntValue DESERT_SIZE;
   public static final ForgeConfigSpec.IntValue DESERT_DISTANCE;
   public static final ForgeConfigSpec.IntValue SAVANNA_SIZE;
   public static final ForgeConfigSpec.IntValue SAVANNA_DISTANCE;
   public static final ForgeConfigSpec.IntValue SNOWY_SIZE;
   public static final ForgeConfigSpec.IntValue SNOWY_DISTANCE;
   public static final ForgeConfigSpec.IntValue TAIGA_SIZE;
   public static final ForgeConfigSpec.IntValue TAIGA_DISTANCE;

   /** Vanilla village structure id -> its configured size / max distance. */
   public static final Map<String, VillageSize> VILLAGE_SIZES;

   /** Vanilla house pool -> the config value that scales it. */
   private static final Map<String, java.util.function.Supplier<Double>> BY_POOL;

   /** The configured generation bounds for one village type. */
   public record VillageSize(ForgeConfigSpec.IntValue size, ForgeConfigSpec.IntValue maxDistance) {
   }

   static {
      ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

      builder.push("village");
      BUILDING_SPAWN_CHANCE = builder
         .comment(
            "The chance (as a percentage) that a house slot in a village will be replaced with one of this mod's custom buildings.",
            "0 = vanilla houses only, 100 = only this mod's buildings will spawn."
         )
         .defineInRange("buildingSpawnChancePercent", 35, 0, 100);

      DECORATION_DENSITY = builder
         .comment(
            "Multiplier for how often vanilla village decorations (lamp posts, flowers, hay, small props) spawn.",
            "1 = vanilla (no change). Higher values fill more of the empty decoration spots, making villages feel fuller.",
            "Note: this only affects the decoration spots villages already provide; it cannot fill the large empty gaps between plots."
         )
         .defineInRange("decorationDensityMultiplier", 2, 1, 16);
      builder.pop();

      builder.comment(
            "Per-biome tuning for how many of this mod's buildings appear, on top of buildingSpawnChancePercent.",
            "1.0 = no change, 2.0 = twice as many custom buildings in that biome's villages, 0 = none.",
            "Use this if one biome's villages feel too sparse or too crowded with modded buildings compared to the others."
         )
         .push("biomeDensity");
      PLAINS_DENSITY = density(builder, "plains");
      DESERT_DENSITY = density(builder, "desert");
      SAVANNA_DENSITY = density(builder, "savanna");
      SNOWY_DENSITY = density(builder, "snowy");
      TAIGA_DENSITY = density(builder, "taiga");
      builder.pop();

      builder.comment(
            "How large each village generates. These override vanilla for every village of that type.",
            "size = how many times the village layout expands outward from the town centre (vanilla 6).",
            "maxDistance = how far from the centre, in blocks, a piece may be placed (vanilla 80).",
            "The defaults below are this mod's tuned values, not vanilla's - set every size to 6 and every",
            "maxDistance to 80 if you want vanilla-sized villages back.",
            "Raising these makes bigger villages but costs more world generation time; a large size with a",
            "small maxDistance is wasted, because pieces beyond the distance are discarded."
         )
         .push("villageSize");
      PLAINS_SIZE = size(builder, "plains", 7);
      PLAINS_DISTANCE = distance(builder, "plains", 60);
      DESERT_SIZE = size(builder, "desert", 7);
      DESERT_DISTANCE = distance(builder, "desert", 80);
      SAVANNA_SIZE = size(builder, "savanna", 7);
      SAVANNA_DISTANCE = distance(builder, "savanna", 60);
      SNOWY_SIZE = size(builder, "snowy", 6);
      SNOWY_DISTANCE = distance(builder, "snowy", 80);
      TAIGA_SIZE = size(builder, "taiga", 6);
      TAIGA_DISTANCE = distance(builder, "taiga", 80);
      builder.pop();

      SPEC = builder.build();

      VILLAGE_SIZES = Map.of(
         "minecraft:village_plains", new VillageSize(PLAINS_SIZE, PLAINS_DISTANCE),
         "minecraft:village_desert", new VillageSize(DESERT_SIZE, DESERT_DISTANCE),
         "minecraft:village_savanna", new VillageSize(SAVANNA_SIZE, SAVANNA_DISTANCE),
         "minecraft:village_snowy", new VillageSize(SNOWY_SIZE, SNOWY_DISTANCE),
         "minecraft:village_taiga", new VillageSize(TAIGA_SIZE, TAIGA_DISTANCE)
      );

      BY_POOL = Map.of(
         "minecraft:village/plains/houses", PLAINS_DENSITY::get,
         "minecraft:village/desert/houses", DESERT_DENSITY::get,
         "minecraft:village/savanna/houses", SAVANNA_DENSITY::get,
         "minecraft:village/snowy/houses", SNOWY_DENSITY::get,
         "minecraft:village/taiga/houses", TAIGA_DENSITY::get
      );
   }

   private static ForgeConfigSpec.IntValue size(ForgeConfigSpec.Builder builder, String biome, int defaultSize) {
      return builder
         .comment("How far the " + biome + " village layout expands from its centre. Vanilla is 6; the game's own limit is 20.")
         .defineInRange(biome + "Size", defaultSize, 0, 20);
   }

   private static ForgeConfigSpec.IntValue distance(ForgeConfigSpec.Builder builder, String biome, int defaultDistance) {
      return builder
         .comment("Furthest a " + biome + " village piece may be placed from the centre, in blocks. Vanilla is 80; the game's own limit is 128.")
         .defineInRange(biome + "MaxDistance", defaultDistance, 1, 128);
   }

   private static ForgeConfigSpec.DoubleValue density(ForgeConfigSpec.Builder builder, String biome) {
      return builder
         .comment("Multiplier for custom buildings in " + biome + " villages. 1.0 = unchanged.")
         .defineInRange(biome + "Density", 1.0D, 0.0D, 100.0D);
   }

   /**
    * The configured multiplier for a village pool, or {@code 1.0} for pools the config doesn't cover
    * (modded biomes, non-house pools). Data packs can still scale those via
    * {@code dynamicvillage/pools/…}; the two multiply together.
    */
   public static double densityFor(ResourceLocation pool) {
      var value = BY_POOL.get(pool.toString());
      return value == null ? 1.0D : value.get();
   }
}
