package net.minecraft.data.recipes;

import com.google.gson.JsonObject;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleRecipeSerializer;

public class SpecialRecipeBuilder {
   final SimpleRecipeSerializer<?> serializer;

   public SpecialRecipeBuilder(SimpleRecipeSerializer<?> pSerializer) {
      this.serializer = pSerializer;
   }

   public static SpecialRecipeBuilder special(SimpleRecipeSerializer<?> pSerializer) {
      return new SpecialRecipeBuilder(pSerializer);
   }

   /**
    * Builds this recipe into an {@link IFinishedRecipe}.
    */
   public void save(Consumer<FinishedRecipe> pFinishedRecipeConsumer, final String pId) {
      pFinishedRecipeConsumer.accept(new FinishedRecipe() {
         public void serializeRecipeData(JsonObject p_126370_) {
         }

         public RecipeSerializer<?> getType() {
            return SpecialRecipeBuilder.this.serializer;
         }

         /**
          * Gets the ID for the recipe.
          */
         public ResourceLocation getId() {
            return new ResourceLocation(pId);
         }

         /**
          * Gets the JSON for the advancement that unlocks this recipe. Null if there is no advancement.
          */
         @Nullable
         public JsonObject serializeAdvancement() {
            return null;
         }

         /**
          * Gets the ID for the advancement associated with this recipe. Should not be null if {@link
          * #getAdvancementJson} is non-null.
          */
         public ResourceLocation getAdvancementId() {
            return new ResourceLocation("");
         }
      });
   }
}