package net.minecraft.client.gui.screens.inventory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.HotbarManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.inventory.Hotbar;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.searchtree.SearchRegistry;
import net.minecraft.client.searchtree.SearchTree;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CreativeModeInventoryScreen extends EffectRenderingInventoryScreen<CreativeModeInventoryScreen.ItemPickerMenu> {
   /** The location of the creative inventory tabs texture */
   private static final ResourceLocation CREATIVE_TABS_LOCATION = new ResourceLocation("textures/gui/container/creative_inventory/tabs.png");
   private static final String GUI_CREATIVE_TAB_PREFIX = "textures/gui/container/creative_inventory/tab_";
   private static final String CUSTOM_SLOT_LOCK = "CustomCreativeLock";
   private static final int NUM_ROWS = 5;
   private static final int NUM_COLS = 9;
   private static final int TAB_WIDTH = 28;
   private static final int TAB_HEIGHT = 32;
   private static final int SCROLLER_WIDTH = 12;
   private static final int SCROLLER_HEIGHT = 15;
   static final SimpleContainer CONTAINER = new SimpleContainer(45);
   private static final Component TRASH_SLOT_TOOLTIP = Component.translatable("inventory.binSlot");
   private static final int TEXT_COLOR = 16777215;
   /** Currently selected creative inventory tab index. */
   private static int selectedTab = CreativeModeTab.TAB_BUILDING_BLOCKS.getId();
   /** Amount scrolled in Creative mode inventory (0 = top, 1 = bottom) */
   private float scrollOffs;
   /** True if the scrollbar is being dragged */
   private boolean scrolling;
   private EditBox searchBox;
   @Nullable
   private List<Slot> originalSlots;
   @Nullable
   private Slot destroyItemSlot;
   private CreativeInventoryListener listener;
   private boolean ignoreTextInput;
   private static int tabPage = 0;
   private int maxPages = 0;
   private boolean hasClickedOutside;
   private final Set<TagKey<Item>> visibleTags = new HashSet<>();

   public CreativeModeInventoryScreen(Player pPlayer) {
      super(new CreativeModeInventoryScreen.ItemPickerMenu(pPlayer), pPlayer.getInventory(), CommonComponents.EMPTY);
      pPlayer.containerMenu = this.menu;
      this.passEvents = true;
      this.imageHeight = 136;
      this.imageWidth = 195;
   }

   public void containerTick() {
      super.containerTick();
      if (!this.minecraft.gameMode.hasInfiniteItems()) {
         this.minecraft.setScreen(new InventoryScreen(this.minecraft.player));
      } else if (this.searchBox != null) {
         this.searchBox.tick();
      }

   }

   /**
    * Called when the mouse is clicked over a slot or outside the gui.
    */
   protected void slotClicked(@Nullable Slot pSlot, int pSlotId, int pMouseButton, ClickType pType) {
      if (this.isCreativeSlot(pSlot)) {
         this.searchBox.moveCursorToEnd();
         this.searchBox.setHighlightPos(0);
      }

      boolean flag = pType == ClickType.QUICK_MOVE;
      pType = pSlotId == -999 && pType == ClickType.PICKUP ? ClickType.THROW : pType;
      if (pSlot == null && selectedTab != CreativeModeTab.TAB_INVENTORY.getId() && pType != ClickType.QUICK_CRAFT) {
         if (!this.menu.getCarried().isEmpty() && this.hasClickedOutside) {
            if (pMouseButton == 0) {
               this.minecraft.player.drop(this.menu.getCarried(), true);
               this.minecraft.gameMode.handleCreativeModeItemDrop(this.menu.getCarried());
               this.menu.setCarried(ItemStack.EMPTY);
            }

            if (pMouseButton == 1) {
               ItemStack itemstack5 = this.menu.getCarried().split(1);
               this.minecraft.player.drop(itemstack5, true);
               this.minecraft.gameMode.handleCreativeModeItemDrop(itemstack5);
            }
         }
      } else {
         if (pSlot != null && !pSlot.mayPickup(this.minecraft.player)) {
            return;
         }

         if (pSlot == this.destroyItemSlot && flag) {
            for(int j = 0; j < this.minecraft.player.inventoryMenu.getItems().size(); ++j) {
               this.minecraft.gameMode.handleCreativeModeItemAdd(ItemStack.EMPTY, j);
            }
         } else if (selectedTab == CreativeModeTab.TAB_INVENTORY.getId()) {
            if (pSlot == this.destroyItemSlot) {
               this.menu.setCarried(ItemStack.EMPTY);
            } else if (pType == ClickType.THROW && pSlot != null && pSlot.hasItem()) {
               ItemStack itemstack = pSlot.remove(pMouseButton == 0 ? 1 : pSlot.getItem().getMaxStackSize());
               ItemStack itemstack1 = pSlot.getItem();
               this.minecraft.player.drop(itemstack, true);
               this.minecraft.gameMode.handleCreativeModeItemDrop(itemstack);
               this.minecraft.gameMode.handleCreativeModeItemAdd(itemstack1, ((CreativeModeInventoryScreen.SlotWrapper)pSlot).target.index);
            } else if (pType == ClickType.THROW && !this.menu.getCarried().isEmpty()) {
               this.minecraft.player.drop(this.menu.getCarried(), true);
               this.minecraft.gameMode.handleCreativeModeItemDrop(this.menu.getCarried());
               this.menu.setCarried(ItemStack.EMPTY);
            } else {
               this.minecraft.player.inventoryMenu.clicked(pSlot == null ? pSlotId : ((CreativeModeInventoryScreen.SlotWrapper)pSlot).target.index, pMouseButton, pType, this.minecraft.player);
               this.minecraft.player.inventoryMenu.broadcastChanges();
            }
         } else if (pType != ClickType.QUICK_CRAFT && pSlot.container == CONTAINER) {
            ItemStack itemstack4 = this.menu.getCarried();
            ItemStack itemstack7 = pSlot.getItem();
            if (pType == ClickType.SWAP) {
               if (!itemstack7.isEmpty()) {
                  ItemStack itemstack10 = itemstack7.copy();
                  itemstack10.setCount(itemstack10.getMaxStackSize());
                  this.minecraft.player.getInventory().setItem(pMouseButton, itemstack10);
                  this.minecraft.player.inventoryMenu.broadcastChanges();
               }

               return;
            }

            if (pType == ClickType.CLONE) {
               if (this.menu.getCarried().isEmpty() && pSlot.hasItem()) {
                  ItemStack itemstack9 = pSlot.getItem().copy();
                  itemstack9.setCount(itemstack9.getMaxStackSize());
                  this.menu.setCarried(itemstack9);
               }

               return;
            }

            if (pType == ClickType.THROW) {
               if (!itemstack7.isEmpty()) {
                  ItemStack itemstack8 = itemstack7.copy();
                  itemstack8.setCount(pMouseButton == 0 ? 1 : itemstack8.getMaxStackSize());
                  this.minecraft.player.drop(itemstack8, true);
                  this.minecraft.gameMode.handleCreativeModeItemDrop(itemstack8);
               }

               return;
            }

            if (!itemstack4.isEmpty() && !itemstack7.isEmpty() && itemstack4.sameItem(itemstack7) && ItemStack.tagMatches(itemstack4, itemstack7)) {
               if (pMouseButton == 0) {
                  if (flag) {
                     itemstack4.setCount(itemstack4.getMaxStackSize());
                  } else if (itemstack4.getCount() < itemstack4.getMaxStackSize()) {
                     itemstack4.grow(1);
                  }
               } else {
                  itemstack4.shrink(1);
               }
            } else if (!itemstack7.isEmpty() && itemstack4.isEmpty()) {
               this.menu.setCarried(itemstack7.copy());
               itemstack4 = this.menu.getCarried();
               if (flag) {
                  itemstack4.setCount(itemstack4.getMaxStackSize());
               }
            } else if (pMouseButton == 0) {
               this.menu.setCarried(ItemStack.EMPTY);
            } else {
               this.menu.getCarried().shrink(1);
            }
         } else if (this.menu != null) {
            ItemStack itemstack3 = pSlot == null ? ItemStack.EMPTY : this.menu.getSlot(pSlot.index).getItem();
            this.menu.clicked(pSlot == null ? pSlotId : pSlot.index, pMouseButton, pType, this.minecraft.player);
            if (AbstractContainerMenu.getQuickcraftHeader(pMouseButton) == 2) {
               for(int k = 0; k < 9; ++k) {
                  this.minecraft.gameMode.handleCreativeModeItemAdd(this.menu.getSlot(45 + k).getItem(), 36 + k);
               }
            } else if (pSlot != null) {
               ItemStack itemstack6 = this.menu.getSlot(pSlot.index).getItem();
               this.minecraft.gameMode.handleCreativeModeItemAdd(itemstack6, pSlot.index - (this.menu).slots.size() + 9 + 36);
               int i = 45 + pMouseButton;
               if (pType == ClickType.SWAP) {
                  this.minecraft.gameMode.handleCreativeModeItemAdd(itemstack3, i - (this.menu).slots.size() + 9 + 36);
               } else if (pType == ClickType.THROW && !itemstack3.isEmpty()) {
                  ItemStack itemstack2 = itemstack3.copy();
                  itemstack2.setCount(pMouseButton == 0 ? 1 : itemstack2.getMaxStackSize());
                  this.minecraft.player.drop(itemstack2, true);
                  this.minecraft.gameMode.handleCreativeModeItemDrop(itemstack2);
               }

               this.minecraft.player.inventoryMenu.broadcastChanges();
            }
         }
      }

   }

   private boolean isCreativeSlot(@Nullable Slot pSlot) {
      return pSlot != null && pSlot.container == CONTAINER;
   }

   protected void init() {
      if (this.minecraft.gameMode.hasInfiniteItems()) {
         super.init();
         int tabCount = CreativeModeTab.TABS.length;
         if (tabCount > 12) {
            addRenderableWidget(new net.minecraft.client.gui.components.Button(leftPos,              topPos - 50, 20, 20, Component.literal("<"), b -> tabPage = Math.max(tabPage - 1, 0       )));
            addRenderableWidget(new net.minecraft.client.gui.components.Button(leftPos + imageWidth - 20, topPos - 50, 20, 20, Component.literal(">"), b -> tabPage = Math.min(tabPage + 1, maxPages)));
            maxPages = (int) Math.ceil((tabCount - 12) / 10D);
         }
         this.minecraft.keyboardHandler.setSendRepeatsToGui(true);
         this.searchBox = new EditBox(this.font, this.leftPos + 82, this.topPos + 6, 80, 9, Component.translatable("itemGroup.search"));
         this.searchBox.setMaxLength(50);
         this.searchBox.setBordered(false);
         this.searchBox.setVisible(false);
         this.searchBox.setTextColor(16777215);
         this.addWidget(this.searchBox);
         int i = selectedTab;
         selectedTab = -1;
         this.selectTab(CreativeModeTab.TABS[i]);
         this.minecraft.player.inventoryMenu.removeSlotListener(this.listener);
         this.listener = new CreativeInventoryListener(this.minecraft);
         this.minecraft.player.inventoryMenu.addSlotListener(this.listener);
      } else {
         this.minecraft.setScreen(new InventoryScreen(this.minecraft.player));
      }

   }

   public void resize(Minecraft pMinecraft, int pWidth, int pHeight) {
      String s = this.searchBox.getValue();
      this.init(pMinecraft, pWidth, pHeight);
      this.searchBox.setValue(s);
      if (!this.searchBox.getValue().isEmpty()) {
         this.refreshSearchResults();
      }

   }

   public void removed() {
      super.removed();
      if (this.minecraft.player != null && this.minecraft.player.getInventory() != null) {
         this.minecraft.player.inventoryMenu.removeSlotListener(this.listener);
      }

      this.minecraft.keyboardHandler.setSendRepeatsToGui(false);
   }

   public boolean charTyped(char pCodePoint, int pModifiers) {
      if (this.ignoreTextInput) {
         return false;
      } else if (!CreativeModeTab.TABS[selectedTab].hasSearchBar()) {
         return false;
      } else {
         String s = this.searchBox.getValue();
         if (this.searchBox.charTyped(pCodePoint, pModifiers)) {
            if (!Objects.equals(s, this.searchBox.getValue())) {
               this.refreshSearchResults();
            }

            return true;
         } else {
            return false;
         }
      }
   }

   public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
      this.ignoreTextInput = false;
      if (!CreativeModeTab.TABS[selectedTab].hasSearchBar()) {
         if (this.minecraft.options.keyChat.matches(pKeyCode, pScanCode)) {
            this.ignoreTextInput = true;
            this.selectTab(CreativeModeTab.TAB_SEARCH);
            return true;
         } else {
            return super.keyPressed(pKeyCode, pScanCode, pModifiers);
         }
      } else {
         boolean flag = !this.isCreativeSlot(this.hoveredSlot) || this.hoveredSlot.hasItem();
         boolean flag1 = InputConstants.getKey(pKeyCode, pScanCode).getNumericKeyValue().isPresent();
         if (flag && flag1 && this.checkHotbarKeyPressed(pKeyCode, pScanCode)) {
            this.ignoreTextInput = true;
            return true;
         } else {
            String s = this.searchBox.getValue();
            if (this.searchBox.keyPressed(pKeyCode, pScanCode, pModifiers)) {
               if (!Objects.equals(s, this.searchBox.getValue())) {
                  this.refreshSearchResults();
               }

               return true;
            } else {
               return this.searchBox.isFocused() && this.searchBox.isVisible() && pKeyCode != 256 ? true : super.keyPressed(pKeyCode, pScanCode, pModifiers);
            }
         }
      }
   }

   public boolean keyReleased(int pKeyCode, int pScanCode, int pModifiers) {
      this.ignoreTextInput = false;
      return super.keyReleased(pKeyCode, pScanCode, pModifiers);
   }

   private void refreshSearchResults() {
      (this.menu).items.clear();
      this.visibleTags.clear();

      CreativeModeTab tab = CreativeModeTab.TABS[selectedTab];
      if (tab.hasSearchBar() && tab != CreativeModeTab.TAB_SEARCH) {
         tab.fillItemList(menu.items);
         if (!this.searchBox.getValue().isEmpty()) {
            //TODO: Make this a SearchTree not a manual search
            String search = this.searchBox.getValue().toLowerCase(Locale.ROOT);
            java.util.Iterator<ItemStack> itr = menu.items.iterator();
            while (itr.hasNext()) {
               ItemStack stack = itr.next();
               boolean matches = false;
               for (Component line : stack.getTooltipLines(this.minecraft.player, this.minecraft.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL)) {
                  if (ChatFormatting.stripFormatting(line.getString()).toLowerCase(Locale.ROOT).contains(search)) {
                     matches = true;
                     break;
                  }
               }
               if (!matches)
                  itr.remove();
            }
         }
         this.scrollOffs = 0.0F;
         menu.scrollTo(0.0F);
         return;
      }

      String s = this.searchBox.getValue();
      if (s.isEmpty()) {
         for(Item item : Registry.ITEM) {
            item.fillItemCategory(CreativeModeTab.TAB_SEARCH, (this.menu).items);
         }
      } else {
         SearchTree<ItemStack> searchtree;
         if (s.startsWith("#")) {
            s = s.substring(1);
            searchtree = this.minecraft.getSearchTree(SearchRegistry.CREATIVE_TAGS);
            this.updateVisibleTags(s);
         } else {
            searchtree = this.minecraft.getSearchTree(SearchRegistry.CREATIVE_NAMES);
         }

         (this.menu).items.addAll(searchtree.search(s.toLowerCase(Locale.ROOT)));
      }

      this.scrollOffs = 0.0F;
      this.menu.scrollTo(0.0F);
   }

   private void updateVisibleTags(String pSearch) {
      int i = pSearch.indexOf(58);
      Predicate<ResourceLocation> predicate;
      if (i == -1) {
         predicate = (p_98609_) -> {
            return p_98609_.getPath().contains(pSearch);
         };
      } else {
         String s = pSearch.substring(0, i).trim();
         String s1 = pSearch.substring(i + 1).trim();
         predicate = (p_98606_) -> {
            return p_98606_.getNamespace().contains(s) && p_98606_.getPath().contains(s1);
         };
      }

      Registry.ITEM.getTagNames().filter((p_205410_) -> {
         return predicate.test(p_205410_.location());
      }).forEach(this.visibleTags::add);
   }

   protected void renderLabels(PoseStack pPoseStack, int pX, int pY) {
      CreativeModeTab creativemodetab = CreativeModeTab.TABS[selectedTab];
      if (creativemodetab != null && creativemodetab.showTitle()) {
         RenderSystem.disableBlend();
         this.font.draw(pPoseStack, creativemodetab.getDisplayName(), 8.0F, 6.0F, creativemodetab.getLabelColor());
      }

   }

   public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
      if (pButton == 0) {
         double d0 = pMouseX - (double)this.leftPos;
         double d1 = pMouseY - (double)this.topPos;

         for(CreativeModeTab creativemodetab : CreativeModeTab.TABS) {
            if (creativemodetab != null && this.checkTabClicked(creativemodetab, d0, d1)) {
               return true;
            }
         }

         if (selectedTab != CreativeModeTab.TAB_INVENTORY.getId() && this.insideScrollbar(pMouseX, pMouseY)) {
            this.scrolling = this.canScroll();
            return true;
         }
      }

      return super.mouseClicked(pMouseX, pMouseY, pButton);
   }

   public boolean mouseReleased(double pMouseX, double pMouseY, int pButton) {
      if (pButton == 0) {
         double d0 = pMouseX - (double)this.leftPos;
         double d1 = pMouseY - (double)this.topPos;
         this.scrolling = false;

         for(CreativeModeTab creativemodetab : CreativeModeTab.TABS) {
            if (creativemodetab != null && this.checkTabClicked(creativemodetab, d0, d1)) {
               this.selectTab(creativemodetab);
               return true;
            }
         }
      }

      return super.mouseReleased(pMouseX, pMouseY, pButton);
   }

   /**
    * returns (if you are not on the inventoryTab) and (the flag isn't set) and (you have more than 1 page of items)
    */
   private boolean canScroll() {
      if (CreativeModeTab.TABS[selectedTab] == null) return false;
      return selectedTab != CreativeModeTab.TAB_INVENTORY.getId() && CreativeModeTab.TABS[selectedTab].canScroll() && this.menu.canScroll();
   }

   /**
    * Sets the current creative tab, restructuring the GUI as needed.
    */
   private void selectTab(CreativeModeTab pTab) {
      if (pTab == null) return;
      int i = selectedTab;
      selectedTab = pTab.getId();
      slotColor = pTab.getSlotColor();
      this.quickCraftSlots.clear();
      (this.menu).items.clear();
      this.clearDraggingState();
      if (pTab == CreativeModeTab.TAB_HOTBAR) {
         HotbarManager hotbarmanager = this.minecraft.getHotbarManager();

         for(int j = 0; j < 9; ++j) {
            Hotbar hotbar = hotbarmanager.get(j);
            if (hotbar.isEmpty()) {
               for(int k = 0; k < 9; ++k) {
                  if (k == j) {
                     ItemStack itemstack = new ItemStack(Items.PAPER);
                     itemstack.getOrCreateTagElement("CustomCreativeLock");
                     Component component = this.minecraft.options.keyHotbarSlots[j].getTranslatedKeyMessage();
                     Component component1 = this.minecraft.options.keySaveHotbarActivator.getTranslatedKeyMessage();
                     itemstack.setHoverName(Component.translatable("inventory.hotbarInfo", component1, component));
                     (this.menu).items.add(itemstack);
                  } else {
                     (this.menu).items.add(ItemStack.EMPTY);
                  }
               }
            } else {
               (this.menu).items.addAll(hotbar);
            }
         }
      } else if (pTab != CreativeModeTab.TAB_SEARCH) {
         pTab.fillItemList((this.menu).items);
      }

      if (pTab == CreativeModeTab.TAB_INVENTORY) {
         AbstractContainerMenu abstractcontainermenu = this.minecraft.player.inventoryMenu;
         if (this.originalSlots == null) {
            this.originalSlots = ImmutableList.copyOf((this.menu).slots);
         }

         (this.menu).slots.clear();

         for(int l = 0; l < abstractcontainermenu.slots.size(); ++l) {
            int i1;
            int j1;
            if (l >= 5 && l < 9) {
               int l1 = l - 5;
               int j2 = l1 / 2;
               int l2 = l1 % 2;
               i1 = 54 + j2 * 54;
               j1 = 6 + l2 * 27;
            } else if (l >= 0 && l < 5) {
               i1 = -2000;
               j1 = -2000;
            } else if (l == 45) {
               i1 = 35;
               j1 = 20;
            } else {
               int k1 = l - 9;
               int i2 = k1 % 9;
               int k2 = k1 / 9;
               i1 = 9 + i2 * 18;
               if (l >= 36) {
                  j1 = 112;
               } else {
                  j1 = 54 + k2 * 18;
               }
            }

            Slot slot = new CreativeModeInventoryScreen.SlotWrapper(abstractcontainermenu.slots.get(l), l, i1, j1);
            (this.menu).slots.add(slot);
         }

         this.destroyItemSlot = new Slot(CONTAINER, 0, 173, 112);
         (this.menu).slots.add(this.destroyItemSlot);
      } else if (i == CreativeModeTab.TAB_INVENTORY.getId()) {
         (this.menu).slots.clear();
         (this.menu).slots.addAll(this.originalSlots);
         this.originalSlots = null;
      }

      if (this.searchBox != null) {
         if (pTab.hasSearchBar()) {
            this.searchBox.setVisible(true);
            this.searchBox.setCanLoseFocus(false);
            this.searchBox.setFocus(true);
            if (i != pTab.getId()) {
               this.searchBox.setValue("");
            }
            this.searchBox.setWidth(pTab.getSearchbarWidth());
            this.searchBox.x = this.leftPos + (82 /*default left*/ + 89 /*default width*/) - this.searchBox.getWidth();

            this.refreshSearchResults();
         } else {
            this.searchBox.setVisible(false);
            this.searchBox.setCanLoseFocus(true);
            this.searchBox.setFocus(false);
            this.searchBox.setValue("");
         }
      }

      this.scrollOffs = 0.0F;
      this.menu.scrollTo(0.0F);
   }

   public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
      if (!this.canScroll()) {
         return false;
      } else {
         int i = ((this.menu).items.size() + 9 - 1) / 9 - 5;
         float f = (float)(pDelta / (double)i);
         this.scrollOffs = Mth.clamp(this.scrollOffs - f, 0.0F, 1.0F);
         this.menu.scrollTo(this.scrollOffs);
         return true;
      }
   }

   protected boolean hasClickedOutside(double pMouseX, double pMouseY, int pGuiLeft, int pGuiTop, int pMouseButton) {
      boolean flag = pMouseX < (double)pGuiLeft || pMouseY < (double)pGuiTop || pMouseX >= (double)(pGuiLeft + this.imageWidth) || pMouseY >= (double)(pGuiTop + this.imageHeight);
      this.hasClickedOutside = flag && !this.checkTabClicked(CreativeModeTab.TABS[selectedTab], pMouseX, pMouseY);
      return this.hasClickedOutside;
   }

   protected boolean insideScrollbar(double pMouseX, double pMouseY) {
      int i = this.leftPos;
      int j = this.topPos;
      int k = i + 175;
      int l = j + 18;
      int i1 = k + 14;
      int j1 = l + 112;
      return pMouseX >= (double)k && pMouseY >= (double)l && pMouseX < (double)i1 && pMouseY < (double)j1;
   }

   public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
      if (this.scrolling) {
         int i = this.topPos + 18;
         int j = i + 112;
         this.scrollOffs = ((float)pMouseY - (float)i - 7.5F) / ((float)(j - i) - 15.0F);
         this.scrollOffs = Mth.clamp(this.scrollOffs, 0.0F, 1.0F);
         this.menu.scrollTo(this.scrollOffs);
         return true;
      } else {
         return super.mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY);
      }
   }

   public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
      this.renderBackground(pPoseStack);
      super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);

      int start = tabPage * 10;
      int end = Math.min(CreativeModeTab.TABS.length, ((tabPage + 1) * 10) + 2);
      if (tabPage != 0) start += 2;
      boolean rendered = false;

      for (int x = start; x < end; x++) {
         CreativeModeTab creativemodetab = CreativeModeTab.TABS[x];
         if (creativemodetab != null && this.checkTabHovering(pPoseStack, creativemodetab, pMouseX, pMouseY)) {
            rendered = true;
            break;
         }
      }
      if (!rendered && !this.checkTabHovering(pPoseStack, CreativeModeTab.TAB_SEARCH, pMouseX, pMouseY))
         this.checkTabHovering(pPoseStack, CreativeModeTab.TAB_INVENTORY, pMouseX, pMouseY);

      if (this.destroyItemSlot != null && selectedTab == CreativeModeTab.TAB_INVENTORY.getId() && this.isHovering(this.destroyItemSlot.x, this.destroyItemSlot.y, 16, 16, (double)pMouseX, (double)pMouseY)) {
         this.renderTooltip(pPoseStack, TRASH_SLOT_TOOLTIP, pMouseX, pMouseY);
      }

      if (maxPages != 0) {
          Component page = Component.literal(String.format("%d / %d", tabPage + 1, maxPages + 1));
          this.setBlitOffset(300);
          this.itemRenderer.blitOffset = 300.0F;
          font.drawShadow(pPoseStack, page.getVisualOrderText(), leftPos + (imageWidth / 2) - (font.width(page) / 2), topPos - 44, -1);
          this.setBlitOffset(0);
          this.itemRenderer.blitOffset = 0.0F;
      }

      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      this.renderTooltip(pPoseStack, pMouseX, pMouseY);
   }

   protected void renderTooltip(PoseStack pPoseStack, ItemStack pItemStack, int pMouseX, int pMouseY) {
      if (selectedTab == CreativeModeTab.TAB_SEARCH.getId()) {
         List<Component> list = pItemStack.getTooltipLines(this.minecraft.player, this.minecraft.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL);
         List<Component> list1 = Lists.newArrayList(list);
         Item item = pItemStack.getItem();
         CreativeModeTab creativemodetab = item.getItemCategory();
         if (creativemodetab == null && pItemStack.is(Items.ENCHANTED_BOOK)) {
            Map<Enchantment, Integer> map = EnchantmentHelper.getEnchantments(pItemStack);
            if (map.size() == 1) {
               Enchantment enchantment = map.keySet().iterator().next();

               for(CreativeModeTab creativemodetab1 : CreativeModeTab.TABS) {
                  if (creativemodetab1.hasEnchantmentCategory(enchantment.category)) {
                     creativemodetab = creativemodetab1;
                     break;
                  }
               }
            }
         }

         this.visibleTags.forEach((p_205407_) -> {
            if (pItemStack.is(p_205407_)) {
               list1.add(1, Component.literal("#" + p_205407_.location()).withStyle(ChatFormatting.DARK_PURPLE));
            }

         });
         if (creativemodetab != null) {
            list1.add(1, creativemodetab.getDisplayName().copy().withStyle(ChatFormatting.BLUE));
         }

         this.renderTooltip(pPoseStack, list1, pItemStack.getTooltipImage(), pMouseX, pMouseY, pItemStack);
      } else {
         super.renderTooltip(pPoseStack, pItemStack, pMouseX, pMouseY);
      }

   }

   protected void renderBg(PoseStack pPoseStack, float pPartialTick, int pX, int pY) {
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      CreativeModeTab creativemodetab = CreativeModeTab.TABS[selectedTab];

      int start = tabPage * 10;
      int end = Math.min(CreativeModeTab.TABS.length, ((tabPage + 1) * 10 + 2));
      if (tabPage != 0) start += 2;

      for (int idx = start; idx < end; idx++) {
         RenderSystem.setShader(GameRenderer::getPositionTexShader);
         CreativeModeTab creativemodetab1 = CreativeModeTab.TABS[idx];
         if (creativemodetab1 != null && creativemodetab1.getId() != selectedTab) {
            RenderSystem.setShaderTexture(0, creativemodetab1.getTabsImage());
            this.renderTabButton(pPoseStack, creativemodetab1);
         }
      }

      RenderSystem.setShader(GameRenderer::getPositionTexShader);
      if (tabPage != 0) {
         if (creativemodetab != CreativeModeTab.TAB_SEARCH) {
            RenderSystem.setShaderTexture(0, CreativeModeTab.TAB_SEARCH.getTabsImage());
            renderTabButton(pPoseStack, CreativeModeTab.TAB_SEARCH);
         }
         if (creativemodetab != CreativeModeTab.TAB_INVENTORY) {
            RenderSystem.setShaderTexture(0, CreativeModeTab.TAB_INVENTORY.getTabsImage());
            renderTabButton(pPoseStack, CreativeModeTab.TAB_INVENTORY);
         }
      }

      RenderSystem.setShaderTexture(0, creativemodetab.getBackgroundImage());
      this.blit(pPoseStack, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
      this.searchBox.render(pPoseStack, pX, pY, pPartialTick);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      int i = this.leftPos + 175;
      int j = this.topPos + 18;
      int k = j + 112;
      RenderSystem.setShader(GameRenderer::getPositionTexShader);
      RenderSystem.setShaderTexture(0, creativemodetab.getTabsImage());
      if (creativemodetab.canScroll()) {
         this.blit(pPoseStack, i, j + (int)((float)(k - j - 17) * this.scrollOffs), 232 + (this.canScroll() ? 0 : 12), 0, 12, 15);
      }

      if ((creativemodetab == null || creativemodetab.getTabPage() != tabPage) && (creativemodetab != CreativeModeTab.TAB_SEARCH && creativemodetab != CreativeModeTab.TAB_INVENTORY))
         return;

      this.renderTabButton(pPoseStack, creativemodetab);
      if (creativemodetab == CreativeModeTab.TAB_INVENTORY) {
         InventoryScreen.renderEntityInInventory(this.leftPos + 88, this.topPos + 45, 20, (float)(this.leftPos + 88 - pX), (float)(this.topPos + 45 - 30 - pY), this.minecraft.player);
      }

   }

   protected boolean checkTabClicked(CreativeModeTab pCreativeModeTab, double pRelativeMouseX, double pRelativeMouseY) {
      if (pCreativeModeTab.getTabPage() != tabPage && pCreativeModeTab != CreativeModeTab.TAB_SEARCH && pCreativeModeTab != CreativeModeTab.TAB_INVENTORY) return false;
      int i = pCreativeModeTab.getColumn();
      int j = 28 * i;
      int k = 0;
      if (pCreativeModeTab.isAlignedRight()) {
         j = this.imageWidth - 28 * (6 - i) + 2;
      } else if (i > 0) {
         j += i;
      }

      if (pCreativeModeTab.isTopRow()) {
         k -= 32;
      } else {
         k += this.imageHeight;
      }

      return pRelativeMouseX >= (double)j && pRelativeMouseX <= (double)(j + 28) && pRelativeMouseY >= (double)k && pRelativeMouseY <= (double)(k + 32);
   }

   protected boolean checkTabHovering(PoseStack pPoseStack, CreativeModeTab pCreativeModeTab, int pMouseX, int pMouseY) {
      int i = pCreativeModeTab.getColumn();
      int j = 28 * i;
      int k = 0;
      if (pCreativeModeTab.isAlignedRight()) {
         j = this.imageWidth - 28 * (6 - i) + 2;
      } else if (i > 0) {
         j += i;
      }

      if (pCreativeModeTab.isTopRow()) {
         k -= 32;
      } else {
         k += this.imageHeight;
      }

      if (this.isHovering(j + 3, k + 3, 23, 27, (double)pMouseX, (double)pMouseY)) {
         this.renderTooltip(pPoseStack, pCreativeModeTab.getDisplayName(), pMouseX, pMouseY);
         return true;
      } else {
         return false;
      }
   }

   protected void renderTabButton(PoseStack pPoseStack, CreativeModeTab pCreativeModeTab) {
      boolean flag = pCreativeModeTab.getId() == selectedTab;
      boolean flag1 = pCreativeModeTab.isTopRow();
      int i = pCreativeModeTab.getColumn();
      int j = i * 28;
      int k = 0;
      int l = this.leftPos + 28 * i;
      int i1 = this.topPos;
      int j1 = 32;
      if (flag) {
         k += 32;
      }

      if (pCreativeModeTab.isAlignedRight()) {
         l = this.leftPos + this.imageWidth - 28 * (6 - i);
      } else if (i > 0) {
         l += i;
      }

      if (flag1) {
         i1 -= 28;
      } else {
         k += 64;
         i1 += this.imageHeight - 4;
      }

      RenderSystem.enableBlend(); //Forge: Make sure blend is enabled else tabs show a white border.
      this.blit(pPoseStack, l, i1, j, k, 28, 32);
      this.itemRenderer.blitOffset = 100.0F;
      l += 6;
      i1 += 8 + (flag1 ? 1 : -1);
      ItemStack itemstack = pCreativeModeTab.getIconItem();
      this.itemRenderer.renderAndDecorateItem(itemstack, l, i1);
      this.itemRenderer.renderGuiItemDecorations(this.font, itemstack, l, i1);
      this.itemRenderer.blitOffset = 0.0F;
   }

   /**
    * Returns the index of the currently selected tab.
    */
   public int getSelectedTab() {
      return selectedTab;
   }

   public static void handleHotbarLoadOrSave(Minecraft pClient, int pIndex, boolean pLoad, boolean pSave) {
      LocalPlayer localplayer = pClient.player;
      HotbarManager hotbarmanager = pClient.getHotbarManager();
      Hotbar hotbar = hotbarmanager.get(pIndex);
      if (pLoad) {
         for(int i = 0; i < Inventory.getSelectionSize(); ++i) {
            ItemStack itemstack = hotbar.get(i).copy();
            localplayer.getInventory().setItem(i, itemstack);
            pClient.gameMode.handleCreativeModeItemAdd(itemstack, 36 + i);
         }

         localplayer.inventoryMenu.broadcastChanges();
      } else if (pSave) {
         for(int j = 0; j < Inventory.getSelectionSize(); ++j) {
            hotbar.set(j, localplayer.getInventory().getItem(j).copy());
         }

         Component component1 = pClient.options.keyHotbarSlots[pIndex].getTranslatedKeyMessage();
         Component component2 = pClient.options.keyLoadHotbarActivator.getTranslatedKeyMessage();
         Component component = Component.translatable("inventory.hotbarSaved", component2, component1);
         pClient.gui.setOverlayMessage(component, false);
         pClient.getNarrator().sayNow(component);
         hotbarmanager.save();
      }

   }

   @OnlyIn(Dist.CLIENT)
   static class CustomCreativeSlot extends Slot {
      public CustomCreativeSlot(Container pContainer, int pSlot, int pX, int pY) {
         super(pContainer, pSlot, pX, pY);
      }

      /**
       * Return whether this slot's stack can be taken from this slot.
       */
      public boolean mayPickup(Player pPlayer) {
         if (super.mayPickup(pPlayer) && this.hasItem()) {
            return this.getItem().getTagElement("CustomCreativeLock") == null;
         } else {
            return !this.hasItem();
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class ItemPickerMenu extends AbstractContainerMenu {
      /** The list of items in this container. */
      public final NonNullList<ItemStack> items = NonNullList.create();
      private final AbstractContainerMenu inventoryMenu;

      public ItemPickerMenu(Player pPlayer) {
         super((MenuType<?>)null, 0);
         this.inventoryMenu = pPlayer.inventoryMenu;
         Inventory inventory = pPlayer.getInventory();

         for(int i = 0; i < 5; ++i) {
            for(int j = 0; j < 9; ++j) {
               this.addSlot(new CreativeModeInventoryScreen.CustomCreativeSlot(CreativeModeInventoryScreen.CONTAINER, i * 9 + j, 9 + j * 18, 18 + i * 18));
            }
         }

         for(int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(inventory, k, 9 + k * 18, 112));
         }

         this.scrollTo(0.0F);
      }

      /**
       * Determines whether supplied player can use this container
       */
      public boolean stillValid(Player pPlayer) {
         return true;
      }

      /**
       * Updates the gui slot's ItemStacks based on scroll position.
       */
      public void scrollTo(float pPos) {
         int i = (this.items.size() + 9 - 1) / 9 - 5;
         int j = (int)((double)(pPos * (float)i) + 0.5D);
         if (j < 0) {
            j = 0;
         }

         for(int k = 0; k < 5; ++k) {
            for(int l = 0; l < 9; ++l) {
               int i1 = l + (k + j) * 9;
               if (i1 >= 0 && i1 < this.items.size()) {
                  CreativeModeInventoryScreen.CONTAINER.setItem(l + k * 9, this.items.get(i1));
               } else {
                  CreativeModeInventoryScreen.CONTAINER.setItem(l + k * 9, ItemStack.EMPTY);
               }
            }
         }

      }

      public boolean canScroll() {
         return this.items.size() > 45;
      }

      /**
       * Handle when the stack in slot {@code index} is shift-clicked. Normally this moves the stack between the player
       * inventory and the other inventory(s).
       */
      public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
         if (pIndex >= this.slots.size() - 9 && pIndex < this.slots.size()) {
            Slot slot = this.slots.get(pIndex);
            if (slot != null && slot.hasItem()) {
               slot.set(ItemStack.EMPTY);
            }
         }

         return ItemStack.EMPTY;
      }

      /**
       * Called to determine if the current slot is valid for the stack merging (double-click) code. The stack passed in
       * is null for the initial slot that was double-clicked.
       */
      public boolean canTakeItemForPickAll(ItemStack pStack, Slot pSlot) {
         return pSlot.container != CreativeModeInventoryScreen.CONTAINER;
      }

      /**
       * Returns true if the player can "drag-spilt" items into this slot,. returns true by default. Called to check if
       * the slot can be added to a list of Slots to split the held ItemStack across.
       */
      public boolean canDragTo(Slot pSlot) {
         return pSlot.container != CreativeModeInventoryScreen.CONTAINER;
      }

      public ItemStack getCarried() {
         return this.inventoryMenu.getCarried();
      }

      public void setCarried(ItemStack pStack) {
         this.inventoryMenu.setCarried(pStack);
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class SlotWrapper extends Slot {
      final Slot target;

      public SlotWrapper(Slot pSlot, int pIndex, int pX, int pY) {
         super(pSlot.container, pIndex, pX, pY);
         this.target = pSlot;
      }

      public void onTake(Player pPlayer, ItemStack pStack) {
         this.target.onTake(pPlayer, pStack);
      }

      /**
       * Check if the stack is allowed to be placed in this slot, used for armor slots as well as furnace fuel.
       */
      public boolean mayPlace(ItemStack pStack) {
         return this.target.mayPlace(pStack);
      }

      /**
       * Helper fnct to get the stack in the slot.
       */
      public ItemStack getItem() {
         return this.target.getItem();
      }

      /**
       * Returns if this slot contains a stack.
       */
      public boolean hasItem() {
         return this.target.hasItem();
      }

      /**
       * Helper method to put a stack in the slot.
       */
      public void set(ItemStack pStack) {
         this.target.set(pStack);
      }

      /**
       * Called when the stack in a Slot changes
       */
      public void setChanged() {
         this.target.setChanged();
      }

      /**
       * Returns the maximum stack size for a given slot (usually the same as getInventoryStackLimit(), but 1 in the
       * case of armor slots)
       */
      public int getMaxStackSize() {
         return this.target.getMaxStackSize();
      }

      public int getMaxStackSize(ItemStack pStack) {
         return this.target.getMaxStackSize(pStack);
      }

      @Nullable
      public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
         return this.target.getNoItemIcon();
      }

      /**
       * Decrease the size of the stack in slot (first int arg) by the amount of the second int arg. Returns the new
       * stack.
       */
      public ItemStack remove(int pAmount) {
         return this.target.remove(pAmount);
      }

      /**
       * Actualy only call when we want to render the white square effect over the slots. Return always True, except for
       * the armor slot of the Donkey/Mule (we can't interact with the Undead and Skeleton horses)
       */
      public boolean isActive() {
         return this.target.isActive();
      }

      /**
       * Return whether this slot's stack can be taken from this slot.
       */
      public boolean mayPickup(Player pPlayer) {
         return this.target.mayPickup(pPlayer);
      }

      @Override
      public int getSlotIndex() {
         return this.target.getSlotIndex();
      }

      @Override
      public boolean isSameInventory(Slot other) {
         return this.target.isSameInventory(other);
      }

      @Override
      public Slot setBackground(ResourceLocation atlas, ResourceLocation sprite) {
         this.target.setBackground(atlas, sprite);
         return this;
      }
   }
}
