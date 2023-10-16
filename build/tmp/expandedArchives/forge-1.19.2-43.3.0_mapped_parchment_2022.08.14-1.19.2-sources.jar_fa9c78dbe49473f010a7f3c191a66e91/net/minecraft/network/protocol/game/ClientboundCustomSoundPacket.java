package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public class ClientboundCustomSoundPacket implements Packet<ClientGamePacketListener> {
   public static final float LOCATION_ACCURACY = 8.0F;
   private final ResourceLocation name;
   private final SoundSource source;
   private final int x;
   private final int y;
   private final int z;
   private final float volume;
   private final float pitch;
   private final long seed;

   public ClientboundCustomSoundPacket(ResourceLocation pName, SoundSource pSource, Vec3 pPosition, float pVolume, float pPitch, long pSeed) {
      this.name = pName;
      this.source = pSource;
      this.x = (int)(pPosition.x * 8.0D);
      this.y = (int)(pPosition.y * 8.0D);
      this.z = (int)(pPosition.z * 8.0D);
      this.volume = pVolume;
      this.pitch = pPitch;
      this.seed = pSeed;
   }

   public ClientboundCustomSoundPacket(FriendlyByteBuf pBuffer) {
      this.name = pBuffer.readResourceLocation();
      this.source = pBuffer.readEnum(SoundSource.class);
      this.x = pBuffer.readInt();
      this.y = pBuffer.readInt();
      this.z = pBuffer.readInt();
      this.volume = pBuffer.readFloat();
      this.pitch = pBuffer.readFloat();
      this.seed = pBuffer.readLong();
   }

   /**
    * Writes the raw packet data to the data stream.
    */
   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeResourceLocation(this.name);
      pBuffer.writeEnum(this.source);
      pBuffer.writeInt(this.x);
      pBuffer.writeInt(this.y);
      pBuffer.writeInt(this.z);
      pBuffer.writeFloat(this.volume);
      pBuffer.writeFloat(this.pitch);
      pBuffer.writeLong(this.seed);
   }

   public ResourceLocation getName() {
      return this.name;
   }

   public SoundSource getSource() {
      return this.source;
   }

   public double getX() {
      return (double)((float)this.x / 8.0F);
   }

   public double getY() {
      return (double)((float)this.y / 8.0F);
   }

   public double getZ() {
      return (double)((float)this.z / 8.0F);
   }

   public float getVolume() {
      return this.volume;
   }

   public float getPitch() {
      return this.pitch;
   }

   public long getSeed() {
      return this.seed;
   }

   /**
    * Passes this Packet on to the NetHandler for processing.
    */
   public void handle(ClientGamePacketListener pHandler) {
      pHandler.handleCustomSoundEvent(this);
   }
}