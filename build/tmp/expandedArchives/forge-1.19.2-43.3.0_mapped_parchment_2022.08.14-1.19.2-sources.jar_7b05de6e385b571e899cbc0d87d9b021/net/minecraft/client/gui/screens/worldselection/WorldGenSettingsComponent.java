package net.minecraft.client.gui.screens.worldselection;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.WorldPresetTags;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.PointerBuffer;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class WorldGenSettingsComponent implements Widget {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Component CUSTOM_WORLD_DESCRIPTION = Component.translatable("generator.custom");
   private static final Component AMPLIFIED_HELP_TEXT = Component.translatable("generator.minecraft.amplified.info");
   private static final Component MAP_FEATURES_INFO = Component.translatable("selectWorld.mapFeatures.info");
   private static final Component SELECT_FILE_PROMPT = Component.translatable("selectWorld.import_worldgen_settings.select_file");
   private MultiLineLabel amplifiedWorldInfo = MultiLineLabel.EMPTY;
   private Font font;
   private int width;
   private EditBox seedEdit;
   private CycleButton<Boolean> featuresButton;
   private CycleButton<Boolean> bonusItemsButton;
   private CycleButton<Holder<WorldPreset>> typeButton;
   private Button customWorldDummyButton;
   private Button customizeTypeButton;
   private Button importSettingsButton;
   private WorldCreationContext settings;
   private Optional<Holder<WorldPreset>> preset;
   private OptionalLong seed;

   public WorldGenSettingsComponent(WorldCreationContext pSettings, Optional<ResourceKey<WorldPreset>> pPreset, OptionalLong pSeed) {
      this.settings = pSettings;
      this.preset = findPreset(pSettings, pPreset);
      this.seed = pSeed;
   }

   private static Optional<Holder<WorldPreset>> findPreset(WorldCreationContext pSettings, Optional<ResourceKey<WorldPreset>> pPreset) {
      return pPreset.flatMap((p_233046_) -> {
         return pSettings.registryAccess().<WorldPreset>registryOrThrow(Registry.WORLD_PRESET_REGISTRY).getHolder(p_233046_);
      });
   }

   public void init(CreateWorldScreen pCreateWorldScreen, Minecraft pMinecraft, Font pFont) {
      this.font = pFont;
      this.width = pCreateWorldScreen.width;
      this.seedEdit = new EditBox(this.font, this.width / 2 - 100, 60, 200, 20, Component.translatable("selectWorld.enterSeed"));
      this.seedEdit.setValue(toString(this.seed));
      this.seedEdit.setResponder((p_233063_) -> {
         this.seed = WorldGenSettings.parseSeed(this.seedEdit.getValue());
      });
      pCreateWorldScreen.addWidget(this.seedEdit);
      int i = this.width / 2 - 155;
      int j = this.width / 2 + 5;
      this.featuresButton = pCreateWorldScreen.addRenderableWidget(CycleButton.onOffBuilder(this.settings.worldGenSettings().generateStructures()).withCustomNarration((p_233081_) -> {
         return CommonComponents.joinForNarration(p_233081_.createDefaultNarrationMessage(), Component.translatable("selectWorld.mapFeatures.info"));
      }).create(i, 100, 150, 20, Component.translatable("selectWorld.mapFeatures"), (p_233083_, p_233084_) -> {
         this.updateSettings(WorldGenSettings::withStructuresToggled);
      }));
      this.featuresButton.visible = false;
      Registry<WorldPreset> registry = this.settings.registryAccess().registryOrThrow(Registry.WORLD_PRESET_REGISTRY);
      List<Holder<WorldPreset>> list = getNonEmptyList(registry, WorldPresetTags.NORMAL).orElseGet(() -> {
         return registry.holders().collect(Collectors.toUnmodifiableList());
      });
      List<Holder<WorldPreset>> list1 = getNonEmptyList(registry, WorldPresetTags.EXTENDED).orElse(list);
      this.typeButton = pCreateWorldScreen.addRenderableWidget(CycleButton.builder(WorldGenSettingsComponent::describePreset).withValues(list, list1).withCustomNarration((p_233030_) -> {
         return isAmplified(p_233030_.getValue()) ? CommonComponents.joinForNarration(p_233030_.createDefaultNarrationMessage(), AMPLIFIED_HELP_TEXT) : p_233030_.createDefaultNarrationMessage();
      }).create(j, 100, 150, 20, Component.translatable("selectWorld.mapType"), (p_233036_, p_233037_) -> {
         this.preset = Optional.of(p_233037_);
         this.updateSettings((p_233054_) -> {
            return ((WorldPreset)p_233037_.value()).recreateWorldGenSettings(p_233054_);
         });
         pCreateWorldScreen.refreshWorldGenSettingsVisibility();
      }));
      this.preset.ifPresent(this.typeButton::setValue);
      this.typeButton.visible = false;
      this.customWorldDummyButton = pCreateWorldScreen.addRenderableWidget(new Button(j, 100, 150, 20, CommonComponents.optionNameValue(Component.translatable("selectWorld.mapType"), CUSTOM_WORLD_DESCRIPTION), (p_233028_) -> {
      }));
      this.customWorldDummyButton.active = false;
      this.customWorldDummyButton.visible = false;
      this.customizeTypeButton = pCreateWorldScreen.addRenderableWidget(new Button(j, 120, 150, 20, Component.translatable("selectWorld.customizeType"), (p_233079_) -> {
         PresetEditor preseteditor = PresetEditor.EDITORS.get(this.preset.flatMap(Holder::unwrapKey));
         if (preseteditor != null) {
            pMinecraft.setScreen(preseteditor.createEditScreen(pCreateWorldScreen, this.settings));
         }

      }));
      this.customizeTypeButton.visible = false;
      this.bonusItemsButton = pCreateWorldScreen.addRenderableWidget(CycleButton.onOffBuilder(this.settings.worldGenSettings().generateBonusChest() && !pCreateWorldScreen.hardCore).create(i, 151, 150, 20, Component.translatable("selectWorld.bonusItems"), (p_233032_, p_233033_) -> {
         this.updateSettings(WorldGenSettings::withBonusChestToggled);
      }));
      this.bonusItemsButton.visible = false;
      this.importSettingsButton = pCreateWorldScreen.addRenderableWidget(new Button(i, 185, 150, 20, Component.translatable("selectWorld.import_worldgen_settings"), (p_233026_) -> {
         String s = TinyFileDialogs.tinyfd_openFileDialog(SELECT_FILE_PROMPT.getString(), (CharSequence)null, (PointerBuffer)null, (CharSequence)null, false);
         if (s != null) {
            DynamicOps<JsonElement> dynamicops = RegistryOps.create(JsonOps.INSTANCE, this.settings.registryAccess());

            DataResult<WorldGenSettings> dataresult;
            try {
               BufferedReader bufferedreader = Files.newBufferedReader(Paths.get(s));

               try {
                  JsonElement jsonelement = JsonParser.parseReader(bufferedreader);
                  dataresult = WorldGenSettings.CODEC.parse(dynamicops, jsonelement);
               } catch (Throwable throwable1) {
                  if (bufferedreader != null) {
                     try {
                        bufferedreader.close();
                     } catch (Throwable throwable) {
                        throwable1.addSuppressed(throwable);
                     }
                  }

                  throw throwable1;
               }

               if (bufferedreader != null) {
                  bufferedreader.close();
               }
            } catch (Exception exception) {
               dataresult = DataResult.error("Failed to parse file: " + exception.getMessage());
            }

            if (dataresult.error().isPresent()) {
               Component component1 = Component.translatable("selectWorld.import_worldgen_settings.failure");
               String s1 = dataresult.error().get().message();
               LOGGER.error("Error parsing world settings: {}", (Object)s1);
               Component component = Component.literal(s1);
               pMinecraft.getToasts().addToast(SystemToast.multiline(pMinecraft, SystemToast.SystemToastIds.WORLD_GEN_SETTINGS_TRANSFER, component1, component));
            } else {
               Lifecycle lifecycle = dataresult.lifecycle();
               dataresult.resultOrPartial(LOGGER::error).ifPresent((p_233022_) -> {
                  WorldOpenFlows.confirmWorldCreation(pMinecraft, pCreateWorldScreen, lifecycle, () -> {
                     this.importSettings(p_233022_);
                  });
               });
            }
         }
      }));
      this.importSettingsButton.visible = false;
      this.amplifiedWorldInfo = MultiLineLabel.create(pFont, AMPLIFIED_HELP_TEXT, this.typeButton.getWidth());
   }

   private static Optional<List<Holder<WorldPreset>>> getNonEmptyList(Registry<WorldPreset> p_233060_, TagKey<WorldPreset> p_233061_) {
      return p_233060_.getTag(p_233061_).map((p_233056_) -> {
         return p_233056_.stream().toList();
      }).filter((p_233065_) -> {
         return !p_233065_.isEmpty();
      });
   }

   private static boolean isAmplified(Holder<WorldPreset> p_233051_) {
      return p_233051_.unwrapKey().filter((p_233073_) -> {
         return p_233073_.equals(WorldPresets.AMPLIFIED);
      }).isPresent();
   }

   private static Component describePreset(Holder<WorldPreset> p_233086_) {
      return p_233086_.unwrapKey().<Component>map((p_233015_) -> {
         return Component.translatable(p_233015_.location().toLanguageKey("generator"));
      }).orElse(CUSTOM_WORLD_DESCRIPTION);
   }

   private void importSettings(WorldGenSettings p_233017_) {
      this.settings = this.settings.withSettings(p_233017_);
      this.preset = findPreset(this.settings, WorldPresets.fromSettings(p_233017_));
      this.selectWorldTypeButton(true);
      this.seed = OptionalLong.of(p_233017_.seed());
      this.seedEdit.setValue(toString(this.seed));
   }

   public void tick() {
      this.seedEdit.tick();
   }

   public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
      if (this.featuresButton.visible) {
         this.font.drawShadow(pPoseStack, MAP_FEATURES_INFO, (float)(this.width / 2 - 150), 122.0F, -6250336);
      }

      this.seedEdit.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
      if (this.preset.filter(WorldGenSettingsComponent::isAmplified).isPresent()) {
         this.amplifiedWorldInfo.renderLeftAligned(pPoseStack, this.typeButton.x + 2, this.typeButton.y + 22, 9, 10526880);
      }

   }

   void updateSettings(WorldCreationContext.SimpleUpdater p_233039_) {
      this.settings = this.settings.withSettings(p_233039_);
   }

   void updateSettings(WorldCreationContext.Updater p_233041_) {
      this.settings = this.settings.withSettings(p_233041_);
   }

   void updateSettings(WorldCreationContext pSettings) {
      this.settings = pSettings;
   }

   private static String toString(OptionalLong pSeed) {
      return pSeed.isPresent() ? Long.toString(pSeed.getAsLong()) : "";
   }

   public WorldCreationContext createFinalSettings(boolean p_233067_) {
      OptionalLong optionallong = WorldGenSettings.parseSeed(this.seedEdit.getValue());
      return this.settings.withSettings((p_233071_) -> {
         return p_233071_.withSeed(p_233067_, optionallong);
      });
   }

   public boolean isDebug() {
      return this.settings.worldGenSettings().isDebug();
   }

   public void setVisibility(boolean pVisible) {
      this.selectWorldTypeButton(pVisible);
      if (this.isDebug()) {
         this.featuresButton.visible = false;
         this.bonusItemsButton.visible = false;
         this.customizeTypeButton.visible = false;
         this.importSettingsButton.visible = false;
      } else {
         this.featuresButton.visible = pVisible;
         this.bonusItemsButton.visible = pVisible;
         this.customizeTypeButton.visible = pVisible && PresetEditor.EDITORS.containsKey(this.preset.flatMap(Holder::unwrapKey));
         this.importSettingsButton.visible = pVisible;
      }

      this.seedEdit.setVisible(pVisible);
   }

   private void selectWorldTypeButton(boolean pVisible) {
      if (this.preset.isPresent()) {
         this.typeButton.visible = pVisible;
         this.customWorldDummyButton.visible = false;
      } else {
         this.typeButton.visible = false;
         this.customWorldDummyButton.visible = pVisible;
      }

   }

   public WorldCreationContext settings() {
      return this.settings;
   }

   public RegistryAccess registryHolder() {
      return this.settings.registryAccess();
   }

   public void switchToHardcore() {
      this.bonusItemsButton.active = false;
      this.bonusItemsButton.setValue(false);
   }

   public void switchOutOfHardcode() {
      this.bonusItemsButton.active = true;
      this.bonusItemsButton.setValue(this.settings.worldGenSettings().generateBonusChest());
   }
}