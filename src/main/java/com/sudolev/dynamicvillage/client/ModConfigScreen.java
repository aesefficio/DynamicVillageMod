package com.sudolev.dynamicvillage.client;

import com.sudolev.dynamicvillage.VillageLife;
import net.createmod.catnip.config.ui.BaseConfigScreen;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

/**
 * Registers the in-game config screen reachable from the Mods list -> Create: Dynamic Village -> Config.
 * Reuses Create's (Catnip's) config UI, which is already on the classpath via the Ponder dependency,
 * so no extra library is needed. Client-only: kept in its own class so these client classes never load
 * on a dedicated server.
 */
public final class ModConfigScreen {
   private ModConfigScreen() {
   }

   public static void register() {
      ModLoadingContext.get().registerExtensionPoint(
         ConfigScreenHandler.ConfigScreenFactory.class,
         () -> new ConfigScreenHandler.ConfigScreenFactory(
            (minecraft, parent) -> new BaseConfigScreen(parent, VillageLife.MODID).searchForConfigSpecs()
         )
      );
   }
}
