package com.sudolev.dynamicvillage.client;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * Client-only registration of the in-game config screen (Mods -> Create: Dynamic Village -> Config).
 * Kept in its own class so the client GUI classes are never loaded on a dedicated server.
 */
public final class ModConfigScreen {
   private ModConfigScreen() {
   }

   public static void register(ModContainer container) {
      container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
   }
}
