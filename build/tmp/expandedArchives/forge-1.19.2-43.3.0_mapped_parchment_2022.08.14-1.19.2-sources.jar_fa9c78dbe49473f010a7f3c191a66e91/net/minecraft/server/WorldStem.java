package net.minecraft.server;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.world.level.storage.WorldData;

public record WorldStem(CloseableResourceManager resourceManager, ReloadableServerResources dataPackResources, RegistryAccess.Frozen registryAccess, WorldData worldData) implements AutoCloseable {
   public static CompletableFuture<WorldStem> load(WorldLoader.InitConfig p_214416_, WorldLoader.WorldDataSupplier<WorldData> p_214417_, Executor p_214418_, Executor p_214419_) {
      return WorldLoader.load(p_214416_, p_214417_, WorldStem::new, p_214418_, p_214419_);
   }

   public void close() {
      this.resourceManager.close();
   }
}