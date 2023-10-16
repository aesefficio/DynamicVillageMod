package com.mojang.blaze3d.font;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import it.unimi.dsi.fastutil.ints.Int2FloatMap;
import it.unimi.dsi.fastutil.ints.Int2FloatMaps;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import java.util.Arrays;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.gui.font.providers.GlyphProviderBuilder;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SpaceProvider implements GlyphProvider {
   private final Int2ObjectMap<GlyphInfo.SpaceGlyphInfo> glyphs;

   public SpaceProvider(Int2FloatMap p_231100_) {
      this.glyphs = new Int2ObjectOpenHashMap<>(p_231100_.size());
      Int2FloatMaps.fastForEach(p_231100_, (p_231109_) -> {
         float f = p_231109_.getFloatValue();
         this.glyphs.put(p_231109_.getIntKey(), () -> {
            return f;
         });
      });
   }

   @Nullable
   public GlyphInfo getGlyph(int p_231105_) {
      return this.glyphs.get(p_231105_);
   }

   public IntSet getSupportedGlyphs() {
      return IntSets.unmodifiable(this.glyphs.keySet());
   }

   public static GlyphProviderBuilder builderFromJson(JsonObject p_231107_) {
      Int2FloatMap int2floatmap = new Int2FloatOpenHashMap();
      JsonObject jsonobject = GsonHelper.getAsJsonObject(p_231107_, "advances");

      for(Map.Entry<String, JsonElement> entry : jsonobject.entrySet()) {
         int[] aint = entry.getKey().codePoints().toArray();
         if (aint.length != 1) {
            throw new JsonParseException("Expected single codepoint, got " + Arrays.toString(aint));
         }

         float f = GsonHelper.convertToFloat(entry.getValue(), "advance");
         int2floatmap.put(aint[0], f);
      }

      return (p_231112_) -> {
         return new SpaceProvider(int2floatmap);
      };
   }
}