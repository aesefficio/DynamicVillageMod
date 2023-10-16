package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ClientboundOpenSignEditorPacket implements Packet<ClientGamePacketListener> {
   private final BlockPos pos;

   public ClientboundOpenSignEditorPacket(BlockPos pPos) {
      this.pos = pPos;
   }

   public ClientboundOpenSignEditorPacket(FriendlyByteBuf pBuffer) {
      this.pos = pBuffer.readBlockPos();
   }

   /**
    * Writes the raw packet data to the data stream.
    */
   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeBlockPos(this.pos);
   }

   /**
    * Passes this Packet on to the NetHandler for processing.
    */
   public void handle(ClientGamePacketListener pHandler) {
      pHandler.handleOpenSignEditor(this);
   }

   public BlockPos getPos() {
      return this.pos;
   }
}