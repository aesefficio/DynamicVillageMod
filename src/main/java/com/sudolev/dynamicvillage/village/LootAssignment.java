package com.sudolev.dynamicvillage.village;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sudolev.dynamicvillage.condition.LoadCondition;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/**
 * A loot-table assignment for a building's chests, loaded from
 * {@code data/<namespace>/dynamicvillage/loot/<structure_path>.json} (see {@link LootAssignmentLoader}).
 *
 * <p>The file's own resource id mirrors the structure it applies to: the assignment for structure
 * {@code ns:path} lives at {@code data/ns/dynamicvillage/loot/path.json}. To re-loot another pack's
 * (or this mod's) building, drop a file at that same path — standard data pack priority applies.
 *
 * <pre>
 * { "loot_table": "dynamicvillage:chests/village/miner" }
 * </pre>
 *
 * @param lootTable        loot table id stamped onto the building's chests
 * @param overrideExisting also replace chests that were hand-authored with their own loot table
 *                         (defaults to false, so authored chests are preserved)
 * @param conditions       optional load conditions; the assignment is skipped if any is unmet
 *                         (see {@link LoadCondition}) — e.g. loot that only applies with another mod
 */
public record LootAssignment(ResourceLocation lootTable, boolean overrideExisting, List<LoadCondition> conditions) {
   public static final Codec<LootAssignment> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("loot_table").forGetter(LootAssignment::lootTable),
            Codec.BOOL.optionalFieldOf("override_existing", false).forGetter(LootAssignment::overrideExisting),
            LoadCondition.CODEC.listOf().optionalFieldOf("conditions", List.of()).forGetter(LootAssignment::conditions)
         )
         .apply(instance, LootAssignment::new)
   );
}
