package net.minecraft.resources;

import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

public interface RegistryResourceAccess {
   <E> Map<ResourceKey<E>, RegistryResourceAccess.EntryThunk<E>> listResources(ResourceKey<? extends Registry<E>> p_214235_);

   <E> Optional<RegistryResourceAccess.EntryThunk<E>> getResource(ResourceKey<E> p_214236_);

   static RegistryResourceAccess forResourceManager(final ResourceManager p_195882_) {
      return new RegistryResourceAccess() {
         private static final String JSON = ".json";

         public <E> Map<ResourceKey<E>, RegistryResourceAccess.EntryThunk<E>> listResources(ResourceKey<? extends Registry<E>> p_214238_) {
            String s = registryDirPath(p_214238_.location());
            Map<ResourceKey<E>, RegistryResourceAccess.EntryThunk<E>> map = Maps.newHashMap();
            p_195882_.listResources(s, (p_214262_) -> {
               return p_214262_.getPath().endsWith(".json");
            }).forEach((p_214257_, p_214258_) -> {
               String s1 = p_214257_.getPath();
               String s2 = s1.substring(s.length() + 1, s1.length() - ".json".length());
               ResourceKey<E> resourcekey = ResourceKey.create(p_214238_, new ResourceLocation(p_214257_.getNamespace(), s2));
               map.put(resourcekey, (p_214266_, p_214267_) -> {
                  try {
                     Reader reader = p_214258_.openAsReader();

                     DataResult dataresult;
                     try {
                        dataresult = this.decodeElement(p_214266_, p_214267_, reader, p_214238_);
                     } catch (Throwable throwable1) {
                        if (reader != null) {
                           try {
                              reader.close();
                           } catch (Throwable throwable) {
                              throwable1.addSuppressed(throwable);
                           }
                        }

                        throw throwable1;
                     }

                     if (reader != null) {
                        reader.close();
                     }

                     return dataresult;
                  } catch (JsonIOException | JsonSyntaxException | IOException ioexception) {
                     return DataResult.error("Failed to parse " + p_214257_ + " file: " + ioexception.getMessage());
                  }
               });
            });
            return map;
         }

         public <E> Optional<RegistryResourceAccess.EntryThunk<E>> getResource(ResourceKey<E> p_214260_) {
            ResourceLocation resourcelocation = elementPath(p_214260_);
            return p_195882_.getResource(resourcelocation).map((p_214243_) -> {
               return (p_214247_, p_214248_) -> {
                  try {
                     Reader reader = p_214243_.openAsReader();

                     DataResult dataresult;
                     try {
                        dataresult = this.decodeElement(p_214247_, p_214248_, reader, p_214260_);
                     } catch (Throwable throwable1) {
                        if (reader != null) {
                           try {
                              reader.close();
                           } catch (Throwable throwable) {
                              throwable1.addSuppressed(throwable);
                           }
                        }

                        throw throwable1;
                     }

                     if (reader != null) {
                        reader.close();
                     }

                     return dataresult;
                  } catch (JsonIOException | JsonSyntaxException | IOException ioexception) {
                     return DataResult.error("Failed to parse " + resourcelocation + " file: " + ioexception.getMessage());
                  }
               };
            });
         }

         private <E> DataResult<RegistryResourceAccess.ParsedEntry<E>> decodeElement(DynamicOps<JsonElement> p_214250_, Decoder<E> p_214251_, Reader p_214252_, ResourceKey<?> key) throws IOException {
            JsonElement jsonelement = JsonParser.parseReader(p_214252_);
            if (jsonelement != null) jsonelement.getAsJsonObject().addProperty("forge:registry_name", key.location().toString());
            return p_214251_.parse(p_214250_, jsonelement).map(RegistryResourceAccess.ParsedEntry::createWithoutId);
         }

         private static String registryDirPath(ResourceLocation p_214240_) {
            return net.minecraftforge.common.ForgeHooks.prefixNamespace(p_214240_); // FORGE: add non-vanilla registry namespace to loader directory, same format as tag directory (see net.minecraft.tags.TagManager#getTagDir(ResourceKey))
         }

         private static <E> ResourceLocation elementPath(ResourceKey<E> p_214269_) {
            return new ResourceLocation(p_214269_.location().getNamespace(), registryDirPath(p_214269_.registry()) + "/" + p_214269_.location().getPath() + ".json");
         }

         public String toString() {
            return "ResourceAccess[" + p_195882_ + "]";
         }
      };
   }

   @FunctionalInterface
   public interface EntryThunk<E> {
      DataResult<RegistryResourceAccess.ParsedEntry<E>> parseElement(DynamicOps<JsonElement> p_214271_, Decoder<E> p_214272_);
   }

   public static final class InMemoryStorage implements RegistryResourceAccess {
      private static final Logger LOGGER = LogUtils.getLogger();
      private final Map<ResourceKey<?>, RegistryResourceAccess.InMemoryStorage.Entry> entries = Maps.newIdentityHashMap();

      public <E> void add(RegistryAccess p_206837_, ResourceKey<E> p_206838_, Encoder<E> p_206839_, int p_206840_, E p_206841_, Lifecycle p_206842_) {
         DataResult<JsonElement> dataresult = p_206839_.encodeStart(RegistryOps.create(JsonOps.INSTANCE, p_206837_), p_206841_);
         Optional<DataResult.PartialResult<JsonElement>> optional = dataresult.error();
         if (optional.isPresent()) {
            LOGGER.error("Error adding element: {}", (Object)optional.get().message());
         } else {
            this.entries.put(p_206838_, new RegistryResourceAccess.InMemoryStorage.Entry(dataresult.result().get(), p_206840_, p_206842_));
         }

      }

      public <E> Map<ResourceKey<E>, RegistryResourceAccess.EntryThunk<E>> listResources(ResourceKey<? extends Registry<E>> p_214274_) {
         return this.entries.entrySet().stream().filter((p_214277_) -> {
            return p_214277_.getKey().isFor(p_214274_);
         }).collect(Collectors.toMap((p_214287_) -> {
            return (ResourceKey<E>)p_214287_.getKey();
         }, (p_214283_) -> {
            return p_214283_.getValue()::parse;
         }));
      }

      public <E> Optional<RegistryResourceAccess.EntryThunk<E>> getResource(ResourceKey<E> p_214285_) {
         RegistryResourceAccess.InMemoryStorage.Entry registryresourceaccess$inmemorystorage$entry = this.entries.get(p_214285_);
         if (registryresourceaccess$inmemorystorage$entry == null) {
            DataResult<RegistryResourceAccess.ParsedEntry<E>> dataresult = DataResult.error("Unknown element: " + p_214285_);
            return Optional.of((p_214280_, p_214281_) -> {
               return dataresult;
            });
         } else {
            return Optional.of(registryresourceaccess$inmemorystorage$entry::parse);
         }
      }

      static record Entry(JsonElement data, int id, Lifecycle lifecycle) {
         public <E> DataResult<RegistryResourceAccess.ParsedEntry<E>> parse(DynamicOps<JsonElement> p_214289_, Decoder<E> p_214290_) {
            return p_214290_.parse(p_214289_, this.data).setLifecycle(this.lifecycle).map((p_214292_) -> {
               return RegistryResourceAccess.ParsedEntry.createWithId(p_214292_, this.id);
            });
         }
      }
   }

   public static record ParsedEntry<E>(E value, OptionalInt fixedId) {
      public static <E> RegistryResourceAccess.ParsedEntry<E> createWithoutId(E p_195957_) {
         return new RegistryResourceAccess.ParsedEntry<>(p_195957_, OptionalInt.empty());
      }

      public static <E> RegistryResourceAccess.ParsedEntry<E> createWithId(E p_195959_, int p_195960_) {
         return new RegistryResourceAccess.ParsedEntry<>(p_195959_, OptionalInt.of(p_195960_));
      }
   }
}
