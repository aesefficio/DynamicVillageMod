package net.minecraft.network.chat;

import javax.annotation.Nullable;

public class ChatPreviewCache {
   @Nullable
   private ChatPreviewCache.Result result;

   public void set(String p_242847_, Component p_242916_) {
      this.result = new ChatPreviewCache.Result(p_242847_, p_242916_);
   }

   @Nullable
   public Component pull(String p_242864_) {
      ChatPreviewCache.Result chatpreviewcache$result = this.result;
      if (chatpreviewcache$result != null && chatpreviewcache$result.matches(p_242864_)) {
         this.result = null;
         return chatpreviewcache$result.preview();
      } else {
         return null;
      }
   }

   static record Result(String query, Component preview) {
      public boolean matches(String p_242915_) {
         return this.query.equals(p_242915_);
      }
   }
}