package com.sudolev.dynamicvillage.client;

import com.sudolev.dynamicvillage.VillageLife;
import net.createmod.catnip.config.ui.BaseConfigScreen;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * Client-only registration of the in-game config screen (Mods -> Create: Dynamic Village -> Config).
 * Uses Create's (Catnip's) config UI so the screen matches Create and this mod's other versions; Catnip
 * is already on the classpath via the Ponder dependency, so no extra library is needed. Kept in its own
 * class so the client GUI classes are never loaded on a dedicated server.
 */
public final class ModConfigScreen {
   private ModConfigScreen() {
   }

   public static void register(ModContainer container) {
      container.registerExtensionPoint(
         IConfigScreenFactory.class,
         (modContainer, modListScreen) -> new BaseConfigScreen(modListScreen, VillageLife.MODID).searchForConfigSpecs()
      );
   }
}
