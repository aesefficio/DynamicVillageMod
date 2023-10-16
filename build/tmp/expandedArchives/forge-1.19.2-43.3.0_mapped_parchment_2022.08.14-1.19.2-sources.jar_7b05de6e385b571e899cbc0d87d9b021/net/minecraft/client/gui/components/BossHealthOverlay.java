package net.minecraft.client.gui.components;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.BossEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BossHealthOverlay extends GuiComponent {
   private static final ResourceLocation GUI_BARS_LOCATION = new ResourceLocation("textures/gui/bars.png");
   private static final int BAR_WIDTH = 182;
   private static final int BAR_HEIGHT = 5;
   private static final int OVERLAY_OFFSET = 80;
   private final Minecraft minecraft;
   final Map<UUID, LerpingBossEvent> events = Maps.newLinkedHashMap();

   public BossHealthOverlay(Minecraft pMinecraft) {
      this.minecraft = pMinecraft;
   }

   public void render(PoseStack pPoseStack) {
      if (!this.events.isEmpty()) {
         int i = this.minecraft.getWindow().getGuiScaledWidth();
         int j = 12;

         for(LerpingBossEvent lerpingbossevent : this.events.values()) {
            int k = i / 2 - 91;
            var event = net.minecraftforge.client.ForgeHooksClient.onCustomizeBossEventProgress(pPoseStack, this.minecraft.getWindow(), lerpingbossevent, k, j, 10 + this.minecraft.font.lineHeight);
            if (!event.isCanceled()) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShaderTexture(0, GUI_BARS_LOCATION);
            this.drawBar(pPoseStack, k, j, lerpingbossevent);
            Component component = lerpingbossevent.getName();
            int l = this.minecraft.font.width(component);
            int i1 = i / 2 - l / 2;
            int j1 = j - 9;
            this.minecraft.font.drawShadow(pPoseStack, component, (float)i1, (float)j1, 16777215);
            }
            j += event.getIncrement();
            if (j >= this.minecraft.getWindow().getGuiScaledHeight() / 3) {
               break;
            }
         }

      }
   }

   private void drawBar(PoseStack pPoseStack, int pX, int pY, BossEvent pBossEvent) {
      this.drawBar(pPoseStack, pX, pY, pBossEvent, 182, 0);
      int i = (int)(pBossEvent.getProgress() * 183.0F);
      if (i > 0) {
         this.drawBar(pPoseStack, pX, pY, pBossEvent, i, 5);
      }

   }

   private void drawBar(PoseStack p_232470_, int p_232471_, int p_232472_, BossEvent p_232473_, int p_232474_, int p_232475_) {
      this.blit(p_232470_, p_232471_, p_232472_, 0, p_232473_.getColor().ordinal() * 5 * 2 + p_232475_, p_232474_, 5);
      if (p_232473_.getOverlay() != BossEvent.BossBarOverlay.PROGRESS) {
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         this.blit(p_232470_, p_232471_, p_232472_, 0, 80 + (p_232473_.getOverlay().ordinal() - 1) * 5 * 2 + p_232475_, p_232474_, 5);
         RenderSystem.disableBlend();
      }

   }

   public void update(ClientboundBossEventPacket pPacket) {
      pPacket.dispatch(new ClientboundBossEventPacket.Handler() {
         public void add(UUID p_168824_, Component p_168825_, float p_168826_, BossEvent.BossBarColor p_168827_, BossEvent.BossBarOverlay p_168828_, boolean p_168829_, boolean p_168830_, boolean p_168831_) {
            BossHealthOverlay.this.events.put(p_168824_, new LerpingBossEvent(p_168824_, p_168825_, p_168826_, p_168827_, p_168828_, p_168829_, p_168830_, p_168831_));
         }

         public void remove(UUID p_168812_) {
            BossHealthOverlay.this.events.remove(p_168812_);
         }

         public void updateProgress(UUID p_168814_, float p_168815_) {
            BossHealthOverlay.this.events.get(p_168814_).setProgress(p_168815_);
         }

         public void updateName(UUID p_168821_, Component p_168822_) {
            BossHealthOverlay.this.events.get(p_168821_).setName(p_168822_);
         }

         public void updateStyle(UUID p_168817_, BossEvent.BossBarColor p_168818_, BossEvent.BossBarOverlay p_168819_) {
            LerpingBossEvent lerpingbossevent = BossHealthOverlay.this.events.get(p_168817_);
            lerpingbossevent.setColor(p_168818_);
            lerpingbossevent.setOverlay(p_168819_);
         }

         public void updateProperties(UUID p_168833_, boolean p_168834_, boolean p_168835_, boolean p_168836_) {
            LerpingBossEvent lerpingbossevent = BossHealthOverlay.this.events.get(p_168833_);
            lerpingbossevent.setDarkenScreen(p_168834_);
            lerpingbossevent.setPlayBossMusic(p_168835_);
            lerpingbossevent.setCreateWorldFog(p_168836_);
         }
      });
   }

   public void reset() {
      this.events.clear();
   }

   public boolean shouldPlayMusic() {
      if (!this.events.isEmpty()) {
         for(BossEvent bossevent : this.events.values()) {
            if (bossevent.shouldPlayBossMusic()) {
               return true;
            }
         }
      }

      return false;
   }

   public boolean shouldDarkenScreen() {
      if (!this.events.isEmpty()) {
         for(BossEvent bossevent : this.events.values()) {
            if (bossevent.shouldDarkenScreen()) {
               return true;
            }
         }
      }

      return false;
   }

   public boolean shouldCreateWorldFog() {
      if (!this.events.isEmpty()) {
         for(BossEvent bossevent : this.events.values()) {
            if (bossevent.shouldCreateWorldFog()) {
               return true;
            }
         }
      }

      return false;
   }
}
