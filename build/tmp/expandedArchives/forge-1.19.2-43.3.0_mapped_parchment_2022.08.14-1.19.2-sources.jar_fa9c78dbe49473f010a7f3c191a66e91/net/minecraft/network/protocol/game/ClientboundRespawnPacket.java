package net.minecraft.network.protocol.game;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

public class ClientboundRespawnPacket implements Packet<ClientGamePacketListener> {
   private final ResourceKey<DimensionType> dimensionType;
   private final ResourceKey<Level> dimension;
   /** First 8 bytes of the SHA-256 hash of the world's seed */
   private final long seed;
   private final GameType playerGameType;
   @Nullable
   private final GameType previousPlayerGameType;
   private final boolean isDebug;
   private final boolean isFlat;
   private final boolean keepAllPlayerData;
   private final Optional<GlobalPos> lastDeathLocation;

   public ClientboundRespawnPacket(ResourceKey<DimensionType> pDimensionType, ResourceKey<Level> pDimension, long pSeed, GameType pPlayerGameType, @Nullable GameType pPreviousPlayerGameType, boolean pIsDebug, boolean pIsFlat, boolean pKeepAllPlayerData, Optional<GlobalPos> pLastDeathLocation) {
      this.dimensionType = pDimensionType;
      this.dimension = pDimension;
      this.seed = pSeed;
      this.playerGameType = pPlayerGameType;
      this.previousPlayerGameType = pPreviousPlayerGameType;
      this.isDebug = pIsDebug;
      this.isFlat = pIsFlat;
      this.keepAllPlayerData = pKeepAllPlayerData;
      this.lastDeathLocation = pLastDeathLocation;
   }

   public ClientboundRespawnPacket(FriendlyByteBuf pBuffer) {
      this.dimensionType = pBuffer.readResourceKey(Registry.DIMENSION_TYPE_REGISTRY);
      this.dimension = pBuffer.readResourceKey(Registry.DIMENSION_REGISTRY);
      this.seed = pBuffer.readLong();
      this.playerGameType = GameType.byId(pBuffer.readUnsignedByte());
      this.previousPlayerGameType = GameType.byNullableId(pBuffer.readByte());
      this.isDebug = pBuffer.readBoolean();
      this.isFlat = pBuffer.readBoolean();
      this.keepAllPlayerData = pBuffer.readBoolean();
      this.lastDeathLocation = pBuffer.readOptional(FriendlyByteBuf::readGlobalPos);
   }

   /**
    * Writes the raw packet data to the data stream.
    */
   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeResourceKey(this.dimensionType);
      pBuffer.writeResourceKey(this.dimension);
      pBuffer.writeLong(this.seed);
      pBuffer.writeByte(this.playerGameType.getId());
      pBuffer.writeByte(GameType.getNullableId(this.previousPlayerGameType));
      pBuffer.writeBoolean(this.isDebug);
      pBuffer.writeBoolean(this.isFlat);
      pBuffer.writeBoolean(this.keepAllPlayerData);
      pBuffer.writeOptional(this.lastDeathLocation, FriendlyByteBuf::writeGlobalPos);
   }

   /**
    * Passes this Packet on to the NetHandler for processing.
    */
   public void handle(ClientGamePacketListener pHandler) {
      pHandler.handleRespawn(this);
   }

   public ResourceKey<DimensionType> getDimensionType() {
      return this.dimensionType;
   }

   public ResourceKey<Level> getDimension() {
      return this.dimension;
   }

   /**
    * get value
    */
   public long getSeed() {
      return this.seed;
   }

   public GameType getPlayerGameType() {
      return this.playerGameType;
   }

   @Nullable
   public GameType getPreviousPlayerGameType() {
      return this.previousPlayerGameType;
   }

   public boolean isDebug() {
      return this.isDebug;
   }

   public boolean isFlat() {
      return this.isFlat;
   }

   public boolean shouldKeepAllPlayerData() {
      return this.keepAllPlayerData;
   }

   public Optional<GlobalPos> getLastDeathLocation() {
      return this.lastDeathLocation;
   }
}