package net.minecraft.network.protocol.status;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.Packet;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.LowerCaseEnumTypeAdapterFactory;

public class ClientboundStatusResponsePacket implements Packet<ClientStatusPacketListener> {
   public static final Gson GSON = (new GsonBuilder()).registerTypeAdapter(ServerStatus.Version.class, new ServerStatus.Version.Serializer()).registerTypeAdapter(ServerStatus.Players.class, new ServerStatus.Players.Serializer()).registerTypeAdapter(ServerStatus.class, new ServerStatus.Serializer()).registerTypeHierarchyAdapter(Component.class, new Component.Serializer()).registerTypeHierarchyAdapter(Style.class, new Style.Serializer()).registerTypeAdapterFactory(new LowerCaseEnumTypeAdapterFactory()).create();
   private final ServerStatus status;

   public ClientboundStatusResponsePacket(ServerStatus pStatus) {
      this.status = pStatus;
   }

   public ClientboundStatusResponsePacket(FriendlyByteBuf pBuffer) {
      this.status = GsonHelper.fromJson(GSON, pBuffer.readUtf(32767), ServerStatus.class);
   }

   /**
    * Writes the raw packet data to the data stream.
    */
   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeUtf(this.status.getJson()); //Forge: Let the response cache the JSON
   }

   /**
    * Passes this Packet on to the NetHandler for processing.
    */
   public void handle(ClientStatusPacketListener pHandler) {
      pHandler.handleStatusResponse(this);
   }

   public ServerStatus getStatus() {
      return this.status;
   }
}
