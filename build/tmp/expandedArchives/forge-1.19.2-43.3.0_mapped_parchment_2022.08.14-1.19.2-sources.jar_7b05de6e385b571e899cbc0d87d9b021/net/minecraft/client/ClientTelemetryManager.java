package net.minecraft.client;

import com.mojang.authlib.minecraft.TelemetryEvent;
import com.mojang.authlib.minecraft.TelemetryPropertyContainer;
import com.mojang.authlib.minecraft.TelemetrySession;
import com.mojang.authlib.minecraft.UserApiService;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.WorldVersion;
import net.minecraft.util.TelemetryConstants;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientTelemetryManager {
   private static final AtomicInteger THREAD_COUNT = new AtomicInteger(1);
   private static final Executor EXECUTOR = Executors.newSingleThreadExecutor((p_193560_) -> {
      Thread thread = new Thread(p_193560_);
      thread.setName("Telemetry-Sender-#" + THREAD_COUNT.getAndIncrement());
      return thread;
   });
   private final Minecraft minecraft;
   private final TelemetrySession telemetrySession;
   private boolean worldLoadEventSent;
   @Nullable
   private ClientTelemetryManager.PlayerInfo playerInfo;
   @Nullable
   private String serverBrand;

   public ClientTelemetryManager(Minecraft pMinecraft, UserApiService pService, Optional<String> pUserId, Optional<String> pClientId, UUID pDeviceSessionId) {
      this.minecraft = pMinecraft;
      if (!SharedConstants.IS_RUNNING_IN_IDE) {
         this.telemetrySession = pService.newTelemetrySession(EXECUTOR);
         TelemetryPropertyContainer telemetrypropertycontainer = this.telemetrySession.globalProperties();
         addOptionalProperty("UserId", pUserId, telemetrypropertycontainer);
         addOptionalProperty("ClientId", pClientId, telemetrypropertycontainer);
         telemetrypropertycontainer.addProperty("deviceSessionId", pDeviceSessionId.toString());
         telemetrypropertycontainer.addProperty("WorldSessionId", UUID.randomUUID().toString());
         this.telemetrySession.eventSetupFunction((p_193549_) -> {
            p_193549_.addProperty("eventTimestampUtc", TelemetryConstants.TIMESTAMP_FORMATTER.format(Instant.now()));
         });
      } else {
         this.telemetrySession = TelemetrySession.DISABLED;
      }

   }

   private static void addOptionalProperty(String pId, Optional<String> pValueOpt, TelemetryPropertyContainer pContainer) {
      pValueOpt.ifPresentOrElse((p_193556_) -> {
         pContainer.addProperty(pId, p_193556_);
      }, () -> {
         pContainer.addNullProperty(pId);
      });
   }

   public void onPlayerInfoReceived(GameType pGameType, boolean pHardcore) {
      this.playerInfo = new ClientTelemetryManager.PlayerInfo(pGameType, pHardcore);
      if (this.serverBrand != null) {
         this.sendWorldLoadEvent(this.playerInfo);
      }

   }

   public void onServerBrandReceived(String pServerBrand) {
      this.serverBrand = pServerBrand;
      if (this.playerInfo != null) {
         this.sendWorldLoadEvent(this.playerInfo);
      }

   }

   private void sendWorldLoadEvent(ClientTelemetryManager.PlayerInfo pInfo) {
      if (!this.worldLoadEventSent) {
         this.worldLoadEventSent = true;
         if (this.telemetrySession.isEnabled()) {
            TelemetryEvent telemetryevent = this.telemetrySession.createNewEvent("WorldLoaded");
            WorldVersion worldversion = SharedConstants.getCurrentVersion();
            telemetryevent.addProperty("build_display_name", worldversion.getId());
            telemetryevent.addProperty("clientModded", Minecraft.checkModStatus().shouldReportAsModified());
            if (this.serverBrand != null) {
               telemetryevent.addProperty("serverModded", !this.serverBrand.equals("vanilla"));
            } else {
               telemetryevent.addNullProperty("serverModded");
            }

            telemetryevent.addProperty("server_type", this.getServerType());
            telemetryevent.addProperty("BuildPlat", Util.getPlatform().telemetryName());
            telemetryevent.addProperty("Plat", System.getProperty("os.name"));
            telemetryevent.addProperty("javaVersion", System.getProperty("java.version"));
            telemetryevent.addProperty("PlayerGameMode", pInfo.getGameModeId());
            telemetryevent.send();
         }
      }
   }

   private String getServerType() {
      if (this.minecraft.isConnectedToRealms()) {
         return "realm";
      } else {
         return this.minecraft.hasSingleplayerServer() ? "local" : "server";
      }
   }

   public void onDisconnect() {
      if (this.playerInfo != null) {
         this.sendWorldLoadEvent(this.playerInfo);
      }

   }

   @OnlyIn(Dist.CLIENT)
   static record PlayerInfo(GameType gameType, boolean hardcore) {
      public int getGameModeId() {
         if (this.hardcore && this.gameType == GameType.SURVIVAL) {
            return 99;
         } else {
            byte b0;
            switch (this.gameType) {
               case SURVIVAL:
                  b0 = 0;
                  break;
               case CREATIVE:
                  b0 = 1;
                  break;
               case ADVENTURE:
                  b0 = 2;
                  break;
               case SPECTATOR:
                  b0 = 6;
                  break;
               default:
                  throw new IncompatibleClassChangeError();
            }

            return b0;
         }
      }
   }
}