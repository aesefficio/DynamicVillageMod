package net.minecraft.client.multiplayer.chat;

import javax.annotation.Nullable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RollingMemoryChatLog implements ChatLog {
   private final LoggedChatEvent[] buffer;
   private int newestId = -1;
   private int oldestId = -1;

   public RollingMemoryChatLog(int p_239903_) {
      this.buffer = new LoggedChatEvent[p_239903_];
   }

   public void push(LoggedChatEvent p_242377_) {
      int i = this.nextId();
      this.buffer[this.index(i)] = p_242377_;
   }

   private int nextId() {
      int i = ++this.newestId;
      if (i >= this.buffer.length) {
         ++this.oldestId;
      } else {
         this.oldestId = 0;
      }

      return i;
   }

   @Nullable
   public LoggedChatEvent lookup(int p_242175_) {
      return this.contains(p_242175_) ? this.buffer[this.index(p_242175_)] : null;
   }

   private int index(int p_239511_) {
      return p_239511_ % this.buffer.length;
   }

   public boolean contains(int p_239977_) {
      return p_239977_ >= this.oldestId && p_239977_ <= this.newestId;
   }

   public int offset(int p_240086_, int p_240087_) {
      int i = p_240086_ + p_240087_;
      return this.contains(i) ? i : -1;
   }

   public int newest() {
      return this.newestId;
   }

   public int oldest() {
      return this.oldestId;
   }
}