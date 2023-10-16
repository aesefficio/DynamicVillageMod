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
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class ParticleArgument implements ArgumentType<ParticleOptions> {
   private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "particle with options");
   public static final DynamicCommandExceptionType ERROR_UNKNOWN_PARTICLE = new DynamicCommandExceptionType((p_103941_) -> {
      return Component.translatable("particle.notFound", p_103941_);
   });

   public static ParticleArgument particle() {
      return new ParticleArgument();
   }

   public static ParticleOptions getParticle(CommandContext<CommandSourceStack> pContext, String pName) {
      return pContext.getArgument(pName, ParticleOptions.class);
   }

   public ParticleOptions parse(StringReader pReader) throws CommandSyntaxException {
      return readParticle(pReader);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   /**
    * Parses a particle, including its type.
    */
   public static ParticleOptions readParticle(StringReader pReader) throws CommandSyntaxException {
      ResourceLocation resourcelocation = ResourceLocation.read(pReader);
      ParticleType<?> particletype = Registry.PARTICLE_TYPE.getOptional(resourcelocation).orElseThrow(() -> {
         return ERROR_UNKNOWN_PARTICLE.create(resourcelocation);
      });
      return readParticle(pReader, particletype);
   }

   /**
    * Deserializes a particle once its type is known.
    */
   private static <T extends ParticleOptions> T readParticle(StringReader pReader, ParticleType<T> pType) throws CommandSyntaxException {
      return pType.getDeserializer().fromCommand(pType, pReader);
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> pContext, SuggestionsBuilder pBuilder) {
      return SharedSuggestionProvider.suggestResource(Registry.PARTICLE_TYPE.keySet(), pBuilder);
   }
}