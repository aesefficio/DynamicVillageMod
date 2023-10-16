package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class PlacedBlockTrigger extends SimpleCriterionTrigger<PlacedBlockTrigger.TriggerInstance> {
   static final ResourceLocation ID = new ResourceLocation("placed_block");

   public ResourceLocation getId() {
      return ID;
   }

   public PlacedBlockTrigger.TriggerInstance createInstance(JsonObject pJson, EntityPredicate.Composite pEntityPredicate, DeserializationContext pConditionsParser) {
      Block block = deserializeBlock(pJson);
      StatePropertiesPredicate statepropertiespredicate = StatePropertiesPredicate.fromJson(pJson.get("state"));
      if (block != null) {
         statepropertiespredicate.checkState(block.getStateDefinition(), (p_59475_) -> {
            throw new JsonSyntaxException("Block " + block + " has no property " + p_59475_ + ":");
         });
      }

      LocationPredicate locationpredicate = LocationPredicate.fromJson(pJson.get("location"));
      ItemPredicate itempredicate = ItemPredicate.fromJson(pJson.get("item"));
      return new PlacedBlockTrigger.TriggerInstance(pEntityPredicate, block, statepropertiespredicate, locationpredicate, itempredicate);
   }

   @Nullable
   private static Block deserializeBlock(JsonObject pObject) {
      if (pObject.has("block")) {
         ResourceLocation resourcelocation = new ResourceLocation(GsonHelper.getAsString(pObject, "block"));
         return Registry.BLOCK.getOptional(resourcelocation).orElseThrow(() -> {
            return new JsonSyntaxException("Unknown block type '" + resourcelocation + "'");
         });
      } else {
         return null;
      }
   }

   public void trigger(ServerPlayer pPlayer, BlockPos pPos, ItemStack pItem) {
      BlockState blockstate = pPlayer.getLevel().getBlockState(pPos);
      this.trigger(pPlayer, (p_59481_) -> {
         return p_59481_.matches(blockstate, pPos, pPlayer.getLevel(), pItem);
      });
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      @Nullable
      private final Block block;
      private final StatePropertiesPredicate state;
      private final LocationPredicate location;
      private final ItemPredicate item;

      public TriggerInstance(EntityPredicate.Composite pPlayer, @Nullable Block pBlock, StatePropertiesPredicate pState, LocationPredicate pLocation, ItemPredicate pItem) {
         super(PlacedBlockTrigger.ID, pPlayer);
         this.block = pBlock;
         this.state = pState;
         this.location = pLocation;
         this.item = pItem;
      }

      public static PlacedBlockTrigger.TriggerInstance placedBlock(Block pBlock) {
         return new PlacedBlockTrigger.TriggerInstance(EntityPredicate.Composite.ANY, pBlock, StatePropertiesPredicate.ANY, LocationPredicate.ANY, ItemPredicate.ANY);
      }

      public boolean matches(BlockState pState, BlockPos pPos, ServerLevel pLevel, ItemStack pItem) {
         if (this.block != null && !pState.is(this.block)) {
            return false;
         } else if (!this.state.matches(pState)) {
            return false;
         } else if (!this.location.matches(pLevel, (double)pPos.getX(), (double)pPos.getY(), (double)pPos.getZ())) {
            return false;
         } else {
            return this.item.matches(pItem);
         }
      }

      public JsonObject serializeToJson(SerializationContext pConditions) {
         JsonObject jsonobject = super.serializeToJson(pConditions);
         if (this.block != null) {
            jsonobject.addProperty("block", Registry.BLOCK.getKey(this.block).toString());
         }

         jsonobject.add("state", this.state.serializeToJson());
         jsonobject.add("location", this.location.serializeToJson());
         jsonobject.add("item", this.item.serializeToJson());
         return jsonobject;
      }
   }
}