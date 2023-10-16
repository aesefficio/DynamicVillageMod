package net.minecraft.client.sounds;

import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WeighedSoundEvents implements Weighted<Sound> {
   private final List<Weighted<Sound>> list = Lists.newArrayList();
   private final RandomSource random = RandomSource.create();
   private final ResourceLocation location;
   @Nullable
   private final Component subtitle;

   public WeighedSoundEvents(ResourceLocation pLocation, @Nullable String pSubtitleKey) {
      this.location = pLocation;
      this.subtitle = pSubtitleKey == null ? null : Component.translatable(pSubtitleKey);
   }

   public int getWeight() {
      int i = 0;

      for(Weighted<Sound> weighted : this.list) {
         i += weighted.getWeight();
      }

      return i;
   }

   public Sound getSound(RandomSource p_235265_) {
      int i = this.getWeight();
      if (!this.list.isEmpty() && i != 0) {
         int j = p_235265_.nextInt(i);

         for(Weighted<Sound> weighted : this.list) {
            j -= weighted.getWeight();
            if (j < 0) {
               return weighted.getSound(p_235265_);
            }
         }

         return SoundManager.EMPTY_SOUND;
      } else {
         return SoundManager.EMPTY_SOUND;
      }
   }

   public void addSound(Weighted<Sound> pAccessor) {
      this.list.add(pAccessor);
   }

   public ResourceLocation getResourceLocation() {
      return this.location;
   }

   @Nullable
   public Component getSubtitle() {
      return this.subtitle;
   }

   public void preloadIfRequired(SoundEngine pEngine) {
      for(Weighted<Sound> weighted : this.list) {
         weighted.preloadIfRequired(pEngine);
      }

   }
}