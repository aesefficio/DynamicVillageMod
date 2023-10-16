package net.minecraft.client.gui.screens.worldselection;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.FileUtil;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.FolderRepositorySource;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.storage.WorldData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.mutable.MutableObject;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class CreateWorldScreen extends Screen {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String TEMP_WORLD_PREFIX = "mcworld-";
   private static final Component GAME_MODEL_LABEL = Component.translatable("selectWorld.gameMode");
   private static final Component SEED_LABEL = Component.translatable("selectWorld.enterSeed");
   private static final Component SEED_INFO = Component.translatable("selectWorld.seedInfo");
   private static final Component NAME_LABEL = Component.translatable("selectWorld.enterName");
   private static final Component OUTPUT_DIR_INFO = Component.translatable("selectWorld.resultFolder");
   private static final Component COMMANDS_INFO = Component.translatable("selectWorld.allowCommands.info");
   private static final Component PREPARING_WORLD_DATA = Component.translatable("createWorld.preparing");
   @Nullable
   private final Screen lastScreen;
   private EditBox nameEdit;
   String resultFolder;
   private CreateWorldScreen.SelectedGameMode gameMode = CreateWorldScreen.SelectedGameMode.SURVIVAL;
   @Nullable
   private CreateWorldScreen.SelectedGameMode oldGameMode;
   private Difficulty difficulty = Difficulty.NORMAL;
   /** If cheats are allowed */
   private boolean commands;
   /**
    * User explicitly clicked "Allow Cheats" at some point
    * Prevents value changes due to changing game mode
    */
   private boolean commandsChanged;
   /** Set to true when "hardcore" is the currently-selected gamemode */
   public boolean hardCore;
   protected DataPackConfig dataPacks;
   @Nullable
   private Path tempDataPackDir;
   @Nullable
   private PackRepository tempDataPackRepository;
   private boolean worldGenSettingsVisible;
   private Button createButton;
   private CycleButton<CreateWorldScreen.SelectedGameMode> modeButton;
   private CycleButton<Difficulty> difficultyButton;
   private Button moreOptionsButton;
   private Button gameRulesButton;
   private Button dataPacksButton;
   private CycleButton<Boolean> commandsButton;
   private Component gameModeHelp1;
   private Component gameModeHelp2;
   private String initName;
   private GameRules gameRules = new GameRules();
   public final WorldGenSettingsComponent worldGenSettingsComponent;

   public static void openFresh(Minecraft p_232897_, @Nullable Screen p_232898_) {
      queueLoadScreen(p_232897_, PREPARING_WORLD_DATA);
      PackRepository packrepository = new PackRepository(PackType.SERVER_DATA, new ServerPacksSource());
      WorldLoader.InitConfig worldloader$initconfig = createDefaultLoadConfig(packrepository, new DataPackConfig(ImmutableList.of("vanilla"), ImmutableList.of())); // FORGE: Load vanilla fallback with vanilla datapacks.
      CompletableFuture<WorldCreationContext> completablefuture = WorldLoader.load(worldloader$initconfig, (p_232935_, p_232936_) -> {
         RegistryAccess.Frozen registryaccess$frozen = RegistryAccess.builtinCopy().freeze();
         WorldGenSettings worldgensettings = WorldPresets.createNormalWorldFromPreset(registryaccess$frozen);
         return Pair.of(worldgensettings, registryaccess$frozen);
      }, (p_232881_, p_232882_, p_232883_, p_232884_) -> {
         p_232881_.close();
         return new WorldCreationContext(p_232884_, Lifecycle.stable(), p_232883_, p_232882_);
      }, Util.backgroundExecutor(), p_232897_);
      p_232897_.managedBlock(completablefuture::isDone);
      // FORGE: Force load mods' datapacks after setting screen and ensure datapack selection reverts to vanilla if invalid.
      CreateWorldScreen createWorldScreen = new CreateWorldScreen(p_232898_, new DataPackConfig(ImmutableList.of("vanilla"), ImmutableList.of()), new WorldGenSettingsComponent(completablefuture.join(), Optional.of(WorldPresets.NORMAL), OptionalLong.empty()));
      p_232897_.setScreen(createWorldScreen);
      createWorldScreen.tryApplyNewDataPacks(packrepository);
   }

   public static CreateWorldScreen createFromExisting(@Nullable Screen pLastScreen, WorldStem pWorldStem, @Nullable Path pTempDataPackDir) {
      WorldData worlddata = pWorldStem.worldData();
      LevelSettings levelsettings = worlddata.getLevelSettings();
      WorldGenSettings worldgensettings = worlddata.worldGenSettings();
      RegistryAccess.Frozen registryaccess$frozen = pWorldStem.registryAccess();
      WorldCreationContext worldcreationcontext = new WorldCreationContext(worldgensettings, worlddata.worldGenSettingsLifecycle(), registryaccess$frozen, pWorldStem.dataPackResources());
      DataPackConfig datapackconfig = levelsettings.getDataPackConfig();
      CreateWorldScreen createworldscreen = new CreateWorldScreen(pLastScreen, datapackconfig, new WorldGenSettingsComponent(worldcreationcontext, WorldPresets.fromSettings(worldgensettings), OptionalLong.of(worldgensettings.seed())));
      createworldscreen.initName = levelsettings.levelName();
      createworldscreen.commands = levelsettings.allowCommands();
      createworldscreen.commandsChanged = true;
      createworldscreen.difficulty = levelsettings.difficulty();
      createworldscreen.gameRules.assignFrom(levelsettings.gameRules(), (MinecraftServer)null);
      if (levelsettings.hardcore()) {
         createworldscreen.gameMode = CreateWorldScreen.SelectedGameMode.HARDCORE;
      } else if (levelsettings.gameType().isSurvival()) {
         createworldscreen.gameMode = CreateWorldScreen.SelectedGameMode.SURVIVAL;
      } else if (levelsettings.gameType().isCreative()) {
         createworldscreen.gameMode = CreateWorldScreen.SelectedGameMode.CREATIVE;
      }

      createworldscreen.tempDataPackDir = pTempDataPackDir;
      return createworldscreen;
   }

   private CreateWorldScreen(@Nullable Screen pLastScreen, DataPackConfig pDataPacks, WorldGenSettingsComponent pWorldGenSettingsComponent) {
      super(Component.translatable("selectWorld.create"));
      this.lastScreen = pLastScreen;
      this.initName = I18n.get("selectWorld.newWorld");
      this.dataPacks = pDataPacks;
      this.worldGenSettingsComponent = pWorldGenSettingsComponent;
   }

   public void tick() {
      this.nameEdit.tick();
      this.worldGenSettingsComponent.tick();
   }

   protected void init() {
      this.minecraft.keyboardHandler.setSendRepeatsToGui(true);
      this.nameEdit = new EditBox(this.font, this.width / 2 - 100, 60, 200, 20, Component.translatable("selectWorld.enterName")) {
         protected MutableComponent createNarrationMessage() {
            return CommonComponents.joinForNarration(super.createNarrationMessage(), Component.translatable("selectWorld.resultFolder")).append(" ").append(CreateWorldScreen.this.resultFolder);
         }
      };
      this.nameEdit.setValue(this.initName);
      this.nameEdit.setResponder((p_232916_) -> {
         this.initName = p_232916_;
         this.createButton.active = !this.nameEdit.getValue().isEmpty();
         this.updateResultFolder();
      });
      this.addWidget(this.nameEdit);
      int i = this.width / 2 - 155;
      int j = this.width / 2 + 5;
      this.modeButton = this.addRenderableWidget(CycleButton.builder(CreateWorldScreen.SelectedGameMode::getDisplayName).withValues(CreateWorldScreen.SelectedGameMode.SURVIVAL, CreateWorldScreen.SelectedGameMode.HARDCORE, CreateWorldScreen.SelectedGameMode.CREATIVE).withInitialValue(this.gameMode).withCustomNarration((p_232940_) -> {
         return AbstractWidget.wrapDefaultNarrationMessage(p_232940_.getMessage()).append(CommonComponents.NARRATION_SEPARATOR).append(this.gameModeHelp1).append(" ").append(this.gameModeHelp2);
      }).create(i, 100, 150, 20, GAME_MODEL_LABEL, (p_232910_, p_232911_) -> {
         this.setGameMode(p_232911_);
      }));
      this.difficultyButton = this.addRenderableWidget(CycleButton.builder(Difficulty::getDisplayName).withValues(Difficulty.values()).withInitialValue(this.getEffectiveDifficulty()).create(j, 100, 150, 20, Component.translatable("options.difficulty"), (p_232907_, p_232908_) -> {
         this.difficulty = p_232908_;
      }));
      this.commandsButton = this.addRenderableWidget(CycleButton.onOffBuilder(this.commands && !this.hardCore).withCustomNarration((p_232905_) -> {
         return CommonComponents.joinForNarration(p_232905_.createDefaultNarrationMessage(), Component.translatable("selectWorld.allowCommands.info"));
      }).create(i, 151, 150, 20, Component.translatable("selectWorld.allowCommands"), (p_232913_, p_232914_) -> {
         this.commandsChanged = true;
         this.commands = p_232914_;
      }));
      this.dataPacksButton = this.addRenderableWidget(new Button(j, 151, 150, 20, Component.translatable("selectWorld.dataPacks"), (p_232947_) -> {
         this.openDataPackSelectionScreen();
      }));
      this.gameRulesButton = this.addRenderableWidget(new Button(i, 185, 150, 20, Component.translatable("selectWorld.gameRules"), (p_170188_) -> {
         this.minecraft.setScreen(new EditGameRulesScreen(this.gameRules.copy(), (p_232929_) -> {
            this.minecraft.setScreen(this);
            p_232929_.ifPresent((p_232892_) -> {
               this.gameRules = p_232892_;
            });
         }));
      }));
      this.worldGenSettingsComponent.init(this, this.minecraft, this.font);
      this.moreOptionsButton = this.addRenderableWidget(new Button(j, 185, 150, 20, Component.translatable("selectWorld.moreWorldOptions"), (p_170158_) -> {
         this.toggleWorldGenSettingsVisibility();
      }));
      this.createButton = this.addRenderableWidget(new Button(i, this.height - 28, 150, 20, Component.translatable("selectWorld.create"), (p_232938_) -> {
         this.onCreate();
      }));
      this.createButton.active = !this.initName.isEmpty();
      this.addRenderableWidget(new Button(j, this.height - 28, 150, 20, CommonComponents.GUI_CANCEL, (p_232903_) -> {
         this.popScreen();
      }));
      this.refreshWorldGenSettingsVisibility();
      this.setInitialFocus(this.nameEdit);
      this.setGameMode(this.gameMode);
      this.updateResultFolder();
   }

   private Difficulty getEffectiveDifficulty() {
      return this.gameMode == CreateWorldScreen.SelectedGameMode.HARDCORE ? Difficulty.HARD : this.difficulty;
   }

   private void updateGameModeHelp() {
      this.gameModeHelp1 = Component.translatable("selectWorld.gameMode." + this.gameMode.name + ".line1");
      this.gameModeHelp2 = Component.translatable("selectWorld.gameMode." + this.gameMode.name + ".line2");
   }

   /**
    * Determine a save-directory name from the world name
    */
   private void updateResultFolder() {
      this.resultFolder = this.nameEdit.getValue().trim();
      if (this.resultFolder.isEmpty()) {
         this.resultFolder = "World";
      }

      try {
         this.resultFolder = FileUtil.findAvailableName(this.minecraft.getLevelSource().getBaseDir(), this.resultFolder, "");
      } catch (Exception exception1) {
         this.resultFolder = "World";

         try {
            this.resultFolder = FileUtil.findAvailableName(this.minecraft.getLevelSource().getBaseDir(), this.resultFolder, "");
         } catch (Exception exception) {
            throw new RuntimeException("Could not create save folder", exception);
         }
      }

   }

   public void removed() {
      this.minecraft.keyboardHandler.setSendRepeatsToGui(false);
   }

   private static void queueLoadScreen(Minecraft p_232900_, Component p_232901_) {
      p_232900_.forceSetScreen(new GenericDirtMessageScreen(p_232901_));
   }

   private void onCreate() {
      WorldOpenFlows.confirmWorldCreation(this.minecraft, this, this.worldGenSettingsComponent.settings().worldSettingsStability(), this::createNewWorld);
   }

   private void createNewWorld() {
      queueLoadScreen(this.minecraft, PREPARING_WORLD_DATA);
      Optional<LevelStorageSource.LevelStorageAccess> optional = this.createNewWorldDirectory();
      if (!optional.isEmpty()) {
         this.removeTempDataPackDir();
         WorldCreationContext worldcreationcontext = this.worldGenSettingsComponent.createFinalSettings(this.hardCore);
         LevelSettings levelsettings = this.createLevelSettings(worldcreationcontext.worldGenSettings().isDebug());
         WorldData worlddata = new PrimaryLevelData(levelsettings, worldcreationcontext.worldGenSettings(), worldcreationcontext.worldSettingsStability());
         this.minecraft.createWorldOpenFlows().createLevelFromExistingSettings(optional.get(), worldcreationcontext.dataPackResources(), worldcreationcontext.registryAccess(), worlddata);
      }
   }

   private LevelSettings createLevelSettings(boolean pDebug) {
      String s = this.nameEdit.getValue().trim();
      if (pDebug) {
         GameRules gamerules = new GameRules();
         gamerules.getRule(GameRules.RULE_DAYLIGHT).set(false, (MinecraftServer)null);
         return new LevelSettings(s, GameType.SPECTATOR, false, Difficulty.PEACEFUL, true, gamerules, DataPackConfig.DEFAULT);
      } else {
         return new LevelSettings(s, this.gameMode.gameType, this.hardCore, this.getEffectiveDifficulty(), this.commands && !this.hardCore, this.gameRules, this.dataPacks);
      }
   }

   private void toggleWorldGenSettingsVisibility() {
      this.setWorldGenSettingsVisible(!this.worldGenSettingsVisible);
   }

   private void setGameMode(CreateWorldScreen.SelectedGameMode pGameMode) {
      if (!this.commandsChanged) {
         this.commands = pGameMode == CreateWorldScreen.SelectedGameMode.CREATIVE;
         this.commandsButton.setValue(this.commands);
      }

      if (pGameMode == CreateWorldScreen.SelectedGameMode.HARDCORE) {
         this.hardCore = true;
         this.commandsButton.active = false;
         this.commandsButton.setValue(false);
         this.worldGenSettingsComponent.switchToHardcore();
         this.difficultyButton.setValue(Difficulty.HARD);
         this.difficultyButton.active = false;
      } else {
         this.hardCore = false;
         this.commandsButton.active = true;
         this.commandsButton.setValue(this.commands);
         this.worldGenSettingsComponent.switchOutOfHardcode();
         this.difficultyButton.setValue(this.difficulty);
         this.difficultyButton.active = true;
      }

      this.gameMode = pGameMode;
      this.updateGameModeHelp();
   }

   public void refreshWorldGenSettingsVisibility() {
      this.setWorldGenSettingsVisible(this.worldGenSettingsVisible);
   }

   private void setWorldGenSettingsVisible(boolean pWorldGenSettingsVisible) {
      this.worldGenSettingsVisible = pWorldGenSettingsVisible;
      this.modeButton.visible = !pWorldGenSettingsVisible;
      this.difficultyButton.visible = !pWorldGenSettingsVisible;
      if (this.worldGenSettingsComponent.isDebug()) {
         this.dataPacksButton.visible = false;
         this.modeButton.active = false;
         if (this.oldGameMode == null) {
            this.oldGameMode = this.gameMode;
         }

         this.setGameMode(CreateWorldScreen.SelectedGameMode.DEBUG);
         this.commandsButton.visible = false;
      } else {
         this.modeButton.active = true;
         if (this.oldGameMode != null) {
            this.setGameMode(this.oldGameMode);
         }

         this.commandsButton.visible = !pWorldGenSettingsVisible;
         this.dataPacksButton.visible = !pWorldGenSettingsVisible;
      }

      this.worldGenSettingsComponent.setVisibility(pWorldGenSettingsVisible);
      this.nameEdit.setVisible(!pWorldGenSettingsVisible);
      if (pWorldGenSettingsVisible) {
         this.moreOptionsButton.setMessage(CommonComponents.GUI_DONE);
      } else {
         this.moreOptionsButton.setMessage(Component.translatable("selectWorld.moreWorldOptions"));
      }

      this.gameRulesButton.visible = !pWorldGenSettingsVisible;
   }

   public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
      if (super.keyPressed(pKeyCode, pScanCode, pModifiers)) {
         return true;
      } else if (pKeyCode != 257 && pKeyCode != 335) {
         return false;
      } else {
         this.onCreate();
         return true;
      }
   }

   public void onClose() {
      if (this.worldGenSettingsVisible) {
         this.setWorldGenSettingsVisible(false);
      } else {
         this.popScreen();
      }

   }

   public void popScreen() {
      this.minecraft.setScreen(this.lastScreen);
      this.removeTempDataPackDir();
   }

   public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
      this.renderBackground(pPoseStack);
      drawCenteredString(pPoseStack, this.font, this.title, this.width / 2, 20, -1);
      if (this.worldGenSettingsVisible) {
         drawString(pPoseStack, this.font, SEED_LABEL, this.width / 2 - 100, 47, -6250336);
         drawString(pPoseStack, this.font, SEED_INFO, this.width / 2 - 100, 85, -6250336);
         this.worldGenSettingsComponent.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
      } else {
         drawString(pPoseStack, this.font, NAME_LABEL, this.width / 2 - 100, 47, -6250336);
         drawString(pPoseStack, this.font, Component.empty().append(OUTPUT_DIR_INFO).append(" ").append(this.resultFolder), this.width / 2 - 100, 85, -6250336);
         this.nameEdit.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
         drawString(pPoseStack, this.font, this.gameModeHelp1, this.width / 2 - 150, 122, -6250336);
         drawString(pPoseStack, this.font, this.gameModeHelp2, this.width / 2 - 150, 134, -6250336);
         if (this.commandsButton.visible) {
            drawString(pPoseStack, this.font, COMMANDS_INFO, this.width / 2 - 150, 172, -6250336);
         }
      }

      super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
   }

   protected <T extends GuiEventListener & NarratableEntry> T addWidget(T pListener) {
      return super.addWidget(pListener);
   }

   protected <T extends GuiEventListener & Widget & NarratableEntry> T addRenderableWidget(T pWidget) {
      return super.addRenderableWidget(pWidget);
   }

   @Nullable
   private Path getTempDataPackDir() {
      if (this.tempDataPackDir == null) {
         try {
            this.tempDataPackDir = Files.createTempDirectory("mcworld-");
         } catch (IOException ioexception) {
            LOGGER.warn("Failed to create temporary dir", (Throwable)ioexception);
            SystemToast.onPackCopyFailure(this.minecraft, this.resultFolder);
            this.popScreen();
         }
      }

      return this.tempDataPackDir;
   }

   private void openDataPackSelectionScreen() {
      Pair<File, PackRepository> pair = this.getDataPackSelectionSettings();
      if (pair != null) {
         this.minecraft.setScreen(new PackSelectionScreen(this, pair.getSecond(), this::tryApplyNewDataPacks, pair.getFirst(), Component.translatable("dataPack.title")));
      }

   }

   private void tryApplyNewDataPacks(PackRepository p_100879_) {
      List<String> list = ImmutableList.copyOf(p_100879_.getSelectedIds());
      List<String> list1 = p_100879_.getAvailableIds().stream().filter((p_232927_) -> {
         return !list.contains(p_232927_);
      }).collect(ImmutableList.toImmutableList());
      DataPackConfig datapackconfig = new DataPackConfig(list, list1);
      if (list.equals(this.dataPacks.getEnabled())) {
         this.dataPacks = datapackconfig;
      } else {
         this.minecraft.tell(() -> {
            this.minecraft.setScreen(new GenericDirtMessageScreen(Component.translatable("dataPack.validation.working")));
         });
         WorldLoader.InitConfig worldloader$initconfig = createDefaultLoadConfig(p_100879_, datapackconfig);
         WorldLoader.load(worldloader$initconfig, (p_232886_, p_232887_) -> {
            WorldCreationContext worldcreationcontext = this.worldGenSettingsComponent.settings();
            RegistryAccess registryaccess = worldcreationcontext.registryAccess();
            RegistryAccess.Writable registryaccess$writable = RegistryAccess.builtinCopy();
            DynamicOps<JsonElement> dynamicops = RegistryOps.create(JsonOps.INSTANCE, registryaccess);
            DynamicOps<JsonElement> dynamicops1 = RegistryOps.createAndLoad(JsonOps.INSTANCE, registryaccess$writable, p_232886_);
            DataResult<JsonElement> dataresult = WorldGenSettings.CODEC.encodeStart(dynamicops, worldcreationcontext.worldGenSettings()).setLifecycle(Lifecycle.stable());
            DataResult<WorldGenSettings> dataresult1 = dataresult.flatMap((p_232895_) -> {
               return WorldGenSettings.CODEC.parse(dynamicops1, p_232895_);
            });
            RegistryAccess.Frozen registryaccess$frozen = registryaccess$writable.freeze();
            Lifecycle lifecycle = dataresult1.lifecycle().add(registryaccess$frozen.allElementsLifecycle());
            WorldGenSettings worldgensettings = dataresult1.getOrThrow(false, Util.prefix("Error parsing worldgen settings after loading data packs: ", LOGGER::error));
            if (registryaccess$frozen.registryOrThrow(Registry.WORLD_PRESET_REGISTRY).size() == 0) {
               throw new IllegalStateException("Needs at least one world preset to continue");
            } else if (registryaccess$frozen.registryOrThrow(Registry.BIOME_REGISTRY).size() == 0) {
               throw new IllegalStateException("Needs at least one biome continue");
            } else {
               return Pair.of(Pair.of(worldgensettings, lifecycle), registryaccess$frozen);
            }
         }, (p_232876_, p_232877_, p_232878_, p_232879_) -> {
            p_232876_.close();
            return new WorldCreationContext((WorldGenSettings)p_232879_.getFirst(), (Lifecycle)p_232879_.getSecond(), p_232878_, p_232877_);
         }, Util.backgroundExecutor(), this.minecraft).thenAcceptAsync((p_232890_) -> {
            this.dataPacks = datapackconfig;
            this.worldGenSettingsComponent.updateSettings(p_232890_);
            this.rebuildWidgets();
         }, this.minecraft).handle((p_232918_, p_232919_) -> {
            if (p_232919_ != null) {
               LOGGER.warn("Failed to validate datapack", p_232919_);
               this.minecraft.tell(() -> {
                  this.minecraft.setScreen(new ConfirmScreen((p_232949_) -> {
                     if (p_232949_) {
                        this.openDataPackSelectionScreen();
                     } else {
                        this.dataPacks = new DataPackConfig(ImmutableList.of("vanilla"), ImmutableList.of()); // FORGE: Revert to *actual* vanilla data
                        this.minecraft.setScreen(this);
                     }

                  }, Component.translatable("dataPack.validation.failed"), CommonComponents.EMPTY, Component.translatable("dataPack.validation.back"), Component.translatable("dataPack.validation.reset")));
               });
            } else {
               this.minecraft.tell(() -> {
                  this.minecraft.setScreen(this);
               });
            }

            return null;
         });
      }
   }

   private static WorldLoader.InitConfig createDefaultLoadConfig(PackRepository p_232873_, DataPackConfig p_232874_) {
      WorldLoader.PackConfig worldloader$packconfig = new WorldLoader.PackConfig(p_232873_, p_232874_, false);
      return new WorldLoader.InitConfig(worldloader$packconfig, Commands.CommandSelection.INTEGRATED, 2);
   }

   private void removeTempDataPackDir() {
      if (this.tempDataPackDir != null) {
         try {
            Stream<Path> stream = Files.walk(this.tempDataPackDir);

            try {
               stream.sorted(Comparator.reverseOrder()).forEach((p_232942_) -> {
                  try {
                     Files.delete(p_232942_);
                  } catch (IOException ioexception1) {
                     LOGGER.warn("Failed to remove temporary file {}", p_232942_, ioexception1);
                  }

               });
            } catch (Throwable throwable1) {
               if (stream != null) {
                  try {
                     stream.close();
                  } catch (Throwable throwable) {
                     throwable1.addSuppressed(throwable);
                  }
               }

               throw throwable1;
            }

            if (stream != null) {
               stream.close();
            }
         } catch (IOException ioexception) {
            LOGGER.warn("Failed to list temporary dir {}", (Object)this.tempDataPackDir);
         }

         this.tempDataPackDir = null;
      }

   }

   private static void copyBetweenDirs(Path pFromDir, Path pToDir, Path pFilePath) {
      try {
         Util.copyBetweenDirs(pFromDir, pToDir, pFilePath);
      } catch (IOException ioexception) {
         LOGGER.warn("Failed to copy datapack file from {} to {}", pFilePath, pToDir);
         throw new UncheckedIOException(ioexception);
      }
   }

   private Optional<LevelStorageSource.LevelStorageAccess> createNewWorldDirectory() {
      try {
         LevelStorageSource.LevelStorageAccess levelstoragesource$levelstorageaccess = this.minecraft.getLevelSource().createAccess(this.resultFolder);
         if (this.tempDataPackDir == null) {
            return Optional.of(levelstoragesource$levelstorageaccess);
         }

         try {
            Stream<Path> stream = Files.walk(this.tempDataPackDir);

            Optional optional;
            try {
               Path path = levelstoragesource$levelstorageaccess.getLevelPath(LevelResource.DATAPACK_DIR);
               Files.createDirectories(path);
               stream.filter((p_232921_) -> {
                  return !p_232921_.equals(this.tempDataPackDir);
               }).forEach((p_232945_) -> {
                  copyBetweenDirs(this.tempDataPackDir, path, p_232945_);
               });
               optional = Optional.of(levelstoragesource$levelstorageaccess);
            } catch (Throwable throwable1) {
               if (stream != null) {
                  try {
                     stream.close();
                  } catch (Throwable throwable) {
                     throwable1.addSuppressed(throwable);
                  }
               }

               throw throwable1;
            }

            if (stream != null) {
               stream.close();
            }

            return optional;
         } catch (UncheckedIOException | IOException ioexception) {
            LOGGER.warn("Failed to copy datapacks to world {}", this.resultFolder, ioexception);
            levelstoragesource$levelstorageaccess.close();
         }
      } catch (UncheckedIOException | IOException ioexception1) {
         LOGGER.warn("Failed to create access for {}", this.resultFolder, ioexception1);
      }

      SystemToast.onPackCopyFailure(this.minecraft, this.resultFolder);
      this.popScreen();
      return Optional.empty();
   }

   @Nullable
   public static Path createTempDataPackDirFromExistingWorld(Path pDatapackDir, Minecraft pMinecraft) {
      MutableObject<Path> mutableobject = new MutableObject<>();

      try {
         Stream<Path> stream = Files.walk(pDatapackDir);

         try {
            stream.filter((p_232924_) -> {
               return !p_232924_.equals(pDatapackDir);
            }).forEach((p_232933_) -> {
               Path path = mutableobject.getValue();
               if (path == null) {
                  try {
                     path = Files.createTempDirectory("mcworld-");
                  } catch (IOException ioexception1) {
                     LOGGER.warn("Failed to create temporary dir");
                     throw new UncheckedIOException(ioexception1);
                  }

                  mutableobject.setValue(path);
               }

               copyBetweenDirs(pDatapackDir, path, p_232933_);
            });
         } catch (Throwable throwable1) {
            if (stream != null) {
               try {
                  stream.close();
               } catch (Throwable throwable) {
                  throwable1.addSuppressed(throwable);
               }
            }

            throw throwable1;
         }

         if (stream != null) {
            stream.close();
         }
      } catch (UncheckedIOException | IOException ioexception) {
         LOGGER.warn("Failed to copy datapacks from world {}", pDatapackDir, ioexception);
         SystemToast.onPackCopyFailure(pMinecraft, pDatapackDir.toString());
         return null;
      }

      return mutableobject.getValue();
   }

   @Nullable
   private Pair<File, PackRepository> getDataPackSelectionSettings() {
      Path path = this.getTempDataPackDir();
      if (path != null) {
         File file1 = path.toFile();
         if (this.tempDataPackRepository == null) {
            this.tempDataPackRepository = new PackRepository(PackType.SERVER_DATA, new ServerPacksSource(), new FolderRepositorySource(file1, PackSource.DEFAULT));
            net.minecraftforge.resource.ResourcePackLoader.loadResourcePacks(this.tempDataPackRepository, net.minecraftforge.server.ServerLifecycleHooks::buildPackFinder);
            this.tempDataPackRepository.reload();
         }

         this.tempDataPackRepository.setSelected(this.dataPacks.getEnabled());
         return Pair.of(file1, this.tempDataPackRepository);
      } else {
         return null;
      }
   }

   @OnlyIn(Dist.CLIENT)
   static enum SelectedGameMode {
      SURVIVAL("survival", GameType.SURVIVAL),
      HARDCORE("hardcore", GameType.SURVIVAL),
      CREATIVE("creative", GameType.CREATIVE),
      DEBUG("spectator", GameType.SPECTATOR);

      final String name;
      final GameType gameType;
      private final Component displayName;

      private SelectedGameMode(String pName, GameType pGameType) {
         this.name = pName;
         this.gameType = pGameType;
         this.displayName = Component.translatable("selectWorld.gameMode." + pName);
      }

      public Component getDisplayName() {
         return this.displayName;
      }
   }
}
