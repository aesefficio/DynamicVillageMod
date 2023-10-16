package net.minecraft.network;

import net.minecraft.network.chat.Component;

/**
 * Describes how packets are handled. There are various implementations of this class for each possible protocol (e.g.
 * PLAY, CLIENTBOUND; PLAY, SERVERBOUND; etc.)
 */
public interface PacketListener {
   /**
    * Invoked when disconnecting, the parameter is a ChatComponent describing the reason for termination
    */
   void onDisconnect(Component pReason);

   /**
    * Returns this the NetworkManager instance registered with this NetworkHandlerPlayClient
    */
   Connection getConnection();

   default boolean shouldPropagateHandlingExceptions() {
      return true;
   }
}