package com.sudolev.dynamicvillage.village;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sudolev.dynamicvillage.condition.LoadCondition;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/**
 * Per-pool tuning for how many custom buildings a single village pool receives, loaded from
 * {@code data/<namespace>/dynamicvillage/pools/<pool_path>.json} (see {@link PoolSettingsLoader}).
 *
 * <p>The file's own resource id mirrors the pool it applies to, exactly like loot assignments mirror
 * their structure: settings for pool {@code minecraft:village/desert/houses} live at
 * {@code data/minecraft/dynamicvillage/pools/village/desert/houses.json}.
 *
 * <p>The end-user config {@code buildingSpawnChancePercent} still sets the overall custom-vs-vanilla
 * budget; this multiplies that budget for one pool only. It is the only way to make one biome carry
 * more (or fewer) custom buildings than another — a building's own {@code weight} is relative to the
 * other custom buildings in the same pool, so it can never change that pool's total share.
 *
 * <pre>
 * { "weight_multiplier": 2.0 }
 * </pre>
 *
 * @param weightMultiplier scales this pool's custom-building budget. {@code 1.0} = unchanged,
 *                         {@code 2.0} = twice as many custom buildings, {@code 0.0} = none.
 * @param conditions       optional load conditions; the settings are ignored if any is unmet
 */
public record PoolSettings(double weightMultiplier, List<LoadCondition> conditions) {
   /** Used when a pool has no settings file at all. */
   public static final PoolSettings DEFAULT = new PoolSettings(1.0D, List.of());

   public static final Codec<PoolSettings> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            Codec.DOUBLE.optionalFieldOf("weight_multiplier", 1.0D).forGetter(PoolSettings::weightMultiplier),
            LoadCondition.CODEC.listOf().optionalFieldOf("conditions", List.of()).forGetter(PoolSettings::conditions)
         )
         .apply(instance, PoolSettings::new)
   );

   /** The multiplier, clamped to a sane range so a typo cannot allocate an enormous pool. */
   public double effectiveMultiplier() {
      return Math.max(0.0D, Math.min(100.0D, weightMultiplier));
   }
}
