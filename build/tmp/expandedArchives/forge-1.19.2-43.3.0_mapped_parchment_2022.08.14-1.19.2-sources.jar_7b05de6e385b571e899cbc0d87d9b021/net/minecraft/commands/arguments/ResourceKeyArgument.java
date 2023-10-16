package net.minecraft.commands.arguments;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

public class ResourceKeyArgument<T> implements ArgumentType<ResourceKey<T>> {
   private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "012");
   private static final DynamicCommandExceptionType ERROR_UNKNOWN_ATTRIBUTE = new DynamicCommandExceptionType((p_212392_) -> {
      return Component.translatable("attribute.unknown", p_212392_);
   });
   private static final DynamicCommandExceptionType ERROR_INVALID_FEATURE = new DynamicCommandExceptionType((p_212385_) -> {
      return Component.translatable("commands.place.feature.invalid", p_212385_);
   });
   private static final DynamicCommandExceptionType ERROR_INVALID_STRUCTURE = new DynamicCommandExceptionType((p_233264_) -> {
      return Component.translatable("commands.place.structure.invalid", p_233264_);
   });
   private static final DynamicCommandExceptionType ERROR_INVALID_TEMPLATE_POOL = new DynamicCommandExceptionType((p_233252_) -> {
      return Component.translatable("commands.place.jigsaw.invalid", p_233252_);
   });
   final ResourceKey<? extends Registry<T>> registryKey;

   public ResourceKeyArgument(ResourceKey<? extends Registry<T>> pRegistryKey) {
      this.registryKey = pRegistryKey;
   }

   public static <T> ResourceKeyArgument<T> key(ResourceKey<? extends Registry<T>> pRegistryKey) {
      return new ResourceKeyArgument<>(pRegistryKey);
   }

   private static <T> ResourceKey<T> getRegistryType(CommandContext<CommandSourceStack> pContext, String pName, ResourceKey<Registry<T>> pRegistryKey, DynamicCommandExceptionType p_212377_) throws CommandSyntaxException {
      ResourceKey<?> resourcekey = pContext.getArgument(pName, ResourceKey.class);
      Optional<ResourceKey<T>> optional = resourcekey.cast(pRegistryKey);
      return optional.orElseThrow(() -> {
         return p_212377_.create(resourcekey);
      });
   }

   private static <T> Registry<T> getRegistry(CommandContext<CommandSourceStack> pContext, ResourceKey<? extends Registry<T>> pRegistryKey) {
      return pContext.getSource().getServer().registryAccess().registryOrThrow(pRegistryKey);
   }

   private static <T> Holder<T> getRegistryKeyType(CommandContext<CommandSourceStack> p_233256_, String p_233257_, ResourceKey<Registry<T>> p_233258_, DynamicCommandExceptionType p_233259_) throws CommandSyntaxException {
      ResourceKey<T> resourcekey = getRegistryType(p_233256_, p_233257_, p_233258_, p_233259_);
      return getRegistry(p_233256_, p_233258_).getHolder(resourcekey).orElseThrow(() -> {
         return p_233259_.create(resourcekey.location());
      });
   }

   public static Attribute getAttribute(CommandContext<CommandSourceStack> pContext, String pName) throws CommandSyntaxException {
      ResourceKey<Attribute> resourcekey = getRegistryType(pContext, pName, Registry.ATTRIBUTE_REGISTRY, ERROR_UNKNOWN_ATTRIBUTE);
      return getRegistry(pContext, Registry.ATTRIBUTE_REGISTRY).getOptional(resourcekey).orElseThrow(() -> {
         return ERROR_UNKNOWN_ATTRIBUTE.create(resourcekey.location());
      });
   }

   public static Holder<ConfiguredFeature<?, ?>> getConfiguredFeature(CommandContext<CommandSourceStack> pContext, String pName) throws CommandSyntaxException {
      return getRegistryKeyType(pContext, pName, Registry.CONFIGURED_FEATURE_REGISTRY, ERROR_INVALID_FEATURE);
   }

   public static Holder<Structure> getStructure(CommandContext<CommandSourceStack> p_233266_, String p_233267_) throws CommandSyntaxException {
      return getRegistryKeyType(p_233266_, p_233267_, Registry.STRUCTURE_REGISTRY, ERROR_INVALID_STRUCTURE);
   }

   public static Holder<StructureTemplatePool> getStructureTemplatePool(CommandContext<CommandSourceStack> p_233269_, String p_233270_) throws CommandSyntaxException {
      return getRegistryKeyType(p_233269_, p_233270_, Registry.TEMPLATE_POOL_REGISTRY, ERROR_INVALID_TEMPLATE_POOL);
   }

   public ResourceKey<T> parse(StringReader p_212369_) throws CommandSyntaxException {
      ResourceLocation resourcelocation = ResourceLocation.read(p_212369_);
      return ResourceKey.create(this.registryKey, resourcelocation);
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> pContext, SuggestionsBuilder p_212400_) {
      Object object = pContext.getSource();
      if (object instanceof SharedSuggestionProvider sharedsuggestionprovider) {
         return sharedsuggestionprovider.suggestRegistryElements(this.registryKey, SharedSuggestionProvider.ElementSuggestionType.ELEMENTS, p_212400_, pContext);
      } else {
         return p_212400_.buildFuture();
      }
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   public static class Info<T> implements ArgumentTypeInfo<ResourceKeyArgument<T>, ResourceKeyArgument.Info<T>.Template> {
      public void serializeToNetwork(ResourceKeyArgument.Info.Template p_233278_, FriendlyByteBuf p_233279_) {
         p_233279_.writeResourceLocation(p_233278_.registryKey.location());
      }

      public ResourceKeyArgument.Info<T>.Template deserializeFromNetwork(FriendlyByteBuf p_233289_) {
         ResourceLocation resourcelocation = p_233289_.readResourceLocation();
         return new ResourceKeyArgument.Info.Template(ResourceKey.createRegistryKey(resourcelocation));
      }

      public void serializeToJson(ResourceKeyArgument.Info.Template p_233275_, JsonObject p_233276_) {
         p_233276_.addProperty("registry", p_233275_.registryKey.location().toString());
      }

      public ResourceKeyArgument.Info<T>.Template unpack(ResourceKeyArgument<T> p_233281_) {
         return new ResourceKeyArgument.Info.Template(p_233281_.registryKey);
      }

      public final class Template implements ArgumentTypeInfo.Template<ResourceKeyArgument<T>> {
         final ResourceKey<? extends Registry<T>> registryKey;

         Template(ResourceKey<? extends Registry<T>> p_233296_) {
            this.registryKey = p_233296_;
         }

         public ResourceKeyArgument<T> instantiate(CommandBuildContext p_233299_) {
            return new ResourceKeyArgument<>(this.registryKey);
         }

         public ArgumentTypeInfo<ResourceKeyArgument<T>, ?> type() {
            return Info.this;
         }
      }
   }
}