package net.minecraft.client.renderer.texture;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AtlasSet implements AutoCloseable {
   private final Map<ResourceLocation, TextureAtlas> atlases;

   public AtlasSet(Collection<TextureAtlas> pAtlases) {
      this.atlases = pAtlases.stream().collect(Collectors.toMap(TextureAtlas::location, Function.identity()));
   }

   public TextureAtlas getAtlas(ResourceLocation pLocation) {
      return this.atlases.get(pLocation);
   }

   public TextureAtlasSprite getSprite(Material pMaterial) {
      return this.atlases.get(pMaterial.atlasLocation()).getSprite(pMaterial.texture());
   }

   public void close() {
      this.atlases.values().forEach(TextureAtlas::clearTextureData);
      this.atlases.clear();
   }
}