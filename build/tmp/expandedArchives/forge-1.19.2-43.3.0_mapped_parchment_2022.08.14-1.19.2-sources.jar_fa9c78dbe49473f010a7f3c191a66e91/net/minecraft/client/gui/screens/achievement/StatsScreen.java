package net.minecraft.client.gui.screens.achievement;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import net.minecraft.stats.Stats;
import net.minecraft.stats.StatsCounter;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class StatsScreen extends Screen implements StatsUpdateListener {
   private static final Component PENDING_TEXT = Component.translatable("multiplayer.downloadingStats");
   protected final Screen lastScreen;
   private StatsScreen.GeneralStatisticsList statsList;
   StatsScreen.ItemStatisticsList itemStatsList;
   private StatsScreen.MobsStatisticsList mobsStatsList;
   final StatsCounter stats;
   @Nullable
   private ObjectSelectionList<?> activeList;
   /** When true, the game will be paused when the gui is shown */
   private boolean isLoading = true;
   private static final int SLOT_TEX_SIZE = 128;
   private static final int SLOT_BG_SIZE = 18;
   private static final int SLOT_STAT_HEIGHT = 20;
   private static final int SLOT_BG_X = 1;
   private static final int SLOT_BG_Y = 1;
   private static final int SLOT_FG_X = 2;
   private static final int SLOT_FG_Y = 2;
   private static final int SLOT_LEFT_INSERT = 40;
   private static final int SLOT_TEXT_OFFSET = 5;
   private static final int SORT_NONE = 0;
   private static final int SORT_DOWN = -1;
   private static final int SORT_UP = 1;

   public StatsScreen(Screen pLastScreen, StatsCounter pStats) {
      super(Component.translatable("gui.stats"));
      this.lastScreen = pLastScreen;
      this.stats = pStats;
   }

   protected void init() {
      this.isLoading = true;
      this.minecraft.getConnection().send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.REQUEST_STATS));
   }

   public void initLists() {
      this.statsList = new StatsScreen.GeneralStatisticsList(this.minecraft);
      this.itemStatsList = new StatsScreen.ItemStatisticsList(this.minecraft);
      this.mobsStatsList = new StatsScreen.MobsStatisticsList(this.minecraft);
   }

   public void initButtons() {
      this.addRenderableWidget(new Button(this.width / 2 - 120, this.height - 52, 80, 20, Component.translatable("stat.generalButton"), (p_96963_) -> {
         this.setActiveList(this.statsList);
      }));
      Button button = this.addRenderableWidget(new Button(this.width / 2 - 40, this.height - 52, 80, 20, Component.translatable("stat.itemsButton"), (p_96959_) -> {
         this.setActiveList(this.itemStatsList);
      }));
      Button button1 = this.addRenderableWidget(new Button(this.width / 2 + 40, this.height - 52, 80, 20, Component.translatable("stat.mobsButton"), (p_96949_) -> {
         this.setActiveList(this.mobsStatsList);
      }));
      this.addRenderableWidget(new Button(this.width / 2 - 100, this.height - 28, 200, 20, CommonComponents.GUI_DONE, (p_96923_) -> {
         this.minecraft.setScreen(this.lastScreen);
      }));
      if (this.itemStatsList.children().isEmpty()) {
         button.active = false;
      }

      if (this.mobsStatsList.children().isEmpty()) {
         button1.active = false;
      }

   }

   public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
      if (this.isLoading) {
         this.renderBackground(pPoseStack);
         drawCenteredString(pPoseStack, this.font, PENDING_TEXT, this.width / 2, this.height / 2, 16777215);
         drawCenteredString(pPoseStack, this.font, LOADING_SYMBOLS[(int)(Util.getMillis() / 150L % (long)LOADING_SYMBOLS.length)], this.width / 2, this.height / 2 + 9 * 2, 16777215);
      } else {
         this.getActiveList().render(pPoseStack, pMouseX, pMouseY, pPartialTick);
         drawCenteredString(pPoseStack, this.font, this.title, this.width / 2, 20, 16777215);
         super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
      }

   }

   public void onStatsUpdated() {
      if (this.isLoading) {
         this.initLists();
         this.initButtons();
         this.setActiveList(this.statsList);
         this.isLoading = false;
      }

   }

   public boolean isPauseScreen() {
      return !this.isLoading;
   }

   @Nullable
   public ObjectSelectionList<?> getActiveList() {
      return this.activeList;
   }

   public void setActiveList(@Nullable ObjectSelectionList<?> pActiveList) {
      if (this.activeList != null) {
         this.removeWidget(this.activeList);
      }

      if (pActiveList != null) {
         this.addWidget(pActiveList);
         this.activeList = pActiveList;
      }

   }

   static String getTranslationKey(Stat<ResourceLocation> pStat) {
      return "stat." + pStat.getValue().toString().replace(':', '.');
   }

   int getColumnX(int pIndex) {
      return 115 + 40 * pIndex;
   }

   void blitSlot(PoseStack pPoseStack, int pX, int pY, Item pItem) {
      this.blitSlotIcon(pPoseStack, pX + 1, pY + 1, 0, 0);
      this.itemRenderer.renderGuiItem(pItem.getDefaultInstance(), pX + 2, pY + 2);
   }

   void blitSlotIcon(PoseStack pPoseStack, int pX, int pY, int pUOffset, int pVOffset) {
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.setShader(GameRenderer::getPositionTexShader);
      RenderSystem.setShaderTexture(0, STATS_ICON_LOCATION);
      blit(pPoseStack, pX, pY, this.getBlitOffset(), (float)pUOffset, (float)pVOffset, 18, 18, 128, 128);
   }

   @OnlyIn(Dist.CLIENT)
   class GeneralStatisticsList extends ObjectSelectionList<StatsScreen.GeneralStatisticsList.Entry> {
      public GeneralStatisticsList(Minecraft pMinecraft) {
         super(pMinecraft, StatsScreen.this.width, StatsScreen.this.height, 32, StatsScreen.this.height - 64, 10);
         ObjectArrayList<Stat<ResourceLocation>> objectarraylist = new ObjectArrayList<>(Stats.CUSTOM.iterator());
         objectarraylist.sort(Comparator.comparing((p_96997_) -> {
            return I18n.get(StatsScreen.getTranslationKey(p_96997_));
         }));

         for(Stat<ResourceLocation> stat : objectarraylist) {
            this.addEntry(new StatsScreen.GeneralStatisticsList.Entry(stat));
         }

      }

      protected void renderBackground(PoseStack pPoseStack) {
         StatsScreen.this.renderBackground(pPoseStack);
      }

      @OnlyIn(Dist.CLIENT)
      class Entry extends ObjectSelectionList.Entry<StatsScreen.GeneralStatisticsList.Entry> {
         private final Stat<ResourceLocation> stat;
         private final Component statDisplay;

         Entry(Stat<ResourceLocation> pStat) {
            this.stat = pStat;
            this.statDisplay = Component.translatable(StatsScreen.getTranslationKey(pStat));
         }

         private String getValueText() {
            return this.stat.format(StatsScreen.this.stats.getValue(this.stat));
         }

         public void render(PoseStack pPoseStack, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pIsMouseOver, float pPartialTick) {
            GuiComponent.drawString(pPoseStack, StatsScreen.this.font, this.statDisplay, pLeft + 2, pTop + 1, pIndex % 2 == 0 ? 16777215 : 9474192);
            String s = this.getValueText();
            GuiComponent.drawString(pPoseStack, StatsScreen.this.font, s, pLeft + 2 + 213 - StatsScreen.this.font.width(s), pTop + 1, pIndex % 2 == 0 ? 16777215 : 9474192);
         }

         public Component getNarration() {
            return Component.translatable("narrator.select", Component.empty().append(this.statDisplay).append(" ").append(this.getValueText()));
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   class ItemStatisticsList extends ObjectSelectionList<StatsScreen.ItemStatisticsList.ItemRow> {
      protected final List<StatType<Block>> blockColumns;
      protected final List<StatType<Item>> itemColumns;
      private final int[] iconOffsets = new int[]{3, 4, 1, 2, 5, 6};
      protected int headerPressed = -1;
      protected final Comparator<StatsScreen.ItemStatisticsList.ItemRow> itemStatSorter = new StatsScreen.ItemStatisticsList.ItemRowComparator();
      @Nullable
      protected StatType<?> sortColumn;
      protected int sortOrder;

      public ItemStatisticsList(Minecraft pMinecraft) {
         super(pMinecraft, StatsScreen.this.width, StatsScreen.this.height, 32, StatsScreen.this.height - 64, 20);
         this.blockColumns = Lists.newArrayList();
         this.blockColumns.add(Stats.BLOCK_MINED);
         this.itemColumns = Lists.newArrayList(Stats.ITEM_BROKEN, Stats.ITEM_CRAFTED, Stats.ITEM_USED, Stats.ITEM_PICKED_UP, Stats.ITEM_DROPPED);
         this.setRenderHeader(true, 20);
         Set<Item> set = Sets.newIdentityHashSet();

         for(Item item : Registry.ITEM) {
            boolean flag = false;

            for(StatType<Item> stattype : this.itemColumns) {
               if (stattype.contains(item) && StatsScreen.this.stats.getValue(stattype.get(item)) > 0) {
                  flag = true;
               }
            }

            if (flag) {
               set.add(item);
            }
         }

         for(Block block : Registry.BLOCK) {
            boolean flag1 = false;

            for(StatType<Block> stattype1 : this.blockColumns) {
               if (stattype1.contains(block) && StatsScreen.this.stats.getValue(stattype1.get(block)) > 0) {
                  flag1 = true;
               }
            }

            if (flag1) {
               set.add(block.asItem());
            }
         }

         set.remove(Items.AIR);

         for(Item item1 : set) {
            this.addEntry(new StatsScreen.ItemStatisticsList.ItemRow(item1));
         }

      }

      protected void renderHeader(PoseStack pPoseStack, int pX, int pY, Tesselator pTessellator) {
         if (!this.minecraft.mouseHandler.isLeftPressed()) {
            this.headerPressed = -1;
         }

         for(int i = 0; i < this.iconOffsets.length; ++i) {
            StatsScreen.this.blitSlotIcon(pPoseStack, pX + StatsScreen.this.getColumnX(i) - 18, pY + 1, 0, this.headerPressed == i ? 0 : 18);
         }

         if (this.sortColumn != null) {
            int k = StatsScreen.this.getColumnX(this.getColumnIndex(this.sortColumn)) - 36;
            int j = this.sortOrder == 1 ? 2 : 1;
            StatsScreen.this.blitSlotIcon(pPoseStack, pX + k, pY + 1, 18 * j, 0);
         }

         for(int l = 0; l < this.iconOffsets.length; ++l) {
            int i1 = this.headerPressed == l ? 1 : 0;
            StatsScreen.this.blitSlotIcon(pPoseStack, pX + StatsScreen.this.getColumnX(l) - 18 + i1, pY + 1 + i1, 18 * this.iconOffsets[l], 18);
         }

      }

      public int getRowWidth() {
         return 375;
      }

      protected int getScrollbarPosition() {
         return this.width / 2 + 140;
      }

      protected void renderBackground(PoseStack pPoseStack) {
         StatsScreen.this.renderBackground(pPoseStack);
      }

      protected void clickedHeader(int pMouseX, int pMouseY) {
         this.headerPressed = -1;

         for(int i = 0; i < this.iconOffsets.length; ++i) {
            int j = pMouseX - StatsScreen.this.getColumnX(i);
            if (j >= -36 && j <= 0) {
               this.headerPressed = i;
               break;
            }
         }

         if (this.headerPressed >= 0) {
            this.sortByColumn(this.getColumn(this.headerPressed));
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
         }

      }

      private StatType<?> getColumn(int pIndex) {
         return pIndex < this.blockColumns.size() ? this.blockColumns.get(pIndex) : this.itemColumns.get(pIndex - this.blockColumns.size());
      }

      private int getColumnIndex(StatType<?> pStatType) {
         int i = this.blockColumns.indexOf(pStatType);
         if (i >= 0) {
            return i;
         } else {
            int j = this.itemColumns.indexOf(pStatType);
            return j >= 0 ? j + this.blockColumns.size() : -1;
         }
      }

      protected void renderDecorations(PoseStack pPoseStack, int pMouseX, int pMouseY) {
         if (pMouseY >= this.y0 && pMouseY <= this.y1) {
            StatsScreen.ItemStatisticsList.ItemRow statsscreen$itemstatisticslist$itemrow = this.getHovered();
            int i = (this.width - this.getRowWidth()) / 2;
            if (statsscreen$itemstatisticslist$itemrow != null) {
               if (pMouseX < i + 40 || pMouseX > i + 40 + 20) {
                  return;
               }

               Item item = statsscreen$itemstatisticslist$itemrow.getItem();
               this.renderMousehoverTooltip(pPoseStack, this.getString(item), pMouseX, pMouseY);
            } else {
               Component component = null;
               int j = pMouseX - i;

               for(int k = 0; k < this.iconOffsets.length; ++k) {
                  int l = StatsScreen.this.getColumnX(k);
                  if (j >= l - 18 && j <= l) {
                     component = this.getColumn(k).getDisplayName();
                     break;
                  }
               }

               this.renderMousehoverTooltip(pPoseStack, component, pMouseX, pMouseY);
            }

         }
      }

      protected void renderMousehoverTooltip(PoseStack pPoseStack, @Nullable Component pComponent, int pMouseX, int pMouseY) {
         if (pComponent != null) {
            int i = pMouseX + 12;
            int j = pMouseY - 12;
            int k = StatsScreen.this.font.width(pComponent);
            this.fillGradient(pPoseStack, i - 3, j - 3, i + k + 3, j + 8 + 3, -1073741824, -1073741824);
            pPoseStack.pushPose();
            pPoseStack.translate(0.0D, 0.0D, 400.0D);
            StatsScreen.this.font.drawShadow(pPoseStack, pComponent, (float)i, (float)j, -1);
            pPoseStack.popPose();
         }
      }

      protected Component getString(Item pItem) {
         return pItem.getDescription();
      }

      protected void sortByColumn(StatType<?> pStatType) {
         if (pStatType != this.sortColumn) {
            this.sortColumn = pStatType;
            this.sortOrder = -1;
         } else if (this.sortOrder == -1) {
            this.sortOrder = 1;
         } else {
            this.sortColumn = null;
            this.sortOrder = 0;
         }

         this.children().sort(this.itemStatSorter);
      }

      @OnlyIn(Dist.CLIENT)
      class ItemRow extends ObjectSelectionList.Entry<StatsScreen.ItemStatisticsList.ItemRow> {
         private final Item item;

         ItemRow(Item pItem) {
            this.item = pItem;
         }

         public Item getItem() {
            return this.item;
         }

         public void render(PoseStack pPoseStack, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pIsMouseOver, float pPartialTick) {
            StatsScreen.this.blitSlot(pPoseStack, pLeft + 40, pTop, this.item);

            for(int i = 0; i < StatsScreen.this.itemStatsList.blockColumns.size(); ++i) {
               Stat<Block> stat;
               if (this.item instanceof BlockItem) {
                  stat = StatsScreen.this.itemStatsList.blockColumns.get(i).get(((BlockItem)this.item).getBlock());
               } else {
                  stat = null;
               }

               this.renderStat(pPoseStack, stat, pLeft + StatsScreen.this.getColumnX(i), pTop, pIndex % 2 == 0);
            }

            for(int j = 0; j < StatsScreen.this.itemStatsList.itemColumns.size(); ++j) {
               this.renderStat(pPoseStack, StatsScreen.this.itemStatsList.itemColumns.get(j).get(this.item), pLeft + StatsScreen.this.getColumnX(j + StatsScreen.this.itemStatsList.blockColumns.size()), pTop, pIndex % 2 == 0);
            }

         }

         protected void renderStat(PoseStack pPoseStack, @Nullable Stat<?> pStat, int pX, int pY, boolean p_97096_) {
            String s = pStat == null ? "-" : pStat.format(StatsScreen.this.stats.getValue(pStat));
            GuiComponent.drawString(pPoseStack, StatsScreen.this.font, s, pX - StatsScreen.this.font.width(s), pY + 5, p_97096_ ? 16777215 : 9474192);
         }

         public Component getNarration() {
            return Component.translatable("narrator.select", this.item.getDescription());
         }
      }

      @OnlyIn(Dist.CLIENT)
      class ItemRowComparator implements Comparator<StatsScreen.ItemStatisticsList.ItemRow> {
         public int compare(StatsScreen.ItemStatisticsList.ItemRow pRow1, StatsScreen.ItemStatisticsList.ItemRow pRow2) {
            Item item = pRow1.getItem();
            Item item1 = pRow2.getItem();
            int i;
            int j;
            if (ItemStatisticsList.this.sortColumn == null) {
               i = 0;
               j = 0;
            } else if (ItemStatisticsList.this.blockColumns.contains(ItemStatisticsList.this.sortColumn)) {
               StatType<Block> stattype = (StatType<Block>) ItemStatisticsList.this.sortColumn;
               i = item instanceof BlockItem ? StatsScreen.this.stats.getValue(stattype, ((BlockItem)item).getBlock()) : -1;
               j = item1 instanceof BlockItem ? StatsScreen.this.stats.getValue(stattype, ((BlockItem)item1).getBlock()) : -1;
            } else {
               StatType<Item> stattype1 = (StatType<Item>) ItemStatisticsList.this.sortColumn;
               i = StatsScreen.this.stats.getValue(stattype1, item);
               j = StatsScreen.this.stats.getValue(stattype1, item1);
            }

            return i == j ? ItemStatisticsList.this.sortOrder * Integer.compare(Item.getId(item), Item.getId(item1)) : ItemStatisticsList.this.sortOrder * Integer.compare(i, j);
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   class MobsStatisticsList extends ObjectSelectionList<StatsScreen.MobsStatisticsList.MobRow> {
      public MobsStatisticsList(Minecraft pMinecraft) {
         super(pMinecraft, StatsScreen.this.width, StatsScreen.this.height, 32, StatsScreen.this.height - 64, 9 * 4);

         for(EntityType<?> entitytype : Registry.ENTITY_TYPE) {
            if (StatsScreen.this.stats.getValue(Stats.ENTITY_KILLED.get(entitytype)) > 0 || StatsScreen.this.stats.getValue(Stats.ENTITY_KILLED_BY.get(entitytype)) > 0) {
               this.addEntry(new StatsScreen.MobsStatisticsList.MobRow(entitytype));
            }
         }

      }

      protected void renderBackground(PoseStack pPoseStack) {
         StatsScreen.this.renderBackground(pPoseStack);
      }

      @OnlyIn(Dist.CLIENT)
      class MobRow extends ObjectSelectionList.Entry<StatsScreen.MobsStatisticsList.MobRow> {
         private final Component mobName;
         private final Component kills;
         private final boolean hasKills;
         private final Component killedBy;
         private final boolean wasKilledBy;

         public MobRow(EntityType<?> pEntityType) {
            this.mobName = pEntityType.getDescription();
            int i = StatsScreen.this.stats.getValue(Stats.ENTITY_KILLED.get(pEntityType));
            if (i == 0) {
               this.kills = Component.translatable("stat_type.minecraft.killed.none", this.mobName);
               this.hasKills = false;
            } else {
               this.kills = Component.translatable("stat_type.minecraft.killed", i, this.mobName);
               this.hasKills = true;
            }

            int j = StatsScreen.this.stats.getValue(Stats.ENTITY_KILLED_BY.get(pEntityType));
            if (j == 0) {
               this.killedBy = Component.translatable("stat_type.minecraft.killed_by.none", this.mobName);
               this.wasKilledBy = false;
            } else {
               this.killedBy = Component.translatable("stat_type.minecraft.killed_by", this.mobName, j);
               this.wasKilledBy = true;
            }

         }

         public void render(PoseStack pPoseStack, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pIsMouseOver, float pPartialTick) {
            GuiComponent.drawString(pPoseStack, StatsScreen.this.font, this.mobName, pLeft + 2, pTop + 1, 16777215);
            GuiComponent.drawString(pPoseStack, StatsScreen.this.font, this.kills, pLeft + 2 + 10, pTop + 1 + 9, this.hasKills ? 9474192 : 6316128);
            GuiComponent.drawString(pPoseStack, StatsScreen.this.font, this.killedBy, pLeft + 2 + 10, pTop + 1 + 9 * 2, this.wasKilledBy ? 9474192 : 6316128);
         }

         public Component getNarration() {
            return Component.translatable("narrator.select", CommonComponents.joinForNarration(this.kills, this.killedBy));
         }
      }
   }
}