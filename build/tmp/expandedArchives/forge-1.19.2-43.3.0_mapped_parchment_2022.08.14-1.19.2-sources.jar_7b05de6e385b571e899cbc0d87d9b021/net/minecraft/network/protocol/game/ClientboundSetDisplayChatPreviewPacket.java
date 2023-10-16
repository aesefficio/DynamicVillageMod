package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public record ClientboundSetDisplayChatPreviewPacket(boolean enabled) implements Packet<ClientGamePacketListener> {
   public ClientboundSetDisplayChatPreviewPacket(FriendlyByteBuf p_237815_) {
      this(p_237815_.readBoolean());
   }

   /**
    * Writes the raw packet data to the data stream.
    */
   public void write(FriendlyByteBuf p_237819_) {
      p_237819_.writeBoolean(this.enabled);
   }

   /**
    * Passes this Packet on to the NetHandler for processing.
    */
   public void handle(ClientGamePacketListener p_237823_) {
      p_237823_.handleSetDisplayChatPreview(this);
   }
}