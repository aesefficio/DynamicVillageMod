package net.minecraft.client.renderer.texture;

import java.util.Collection;
import java.util.Locale;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class StitcherException extends RuntimeException {
   private final Collection<TextureAtlasSprite.Info> allSprites;

   public StitcherException(TextureAtlasSprite.Info pSpriteInfo, Collection<TextureAtlasSprite.Info> pAllSprites) {
      super(String.format(Locale.ROOT, "Unable to fit: %s - size: %dx%d - Maybe try a lower resolution resourcepack?", pSpriteInfo.name(), pSpriteInfo.width(), pSpriteInfo.height()));
      this.allSprites = pAllSprites;
   }

   public Collection<TextureAtlasSprite.Info> getAllSprites() {
      return this.allSprites;
   }
}