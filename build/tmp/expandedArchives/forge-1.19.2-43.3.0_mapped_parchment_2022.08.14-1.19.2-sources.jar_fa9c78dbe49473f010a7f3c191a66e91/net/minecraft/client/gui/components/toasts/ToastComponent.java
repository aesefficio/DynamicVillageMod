package net.minecraft.client.gui.components.toasts;

import com.google.common.collect.Queues;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Deque;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ToastComponent extends GuiComponent {
   private static final int SLOT_COUNT = 5;
   private static final int NO_SPACE = -1;
   final Minecraft minecraft;
   private final List<ToastComponent.ToastInstance<?>> visible = new ArrayList<>();
   private final BitSet occupiedSlots = new BitSet(5);
   private final Deque<Toast> queued = Queues.newArrayDeque();

   public ToastComponent(Minecraft pMinecraft) {
      this.minecraft = pMinecraft;
   }

   public void render(PoseStack pPoseStack) {
      if (!this.minecraft.options.hideGui) {
         int i = this.minecraft.getWindow().getGuiScaledWidth();
         this.visible.removeIf((p_243286_) -> {
            if (p_243286_ != null && p_243286_.render(i, pPoseStack)) {
               this.occupiedSlots.clear(p_243286_.index, p_243286_.index + p_243286_.slotCount);
               return true;
            } else {
               return false;
            }
         });
         if (!this.queued.isEmpty() && this.freeSlots() > 0) {
            this.queued.removeIf((p_243239_) -> {
               int j = p_243239_.slotCount();
               int k = this.findFreeIndex(j);
               if (k != -1) {
                  this.visible.add(new ToastComponent.ToastInstance<>(p_243239_, k, j));
                  this.occupiedSlots.set(k, k + j);
                  return true;
               } else {
                  return false;
               }
            });
         }

      }
   }

   private int findFreeIndex(int p_243272_) {
      if (this.freeSlots() >= p_243272_) {
         int i = 0;

         for(int j = 0; j < 5; ++j) {
            if (this.occupiedSlots.get(j)) {
               i = 0;
            } else {
               ++i;
               if (i == p_243272_) {
                  return j + 1 - i;
               }
            }
         }
      }

      return -1;
   }

   private int freeSlots() {
      return 5 - this.occupiedSlots.cardinality();
   }

   @Nullable
   public <T extends Toast> T getToast(Class<? extends T> pToastClass, Object pToken) {
      for(ToastComponent.ToastInstance<?> toastinstance : this.visible) {
         if (toastinstance != null && pToastClass.isAssignableFrom(toastinstance.getToast().getClass()) && toastinstance.getToast().getToken().equals(pToken)) {
            return (T)toastinstance.getToast();
         }
      }

      for(Toast toast : this.queued) {
         if (pToastClass.isAssignableFrom(toast.getClass()) && toast.getToken().equals(pToken)) {
            return (T)toast;
         }
      }

      return (T)null;
   }

   public void clear() {
      this.occupiedSlots.clear();
      this.visible.clear();
      this.queued.clear();
   }

   public void addToast(Toast pToast) {
      if (net.minecraftforge.client.ForgeHooksClient.onToastAdd(pToast)) return;
      this.queued.add(pToast);
   }

   public Minecraft getMinecraft() {
      return this.minecraft;
   }

   @OnlyIn(Dist.CLIENT)
   class ToastInstance<T extends Toast> {
      private static final long ANIMATION_TIME = 600L;
      private final T toast;
      final int index;
      final int slotCount;
      private long animationTime = -1L;
      private long visibleTime = -1L;
      private Toast.Visibility visibility = Toast.Visibility.SHOW;

      ToastInstance(T pToast, int pIndex, int pSlotCount) {
         this.toast = pToast;
         this.index = pIndex;
         this.slotCount = pSlotCount;
      }

      public T getToast() {
         return this.toast;
      }

      private float getVisibility(long pTime) {
         float f = Mth.clamp((float)(pTime - this.animationTime) / 600.0F, 0.0F, 1.0F);
         f *= f;
         return this.visibility == Toast.Visibility.HIDE ? 1.0F - f : f;
      }

      public boolean render(int pWidth, PoseStack pPoseStack) {
         long i = Util.getMillis();
         if (this.animationTime == -1L) {
            this.animationTime = i;
            this.visibility.playSound(ToastComponent.this.minecraft.getSoundManager());
         }

         if (this.visibility == Toast.Visibility.SHOW && i - this.animationTime <= 600L) {
            this.visibleTime = i;
         }

         PoseStack posestack = RenderSystem.getModelViewStack();
         posestack.pushPose();
         posestack.translate((double)((float)pWidth - (float)this.toast.width() * this.getVisibility(i)), (double)(this.index * 32), 800.0D);
         RenderSystem.applyModelViewMatrix();
         Toast.Visibility toast$visibility = this.toast.render(pPoseStack, ToastComponent.this, i - this.visibleTime);
         posestack.popPose();
         RenderSystem.applyModelViewMatrix();
         if (toast$visibility != this.visibility) {
            this.animationTime = i - (long)((int)((1.0F - this.getVisibility(i)) * 600.0F));
            this.visibility = toast$visibility;
            this.visibility.playSound(ToastComponent.this.minecraft.getSoundManager());
         }

         return this.visibility == Toast.Visibility.HIDE && i - this.animationTime > 600L;
      }
   }
}
