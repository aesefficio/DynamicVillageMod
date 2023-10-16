package net.minecraft.network.chat;

import java.time.Instant;
import java.util.UUID;
import net.minecraft.Util;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Crypt;

public record MessageSigner(UUID profileId, Instant timeStamp, long salt) {
   public MessageSigner(FriendlyByteBuf p_241430_) {
      this(p_241430_.readUUID(), p_241430_.readInstant(), p_241430_.readLong());
   }

   public static MessageSigner create(UUID p_237184_) {
      return new MessageSigner(p_237184_, Instant.now(), Crypt.SaltSupplier.getLong());
   }

   public static MessageSigner system() {
      return create(Util.NIL_UUID);
   }

   public void write(FriendlyByteBuf p_241475_) {
      p_241475_.writeUUID(this.profileId);
      p_241475_.writeInstant(this.timeStamp);
      p_241475_.writeLong(this.salt);
   }

   public boolean isSystem() {
      return this.profileId.equals(Util.NIL_UUID);
   }
}