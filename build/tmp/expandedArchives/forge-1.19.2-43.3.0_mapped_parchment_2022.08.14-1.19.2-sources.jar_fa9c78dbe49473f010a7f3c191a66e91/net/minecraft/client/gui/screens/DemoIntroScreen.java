package net.minecraft.client.gui.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DemoIntroScreen extends Screen {
   private static final ResourceLocation DEMO_BACKGROUND_LOCATION = new ResourceLocation("textures/gui/demo_background.png");
   private MultiLineLabel movementMessage = MultiLineLabel.EMPTY;
   private MultiLineLabel durationMessage = MultiLineLabel.EMPTY;

   public DemoIntroScreen() {
      super(Component.translatable("demo.help.title"));
   }

   protected void init() {
      int i = -16;
      this.addRenderableWidget(new Button(this.width / 2 - 116, this.height / 2 + 62 + -16, 114, 20, Component.translatable("demo.help.buy"), (p_95951_) -> {
         p_95951_.active = false;
         Util.getPlatform().openUri("https://aka.ms/BuyMinecraftJava");
      }));
      this.addRenderableWidget(new Button(this.width / 2 + 2, this.height / 2 + 62 + -16, 114, 20, Component.translatable("demo.help.later"), (p_95948_) -> {
         this.minecraft.setScreen((Screen)null);
         this.minecraft.mouseHandler.grabMouse();
      }));
      Options options = this.minecraft.options;
      this.movementMessage = MultiLineLabel.create(this.font, Component.translatable("demo.help.movementShort", options.keyUp.getTranslatedKeyMessage(), options.keyLeft.getTranslatedKeyMessage(), options.keyDown.getTranslatedKeyMessage(), options.keyRight.getTranslatedKeyMessage()), Component.translatable("demo.help.movementMouse"), Component.translatable("demo.help.jump", options.keyJump.getTranslatedKeyMessage()), Component.translatable("demo.help.inventory", options.keyInventory.getTranslatedKeyMessage()));
      this.durationMessage = MultiLineLabel.create(this.font, Component.translatable("demo.help.fullWrapped"), 218);
   }

   public void renderBackground(PoseStack pPoseStack) {
      super.renderBackground(pPoseStack);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.setShaderTexture(0, DEMO_BACKGROUND_LOCATION);
      int i = (this.width - 248) / 2;
      int j = (this.height - 166) / 2;
      this.blit(pPoseStack, i, j, 0, 0, 248, 166);
   }

   public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
      this.renderBackground(pPoseStack);
      int i = (this.width - 248) / 2 + 10;
      int j = (this.height - 166) / 2 + 8;
      this.font.draw(pPoseStack, this.title, (float)i, (float)j, 2039583);
      j = this.movementMessage.renderLeftAlignedNoShadow(pPoseStack, i, j + 12, 12, 5197647);
      this.durationMessage.renderLeftAlignedNoShadow(pPoseStack, i, j + 20, 9, 2039583);
      super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
   }
}