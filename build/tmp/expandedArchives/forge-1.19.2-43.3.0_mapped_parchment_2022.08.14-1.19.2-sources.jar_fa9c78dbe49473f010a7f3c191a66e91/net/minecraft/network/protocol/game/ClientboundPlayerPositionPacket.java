package net.minecraft.network.protocol.game;

import java.util.EnumSet;
import java.util.Set;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ClientboundPlayerPositionPacket implements Packet<ClientGamePacketListener> {
   private final double x;
   private final double y;
   private final double z;
   private final float yRot;
   private final float xRot;
   private final Set<ClientboundPlayerPositionPacket.RelativeArgument> relativeArguments;
   private final int id;
   private final boolean dismountVehicle;

   public ClientboundPlayerPositionPacket(double pX, double pY, double pZ, float pYRot, float pXRot, Set<ClientboundPlayerPositionPacket.RelativeArgument> pRelativeArguments, int pId, boolean pDismountVehicle) {
      this.x = pX;
      this.y = pY;
      this.z = pZ;
      this.yRot = pYRot;
      this.xRot = pXRot;
      this.relativeArguments = pRelativeArguments;
      this.id = pId;
      this.dismountVehicle = pDismountVehicle;
   }

   public ClientboundPlayerPositionPacket(FriendlyByteBuf pBuffer) {
      this.x = pBuffer.readDouble();
      this.y = pBuffer.readDouble();
      this.z = pBuffer.readDouble();
      this.yRot = pBuffer.readFloat();
      this.xRot = pBuffer.readFloat();
      this.relativeArguments = ClientboundPlayerPositionPacket.RelativeArgument.unpack(pBuffer.readUnsignedByte());
      this.id = pBuffer.readVarInt();
      this.dismountVehicle = pBuffer.readBoolean();
   }

   /**
    * Writes the raw packet data to the data stream.
    */
   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeDouble(this.x);
      pBuffer.writeDouble(this.y);
      pBuffer.writeDouble(this.z);
      pBuffer.writeFloat(this.yRot);
      pBuffer.writeFloat(this.xRot);
      pBuffer.writeByte(ClientboundPlayerPositionPacket.RelativeArgument.pack(this.relativeArguments));
      pBuffer.writeVarInt(this.id);
      pBuffer.writeBoolean(this.dismountVehicle);
   }

   /**
    * Passes this Packet on to the NetHandler for processing.
    */
   public void handle(ClientGamePacketListener pHandler) {
      pHandler.handleMovePlayer(this);
   }

   public double getX() {
      return this.x;
   }

   public double getY() {
      return this.y;
   }

   public double getZ() {
      return this.z;
   }

   public float getYRot() {
      return this.yRot;
   }

   public float getXRot() {
      return this.xRot;
   }

   public int getId() {
      return this.id;
   }

   public boolean requestDismountVehicle() {
      return this.dismountVehicle;
   }

   /**
    * Returns a set of which fields are relative. Items in this set indicate that the value is a relative change applied
    * to the player's position, rather than an exact value.
    */
   public Set<ClientboundPlayerPositionPacket.RelativeArgument> getRelativeArguments() {
      return this.relativeArguments;
   }

   public static enum RelativeArgument {
      X(0),
      Y(1),
      Z(2),
      Y_ROT(3),
      X_ROT(4);

      private final int bit;

      private RelativeArgument(int pBit) {
         this.bit = pBit;
      }

      private int getMask() {
         return 1 << this.bit;
      }

      /**
       * Checks if this flag is set within the given set of flags.
       */
      private boolean isSet(int pFlags) {
         return (pFlags & this.getMask()) == this.getMask();
      }

      public static Set<ClientboundPlayerPositionPacket.RelativeArgument> unpack(int pFlags) {
         Set<ClientboundPlayerPositionPacket.RelativeArgument> set = EnumSet.noneOf(ClientboundPlayerPositionPacket.RelativeArgument.class);

         for(ClientboundPlayerPositionPacket.RelativeArgument clientboundplayerpositionpacket$relativeargument : values()) {
            if (clientboundplayerpositionpacket$relativeargument.isSet(pFlags)) {
               set.add(clientboundplayerpositionpacket$relativeargument);
            }
         }

         return set;
      }

      public static int pack(Set<ClientboundPlayerPositionPacket.RelativeArgument> pFlags) {
         int i = 0;

         for(ClientboundPlayerPositionPacket.RelativeArgument clientboundplayerpositionpacket$relativeargument : pFlags) {
            i |= clientboundplayerpositionpacket$relativeargument.getMask();
         }

         return i;
      }
   }
}