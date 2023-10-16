package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;

public class TradeTrigger extends SimpleCriterionTrigger<TradeTrigger.TriggerInstance> {
   static final ResourceLocation ID = new ResourceLocation("villager_trade");

   public ResourceLocation getId() {
      return ID;
   }

   public TradeTrigger.TriggerInstance createInstance(JsonObject pJson, EntityPredicate.Composite pEntityPredicate, DeserializationContext pConditionsParser) {
      EntityPredicate.Composite entitypredicate$composite = EntityPredicate.Composite.fromJson(pJson, "villager", pConditionsParser);
      ItemPredicate itempredicate = ItemPredicate.fromJson(pJson.get("item"));
      return new TradeTrigger.TriggerInstance(pEntityPredicate, entitypredicate$composite, itempredicate);
   }

   public void trigger(ServerPlayer pPlayer, AbstractVillager pVillager, ItemStack pStack) {
      LootContext lootcontext = EntityPredicate.createContext(pPlayer, pVillager);
      this.trigger(pPlayer, (p_70970_) -> {
         return p_70970_.matches(lootcontext, pStack);
      });
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final EntityPredicate.Composite villager;
      private final ItemPredicate item;

      public TriggerInstance(EntityPredicate.Composite pPlayer, EntityPredicate.Composite pVillager, ItemPredicate pItem) {
         super(TradeTrigger.ID, pPlayer);
         this.villager = pVillager;
         this.item = pItem;
      }

      public static TradeTrigger.TriggerInstance tradedWithVillager() {
         return new TradeTrigger.TriggerInstance(EntityPredicate.Composite.ANY, EntityPredicate.Composite.ANY, ItemPredicate.ANY);
      }

      public static TradeTrigger.TriggerInstance tradedWithVillager(EntityPredicate.Builder pVillager) {
         return new TradeTrigger.TriggerInstance(EntityPredicate.Composite.wrap(pVillager.build()), EntityPredicate.Composite.ANY, ItemPredicate.ANY);
      }

      public boolean matches(LootContext pContext, ItemStack pStack) {
         if (!this.villager.matches(pContext)) {
            return false;
         } else {
            return this.item.matches(pStack);
         }
      }

      public JsonObject serializeToJson(SerializationContext pConditions) {
         JsonObject jsonobject = super.serializeToJson(pConditions);
         jsonobject.add("item", this.item.serializeToJson());
         jsonobject.add("villager", this.villager.toJson(pConditions));
         return jsonobject;
      }
   }
}