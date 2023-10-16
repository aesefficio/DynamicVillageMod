package net.minecraft.core;

import com.mojang.serialization.Lifecycle;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

public class DefaultedRegistry<T> extends MappedRegistry<T> {
   /** The key of the default value. */
   private final ResourceLocation defaultKey;
   /** The default value for this registry, retrurned in the place of a null value. */
   private Holder<T> defaultValue;

   public DefaultedRegistry(String pDefaultName, ResourceKey<? extends Registry<T>> pKey, Lifecycle pElementsLifecycle, @Nullable Function<T, Holder.Reference<T>> pCustomHolderProvider) {
      super(pKey, pElementsLifecycle, pCustomHolderProvider);
      this.defaultKey = new ResourceLocation(pDefaultName);
   }

   public Holder<T> registerMapping(int pId, ResourceKey<T> pKey, T pValue, Lifecycle pLifecycle) {
      Holder<T> holder = super.registerMapping(pId, pKey, pValue, pLifecycle);
      if (this.defaultKey.equals(pKey.location())) {
         this.defaultValue = holder;
      }

      return holder;
   }

   /**
    * @return the integer ID used to identify the given object
    */
   public int getId(@Nullable T pValue) {
      int i = super.getId(pValue);
      return i == -1 ? super.getId(this.defaultValue.value()) : i;
   }

   /**
    * @return the name used to identify the given object within this registry or {@code null} if the object is not
    * within this registry
    */
   @Nonnull
   public ResourceLocation getKey(T pValue) {
      ResourceLocation resourcelocation = super.getKey(pValue);
      return resourcelocation == null ? this.defaultKey : resourcelocation;
   }

   @Nonnull
   public T get(@Nullable ResourceLocation pName) {
      T t = super.get(pName);
      return (T)(t == null ? this.defaultValue.value() : t);
   }

   public Optional<T> getOptional(@Nullable ResourceLocation pName) {
      return Optional.ofNullable(super.get(pName));
   }

   @Nonnull
   public T byId(int pId) {
      T t = super.byId(pId);
      return (T)(t == null ? this.defaultValue.value() : t);
   }

   public Optional<Holder<T>> getRandom(RandomSource pRandom) {
      return super.getRandom(pRandom).or(() -> {
         return Optional.of(this.defaultValue);
      });
   }

   public ResourceLocation getDefaultKey() {
      return this.defaultKey;
   }
}