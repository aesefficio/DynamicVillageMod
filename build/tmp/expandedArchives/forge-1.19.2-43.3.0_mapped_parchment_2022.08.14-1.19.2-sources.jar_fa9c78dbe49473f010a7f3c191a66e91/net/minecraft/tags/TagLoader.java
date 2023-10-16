package net.minecraft.tags;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

public class TagLoader<T> {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String PATH_SUFFIX = ".json";
   private static final int PATH_SUFFIX_LENGTH = ".json".length();
   final Function<ResourceLocation, Optional<T>> idToValue;
   private final String directory;

   public TagLoader(Function<ResourceLocation, Optional<T>> pIdToValue, String pDirectory) {
      this.idToValue = pIdToValue;
      this.directory = pDirectory;
   }

   public Map<ResourceLocation, List<TagLoader.EntryWithSource>> load(ResourceManager pResourceManager) {
      Map<ResourceLocation, List<TagLoader.EntryWithSource>> map = Maps.newHashMap();

      for(Map.Entry<ResourceLocation, List<Resource>> entry : pResourceManager.listResourceStacks(this.directory, (p_216016_) -> {
         return p_216016_.getPath().endsWith(".json");
      }).entrySet()) {
         ResourceLocation resourcelocation = entry.getKey();
         String s = resourcelocation.getPath();
         ResourceLocation resourcelocation1 = new ResourceLocation(resourcelocation.getNamespace(), s.substring(this.directory.length() + 1, s.length() - PATH_SUFFIX_LENGTH));

         for(Resource resource : entry.getValue()) {
            try {
               Reader reader = resource.openAsReader();

               try {
                  JsonElement jsonelement = JsonParser.parseReader(reader);
                  List<TagLoader.EntryWithSource> list = map.computeIfAbsent(resourcelocation1, (p_215974_) -> {
                     return new ArrayList();
                  });
                  TagFile tagfile = TagFile.CODEC.parse(new Dynamic<>(JsonOps.INSTANCE, jsonelement)).getOrThrow(false, LOGGER::error);
                  if (tagfile.replace()) {
                     list.clear();
                  }

                  String s1 = resource.sourcePackId();
                  tagfile.entries().forEach((p_215997_) -> {
                     list.add(new TagLoader.EntryWithSource(p_215997_, s1));
                  });
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
            } catch (Exception exception) {
               LOGGER.error("Couldn't read tag list {} from {} in data pack {}", resourcelocation1, resourcelocation, resource.sourcePackId(), exception);
            }
         }
      }

      return map;
   }

   private static void visitDependenciesAndElement(Map<ResourceLocation, List<TagLoader.EntryWithSource>> pBuilders, Multimap<ResourceLocation, ResourceLocation> pDependencyNames, Set<ResourceLocation> pNames, ResourceLocation pName, BiConsumer<ResourceLocation, List<TagLoader.EntryWithSource>> pVisitor) {
      if (pNames.add(pName)) {
         pDependencyNames.get(pName).forEach((p_216014_) -> {
            visitDependenciesAndElement(pBuilders, pDependencyNames, pNames, p_216014_, pVisitor);
         });
         List<TagLoader.EntryWithSource> list = pBuilders.get(pName);
         if (list != null) {
            pVisitor.accept(pName, list);
         }

      }
   }

   private static boolean isCyclic(Multimap<ResourceLocation, ResourceLocation> pDependencyNames, ResourceLocation pName, ResourceLocation pDependencyName) {
      Collection<ResourceLocation> collection = pDependencyNames.get(pDependencyName);
      return collection.contains(pName) ? true : collection.stream().anyMatch((p_216032_) -> {
         return isCyclic(pDependencyNames, pName, p_216032_);
      });
   }

   private static void addDependencyIfNotCyclic(Multimap<ResourceLocation, ResourceLocation> pDependencyNames, ResourceLocation pName, ResourceLocation pDependencyName) {
      if (!isCyclic(pDependencyNames, pName, pDependencyName)) {
         pDependencyNames.put(pName, pDependencyName);
      }

   }

   private Either<Collection<TagLoader.EntryWithSource>, Collection<T>> build(TagEntry.Lookup<T> p_215979_, List<TagLoader.EntryWithSource> p_215980_) {
      ImmutableSet.Builder<T> builder = ImmutableSet.builder();
      List<TagLoader.EntryWithSource> list = new ArrayList<>();

      for(TagLoader.EntryWithSource tagloader$entrywithsource : p_215980_) {
         if (!tagloader$entrywithsource.entry().build(p_215979_, builder::add)) {
            list.add(tagloader$entrywithsource);
         }
      }

      return list.isEmpty() ? Either.right(builder.build()) : Either.left(list);
   }

   public Map<ResourceLocation, Collection<T>> build(Map<ResourceLocation, List<TagLoader.EntryWithSource>> pBuilders) {
      final Map<ResourceLocation, Collection<T>> map = Maps.newHashMap();
      TagEntry.Lookup<T> lookup = new TagEntry.Lookup<T>() {
         @Nullable
         public T element(ResourceLocation p_216039_) {
            return TagLoader.this.idToValue.apply(p_216039_).orElse((T)null);
         }

         @Nullable
         public Collection<T> tag(ResourceLocation p_216041_) {
            return map.get(p_216041_);
         }
      };
      Multimap<ResourceLocation, ResourceLocation> multimap = HashMultimap.create();
      pBuilders.forEach((p_216023_, p_216024_) -> {
         p_216024_.forEach((p_216020_) -> {
            p_216020_.entry.visitRequiredDependencies((p_144563_) -> {
               addDependencyIfNotCyclic(multimap, p_216023_, p_144563_);
            });
         });
      });
      pBuilders.forEach((p_215992_, p_215993_) -> {
         p_215993_.forEach((p_215989_) -> {
            p_215989_.entry.visitOptionalDependencies((p_216028_) -> {
               addDependencyIfNotCyclic(multimap, p_215992_, p_216028_);
            });
         });
      });
      Set<ResourceLocation> set = Sets.newHashSet();
      pBuilders.keySet().forEach((p_216008_) -> {
         visitDependenciesAndElement(pBuilders, multimap, set, p_216008_, (p_215984_, p_215985_) -> {
            this.build(lookup, p_215985_).ifLeft((p_215977_) -> {
               LOGGER.error("Couldn't load tag {} as it is missing following references: {}", p_215977_, p_215977_.stream().map(Objects::toString).collect(Collectors.joining(", \n\t")));
            }).ifRight((p_216001_) -> {
               map.put(p_215984_, p_216001_);
            });
         });
      });
      return map;
   }

   public Map<ResourceLocation, Collection<T>> loadAndBuild(ResourceManager pResourceManager) {
      return this.build(this.load(pResourceManager));
   }

   public static record EntryWithSource(TagEntry entry, String source) {
      public String toString() {
         return this.entry + " (from " + this.source + ")";
      }
   }
}
