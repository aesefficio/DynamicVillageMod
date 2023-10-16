package net.minecraft.client.gui.chat;

import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ChatPreviewAnimator {
   private static final long FADE_DURATION = 200L;
   @Nullable
   private Component residualPreview;
   private long fadeTime;
   private long lastTime;

   public void reset(long p_242307_) {
      this.residualPreview = null;
      this.fadeTime = 0L;
      this.lastTime = p_242307_;
   }

   public ChatPreviewAnimator.State get(long p_242415_, @Nullable Component p_242349_) {
      long i = p_242415_ - this.lastTime;
      this.lastTime = p_242415_;
      return p_242349_ != null ? this.getEnabled(i, p_242349_) : this.getDisabled(i);
   }

   private ChatPreviewAnimator.State getEnabled(long p_242198_, Component p_242208_) {
      this.residualPreview = p_242208_;
      if (this.fadeTime < 200L) {
         this.fadeTime = Math.min(this.fadeTime + p_242198_, 200L);
      }

      return new ChatPreviewAnimator.State(p_242208_, alpha(this.fadeTime));
   }

   private ChatPreviewAnimator.State getDisabled(long p_242440_) {
      if (this.fadeTime > 0L) {
         this.fadeTime = Math.max(this.fadeTime - p_242440_, 0L);
      }

      return this.fadeTime > 0L ? new ChatPreviewAnimator.State(this.residualPreview, alpha(this.fadeTime)) : ChatPreviewAnimator.State.DISABLED;
   }

   private static float alpha(long p_242250_) {
      return (float)p_242250_ / 200.0F;
   }

   @OnlyIn(Dist.CLIENT)
   public static record State(@Nullable Component preview, float alpha) {
      public static final ChatPreviewAnimator.State DISABLED = new ChatPreviewAnimator.State((Component)null, 0.0F);
   }
}