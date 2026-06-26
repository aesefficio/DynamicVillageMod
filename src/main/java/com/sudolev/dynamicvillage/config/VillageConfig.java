package com.sudolev.dynamicvillage.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class VillageConfig {
   public static final ModConfigSpec SPEC;
   public static final ModConfigSpec.IntValue BUILDING_SPAWN_CHANCE;
   public static final ModConfigSpec.IntValue DECORATION_DENSITY;

   static {
      ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

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

      SPEC = builder.build();
   }
}
