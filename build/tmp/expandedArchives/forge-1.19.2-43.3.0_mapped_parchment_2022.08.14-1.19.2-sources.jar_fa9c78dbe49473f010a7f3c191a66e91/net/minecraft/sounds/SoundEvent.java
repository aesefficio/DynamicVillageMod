package net.minecraft.sounds;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;

public class SoundEvent {
   public static final Codec<SoundEvent> CODEC = ResourceLocation.CODEC.xmap(SoundEvent::new, (p_11662_) -> {
      return p_11662_.location;
   });
   private final ResourceLocation location;
   private final float range;
   private final boolean newSystem;

   public SoundEvent(ResourceLocation p_11659_) {
      this(p_11659_, 16.0F, false);
   }

   public SoundEvent(ResourceLocation p_215662_, float p_215663_) {
      this(p_215662_, p_215663_, true);
   }

   private SoundEvent(ResourceLocation p_215665_, float p_215666_, boolean p_215667_) {
      this.location = p_215665_;
      this.range = p_215666_;
      this.newSystem = p_215667_;
   }

   public ResourceLocation getLocation() {
      return this.location;
   }

   public float getRange(float p_215669_) {
      if (this.newSystem) {
         return this.range;
      } else {
         return p_215669_ > 1.0F ? 16.0F * p_215669_ : 16.0F;
      }
   }
}