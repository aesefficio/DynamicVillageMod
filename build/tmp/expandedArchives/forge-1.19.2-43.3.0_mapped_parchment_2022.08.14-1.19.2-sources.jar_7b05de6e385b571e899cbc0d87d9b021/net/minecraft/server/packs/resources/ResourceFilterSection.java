package net.minecraft.server.packs.resources;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.util.ExtraCodecs;
import org.slf4j.Logger;

public class ResourceFilterSection {
   static final Logger LOGGER = LogUtils.getLogger();
   static final Codec<ResourceFilterSection> CODEC = RecordCodecBuilder.create((p_215522_) -> {
      return p_215522_.group(Codec.list(ResourceFilterSection.ResourceLocationPattern.CODEC).fieldOf("block").forGetter((p_215520_) -> {
         return p_215520_.blockList;
      })).apply(p_215522_, ResourceFilterSection::new);
   });
   public static final MetadataSectionSerializer<ResourceFilterSection> SERIALIZER = new MetadataSectionSerializer<ResourceFilterSection>() {
      /**
       * The name of this section type as it appears in JSON.
       */
      public String getMetadataSectionName() {
         return "filter";
      }

      public ResourceFilterSection fromJson(JsonObject p_215538_) {
         return ResourceFilterSection.CODEC.parse(JsonOps.INSTANCE, p_215538_).getOrThrow(false, ResourceFilterSection.LOGGER::error);
      }
   };
   private final List<ResourceFilterSection.ResourceLocationPattern> blockList;

   public ResourceFilterSection(List<ResourceFilterSection.ResourceLocationPattern> p_215518_) {
      this.blockList = List.copyOf(p_215518_);
   }

   public boolean isNamespaceFiltered(String p_215524_) {
      return this.blockList.stream().anyMatch((p_215532_) -> {
         return p_215532_.namespacePredicate.test(p_215524_);
      });
   }

   public boolean isPathFiltered(String p_215529_) {
      return this.blockList.stream().anyMatch((p_215527_) -> {
         return p_215527_.pathPredicate.test(p_215529_);
      });
   }

   static class ResourceLocationPattern implements Predicate<ResourceLocation> {
      static final Codec<ResourceFilterSection.ResourceLocationPattern> CODEC = RecordCodecBuilder.create((p_215553_) -> {
         return p_215553_.group(ExtraCodecs.PATTERN.optionalFieldOf("namespace").forGetter((p_215557_) -> {
            return p_215557_.namespacePattern;
         }), ExtraCodecs.PATTERN.optionalFieldOf("path").forGetter((p_215551_) -> {
            return p_215551_.pathPattern;
         })).apply(p_215553_, ResourceFilterSection.ResourceLocationPattern::new);
      });
      private final Optional<Pattern> namespacePattern;
      final Predicate<String> namespacePredicate;
      private final Optional<Pattern> pathPattern;
      final Predicate<String> pathPredicate;

      private ResourceLocationPattern(Optional<Pattern> p_215546_, Optional<Pattern> p_215547_) {
         this.namespacePattern = p_215546_;
         this.namespacePredicate = p_215546_.map(Pattern::asPredicate).orElse((p_215559_) -> {
            return true;
         });
         this.pathPattern = p_215547_;
         this.pathPredicate = p_215547_.map(Pattern::asPredicate).orElse((p_215555_) -> {
            return true;
         });
      }

      public boolean test(ResourceLocation p_215549_) {
         return this.namespacePredicate.test(p_215549_.getNamespace()) && this.pathPredicate.test(p_215549_.getPath());
      }
   }
}