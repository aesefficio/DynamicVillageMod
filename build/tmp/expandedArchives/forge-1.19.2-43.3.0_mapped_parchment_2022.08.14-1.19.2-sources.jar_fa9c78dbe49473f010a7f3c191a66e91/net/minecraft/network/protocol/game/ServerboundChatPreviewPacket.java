package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.util.StringUtil;

public record ServerboundChatPreviewPacket(int queryId, String query) implements Packet<ServerGamePacketListener> {
   public ServerboundChatPreviewPacket {
      query = StringUtil.trimChatMessage(query);
   }

   public ServerboundChatPreviewPacket(FriendlyByteBuf p_237968_) {
      this(p_237968_.readInt(), p_237968_.readUtf(256));
   }

   /**
    * Writes the raw packet data to the data stream.
    */
   public void write(FriendlyByteBuf p_237970_) {
      p_237970_.writeInt(this.queryId);
      p_237970_.writeUtf(this.query, 256);
   }

   /**
    * Passes this Packet on to the NetHandler for processing.
    */
   public void handle(ServerGamePacketListener p_237974_) {
      p_237974_.handleChatPreview(this);
   }
}