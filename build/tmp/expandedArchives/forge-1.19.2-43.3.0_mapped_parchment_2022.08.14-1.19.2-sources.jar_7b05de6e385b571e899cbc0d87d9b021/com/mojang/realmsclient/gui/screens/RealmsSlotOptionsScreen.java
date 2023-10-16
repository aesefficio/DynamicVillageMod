package com.mojang.realmsclient.gui.screens;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsWorldOptions;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsLabel;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RealmsSlotOptionsScreen extends RealmsScreen {
   private static final int DEFAULT_DIFFICULTY = 2;
   public static final List<Difficulty> DIFFICULTIES = ImmutableList.of(Difficulty.PEACEFUL, Difficulty.EASY, Difficulty.NORMAL, Difficulty.HARD);
   private static final int DEFAULT_GAME_MODE = 0;
   public static final List<GameType> GAME_MODES = ImmutableList.of(GameType.SURVIVAL, GameType.CREATIVE, GameType.ADVENTURE);
   private static final Component NAME_LABEL = Component.translatable("mco.configure.world.edit.slot.name");
   static final Component SPAWN_PROTECTION_TEXT = Component.translatable("mco.configure.world.spawnProtection");
   private static final Component SPAWN_WARNING_TITLE = Component.translatable("mco.configure.world.spawn_toggle.title").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
   private EditBox nameEdit;
   protected final RealmsConfigureWorldScreen parent;
   private int column1X;
   private int columnWidth;
   private final RealmsWorldOptions options;
   private final RealmsServer.WorldType worldType;
   private Difficulty difficulty;
   private GameType gameMode;
   private final String defaultSlotName;
   private String worldName;
   private boolean pvp;
   private boolean spawnNPCs;
   private boolean spawnAnimals;
   private boolean spawnMonsters;
   int spawnProtection;
   private boolean commandBlocks;
   private boolean forceGameMode;
   RealmsSlotOptionsScreen.SettingsSlider spawnProtectionButton;

   public RealmsSlotOptionsScreen(RealmsConfigureWorldScreen pParent, RealmsWorldOptions pOptions, RealmsServer.WorldType pWorldType, int pActiveSlot) {
      super(Component.translatable("mco.configure.world.buttons.options"));
      this.parent = pParent;
      this.options = pOptions;
      this.worldType = pWorldType;
      this.difficulty = findByIndex(DIFFICULTIES, pOptions.difficulty, 2);
      this.gameMode = findByIndex(GAME_MODES, pOptions.gameMode, 0);
      this.defaultSlotName = pOptions.getDefaultSlotName(pActiveSlot);
      this.setWorldName(pOptions.getSlotName(pActiveSlot));
      if (pWorldType == RealmsServer.WorldType.NORMAL) {
         this.pvp = pOptions.pvp;
         this.spawnProtection = pOptions.spawnProtection;
         this.forceGameMode = pOptions.forceGameMode;
         this.spawnAnimals = pOptions.spawnAnimals;
         this.spawnMonsters = pOptions.spawnMonsters;
         this.spawnNPCs = pOptions.spawnNPCs;
         this.commandBlocks = pOptions.commandBlocks;
      } else {
         this.pvp = true;
         this.spawnProtection = 0;
         this.forceGameMode = false;
         this.spawnAnimals = true;
         this.spawnMonsters = true;
         this.spawnNPCs = true;
         this.commandBlocks = true;
      }

   }

   public void removed() {
      this.minecraft.keyboardHandler.setSendRepeatsToGui(false);
   }

   public void tick() {
      this.nameEdit.tick();
   }

   public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
      if (pKeyCode == 256) {
         this.minecraft.setScreen(this.parent);
         return true;
      } else {
         return super.keyPressed(pKeyCode, pScanCode, pModifiers);
      }
   }

   private static <T> T findByIndex(List<T> p_167525_, int p_167526_, int p_167527_) {
      try {
         return p_167525_.get(p_167526_);
      } catch (IndexOutOfBoundsException indexoutofboundsexception) {
         return p_167525_.get(p_167527_);
      }
   }

   private static <T> int findIndex(List<T> p_167529_, T p_167530_, int p_167531_) {
      int i = p_167529_.indexOf(p_167530_);
      return i == -1 ? p_167531_ : i;
   }

   public void init() {
      this.columnWidth = 170;
      this.column1X = this.width / 2 - this.columnWidth;
      int i = this.width / 2 + 10;
      if (this.worldType != RealmsServer.WorldType.NORMAL) {
         Component component;
         if (this.worldType == RealmsServer.WorldType.ADVENTUREMAP) {
            component = Component.translatable("mco.configure.world.edit.subscreen.adventuremap");
         } else if (this.worldType == RealmsServer.WorldType.INSPIRATION) {
            component = Component.translatable("mco.configure.world.edit.subscreen.inspiration");
         } else {
            component = Component.translatable("mco.configure.world.edit.subscreen.experience");
         }

         this.addLabel(new RealmsLabel(component, this.width / 2, 26, 16711680));
      }

      this.nameEdit = new EditBox(this.minecraft.font, this.column1X + 2, row(1), this.columnWidth - 4, 20, (EditBox)null, Component.translatable("mco.configure.world.edit.slot.name"));
      this.nameEdit.setMaxLength(10);
      this.nameEdit.setValue(this.worldName);
      this.nameEdit.setResponder(this::setWorldName);
      this.magicalSpecialHackyFocus(this.nameEdit);
      CycleButton<Boolean> cyclebutton5 = this.addRenderableWidget(CycleButton.onOffBuilder(this.pvp).create(i, row(1), this.columnWidth, 20, Component.translatable("mco.configure.world.pvp"), (p_167546_, p_167547_) -> {
         this.pvp = p_167547_;
      }));
      this.addRenderableWidget(CycleButton.builder(GameType::getShortDisplayName).withValues(GAME_MODES).withInitialValue(this.gameMode).create(this.column1X, row(3), this.columnWidth, 20, Component.translatable("selectWorld.gameMode"), (p_167515_, p_167516_) -> {
         this.gameMode = p_167516_;
      }));
      Component component1 = Component.translatable("mco.configure.world.spawn_toggle.message");
      CycleButton<Boolean> cyclebutton = this.addRenderableWidget(CycleButton.onOffBuilder(this.spawnAnimals).create(i, row(3), this.columnWidth, 20, Component.translatable("mco.configure.world.spawnAnimals"), this.confirmDangerousOption(component1, (p_231329_) -> {
         this.spawnAnimals = p_231329_;
      })));
      CycleButton<Boolean> cyclebutton1 = CycleButton.onOffBuilder(this.difficulty != Difficulty.PEACEFUL && this.spawnMonsters).create(i, row(5), this.columnWidth, 20, Component.translatable("mco.configure.world.spawnMonsters"), this.confirmDangerousOption(component1, (p_231327_) -> {
         this.spawnMonsters = p_231327_;
      }));
      this.addRenderableWidget(CycleButton.builder(Difficulty::getDisplayName).withValues(DIFFICULTIES).withInitialValue(this.difficulty).create(this.column1X, row(5), this.columnWidth, 20, Component.translatable("options.difficulty"), (p_167519_, p_167520_) -> {
         this.difficulty = p_167520_;
         if (this.worldType == RealmsServer.WorldType.NORMAL) {
            boolean flag = this.difficulty != Difficulty.PEACEFUL;
            cyclebutton1.active = flag;
            cyclebutton1.setValue(flag && this.spawnMonsters);
         }

      }));
      this.addRenderableWidget(cyclebutton1);
      this.spawnProtectionButton = this.addRenderableWidget(new RealmsSlotOptionsScreen.SettingsSlider(this.column1X, row(7), this.columnWidth, this.spawnProtection, 0.0F, 16.0F));
      CycleButton<Boolean> cyclebutton2 = this.addRenderableWidget(CycleButton.onOffBuilder(this.spawnNPCs).create(i, row(7), this.columnWidth, 20, Component.translatable("mco.configure.world.spawnNPCs"), this.confirmDangerousOption(Component.translatable("mco.configure.world.spawn_toggle.message.npc"), (p_231312_) -> {
         this.spawnNPCs = p_231312_;
      })));
      CycleButton<Boolean> cyclebutton3 = this.addRenderableWidget(CycleButton.onOffBuilder(this.forceGameMode).create(this.column1X, row(9), this.columnWidth, 20, Component.translatable("mco.configure.world.forceGameMode"), (p_167534_, p_167535_) -> {
         this.forceGameMode = p_167535_;
      }));
      CycleButton<Boolean> cyclebutton4 = this.addRenderableWidget(CycleButton.onOffBuilder(this.commandBlocks).create(i, row(9), this.columnWidth, 20, Component.translatable("mco.configure.world.commandBlocks"), (p_167522_, p_167523_) -> {
         this.commandBlocks = p_167523_;
      }));
      if (this.worldType != RealmsServer.WorldType.NORMAL) {
         cyclebutton5.active = false;
         cyclebutton.active = false;
         cyclebutton2.active = false;
         cyclebutton1.active = false;
         this.spawnProtectionButton.active = false;
         cyclebutton4.active = false;
         cyclebutton3.active = false;
      }

      if (this.difficulty == Difficulty.PEACEFUL) {
         cyclebutton1.active = false;
      }

      this.addRenderableWidget(new Button(this.column1X, row(13), this.columnWidth, 20, Component.translatable("mco.configure.world.buttons.done"), (p_89910_) -> {
         this.saveSettings();
      }));
      this.addRenderableWidget(new Button(i, row(13), this.columnWidth, 20, CommonComponents.GUI_CANCEL, (p_89905_) -> {
         this.minecraft.setScreen(this.parent);
      }));
      this.addWidget(this.nameEdit);
   }

   private CycleButton.OnValueChange<Boolean> confirmDangerousOption(Component p_231324_, Consumer<Boolean> p_231325_) {
      return (p_231318_, p_231319_) -> {
         if (p_231319_) {
            p_231325_.accept(true);
         } else {
            this.minecraft.setScreen(new ConfirmScreen((p_231322_) -> {
               if (p_231322_) {
                  p_231325_.accept(false);
               }

               this.minecraft.setScreen(this);
            }, SPAWN_WARNING_TITLE, p_231324_, CommonComponents.GUI_PROCEED, CommonComponents.GUI_CANCEL));
         }

      };
   }

   public Component getNarrationMessage() {
      return CommonComponents.joinForNarration(this.getTitle(), this.createLabelNarration());
   }

   public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
      this.renderBackground(pPoseStack);
      drawCenteredString(pPoseStack, this.font, this.title, this.width / 2, 17, 16777215);
      this.font.draw(pPoseStack, NAME_LABEL, (float)(this.column1X + this.columnWidth / 2 - this.font.width(NAME_LABEL) / 2), (float)(row(0) - 5), 16777215);
      this.nameEdit.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
      super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
   }

   private void setWorldName(String p_231314_) {
      if (p_231314_.equals(this.defaultSlotName)) {
         this.worldName = "";
      } else {
         this.worldName = p_231314_;
      }

   }

   private void saveSettings() {
      int i = findIndex(DIFFICULTIES, this.difficulty, 2);
      int j = findIndex(GAME_MODES, this.gameMode, 0);
      if (this.worldType != RealmsServer.WorldType.ADVENTUREMAP && this.worldType != RealmsServer.WorldType.EXPERIENCE && this.worldType != RealmsServer.WorldType.INSPIRATION) {
         boolean flag = this.worldType == RealmsServer.WorldType.NORMAL && this.difficulty != Difficulty.PEACEFUL && this.spawnMonsters;
         this.parent.saveSlotSettings(new RealmsWorldOptions(this.pvp, this.spawnAnimals, flag, this.spawnNPCs, this.spawnProtection, this.commandBlocks, i, j, this.forceGameMode, this.worldName));
      } else {
         this.parent.saveSlotSettings(new RealmsWorldOptions(this.options.pvp, this.options.spawnAnimals, this.options.spawnMonsters, this.options.spawnNPCs, this.options.spawnProtection, this.options.commandBlocks, i, j, this.options.forceGameMode, this.worldName));
      }

   }

   @OnlyIn(Dist.CLIENT)
   class SettingsSlider extends AbstractSliderButton {
      private final double minValue;
      private final double maxValue;

      public SettingsSlider(int pX, int pY, int pWidth, int pValue, float pMinValue, float pMaxValue) {
         super(pX, pY, pWidth, 20, CommonComponents.EMPTY, 0.0D);
         this.minValue = (double)pMinValue;
         this.maxValue = (double)pMaxValue;
         this.value = (double)((Mth.clamp((float)pValue, pMinValue, pMaxValue) - pMinValue) / (pMaxValue - pMinValue));
         this.updateMessage();
      }

      public void applyValue() {
         if (RealmsSlotOptionsScreen.this.spawnProtectionButton.active) {
            RealmsSlotOptionsScreen.this.spawnProtection = (int)Mth.lerp(Mth.clamp(this.value, 0.0D, 1.0D), this.minValue, this.maxValue);
         }
      }

      protected void updateMessage() {
         this.setMessage(CommonComponents.optionNameValue(RealmsSlotOptionsScreen.SPAWN_PROTECTION_TEXT, (Component)(RealmsSlotOptionsScreen.this.spawnProtection == 0 ? CommonComponents.OPTION_OFF : Component.literal(String.valueOf(RealmsSlotOptionsScreen.this.spawnProtection)))));
      }

      public void onClick(double pMouseX, double pMouseY) {
      }

      public void onRelease(double pMouseX, double pMouseY) {
      }
   }
}