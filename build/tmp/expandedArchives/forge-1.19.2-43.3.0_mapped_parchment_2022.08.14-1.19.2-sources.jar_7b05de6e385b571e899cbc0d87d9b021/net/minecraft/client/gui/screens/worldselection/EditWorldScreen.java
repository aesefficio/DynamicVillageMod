package net.minecraft.client.gui.screens.worldselection;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.stream.JsonWriter;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.BackupConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.WorldStem;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class EditWorldScreen extends Screen {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Gson WORLD_GEN_SETTINGS_GSON = (new GsonBuilder()).setPrettyPrinting().serializeNulls().disableHtmlEscaping().create();
   private static final Component NAME_LABEL = Component.translatable("selectWorld.enterName");
   private Button renameButton;
   private final BooleanConsumer callback;
   private EditBox nameEdit;
   private final LevelStorageSource.LevelStorageAccess levelAccess;

   public EditWorldScreen(BooleanConsumer pCallback, LevelStorageSource.LevelStorageAccess pLevelAccess) {
      super(Component.translatable("selectWorld.edit.title"));
      this.callback = pCallback;
      this.levelAccess = pLevelAccess;
   }

   public void tick() {
      this.nameEdit.tick();
   }

   protected void init() {
      this.minecraft.keyboardHandler.setSendRepeatsToGui(true);
      Button button = this.addRenderableWidget(new Button(this.width / 2 - 100, this.height / 4 + 0 + 5, 200, 20, Component.translatable("selectWorld.edit.resetIcon"), (p_101297_) -> {
         this.levelAccess.getIconFile().ifPresent((p_182594_) -> {
            FileUtils.deleteQuietly(p_182594_.toFile());
         });
         p_101297_.active = false;
      }));
      this.addRenderableWidget(new Button(this.width / 2 - 100, this.height / 4 + 24 + 5, 200, 20, Component.translatable("selectWorld.edit.openFolder"), (p_101294_) -> {
         Util.getPlatform().openFile(this.levelAccess.getLevelPath(LevelResource.ROOT).toFile());
      }));
      this.addRenderableWidget(new Button(this.width / 2 - 100, this.height / 4 + 48 + 5, 200, 20, Component.translatable("selectWorld.edit.backup"), (p_101292_) -> {
         boolean flag = makeBackupAndShowToast(this.levelAccess);
         this.callback.accept(!flag);
      }));
      this.addRenderableWidget(new Button(this.width / 2 - 100, this.height / 4 + 72 + 5, 200, 20, Component.translatable("selectWorld.edit.backupFolder"), (p_101290_) -> {
         LevelStorageSource levelstoragesource = this.minecraft.getLevelSource();
         Path path = levelstoragesource.getBackupPath();

         try {
            Files.createDirectories(Files.exists(path) ? path.toRealPath() : path);
         } catch (IOException ioexception) {
            throw new RuntimeException(ioexception);
         }

         Util.getPlatform().openFile(path.toFile());
      }));
      this.addRenderableWidget(new Button(this.width / 2 - 100, this.height / 4 + 96 + 5, 200, 20, Component.translatable("selectWorld.edit.optimize"), (p_101287_) -> {
         this.minecraft.setScreen(new BackupConfirmScreen(this, (p_170235_, p_170236_) -> {
            if (p_170235_) {
               makeBackupAndShowToast(this.levelAccess);
            }

            this.minecraft.setScreen(OptimizeWorldScreen.create(this.minecraft, this.callback, this.minecraft.getFixerUpper(), this.levelAccess, p_170236_));
         }, Component.translatable("optimizeWorld.confirm.title"), Component.translatable("optimizeWorld.confirm.description"), true));
      }));
      this.addRenderableWidget(new Button(this.width / 2 - 100, this.height / 4 + 120 + 5, 200, 20, Component.translatable("selectWorld.edit.export_worldgen_settings"), (p_101284_) -> {
         DataResult<String> dataresult;
         try {
            WorldStem worldstem = this.minecraft.createWorldOpenFlows().loadWorldStem(this.levelAccess, false);

            try {
               DynamicOps<JsonElement> dynamicops = RegistryOps.create(JsonOps.INSTANCE, worldstem.registryAccess());
               DataResult<JsonElement> dataresult1 = WorldGenSettings.CODEC.encodeStart(dynamicops, worldstem.worldData().worldGenSettings());
               dataresult = dataresult1.flatMap((p_170231_) -> {
                  Path path = this.levelAccess.getLevelPath(LevelResource.ROOT).resolve("worldgen_settings_export.json");

                  try {
                     JsonWriter jsonwriter = WORLD_GEN_SETTINGS_GSON.newJsonWriter(Files.newBufferedWriter(path, StandardCharsets.UTF_8));

                     try {
                        WORLD_GEN_SETTINGS_GSON.toJson(p_170231_, jsonwriter);
                     } catch (Throwable throwable3) {
                        if (jsonwriter != null) {
                           try {
                              jsonwriter.close();
                           } catch (Throwable throwable2) {
                              throwable3.addSuppressed(throwable2);
                           }
                        }

                        throw throwable3;
                     }

                     if (jsonwriter != null) {
                        jsonwriter.close();
                     }
                  } catch (JsonIOException | IOException ioexception) {
                     return DataResult.error("Error writing file: " + ioexception.getMessage());
                  }

                  return DataResult.success(path.toString());
               });
            } catch (Throwable throwable1) {
               if (worldstem != null) {
                  try {
                     worldstem.close();
                  } catch (Throwable throwable) {
                     throwable1.addSuppressed(throwable);
                  }
               }

               throw throwable1;
            }

            if (worldstem != null) {
               worldstem.close();
            }
         } catch (Exception exception) {
            LOGGER.warn("Could not parse level data", (Throwable)exception);
            dataresult = DataResult.error("Could not parse level data: " + exception.getMessage());
         }

         Component component = Component.literal(dataresult.get().map(Function.identity(), DataResult.PartialResult::message));
         Component component1 = Component.translatable(dataresult.result().isPresent() ? "selectWorld.edit.export_worldgen_settings.success" : "selectWorld.edit.export_worldgen_settings.failure");
         dataresult.error().ifPresent((p_170233_) -> {
            LOGGER.error("Error exporting world settings: {}", (Object)p_170233_);
         });
         this.minecraft.getToasts().addToast(SystemToast.multiline(this.minecraft, SystemToast.SystemToastIds.WORLD_GEN_SETTINGS_TRANSFER, component1, component));
      }));
      this.renameButton = this.addRenderableWidget(new Button(this.width / 2 - 100, this.height / 4 + 144 + 5, 98, 20, Component.translatable("selectWorld.edit.save"), (p_101280_) -> {
         this.onRename();
      }));
      this.addRenderableWidget(new Button(this.width / 2 + 2, this.height / 4 + 144 + 5, 98, 20, CommonComponents.GUI_CANCEL, (p_101273_) -> {
         this.callback.accept(false);
      }));
      button.active = this.levelAccess.getIconFile().filter((p_182587_) -> {
         return Files.isRegularFile(p_182587_);
      }).isPresent();
      LevelSummary levelsummary = this.levelAccess.getSummary();
      String s = levelsummary == null ? "" : levelsummary.getLevelName();
      this.nameEdit = new EditBox(this.font, this.width / 2 - 100, 38, 200, 20, Component.translatable("selectWorld.enterName"));
      this.nameEdit.setValue(s);
      this.nameEdit.setResponder((p_101282_) -> {
         this.renameButton.active = !p_101282_.trim().isEmpty();
      });
      this.addWidget(this.nameEdit);
      this.setInitialFocus(this.nameEdit);
   }

   public void resize(Minecraft pMinecraft, int pWidth, int pHeight) {
      String s = this.nameEdit.getValue();
      this.init(pMinecraft, pWidth, pHeight);
      this.nameEdit.setValue(s);
   }

   public void onClose() {
      this.callback.accept(false);
   }

   public void removed() {
      this.minecraft.keyboardHandler.setSendRepeatsToGui(false);
   }

   /**
    * Saves changes to the world name and closes this GUI.
    */
   private void onRename() {
      try {
         this.levelAccess.renameLevel(this.nameEdit.getValue().trim());
         this.callback.accept(true);
      } catch (IOException ioexception) {
         LOGGER.error("Failed to access world '{}'", this.levelAccess.getLevelId(), ioexception);
         SystemToast.onWorldAccessFailure(this.minecraft, this.levelAccess.getLevelId());
         this.callback.accept(true);
      }

   }

   public static void makeBackupAndShowToast(LevelStorageSource pLevelSource, String pLevelName) {
      boolean flag = false;

      try {
         LevelStorageSource.LevelStorageAccess levelstoragesource$levelstorageaccess = pLevelSource.createAccess(pLevelName);

         try {
            flag = true;
            makeBackupAndShowToast(levelstoragesource$levelstorageaccess);
         } catch (Throwable throwable1) {
            if (levelstoragesource$levelstorageaccess != null) {
               try {
                  levelstoragesource$levelstorageaccess.close();
               } catch (Throwable throwable) {
                  throwable1.addSuppressed(throwable);
               }
            }

            throw throwable1;
         }

         if (levelstoragesource$levelstorageaccess != null) {
            levelstoragesource$levelstorageaccess.close();
         }
      } catch (IOException ioexception) {
         if (!flag) {
            SystemToast.onWorldAccessFailure(Minecraft.getInstance(), pLevelName);
         }

         LOGGER.warn("Failed to create backup of level {}", pLevelName, ioexception);
      }

   }

   public static boolean makeBackupAndShowToast(LevelStorageSource.LevelStorageAccess pLevelAccess) {
      long i = 0L;
      IOException ioexception = null;

      try {
         i = pLevelAccess.makeWorldBackup();
      } catch (IOException ioexception1) {
         ioexception = ioexception1;
      }

      if (ioexception != null) {
         Component component2 = Component.translatable("selectWorld.edit.backupFailed");
         Component component3 = Component.literal(ioexception.getMessage());
         Minecraft.getInstance().getToasts().addToast(new SystemToast(SystemToast.SystemToastIds.WORLD_BACKUP, component2, component3));
         return false;
      } else {
         Component component = Component.translatable("selectWorld.edit.backupCreated", pLevelAccess.getLevelId());
         Component component1 = Component.translatable("selectWorld.edit.backupSize", Mth.ceil((double)i / 1048576.0D));
         Minecraft.getInstance().getToasts().addToast(new SystemToast(SystemToast.SystemToastIds.WORLD_BACKUP, component, component1));
         return true;
      }
   }

   public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
      this.renderBackground(pPoseStack);
      drawCenteredString(pPoseStack, this.font, this.title, this.width / 2, 15, 16777215);
      drawString(pPoseStack, this.font, NAME_LABEL, this.width / 2 - 100, 24, 10526880);
      this.nameEdit.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
      super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
   }
}