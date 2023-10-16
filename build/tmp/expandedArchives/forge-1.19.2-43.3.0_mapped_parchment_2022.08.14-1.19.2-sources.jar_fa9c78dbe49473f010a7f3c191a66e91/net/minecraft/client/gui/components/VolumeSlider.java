package net.minecraft.client.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class VolumeSlider extends AbstractOptionSliderButton {
   private final SoundSource source;

   public VolumeSlider(Minecraft pMinecraft, int pX, int pY, SoundSource pSource, int pWidth) {
      super(pMinecraft.options, pX, pY, pWidth, 20, (double)pMinecraft.options.getSoundSourceVolume(pSource));
      this.source = pSource;
      this.updateMessage();
   }

   protected void updateMessage() {
      Component component = (Component)((float)this.value == (float)this.getYImage(false) ? CommonComponents.OPTION_OFF : Component.literal((int)(this.value * 100.0D) + "%"));
      this.setMessage(Component.translatable("soundCategory." + this.source.getName()).append(": ").append(component));
   }

   protected void applyValue() {
      this.options.setSoundCategoryVolume(this.source, (float)this.value);
      this.options.save();
   }
}