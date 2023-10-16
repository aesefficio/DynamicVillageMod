package net.minecraft.client.gui.font;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.providers.GlyphProviderBuilderType;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class FontManager implements AutoCloseable {
   static final Logger LOGGER = LogUtils.getLogger();
   private static final String FONTS_PATH = "fonts.json";
   public static final ResourceLocation MISSING_FONT = new ResourceLocation("minecraft", "missing");
   private final FontSet missingFontSet;
   final Map<ResourceLocation, FontSet> fontSets = Maps.newHashMap();
   final TextureManager textureManager;
   private Map<ResourceLocation, ResourceLocation> renames = ImmutableMap.of();
   private final PreparableReloadListener reloadListener = new SimplePreparableReloadListener<Map<ResourceLocation, List<GlyphProvider>>>() {
      /**
       * Performs any reloading that can be done off-thread, such as file IO
       */
      protected Map<ResourceLocation, List<GlyphProvider>> prepare(ResourceManager p_95024_, ProfilerFiller p_95025_) {
         p_95025_.startTick();
         Gson gson = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();
         Map<ResourceLocation, List<GlyphProvider>> map = Maps.newHashMap();

         for(Map.Entry<ResourceLocation, List<Resource>> entry : p_95024_.listResourceStacks("font", (p_232555_) -> {
            return p_232555_.getPath().endsWith(".json");
         }).entrySet()) {
            ResourceLocation resourcelocation = entry.getKey();
            String s = resourcelocation.getPath();
            ResourceLocation resourcelocation1 = new ResourceLocation(resourcelocation.getNamespace(), s.substring("font/".length(), s.length() - ".json".length()));
            List<GlyphProvider> list = map.computeIfAbsent(resourcelocation1, (p_95040_) -> {
               return Lists.newArrayList(new AllMissingGlyphProvider());
            });
            p_95025_.push(resourcelocation1::toString);

            for(Resource resource : entry.getValue()) {
               p_95025_.push(resource.sourcePackId());

               try {
                  Reader reader = resource.openAsReader();

                  try {
                     try {
                        p_95025_.push("reading");
                        JsonArray jsonarray = GsonHelper.getAsJsonArray(GsonHelper.fromJson(gson, reader, JsonObject.class), "providers");
                        p_95025_.popPush("parsing");

                        for(int i = jsonarray.size() - 1; i >= 0; --i) {
                           JsonObject jsonobject = GsonHelper.convertToJsonObject(jsonarray.get(i), "providers[" + i + "]");
                           String s1 = GsonHelper.getAsString(jsonobject, "type");
                           GlyphProviderBuilderType glyphproviderbuildertype = GlyphProviderBuilderType.byName(s1);

                           try {
                              p_95025_.push(s1);
                              GlyphProvider glyphprovider = glyphproviderbuildertype.create(jsonobject).create(p_95024_);
                              if (glyphprovider != null) {
                                 list.add(glyphprovider);
                              }
                           } finally {
                              p_95025_.pop();
                           }
                        }
                     } finally {
                        p_95025_.pop();
                     }
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
                  FontManager.LOGGER.warn("Unable to load font '{}' in {} in resourcepack: '{}'", resourcelocation1, "fonts.json", resource.sourcePackId(), exception);
               }

               p_95025_.pop();
            }

            p_95025_.push("caching");
            IntSet intset = new IntOpenHashSet();

            for(GlyphProvider glyphprovider1 : list) {
               intset.addAll(glyphprovider1.getSupportedGlyphs());
            }

            intset.forEach((int p_95034_) -> {
               if (p_95034_ != 32) {
                  for(GlyphProvider glyphprovider2 : Lists.reverse(list)) {
                     if (glyphprovider2.getGlyph(p_95034_) != null) {
                        break;
                     }
                  }

               }
            });
            p_95025_.pop();
            p_95025_.pop();
         }

         p_95025_.endTick();
         return map;
      }

      protected void apply(Map<ResourceLocation, List<GlyphProvider>> p_95036_, ResourceManager p_95037_, ProfilerFiller p_95038_) {
         p_95038_.startTick();
         p_95038_.push("closing");
         FontManager.this.fontSets.values().forEach(FontSet::close);
         FontManager.this.fontSets.clear();
         p_95038_.popPush("reloading");
         p_95036_.forEach((p_95042_, p_95043_) -> {
            FontSet fontset = new FontSet(FontManager.this.textureManager, p_95042_);
            fontset.reload(Lists.reverse(p_95043_));
            FontManager.this.fontSets.put(p_95042_, fontset);
         });
         p_95038_.pop();
         p_95038_.endTick();
      }

      public String getName() {
         return "FontManager";
      }
   };

   public FontManager(TextureManager pTextureManager) {
      this.textureManager = pTextureManager;
      this.missingFontSet = Util.make(new FontSet(pTextureManager, MISSING_FONT), (p_95010_) -> {
         p_95010_.reload(Lists.newArrayList(new AllMissingGlyphProvider()));
      });
   }

   public void setRenames(Map<ResourceLocation, ResourceLocation> pUnicodeForcedMap) {
      this.renames = pUnicodeForcedMap;
   }

   public Font createFont() {
      return new Font((p_95014_) -> {
         return this.fontSets.getOrDefault(this.renames.getOrDefault(p_95014_, p_95014_), this.missingFontSet);
      }, false);
   }

   public Font createFontFilterFishy() {
      return new Font((p_243145_) -> {
         return this.fontSets.getOrDefault(this.renames.getOrDefault(p_243145_, p_243145_), this.missingFontSet);
      }, true);
   }

   public PreparableReloadListener getReloadListener() {
      return this.reloadListener;
   }

   public void close() {
      this.fontSets.values().forEach(FontSet::close);
      this.missingFontSet.close();
   }
}