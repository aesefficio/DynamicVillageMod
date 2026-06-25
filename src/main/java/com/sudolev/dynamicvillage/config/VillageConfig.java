package com.sudolev.dynamicvillage.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class VillageConfig {
   public static final ModConfigSpec SPEC;
   public static final ModConfigSpec.IntValue BUILDING_SPAWN_CHANCE;

   static {
      ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

      builder.push("village");
      BUILDING_SPAWN_CHANCE = builder
         .comment(
            "The chance (as a percentage) that a house slot in a village will be replaced with one of this mod's custom buildings.",
            "0 = vanilla houses only, 100 = only this mod's buildings will spawn."
         )
         .defineInRange("buildingSpawnChancePercent", 15, 0, 100);
      builder.pop();

      SPEC = builder.build();
   }
}
