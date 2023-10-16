package net.minecraft.client.gui.font.providers;

import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.font.SpaceProvider;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum GlyphProviderBuilderType {
   BITMAP("bitmap", BitmapProvider.Builder::fromJson),
   TTF("ttf", TrueTypeGlyphProviderBuilder::fromJson),
   SPACE("space", SpaceProvider::builderFromJson),
   LEGACY_UNICODE("legacy_unicode", LegacyUnicodeBitmapsProvider.Builder::fromJson);

   private static final Map<String, GlyphProviderBuilderType> BY_NAME = Util.make(Maps.newHashMap(), (p_95418_) -> {
      for(GlyphProviderBuilderType glyphproviderbuildertype : values()) {
         p_95418_.put(glyphproviderbuildertype.name, glyphproviderbuildertype);
      }

   });
   private final String name;
   private final Function<JsonObject, GlyphProviderBuilder> factory;

   private GlyphProviderBuilderType(String pName, Function<JsonObject, GlyphProviderBuilder> pFactory) {
      this.name = pName;
      this.factory = pFactory;
   }

   public static GlyphProviderBuilderType byName(String pType) {
      GlyphProviderBuilderType glyphproviderbuildertype = BY_NAME.get(pType);
      if (glyphproviderbuildertype == null) {
         throw new IllegalArgumentException("Invalid type: " + pType);
      } else {
         return glyphproviderbuildertype;
      }
   }

   public GlyphProviderBuilder create(JsonObject pJson) {
      return this.factory.apply(pJson);
   }
}