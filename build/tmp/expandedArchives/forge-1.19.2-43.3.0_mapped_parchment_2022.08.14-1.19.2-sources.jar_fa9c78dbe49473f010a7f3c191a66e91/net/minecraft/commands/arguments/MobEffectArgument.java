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
import net.minecraft.world.effect.MobEffect;

public class MobEffectArgument implements ArgumentType<MobEffect> {
   private static final Collection<String> EXAMPLES = Arrays.asList("spooky", "effect");
   public static final DynamicCommandExceptionType ERROR_UNKNOWN_EFFECT = new DynamicCommandExceptionType((p_98433_) -> {
      return Component.translatable("effect.effectNotFound", p_98433_);
   });

   public static MobEffectArgument effect() {
      return new MobEffectArgument();
   }

   public static MobEffect getEffect(CommandContext<CommandSourceStack> pContext, String pName) {
      return pContext.getArgument(pName, MobEffect.class);
   }

   public MobEffect parse(StringReader pReader) throws CommandSyntaxException {
      ResourceLocation resourcelocation = ResourceLocation.read(pReader);
      return Registry.MOB_EFFECT.getOptional(resourcelocation).orElseThrow(() -> {
         return ERROR_UNKNOWN_EFFECT.create(resourcelocation);
      });
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> pContext, SuggestionsBuilder pBuilder) {
      return SharedSuggestionProvider.suggestResource(Registry.MOB_EFFECT.keySet(), pBuilder);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }
}