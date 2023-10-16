package net.minecraft.network.protocol.game;

import java.time.Instant;
import net.minecraft.commands.arguments.ArgumentSignatures;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.LastSeenMessages;
import net.minecraft.network.protocol.Packet;
import net.minecraft.util.StringUtil;

public record ServerboundChatCommandPacket(String command, Instant timeStamp, long salt, ArgumentSignatures argumentSignatures, boolean signedPreview, LastSeenMessages.Update lastSeenMessages) implements Packet<ServerGamePacketListener> {
   public ServerboundChatCommandPacket {
      command = StringUtil.trimChatMessage(command);
   }

   public ServerboundChatCommandPacket(FriendlyByteBuf p_237932_) {
      this(p_237932_.readUtf(256), p_237932_.readInstant(), p_237932_.readLong(), new ArgumentSignatures(p_237932_), p_237932_.readBoolean(), new LastSeenMessages.Update(p_237932_));
   }

   /**
    * Writes the raw packet data to the data stream.
    */
   public void write(FriendlyByteBuf p_237936_) {
      p_237936_.writeUtf(this.command, 256);
      p_237936_.writeInstant(this.timeStamp);
      p_237936_.writeLong(this.salt);
      this.argumentSignatures.write(p_237936_);
      p_237936_.writeBoolean(this.signedPreview);
      this.lastSeenMessages.write(p_237936_);
   }

   /**
    * Passes this Packet on to the NetHandler for processing.
    */
   public void handle(ServerGamePacketListener p_237940_) {
      p_237940_.handleChatCommand(this);
   }
}