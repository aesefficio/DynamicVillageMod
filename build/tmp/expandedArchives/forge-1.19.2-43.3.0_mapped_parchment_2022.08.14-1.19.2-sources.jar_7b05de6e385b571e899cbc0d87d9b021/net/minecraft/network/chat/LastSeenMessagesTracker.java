package net.minecraft.network.chat;

import java.util.Arrays;

public class LastSeenMessagesTracker {
   private final LastSeenMessages.Entry[] status;
   private int size;
   private LastSeenMessages result = LastSeenMessages.EMPTY;

   public LastSeenMessagesTracker(int p_242388_) {
      this.status = new LastSeenMessages.Entry[p_242388_];
   }

   public void push(LastSeenMessages.Entry p_242466_) {
      LastSeenMessages.Entry lastseenmessages$entry = p_242466_;

      for(int i = 0; i < this.size; ++i) {
         LastSeenMessages.Entry lastseenmessages$entry1 = this.status[i];
         this.status[i] = lastseenmessages$entry;
         lastseenmessages$entry = lastseenmessages$entry1;
         if (lastseenmessages$entry1.profileId().equals(p_242466_.profileId())) {
            lastseenmessages$entry = null;
            break;
         }
      }

      if (lastseenmessages$entry != null && this.size < this.status.length) {
         this.status[this.size++] = lastseenmessages$entry;
      }

      this.result = new LastSeenMessages(Arrays.asList(Arrays.copyOf(this.status, this.size)));
   }

   public LastSeenMessages get() {
      return this.result;
   }
}