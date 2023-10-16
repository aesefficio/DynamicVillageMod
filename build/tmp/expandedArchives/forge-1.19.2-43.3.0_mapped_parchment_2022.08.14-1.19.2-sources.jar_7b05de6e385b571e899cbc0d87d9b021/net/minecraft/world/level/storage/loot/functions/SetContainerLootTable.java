package net.minecraft.world.level.storage.loot.functions;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * LootItemFunction that sets the LootTable and optionally the loot table seed on the stack's {@code BlockEntityTag}.
 * The effect of this is that containers such as chests will receive the given LootTable when placed.
 */
public class SetContainerLootTable extends LootItemConditionalFunction {
   final ResourceLocation name;
   final long seed;
   final BlockEntityType<?> type;

   SetContainerLootTable(LootItemCondition[] pConditions, ResourceLocation pName, long pSeed, BlockEntityType<?> pType) {
      super(pConditions);
      this.name = pName;
      this.seed = pSeed;
      this.type = pType;
   }

   public LootItemFunctionType getType() {
      return LootItemFunctions.SET_LOOT_TABLE;
   }

   /**
    * Called to perform the actual action of this function, after conditions have been checked.
    */
   public ItemStack run(ItemStack pStack, LootContext pContext) {
      if (pStack.isEmpty()) {
         return pStack;
      } else {
         CompoundTag compoundtag = BlockItem.getBlockEntityData(pStack);
         if (compoundtag == null) {
            compoundtag = new CompoundTag();
         }

         compoundtag.putString("LootTable", this.name.toString());
         if (this.seed != 0L) {
            compoundtag.putLong("LootTableSeed", this.seed);
         }

         BlockItem.setBlockEntityData(pStack, this.type, compoundtag);
         return pStack;
      }
   }

   /**
    * Validate that this object is used correctly according to the given ValidationContext.
    */
   public void validate(ValidationContext pContext) {
      if (pContext.hasVisitedTable(this.name)) {
         pContext.reportProblem("Table " + this.name + " is recursively called");
      } else {
         super.validate(pContext);
         LootTable loottable = pContext.resolveLootTable(this.name);
         if (loottable == null) {
            pContext.reportProblem("Unknown loot table called " + this.name);
         } else {
            loottable.validate(pContext.enterTable("->{" + this.name + "}", this.name));
         }

      }
   }

   public static LootItemConditionalFunction.Builder<?> withLootTable(BlockEntityType<?> p_193050_, ResourceLocation p_193051_) {
      return simpleBuilder((p_193064_) -> {
         return new SetContainerLootTable(p_193064_, p_193051_, 0L, p_193050_);
      });
   }

   public static LootItemConditionalFunction.Builder<?> withLootTable(BlockEntityType<?> p_193053_, ResourceLocation p_193054_, long p_193055_) {
      return simpleBuilder((p_193060_) -> {
         return new SetContainerLootTable(p_193060_, p_193054_, p_193055_, p_193053_);
      });
   }

   public static class Serializer extends LootItemConditionalFunction.Serializer<SetContainerLootTable> {
      /**
       * Serialize the value by putting its data into the JsonObject.
       */
      public void serialize(JsonObject pJson, SetContainerLootTable pValue, JsonSerializationContext pSerializationContext) {
         super.serialize(pJson, pValue, pSerializationContext);
         pJson.addProperty("name", pValue.name.toString());
         pJson.addProperty("type", Registry.BLOCK_ENTITY_TYPE.getKey(pValue.type).toString());
         if (pValue.seed != 0L) {
            pJson.addProperty("seed", pValue.seed);
         }

      }

      public SetContainerLootTable deserialize(JsonObject pObject, JsonDeserializationContext pDeserializationContext, LootItemCondition[] pConditions) {
         ResourceLocation resourcelocation = new ResourceLocation(GsonHelper.getAsString(pObject, "name"));
         long i = GsonHelper.getAsLong(pObject, "seed", 0L);
         ResourceLocation resourcelocation1 = new ResourceLocation(GsonHelper.getAsString(pObject, "type"));
         BlockEntityType<?> blockentitytype = Registry.BLOCK_ENTITY_TYPE.getOptional(resourcelocation1).orElseThrow(() -> {
            return new JsonSyntaxException("Unknown block entity type id '" + resourcelocation1 + "'");
         });
         return new SetContainerLootTable(pConditions, resourcelocation, i, blockentitytype);
      }
   }
}