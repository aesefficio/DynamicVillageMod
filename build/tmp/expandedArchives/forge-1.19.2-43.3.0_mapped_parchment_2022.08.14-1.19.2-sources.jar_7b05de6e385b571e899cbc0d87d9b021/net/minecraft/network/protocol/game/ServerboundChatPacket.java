package net.minecraft.network.protocol.game;

import java.time.Instant;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.LastSeenMessages;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.MessageSigner;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;

public record ServerboundChatPacket(String message, Instant timeStamp, long salt, MessageSignature signature, boolean signedPreview, LastSeenMessages.Update lastSeenMessages) implements Packet<ServerGamePacketListener> {
   public ServerboundChatPacket(FriendlyByteBuf pBuffer) {
      this(pBuffer.readUtf(256), pBuffer.readInstant(), pBuffer.readLong(), new MessageSignature(pBuffer), pBuffer.readBoolean(), new LastSeenMessages.Update(pBuffer));
   }

   /**
    * Writes the raw packet data to the data stream.
    */
   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeUtf(this.message, 256);
      pBuffer.writeInstant(this.timeStamp);
      pBuffer.writeLong(this.salt);
      this.signature.write(pBuffer);
      pBuffer.writeBoolean(this.signedPreview);
      this.lastSeenMessages.write(pBuffer);
   }

   /**
    * Passes this Packet on to the NetHandler for processing.
    */
   public void handle(ServerGamePacketListener pHandler) {
      pHandler.handleChat(this);
   }

   public MessageSigner getSigner(ServerPlayer p_241405_) {
      return new MessageSigner(p_241405_.getUUID(), this.timeStamp, this.salt);
   }
}