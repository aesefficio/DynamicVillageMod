package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.damagesource.CombatTracker;

public class ClientboundPlayerCombatEndPacket implements Packet<ClientGamePacketListener> {
   private final int killerId;
   private final int duration;

   public ClientboundPlayerCombatEndPacket(CombatTracker pCombatTracker) {
      this(pCombatTracker.getKillerId(), pCombatTracker.getCombatDuration());
   }

   public ClientboundPlayerCombatEndPacket(int pKillerId, int pDuration) {
      this.killerId = pKillerId;
      this.duration = pDuration;
   }

   public ClientboundPlayerCombatEndPacket(FriendlyByteBuf pBuffer) {
      this.duration = pBuffer.readVarInt();
      this.killerId = pBuffer.readInt();
   }

   /**
    * Writes the raw packet data to the data stream.
    */
   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeVarInt(this.duration);
      pBuffer.writeInt(this.killerId);
   }

   /**
    * Passes this Packet on to the NetHandler for processing.
    */
   public void handle(ClientGamePacketListener pHandler) {
      pHandler.handlePlayerCombatEnd(this);
   }
}