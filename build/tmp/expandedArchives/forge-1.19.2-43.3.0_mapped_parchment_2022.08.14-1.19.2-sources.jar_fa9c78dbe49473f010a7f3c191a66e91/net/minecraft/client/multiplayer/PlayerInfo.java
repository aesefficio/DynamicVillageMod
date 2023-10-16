package net.minecraft.client.multiplayer;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import com.mojang.logging.LogUtils;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.SignedMessageValidator;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.SignatureValidator;
import net.minecraft.world.entity.player.ProfilePublicKey;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class PlayerInfo {
   private static final Logger LOGGER = LogUtils.getLogger();
   /** The GameProfile for the player represented by this NetworkPlayerInfo instance */
   private final GameProfile profile;
   private final Map<MinecraftProfileTexture.Type, ResourceLocation> textureLocations = Maps.newEnumMap(MinecraftProfileTexture.Type.class);
   private GameType gameMode;
   private int latency;
   private boolean pendingTextures;
   @Nullable
   private String skinModel;
   /** When this is non-null, it is displayed instead of the player's real name */
   @Nullable
   private Component tabListDisplayName;
   private int lastHealth;
   private int displayHealth;
   private long lastHealthTime;
   private long healthBlinkTime;
   private long renderVisibilityId;
   @Nullable
   private final ProfilePublicKey profilePublicKey;
   private final SignedMessageValidator messageValidator;

   public PlayerInfo(ClientboundPlayerInfoPacket.PlayerUpdate pPlayerUpdatePacket, SignatureValidator p_233763_, boolean p_242970_) {
      this.profile = pPlayerUpdatePacket.getProfile();
      this.gameMode = pPlayerUpdatePacket.getGameMode();
      this.latency = pPlayerUpdatePacket.getLatency();
      this.tabListDisplayName = pPlayerUpdatePacket.getDisplayName();
      ProfilePublicKey profilepublickey = null;

      try {
         ProfilePublicKey.Data profilepublickey$data = pPlayerUpdatePacket.getProfilePublicKey();
         if (profilepublickey$data != null) {
            profilepublickey = ProfilePublicKey.createValidated(p_233763_, this.profile.getId(), profilepublickey$data, ProfilePublicKey.EXPIRY_GRACE_PERIOD);
         }
      } catch (Exception exception) {
         LOGGER.error("Failed to validate publicKey property for profile {}", this.profile.getId(), exception);
      }

      this.profilePublicKey = profilepublickey;
      this.messageValidator = SignedMessageValidator.create(profilepublickey, p_242970_);
   }

   /**
    * Returns the GameProfile for the player represented by this NetworkPlayerInfo instance
    */
   public GameProfile getProfile() {
      return this.profile;
   }

   @Nullable
   public ProfilePublicKey getProfilePublicKey() {
      return this.profilePublicKey;
   }

   public SignedMessageValidator getMessageValidator() {
      return this.messageValidator;
   }

   @Nullable
   public GameType getGameMode() {
      return this.gameMode;
   }

   protected void setGameMode(GameType pGameMode) {
      net.minecraftforge.client.ForgeHooksClient.onClientChangeGameType(this, this.gameMode, pGameMode);
      this.gameMode = pGameMode;
   }

   public int getLatency() {
      return this.latency;
   }

   protected void setLatency(int pLatency) {
      this.latency = pLatency;
   }

   public boolean isCapeLoaded() {
      return this.getCapeLocation() != null;
   }

   public boolean isSkinLoaded() {
      return this.getSkinLocation() != null;
   }

   public String getModelName() {
      return this.skinModel == null ? DefaultPlayerSkin.getSkinModelName(this.profile.getId()) : this.skinModel;
   }

   public ResourceLocation getSkinLocation() {
      this.registerTextures();
      return MoreObjects.firstNonNull(this.textureLocations.get(Type.SKIN), DefaultPlayerSkin.getDefaultSkin(this.profile.getId()));
   }

   @Nullable
   public ResourceLocation getCapeLocation() {
      this.registerTextures();
      return this.textureLocations.get(Type.CAPE);
   }

   /**
    * Gets the special Elytra texture for the player.
    */
   @Nullable
   public ResourceLocation getElytraLocation() {
      this.registerTextures();
      return this.textureLocations.get(Type.ELYTRA);
   }

   @Nullable
   public PlayerTeam getTeam() {
      return Minecraft.getInstance().level.getScoreboard().getPlayersTeam(this.getProfile().getName());
   }

   protected void registerTextures() {
      synchronized(this) {
         if (!this.pendingTextures) {
            this.pendingTextures = true;
            Minecraft.getInstance().getSkinManager().registerSkins(this.profile, (p_105320_, p_105321_, p_105322_) -> {
               this.textureLocations.put(p_105320_, p_105321_);
               if (p_105320_ == Type.SKIN) {
                  this.skinModel = p_105322_.getMetadata("model");
                  if (this.skinModel == null) {
                     this.skinModel = "default";
                  }
               }

            }, true);
         }

      }
   }

   public void setTabListDisplayName(@Nullable Component pDisplayName) {
      this.tabListDisplayName = pDisplayName;
   }

   @Nullable
   public Component getTabListDisplayName() {
      return this.tabListDisplayName;
   }

   public int getLastHealth() {
      return this.lastHealth;
   }

   public void setLastHealth(int pLastHealth) {
      this.lastHealth = pLastHealth;
   }

   public int getDisplayHealth() {
      return this.displayHealth;
   }

   public void setDisplayHealth(int pDisplayHealth) {
      this.displayHealth = pDisplayHealth;
   }

   public long getLastHealthTime() {
      return this.lastHealthTime;
   }

   public void setLastHealthTime(long pLastHealthTime) {
      this.lastHealthTime = pLastHealthTime;
   }

   public long getHealthBlinkTime() {
      return this.healthBlinkTime;
   }

   public void setHealthBlinkTime(long pHealthBlinkTime) {
      this.healthBlinkTime = pHealthBlinkTime;
   }

   public long getRenderVisibilityId() {
      return this.renderVisibilityId;
   }

   public void setRenderVisibilityId(long pRenderVisibilityId) {
      this.renderVisibilityId = pRenderVisibilityId;
   }
}
