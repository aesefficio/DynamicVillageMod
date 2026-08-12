package com.sudolev.dynamicvillage.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * A structure processor that stamps a loot table onto (trapped) chests as a structure is placed,
 * turning "empty chest" pieces into vanilla-style randomized chests. Data pack authors never touch
 * this directly: {@link com.sudolev.dynamicvillage.village.VillageAddition} attaches it to a building
 * when that building's structure has a loot assignment (see
 * {@link com.sudolev.dynamicvillage.village.LootAssignmentLoader}).
 *
 * <p>By default it only fills chests that have no {@code LootTable} yet, so hand-authored loot chests
 * are preserved. {@code overrideExisting} forces it to replace even an authored table.
 */
public class ChestLootProcessor extends StructureProcessor {
   public static final Codec<ChestLootProcessor> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("loot_table").forGetter(p -> p.lootTable),
            Codec.BOOL.optionalFieldOf("override_existing", false).forGetter(p -> p.overrideExisting)
         )
         .apply(instance, ChestLootProcessor::new)
   );

   private final ResourceLocation lootTable;
   private final boolean overrideExisting;

   public ChestLootProcessor(ResourceLocation lootTable, boolean overrideExisting) {
      this.lootTable = lootTable;
      this.overrideExisting = overrideExisting;
   }

   @Override
   public StructureTemplate.StructureBlockInfo processBlock(
      LevelReader level,
      BlockPos offset,
      BlockPos pos,
      StructureTemplate.StructureBlockInfo blockInfo,
      StructureTemplate.StructureBlockInfo relative,
      StructurePlaceSettings settings
   ) {
      BlockState state = relative.state();
      boolean isChest = state.is(Blocks.CHEST) || state.is(Blocks.TRAPPED_CHEST);
      if (!isChest) {
         return relative;
      }

      CompoundTag nbt = relative.nbt();
      if (nbt != null && nbt.contains("LootTable") && !overrideExisting) {
         return relative; // hand-authored loot chest: leave it alone
      }

      // A chest saved without block-entity data would otherwise stay empty forever, so synthesise the
      // minimal tag rather than skipping it — an empty chest is the exact bug this processor exists for.
      CompoundTag copy = nbt == null ? new CompoundTag() : nbt.copy();
      if (!copy.contains("id")) {
         copy.putString("id", BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
      }
      copy.putString("LootTable", lootTable.toString());
      // Mix the table id into the position so two buildings sharing a spot-in-the-world don't roll
      // identical contents, while staying deterministic for a given placement.
      copy.putLong("LootTableSeed", relative.pos().asLong() * 31L + lootTable.hashCode());
      copy.remove("Items"); // let the loot table fully own the contents
      return new StructureTemplate.StructureBlockInfo(relative.pos(), state, copy);
   }

   @Override
   protected StructureProcessorType<?> getType() {
      return ModProcessors.CHEST_LOOT.get();
   }

   @Override
   public boolean equals(Object other) {
      return other instanceof ChestLootProcessor p && p.overrideExisting == overrideExisting && p.lootTable.equals(lootTable);
   }

   @Override
   public int hashCode() {
      return lootTable.hashCode() * 31 + Boolean.hashCode(overrideExisting);
   }
}
