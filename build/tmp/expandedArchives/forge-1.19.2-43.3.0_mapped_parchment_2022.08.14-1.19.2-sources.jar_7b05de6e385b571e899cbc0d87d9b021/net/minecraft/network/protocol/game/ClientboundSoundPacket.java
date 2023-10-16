package net.minecraft.network.protocol.game;

import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.apache.commons.lang3.Validate;

public class ClientboundSoundPacket implements Packet<ClientGamePacketListener> {
   public static final float LOCATION_ACCURACY = 8.0F;
   private final SoundEvent sound;
   private final SoundSource source;
   private final int x;
   private final int y;
   private final int z;
   private final float volume;
   private final float pitch;
   private final long seed;

   public ClientboundSoundPacket(SoundEvent pSound, SoundSource pSource, double pX, double pY, double pZ, float pVolume, float pPitch, long pSeed) {
      Validate.notNull(pSound, "sound");
      this.sound = pSound;
      this.source = pSource;
      this.x = (int)(pX * 8.0D);
      this.y = (int)(pY * 8.0D);
      this.z = (int)(pZ * 8.0D);
      this.volume = pVolume;
      this.pitch = pPitch;
      this.seed = pSeed;
   }

   public ClientboundSoundPacket(FriendlyByteBuf pBuffer) {
      this.sound = pBuffer.readById(Registry.SOUND_EVENT);
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
      pBuffer.writeId(Registry.SOUND_EVENT, this.sound);
      pBuffer.writeEnum(this.source);
      pBuffer.writeInt(this.x);
      pBuffer.writeInt(this.y);
      pBuffer.writeInt(this.z);
      pBuffer.writeFloat(this.volume);
      pBuffer.writeFloat(this.pitch);
      pBuffer.writeLong(this.seed);
   }

   public SoundEvent getSound() {
      return this.sound;
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
      pHandler.handleSoundEvent(this);
   }
}