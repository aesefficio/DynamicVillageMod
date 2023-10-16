package net.minecraft.network.protocol.game;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;

public class ClientboundServerDataPacket implements Packet<ClientGamePacketListener> {
   private final Optional<Component> motd;
   private final Optional<String> iconBase64;
   private final boolean previewsChat;
   private final boolean enforcesSecureChat;

   public ClientboundServerDataPacket(@Nullable Component p_242977_, @Nullable String p_242969_, boolean p_242973_, boolean p_242974_) {
      this.motd = Optional.ofNullable(p_242977_);
      this.iconBase64 = Optional.ofNullable(p_242969_);
      this.previewsChat = p_242973_;
      this.enforcesSecureChat = p_242974_;
   }

   public ClientboundServerDataPacket(FriendlyByteBuf p_237799_) {
      this.motd = p_237799_.readOptional(FriendlyByteBuf::readComponent);
      this.iconBase64 = p_237799_.readOptional(FriendlyByteBuf::readUtf);
      this.previewsChat = p_237799_.readBoolean();
      this.enforcesSecureChat = p_237799_.readBoolean();
   }

   /**
    * Writes the raw packet data to the data stream.
    */
   public void write(FriendlyByteBuf p_237805_) {
      p_237805_.writeOptional(this.motd, FriendlyByteBuf::writeComponent);
      p_237805_.writeOptional(this.iconBase64, FriendlyByteBuf::writeUtf);
      p_237805_.writeBoolean(this.previewsChat);
      p_237805_.writeBoolean(this.enforcesSecureChat);
   }

   /**
    * Passes this Packet on to the NetHandler for processing.
    */
   public void handle(ClientGamePacketListener p_237809_) {
      p_237809_.handleServerData(this);
   }

   public Optional<Component> getMotd() {
      return this.motd;
   }

   public Optional<String> getIconBase64() {
      return this.iconBase64;
   }

   public boolean previewsChat() {
      return this.previewsChat;
   }

   public boolean enforcesSecureChat() {
      return this.enforcesSecureChat;
   }
}