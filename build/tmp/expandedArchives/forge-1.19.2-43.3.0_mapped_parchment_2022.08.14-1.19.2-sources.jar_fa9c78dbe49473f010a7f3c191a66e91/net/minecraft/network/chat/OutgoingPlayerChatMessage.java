package net.minecraft.network.chat;

import com.google.common.collect.Sets;
import java.util.Set;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.game.ClientboundPlayerChatHeaderPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

public interface OutgoingPlayerChatMessage {
   Component serverContent();

   void sendToPlayer(ServerPlayer p_241554_, boolean p_243298_, ChatType.Bound p_241444_);

   void sendHeadersToRemainingPlayers(PlayerList p_241453_);

   static OutgoingPlayerChatMessage create(PlayerChatMessage p_242935_) {
      return (OutgoingPlayerChatMessage)(p_242935_.signer().isSystem() ? new OutgoingPlayerChatMessage.NotTracked(p_242935_) : new OutgoingPlayerChatMessage.Tracked(p_242935_));
   }

   public static class NotTracked implements OutgoingPlayerChatMessage {
      private final PlayerChatMessage message;

      public NotTracked(PlayerChatMessage p_241413_) {
         this.message = p_241413_;
      }

      public Component serverContent() {
         return this.message.serverContent();
      }

      public void sendToPlayer(ServerPlayer p_243208_, boolean p_243217_, ChatType.Bound p_243207_) {
         PlayerChatMessage playerchatmessage = this.message.filter(p_243217_);
         if (!playerchatmessage.isFullyFiltered()) {
            RegistryAccess registryaccess = p_243208_.level.registryAccess();
            ChatType.BoundNetwork chattype$boundnetwork = p_243207_.toNetwork(registryaccess);
            p_243208_.connection.send(new ClientboundPlayerChatPacket(playerchatmessage, chattype$boundnetwork));
            p_243208_.connection.addPendingMessage(playerchatmessage);
         }

      }

      public void sendHeadersToRemainingPlayers(PlayerList p_241443_) {
      }
   }

   public static class Tracked implements OutgoingPlayerChatMessage {
      private final PlayerChatMessage message;
      private final Set<ServerPlayer> playersWithFullMessage = Sets.newIdentityHashSet();

      public Tracked(PlayerChatMessage p_241558_) {
         this.message = p_241558_;
      }

      public Component serverContent() {
         return this.message.serverContent();
      }

      public void sendToPlayer(ServerPlayer p_243241_, boolean p_243304_, ChatType.Bound p_243225_) {
         PlayerChatMessage playerchatmessage = this.message.filter(p_243304_);
         if (!playerchatmessage.isFullyFiltered()) {
            this.playersWithFullMessage.add(p_243241_);
            RegistryAccess registryaccess = p_243241_.level.registryAccess();
            ChatType.BoundNetwork chattype$boundnetwork = p_243225_.toNetwork(registryaccess);
            p_243241_.connection.send(new ClientboundPlayerChatPacket(playerchatmessage, chattype$boundnetwork), PacketSendListener.exceptionallySend(() -> {
               return new ClientboundPlayerChatHeaderPacket(this.message);
            }));
            p_243241_.connection.addPendingMessage(playerchatmessage);
         }

      }

      public void sendHeadersToRemainingPlayers(PlayerList p_241386_) {
         p_241386_.broadcastMessageHeader(this.message, this.playersWithFullMessage);
      }
   }
}