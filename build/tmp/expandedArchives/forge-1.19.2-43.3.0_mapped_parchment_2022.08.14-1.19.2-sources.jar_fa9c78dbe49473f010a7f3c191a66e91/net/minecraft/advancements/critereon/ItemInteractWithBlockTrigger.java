package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class ItemInteractWithBlockTrigger extends SimpleCriterionTrigger<ItemInteractWithBlockTrigger.TriggerInstance> {
   final ResourceLocation id;

   public ItemInteractWithBlockTrigger(ResourceLocation pId) {
      this.id = pId;
   }

   public ResourceLocation getId() {
      return this.id;
   }

   public ItemInteractWithBlockTrigger.TriggerInstance createInstance(JsonObject pJson, EntityPredicate.Composite pPlayer, DeserializationContext pContext) {
      LocationPredicate locationpredicate = LocationPredicate.fromJson(pJson.get("location"));
      ItemPredicate itempredicate = ItemPredicate.fromJson(pJson.get("item"));
      return new ItemInteractWithBlockTrigger.TriggerInstance(this.id, pPlayer, locationpredicate, itempredicate);
   }

   public void trigger(ServerPlayer pPlayer, BlockPos pPos, ItemStack pStack) {
      BlockState blockstate = pPlayer.getLevel().getBlockState(pPos);
      this.trigger(pPlayer, (p_220053_) -> {
         return p_220053_.matches(blockstate, pPlayer.getLevel(), pPos, pStack);
      });
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final LocationPredicate location;
      private final ItemPredicate item;

      public TriggerInstance(ResourceLocation pCriterion, EntityPredicate.Composite pPlayer, LocationPredicate pLocation, ItemPredicate pItem) {
         super(pCriterion, pPlayer);
         this.location = pLocation;
         this.item = pItem;
      }

      public static ItemInteractWithBlockTrigger.TriggerInstance itemUsedOnBlock(LocationPredicate.Builder pLocation, ItemPredicate.Builder pItem) {
         return new ItemInteractWithBlockTrigger.TriggerInstance(CriteriaTriggers.ITEM_USED_ON_BLOCK.id, EntityPredicate.Composite.ANY, pLocation.build(), pItem.build());
      }

      public static ItemInteractWithBlockTrigger.TriggerInstance allayDropItemOnBlock(LocationPredicate.Builder pLocation, ItemPredicate.Builder pItem) {
         return new ItemInteractWithBlockTrigger.TriggerInstance(CriteriaTriggers.ALLAY_DROP_ITEM_ON_BLOCK.id, EntityPredicate.Composite.ANY, pLocation.build(), pItem.build());
      }

      public boolean matches(BlockState pState, ServerLevel pLevel, BlockPos pPos, ItemStack pStack) {
         return !this.location.matches(pLevel, (double)pPos.getX() + 0.5D, (double)pPos.getY() + 0.5D, (double)pPos.getZ() + 0.5D) ? false : this.item.matches(pStack);
      }

      public JsonObject serializeToJson(SerializationContext pConditions) {
         JsonObject jsonobject = super.serializeToJson(pConditions);
         jsonobject.add("location", this.location.serializeToJson());
         jsonobject.add("item", this.item.serializeToJson());
         return jsonobject;
      }
   }
}