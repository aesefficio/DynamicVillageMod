package net.minecraft.server;

import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.commands.Commands;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.DataPackConfig;

public class WorldLoader {
   public static <D, R> CompletableFuture<R> load(WorldLoader.InitConfig p_214363_, WorldLoader.WorldDataSupplier<D> p_214364_, WorldLoader.ResultFactory<D, R> p_214365_, Executor p_214366_, Executor p_214367_) {
      try {
         Pair<DataPackConfig, CloseableResourceManager> pair = p_214363_.packConfig.createResourceManager();
         CloseableResourceManager closeableresourcemanager = pair.getSecond();
         Pair<D, RegistryAccess.Frozen> pair1 = p_214364_.get(closeableresourcemanager, pair.getFirst());
         D d = pair1.getFirst();
         RegistryAccess.Frozen registryaccess$frozen = pair1.getSecond();
         return ReloadableServerResources.loadResources(closeableresourcemanager, registryaccess$frozen, p_214363_.commandSelection(), p_214363_.functionCompilationLevel(), p_214366_, p_214367_).whenComplete((p_214370_, p_214371_) -> {
            if (p_214371_ != null) {
               closeableresourcemanager.close();
            }

         }).thenApplyAsync((p_214377_) -> {
            p_214377_.updateRegistryTags(registryaccess$frozen);
            return p_214365_.create(closeableresourcemanager, p_214377_, registryaccess$frozen, d);
         }, p_214367_);
      } catch (Exception exception) {
         return CompletableFuture.failedFuture(exception);
      }
   }

   public static record InitConfig(WorldLoader.PackConfig packConfig, Commands.CommandSelection commandSelection, int functionCompilationLevel) {
   }

   public static record PackConfig(PackRepository packRepository, DataPackConfig initialDataPacks, boolean safeMode) {
      public Pair<DataPackConfig, CloseableResourceManager> createResourceManager() {
         DataPackConfig datapackconfig = MinecraftServer.configurePackRepository(this.packRepository, this.initialDataPacks, this.safeMode);
         List<PackResources> list = this.packRepository.openAllSelected();
         CloseableResourceManager closeableresourcemanager = new MultiPackResourceManager(PackType.SERVER_DATA, list);
         return Pair.of(datapackconfig, closeableresourcemanager);
      }
   }

   @FunctionalInterface
   public interface ResultFactory<D, R> {
      R create(CloseableResourceManager p_214408_, ReloadableServerResources p_214409_, RegistryAccess.Frozen p_214410_, D p_214411_);
   }

   @FunctionalInterface
   public interface WorldDataSupplier<D> {
      Pair<D, RegistryAccess.Frozen> get(ResourceManager p_214413_, DataPackConfig p_214414_);
   }
}