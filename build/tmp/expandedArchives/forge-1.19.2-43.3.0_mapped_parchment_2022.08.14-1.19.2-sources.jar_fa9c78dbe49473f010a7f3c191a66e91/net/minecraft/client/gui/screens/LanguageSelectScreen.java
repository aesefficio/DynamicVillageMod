package net.minecraft.client.gui.screens;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.resources.language.LanguageInfo;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LanguageSelectScreen extends OptionsSubScreen {
   private static final Component WARNING_LABEL = Component.literal("(").append(Component.translatable("options.languageWarning")).append(")").withStyle(ChatFormatting.GRAY);
   /** The List GuiSlot object reference. */
   private LanguageSelectScreen.LanguageSelectionList packSelectionList;
   /** Reference to the LanguageManager object. */
   final LanguageManager languageManager;

   public LanguageSelectScreen(Screen pLastScreen, Options pOptions, LanguageManager pLanguageManager) {
      super(pLastScreen, pOptions, Component.translatable("options.language"));
      this.languageManager = pLanguageManager;
   }

   protected void init() {
      this.packSelectionList = new LanguageSelectScreen.LanguageSelectionList(this.minecraft);
      this.addWidget(this.packSelectionList);
      this.addRenderableWidget(this.options.forceUnicodeFont().createButton(this.options, this.width / 2 - 155, this.height - 38, 150));
      this.addRenderableWidget(new Button(this.width / 2 - 155 + 160, this.height - 38, 150, 20, CommonComponents.GUI_DONE, (p_96099_) -> {
         LanguageSelectScreen.LanguageSelectionList.Entry languageselectscreen$languageselectionlist$entry = this.packSelectionList.getSelected();
         if (languageselectscreen$languageselectionlist$entry != null && !languageselectscreen$languageselectionlist$entry.language.getCode().equals(this.languageManager.getSelected().getCode())) {
            this.languageManager.setSelected(languageselectscreen$languageselectionlist$entry.language);
            this.options.languageCode = languageselectscreen$languageselectionlist$entry.language.getCode();
            this.minecraft.reloadResourcePacks();
            this.options.save();
         }

         this.minecraft.setScreen(this.lastScreen);
      }));
      super.init();
   }

   public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
      this.packSelectionList.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
      drawCenteredString(pPoseStack, this.font, this.title, this.width / 2, 16, 16777215);
      drawCenteredString(pPoseStack, this.font, WARNING_LABEL, this.width / 2, this.height - 56, 8421504);
      super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
   }

   @OnlyIn(Dist.CLIENT)
   class LanguageSelectionList extends ObjectSelectionList<LanguageSelectScreen.LanguageSelectionList.Entry> {
      public LanguageSelectionList(Minecraft pMinecraft) {
         super(pMinecraft, LanguageSelectScreen.this.width, LanguageSelectScreen.this.height, 32, LanguageSelectScreen.this.height - 65 + 4, 18);

         for(LanguageInfo languageinfo : LanguageSelectScreen.this.languageManager.getLanguages()) {
            LanguageSelectScreen.LanguageSelectionList.Entry languageselectscreen$languageselectionlist$entry = new LanguageSelectScreen.LanguageSelectionList.Entry(languageinfo);
            this.addEntry(languageselectscreen$languageselectionlist$entry);
            if (LanguageSelectScreen.this.languageManager.getSelected().getCode().equals(languageinfo.getCode())) {
               this.setSelected(languageselectscreen$languageselectionlist$entry);
            }
         }

         if (this.getSelected() != null) {
            this.centerScrollOn(this.getSelected());
         }

      }

      protected int getScrollbarPosition() {
         return super.getScrollbarPosition() + 20;
      }

      public int getRowWidth() {
         return super.getRowWidth() + 50;
      }

      protected void renderBackground(PoseStack pPoseStack) {
         LanguageSelectScreen.this.renderBackground(pPoseStack);
      }

      protected boolean isFocused() {
         return LanguageSelectScreen.this.getFocused() == this;
      }

      @OnlyIn(Dist.CLIENT)
      public class Entry extends ObjectSelectionList.Entry<LanguageSelectScreen.LanguageSelectionList.Entry> {
         final LanguageInfo language;

         public Entry(LanguageInfo pLanguage) {
            this.language = pLanguage;
         }

         public void render(PoseStack pPoseStack, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pIsMouseOver, float pPartialTick) {
            String s = this.language.toString();
            LanguageSelectScreen.this.font.drawShadow(pPoseStack, s, (float)(LanguageSelectionList.this.width / 2 - LanguageSelectScreen.this.font.width(s) / 2), (float)(pTop + 1), 16777215, true);
         }

         public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
            if (pButton == 0) {
               this.select();
               return true;
            } else {
               return false;
            }
         }

         private void select() {
            LanguageSelectionList.this.setSelected(this);
         }

         public Component getNarration() {
            return Component.translatable("narrator.select", this.language);
         }
      }
   }
}