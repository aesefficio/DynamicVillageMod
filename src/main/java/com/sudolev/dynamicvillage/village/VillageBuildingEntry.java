package com.sudolev.dynamicvillage.village;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sudolev.dynamicvillage.condition.LoadCondition;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool.Projection;

/**
 * A single data-driven village building definition, loaded from
 * {@code data/<namespace>/dynamicvillage/buildings/<name>.json}.
 *
 * <p>Data pack authors only need {@code pool} and {@code structure}; everything else has a sensible
 * default. Example:
 * <pre>
 * {
 *   "pool": "minecraft:village/plains/houses",
 *   "structure": "mypack:houses/cool_house",
 *   "weight": 5
 * }
 * </pre>
 *
 * @param pool       the village template pool to inject this building into (e.g. {@code minecraft:village/plains/houses})
 * @param structure  the NBT structure to place (any namespace; the game loads it from any data pack)
 * @param weight     relative weight among the custom buildings in this pool (defaults to 1)
 * @param projection {@code rigid} or {@code terrain_matching} (defaults to {@code rigid})
 * @param processors the processor list to apply (defaults to {@code minecraft:empty})
 * @param conditions optional load conditions; the building is skipped if any is unmet (see {@link LoadCondition})
 */
public record VillageBuildingEntry(
   ResourceLocation pool,
   ResourceLocation structure,
   int weight,
   Projection projection,
   ResourceLocation processors,
   List<LoadCondition> conditions
) {
   private static final Codec<Projection> PROJECTION_CODEC = StringRepresentable.fromEnum(Projection::values);

   public static final Codec<VillageBuildingEntry> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("pool").forGetter(VillageBuildingEntry::pool),
            ResourceLocation.CODEC.fieldOf("structure").forGetter(VillageBuildingEntry::structure),
            Codec.INT.optionalFieldOf("weight", 1).forGetter(VillageBuildingEntry::weight),
            PROJECTION_CODEC.optionalFieldOf("projection", Projection.RIGID).forGetter(VillageBuildingEntry::projection),
            ResourceLocation.CODEC
               .optionalFieldOf("processors", ResourceLocation.withDefaultNamespace("empty"))
               .forGetter(VillageBuildingEntry::processors),
            LoadCondition.CODEC.listOf().optionalFieldOf("conditions", List.of()).forGetter(VillageBuildingEntry::conditions)
         )
         .apply(instance, VillageBuildingEntry::new)
   );
}
