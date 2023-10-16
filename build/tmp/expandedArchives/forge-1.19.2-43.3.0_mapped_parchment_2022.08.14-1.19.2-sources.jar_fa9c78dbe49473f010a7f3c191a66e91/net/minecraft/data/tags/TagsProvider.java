package net.minecraft.data.tags;

import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.Registry;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagFile;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagManager;
import org.slf4j.Logger;

public abstract class TagsProvider<T> implements DataProvider {
   private static final Logger LOGGER = LogUtils.getLogger();
   protected final DataGenerator.PathProvider pathProvider;
   protected final Registry<T> registry;
   protected final Map<ResourceLocation, TagBuilder> builders = Maps.newLinkedHashMap();
   protected final String modId;
   protected final net.minecraftforge.common.data.ExistingFileHelper existingFileHelper;
   private final net.minecraftforge.common.data.ExistingFileHelper.IResourceType resourceType;
   private final net.minecraftforge.common.data.ExistingFileHelper.IResourceType elementResourceType; // FORGE: Resource type for validating required references to datapack registry elements.

   /**
    * @see #TagsProvider(DataGenerator, Registry, String, net.minecraftforge.common.data.ExistingFileHelper)
    * @deprecated Forge: Use the mod id variant
    */
   @Deprecated
   protected TagsProvider(DataGenerator pGenerator, Registry<T> pRegistry) {
      this(pGenerator, pRegistry, "vanilla", null);
   }
   protected TagsProvider(DataGenerator pGenerator, Registry<T> pRegistry, String modId, @org.jetbrains.annotations.Nullable net.minecraftforge.common.data.ExistingFileHelper existingFileHelper) {
      this.pathProvider = pGenerator.createPathProvider(DataGenerator.Target.DATA_PACK, TagManager.getTagDir(pRegistry.key()));
      this.registry = pRegistry;
      this.modId = modId;
      this.existingFileHelper = existingFileHelper;
      this.resourceType = new net.minecraftforge.common.data.ExistingFileHelper.ResourceType(net.minecraft.server.packs.PackType.SERVER_DATA, ".json", TagManager.getTagDir(pRegistry.key()));
      this.elementResourceType = new net.minecraftforge.common.data.ExistingFileHelper.ResourceType(net.minecraft.server.packs.PackType.SERVER_DATA, ".json", net.minecraftforge.common.ForgeHooks.prefixNamespace(pRegistry.key().location()));
   }

   // Forge: Allow customizing the path for a given tag or returning null
   @org.jetbrains.annotations.Nullable
   protected Path getPath(ResourceLocation id) {
      return this.pathProvider.json(id);
   }

   /**
    * Gets a name for this provider, to use in logging.
    */
   public String getName() {
      return "Tags for " + this.registry.key().location();
   }

   protected abstract void addTags();

   public void run(CachedOutput pOutput) {
      this.builders.clear();
      this.addTags();
      this.builders.forEach((p_236449_, p_236450_) -> {
         List<TagEntry> list = p_236450_.build();
         List<TagEntry> list1 = list.stream().filter((p_236444_) -> {
            return !p_236444_.verifyIfPresent(this.registry::containsKey, this.builders::containsKey);
         }).filter(this::missing).collect(Collectors.toList()); // Forge: Add validation via existing resources
         if (!list1.isEmpty()) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, "Couldn't define tag %s as it is missing following references: %s", p_236449_, list1.stream().map(Objects::toString).collect(Collectors.joining(","))));
         } else {
            JsonElement jsonelement = TagFile.CODEC.encodeStart(JsonOps.INSTANCE, new TagFile(list, false)).getOrThrow(false, LOGGER::error);
            Path path = this.getPath(p_236449_);
            if (path == null) return; // Forge: Allow running this data provider without writing it. Recipe provider needs valid tags.

            try {
               DataProvider.saveStable(pOutput, jsonelement, path);
            } catch (IOException ioexception) {
               LOGGER.error("Couldn't save tags to {}", path, ioexception);
            }

         }
      });
   }

   private boolean missing(TagEntry reference) {
      // Optional tags should not be validated

      if (reference.isRequired()) {
         return existingFileHelper == null || !existingFileHelper.exists(reference.getId(), reference.isTag() ? resourceType : elementResourceType);
      }
      return false;
   }

   protected TagsProvider.TagAppender<T> tag(TagKey<T> pTag) {
      TagBuilder tagbuilder = this.getOrCreateRawBuilder(pTag);
      return new TagsProvider.TagAppender<>(tagbuilder, this.registry, modId);
   }

   protected TagBuilder getOrCreateRawBuilder(TagKey<T> pTag) {
      return this.builders.computeIfAbsent(pTag.location(), (p_236442_) -> {
         existingFileHelper.trackGenerated(p_236442_, resourceType);
         return TagBuilder.create();
      });
   }

   public static class TagAppender<T> implements net.minecraftforge.common.extensions.IForgeTagAppender<T> {
      private final TagBuilder builder;
      public final Registry<T> registry;
      private final String modId;

      TagAppender(TagBuilder pBuilder, Registry<T> pRegistry, String modId) {
         this.builder = pBuilder;
         this.registry = pRegistry;
         this.modId = modId;
      }

      public TagsProvider.TagAppender<T> add(T pItem) {
         this.builder.addElement(this.registry.getKey(pItem));
         return this;
      }

      @SafeVarargs
      public final TagsProvider.TagAppender<T> add(ResourceKey<T>... pToAdd) {
         for(ResourceKey<T> resourcekey : pToAdd) {
            this.builder.addElement(resourcekey.location());
         }

         return this;
      }

      public TagsProvider.TagAppender<T> addOptional(ResourceLocation pLocation) {
         this.builder.addOptionalElement(pLocation);
         return this;
      }

      public TagsProvider.TagAppender<T> addTag(TagKey<T> pTag) {
         this.builder.addTag(pTag.location());
         return this;
      }

      public TagsProvider.TagAppender<T> addOptionalTag(ResourceLocation pLocation) {
         this.builder.addOptionalTag(pLocation);
         return this;
      }

      @SafeVarargs
      public final TagsProvider.TagAppender<T> add(T... pToAdd) {
         Stream.<T>of(pToAdd).map(this.registry::getKey).forEach((p_126587_) -> {
            this.builder.addElement(p_126587_);
         });
         return this;
      }

      public TagsProvider.TagAppender<T> add(TagEntry tag) {
          builder.add(tag);
          return this;
      }

      public TagBuilder getInternalBuilder() {
          return builder;
      }

      public String getModID() {
          return modId;
      }
   }
}
