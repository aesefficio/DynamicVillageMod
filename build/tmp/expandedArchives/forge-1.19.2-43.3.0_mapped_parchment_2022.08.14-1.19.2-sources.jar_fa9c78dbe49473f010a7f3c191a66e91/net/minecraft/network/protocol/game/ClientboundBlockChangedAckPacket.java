package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public record ClientboundBlockChangedAckPacket(int sequence) implements Packet<ClientGamePacketListener> {
   public ClientboundBlockChangedAckPacket(FriendlyByteBuf p_237582_) {
      this(p_237582_.readVarInt());
   }

   /**
    * Writes the raw packet data to the data stream.
    */
   public void write(FriendlyByteBuf p_237584_) {
      p_237584_.writeVarInt(this.sequence);
   }

   /**
    * Passes this Packet on to the NetHandler for processing.
    */
   public void handle(ClientGamePacketListener p_237588_) {
      p_237588_.handleBlockChangedAck(this);
   }
}