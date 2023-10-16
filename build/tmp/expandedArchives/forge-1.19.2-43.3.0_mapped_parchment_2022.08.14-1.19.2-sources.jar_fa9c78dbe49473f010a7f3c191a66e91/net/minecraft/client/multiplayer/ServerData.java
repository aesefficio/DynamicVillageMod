package net.minecraft.client.multiplayer;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.text.ParseException;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ServerData {
   private static final Logger LOGGER = LogUtils.getLogger();
   public String name;
   public String ip;
   public Component status;
   public Component motd;
   public long ping;
   public int protocol = SharedConstants.getCurrentVersion().getProtocolVersion();
   public Component version = Component.literal(SharedConstants.getCurrentVersion().getName());
   public boolean pinged;
   public List<Component> playerList = Collections.emptyList();
   private ServerData.ServerPackStatus packStatus = ServerData.ServerPackStatus.PROMPT;
   @Nullable
   private String iconB64;
   /** True if the server is a LAN server */
   private boolean lan;
   @Nullable
   private ServerData.ChatPreview chatPreview;
   private boolean chatPreviewEnabled = true;
   private boolean enforcesSecureChat;
   public net.minecraftforge.client.ExtendedServerListData forgeData = null;

   public ServerData(String pName, String pIp, boolean pLan) {
      this.name = pName;
      this.ip = pIp;
      this.lan = pLan;
   }

   /**
    * Returns an NBTTagCompound with the server's name, IP and maybe acceptTextures.
    */
   public CompoundTag write() {
      CompoundTag compoundtag = new CompoundTag();
      compoundtag.putString("name", this.name);
      compoundtag.putString("ip", this.ip);
      if (this.iconB64 != null) {
         compoundtag.putString("icon", this.iconB64);
      }

      if (this.packStatus == ServerData.ServerPackStatus.ENABLED) {
         compoundtag.putBoolean("acceptTextures", true);
      } else if (this.packStatus == ServerData.ServerPackStatus.DISABLED) {
         compoundtag.putBoolean("acceptTextures", false);
      }

      if (this.chatPreview != null) {
         ServerData.ChatPreview.CODEC.encodeStart(NbtOps.INSTANCE, this.chatPreview).result().ifPresent((p_233812_) -> {
            compoundtag.put("chatPreview", p_233812_);
         });
      }

      return compoundtag;
   }

   public ServerData.ServerPackStatus getResourcePackStatus() {
      return this.packStatus;
   }

   public void setResourcePackStatus(ServerData.ServerPackStatus pPackStatus) {
      this.packStatus = pPackStatus;
   }

   /**
    * Takes an NBTTagCompound with 'name' and 'ip' keys, returns a ServerData instance.
    */
   public static ServerData read(CompoundTag pNbtCompound) {
      ServerData serverdata = new ServerData(pNbtCompound.getString("name"), pNbtCompound.getString("ip"), false);
      if (pNbtCompound.contains("icon", 8)) {
         serverdata.setIconB64(pNbtCompound.getString("icon"));
      }

      if (pNbtCompound.contains("acceptTextures", 1)) {
         if (pNbtCompound.getBoolean("acceptTextures")) {
            serverdata.setResourcePackStatus(ServerData.ServerPackStatus.ENABLED);
         } else {
            serverdata.setResourcePackStatus(ServerData.ServerPackStatus.DISABLED);
         }
      } else {
         serverdata.setResourcePackStatus(ServerData.ServerPackStatus.PROMPT);
      }

      if (pNbtCompound.contains("chatPreview", 10)) {
         ServerData.ChatPreview.CODEC.parse(NbtOps.INSTANCE, pNbtCompound.getCompound("chatPreview")).resultOrPartial(LOGGER::error).ifPresent((p_233807_) -> {
            serverdata.chatPreview = p_233807_;
         });
      }

      return serverdata;
   }

   /**
    * Returns the base-64 encoded representation of the server's icon, or null if not available
    */
   @Nullable
   public String getIconB64() {
      return this.iconB64;
   }

   public static String parseFavicon(String p_233809_) throws ParseException {
      if (p_233809_.startsWith("data:image/png;base64,")) {
         return p_233809_.substring("data:image/png;base64,".length());
      } else {
         throw new ParseException("Unknown format", 0);
      }
   }

   public void setIconB64(@Nullable String pIconB64) {
      this.iconB64 = pIconB64;
   }

   /**
    * Return true if the server is a LAN server
    */
   public boolean isLan() {
      return this.lan;
   }

   public void setPreviewsChat(boolean p_233814_) {
      if (p_233814_ && this.chatPreview == null) {
         this.chatPreview = new ServerData.ChatPreview(false, false);
      } else if (!p_233814_ && this.chatPreview != null) {
         this.chatPreview = null;
      }

   }

   @Nullable
   public ServerData.ChatPreview getChatPreview() {
      return this.chatPreview;
   }

   public void setChatPreviewEnabled(boolean p_233816_) {
      this.chatPreviewEnabled = p_233816_;
   }

   public boolean previewsChat() {
      return this.chatPreviewEnabled && this.chatPreview != null;
   }

   public void setEnforcesSecureChat(boolean p_242972_) {
      this.enforcesSecureChat = p_242972_;
   }

   public boolean enforcesSecureChat() {
      return this.enforcesSecureChat;
   }

   public void copyNameIconFrom(ServerData p_233804_) {
      this.ip = p_233804_.ip;
      this.name = p_233804_.name;
      this.iconB64 = p_233804_.iconB64;
   }

   public void copyFrom(ServerData pServerData) {
      this.copyNameIconFrom(pServerData);
      this.setResourcePackStatus(pServerData.getResourcePackStatus());
      this.lan = pServerData.lan;
      this.chatPreview = Util.mapNullable(pServerData.chatPreview, ServerData.ChatPreview::copy);
      this.enforcesSecureChat = pServerData.enforcesSecureChat;
   }

   @OnlyIn(Dist.CLIENT)
   public static class ChatPreview {
      public static final Codec<ServerData.ChatPreview> CODEC = RecordCodecBuilder.create((p_233828_) -> {
         return p_233828_.group(Codec.BOOL.optionalFieldOf("acknowledged", Boolean.valueOf(false)).forGetter((p_233833_) -> {
            return p_233833_.acknowledged;
         }), Codec.BOOL.optionalFieldOf("toastShown", Boolean.valueOf(false)).forGetter((p_233830_) -> {
            return p_233830_.toastShown;
         })).apply(p_233828_, ServerData.ChatPreview::new);
      });
      private boolean acknowledged;
      private boolean toastShown;

      ChatPreview(boolean p_233824_, boolean p_233825_) {
         this.acknowledged = p_233824_;
         this.toastShown = p_233825_;
      }

      public void acknowledge() {
         this.acknowledged = true;
      }

      public boolean showToast() {
         if (!this.toastShown) {
            this.toastShown = true;
            return true;
         } else {
            return false;
         }
      }

      public boolean isAcknowledged() {
         return this.acknowledged;
      }

      private ServerData.ChatPreview copy() {
         return new ServerData.ChatPreview(this.acknowledged, this.toastShown);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static enum ServerPackStatus {
      ENABLED("enabled"),
      DISABLED("disabled"),
      PROMPT("prompt");

      private final Component name;

      private ServerPackStatus(String pName) {
         this.name = Component.translatable("addServer.resourcePack." + pName);
      }

      public Component getName() {
         return this.name;
      }
   }
}
