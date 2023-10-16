package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.damagesource.CombatTracker;

public class ClientboundPlayerCombatKillPacket implements Packet<ClientGamePacketListener> {
   private final int playerId;
   private final int killerId;
   private final Component message;

   public ClientboundPlayerCombatKillPacket(CombatTracker pCombatTracker, Component pMessage) {
      this(pCombatTracker.getMob().getId(), pCombatTracker.getKillerId(), pMessage);
   }

   public ClientboundPlayerCombatKillPacket(int pPlayerId, int pKillerId, Component pMessage) {
      this.playerId = pPlayerId;
      this.killerId = pKillerId;
      this.message = pMessage;
   }

   public ClientboundPlayerCombatKillPacket(FriendlyByteBuf pBuffer) {
      this.playerId = pBuffer.readVarInt();
      this.killerId = pBuffer.readInt();
      this.message = pBuffer.readComponent();
   }

   /**
    * Writes the raw packet data to the data stream.
    */
   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeVarInt(this.playerId);
      pBuffer.writeInt(this.killerId);
      pBuffer.writeComponent(this.message);
   }

   /**
    * Passes this Packet on to the NetHandler for processing.
    */
   public void handle(ClientGamePacketListener pHandler) {
      pHandler.handlePlayerCombatKill(this);
   }

   /**
    * Whether decoding errors will be ignored for this packet.
    */
   public boolean isSkippable() {
      return true;
   }

   public int getKillerId() {
      return this.killerId;
   }

   public int getPlayerId() {
      return this.playerId;
   }

   public Component getMessage() {
      return this.message;
   }
}