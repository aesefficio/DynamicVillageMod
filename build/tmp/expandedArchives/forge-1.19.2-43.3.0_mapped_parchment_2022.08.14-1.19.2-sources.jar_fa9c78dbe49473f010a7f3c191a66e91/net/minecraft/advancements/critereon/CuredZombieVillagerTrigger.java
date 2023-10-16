package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.storage.loot.LootContext;

public class CuredZombieVillagerTrigger extends SimpleCriterionTrigger<CuredZombieVillagerTrigger.TriggerInstance> {
   static final ResourceLocation ID = new ResourceLocation("cured_zombie_villager");

   public ResourceLocation getId() {
      return ID;
   }

   public CuredZombieVillagerTrigger.TriggerInstance createInstance(JsonObject pJson, EntityPredicate.Composite pEntityPredicate, DeserializationContext pConditionsParser) {
      EntityPredicate.Composite entitypredicate$composite = EntityPredicate.Composite.fromJson(pJson, "zombie", pConditionsParser);
      EntityPredicate.Composite entitypredicate$composite1 = EntityPredicate.Composite.fromJson(pJson, "villager", pConditionsParser);
      return new CuredZombieVillagerTrigger.TriggerInstance(pEntityPredicate, entitypredicate$composite, entitypredicate$composite1);
   }

   public void trigger(ServerPlayer pPlayer, Zombie pZombie, Villager pVillager) {
      LootContext lootcontext = EntityPredicate.createContext(pPlayer, pZombie);
      LootContext lootcontext1 = EntityPredicate.createContext(pPlayer, pVillager);
      this.trigger(pPlayer, (p_24285_) -> {
         return p_24285_.matches(lootcontext, lootcontext1);
      });
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final EntityPredicate.Composite zombie;
      private final EntityPredicate.Composite villager;

      public TriggerInstance(EntityPredicate.Composite pPlayer, EntityPredicate.Composite pZombie, EntityPredicate.Composite pVillager) {
         super(CuredZombieVillagerTrigger.ID, pPlayer);
         this.zombie = pZombie;
         this.villager = pVillager;
      }

      public static CuredZombieVillagerTrigger.TriggerInstance curedZombieVillager() {
         return new CuredZombieVillagerTrigger.TriggerInstance(EntityPredicate.Composite.ANY, EntityPredicate.Composite.ANY, EntityPredicate.Composite.ANY);
      }

      public boolean matches(LootContext pZombie, LootContext pVillager) {
         if (!this.zombie.matches(pZombie)) {
            return false;
         } else {
            return this.villager.matches(pVillager);
         }
      }

      public JsonObject serializeToJson(SerializationContext pConditions) {
         JsonObject jsonobject = super.serializeToJson(pConditions);
         jsonobject.add("zombie", this.zombie.toJson(pConditions));
         jsonobject.add("villager", this.villager.toJson(pConditions));
         return jsonobject;
      }
   }
}