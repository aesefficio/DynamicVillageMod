package com.mojang.realmsclient.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsWorldOptions;
import com.mojang.realmsclient.util.RealmsTextureManager;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RealmsWorldSlotButton extends Button {
   public static final ResourceLocation SLOT_FRAME_LOCATION = new ResourceLocation("realms", "textures/gui/realms/slot_frame.png");
   public static final ResourceLocation EMPTY_SLOT_LOCATION = new ResourceLocation("realms", "textures/gui/realms/empty_frame.png");
   public static final ResourceLocation CHECK_MARK_LOCATION = new ResourceLocation("realms", "textures/gui/realms/checkmark.png");
   public static final ResourceLocation DEFAULT_WORLD_SLOT_1 = new ResourceLocation("minecraft", "textures/gui/title/background/panorama_0.png");
   public static final ResourceLocation DEFAULT_WORLD_SLOT_2 = new ResourceLocation("minecraft", "textures/gui/title/background/panorama_2.png");
   public static final ResourceLocation DEFAULT_WORLD_SLOT_3 = new ResourceLocation("minecraft", "textures/gui/title/background/panorama_3.png");
   private static final Component SLOT_ACTIVE_TOOLTIP = Component.translatable("mco.configure.world.slot.tooltip.active");
   private static final Component SWITCH_TO_MINIGAME_SLOT_TOOLTIP = Component.translatable("mco.configure.world.slot.tooltip.minigame");
   private static final Component SWITCH_TO_WORLD_SLOT_TOOLTIP = Component.translatable("mco.configure.world.slot.tooltip");
   private final Supplier<RealmsServer> serverDataProvider;
   private final Consumer<Component> toolTipSetter;
   private final int slotIndex;
   @Nullable
   private RealmsWorldSlotButton.State state;

   public RealmsWorldSlotButton(int pX, int pY, int pWidth, int pHeight, Supplier<RealmsServer> pServerDataProvider, Consumer<Component> pToolTipSetter, int pSlotIndex, Button.OnPress pOnPress) {
      super(pX, pY, pWidth, pHeight, CommonComponents.EMPTY, pOnPress);
      this.serverDataProvider = pServerDataProvider;
      this.slotIndex = pSlotIndex;
      this.toolTipSetter = pToolTipSetter;
   }

   @Nullable
   public RealmsWorldSlotButton.State getState() {
      return this.state;
   }

   public void tick() {
      RealmsServer realmsserver = this.serverDataProvider.get();
      if (realmsserver != null) {
         RealmsWorldOptions realmsworldoptions = realmsserver.slots.get(this.slotIndex);
         boolean flag2 = this.slotIndex == 4;
         boolean flag;
         String s;
         long i;
         String s1;
         boolean flag1;
         if (flag2) {
            flag = realmsserver.worldType == RealmsServer.WorldType.MINIGAME;
            s = "Minigame";
            i = (long)realmsserver.minigameId;
            s1 = realmsserver.minigameImage;
            flag1 = realmsserver.minigameId == -1;
         } else {
            flag = realmsserver.activeSlot == this.slotIndex && realmsserver.worldType != RealmsServer.WorldType.MINIGAME;
            s = realmsworldoptions.getSlotName(this.slotIndex);
            i = realmsworldoptions.templateId;
            s1 = realmsworldoptions.templateImage;
            flag1 = realmsworldoptions.empty;
         }

         RealmsWorldSlotButton.Action realmsworldslotbutton$action = getAction(realmsserver, flag, flag2);
         Pair<Component, Component> pair = this.getTooltipAndNarration(realmsserver, s, flag1, flag2, realmsworldslotbutton$action);
         this.state = new RealmsWorldSlotButton.State(flag, s, i, s1, flag1, flag2, realmsworldslotbutton$action, pair.getFirst());
         this.setMessage(pair.getSecond());
      }
   }

   private static RealmsWorldSlotButton.Action getAction(RealmsServer pRealmsServer, boolean p_87961_, boolean p_87962_) {
      if (p_87961_) {
         if (!pRealmsServer.expired && pRealmsServer.state != RealmsServer.State.UNINITIALIZED) {
            return RealmsWorldSlotButton.Action.JOIN;
         }
      } else {
         if (!p_87962_) {
            return RealmsWorldSlotButton.Action.SWITCH_SLOT;
         }

         if (!pRealmsServer.expired) {
            return RealmsWorldSlotButton.Action.SWITCH_SLOT;
         }
      }

      return RealmsWorldSlotButton.Action.NOTHING;
   }

   private Pair<Component, Component> getTooltipAndNarration(RealmsServer pRealmsServer, String p_87955_, boolean p_87956_, boolean p_87957_, RealmsWorldSlotButton.Action p_87958_) {
      if (p_87958_ == RealmsWorldSlotButton.Action.NOTHING) {
         return Pair.of((Component)null, Component.literal(p_87955_));
      } else {
         Component component;
         if (p_87957_) {
            if (p_87956_) {
               component = CommonComponents.EMPTY;
            } else {
               component = Component.literal(" ").append(p_87955_).append(" ").append(pRealmsServer.minigameName);
            }
         } else {
            component = Component.literal(" ").append(p_87955_);
         }

         Component component1;
         if (p_87958_ == RealmsWorldSlotButton.Action.JOIN) {
            component1 = SLOT_ACTIVE_TOOLTIP;
         } else {
            component1 = p_87957_ ? SWITCH_TO_MINIGAME_SLOT_TOOLTIP : SWITCH_TO_WORLD_SLOT_TOOLTIP;
         }

         Component component2 = component1.copy().append(component);
         return Pair.of(component1, component2);
      }
   }

   public void renderButton(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
      if (this.state != null) {
         this.drawSlotFrame(pPoseStack, this.x, this.y, pMouseX, pMouseY, this.state.isCurrentlyActiveSlot, this.state.slotName, this.slotIndex, this.state.imageId, this.state.image, this.state.empty, this.state.minigame, this.state.action, this.state.actionPrompt);
      }
   }

   private void drawSlotFrame(PoseStack pPoseStack, int pX, int pY, int p_87942_, int p_87943_, boolean p_87944_, String pText, int p_87946_, long p_87947_, @Nullable String p_87948_, boolean p_87949_, boolean p_87950_, RealmsWorldSlotButton.Action p_87951_, @Nullable Component p_87952_) {
      boolean flag = this.isHoveredOrFocused();
      if (this.isMouseOver((double)p_87942_, (double)p_87943_) && p_87952_ != null) {
         this.toolTipSetter.accept(p_87952_);
      }

      Minecraft minecraft = Minecraft.getInstance();
      if (p_87950_) {
         RealmsTextureManager.bindWorldTemplate(String.valueOf(p_87947_), p_87948_);
      } else if (p_87949_) {
         RenderSystem.setShaderTexture(0, EMPTY_SLOT_LOCATION);
      } else if (p_87948_ != null && p_87947_ != -1L) {
         RealmsTextureManager.bindWorldTemplate(String.valueOf(p_87947_), p_87948_);
      } else if (p_87946_ == 1) {
         RenderSystem.setShaderTexture(0, DEFAULT_WORLD_SLOT_1);
      } else if (p_87946_ == 2) {
         RenderSystem.setShaderTexture(0, DEFAULT_WORLD_SLOT_2);
      } else if (p_87946_ == 3) {
         RenderSystem.setShaderTexture(0, DEFAULT_WORLD_SLOT_3);
      }

      if (p_87944_) {
         RenderSystem.setShaderColor(0.56F, 0.56F, 0.56F, 1.0F);
      } else {
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      }

      blit(pPoseStack, pX + 3, pY + 3, 0.0F, 0.0F, 74, 74, 74, 74);
      RenderSystem.setShaderTexture(0, SLOT_FRAME_LOCATION);
      boolean flag1 = flag && p_87951_ != RealmsWorldSlotButton.Action.NOTHING;
      if (flag1) {
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      } else if (p_87944_) {
         RenderSystem.setShaderColor(0.8F, 0.8F, 0.8F, 1.0F);
      } else {
         RenderSystem.setShaderColor(0.56F, 0.56F, 0.56F, 1.0F);
      }

      blit(pPoseStack, pX, pY, 0.0F, 0.0F, 80, 80, 80, 80);
      if (p_87944_) {
         this.renderCheckMark(pPoseStack, pX, pY);
      }

      drawCenteredString(pPoseStack, minecraft.font, pText, pX + 40, pY + 66, 16777215);
   }

   private void renderCheckMark(PoseStack p_231299_, int p_231300_, int p_231301_) {
      RenderSystem.setShaderTexture(0, CHECK_MARK_LOCATION);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      blit(p_231299_, p_231300_ + 67, p_231301_ + 4, 0.0F, 0.0F, 9, 8, 9, 8);
      RenderSystem.disableBlend();
   }

   @OnlyIn(Dist.CLIENT)
   public static enum Action {
      NOTHING,
      SWITCH_SLOT,
      JOIN;
   }

   @OnlyIn(Dist.CLIENT)
   public static class State {
      final boolean isCurrentlyActiveSlot;
      final String slotName;
      final long imageId;
      @Nullable
      final String image;
      public final boolean empty;
      public final boolean minigame;
      public final RealmsWorldSlotButton.Action action;
      @Nullable
      final Component actionPrompt;

      State(boolean pIsCurrentlyActiveSlot, String pSlotName, long pImageId, @Nullable String pImage, boolean pEmpty, boolean pMinigame, RealmsWorldSlotButton.Action pAction, @Nullable Component pActionPrompt) {
         this.isCurrentlyActiveSlot = pIsCurrentlyActiveSlot;
         this.slotName = pSlotName;
         this.imageId = pImageId;
         this.image = pImage;
         this.empty = pEmpty;
         this.minigame = pMinigame;
         this.action = pAction;
         this.actionPrompt = pActionPrompt;
      }
   }
}