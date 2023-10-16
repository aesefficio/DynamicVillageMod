package net.minecraft.network.protocol.game;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.ProfilePublicKey;
import net.minecraft.world.level.GameType;

public class ClientboundPlayerInfoPacket implements Packet<ClientGamePacketListener> {
   private final ClientboundPlayerInfoPacket.Action action;
   private final List<ClientboundPlayerInfoPacket.PlayerUpdate> entries;

   public ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action pAction, ServerPlayer... pPlayers) {
      this.action = pAction;
      this.entries = Lists.newArrayListWithCapacity(pPlayers.length);

      for(ServerPlayer serverplayer : pPlayers) {
         this.entries.add(createPlayerUpdate(serverplayer));
      }

   }

   public ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action pAction, Collection<ServerPlayer> pPlayers) {
      this.action = pAction;
      this.entries = Lists.newArrayListWithCapacity(pPlayers.size());

      for(ServerPlayer serverplayer : pPlayers) {
         this.entries.add(createPlayerUpdate(serverplayer));
      }

   }

   public ClientboundPlayerInfoPacket(FriendlyByteBuf pBuffer) {
      this.action = pBuffer.readEnum(ClientboundPlayerInfoPacket.Action.class);
      this.entries = pBuffer.readList(this.action::read);
   }

   private static ClientboundPlayerInfoPacket.PlayerUpdate createPlayerUpdate(ServerPlayer pPlayer) {
      ProfilePublicKey profilepublickey = pPlayer.getProfilePublicKey();
      ProfilePublicKey.Data profilepublickey$data = profilepublickey != null ? profilepublickey.data() : null;
      return new ClientboundPlayerInfoPacket.PlayerUpdate(pPlayer.getGameProfile(), pPlayer.latency, pPlayer.gameMode.getGameModeForPlayer(), pPlayer.getTabListDisplayName(), profilepublickey$data);
   }

   /**
    * Writes the raw packet data to the data stream.
    */
   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeEnum(this.action);
      pBuffer.writeCollection(this.entries, this.action::write);
   }

   /**
    * Passes this Packet on to the NetHandler for processing.
    */
   public void handle(ClientGamePacketListener pHandler) {
      pHandler.handlePlayerInfo(this);
   }

   public List<ClientboundPlayerInfoPacket.PlayerUpdate> getEntries() {
      return this.entries;
   }

   public ClientboundPlayerInfoPacket.Action getAction() {
      return this.action;
   }

   public String toString() {
      return MoreObjects.toStringHelper(this).add("action", this.action).add("entries", this.entries).toString();
   }

   public static enum Action {
      ADD_PLAYER {
         protected ClientboundPlayerInfoPacket.PlayerUpdate read(FriendlyByteBuf p_179101_) {
            GameProfile gameprofile = p_179101_.readGameProfile();
            GameType gametype = GameType.byId(p_179101_.readVarInt());
            int i = p_179101_.readVarInt();
            Component component = p_179101_.readNullable(FriendlyByteBuf::readComponent);
            ProfilePublicKey.Data profilepublickey$data = p_179101_.readNullable(ProfilePublicKey.Data::new);
            return new ClientboundPlayerInfoPacket.PlayerUpdate(gameprofile, i, gametype, component, profilepublickey$data);
         }

         protected void write(FriendlyByteBuf p_179106_, ClientboundPlayerInfoPacket.PlayerUpdate p_179107_) {
            p_179106_.writeGameProfile(p_179107_.getProfile());
            p_179106_.writeVarInt(p_179107_.getGameMode().getId());
            p_179106_.writeVarInt(p_179107_.getLatency());
            p_179106_.writeNullable(p_179107_.getDisplayName(), FriendlyByteBuf::writeComponent);
            p_179106_.writeNullable(p_179107_.getProfilePublicKey(), (p_237775_, p_237776_) -> {
               p_237776_.write(p_237775_);
            });
         }
      },
      UPDATE_GAME_MODE {
         protected ClientboundPlayerInfoPacket.PlayerUpdate read(FriendlyByteBuf p_179112_) {
            GameProfile gameprofile = new GameProfile(p_179112_.readUUID(), (String)null);
            GameType gametype = GameType.byId(p_179112_.readVarInt());
            return new ClientboundPlayerInfoPacket.PlayerUpdate(gameprofile, 0, gametype, (Component)null, (ProfilePublicKey.Data)null);
         }

         protected void write(FriendlyByteBuf p_179114_, ClientboundPlayerInfoPacket.PlayerUpdate p_179115_) {
            p_179114_.writeUUID(p_179115_.getProfile().getId());
            p_179114_.writeVarInt(p_179115_.getGameMode().getId());
         }
      },
      UPDATE_LATENCY {
         protected ClientboundPlayerInfoPacket.PlayerUpdate read(FriendlyByteBuf p_179120_) {
            GameProfile gameprofile = new GameProfile(p_179120_.readUUID(), (String)null);
            int i = p_179120_.readVarInt();
            return new ClientboundPlayerInfoPacket.PlayerUpdate(gameprofile, i, (GameType)null, (Component)null, (ProfilePublicKey.Data)null);
         }

         protected void write(FriendlyByteBuf p_179122_, ClientboundPlayerInfoPacket.PlayerUpdate p_179123_) {
            p_179122_.writeUUID(p_179123_.getProfile().getId());
            p_179122_.writeVarInt(p_179123_.getLatency());
         }
      },
      UPDATE_DISPLAY_NAME {
         protected ClientboundPlayerInfoPacket.PlayerUpdate read(FriendlyByteBuf p_179128_) {
            GameProfile gameprofile = new GameProfile(p_179128_.readUUID(), (String)null);
            Component component = p_179128_.readNullable(FriendlyByteBuf::readComponent);
            return new ClientboundPlayerInfoPacket.PlayerUpdate(gameprofile, 0, (GameType)null, component, (ProfilePublicKey.Data)null);
         }

         protected void write(FriendlyByteBuf p_179130_, ClientboundPlayerInfoPacket.PlayerUpdate p_179131_) {
            p_179130_.writeUUID(p_179131_.getProfile().getId());
            p_179130_.writeNullable(p_179131_.getDisplayName(), FriendlyByteBuf::writeComponent);
         }
      },
      REMOVE_PLAYER {
         protected ClientboundPlayerInfoPacket.PlayerUpdate read(FriendlyByteBuf p_179136_) {
            GameProfile gameprofile = new GameProfile(p_179136_.readUUID(), (String)null);
            return new ClientboundPlayerInfoPacket.PlayerUpdate(gameprofile, 0, (GameType)null, (Component)null, (ProfilePublicKey.Data)null);
         }

         protected void write(FriendlyByteBuf p_179138_, ClientboundPlayerInfoPacket.PlayerUpdate p_179139_) {
            p_179138_.writeUUID(p_179139_.getProfile().getId());
         }
      };

      protected abstract ClientboundPlayerInfoPacket.PlayerUpdate read(FriendlyByteBuf pBuffer);

      protected abstract void write(FriendlyByteBuf pBuffer, ClientboundPlayerInfoPacket.PlayerUpdate pUpdateData);
   }

   public static class PlayerUpdate {
      private final int latency;
      private final GameType gameMode;
      private final GameProfile profile;
      @Nullable
      private final Component displayName;
      @Nullable
      private final ProfilePublicKey.Data profilePublicKey;

      public PlayerUpdate(GameProfile pProfile, int pLatency, @Nullable GameType pGameMode, @Nullable Component pDisplayName, @Nullable ProfilePublicKey.Data pProfilePublicKey) {
         this.profile = pProfile;
         this.latency = pLatency;
         this.gameMode = pGameMode;
         this.displayName = pDisplayName;
         this.profilePublicKey = pProfilePublicKey;
      }

      public GameProfile getProfile() {
         return this.profile;
      }

      public int getLatency() {
         return this.latency;
      }

      public GameType getGameMode() {
         return this.gameMode;
      }

      @Nullable
      public Component getDisplayName() {
         return this.displayName;
      }

      @Nullable
      public ProfilePublicKey.Data getProfilePublicKey() {
         return this.profilePublicKey;
      }

      public String toString() {
         return MoreObjects.toStringHelper(this).add("latency", this.latency).add("gameMode", this.gameMode).add("profile", this.profile).add("displayName", this.displayName == null ? null : Component.Serializer.toJson(this.displayName)).add("profilePublicKey", this.profilePublicKey).toString();
      }
   }
}