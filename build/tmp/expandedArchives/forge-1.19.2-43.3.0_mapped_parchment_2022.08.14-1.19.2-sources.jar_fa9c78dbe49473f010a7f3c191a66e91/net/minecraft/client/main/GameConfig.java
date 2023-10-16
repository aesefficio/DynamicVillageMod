package net.minecraft.client.main;

import com.mojang.authlib.properties.PropertyMap;
import com.mojang.blaze3d.platform.DisplayData;
import java.io.File;
import java.net.Proxy;
import javax.annotation.Nullable;
import net.minecraft.client.User;
import net.minecraft.client.resources.AssetIndex;
import net.minecraft.client.resources.DirectAssetIndex;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GameConfig {
   public final GameConfig.UserData user;
   public final DisplayData display;
   public final GameConfig.FolderData location;
   public final GameConfig.GameData game;
   public final GameConfig.ServerData server;

   public GameConfig(GameConfig.UserData pUser, DisplayData pDisplay, GameConfig.FolderData pLocation, GameConfig.GameData pGame, GameConfig.ServerData pServer) {
      this.user = pUser;
      this.display = pDisplay;
      this.location = pLocation;
      this.game = pGame;
      this.server = pServer;
   }

   @OnlyIn(Dist.CLIENT)
   public static class FolderData {
      public final File gameDirectory;
      public final File resourcePackDirectory;
      public final File assetDirectory;
      @Nullable
      public final String assetIndex;

      public FolderData(File pGameDirectory, File pResourcePackDirectory, File pAssetDirectory, @Nullable String pAssetIndex) {
         this.gameDirectory = pGameDirectory;
         this.resourcePackDirectory = pResourcePackDirectory;
         this.assetDirectory = pAssetDirectory;
         this.assetIndex = pAssetIndex;
      }

      public AssetIndex getAssetIndex() {
         return (AssetIndex)(this.assetIndex == null ? new DirectAssetIndex(this.assetDirectory) : new AssetIndex(this.assetDirectory, this.assetIndex));
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class GameData {
      public final boolean demo;
      public final String launchVersion;
      public final String versionType;
      public final boolean disableMultiplayer;
      public final boolean disableChat;

      public GameData(boolean pDemo, String pLaunchVersion, String pVersionType, boolean pDisableMultiplayer, boolean pDisableChat) {
         this.demo = pDemo;
         this.launchVersion = pLaunchVersion;
         this.versionType = pVersionType;
         this.disableMultiplayer = pDisableMultiplayer;
         this.disableChat = pDisableChat;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class ServerData {
      @Nullable
      public final String hostname;
      public final int port;

      public ServerData(@Nullable String pHostname, int pPort) {
         this.hostname = pHostname;
         this.port = pPort;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class UserData {
      public final User user;
      public final PropertyMap userProperties;
      public final PropertyMap profileProperties;
      public final Proxy proxy;

      public UserData(User pUser, PropertyMap pUserProperties, PropertyMap pProfileProperties, Proxy pProxy) {
         this.user = pUser;
         this.userProperties = pUserProperties;
         this.profileProperties = pProfileProperties;
         this.proxy = pProxy;
      }
   }
}