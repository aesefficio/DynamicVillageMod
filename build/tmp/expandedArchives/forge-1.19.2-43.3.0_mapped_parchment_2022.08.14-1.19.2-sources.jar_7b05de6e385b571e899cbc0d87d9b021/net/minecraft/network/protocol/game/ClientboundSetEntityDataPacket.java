package net.minecraft.network.protocol.game;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.SynchedEntityData;

public class ClientboundSetEntityDataPacket implements Packet<ClientGamePacketListener> {
   private final int id;
   @Nullable
   private final List<SynchedEntityData.DataItem<?>> packedItems;

   public ClientboundSetEntityDataPacket(int pId, SynchedEntityData pEntityData, boolean pSendAll) {
      this.id = pId;
      if (pSendAll) {
         this.packedItems = pEntityData.getAll();
         pEntityData.clearDirty();
      } else {
         this.packedItems = pEntityData.packDirty();
      }

   }

   public ClientboundSetEntityDataPacket(FriendlyByteBuf pBuffer) {
      this.id = pBuffer.readVarInt();
      this.packedItems = SynchedEntityData.unpack(pBuffer);
   }

   /**
    * Writes the raw packet data to the data stream.
    */
   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeVarInt(this.id);
      SynchedEntityData.pack(this.packedItems, pBuffer);
   }

   /**
    * Passes this Packet on to the NetHandler for processing.
    */
   public void handle(ClientGamePacketListener pHandler) {
      pHandler.handleSetEntityData(this);
   }

   @Nullable
   public List<SynchedEntityData.DataItem<?>> getUnpackedData() {
      return this.packedItems;
   }

   public int getId() {
      return this.id;
   }
}