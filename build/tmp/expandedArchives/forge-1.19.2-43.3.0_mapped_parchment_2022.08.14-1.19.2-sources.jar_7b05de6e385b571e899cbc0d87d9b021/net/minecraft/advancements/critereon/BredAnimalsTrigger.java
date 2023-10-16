package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.storage.loot.LootContext;

public class BredAnimalsTrigger extends SimpleCriterionTrigger<BredAnimalsTrigger.TriggerInstance> {
   static final ResourceLocation ID = new ResourceLocation("bred_animals");

   public ResourceLocation getId() {
      return ID;
   }

   public BredAnimalsTrigger.TriggerInstance createInstance(JsonObject pJson, EntityPredicate.Composite pEntityPredicate, DeserializationContext pConditionsParser) {
      EntityPredicate.Composite entitypredicate$composite = EntityPredicate.Composite.fromJson(pJson, "parent", pConditionsParser);
      EntityPredicate.Composite entitypredicate$composite1 = EntityPredicate.Composite.fromJson(pJson, "partner", pConditionsParser);
      EntityPredicate.Composite entitypredicate$composite2 = EntityPredicate.Composite.fromJson(pJson, "child", pConditionsParser);
      return new BredAnimalsTrigger.TriggerInstance(pEntityPredicate, entitypredicate$composite, entitypredicate$composite1, entitypredicate$composite2);
   }

   public void trigger(ServerPlayer pPlayer, Animal pParent, Animal pPartner, @Nullable AgeableMob pChild) {
      LootContext lootcontext = EntityPredicate.createContext(pPlayer, pParent);
      LootContext lootcontext1 = EntityPredicate.createContext(pPlayer, pPartner);
      LootContext lootcontext2 = pChild != null ? EntityPredicate.createContext(pPlayer, pChild) : null;
      this.trigger(pPlayer, (p_18653_) -> {
         return p_18653_.matches(lootcontext, lootcontext1, lootcontext2);
      });
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final EntityPredicate.Composite parent;
      private final EntityPredicate.Composite partner;
      private final EntityPredicate.Composite child;

      public TriggerInstance(EntityPredicate.Composite pPlayer, EntityPredicate.Composite pParent, EntityPredicate.Composite pPartner, EntityPredicate.Composite pChild) {
         super(BredAnimalsTrigger.ID, pPlayer);
         this.parent = pParent;
         this.partner = pPartner;
         this.child = pChild;
      }

      public static BredAnimalsTrigger.TriggerInstance bredAnimals() {
         return new BredAnimalsTrigger.TriggerInstance(EntityPredicate.Composite.ANY, EntityPredicate.Composite.ANY, EntityPredicate.Composite.ANY, EntityPredicate.Composite.ANY);
      }

      public static BredAnimalsTrigger.TriggerInstance bredAnimals(EntityPredicate.Builder pChildBuilder) {
         return new BredAnimalsTrigger.TriggerInstance(EntityPredicate.Composite.ANY, EntityPredicate.Composite.ANY, EntityPredicate.Composite.ANY, EntityPredicate.Composite.wrap(pChildBuilder.build()));
      }

      public static BredAnimalsTrigger.TriggerInstance bredAnimals(EntityPredicate pParent, EntityPredicate pPartner, EntityPredicate pChild) {
         return new BredAnimalsTrigger.TriggerInstance(EntityPredicate.Composite.ANY, EntityPredicate.Composite.wrap(pParent), EntityPredicate.Composite.wrap(pPartner), EntityPredicate.Composite.wrap(pChild));
      }

      public boolean matches(LootContext pParentContext, LootContext pPartnerContext, @Nullable LootContext pChildContext) {
         if (this.child == EntityPredicate.Composite.ANY || pChildContext != null && this.child.matches(pChildContext)) {
            return this.parent.matches(pParentContext) && this.partner.matches(pPartnerContext) || this.parent.matches(pPartnerContext) && this.partner.matches(pParentContext);
         } else {
            return false;
         }
      }

      public JsonObject serializeToJson(SerializationContext pConditions) {
         JsonObject jsonobject = super.serializeToJson(pConditions);
         jsonobject.add("parent", this.parent.toJson(pConditions));
         jsonobject.add("partner", this.partner.toJson(pConditions));
         jsonobject.add("child", this.child.toJson(pConditions));
         return jsonobject;
      }
   }
}