package net.minecraft.client.gui.screens.controls;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.ArrayUtils;

@OnlyIn(Dist.CLIENT)
public class KeyBindsList extends ContainerObjectSelectionList<KeyBindsList.Entry> {
   final KeyBindsScreen keyBindsScreen;
   int maxNameWidth;

   public KeyBindsList(KeyBindsScreen pKeyBindsScreen, Minecraft pMinecraft) {
      super(pMinecraft, pKeyBindsScreen.width + 45, pKeyBindsScreen.height, 20, pKeyBindsScreen.height - 32, 20);
      this.keyBindsScreen = pKeyBindsScreen;
      KeyMapping[] akeymapping = ArrayUtils.clone((KeyMapping[])pMinecraft.options.keyMappings);
      Arrays.sort((Object[])akeymapping);
      String s = null;

      for(KeyMapping keymapping : akeymapping) {
         String s1 = keymapping.getCategory();
         if (!s1.equals(s)) {
            s = s1;
            this.addEntry(new KeyBindsList.CategoryEntry(Component.translatable(s1)));
         }

         Component component = Component.translatable(keymapping.getName());
         int i = pMinecraft.font.width(component);
         if (i > this.maxNameWidth) {
            this.maxNameWidth = i;
         }

         this.addEntry(new KeyBindsList.KeyEntry(keymapping, component));
      }

   }

   protected int getScrollbarPosition() {
      return super.getScrollbarPosition() + 15 + 20;
   }

   public int getRowWidth() {
      return super.getRowWidth() + 32;
   }

   @OnlyIn(Dist.CLIENT)
   public class CategoryEntry extends KeyBindsList.Entry {
      final Component name;
      private final int width;

      public CategoryEntry(Component pName) {
         this.name = pName;
         this.width = KeyBindsList.this.minecraft.font.width(this.name);
      }

      public void render(PoseStack pPoseStack, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pIsMouseOver, float pPartialTick) {
         KeyBindsList.this.minecraft.font.draw(pPoseStack, this.name, (float)(KeyBindsList.this.minecraft.screen.width / 2 - this.width / 2), (float)(pTop + pHeight - 9 - 1), 16777215);
      }

      public boolean changeFocus(boolean pFocus) {
         return false;
      }

      public List<? extends GuiEventListener> children() {
         return Collections.emptyList();
      }

      public List<? extends NarratableEntry> narratables() {
         return ImmutableList.of(new NarratableEntry() {
            public NarratableEntry.NarrationPriority narrationPriority() {
               return NarratableEntry.NarrationPriority.HOVERED;
            }

            public void updateNarration(NarrationElementOutput p_193906_) {
               p_193906_.add(NarratedElementType.TITLE, CategoryEntry.this.name);
            }
         });
      }
   }

   @OnlyIn(Dist.CLIENT)
   public abstract static class Entry extends ContainerObjectSelectionList.Entry<KeyBindsList.Entry> {
   }

   @OnlyIn(Dist.CLIENT)
   public class KeyEntry extends KeyBindsList.Entry {
      /** The keybinding specified for this KeyEntry */
      private final KeyMapping key;
      /** The localized key description for this KeyEntry */
      private final Component name;
      private final Button changeButton;
      private final Button resetButton;

      KeyEntry(final KeyMapping pKey, final Component pName) {
         this.key = pKey;
         this.name = pName;
         this.changeButton = new Button(0, 0, 75 + 20 /* Forge: Add space */, 20, pName, (p_193939_) -> {
            KeyBindsList.this.keyBindsScreen.selectedKey = pKey;
         }) {
            protected MutableComponent createNarrationMessage() {
               return pKey.isUnbound() ? Component.translatable("narrator.controls.unbound", pName) : Component.translatable("narrator.controls.bound", pName, super.createNarrationMessage());
            }
         };
         this.resetButton = new Button(0, 0, 50, 20, Component.translatable("controls.reset"), (p_193935_) -> {
            this.key.setToDefault();
            KeyBindsList.this.minecraft.options.setKey(pKey, pKey.getDefaultKey());
            KeyMapping.resetMapping();
         }) {
            protected MutableComponent createNarrationMessage() {
               return Component.translatable("narrator.controls.reset", pName);
            }
         };
      }

      public void render(PoseStack pPoseStack, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pIsMouseOver, float pPartialTick) {
         boolean flag = KeyBindsList.this.keyBindsScreen.selectedKey == this.key;
         float f = (float)(pLeft + 90 - KeyBindsList.this.maxNameWidth);
         KeyBindsList.this.minecraft.font.draw(pPoseStack, this.name, f, (float)(pTop + pHeight / 2 - 9 / 2), 16777215);
         this.resetButton.x = pLeft + 190 + 20;
         this.resetButton.y = pTop;
         this.resetButton.active = !this.key.isDefault();
         this.resetButton.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
         this.changeButton.x = pLeft + 105;
         this.changeButton.y = pTop;
         this.changeButton.setMessage(this.key.getTranslatedKeyMessage());
         boolean flag1 = false;
         boolean keyCodeModifierConflict = true; // gracefully handle conflicts like SHIFT vs SHIFT+G
         if (!this.key.isUnbound()) {
            for(KeyMapping keymapping : KeyBindsList.this.minecraft.options.keyMappings) {
               if (keymapping != this.key && this.key.same(keymapping)) {
                  flag1 = true;
                  keyCodeModifierConflict &= keymapping.hasKeyModifierConflict(this.key);
               }
            }
         }

         if (flag) {
            this.changeButton.setMessage(Component.literal("> ").append(this.changeButton.getMessage().copy().withStyle(ChatFormatting.YELLOW)).append(" <").withStyle(ChatFormatting.YELLOW));
         } else if (flag1) {
            this.changeButton.setMessage(this.changeButton.getMessage().copy().withStyle(keyCodeModifierConflict ? ChatFormatting.GOLD : ChatFormatting.RED));
         }

         this.changeButton.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
      }

      public List<? extends GuiEventListener> children() {
         return ImmutableList.of(this.changeButton, this.resetButton);
      }

      public List<? extends NarratableEntry> narratables() {
         return ImmutableList.of(this.changeButton, this.resetButton);
      }

      public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
         if (this.changeButton.mouseClicked(pMouseX, pMouseY, pButton)) {
            return true;
         } else {
            return this.resetButton.mouseClicked(pMouseX, pMouseY, pButton);
         }
      }

      public boolean mouseReleased(double pMouseX, double pMouseY, int pButton) {
         return this.changeButton.mouseReleased(pMouseX, pMouseY, pButton) || this.resetButton.mouseReleased(pMouseX, pMouseY, pButton);
      }
   }
}
