package net.minecraft.core;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.codecs.UnboundedMapCodec;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.network.chat.ChatType;
import net.minecraft.resources.RegistryLoader;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.RegistryResourceAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorPreset;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.slf4j.Logger;

/**
 * The root level registry, essentially a registry of registries. It is also an access point, hence the name, for other
 * dynamic registries.
 */
public interface RegistryAccess {
   Logger LOGGER = LogUtils.getLogger();
   /**
    * Metadata about all registries. Maps registry keys to a {@link RegistryData} object, which defines the codecs, and
    * if applicable, codecs for synchronization of the registry's elements.
    */
   Map<ResourceKey<? extends Registry<?>>, RegistryAccess.RegistryData<?>> REGISTRIES = Util.make(() -> {
      ImmutableMap.Builder<ResourceKey<? extends Registry<?>>, RegistryAccess.RegistryData<?>> builder = ImmutableMap.builder();
      put(builder, Registry.DIMENSION_TYPE_REGISTRY, DimensionType.DIRECT_CODEC, DimensionType.DIRECT_CODEC);
      put(builder, Registry.BIOME_REGISTRY, Biome.DIRECT_CODEC, Biome.NETWORK_CODEC);
      put(builder, Registry.CONFIGURED_CARVER_REGISTRY, ConfiguredWorldCarver.DIRECT_CODEC);
      put(builder, Registry.CONFIGURED_FEATURE_REGISTRY, ConfiguredFeature.DIRECT_CODEC);
      put(builder, Registry.PLACED_FEATURE_REGISTRY, PlacedFeature.DIRECT_CODEC);
      put(builder, Registry.STRUCTURE_REGISTRY, Structure.DIRECT_CODEC);
      put(builder, Registry.STRUCTURE_SET_REGISTRY, StructureSet.DIRECT_CODEC);
      put(builder, Registry.PROCESSOR_LIST_REGISTRY, StructureProcessorType.DIRECT_CODEC);
      put(builder, Registry.TEMPLATE_POOL_REGISTRY, StructureTemplatePool.DIRECT_CODEC);
      put(builder, Registry.NOISE_GENERATOR_SETTINGS_REGISTRY, NoiseGeneratorSettings.DIRECT_CODEC);
      put(builder, Registry.NOISE_REGISTRY, NormalNoise.NoiseParameters.DIRECT_CODEC);
      put(builder, Registry.DENSITY_FUNCTION_REGISTRY, DensityFunction.DIRECT_CODEC);
      put(builder, Registry.CHAT_TYPE_REGISTRY, ChatType.CODEC, ChatType.CODEC);
      put(builder, Registry.WORLD_PRESET_REGISTRY, WorldPreset.DIRECT_CODEC);
      put(builder, Registry.FLAT_LEVEL_GENERATOR_PRESET_REGISTRY, FlatLevelGeneratorPreset.DIRECT_CODEC);
      return net.minecraftforge.registries.DataPackRegistriesHooks.grabBuiltinRegistries(builder); // FORGE: Keep the map so custom registries can be added later
   });
   Codec<RegistryAccess> NETWORK_CODEC = makeNetworkCodec();
   /**
    * A registry access containing the builtin registries (excluding the dimension type registry).
    * When this class is loaded, this registry holder is initialized, which involves copying all elements from the
    * builtin registries at {@link net.minecraft.data.BuiltinRegistries} into this field, which contains the static,
    * code defined registries such as configured features, etc.
    * Early classloading of this class <strong>can cause issues</strong> because this field will not contain any
    * elements registered to the builtin registries after classloading of {@code RegistryAccess}.
    */
   Supplier<RegistryAccess.Frozen> BUILTIN = Suppliers.memoize(() -> {
      return builtinCopy().freeze();
   });

   /**
    * Get the registry owned by this registry access. The returned value, if it exists, will be writable.
    */
   <E> Optional<Registry<E>> ownedRegistry(ResourceKey<? extends Registry<? extends E>> pRegistryKey);

   default <E> Registry<E> ownedRegistryOrThrow(ResourceKey<? extends Registry<? extends E>> p_206192_) {
      return this.ownedRegistry(p_206192_).orElseThrow(() -> {
         return new IllegalStateException("Missing registry: " + p_206192_);
      });
   }

   /**
    * Get the registry owned by this registry access by the given key. If it doesn't exist, the default registry of
    * registries is queried instead, which contains static registries such as blocks.
    * The returned registry can not gaurentee that it is writable here, so the return type is widened to {@code
    * Registry<E>} instead.
    */
   default <E> Optional<? extends Registry<E>> registry(ResourceKey<? extends Registry<? extends E>> pRegistryKey) {
      Optional<? extends Registry<E>> optional = this.ownedRegistry(pRegistryKey);
      return optional.isPresent() ? optional : (Optional<? extends Registry<E>>)Registry.REGISTRY.getOptional(pRegistryKey.location());
   }

   /**
    * A variant of {@link #registry(ResourceKey)} that throws if the registry does not exist.
    */
   default <E> Registry<E> registryOrThrow(ResourceKey<? extends Registry<? extends E>> pRegistryKey) {
      return this.registry(pRegistryKey).orElseThrow(() -> {
         return new IllegalStateException("Missing registry: " + pRegistryKey);
      });
   }

   private static <E> void put(ImmutableMap.Builder<ResourceKey<? extends Registry<?>>, RegistryAccess.RegistryData<?>> pBuilder, ResourceKey<? extends Registry<E>> pRegistryKey, Codec<E> pElementCodec) {
      pBuilder.put(pRegistryKey, new RegistryAccess.RegistryData<>(pRegistryKey, pElementCodec, (Codec<E>)null));
   }

   private static <E> void put(ImmutableMap.Builder<ResourceKey<? extends Registry<?>>, RegistryAccess.RegistryData<?>> pBuilder, ResourceKey<? extends Registry<E>> pRegistryKey, Codec<E> pElementCodec, Codec<E> pNetworkCodec) {
      pBuilder.put(pRegistryKey, new RegistryAccess.RegistryData<>(pRegistryKey, pElementCodec, pNetworkCodec));
   }

   static Iterable<RegistryAccess.RegistryData<?>> knownRegistries() {
      return REGISTRIES.values();
   }

   Stream<RegistryAccess.RegistryEntry<?>> ownedRegistries();

   private static Stream<RegistryAccess.RegistryEntry<Object>> globalRegistries() {
      return Registry.REGISTRY.holders().map(RegistryAccess.RegistryEntry::fromHolder);
   }

   default Stream<RegistryAccess.RegistryEntry<?>> registries() {
      return Stream.concat(this.ownedRegistries(), globalRegistries());
   }

   default Stream<RegistryAccess.RegistryEntry<?>> networkSafeRegistries() {
      return Stream.concat(this.ownedNetworkableRegistries(), globalRegistries());
   }

   private static <E> Codec<RegistryAccess> makeNetworkCodec() {
      Codec<ResourceKey<? extends Registry<E>>> codec = ResourceLocation.CODEC.xmap(ResourceKey::createRegistryKey, ResourceKey::location);
      Codec<Registry<E>> codec1 = codec.partialDispatch("type", (p_206188_) -> {
         return DataResult.success(p_206188_.key());
      }, (p_206214_) -> {
         return getNetworkCodec(p_206214_).map((p_206183_) -> {
            return RegistryCodecs.networkCodec(p_206214_, Lifecycle.experimental(), p_206183_);
         });
      });
      UnboundedMapCodec<? extends ResourceKey<? extends Registry<?>>, ? extends Registry<?>> unboundedmapcodec = Codec.unboundedMap(codec, codec1);
      return captureMap(unboundedmapcodec);
   }

   private static <K extends ResourceKey<? extends Registry<?>>, V extends Registry<?>> Codec<RegistryAccess> captureMap(UnboundedMapCodec<K, V> pCodec) {
      return pCodec.xmap(RegistryAccess.ImmutableRegistryAccess::new, (p_206180_) -> {
         return p_206180_.ownedNetworkableRegistries().collect(ImmutableMap.toImmutableMap((p_206195_) -> {
            return (K)p_206195_.key();
         }, (p_206190_) -> {
            return (V)p_206190_.value();
         }));
      });
   }

   private Stream<RegistryAccess.RegistryEntry<?>> ownedNetworkableRegistries() {
      return this.ownedRegistries().filter((p_206170_) -> {
         return REGISTRIES.get(p_206170_.key).sendToClient();
      });
   }

   private static <E> DataResult<? extends Codec<E>> getNetworkCodec(ResourceKey<? extends Registry<E>> pKey) {
      return Optional.ofNullable(REGISTRIES.get(pKey)).map((p_206168_) -> {
         return (Codec<E>)p_206168_.networkCodec();
      }).map(DataResult::success).orElseGet(() -> {
         return DataResult.error("Unknown or not serializable registry: " + pKey);
      });
   }

   private static Map<ResourceKey<? extends Registry<?>>, ? extends WritableRegistry<?>> createFreshRegistries() {
      return REGISTRIES.keySet().stream().collect(Collectors.toMap(Function.identity(), RegistryAccess::createRegistry));
   }

   private static RegistryAccess.Writable blankWriteable() {
      return new RegistryAccess.WritableRegistryAccess(createFreshRegistries());
   }

   static RegistryAccess.Frozen fromRegistryOfRegistries(final Registry<? extends Registry<?>> pRegistryOfRegistries) {
      return new RegistryAccess.Frozen() {
         /**
          * Get the registry owned by this registry access. The returned value, if it exists, will be writable.
          */
         public <T> Optional<Registry<T>> ownedRegistry(ResourceKey<? extends Registry<? extends T>> p_206220_) {
            Registry<Registry<T>> registry = (Registry<Registry<T>>)pRegistryOfRegistries;
            return registry.getOptional((ResourceKey<Registry<T>>)p_206220_);
         }

         public Stream<RegistryAccess.RegistryEntry<?>> ownedRegistries() {
            return pRegistryOfRegistries.entrySet().stream().map(RegistryAccess.RegistryEntry::fromMapEntry);
         }
      };
   }

   static RegistryAccess.Writable builtinCopy() {
      RegistryAccess.Writable registryaccess$writable = blankWriteable();
      RegistryResourceAccess.InMemoryStorage registryresourceaccess$inmemorystorage = new RegistryResourceAccess.InMemoryStorage();

      for(Map.Entry<ResourceKey<? extends Registry<?>>, RegistryAccess.RegistryData<?>> entry : REGISTRIES.entrySet()) {
         addBuiltinElements(registryresourceaccess$inmemorystorage, entry.getValue());
      }

      RegistryOps.createAndLoad(JsonOps.INSTANCE, registryaccess$writable, registryresourceaccess$inmemorystorage);
      return registryaccess$writable;
   }

   private static <E> void addBuiltinElements(RegistryResourceAccess.InMemoryStorage pDestinationRegistryHolder, RegistryAccess.RegistryData<E> pData) {
      ResourceKey<? extends Registry<E>> resourcekey = pData.key();
      Registry<E> registry = BuiltinRegistries.ACCESS.registryOrThrow(resourcekey);

      for(Map.Entry<ResourceKey<E>, E> entry : registry.entrySet()) {
         ResourceKey<E> resourcekey1 = entry.getKey();
         E e = entry.getValue();
         pDestinationRegistryHolder.add(BuiltinRegistries.ACCESS, resourcekey1, pData.codec(), registry.getId(e), e, registry.lifecycle(e));
      }

   }

   static void load(RegistryAccess.Writable p_206172_, DynamicOps<JsonElement> p_206173_, RegistryLoader pLoader) {
      RegistryLoader.Bound registryloader$bound = pLoader.bind(p_206172_);

      for(RegistryAccess.RegistryData<?> registrydata : REGISTRIES.values()) {
         readRegistry(p_206173_, registryloader$bound, registrydata);
      }

   }

   private static <E> void readRegistry(DynamicOps<JsonElement> p_206160_, RegistryLoader.Bound p_206161_, RegistryAccess.RegistryData<E> p_206162_) {
      DataResult<? extends Registry<E>> dataresult = p_206161_.overrideRegistryFromResources(p_206162_.key(), p_206162_.codec(), p_206160_);
      dataresult.error().ifPresent((p_206153_) -> {
         throw new JsonParseException("Error loading registry data: " + p_206153_.message());
      });
   }

   static RegistryAccess readFromDisk(Dynamic<?> p_206155_) {
      return new RegistryAccess.ImmutableRegistryAccess(REGISTRIES.keySet().stream().collect(Collectors.toMap(Function.identity(), (p_206158_) -> {
         return retrieveRegistry(p_206158_, p_206155_);
      })));
   }

   static <E> Registry<E> retrieveRegistry(ResourceKey<? extends Registry<? extends E>> p_206185_, Dynamic<?> p_206186_) {
      return RegistryOps.retrieveRegistry(p_206185_).codec().parse(p_206186_).resultOrPartial(Util.prefix(p_206185_ + " registry: ", LOGGER::error)).orElseThrow(() -> {
         return new IllegalStateException("Failed to get " + p_206185_ + " registry");
      });
   }

   static <E> WritableRegistry<?> createRegistry(ResourceKey<? extends Registry<?>> p_206201_) {
      return new MappedRegistry<>((ResourceKey<? extends Registry<E>>)p_206201_, Lifecycle.stable(), (Function<E, Holder.Reference<E>>)null);
   }

   default RegistryAccess.Frozen freeze() {
      return new RegistryAccess.ImmutableRegistryAccess(this.ownedRegistries().map(RegistryAccess.RegistryEntry::freeze));
   }

   default Lifecycle allElementsLifecycle() {
      return this.ownedRegistries().map((p_211815_) -> {
         return p_211815_.value.elementsLifecycle();
      }).reduce(Lifecycle.stable(), Lifecycle::add);
   }

   public interface Frozen extends RegistryAccess {
      default RegistryAccess.Frozen freeze() {
         return this;
      }
   }

   public static final class ImmutableRegistryAccess implements RegistryAccess.Frozen {
      private final Map<? extends ResourceKey<? extends Registry<?>>, ? extends Registry<?>> registries;

      public ImmutableRegistryAccess(Map<? extends ResourceKey<? extends Registry<?>>, ? extends Registry<?>> pRegistries) {
         this.registries = Map.copyOf(pRegistries);
      }

      ImmutableRegistryAccess(Stream<RegistryAccess.RegistryEntry<?>> pRegistries) {
         this.registries = pRegistries.collect(ImmutableMap.toImmutableMap(RegistryAccess.RegistryEntry::key, RegistryAccess.RegistryEntry::value));
      }

      /**
       * Get the registry owned by this registry access. The returned value, if it exists, will be writable.
       */
      public <E> Optional<Registry<E>> ownedRegistry(ResourceKey<? extends Registry<? extends E>> pRegistryKey) {
         return Optional.ofNullable(this.registries.get(pRegistryKey)).map((p_206232_) -> {
            return (Registry<E>)p_206232_;
         });
      }

      public Stream<RegistryAccess.RegistryEntry<?>> ownedRegistries() {
         return this.registries.entrySet().stream().map(RegistryAccess.RegistryEntry::fromMapEntry);
      }
   }

   public static record RegistryData<E>(ResourceKey<? extends Registry<E>> key, Codec<E> codec, @Nullable Codec<E> networkCodec) {
      /**
       * @return {@code true} if this registry should be synchronized with the client.
       */
      public boolean sendToClient() {
         return this.networkCodec != null;
      }
   }

   public static record RegistryEntry<T>(ResourceKey<? extends Registry<T>> key, Registry<T> value) {
      private static <T, R extends Registry<? extends T>> RegistryAccess.RegistryEntry<T> fromMapEntry(Map.Entry<? extends ResourceKey<? extends Registry<?>>, R> pMapEntry) {
         return fromUntyped(pMapEntry.getKey(), pMapEntry.getValue());
      }

      private static <T> RegistryAccess.RegistryEntry<T> fromHolder(Holder.Reference<? extends Registry<? extends T>> pHolder) {
         return fromUntyped(pHolder.key(), pHolder.value());
      }

      private static <T> RegistryAccess.RegistryEntry<T> fromUntyped(ResourceKey<? extends Registry<?>> pKey, Registry<?> pValue) {
         return new RegistryAccess.RegistryEntry<>((ResourceKey<? extends Registry<T>>)pKey, (Registry<T>)pValue);
      }

      private RegistryAccess.RegistryEntry<T> freeze() {
         return new RegistryAccess.RegistryEntry<>(this.key, this.value.freeze());
      }
   }

   public interface Writable extends RegistryAccess {
      <E> Optional<WritableRegistry<E>> ownedWritableRegistry(ResourceKey<? extends Registry<? extends E>> pRegistryKey);

      default <E> WritableRegistry<E> ownedWritableRegistryOrThrow(ResourceKey<? extends Registry<? extends E>> pRegistryKey) {
         return this.<E>ownedWritableRegistry(pRegistryKey).orElseThrow(() -> {
            return new IllegalStateException("Missing registry: " + pRegistryKey);
         });
      }
   }

   public static final class WritableRegistryAccess implements RegistryAccess.Writable {
      private final Map<? extends ResourceKey<? extends Registry<?>>, ? extends WritableRegistry<?>> registries;

      WritableRegistryAccess(Map<? extends ResourceKey<? extends Registry<?>>, ? extends WritableRegistry<?>> pRegistries) {
         this.registries = pRegistries;
      }

      /**
       * Get the registry owned by this registry access. The returned value, if it exists, will be writable.
       */
      public <E> Optional<Registry<E>> ownedRegistry(ResourceKey<? extends Registry<? extends E>> pRegistryKey) {
         return Optional.ofNullable(this.registries.get(pRegistryKey)).map((p_206266_) -> {
            return (Registry<E>)p_206266_;
         });
      }

      public <E> Optional<WritableRegistry<E>> ownedWritableRegistry(ResourceKey<? extends Registry<? extends E>> pRegistryKey) {
         return Optional.ofNullable(this.registries.get(pRegistryKey)).map((p_206261_) -> {
            return (WritableRegistry<E>)p_206261_;
         });
      }

      public Stream<RegistryAccess.RegistryEntry<?>> ownedRegistries() {
         return this.registries.entrySet().stream().map(RegistryAccess.RegistryEntry::fromMapEntry);
      }
   }
}
