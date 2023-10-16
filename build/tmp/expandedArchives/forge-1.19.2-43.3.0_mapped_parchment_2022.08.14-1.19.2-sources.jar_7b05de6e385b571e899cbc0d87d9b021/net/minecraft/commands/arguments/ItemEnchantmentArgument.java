package net.minecraft.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;

public class ItemEnchantmentArgument implements ArgumentType<Enchantment> {
   private static final Collection<String> EXAMPLES = Arrays.asList("unbreaking", "silk_touch");
   public static final DynamicCommandExceptionType ERROR_UNKNOWN_ENCHANTMENT = new DynamicCommandExceptionType((p_95267_) -> {
      return Component.translatable("enchantment.unknown", p_95267_);
   });

   public static ItemEnchantmentArgument enchantment() {
      return new ItemEnchantmentArgument();
   }

   public static Enchantment getEnchantment(CommandContext<CommandSourceStack> pContext, String pName) {
      return pContext.getArgument(pName, Enchantment.class);
   }

   public Enchantment parse(StringReader pReader) throws CommandSyntaxException {
      ResourceLocation resourcelocation = ResourceLocation.read(pReader);
      return Registry.ENCHANTMENT.getOptional(resourcelocation).orElseThrow(() -> {
         return ERROR_UNKNOWN_ENCHANTMENT.create(resourcelocation);
      });
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> pContext, SuggestionsBuilder pBuilder) {
      return SharedSuggestionProvider.suggestResource(Registry.ENCHANTMENT.keySet(), pBuilder);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }
}