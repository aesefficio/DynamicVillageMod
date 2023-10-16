package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.resources.ResourceLocation;

public abstract class AbstractCriterionTriggerInstance implements CriterionTriggerInstance {
   private final ResourceLocation criterion;
   private final EntityPredicate.Composite player;

   public AbstractCriterionTriggerInstance(ResourceLocation pCriterion, EntityPredicate.Composite pPlayer) {
      this.criterion = pCriterion;
      this.player = pPlayer;
   }

   public ResourceLocation getCriterion() {
      return this.criterion;
   }

   protected EntityPredicate.Composite getPlayerPredicate() {
      return this.player;
   }

   public JsonObject serializeToJson(SerializationContext pConditions) {
      JsonObject jsonobject = new JsonObject();
      jsonobject.add("player", this.player.toJson(pConditions));
      return jsonobject;
   }

   public String toString() {
      return "AbstractCriterionInstance{criterion=" + this.criterion + "}";
   }
}