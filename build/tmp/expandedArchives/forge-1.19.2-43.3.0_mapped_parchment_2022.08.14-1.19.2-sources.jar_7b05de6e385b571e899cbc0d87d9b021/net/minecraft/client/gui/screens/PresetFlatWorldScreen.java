package net.minecraft.client.gui.screens;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FlatLevelGeneratorPresetTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorPreset;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class PresetFlatWorldScreen extends Screen {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int SLOT_TEX_SIZE = 128;
   private static final int SLOT_BG_SIZE = 18;
   private static final int SLOT_STAT_HEIGHT = 20;
   private static final int SLOT_BG_X = 1;
   private static final int SLOT_BG_Y = 1;
   private static final int SLOT_FG_X = 2;
   private static final int SLOT_FG_Y = 2;
   private static final ResourceKey<Biome> DEFAULT_BIOME = Biomes.PLAINS;
   public static final Component UNKNOWN_PRESET = Component.translatable("flat_world_preset.unknown");
   /** The parent GUI */
   private final CreateFlatWorldScreen parent;
   private Component shareText;
   private Component listText;
   private PresetFlatWorldScreen.PresetsList list;
   private Button selectButton;
   EditBox export;
   FlatLevelGeneratorSettings settings;

   public PresetFlatWorldScreen(CreateFlatWorldScreen pParent) {
      super(Component.translatable("createWorld.customize.presets.title"));
      this.parent = pParent;
   }

   @Nullable
   private static FlatLayerInfo getLayerInfoFromString(String pLayerInfo, int pCurrentHeight) {
      String[] astring = pLayerInfo.split("\\*", 2);
      int i;
      if (astring.length == 2) {
         try {
            i = Math.max(Integer.parseInt(astring[0]), 0);
         } catch (NumberFormatException numberformatexception) {
            LOGGER.error("Error while parsing flat world string => {}", (Object)numberformatexception.getMessage());
            return null;
         }
      } else {
         i = 1;
      }

      int j = Math.min(pCurrentHeight + i, DimensionType.Y_SIZE);
      int k = j - pCurrentHeight;
      String s = astring[astring.length - 1];

      Block block;
      try {
         block = Registry.BLOCK.getOptional(new ResourceLocation(s)).orElse((Block)null);
      } catch (Exception exception) {
         LOGGER.error("Error while parsing flat world string => {}", (Object)exception.getMessage());
         return null;
      }

      if (block == null) {
         LOGGER.error("Error while parsing flat world string => Unknown block, {}", (Object)s);
         return null;
      } else {
         return new FlatLayerInfo(k, block);
      }
   }

   private static List<FlatLayerInfo> getLayersInfoFromString(String pLayerInfo) {
      List<FlatLayerInfo> list = Lists.newArrayList();
      String[] astring = pLayerInfo.split(",");
      int i = 0;

      for(String s : astring) {
         FlatLayerInfo flatlayerinfo = getLayerInfoFromString(s, i);
         if (flatlayerinfo == null) {
            return Collections.emptyList();
         }

         list.add(flatlayerinfo);
         i += flatlayerinfo.getHeight();
      }

      return list;
   }

   public static FlatLevelGeneratorSettings fromString(Registry<Biome> pBiomeRegistry, Registry<StructureSet> pStructureSetRegistry, String pSettings, FlatLevelGeneratorSettings pLayerGenerationSettings) {
      Iterator<String> iterator = Splitter.on(';').split(pSettings).iterator();
      if (!iterator.hasNext()) {
         return FlatLevelGeneratorSettings.getDefault(pBiomeRegistry, pStructureSetRegistry);
      } else {
         List<FlatLayerInfo> list = getLayersInfoFromString(iterator.next());
         if (list.isEmpty()) {
            return FlatLevelGeneratorSettings.getDefault(pBiomeRegistry, pStructureSetRegistry);
         } else {
            FlatLevelGeneratorSettings flatlevelgeneratorsettings = pLayerGenerationSettings.withLayers(list, pLayerGenerationSettings.structureOverrides());
            ResourceKey<Biome> resourcekey = DEFAULT_BIOME;
            if (iterator.hasNext()) {
               try {
                  ResourceLocation resourcelocation = new ResourceLocation(iterator.next());
                  resourcekey = ResourceKey.create(Registry.BIOME_REGISTRY, resourcelocation);
                  pBiomeRegistry.getOptional(resourcekey).orElseThrow(() -> {
                     return new IllegalArgumentException("Invalid Biome: " + resourcelocation);
                  });
               } catch (Exception exception) {
                  LOGGER.error("Error while parsing flat world string => {}", (Object)exception.getMessage());
                  resourcekey = DEFAULT_BIOME;
               }
            }

            flatlevelgeneratorsettings.setBiome(pBiomeRegistry.getOrCreateHolderOrThrow(resourcekey));
            return flatlevelgeneratorsettings;
         }
      }
   }

   static String save(FlatLevelGeneratorSettings pLevelGeneratorSettings) {
      StringBuilder stringbuilder = new StringBuilder();

      for(int i = 0; i < pLevelGeneratorSettings.getLayersInfo().size(); ++i) {
         if (i > 0) {
            stringbuilder.append(",");
         }

         stringbuilder.append(pLevelGeneratorSettings.getLayersInfo().get(i));
      }

      stringbuilder.append(";");
      stringbuilder.append(pLevelGeneratorSettings.getBiome().unwrapKey().map(ResourceKey::location).orElseThrow(() -> {
         return new IllegalStateException("Biome not registered");
      }));
      return stringbuilder.toString();
   }

   protected void init() {
      this.minecraft.keyboardHandler.setSendRepeatsToGui(true);
      this.shareText = Component.translatable("createWorld.customize.presets.share");
      this.listText = Component.translatable("createWorld.customize.presets.list");
      this.export = new EditBox(this.font, 50, 40, this.width - 100, 20, this.shareText);
      this.export.setMaxLength(1230);
      RegistryAccess registryaccess = this.parent.parent.worldGenSettingsComponent.registryHolder();
      Registry<Biome> registry = registryaccess.registryOrThrow(Registry.BIOME_REGISTRY);
      Registry<StructureSet> registry1 = registryaccess.registryOrThrow(Registry.STRUCTURE_SET_REGISTRY);
      this.export.setValue(save(this.parent.settings()));
      this.settings = this.parent.settings();
      this.addWidget(this.export);
      this.list = new PresetFlatWorldScreen.PresetsList(this.parent.parent.worldGenSettingsComponent.registryHolder());
      this.addWidget(this.list);
      this.selectButton = this.addRenderableWidget(new Button(this.width / 2 - 155, this.height - 28, 150, 20, Component.translatable("createWorld.customize.presets.select"), (p_211770_) -> {
         FlatLevelGeneratorSettings flatlevelgeneratorsettings = fromString(registry, registry1, this.export.getValue(), this.settings);
         this.parent.setConfig(flatlevelgeneratorsettings);
         this.minecraft.setScreen(this.parent);
      }));
      this.addRenderableWidget(new Button(this.width / 2 + 5, this.height - 28, 150, 20, CommonComponents.GUI_CANCEL, (p_96394_) -> {
         this.minecraft.setScreen(this.parent);
      }));
      this.updateButtonValidity(this.list.getSelected() != null);
   }

   public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
      return this.list.mouseScrolled(pMouseX, pMouseY, pDelta);
   }

   public void resize(Minecraft pMinecraft, int pWidth, int pHeight) {
      String s = this.export.getValue();
      this.init(pMinecraft, pWidth, pHeight);
      this.export.setValue(s);
   }

   public void onClose() {
      this.minecraft.setScreen(this.parent);
   }

   public void removed() {
      this.minecraft.keyboardHandler.setSendRepeatsToGui(false);
   }

   public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
      this.renderBackground(pPoseStack);
      this.list.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
      pPoseStack.pushPose();
      pPoseStack.translate(0.0D, 0.0D, 400.0D);
      drawCenteredString(pPoseStack, this.font, this.title, this.width / 2, 8, 16777215);
      drawString(pPoseStack, this.font, this.shareText, 50, 30, 10526880);
      drawString(pPoseStack, this.font, this.listText, 50, 70, 10526880);
      pPoseStack.popPose();
      this.export.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
      super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
   }

   public void tick() {
      this.export.tick();
      super.tick();
   }

   public void updateButtonValidity(boolean pValid) {
      this.selectButton.active = pValid || this.export.getValue().length() > 1;
   }

   @OnlyIn(Dist.CLIENT)
   class PresetsList extends ObjectSelectionList<PresetFlatWorldScreen.PresetsList.Entry> {
      public PresetsList(RegistryAccess pRegistryAccess) {
         super(PresetFlatWorldScreen.this.minecraft, PresetFlatWorldScreen.this.width, PresetFlatWorldScreen.this.height, 80, PresetFlatWorldScreen.this.height - 37, 24);

         for(Holder<FlatLevelGeneratorPreset> holder : pRegistryAccess.registryOrThrow(Registry.FLAT_LEVEL_GENERATOR_PRESET_REGISTRY).getTagOrEmpty(FlatLevelGeneratorPresetTags.VISIBLE)) {
            this.addEntry(new PresetFlatWorldScreen.PresetsList.Entry(holder));
         }

      }

      public void setSelected(@Nullable PresetFlatWorldScreen.PresetsList.Entry pEntry) {
         super.setSelected(pEntry);
         PresetFlatWorldScreen.this.updateButtonValidity(pEntry != null);
      }

      protected boolean isFocused() {
         return PresetFlatWorldScreen.this.getFocused() == this;
      }

      public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
         if (super.keyPressed(pKeyCode, pScanCode, pModifiers)) {
            return true;
         } else {
            if ((pKeyCode == 257 || pKeyCode == 335) && this.getSelected() != null) {
               this.getSelected().select();
            }

            return false;
         }
      }

      @OnlyIn(Dist.CLIENT)
      public class Entry extends ObjectSelectionList.Entry<PresetFlatWorldScreen.PresetsList.Entry> {
         private final FlatLevelGeneratorPreset preset;
         private final Component name;

         public Entry(Holder<FlatLevelGeneratorPreset> pPresetHolder) {
            this.preset = pPresetHolder.value();
            this.name = pPresetHolder.unwrapKey().<Component>map((p_232760_) -> {
               return Component.translatable(p_232760_.location().toLanguageKey("flat_world_preset"));
            }).orElse(PresetFlatWorldScreen.UNKNOWN_PRESET);
         }

         public void render(PoseStack pPoseStack, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pIsMouseOver, float pPartialTick) {
            this.blitSlot(pPoseStack, pLeft, pTop, this.preset.displayItem().value());
            PresetFlatWorldScreen.this.font.draw(pPoseStack, this.name, (float)(pLeft + 18 + 5), (float)(pTop + 6), 16777215);
         }

         public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
            if (pButton == 0) {
               this.select();
            }

            return false;
         }

         void select() {
            PresetsList.this.setSelected(this);
            PresetFlatWorldScreen.this.settings = this.preset.settings();
            PresetFlatWorldScreen.this.export.setValue(PresetFlatWorldScreen.save(PresetFlatWorldScreen.this.settings));
            PresetFlatWorldScreen.this.export.moveCursorToStart();
         }

         private void blitSlot(PoseStack pPoseStack, int pX, int pY, Item pItem) {
            this.blitSlotBg(pPoseStack, pX + 1, pY + 1);
            PresetFlatWorldScreen.this.itemRenderer.renderGuiItem(new ItemStack(pItem), pX + 2, pY + 2);
         }

         private void blitSlotBg(PoseStack pPoseStack, int pX, int pY) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShaderTexture(0, GuiComponent.STATS_ICON_LOCATION);
            GuiComponent.blit(pPoseStack, pX, pY, PresetFlatWorldScreen.this.getBlitOffset(), 0.0F, 0.0F, 18, 18, 128, 128);
         }

         public Component getNarration() {
            return Component.translatable("narrator.select", this.name);
         }
      }
   }
}