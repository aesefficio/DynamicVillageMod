package net.minecraft.data.recipes;

import com.google.gson.JsonObject;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.advancements.RequirementsStrategy;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class UpgradeRecipeBuilder {
   private final Ingredient base;
   private final Ingredient addition;
   private final Item result;
   private final Advancement.Builder advancement = Advancement.Builder.advancement();
   private final RecipeSerializer<?> type;

   public UpgradeRecipeBuilder(RecipeSerializer<?> pType, Ingredient pBase, Ingredient pAddition, Item pResult) {
      this.type = pType;
      this.base = pBase;
      this.addition = pAddition;
      this.result = pResult;
   }

   public static UpgradeRecipeBuilder smithing(Ingredient pBase, Ingredient pAddition, Item pResult) {
      return new UpgradeRecipeBuilder(RecipeSerializer.SMITHING, pBase, pAddition, pResult);
   }

   public UpgradeRecipeBuilder unlocks(String pName, CriterionTriggerInstance pCriterion) {
      this.advancement.addCriterion(pName, pCriterion);
      return this;
   }

   public void save(Consumer<FinishedRecipe> pFinishedRecipeConsumer, String pId) {
      this.save(pFinishedRecipeConsumer, new ResourceLocation(pId));
   }

   public void save(Consumer<FinishedRecipe> pFinishedRecipeConsumer, ResourceLocation pId) {
      this.ensureValid(pId);
      this.advancement.parent(RecipeBuilder.ROOT_RECIPE_ADVANCEMENT).addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(pId)).rewards(AdvancementRewards.Builder.recipe(pId)).requirements(RequirementsStrategy.OR);
      pFinishedRecipeConsumer.accept(new UpgradeRecipeBuilder.Result(pId, this.type, this.base, this.addition, this.result, this.advancement, new ResourceLocation(pId.getNamespace(), "recipes/" + this.result.getItemCategory().getRecipeFolderName() + "/" + pId.getPath())));
   }

   private void ensureValid(ResourceLocation pId) {
      if (this.advancement.getCriteria().isEmpty()) {
         throw new IllegalStateException("No way of obtaining recipe " + pId);
      }
   }

   public static class Result implements FinishedRecipe {
      private final ResourceLocation id;
      private final Ingredient base;
      private final Ingredient addition;
      private final Item result;
      private final Advancement.Builder advancement;
      private final ResourceLocation advancementId;
      private final RecipeSerializer<?> type;

      public Result(ResourceLocation pId, RecipeSerializer<?> pType, Ingredient pBase, Ingredient pAddition, Item pResult, Advancement.Builder pAdvancement, ResourceLocation pAdvancementId) {
         this.id = pId;
         this.type = pType;
         this.base = pBase;
         this.addition = pAddition;
         this.result = pResult;
         this.advancement = pAdvancement;
         this.advancementId = pAdvancementId;
      }

      public void serializeRecipeData(JsonObject pJson) {
         pJson.add("base", this.base.toJson());
         pJson.add("addition", this.addition.toJson());
         JsonObject jsonobject = new JsonObject();
         jsonobject.addProperty("item", Registry.ITEM.getKey(this.result).toString());
         pJson.add("result", jsonobject);
      }

      /**
       * Gets the ID for the recipe.
       */
      public ResourceLocation getId() {
         return this.id;
      }

      public RecipeSerializer<?> getType() {
         return this.type;
      }

      /**
       * Gets the JSON for the advancement that unlocks this recipe. Null if there is no advancement.
       */
      @Nullable
      public JsonObject serializeAdvancement() {
         return this.advancement.serializeToJson();
      }

      /**
       * Gets the ID for the advancement associated with this recipe. Should not be null if {@link #getAdvancementJson}
       * is non-null.
       */
      @Nullable
      public ResourceLocation getAdvancementId() {
         return this.advancementId;
      }
   }
}