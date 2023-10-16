package net.minecraft.world.item.crafting;

import com.google.gson.JsonObject;
import java.util.function.Function;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public class SimpleRecipeSerializer<T extends Recipe<?>> implements RecipeSerializer<T> {
   private final Function<ResourceLocation, T> constructor;

   public SimpleRecipeSerializer(Function<ResourceLocation, T> pConstructor) {
      this.constructor = pConstructor;
   }

   public T fromJson(ResourceLocation pRecipeId, JsonObject pJson) {
      return this.constructor.apply(pRecipeId);
   }

   public T fromNetwork(ResourceLocation pRecipeId, FriendlyByteBuf pBuffer) {
      return this.constructor.apply(pRecipeId);
   }

   public void toNetwork(FriendlyByteBuf pBuffer, T pRecipe) {
   }
}