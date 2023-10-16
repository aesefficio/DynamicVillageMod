package net.minecraft.core;

import com.mojang.datafixers.util.Either;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

public interface Holder<T> extends java.util.function.Supplier<T>, net.minecraftforge.registries.tags.IReverseTag<T> {
   @Override
   default boolean containsTag(TagKey<T> key) {
      return this.is(key);
   }

   @Override
   default Stream<TagKey<T>> getTagKeys() {
      return this.tags();
   }

   @Override
   default T get() {
      return this.value();
   }

   T value();

   boolean isBound();

   boolean is(ResourceLocation pLocation);

   boolean is(ResourceKey<T> pResourceKey);

   boolean is(Predicate<ResourceKey<T>> pPredicate);

   boolean is(TagKey<T> pTagKey);

   Stream<TagKey<T>> tags();

   Either<ResourceKey<T>, T> unwrap();

   Optional<ResourceKey<T>> unwrapKey();

   Holder.Kind kind();

   boolean isValidInRegistry(Registry<T> pRegistry);

   static <T> Holder<T> direct(T pValue) {
      return new Holder.Direct<>(pValue);
   }

   static <T> Holder<T> hackyErase(Holder<? extends T> p_205707_) {
      return (Holder<T>)p_205707_;
   }

   public static record Direct<T>(T value) implements Holder<T> {
      public boolean isBound() {
         return true;
      }

      public boolean is(ResourceLocation p_205727_) {
         return false;
      }

      public boolean is(ResourceKey<T> p_205725_) {
         return false;
      }

      public boolean is(TagKey<T> p_205719_) {
         return false;
      }

      public boolean is(Predicate<ResourceKey<T>> p_205723_) {
         return false;
      }

      public Either<ResourceKey<T>, T> unwrap() {
         return Either.right(this.value);
      }

      public Optional<ResourceKey<T>> unwrapKey() {
         return Optional.empty();
      }

      public Holder.Kind kind() {
         return Holder.Kind.DIRECT;
      }

      public String toString() {
         return "Direct{" + this.value + "}";
      }

      public boolean isValidInRegistry(Registry<T> p_205721_) {
         return true;
      }

      public Stream<TagKey<T>> tags() {
         return Stream.of();
      }

      public T value() {
         return this.value;
      }
   }

   public static enum Kind {
      REFERENCE,
      DIRECT;
   }

   public static class Reference<T> implements Holder<T> {
      private final Registry<T> registry;
      private Set<TagKey<T>> tags = Set.of();
      private final Holder.Reference.Type type;
      @Nullable
      private ResourceKey<T> key;
      @Nullable
      private T value;

      private Reference(Holder.Reference.Type pType, Registry<T> pRegistry, @Nullable ResourceKey<T> pKey, @Nullable T pValue) {
         this.registry = pRegistry;
         this.type = pType;
         this.key = pKey;
         this.value = pValue;
      }

      public static <T> Holder.Reference<T> createStandAlone(Registry<T> pRegistry, ResourceKey<T> pKey) {
         return new Holder.Reference<>(Holder.Reference.Type.STAND_ALONE, pRegistry, pKey, (T)null);
      }

      /** @deprecated */
      @Deprecated
      public static <T> Holder.Reference<T> createIntrusive(Registry<T> pRegistry, @Nullable T pValue) {
         return new Holder.Reference<>(Holder.Reference.Type.INTRUSIVE, pRegistry, (ResourceKey<T>)null, pValue);
      }

      public ResourceKey<T> key() {
         if (this.key == null) {
            throw new IllegalStateException("Trying to access unbound value '" + this.value + "' from registry " + this.registry);
         } else {
            return this.key;
         }
      }

      public T value() {
         if (this.value == null) {
            throw new IllegalStateException("Trying to access unbound value '" + this.key + "' from registry " + this.registry);
         } else {
            return this.value;
         }
      }

      public boolean is(ResourceLocation pLocation) {
         return this.key().location().equals(pLocation);
      }

      public boolean is(ResourceKey<T> pResourceKey) {
         return this.key() == pResourceKey;
      }

      public boolean is(TagKey<T> pTagKey) {
         return this.tags.contains(pTagKey);
      }

      public boolean is(Predicate<ResourceKey<T>> pPredicate) {
         return pPredicate.test(this.key());
      }

      public boolean isValidInRegistry(Registry<T> pRegistry) {
         return this.registry == pRegistry;
      }

      public Either<ResourceKey<T>, T> unwrap() {
         return Either.left(this.key());
      }

      public Optional<ResourceKey<T>> unwrapKey() {
         return Optional.of(this.key());
      }

      public Holder.Kind kind() {
         return Holder.Kind.REFERENCE;
      }

      public boolean isBound() {
         return this.key != null && this.value != null;
      }

      public void bind(ResourceKey<T> pKey, T pValue) {
         if (this.key != null && pKey != this.key) {
            throw new IllegalStateException("Can't change holder key: existing=" + this.key + ", new=" + pKey);
         } else if (this.type == Holder.Reference.Type.INTRUSIVE && this.value != pValue) {
            throw new IllegalStateException("Can't change holder " + pKey + " value: existing=" + this.value + ", new=" + pValue);
         } else {
            this.key = pKey;
            this.value = pValue;
         }
      }

      public void bindTags(Collection<TagKey<T>> pTags) {
         this.tags = Set.copyOf(pTags);
      }

      public Stream<TagKey<T>> tags() {
         return this.tags.stream();
      }

      public Type getType() {
         return this.type;
      }

      public String toString() {
         return "Reference{" + this.key + "=" + this.value + "}";
      }

      public static enum Type {
         STAND_ALONE,
         INTRUSIVE;
      }
   }
}
