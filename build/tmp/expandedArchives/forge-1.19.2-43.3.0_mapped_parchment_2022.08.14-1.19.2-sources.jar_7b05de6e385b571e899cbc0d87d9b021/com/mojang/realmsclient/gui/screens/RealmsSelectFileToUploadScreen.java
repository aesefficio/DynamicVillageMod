package com.mojang.realmsclient.gui.screens;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsLabel;
import net.minecraft.realms.RealmsObjectSelectionList;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsSelectFileToUploadScreen extends RealmsScreen {
   private static final Logger LOGGER = LogUtils.getLogger();
   static final Component WORLD_TEXT = Component.translatable("selectWorld.world");
   static final Component HARDCORE_TEXT = Component.translatable("mco.upload.hardcore").withStyle(ChatFormatting.DARK_RED);
   static final Component CHEATS_TEXT = Component.translatable("selectWorld.cheats");
   private static final DateFormat DATE_FORMAT = new SimpleDateFormat();
   private final RealmsResetWorldScreen lastScreen;
   private final long worldId;
   private final int slotId;
   Button uploadButton;
   List<LevelSummary> levelList = Lists.newArrayList();
   int selectedWorld = -1;
   RealmsSelectFileToUploadScreen.WorldSelectionList worldSelectionList;
   private final Runnable callback;

   public RealmsSelectFileToUploadScreen(long pWorldId, int pSlotId, RealmsResetWorldScreen pLastScreen, Runnable pCallback) {
      super(Component.translatable("mco.upload.select.world.title"));
      this.lastScreen = pLastScreen;
      this.worldId = pWorldId;
      this.slotId = pSlotId;
      this.callback = pCallback;
   }

   private void loadLevelList() throws Exception {
      LevelStorageSource.LevelCandidates levelstoragesource$levelcandidates = this.minecraft.getLevelSource().findLevelCandidates();
      this.levelList = this.minecraft.getLevelSource().loadLevelSummaries(levelstoragesource$levelcandidates).join().stream().filter((p_193517_) -> {
         return !p_193517_.requiresManualConversion() && !p_193517_.isLocked();
      }).collect(Collectors.toList());

      for(LevelSummary levelsummary : this.levelList) {
         this.worldSelectionList.addEntry(levelsummary);
      }

   }

   public void init() {
      this.minecraft.keyboardHandler.setSendRepeatsToGui(true);
      this.worldSelectionList = new RealmsSelectFileToUploadScreen.WorldSelectionList();

      try {
         this.loadLevelList();
      } catch (Exception exception) {
         LOGGER.error("Couldn't load level list", (Throwable)exception);
         this.minecraft.setScreen(new RealmsGenericErrorScreen(Component.literal("Unable to load worlds"), Component.nullToEmpty(exception.getMessage()), this.lastScreen));
         return;
      }

      this.addWidget(this.worldSelectionList);
      this.uploadButton = this.addRenderableWidget(new Button(this.width / 2 - 154, this.height - 32, 153, 20, Component.translatable("mco.upload.button.name"), (p_231307_) -> {
         this.upload();
      }));
      this.uploadButton.active = this.selectedWorld >= 0 && this.selectedWorld < this.levelList.size();
      this.addRenderableWidget(new Button(this.width / 2 + 6, this.height - 32, 153, 20, CommonComponents.GUI_BACK, (p_89525_) -> {
         this.minecraft.setScreen(this.lastScreen);
      }));
      this.addLabel(new RealmsLabel(Component.translatable("mco.upload.select.world.subtitle"), this.width / 2, row(-1), 10526880));
      if (this.levelList.isEmpty()) {
         this.addLabel(new RealmsLabel(Component.translatable("mco.upload.select.world.none"), this.width / 2, this.height / 2 - 20, 16777215));
      }

   }

   public Component getNarrationMessage() {
      return CommonComponents.joinForNarration(this.getTitle(), this.createLabelNarration());
   }

   public void removed() {
      this.minecraft.keyboardHandler.setSendRepeatsToGui(false);
   }

   private void upload() {
      if (this.selectedWorld != -1 && !this.levelList.get(this.selectedWorld).isHardcore()) {
         LevelSummary levelsummary = this.levelList.get(this.selectedWorld);
         this.minecraft.setScreen(new RealmsUploadScreen(this.worldId, this.slotId, this.lastScreen, levelsummary, this.callback));
      }

   }

   public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
      this.renderBackground(pPoseStack);
      this.worldSelectionList.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
      drawCenteredString(pPoseStack, this.font, this.title, this.width / 2, 13, 16777215);
      super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
   }

   public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
      if (pKeyCode == 256) {
         this.minecraft.setScreen(this.lastScreen);
         return true;
      } else {
         return super.keyPressed(pKeyCode, pScanCode, pModifiers);
      }
   }

   static Component gameModeName(LevelSummary pLevelSummary) {
      return pLevelSummary.getGameMode().getLongDisplayName();
   }

   static String formatLastPlayed(LevelSummary pLevelSummary) {
      return DATE_FORMAT.format(new Date(pLevelSummary.getLastPlayed()));
   }

   @OnlyIn(Dist.CLIENT)
   class Entry extends ObjectSelectionList.Entry<RealmsSelectFileToUploadScreen.Entry> {
      private final LevelSummary levelSummary;
      private final String name;
      private final String id;
      private final Component info;

      public Entry(LevelSummary pLevelSummary) {
         this.levelSummary = pLevelSummary;
         this.name = pLevelSummary.getLevelName();
         this.id = pLevelSummary.getLevelId() + " (" + RealmsSelectFileToUploadScreen.formatLastPlayed(pLevelSummary) + ")";
         Component component;
         if (pLevelSummary.isHardcore()) {
            component = RealmsSelectFileToUploadScreen.HARDCORE_TEXT;
         } else {
            component = RealmsSelectFileToUploadScreen.gameModeName(pLevelSummary);
         }

         if (pLevelSummary.hasCheats()) {
            component = component.copy().append(", ").append(RealmsSelectFileToUploadScreen.CHEATS_TEXT);
         }

         this.info = component;
      }

      public void render(PoseStack pPoseStack, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pIsMouseOver, float pPartialTick) {
         this.renderItem(pPoseStack, pIndex, pLeft, pTop);
      }

      public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
         RealmsSelectFileToUploadScreen.this.worldSelectionList.selectItem(RealmsSelectFileToUploadScreen.this.levelList.indexOf(this.levelSummary));
         return true;
      }

      protected void renderItem(PoseStack pPoseStack, int pIndex, int pX, int pY) {
         String s;
         if (this.name.isEmpty()) {
            s = RealmsSelectFileToUploadScreen.WORLD_TEXT + " " + (pIndex + 1);
         } else {
            s = this.name;
         }

         RealmsSelectFileToUploadScreen.this.font.draw(pPoseStack, s, (float)(pX + 2), (float)(pY + 1), 16777215);
         RealmsSelectFileToUploadScreen.this.font.draw(pPoseStack, this.id, (float)(pX + 2), (float)(pY + 12), 8421504);
         RealmsSelectFileToUploadScreen.this.font.draw(pPoseStack, this.info, (float)(pX + 2), (float)(pY + 12 + 10), 8421504);
      }

      public Component getNarration() {
         Component component = CommonComponents.joinLines(Component.literal(this.levelSummary.getLevelName()), Component.literal(RealmsSelectFileToUploadScreen.formatLastPlayed(this.levelSummary)), RealmsSelectFileToUploadScreen.gameModeName(this.levelSummary));
         return Component.translatable("narrator.select", component);
      }
   }

   @OnlyIn(Dist.CLIENT)
   class WorldSelectionList extends RealmsObjectSelectionList<RealmsSelectFileToUploadScreen.Entry> {
      public WorldSelectionList() {
         super(RealmsSelectFileToUploadScreen.this.width, RealmsSelectFileToUploadScreen.this.height, RealmsSelectFileToUploadScreen.row(0), RealmsSelectFileToUploadScreen.this.height - 40, 36);
      }

      public void addEntry(LevelSummary pLevelSummary) {
         this.addEntry(RealmsSelectFileToUploadScreen.this.new Entry(pLevelSummary));
      }

      public int getMaxPosition() {
         return RealmsSelectFileToUploadScreen.this.levelList.size() * 36;
      }

      public boolean isFocused() {
         return RealmsSelectFileToUploadScreen.this.getFocused() == this;
      }

      public void renderBackground(PoseStack pPoseStack) {
         RealmsSelectFileToUploadScreen.this.renderBackground(pPoseStack);
      }

      public void setSelected(@Nullable RealmsSelectFileToUploadScreen.Entry pSelected) {
         super.setSelected(pSelected);
         RealmsSelectFileToUploadScreen.this.selectedWorld = this.children().indexOf(pSelected);
         RealmsSelectFileToUploadScreen.this.uploadButton.active = RealmsSelectFileToUploadScreen.this.selectedWorld >= 0 && RealmsSelectFileToUploadScreen.this.selectedWorld < this.getItemCount() && !RealmsSelectFileToUploadScreen.this.levelList.get(RealmsSelectFileToUploadScreen.this.selectedWorld).isHardcore();
      }
   }
}