package net.minecraft.client.gui.screens.debug;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GameModeSwitcherScreen extends Screen {
   static final ResourceLocation GAMEMODE_SWITCHER_LOCATION = new ResourceLocation("textures/gui/container/gamemode_switcher.png");
   private static final int SPRITE_SHEET_WIDTH = 128;
   private static final int SPRITE_SHEET_HEIGHT = 128;
   private static final int SLOT_AREA = 26;
   private static final int SLOT_PADDING = 5;
   private static final int SLOT_AREA_PADDED = 31;
   private static final int HELP_TIPS_OFFSET_Y = 5;
   private static final int ALL_SLOTS_WIDTH = GameModeSwitcherScreen.GameModeIcon.values().length * 31 - 5;
   private static final Component SELECT_KEY = Component.translatable("debug.gamemodes.select_next", Component.translatable("debug.gamemodes.press_f4").withStyle(ChatFormatting.AQUA));
   private final Optional<GameModeSwitcherScreen.GameModeIcon> previousHovered;
   private Optional<GameModeSwitcherScreen.GameModeIcon> currentlyHovered = Optional.empty();
   private int firstMouseX;
   private int firstMouseY;
   private boolean setFirstMousePos;
   private final List<GameModeSwitcherScreen.GameModeSlot> slots = Lists.newArrayList();

   public GameModeSwitcherScreen() {
      super(GameNarrator.NO_TITLE);
      this.previousHovered = GameModeSwitcherScreen.GameModeIcon.getFromGameType(this.getDefaultSelected());
   }

   private GameType getDefaultSelected() {
      MultiPlayerGameMode multiplayergamemode = Minecraft.getInstance().gameMode;
      GameType gametype = multiplayergamemode.getPreviousPlayerMode();
      if (gametype != null) {
         return gametype;
      } else {
         return multiplayergamemode.getPlayerMode() == GameType.CREATIVE ? GameType.SURVIVAL : GameType.CREATIVE;
      }
   }

   protected void init() {
      super.init();
      this.currentlyHovered = this.previousHovered.isPresent() ? this.previousHovered : GameModeSwitcherScreen.GameModeIcon.getFromGameType(this.minecraft.gameMode.getPlayerMode());

      for(int i = 0; i < GameModeSwitcherScreen.GameModeIcon.VALUES.length; ++i) {
         GameModeSwitcherScreen.GameModeIcon gamemodeswitcherscreen$gamemodeicon = GameModeSwitcherScreen.GameModeIcon.VALUES[i];
         this.slots.add(new GameModeSwitcherScreen.GameModeSlot(gamemodeswitcherscreen$gamemodeicon, this.width / 2 - ALL_SLOTS_WIDTH / 2 + i * 31, this.height / 2 - 31));
      }

   }

   public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
      if (!this.checkToClose()) {
         RenderSystem.setShader(GameRenderer::getPositionTexShader);
         pPoseStack.pushPose();
         RenderSystem.enableBlend();
         RenderSystem.setShaderTexture(0, GAMEMODE_SWITCHER_LOCATION);
         int i = this.width / 2 - 62;
         int j = this.height / 2 - 31 - 27;
         blit(pPoseStack, i, j, 0.0F, 0.0F, 125, 75, 128, 128);
         pPoseStack.popPose();
         super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
         this.currentlyHovered.ifPresent((p_97563_) -> {
            drawCenteredString(pPoseStack, this.font, p_97563_.getName(), this.width / 2, this.height / 2 - 31 - 20, -1);
         });
         drawCenteredString(pPoseStack, this.font, SELECT_KEY, this.width / 2, this.height / 2 + 5, 16777215);
         if (!this.setFirstMousePos) {
            this.firstMouseX = pMouseX;
            this.firstMouseY = pMouseY;
            this.setFirstMousePos = true;
         }

         boolean flag = this.firstMouseX == pMouseX && this.firstMouseY == pMouseY;

         for(GameModeSwitcherScreen.GameModeSlot gamemodeswitcherscreen$gamemodeslot : this.slots) {
            gamemodeswitcherscreen$gamemodeslot.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
            this.currentlyHovered.ifPresent((p_97569_) -> {
               gamemodeswitcherscreen$gamemodeslot.setSelected(p_97569_ == gamemodeswitcherscreen$gamemodeslot.icon);
            });
            if (!flag && gamemodeswitcherscreen$gamemodeslot.isHoveredOrFocused()) {
               this.currentlyHovered = Optional.of(gamemodeswitcherscreen$gamemodeslot.icon);
            }
         }

      }
   }

   private void switchToHoveredGameMode() {
      switchToHoveredGameMode(this.minecraft, this.currentlyHovered);
   }

   private static void switchToHoveredGameMode(Minecraft pMinecraft, Optional<GameModeSwitcherScreen.GameModeIcon> pIcon) {
      if (pMinecraft.gameMode != null && pMinecraft.player != null && pIcon.isPresent()) {
         Optional<GameModeSwitcherScreen.GameModeIcon> optional = GameModeSwitcherScreen.GameModeIcon.getFromGameType(pMinecraft.gameMode.getPlayerMode());
         GameModeSwitcherScreen.GameModeIcon gamemodeswitcherscreen$gamemodeicon = pIcon.get();
         if (optional.isPresent() && pMinecraft.player.hasPermissions(2) && gamemodeswitcherscreen$gamemodeicon != optional.get()) {
            pMinecraft.player.commandUnsigned(gamemodeswitcherscreen$gamemodeicon.getCommand());
         }

      }
   }

   private boolean checkToClose() {
      if (!InputConstants.isKeyDown(this.minecraft.getWindow().getWindow(), 292)) {
         this.switchToHoveredGameMode();
         this.minecraft.setScreen((Screen)null);
         return true;
      } else {
         return false;
      }
   }

   public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
      if (pKeyCode == 293 && this.currentlyHovered.isPresent()) {
         this.setFirstMousePos = false;
         this.currentlyHovered = this.currentlyHovered.get().getNext();
         return true;
      } else {
         return super.keyPressed(pKeyCode, pScanCode, pModifiers);
      }
   }

   public boolean isPauseScreen() {
      return false;
   }

   @OnlyIn(Dist.CLIENT)
   static enum GameModeIcon {
      CREATIVE(Component.translatable("gameMode.creative"), "gamemode creative", new ItemStack(Blocks.GRASS_BLOCK)),
      SURVIVAL(Component.translatable("gameMode.survival"), "gamemode survival", new ItemStack(Items.IRON_SWORD)),
      ADVENTURE(Component.translatable("gameMode.adventure"), "gamemode adventure", new ItemStack(Items.MAP)),
      SPECTATOR(Component.translatable("gameMode.spectator"), "gamemode spectator", new ItemStack(Items.ENDER_EYE));

      protected static final GameModeSwitcherScreen.GameModeIcon[] VALUES = values();
      private static final int ICON_AREA = 16;
      protected static final int ICON_TOP_LEFT = 5;
      final Component name;
      final String command;
      final ItemStack renderStack;

      private GameModeIcon(Component pName, String pCommand, ItemStack pRenderStack) {
         this.name = pName;
         this.command = pCommand;
         this.renderStack = pRenderStack;
      }

      void drawIcon(ItemRenderer pItemRenderer, int pX, int pY) {
         pItemRenderer.renderAndDecorateItem(this.renderStack, pX, pY);
      }

      Component getName() {
         return this.name;
      }

      String getCommand() {
         return this.command;
      }

      Optional<GameModeSwitcherScreen.GameModeIcon> getNext() {
         switch (this) {
            case CREATIVE:
               return Optional.of(SURVIVAL);
            case SURVIVAL:
               return Optional.of(ADVENTURE);
            case ADVENTURE:
               return Optional.of(SPECTATOR);
            default:
               return Optional.of(CREATIVE);
         }
      }

      static Optional<GameModeSwitcherScreen.GameModeIcon> getFromGameType(GameType pGameType) {
         switch (pGameType) {
            case SPECTATOR:
               return Optional.of(SPECTATOR);
            case SURVIVAL:
               return Optional.of(SURVIVAL);
            case CREATIVE:
               return Optional.of(CREATIVE);
            case ADVENTURE:
               return Optional.of(ADVENTURE);
            default:
               return Optional.empty();
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   public class GameModeSlot extends AbstractWidget {
      final GameModeSwitcherScreen.GameModeIcon icon;
      private boolean isSelected;

      public GameModeSlot(GameModeSwitcherScreen.GameModeIcon pIcon, int pX, int pY) {
         super(pX, pY, 26, 26, pIcon.getName());
         this.icon = pIcon;
      }

      public void renderButton(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
         Minecraft minecraft = Minecraft.getInstance();
         this.drawSlot(pPoseStack, minecraft.getTextureManager());
         this.icon.drawIcon(GameModeSwitcherScreen.this.itemRenderer, this.x + 5, this.y + 5);
         if (this.isSelected) {
            this.drawSelection(pPoseStack, minecraft.getTextureManager());
         }

      }

      public void updateNarration(NarrationElementOutput pNarrationElementOutput) {
         this.defaultButtonNarrationText(pNarrationElementOutput);
      }

      public boolean isHoveredOrFocused() {
         return super.isHoveredOrFocused() || this.isSelected;
      }

      public void setSelected(boolean pIsSelected) {
         this.isSelected = pIsSelected;
      }

      private void drawSlot(PoseStack pPoseStack, TextureManager pTextureManager) {
         RenderSystem.setShader(GameRenderer::getPositionTexShader);
         RenderSystem.setShaderTexture(0, GameModeSwitcherScreen.GAMEMODE_SWITCHER_LOCATION);
         pPoseStack.pushPose();
         pPoseStack.translate((double)this.x, (double)this.y, 0.0D);
         blit(pPoseStack, 0, 0, 0.0F, 75.0F, 26, 26, 128, 128);
         pPoseStack.popPose();
      }

      private void drawSelection(PoseStack pPoseStack, TextureManager pTextureManager) {
         RenderSystem.setShader(GameRenderer::getPositionTexShader);
         RenderSystem.setShaderTexture(0, GameModeSwitcherScreen.GAMEMODE_SWITCHER_LOCATION);
         pPoseStack.pushPose();
         pPoseStack.translate((double)this.x, (double)this.y, 0.0D);
         blit(pPoseStack, 0, 0, 26.0F, 75.0F, 26, 26, 128, 128);
         pPoseStack.popPose();
      }
   }
}