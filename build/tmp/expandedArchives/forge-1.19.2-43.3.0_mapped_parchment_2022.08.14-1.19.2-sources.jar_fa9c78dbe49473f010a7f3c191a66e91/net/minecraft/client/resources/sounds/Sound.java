package net.minecraft.client.resources.sounds;

import javax.annotation.Nullable;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.Weighted;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.SampledFloat;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class Sound implements Weighted<Sound> {
   private final ResourceLocation location;
   private final SampledFloat volume;
   private final SampledFloat pitch;
   private final int weight;
   private final Sound.Type type;
   private final boolean stream;
   private final boolean preload;
   private final int attenuationDistance;

   public Sound(String pPath, SampledFloat pVolume, SampledFloat pPitch, int pWeight, Sound.Type pType, boolean pStream, boolean pPreload, int pAttenuationDistance) {
      this.location = new ResourceLocation(pPath);
      this.volume = pVolume;
      this.pitch = pPitch;
      this.weight = pWeight;
      this.type = pType;
      this.stream = pStream;
      this.preload = pPreload;
      this.attenuationDistance = pAttenuationDistance;
   }

   public ResourceLocation getLocation() {
      return this.location;
   }

   public ResourceLocation getPath() {
      return new ResourceLocation(this.location.getNamespace(), "sounds/" + this.location.getPath() + ".ogg");
   }

   public SampledFloat getVolume() {
      return this.volume;
   }

   public SampledFloat getPitch() {
      return this.pitch;
   }

   public int getWeight() {
      return this.weight;
   }

   public Sound getSound(RandomSource p_235143_) {
      return this;
   }

   public void preloadIfRequired(SoundEngine pEngine) {
      if (this.preload) {
         pEngine.requestPreload(this);
      }

   }

   public Sound.Type getType() {
      return this.type;
   }

   public boolean shouldStream() {
      return this.stream;
   }

   public boolean shouldPreload() {
      return this.preload;
   }

   public int getAttenuationDistance() {
      return this.attenuationDistance;
   }

   public String toString() {
      return "Sound[" + this.location + "]";
   }

   @OnlyIn(Dist.CLIENT)
   public static enum Type {
      FILE("file"),
      SOUND_EVENT("event");

      private final String name;

      private Type(String pName) {
         this.name = pName;
      }

      @Nullable
      public static Sound.Type getByName(String pName) {
         for(Sound.Type sound$type : values()) {
            if (sound$type.name.equals(pName)) {
               return sound$type;
            }
         }

         return null;
      }
   }
}