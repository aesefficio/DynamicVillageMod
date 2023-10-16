package net.minecraft.core;

import com.mojang.serialization.Lifecycle;
import java.util.OptionalInt;
import net.minecraft.resources.ResourceKey;

public abstract class WritableRegistry<T> extends Registry<T> {
   public WritableRegistry(ResourceKey<? extends Registry<T>> pKey, Lifecycle pLifecycle) {
      super(pKey, pLifecycle);
   }

   public abstract Holder<T> registerMapping(int pId, ResourceKey<T> pKey, T pValue, Lifecycle pLifecycle);

   public abstract Holder<T> register(ResourceKey<T> pKey, T pValue, Lifecycle pLifecycle);

   public abstract Holder<T> registerOrOverride(OptionalInt pId, ResourceKey<T> pKey, T pValue, Lifecycle pLifecycle);

   public abstract boolean isEmpty();
}