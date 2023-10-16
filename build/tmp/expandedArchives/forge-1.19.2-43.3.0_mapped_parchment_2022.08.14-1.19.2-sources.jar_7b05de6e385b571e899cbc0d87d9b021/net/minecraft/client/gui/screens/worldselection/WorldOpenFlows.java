package net.minecraft.client.gui.screens.worldselection;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.BackupConfirmScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.DatapackLoadFailureScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.commands.Commands;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.FolderRepositorySource;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.storage.WorldData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class WorldOpenFlows {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Minecraft minecraft;
   private final LevelStorageSource levelSource;

   public WorldOpenFlows(Minecraft pMinecraft, LevelStorageSource pLevelSource) {
      this.minecraft = pMinecraft;
      this.levelSource = pLevelSource;
   }

   public void loadLevel(Screen pLastScreen, String pLevelName) {
      this.doLoadLevel(pLastScreen, pLevelName, false, true);
   }

   public void createFreshLevel(String pLevelName, LevelSettings pLevelSettings, RegistryAccess pRegistryAccess, WorldGenSettings pWorldGenSettings) {
      LevelStorageSource.LevelStorageAccess levelstoragesource$levelstorageaccess = this.createWorldAccess(pLevelName);
      if (levelstoragesource$levelstorageaccess != null) {
         PackRepository packrepository = createPackRepository(levelstoragesource$levelstorageaccess);
         DataPackConfig datapackconfig = pLevelSettings.getDataPackConfig();

         try {
            WorldLoader.PackConfig worldloader$packconfig = new WorldLoader.PackConfig(packrepository, datapackconfig, false);
            WorldStem worldstem = this.loadWorldStem(worldloader$packconfig, (p_233103_, p_233104_) -> {
               return Pair.of(new PrimaryLevelData(pLevelSettings, pWorldGenSettings, Lifecycle.stable()), pRegistryAccess.freeze());
            });
            this.minecraft.doWorldLoad(pLevelName, levelstoragesource$levelstorageaccess, packrepository, worldstem);
         } catch (Exception exception) {
            LOGGER.warn("Failed to load datapacks, can't proceed with server load", (Throwable)exception);
            safeCloseAccess(levelstoragesource$levelstorageaccess, pLevelName);
         }

      }
   }

   @Nullable
   private LevelStorageSource.LevelStorageAccess createWorldAccess(String pLevelName) {
      try {
         return this.levelSource.createAccess(pLevelName);
      } catch (IOException ioexception) {
         LOGGER.warn("Failed to read level {} data", pLevelName, ioexception);
         SystemToast.onWorldAccessFailure(this.minecraft, pLevelName);
         this.minecraft.setScreen((Screen)null);
         return null;
      }
   }

   public void createLevelFromExistingSettings(LevelStorageSource.LevelStorageAccess pLevelStorage, ReloadableServerResources pResources, RegistryAccess.Frozen p_233110_, WorldData pWorldData) {
      PackRepository packrepository = createPackRepository(pLevelStorage);
      CloseableResourceManager closeableresourcemanager = (new WorldLoader.PackConfig(packrepository, pWorldData.getDataPackConfig(), false)).createResourceManager().getSecond();
      this.minecraft.doWorldLoad(pLevelStorage.getLevelId(), pLevelStorage, packrepository, new WorldStem(closeableresourcemanager, pResources, p_233110_, pWorldData));
   }

   private static PackRepository createPackRepository(LevelStorageSource.LevelStorageAccess p_233106_) {
      return new PackRepository(PackType.SERVER_DATA, new ServerPacksSource(), new FolderRepositorySource(p_233106_.getLevelPath(LevelResource.DATAPACK_DIR).toFile(), PackSource.WORLD));
   }

   private WorldStem loadWorldStem(LevelStorageSource.LevelStorageAccess pLevelStorage, boolean pSafeMode, PackRepository pPackRepository) throws Exception {
      DataPackConfig datapackconfig = pLevelStorage.getDataPacks();
      if (datapackconfig == null) {
         throw new IllegalStateException("Failed to load data pack config");
      } else {
         WorldLoader.PackConfig worldloader$packconfig = new WorldLoader.PackConfig(pPackRepository, datapackconfig, pSafeMode);
         return this.loadWorldStem(worldloader$packconfig, (p_233114_, p_233115_) -> {
            RegistryAccess.Writable registryaccess$writable = RegistryAccess.builtinCopy();
            DynamicOps<Tag> dynamicops = RegistryOps.createAndLoad(NbtOps.INSTANCE, registryaccess$writable, p_233114_);
            WorldData worlddata = pLevelStorage.getDataTag(dynamicops, p_233115_, registryaccess$writable.allElementsLifecycle());
            if (worlddata == null) {
               throw new IllegalStateException("Failed to load world");
            } else {
               return Pair.of(worlddata, registryaccess$writable.freeze());
            }
         });
      }
   }

   public WorldStem loadWorldStem(LevelStorageSource.LevelStorageAccess pLevelStorage, boolean pSafeMode) throws Exception {
      PackRepository packrepository = createPackRepository(pLevelStorage);
      return this.loadWorldStem(pLevelStorage, pSafeMode, packrepository);
   }

   private WorldStem loadWorldStem(WorldLoader.PackConfig p_233097_, WorldLoader.WorldDataSupplier<WorldData> p_233098_) throws Exception {
      WorldLoader.InitConfig worldloader$initconfig = new WorldLoader.InitConfig(p_233097_, Commands.CommandSelection.INTEGRATED, 2);
      CompletableFuture<WorldStem> completablefuture = WorldStem.load(worldloader$initconfig, p_233098_, Util.backgroundExecutor(), this.minecraft);
      this.minecraft.managedBlock(completablefuture::isDone);
      return completablefuture.get();
   }

   private void doLoadLevel(Screen pLastScreen, String pLevelName, boolean pSafeMode, boolean p_233149_) {
      // FORGE: Patch in overload to reduce further patching
      this.doLoadLevel(pLastScreen, pLevelName, pSafeMode, p_233149_, false);
   }

   // FORGE: Patch in confirmExperimentalWarning which confirms the experimental warning when true
   private void doLoadLevel(Screen pLastScreen, String pLevelName, boolean pSafeMode, boolean p_233149_, boolean confirmExperimentalWarning) {
      LevelStorageSource.LevelStorageAccess levelstoragesource$levelstorageaccess = this.createWorldAccess(pLevelName);
      if (levelstoragesource$levelstorageaccess != null) {
         PackRepository packrepository = createPackRepository(levelstoragesource$levelstorageaccess);

         WorldStem worldstem;
         try {
            levelstoragesource$levelstorageaccess.readAdditionalLevelSaveData(); // Read extra (e.g. modded) data from the world before creating it
            worldstem = this.loadWorldStem(levelstoragesource$levelstorageaccess, pSafeMode, packrepository);
            if (confirmExperimentalWarning && worldstem.worldData() instanceof PrimaryLevelData pld) {
               pld.withConfirmedWarning(true);
            }
         } catch (Exception exception) {
            LOGGER.warn("Failed to load datapacks, can't proceed with server load", (Throwable)exception);
            this.minecraft.setScreen(new DatapackLoadFailureScreen(() -> {
               this.doLoadLevel(pLastScreen, pLevelName, true, p_233149_);
            }));
            safeCloseAccess(levelstoragesource$levelstorageaccess, pLevelName);
            return;
         }

         WorldData worlddata = worldstem.worldData();
         boolean flag = worlddata.worldGenSettings().isOldCustomizedWorld();
         boolean flag1 = worlddata.worldGenSettingsLifecycle() != Lifecycle.stable();
         // Forge: Skip confirmation if it has been done already for this world
         boolean skipConfirmation = worlddata instanceof PrimaryLevelData pld && pld.hasConfirmedExperimentalWarning();
         if (skipConfirmation || !p_233149_ || !flag && !flag1) {
            this.minecraft.getClientPackSource().loadBundledResourcePack(levelstoragesource$levelstorageaccess).thenApply((p_233177_) -> {
               return true;
            }).exceptionallyComposeAsync((p_233183_) -> {
               LOGGER.warn("Failed to load pack: ", p_233183_);
               return this.promptBundledPackLoadFailure();
            }, this.minecraft).thenAcceptAsync((p_233168_) -> {
               if (p_233168_) {
                  this.minecraft.doWorldLoad(pLevelName, levelstoragesource$levelstorageaccess, packrepository, worldstem);
               } else {
                  worldstem.close();
                  safeCloseAccess(levelstoragesource$levelstorageaccess, pLevelName);
                  this.minecraft.getClientPackSource().clearServerPack().thenRunAsync(() -> {
                     this.minecraft.setScreen(pLastScreen);
                  }, this.minecraft);
               }

            }, this.minecraft).exceptionally((p_233175_) -> {
               this.minecraft.delayCrash(CrashReport.forThrowable(p_233175_, "Load world"));
               return null;
            });
         } else {
            if (flag) // Forge: For legacy world options, let vanilla handle it.
            this.askForBackup(pLastScreen, pLevelName, flag, () -> {
               this.doLoadLevel(pLastScreen, pLevelName, pSafeMode, false);
            });
            else net.minecraftforge.client.ForgeHooksClient.createWorldConfirmationScreen(() -> this.doLoadLevel(pLastScreen, pLevelName, pSafeMode, false, true));
            worldstem.close();
            safeCloseAccess(levelstoragesource$levelstorageaccess, pLevelName);
         }
      }
   }

   private CompletableFuture<Boolean> promptBundledPackLoadFailure() {
      CompletableFuture<Boolean> completablefuture = new CompletableFuture<>();
      this.minecraft.setScreen(new ConfirmScreen(completablefuture::complete, Component.translatable("multiplayer.texturePrompt.failure.line1"), Component.translatable("multiplayer.texturePrompt.failure.line2"), CommonComponents.GUI_PROCEED, CommonComponents.GUI_CANCEL));
      return completablefuture;
   }

   private static void safeCloseAccess(LevelStorageSource.LevelStorageAccess pLevelStorage, String pLevelName) {
      try {
         pLevelStorage.close();
      } catch (IOException ioexception) {
         LOGGER.warn("Failed to unlock access to level {}", pLevelName, ioexception);
      }

   }

   private void askForBackup(Screen pLastScreen, String pLevelName, boolean pCustomized, Runnable p_233144_) {
      Component component;
      Component component1;
      if (pCustomized) {
         component = Component.translatable("selectWorld.backupQuestion.customized");
         component1 = Component.translatable("selectWorld.backupWarning.customized");
      } else {
         component = Component.translatable("selectWorld.backupQuestion.experimental");
         component1 = Component.translatable("selectWorld.backupWarning.experimental");
      }

      this.minecraft.setScreen(new BackupConfirmScreen(pLastScreen, (p_233172_, p_233173_) -> {
         if (p_233172_) {
            EditWorldScreen.makeBackupAndShowToast(this.levelSource, pLevelName);
         }

         p_233144_.run();
      }, component, component1, false));
   }

   public static void confirmWorldCreation(Minecraft pMinecraft, CreateWorldScreen p_233128_, Lifecycle p_233129_, Runnable p_233130_) {
      BooleanConsumer booleanconsumer = (p_233154_) -> {
         if (p_233154_) {
            p_233130_.run();
         } else {
            pMinecraft.setScreen(p_233128_);
         }

      };
      if (p_233129_ == Lifecycle.stable()) {
         p_233130_.run();
      } else if (p_233129_ == Lifecycle.experimental()) {
         pMinecraft.setScreen(new ConfirmScreen(booleanconsumer, Component.translatable("selectWorld.import_worldgen_settings.experimental.title"), Component.translatable("selectWorld.import_worldgen_settings.experimental.question")));
      } else {
         pMinecraft.setScreen(new ConfirmScreen(booleanconsumer, Component.translatable("selectWorld.import_worldgen_settings.deprecated.title"), Component.translatable("selectWorld.import_worldgen_settings.deprecated.question")));
      }

   }
}
