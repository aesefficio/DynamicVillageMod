package net.minecraft.client.gui.components.toasts;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SystemToast implements Toast {
   private static final int MAX_LINE_SIZE = 200;
   private static final int LINE_SPACING = 12;
   private static final int MARGIN = 10;
   private final SystemToast.SystemToastIds id;
   private Component title;
   private List<FormattedCharSequence> messageLines;
   private long lastChanged;
   private boolean changed;
   private final int width;

   public SystemToast(SystemToast.SystemToastIds pId, Component pTitle, @Nullable Component pMessage) {
      this(pId, pTitle, nullToEmpty(pMessage), Math.max(160, 30 + Math.max(Minecraft.getInstance().font.width(pTitle), pMessage == null ? 0 : Minecraft.getInstance().font.width(pMessage))));
   }

   public static SystemToast multiline(Minecraft pMinecraft, SystemToast.SystemToastIds pId, Component pTitle, Component pMessage) {
      Font font = pMinecraft.font;
      List<FormattedCharSequence> list = font.split(pMessage, 200);
      int i = Math.max(200, list.stream().mapToInt(font::width).max().orElse(200));
      return new SystemToast(pId, pTitle, list, i + 30);
   }

   private SystemToast(SystemToast.SystemToastIds pId, Component pTitle, List<FormattedCharSequence> pMessageLines, int pWidth) {
      this.id = pId;
      this.title = pTitle;
      this.messageLines = pMessageLines;
      this.width = pWidth;
   }

   private static ImmutableList<FormattedCharSequence> nullToEmpty(@Nullable Component pMessage) {
      return pMessage == null ? ImmutableList.of() : ImmutableList.of(pMessage.getVisualOrderText());
   }

   public int width() {
      return this.width;
   }

   public int height() {
      return 20 + this.messageLines.size() * 12;
   }

   /**
    * 
    * @param pTimeSinceLastVisible time in milliseconds
    */
   public Toast.Visibility render(PoseStack pPoseStack, ToastComponent pToastComponent, long pTimeSinceLastVisible) {
      if (this.changed) {
         this.lastChanged = pTimeSinceLastVisible;
         this.changed = false;
      }

      RenderSystem.setShaderTexture(0, TEXTURE);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      int i = this.width();
      if (i == 160 && this.messageLines.size() <= 1) {
         pToastComponent.blit(pPoseStack, 0, 0, 0, 64, i, this.height());
      } else {
         int j = this.height();
         int k = 28;
         int l = Math.min(4, j - 28);
         this.renderBackgroundRow(pPoseStack, pToastComponent, i, 0, 0, 28);

         for(int i1 = 28; i1 < j - l; i1 += 10) {
            this.renderBackgroundRow(pPoseStack, pToastComponent, i, 16, i1, Math.min(16, j - i1 - l));
         }

         this.renderBackgroundRow(pPoseStack, pToastComponent, i, 32 - l, j - l, l);
      }

      if (this.messageLines == null) {
         pToastComponent.getMinecraft().font.draw(pPoseStack, this.title, 18.0F, 12.0F, -256);
      } else {
         pToastComponent.getMinecraft().font.draw(pPoseStack, this.title, 18.0F, 7.0F, -256);

         for(int j1 = 0; j1 < this.messageLines.size(); ++j1) {
            pToastComponent.getMinecraft().font.draw(pPoseStack, this.messageLines.get(j1), 18.0F, (float)(18 + j1 * 12), -1);
         }
      }

      return pTimeSinceLastVisible - this.lastChanged < this.id.displayTime ? Toast.Visibility.SHOW : Toast.Visibility.HIDE;
   }

   private void renderBackgroundRow(PoseStack pPoseStack, ToastComponent pToastComponent, int p_94839_, int p_94840_, int p_94841_, int p_94842_) {
      int i = p_94840_ == 0 ? 20 : 5;
      int j = Math.min(60, p_94839_ - i);
      pToastComponent.blit(pPoseStack, 0, p_94841_, 0, 64 + p_94840_, i, p_94842_);

      for(int k = i; k < p_94839_ - j; k += 64) {
         pToastComponent.blit(pPoseStack, k, p_94841_, 32, 64 + p_94840_, Math.min(64, p_94839_ - k - j), p_94842_);
      }

      pToastComponent.blit(pPoseStack, p_94839_ - j, p_94841_, 160 - j, 64 + p_94840_, j, p_94842_);
   }

   public void reset(Component pTitle, @Nullable Component pMessage) {
      this.title = pTitle;
      this.messageLines = nullToEmpty(pMessage);
      this.changed = true;
   }

   public SystemToast.SystemToastIds getToken() {
      return this.id;
   }

   public static void add(ToastComponent pToastComponent, SystemToast.SystemToastIds pId, Component pTitle, @Nullable Component pMessage) {
      pToastComponent.addToast(new SystemToast(pId, pTitle, pMessage));
   }

   public static void addOrUpdate(ToastComponent pToastComponent, SystemToast.SystemToastIds pId, Component pTitle, @Nullable Component pMessage) {
      SystemToast systemtoast = pToastComponent.getToast(SystemToast.class, pId);
      if (systemtoast == null) {
         add(pToastComponent, pId, pTitle, pMessage);
      } else {
         systemtoast.reset(pTitle, pMessage);
      }

   }

   public static void onWorldAccessFailure(Minecraft pMinecraft, String pMessage) {
      add(pMinecraft.getToasts(), SystemToast.SystemToastIds.WORLD_ACCESS_FAILURE, Component.translatable("selectWorld.access_failure"), Component.literal(pMessage));
   }

   public static void onWorldDeleteFailure(Minecraft pMinecraft, String pMessage) {
      add(pMinecraft.getToasts(), SystemToast.SystemToastIds.WORLD_ACCESS_FAILURE, Component.translatable("selectWorld.delete_failure"), Component.literal(pMessage));
   }

   public static void onPackCopyFailure(Minecraft pMinecraft, String pMessage) {
      add(pMinecraft.getToasts(), SystemToast.SystemToastIds.PACK_COPY_FAILURE, Component.translatable("pack.copyFailure"), Component.literal(pMessage));
   }

   @OnlyIn(Dist.CLIENT)
   public static enum SystemToastIds {
      TUTORIAL_HINT,
      NARRATOR_TOGGLE,
      WORLD_BACKUP,
      WORLD_GEN_SETTINGS_TRANSFER,
      PACK_LOAD_FAILURE,
      WORLD_ACCESS_FAILURE,
      PACK_COPY_FAILURE,
      PERIODIC_NOTIFICATION,
      CHAT_PREVIEW_WARNING(10000L),
      UNSECURE_SERVER_WARNING(10000L);

      final long displayTime;

      private SystemToastIds(long p_232551_) {
         this.displayTime = p_232551_;
      }

      private SystemToastIds() {
         this(5000L);
      }
   }
}