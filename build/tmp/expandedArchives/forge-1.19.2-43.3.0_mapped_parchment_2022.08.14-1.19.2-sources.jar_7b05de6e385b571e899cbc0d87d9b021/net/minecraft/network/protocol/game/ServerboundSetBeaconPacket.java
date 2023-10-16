package net.minecraft.network.protocol.game;

import java.util.Optional;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.effect.MobEffect;

public class ServerboundSetBeaconPacket implements Packet<ServerGamePacketListener> {
   private final Optional<MobEffect> primary;
   private final Optional<MobEffect> secondary;

   public ServerboundSetBeaconPacket(Optional<MobEffect> pPrimary, Optional<MobEffect> pSecondary) {
      this.primary = pPrimary;
      this.secondary = pSecondary;
   }

   public ServerboundSetBeaconPacket(FriendlyByteBuf pBuffer) {
      this.primary = pBuffer.readOptional((p_238002_) -> {
         return p_238002_.readById(Registry.MOB_EFFECT);
      });
      this.secondary = pBuffer.readOptional((p_237996_) -> {
         return p_237996_.readById(Registry.MOB_EFFECT);
      });
   }

   /**
    * Writes the raw packet data to the data stream.
    */
   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeOptional(this.primary, (p_237998_, p_237999_) -> {
         p_237998_.writeId(Registry.MOB_EFFECT, p_237999_);
      });
      pBuffer.writeOptional(this.secondary, (p_237992_, p_237993_) -> {
         p_237992_.writeId(Registry.MOB_EFFECT, p_237993_);
      });
   }

   /**
    * Passes this Packet on to the NetHandler for processing.
    */
   public void handle(ServerGamePacketListener pHandler) {
      pHandler.handleSetBeaconPacket(this);
   }

   public Optional<MobEffect> getPrimary() {
      return this.primary;
   }

   public Optional<MobEffect> getSecondary() {
      return this.secondary;
   }
}