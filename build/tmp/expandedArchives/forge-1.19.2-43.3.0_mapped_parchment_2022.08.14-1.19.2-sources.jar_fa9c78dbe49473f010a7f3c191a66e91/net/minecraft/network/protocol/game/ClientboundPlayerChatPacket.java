package net.minecraft.network.protocol.game;

import java.util.Optional;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.protocol.Packet;

public record ClientboundPlayerChatPacket(PlayerChatMessage message, ChatType.BoundNetwork chatType) implements Packet<ClientGamePacketListener> {
   public ClientboundPlayerChatPacket(FriendlyByteBuf p_237741_) {
      this(new PlayerChatMessage(p_237741_), new ChatType.BoundNetwork(p_237741_));
   }

   /**
    * Writes the raw packet data to the data stream.
    */
   public void write(FriendlyByteBuf p_237755_) {
      this.message.write(p_237755_);
      this.chatType.write(p_237755_);
   }

   /**
    * Passes this Packet on to the NetHandler for processing.
    */
   public void handle(ClientGamePacketListener p_237759_) {
      p_237759_.handlePlayerChat(this);
   }

   /**
    * Whether decoding errors will be ignored for this packet.
    */
   public boolean isSkippable() {
      return true;
   }

   public Optional<ChatType.Bound> resolveChatType(RegistryAccess p_242874_) {
      return this.chatType.resolve(p_242874_);
   }
}