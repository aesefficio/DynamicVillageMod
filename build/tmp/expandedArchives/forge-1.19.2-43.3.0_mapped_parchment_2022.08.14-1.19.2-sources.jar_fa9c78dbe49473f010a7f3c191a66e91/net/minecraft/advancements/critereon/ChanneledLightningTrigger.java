package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;

public class ChanneledLightningTrigger extends SimpleCriterionTrigger<ChanneledLightningTrigger.TriggerInstance> {
   static final ResourceLocation ID = new ResourceLocation("channeled_lightning");

   public ResourceLocation getId() {
      return ID;
   }

   public ChanneledLightningTrigger.TriggerInstance createInstance(JsonObject pJson, EntityPredicate.Composite pEntityPredicate, DeserializationContext pConditionsParser) {
      EntityPredicate.Composite[] aentitypredicate$composite = EntityPredicate.Composite.fromJsonArray(pJson, "victims", pConditionsParser);
      return new ChanneledLightningTrigger.TriggerInstance(pEntityPredicate, aentitypredicate$composite);
   }

   public void trigger(ServerPlayer pPlayer, Collection<? extends Entity> pEntityTriggered) {
      List<LootContext> list = pEntityTriggered.stream().map((p_21720_) -> {
         return EntityPredicate.createContext(pPlayer, p_21720_);
      }).collect(Collectors.toList());
      this.trigger(pPlayer, (p_21730_) -> {
         return p_21730_.matches(list);
      });
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final EntityPredicate.Composite[] victims;

      public TriggerInstance(EntityPredicate.Composite pPlayer, EntityPredicate.Composite[] pVictims) {
         super(ChanneledLightningTrigger.ID, pPlayer);
         this.victims = pVictims;
      }

      public static ChanneledLightningTrigger.TriggerInstance channeledLightning(EntityPredicate... pVictims) {
         return new ChanneledLightningTrigger.TriggerInstance(EntityPredicate.Composite.ANY, Stream.of(pVictims).map(EntityPredicate.Composite::wrap).toArray((p_21741_) -> {
            return new EntityPredicate.Composite[p_21741_];
         }));
      }

      public boolean matches(Collection<? extends LootContext> pVictims) {
         for(EntityPredicate.Composite entitypredicate$composite : this.victims) {
            boolean flag = false;

            for(LootContext lootcontext : pVictims) {
               if (entitypredicate$composite.matches(lootcontext)) {
                  flag = true;
                  break;
               }
            }

            if (!flag) {
               return false;
            }
         }

         return true;
      }

      public JsonObject serializeToJson(SerializationContext pConditions) {
         JsonObject jsonobject = super.serializeToJson(pConditions);
         jsonobject.add("victims", EntityPredicate.Composite.toJson(this.victims, pConditions));
         return jsonobject;
      }
   }
}