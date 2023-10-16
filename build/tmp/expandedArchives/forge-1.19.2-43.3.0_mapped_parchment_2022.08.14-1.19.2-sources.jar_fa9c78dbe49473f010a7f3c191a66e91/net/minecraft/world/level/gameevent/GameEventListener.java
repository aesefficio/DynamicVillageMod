package net.minecraft.world.level.gameevent;

import net.minecraft.server.level.ServerLevel;

public interface GameEventListener {
   default boolean handleEventsImmediately() {
      return false;
   }

   /**
    * Gets the position of the listener itself.
    */
   PositionSource getListenerSource();

   /**
    * Gets the listening radius of the listener. Events within this radius will notify the listener when broadcasted.
    */
   int getListenerRadius();

   boolean handleGameEvent(ServerLevel pLevel, GameEvent.Message pEventMessage);
}