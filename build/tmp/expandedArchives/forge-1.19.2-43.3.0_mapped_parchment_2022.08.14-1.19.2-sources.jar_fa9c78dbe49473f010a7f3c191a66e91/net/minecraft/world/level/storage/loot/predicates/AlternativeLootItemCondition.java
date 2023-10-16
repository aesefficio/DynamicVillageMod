package net.minecraft.world.level.storage.loot.predicates;

import com.google.common.collect.Lists;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;

/**
 * A LootItemCondition that combines a list of other conditions using "or".
 * 
 * @see LootItemConditions#orConditions
 */
public class AlternativeLootItemCondition implements LootItemCondition {
   final LootItemCondition[] terms;
   private final Predicate<LootContext> composedPredicate;

   AlternativeLootItemCondition(LootItemCondition[] pTerms) {
      this.terms = pTerms;
      this.composedPredicate = LootItemConditions.orConditions(pTerms);
   }

   public LootItemConditionType getType() {
      return LootItemConditions.ALTERNATIVE;
   }

   public final boolean test(LootContext p_81476_) {
      return this.composedPredicate.test(p_81476_);
   }

   /**
    * Validate that this object is used correctly according to the given ValidationContext.
    */
   public void validate(ValidationContext pContext) {
      LootItemCondition.super.validate(pContext);

      for(int i = 0; i < this.terms.length; ++i) {
         this.terms[i].validate(pContext.forChild(".term[" + i + "]"));
      }

   }

   public static AlternativeLootItemCondition.Builder alternative(LootItemCondition.Builder... pBuilders) {
      return new AlternativeLootItemCondition.Builder(pBuilders);
   }

   public static class Builder implements LootItemCondition.Builder {
      private final List<LootItemCondition> terms = Lists.newArrayList();

      public Builder(LootItemCondition.Builder... p_81488_) {
         for(LootItemCondition.Builder lootitemcondition$builder : p_81488_) {
            this.terms.add(lootitemcondition$builder.build());
         }

      }

      public AlternativeLootItemCondition.Builder or(LootItemCondition.Builder pBuilder) {
         this.terms.add(pBuilder.build());
         return this;
      }

      public LootItemCondition build() {
         return new AlternativeLootItemCondition(this.terms.toArray(new LootItemCondition[0]));
      }
   }

   public static class Serializer implements net.minecraft.world.level.storage.loot.Serializer<AlternativeLootItemCondition> {
      /**
       * Serialize the value by putting its data into the JsonObject.
       */
      public void serialize(JsonObject p_81497_, AlternativeLootItemCondition p_81498_, JsonSerializationContext p_81499_) {
         p_81497_.add("terms", p_81499_.serialize(p_81498_.terms));
      }

      /**
       * Deserialize a value by reading it from the JsonObject.
       */
      public AlternativeLootItemCondition deserialize(JsonObject p_81505_, JsonDeserializationContext p_81506_) {
         LootItemCondition[] alootitemcondition = GsonHelper.getAsObject(p_81505_, "terms", p_81506_, LootItemCondition[].class);
         return new AlternativeLootItemCondition(alootitemcondition);
      }
   }
}