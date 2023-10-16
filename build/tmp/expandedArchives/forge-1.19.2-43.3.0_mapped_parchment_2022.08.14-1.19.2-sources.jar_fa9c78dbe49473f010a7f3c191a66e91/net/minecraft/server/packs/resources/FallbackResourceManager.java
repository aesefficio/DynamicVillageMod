package net.minecraft.server.packs.resources;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import org.slf4j.Logger;

public class FallbackResourceManager implements ResourceManager {
   static final Logger LOGGER = LogUtils.getLogger();
   public final List<FallbackResourceManager.PackEntry> fallbacks = Lists.newArrayList();
   final PackType type;
   private final String namespace;

   public FallbackResourceManager(PackType pType, String pNamespace) {
      this.type = pType;
      this.namespace = pNamespace;
   }

   public void push(PackResources pResources) {
      this.pushInternal(pResources.getName(), pResources, (Predicate<ResourceLocation>)null);
   }

   public void push(PackResources pResources, Predicate<ResourceLocation> pFilter) {
      this.pushInternal(pResources.getName(), pResources, pFilter);
   }

   public void pushFilterOnly(String pName, Predicate<ResourceLocation> pFilter) {
      this.pushInternal(pName, (PackResources)null, pFilter);
   }

   private void pushInternal(String pName, @Nullable PackResources pResources, @Nullable Predicate<ResourceLocation> pFilter) {
      this.fallbacks.add(new FallbackResourceManager.PackEntry(pName, pResources, pFilter));
   }

   public Set<String> getNamespaces() {
      return ImmutableSet.of(this.namespace);
   }

   public Optional<Resource> getResource(ResourceLocation pLocation) {
      if (!this.isValidLocation(pLocation)) {
         return Optional.empty();
      } else {
         for(int i = this.fallbacks.size() - 1; i >= 0; --i) {
            FallbackResourceManager.PackEntry fallbackresourcemanager$packentry = this.fallbacks.get(i);
            PackResources packresources = fallbackresourcemanager$packentry.resources;
            if (packresources != null && packresources.hasResource(this.type, pLocation)) {
               return Optional.of(new Resource(packresources.getName(), this.createResourceGetter(pLocation, packresources), this.createStackMetadataFinder(pLocation, i)));
            }

            if (fallbackresourcemanager$packentry.isFiltered(pLocation)) {
               LOGGER.warn("Resource {} not found, but was filtered by pack {}", pLocation, fallbackresourcemanager$packentry.name);
               return Optional.empty();
            }
         }

         return Optional.empty();
      }
   }

   Resource.IoSupplier<InputStream> createResourceGetter(ResourceLocation pLocation, PackResources pResources) {
      return LOGGER.isDebugEnabled() ? () -> {
         InputStream inputstream = pResources.getResource(this.type, pLocation);
         return new FallbackResourceManager.LeakedResourceWarningInputStream(inputstream, pLocation, pResources.getName());
      } : () -> {
         return pResources.getResource(this.type, pLocation);
      };
   }

   private boolean isValidLocation(ResourceLocation pLocation) {
      return !pLocation.getPath().contains("..");
   }

   public List<Resource> getResourceStack(ResourceLocation pLocation) {
      if (!this.isValidLocation(pLocation)) {
         return List.of();
      } else {
         List<FallbackResourceManager.SinglePackResourceThunkSupplier> list = Lists.newArrayList();
         ResourceLocation resourcelocation = getMetadataLocation(pLocation);
         String s = null;

         for(FallbackResourceManager.PackEntry fallbackresourcemanager$packentry : this.fallbacks) {
            if (fallbackresourcemanager$packentry.isFiltered(pLocation)) {
               if (!list.isEmpty()) {
                  s = fallbackresourcemanager$packentry.name;
               }

               list.clear();
            } else if (fallbackresourcemanager$packentry.isFiltered(resourcelocation)) {
               list.forEach(FallbackResourceManager.SinglePackResourceThunkSupplier::ignoreMeta);
            }

            PackResources packresources = fallbackresourcemanager$packentry.resources;
            if (packresources != null && packresources.hasResource(this.type, pLocation)) {
               list.add(new FallbackResourceManager.SinglePackResourceThunkSupplier(pLocation, resourcelocation, packresources));
            }
         }

         if (list.isEmpty() && s != null) {
            LOGGER.info("Resource {} was filtered by pack {}", pLocation, s);
         }

         return list.stream().map(FallbackResourceManager.SinglePackResourceThunkSupplier::create).toList();
      }
   }

   public Map<ResourceLocation, Resource> listResources(String pPath, Predicate<ResourceLocation> pFilter) {
      Object2IntMap<ResourceLocation> object2intmap = new Object2IntOpenHashMap<>();
      int i = this.fallbacks.size();

      for(int j = 0; j < i; ++j) {
         FallbackResourceManager.PackEntry fallbackresourcemanager$packentry = this.fallbacks.get(j);
         fallbackresourcemanager$packentry.filterAll(object2intmap.keySet());
         if (fallbackresourcemanager$packentry.resources != null) {
            for(ResourceLocation resourcelocation : fallbackresourcemanager$packentry.resources.getResources(this.type, this.namespace, pPath, pFilter)) {
               object2intmap.put(resourcelocation, j);
            }
         }
      }

      Map<ResourceLocation, Resource> map = Maps.newTreeMap();

      for(Object2IntMap.Entry<ResourceLocation> entry : Object2IntMaps.fastIterable(object2intmap)) {
         int k = entry.getIntValue();
         ResourceLocation resourcelocation1 = entry.getKey();
         PackResources packresources = (this.fallbacks.get(k)).resources;
         map.put(resourcelocation1, new Resource(packresources.getName(), this.createResourceGetter(resourcelocation1, packresources), this.createStackMetadataFinder(resourcelocation1, k)));
      }

      return map;
   }

   private Resource.IoSupplier<ResourceMetadata> createStackMetadataFinder(ResourceLocation pLocation, int p_215370_) {
      return () -> {
         ResourceLocation resourcelocation = getMetadataLocation(pLocation);

         for(int i = this.fallbacks.size() - 1; i >= p_215370_; --i) {
            FallbackResourceManager.PackEntry fallbackresourcemanager$packentry = this.fallbacks.get(i);
            PackResources packresources = fallbackresourcemanager$packentry.resources;
            if (packresources != null && packresources.hasResource(this.type, resourcelocation)) {
               InputStream inputstream = packresources.getResource(this.type, resourcelocation);

               ResourceMetadata resourcemetadata;
               try {
                  resourcemetadata = ResourceMetadata.fromJsonStream(inputstream);
               } catch (Throwable throwable1) {
                  if (inputstream != null) {
                     try {
                        inputstream.close();
                     } catch (Throwable throwable) {
                        throwable1.addSuppressed(throwable);
                     }
                  }

                  throw throwable1;
               }

               if (inputstream != null) {
                  inputstream.close();
               }

               return resourcemetadata;
            }

            if (fallbackresourcemanager$packentry.isFiltered(resourcelocation)) {
               break;
            }
         }

         return ResourceMetadata.EMPTY;
      };
   }

   private static void applyPackFiltersToExistingResources(FallbackResourceManager.PackEntry p_215393_, Map<ResourceLocation, FallbackResourceManager.EntryStack> p_215394_) {
      Iterator<Map.Entry<ResourceLocation, FallbackResourceManager.EntryStack>> iterator = p_215394_.entrySet().iterator();

      while(iterator.hasNext()) {
         Map.Entry<ResourceLocation, FallbackResourceManager.EntryStack> entry = iterator.next();
         ResourceLocation resourcelocation = entry.getKey();
         FallbackResourceManager.EntryStack fallbackresourcemanager$entrystack = entry.getValue();
         if (p_215393_.isFiltered(resourcelocation)) {
            iterator.remove();
         } else if (p_215393_.isFiltered(fallbackresourcemanager$entrystack.metadataLocation())) {
            fallbackresourcemanager$entrystack.entries.forEach(FallbackResourceManager.SinglePackResourceThunkSupplier::ignoreMeta);
         }
      }

   }

   private void listPackResources(FallbackResourceManager.PackEntry p_215388_, String p_215389_, Predicate<ResourceLocation> p_215390_, Map<ResourceLocation, FallbackResourceManager.EntryStack> p_215391_) {
      PackResources packresources = p_215388_.resources;
      if (packresources != null) {
         for(ResourceLocation resourcelocation : packresources.getResources(this.type, this.namespace, p_215389_, p_215390_)) {
            ResourceLocation resourcelocation1 = getMetadataLocation(resourcelocation);
            p_215391_.computeIfAbsent(resourcelocation, (p_215373_) -> {
               return new FallbackResourceManager.EntryStack(resourcelocation1, Lists.newArrayList());
            }).entries().add(new FallbackResourceManager.SinglePackResourceThunkSupplier(resourcelocation, resourcelocation1, packresources));
         }

      }
   }

   public Map<ResourceLocation, List<Resource>> listResourceStacks(String p_215416_, Predicate<ResourceLocation> pFilter) {
      Map<ResourceLocation, FallbackResourceManager.EntryStack> map = Maps.newHashMap();

      for(FallbackResourceManager.PackEntry fallbackresourcemanager$packentry : this.fallbacks) {
         applyPackFiltersToExistingResources(fallbackresourcemanager$packentry, map);
         this.listPackResources(fallbackresourcemanager$packentry, p_215416_, pFilter, map);
      }

      TreeMap<ResourceLocation, List<Resource>> treemap = Maps.newTreeMap();
      map.forEach((p_215404_, p_215405_) -> {
         treemap.put(p_215404_, p_215405_.createThunks());
      });
      return treemap;
   }

   public Stream<PackResources> listPacks() {
      return this.fallbacks.stream().map((p_215386_) -> {
         return p_215386_.resources;
      }).filter(Objects::nonNull);
   }

   static ResourceLocation getMetadataLocation(ResourceLocation pLocation) {
      return new ResourceLocation(pLocation.getNamespace(), pLocation.getPath() + ".mcmeta");
   }

   static record EntryStack(ResourceLocation metadataLocation, List<FallbackResourceManager.SinglePackResourceThunkSupplier> entries) {
      List<Resource> createThunks() {
         return this.entries().stream().map(FallbackResourceManager.SinglePackResourceThunkSupplier::create).toList();
      }
   }

   static class LeakedResourceWarningInputStream extends FilterInputStream {
      private final String message;
      private boolean closed;

      public LeakedResourceWarningInputStream(InputStream pInputStream, ResourceLocation pResourceLocation, String pPackName) {
         super(pInputStream);
         ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream();
         (new Exception()).printStackTrace(new PrintStream(bytearrayoutputstream));
         this.message = "Leaked resource: '" + pResourceLocation + "' loaded from pack: '" + pPackName + "'\n" + bytearrayoutputstream;
      }

      public void close() throws IOException {
         super.close();
         this.closed = true;
      }

      protected void finalize() throws Throwable {
         if (!this.closed) {
            FallbackResourceManager.LOGGER.warn(this.message);
         }

         super.finalize();
      }
   }

   static record PackEntry(String name, @Nullable PackResources resources, @Nullable Predicate<ResourceLocation> filter) {
      public void filterAll(Collection<ResourceLocation> p_215443_) {
         if (this.filter != null) {
            p_215443_.removeIf(this.filter);
         }

      }

      public boolean isFiltered(ResourceLocation p_215441_) {
         return this.filter != null && this.filter.test(p_215441_);
      }
   }

   class SinglePackResourceThunkSupplier {
      private final ResourceLocation location;
      private final ResourceLocation metadataLocation;
      private final PackResources source;
      private boolean shouldGetMeta = true;

      SinglePackResourceThunkSupplier(ResourceLocation pLocation, ResourceLocation pMetadataLocation, PackResources pSource) {
         this.source = pSource;
         this.location = pLocation;
         this.metadataLocation = pMetadataLocation;
      }

      public void ignoreMeta() {
         this.shouldGetMeta = false;
      }

      public Resource create() {
         String s = this.source.getName();
         return this.shouldGetMeta ? new Resource(s, FallbackResourceManager.this.createResourceGetter(this.location, this.source), () -> {
            if (this.source.hasResource(FallbackResourceManager.this.type, this.metadataLocation)) {
               InputStream inputstream = this.source.getResource(FallbackResourceManager.this.type, this.metadataLocation);

               ResourceMetadata resourcemetadata;
               try {
                  resourcemetadata = ResourceMetadata.fromJsonStream(inputstream);
               } catch (Throwable throwable1) {
                  if (inputstream != null) {
                     try {
                        inputstream.close();
                     } catch (Throwable throwable) {
                        throwable1.addSuppressed(throwable);
                     }
                  }

                  throw throwable1;
               }

               if (inputstream != null) {
                  inputstream.close();
               }

               return resourcemetadata;
            } else {
               return ResourceMetadata.EMPTY;
            }
         }) : new Resource(s, FallbackResourceManager.this.createResourceGetter(this.location, this.source));
      }
   }
}