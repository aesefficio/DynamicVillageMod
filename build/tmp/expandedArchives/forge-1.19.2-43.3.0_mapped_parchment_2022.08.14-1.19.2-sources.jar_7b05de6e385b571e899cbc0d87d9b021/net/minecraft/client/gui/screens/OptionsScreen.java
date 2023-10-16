package net.minecraft.client.gui.screens;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.LockIconButton;
import net.minecraft.client.gui.screens.controls.ControlsScreen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ServerboundLockDifficultyPacket;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.Difficulty;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class OptionsScreen extends Screen {
   private final Screen lastScreen;
   private final Options options;
   private CycleButton<Difficulty> difficultyButton;
   private LockIconButton lockButton;

   public OptionsScreen(Screen pLastScreen, Options pOptions) {
      super(Component.translatable("options.title"));
      this.lastScreen = pLastScreen;
      this.options = pOptions;
   }

   protected void init() {
      int i = 0;

      for(OptionInstance<?> optioninstance : new OptionInstance[]{this.options.fov()}) {
         int j = this.width / 2 - 155 + i % 2 * 160;
         int k = this.height / 6 - 12 + 24 * (i >> 1);
         this.addRenderableWidget(optioninstance.createButton(this.minecraft.options, j, k, 150));
         ++i;
      }

      if (this.minecraft.level != null && this.minecraft.hasSingleplayerServer()) {
         this.difficultyButton = this.addRenderableWidget(createDifficultyButton(i, this.width, this.height, "options.difficulty", this.minecraft));
         if (!this.minecraft.level.getLevelData().isHardcore()) {
            this.difficultyButton.setWidth(this.difficultyButton.getWidth() - 20);
            this.lockButton = this.addRenderableWidget(new LockIconButton(this.difficultyButton.x + this.difficultyButton.getWidth(), this.difficultyButton.y, (p_193857_) -> {
               this.minecraft.setScreen(new ConfirmScreen(this::lockCallback, Component.translatable("difficulty.lock.title"), Component.translatable("difficulty.lock.question", this.minecraft.level.getLevelData().getDifficulty().getDisplayName())));
            }));
            this.lockButton.setLocked(this.minecraft.level.getLevelData().isDifficultyLocked());
            this.lockButton.active = !this.lockButton.isLocked();
            this.difficultyButton.active = !this.lockButton.isLocked();
         } else {
            this.difficultyButton.active = false;
         }
      } else {
         this.addRenderableWidget(new Button(this.width / 2 + 5, this.height / 6 - 12 + 24 * (i >> 1), 150, 20, Component.translatable("options.online"), (p_96278_) -> {
            this.minecraft.setScreen(new OnlineOptionsScreen(this, this.options));
         }));
      }

      this.addRenderableWidget(new Button(this.width / 2 - 155, this.height / 6 + 48 - 6, 150, 20, Component.translatable("options.skinCustomisation"), (p_96276_) -> {
         this.minecraft.setScreen(new SkinCustomizationScreen(this, this.options));
      }));
      this.addRenderableWidget(new Button(this.width / 2 + 5, this.height / 6 + 48 - 6, 150, 20, Component.translatable("options.sounds"), (p_96274_) -> {
         this.minecraft.setScreen(new SoundOptionsScreen(this, this.options));
      }));
      this.addRenderableWidget(new Button(this.width / 2 - 155, this.height / 6 + 72 - 6, 150, 20, Component.translatable("options.video"), (p_96272_) -> {
         this.minecraft.setScreen(new VideoSettingsScreen(this, this.options));
      }));
      this.addRenderableWidget(new Button(this.width / 2 + 5, this.height / 6 + 72 - 6, 150, 20, Component.translatable("options.controls"), (p_96270_) -> {
         this.minecraft.setScreen(new ControlsScreen(this, this.options));
      }));
      this.addRenderableWidget(new Button(this.width / 2 - 155, this.height / 6 + 96 - 6, 150, 20, Component.translatable("options.language"), (p_96268_) -> {
         this.minecraft.setScreen(new LanguageSelectScreen(this, this.options, this.minecraft.getLanguageManager()));
      }));
      this.addRenderableWidget(new Button(this.width / 2 + 5, this.height / 6 + 96 - 6, 150, 20, Component.translatable("options.chat.title"), (p_96266_) -> {
         this.minecraft.setScreen(new ChatOptionsScreen(this, this.options));
      }));
      this.addRenderableWidget(new Button(this.width / 2 - 155, this.height / 6 + 120 - 6, 150, 20, Component.translatable("options.resourcepack"), (p_96263_) -> {
         this.minecraft.setScreen(new PackSelectionScreen(this, this.minecraft.getResourcePackRepository(), this::updatePackList, this.minecraft.getResourcePackDirectory(), Component.translatable("resourcePack.title")));
      }));
      this.addRenderableWidget(new Button(this.width / 2 + 5, this.height / 6 + 120 - 6, 150, 20, Component.translatable("options.accessibility.title"), (p_96259_) -> {
         this.minecraft.setScreen(new AccessibilityOptionsScreen(this, this.options));
      }));
      this.addRenderableWidget(new Button(this.width / 2 - 100, this.height / 6 + 168, 200, 20, CommonComponents.GUI_DONE, (p_96257_) -> {
         this.minecraft.setScreen(this.lastScreen);
      }));
   }

   public static CycleButton<Difficulty> createDifficultyButton(int pAmount, int pWidth, int pHeight, String pTranslatableKey, Minecraft pMinecraft) {
      return CycleButton.builder(Difficulty::getDisplayName).withValues(Difficulty.values()).withInitialValue(pMinecraft.level.getDifficulty()).create(pWidth / 2 - 155 + pAmount % 2 * 160, pHeight / 6 - 12 + 24 * (pAmount >> 1), 150, 20, Component.translatable(pTranslatableKey), (p_193854_, p_193855_) -> {
         pMinecraft.getConnection().send(new ServerboundChangeDifficultyPacket(p_193855_));
      });
   }

   private void updatePackList(PackRepository p_96245_) {
      List<String> list = ImmutableList.copyOf(this.options.resourcePacks);
      this.options.resourcePacks.clear();
      this.options.incompatibleResourcePacks.clear();

      for(Pack pack : p_96245_.getSelectedPacks()) {
         if (!pack.isFixedPosition()) {
            this.options.resourcePacks.add(pack.getId());
            if (!pack.getCompatibility().isCompatible()) {
               this.options.incompatibleResourcePacks.add(pack.getId());
            }
         }
      }

      this.options.save();
      List<String> list1 = ImmutableList.copyOf(this.options.resourcePacks);
      if (!list1.equals(list)) {
         this.minecraft.reloadResourcePacks();
      }

   }

   private void lockCallback(boolean p_96261_) {
      this.minecraft.setScreen(this);
      if (p_96261_ && this.minecraft.level != null) {
         this.minecraft.getConnection().send(new ServerboundLockDifficultyPacket(true));
         this.lockButton.setLocked(true);
         this.lockButton.active = false;
         this.difficultyButton.active = false;
      }

   }

   public void removed() {
      this.options.save();
   }

   public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
      this.renderBackground(pPoseStack);
      drawCenteredString(pPoseStack, this.font, this.title, this.width / 2, 15, 16777215);
      super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
   }

    @Override
    public void onClose() {
        // We need to consider 2 potential parent screens here:
        // 1. From the main menu, in which case display the main menu
        // 2. From the pause menu, in which case exit back to game
        this.minecraft.setScreen(this.lastScreen instanceof PauseScreen ? null : this.lastScreen);
    }
}
