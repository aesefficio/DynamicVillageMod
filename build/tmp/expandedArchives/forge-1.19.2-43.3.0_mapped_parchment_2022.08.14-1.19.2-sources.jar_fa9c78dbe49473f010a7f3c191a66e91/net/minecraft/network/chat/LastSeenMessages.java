package net.minecraft.network.chat;

import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;

public record LastSeenMessages(List<LastSeenMessages.Entry> entries) {
   public static LastSeenMessages EMPTY = new LastSeenMessages(List.of());
   public static final int LAST_SEEN_MESSAGES_MAX_LENGTH = 5;

   public LastSeenMessages(FriendlyByteBuf p_242268_) {
      this(p_242268_.<LastSeenMessages.Entry, List<LastSeenMessages.Entry>>readCollection(FriendlyByteBuf.limitValue(ArrayList::new, 5), LastSeenMessages.Entry::new));
   }

   public void write(FriendlyByteBuf p_242309_) {
      p_242309_.writeCollection(this.entries, (p_242176_, p_242457_) -> {
         p_242457_.write(p_242176_);
      });
   }

   public void updateHash(DataOutput p_242294_) throws IOException {
      for(LastSeenMessages.Entry lastseenmessages$entry : this.entries) {
         UUID uuid = lastseenmessages$entry.profileId();
         MessageSignature messagesignature = lastseenmessages$entry.lastSignature();
         p_242294_.writeByte(70);
         p_242294_.writeLong(uuid.getMostSignificantBits());
         p_242294_.writeLong(uuid.getLeastSignificantBits());
         p_242294_.write(messagesignature.bytes());
      }

   }

   public static record Entry(UUID profileId, MessageSignature lastSignature) {
      public Entry(FriendlyByteBuf p_242242_) {
         this(p_242242_.readUUID(), new MessageSignature(p_242242_));
      }

      public void write(FriendlyByteBuf p_242253_) {
         p_242253_.writeUUID(this.profileId);
         this.lastSignature.write(p_242253_);
      }
   }

   public static record Update(LastSeenMessages lastSeen, Optional<LastSeenMessages.Entry> lastReceived) {
      public Update(FriendlyByteBuf p_242184_) {
         this(new LastSeenMessages(p_242184_), p_242184_.readOptional(LastSeenMessages.Entry::new));
      }

      public void write(FriendlyByteBuf p_242221_) {
         this.lastSeen.write(p_242221_);
         p_242221_.writeOptional(this.lastReceived, (p_242427_, p_242226_) -> {
            p_242226_.write(p_242427_);
         });
      }
   }
}