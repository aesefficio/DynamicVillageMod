package net.minecraft.client.gui.screens;

import net.minecraft.Util;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AccessibilityOptionsScreen extends SimpleOptionsSubScreen {
   private static final String GUIDE_LINK = "https://aka.ms/MinecraftJavaAccessibility";

   private static OptionInstance<?>[] options(Options p_232691_) {
      return new OptionInstance[]{p_232691_.narrator(), p_232691_.showSubtitles(), p_232691_.textBackgroundOpacity(), p_232691_.backgroundForChatOnly(), p_232691_.chatOpacity(), p_232691_.chatLineSpacing(), p_232691_.chatDelay(), p_232691_.autoJump(), p_232691_.toggleCrouch(), p_232691_.toggleSprint(), p_232691_.screenEffectScale(), p_232691_.fovEffectScale(), p_232691_.darkMojangStudiosBackground(), p_232691_.hideLightningFlash(), p_232691_.darknessEffectScale()};
   }

   public AccessibilityOptionsScreen(Screen pLastScreen, Options pOptions) {
      super(pLastScreen, pOptions, Component.translatable("options.accessibility.title"), options(pOptions));
   }

   protected void createFooter() {
      this.addRenderableWidget(new Button(this.width / 2 - 155, this.height - 27, 150, 20, Component.translatable("options.accessibility.link"), (p_95509_) -> {
         this.minecraft.setScreen(new ConfirmLinkScreen((p_169232_) -> {
            if (p_169232_) {
               Util.getPlatform().openUri("https://aka.ms/MinecraftJavaAccessibility");
            }

            this.minecraft.setScreen(this);
         }, "https://aka.ms/MinecraftJavaAccessibility", true));
      }));
      this.addRenderableWidget(new Button(this.width / 2 + 5, this.height - 27, 150, 20, CommonComponents.GUI_DONE, (p_95507_) -> {
         this.minecraft.setScreen(this.lastScreen);
      }));
   }
}