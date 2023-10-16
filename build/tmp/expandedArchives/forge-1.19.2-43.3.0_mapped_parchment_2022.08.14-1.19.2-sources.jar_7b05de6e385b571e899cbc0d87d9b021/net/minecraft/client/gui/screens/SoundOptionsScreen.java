package net.minecraft.client.gui.screens;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.TooltipAccessor;
import net.minecraft.client.gui.components.VolumeSlider;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SoundOptionsScreen extends OptionsSubScreen {
   @Nullable
   private AbstractWidget directionalAudioButton;

   public SoundOptionsScreen(Screen pLastScreen, Options pOptions) {
      super(pLastScreen, pOptions, Component.translatable("options.sounds.title"));
   }

   protected void init() {
      int i = this.height / 6 - 12;
      int j = 22;
      int k = 0;
      this.addRenderableWidget(new VolumeSlider(this.minecraft, this.width / 2 - 155 + k % 2 * 160, i + 22 * (k >> 1), SoundSource.MASTER, 310));
      k += 2;

      for(SoundSource soundsource : SoundSource.values()) {
         if (soundsource != SoundSource.MASTER) {
            this.addRenderableWidget(new VolumeSlider(this.minecraft, this.width / 2 - 155 + k % 2 * 160, i + 22 * (k >> 1), soundsource, 150));
            ++k;
         }
      }

      if (k % 2 == 1) {
         ++k;
      }

      this.addRenderableWidget(this.options.soundDevice().createButton(this.options, this.width / 2 - 155, i + 22 * (k >> 1), 310));
      k += 2;
      this.addRenderableWidget(this.options.showSubtitles().createButton(this.options, this.width / 2 - 155, i + 22 * (k >> 1), 150));
      this.directionalAudioButton = this.options.directionalAudio().createButton(this.options, this.width / 2 + 5, i + 22 * (k >> 1), 150);
      this.addRenderableWidget(this.directionalAudioButton);
      k += 2;
      this.addRenderableWidget(new Button(this.width / 2 - 100, i + 22 * (k >> 1), 200, 20, CommonComponents.GUI_DONE, (p_96713_) -> {
         this.minecraft.setScreen(this.lastScreen);
      }));
   }

   public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
      this.renderBackground(pPoseStack);
      drawCenteredString(pPoseStack, this.font, this.title, this.width / 2, 15, 16777215);
      super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
      if (this.directionalAudioButton != null && this.directionalAudioButton.isMouseOver((double)pMouseX, (double)pMouseY)) {
         List<FormattedCharSequence> list = ((TooltipAccessor)this.directionalAudioButton).getTooltip();
         this.renderTooltip(pPoseStack, list, pMouseX, pMouseY);
      }

   }
}