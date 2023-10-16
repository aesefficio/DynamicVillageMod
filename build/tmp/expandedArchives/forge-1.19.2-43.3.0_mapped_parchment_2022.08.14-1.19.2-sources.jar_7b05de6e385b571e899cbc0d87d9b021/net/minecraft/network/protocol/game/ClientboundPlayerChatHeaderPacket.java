package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.chat.SignedMessageHeader;
import net.minecraft.network.protocol.Packet;

public record ClientboundPlayerChatHeaderPacket(SignedMessageHeader header, MessageSignature headerSignature, byte[] bodyDigest) implements Packet<ClientGamePacketListener> {
   public ClientboundPlayerChatHeaderPacket(PlayerChatMessage p_243270_) {
      this(p_243270_.signedHeader(), p_243270_.headerSignature(), p_243270_.signedBody().hash().asBytes());
   }

   public ClientboundPlayerChatHeaderPacket(FriendlyByteBuf p_241327_) {
      this(new SignedMessageHeader(p_241327_), new MessageSignature(p_241327_), p_241327_.readByteArray());
   }

   /**
    * Writes the raw packet data to the data stream.
    */
   public void write(FriendlyByteBuf p_241388_) {
      this.header.write(p_241388_);
      this.headerSignature.write(p_241388_);
      p_241388_.writeByteArray(this.bodyDigest);
   }

   /**
    * Passes this Packet on to the NetHandler for processing.
    */
   public void handle(ClientGamePacketListener p_241550_) {
      p_241550_.handlePlayerChatHeader(this);
   }
}