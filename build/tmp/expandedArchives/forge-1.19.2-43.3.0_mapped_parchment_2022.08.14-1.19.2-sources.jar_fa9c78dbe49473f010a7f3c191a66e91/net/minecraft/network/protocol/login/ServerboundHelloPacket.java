package net.minecraft.network.protocol.login;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.player.ProfilePublicKey;

public record ServerboundHelloPacket(String name, Optional<ProfilePublicKey.Data> publicKey, Optional<UUID> profileId) implements Packet<ServerLoginPacketListener> {
   public ServerboundHelloPacket(FriendlyByteBuf pBuffer) {
      this(pBuffer.readUtf(16), pBuffer.readOptional(ProfilePublicKey.Data::new), pBuffer.readOptional(FriendlyByteBuf::readUUID));
   }

   /**
    * Writes the raw packet data to the data stream.
    */
   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeUtf(this.name, 16);
      pBuffer.writeOptional(this.publicKey, (p_238047_, p_238048_) -> {
         p_238048_.write(pBuffer);
      });
      pBuffer.writeOptional(this.profileId, FriendlyByteBuf::writeUUID);
   }

   /**
    * Passes this Packet on to the NetHandler for processing.
    */
   public void handle(ServerLoginPacketListener pHandler) {
      pHandler.handleHello(this);
   }
}