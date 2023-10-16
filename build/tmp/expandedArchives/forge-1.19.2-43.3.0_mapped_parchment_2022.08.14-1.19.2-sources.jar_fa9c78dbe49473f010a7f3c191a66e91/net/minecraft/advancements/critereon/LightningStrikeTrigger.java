package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.storage.loot.LootContext;

public class LightningStrikeTrigger extends SimpleCriterionTrigger<LightningStrikeTrigger.TriggerInstance> {
   static final ResourceLocation ID = new ResourceLocation("lightning_strike");

   public ResourceLocation getId() {
      return ID;
   }

   public LightningStrikeTrigger.TriggerInstance createInstance(JsonObject pJson, EntityPredicate.Composite pPlayer, DeserializationContext pContext) {
      EntityPredicate.Composite entitypredicate$composite = EntityPredicate.Composite.fromJson(pJson, "lightning", pContext);
      EntityPredicate.Composite entitypredicate$composite1 = EntityPredicate.Composite.fromJson(pJson, "bystander", pContext);
      return new LightningStrikeTrigger.TriggerInstance(pPlayer, entitypredicate$composite, entitypredicate$composite1);
   }

   public void trigger(ServerPlayer pPlayer, LightningBolt pLightning, List<Entity> pNearbyEntities) {
      List<LootContext> list = pNearbyEntities.stream().map((p_153390_) -> {
         return EntityPredicate.createContext(pPlayer, p_153390_);
      }).collect(Collectors.toList());
      LootContext lootcontext = EntityPredicate.createContext(pPlayer, pLightning);
      this.trigger(pPlayer, (p_153402_) -> {
         return p_153402_.matches(lootcontext, list);
      });
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final EntityPredicate.Composite lightning;
      private final EntityPredicate.Composite bystander;

      public TriggerInstance(EntityPredicate.Composite pPlayer, EntityPredicate.Composite pLightning, EntityPredicate.Composite pBystander) {
         super(LightningStrikeTrigger.ID, pPlayer);
         this.lightning = pLightning;
         this.bystander = pBystander;
      }

      public static LightningStrikeTrigger.TriggerInstance lighthingStrike(EntityPredicate pLightning, EntityPredicate pBystander) {
         return new LightningStrikeTrigger.TriggerInstance(EntityPredicate.Composite.ANY, EntityPredicate.Composite.wrap(pLightning), EntityPredicate.Composite.wrap(pBystander));
      }

      public boolean matches(LootContext pPlayerContext, List<LootContext> pEntityContexts) {
         if (!this.lightning.matches(pPlayerContext)) {
            return false;
         } else {
            return this.bystander == EntityPredicate.Composite.ANY || !pEntityContexts.stream().noneMatch(this.bystander::matches);
         }
      }

      public JsonObject serializeToJson(SerializationContext pConditions) {
         JsonObject jsonobject = super.serializeToJson(pConditions);
         jsonobject.add("lightning", this.lightning.toJson(pConditions));
         jsonobject.add("bystander", this.bystander.toJson(pConditions));
         return jsonobject;
      }
   }
}