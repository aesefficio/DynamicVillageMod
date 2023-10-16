package net.minecraft.core;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

public interface HolderLookup<T> {
   Optional<Holder<T>> get(ResourceKey<T> p_235699_);

   Stream<ResourceKey<T>> listElements();

   Optional<? extends HolderSet<T>> get(TagKey<T> p_235700_);

   Stream<TagKey<T>> listTags();

   static <T> HolderLookup<T> forRegistry(Registry<T> p_235702_) {
      return new HolderLookup.RegistryLookup<>(p_235702_);
   }

   public static class RegistryLookup<T> implements HolderLookup<T> {
      protected final Registry<T> registry;

      public RegistryLookup(Registry<T> p_235705_) {
         this.registry = p_235705_;
      }

      public Optional<Holder<T>> get(ResourceKey<T> p_235708_) {
         return this.registry.getHolder(p_235708_);
      }

      public Stream<ResourceKey<T>> listElements() {
         return this.registry.entrySet().stream().map(Map.Entry::getKey);
      }

      public Optional<? extends HolderSet<T>> get(TagKey<T> p_235710_) {
         return this.registry.getTag(p_235710_);
      }

      public Stream<TagKey<T>> listTags() {
         return this.registry.getTagNames();
      }
   }
}