package net.minecraft.client;

import com.mojang.bridge.game.GameSession;
import java.util.UUID;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class Session implements GameSession {
   private final int players;
   private final boolean isRemoteServer;
   private final String difficulty;
   private final String gameMode;
   private final UUID id;

   public Session(ClientLevel pLevel, LocalPlayer pPlayer, ClientPacketListener pListener) {
      this.players = pListener.getOnlinePlayers().size();
      this.isRemoteServer = !pListener.getConnection().isMemoryConnection();
      this.difficulty = pLevel.getDifficulty().getKey();
      PlayerInfo playerinfo = pListener.getPlayerInfo(pPlayer.getUUID());
      if (playerinfo != null) {
         this.gameMode = playerinfo.getGameMode().getName();
      } else {
         this.gameMode = "unknown";
      }

      this.id = pListener.getId();
   }

   public int getPlayerCount() {
      return this.players;
   }

   public boolean isRemoteServer() {
      return this.isRemoteServer;
   }

   public String getDifficulty() {
      return this.difficulty;
   }

   public String getGameMode() {
      return this.gameMode;
   }

   public UUID getSessionId() {
      return this.id;
   }
}