package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;

public class PlayerInteractTrigger extends SimpleCriterionTrigger<PlayerInteractTrigger.TriggerInstance> {
   static final ResourceLocation ID = new ResourceLocation("player_interacted_with_entity");

   public ResourceLocation getId() {
      return ID;
   }

   protected PlayerInteractTrigger.TriggerInstance createInstance(JsonObject pJson, EntityPredicate.Composite pEntityPredicate, DeserializationContext pConditionsParser) {
      ItemPredicate itempredicate = ItemPredicate.fromJson(pJson.get("item"));
      EntityPredicate.Composite entitypredicate$composite = EntityPredicate.Composite.fromJson(pJson, "entity", pConditionsParser);
      return new PlayerInteractTrigger.TriggerInstance(pEntityPredicate, itempredicate, entitypredicate$composite);
   }

   public void trigger(ServerPlayer pPlayer, ItemStack pItem, Entity pEntity) {
      LootContext lootcontext = EntityPredicate.createContext(pPlayer, pEntity);
      this.trigger(pPlayer, (p_61501_) -> {
         return p_61501_.matches(pItem, lootcontext);
      });
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final ItemPredicate item;
      private final EntityPredicate.Composite entity;

      public TriggerInstance(EntityPredicate.Composite pPlayer, ItemPredicate pItem, EntityPredicate.Composite pEntity) {
         super(PlayerInteractTrigger.ID, pPlayer);
         this.item = pItem;
         this.entity = pEntity;
      }

      public static PlayerInteractTrigger.TriggerInstance itemUsedOnEntity(EntityPredicate.Composite pPlayer, ItemPredicate.Builder pItem, EntityPredicate.Composite pEntity) {
         return new PlayerInteractTrigger.TriggerInstance(pPlayer, pItem.build(), pEntity);
      }

      public static PlayerInteractTrigger.TriggerInstance itemUsedOnEntity(ItemPredicate.Builder p_222016_, EntityPredicate.Composite p_222017_) {
         return itemUsedOnEntity(EntityPredicate.Composite.ANY, p_222016_, p_222017_);
      }

      public boolean matches(ItemStack pItem, LootContext pLootContext) {
         return !this.item.matches(pItem) ? false : this.entity.matches(pLootContext);
      }

      public JsonObject serializeToJson(SerializationContext pConditions) {
         JsonObject jsonobject = super.serializeToJson(pConditions);
         jsonobject.add("item", this.item.serializeToJson());
         jsonobject.add("entity", this.entity.toJson(pConditions));
         return jsonobject;
      }
   }
}