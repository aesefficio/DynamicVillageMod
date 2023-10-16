package net.minecraft.world.item;

import javax.annotation.Nullable;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.level.block.Blocks;

public abstract class CreativeModeTab {
   public static CreativeModeTab[] TABS = new CreativeModeTab[12];
   public static final CreativeModeTab TAB_BUILDING_BLOCKS = (new CreativeModeTab(0, "buildingBlocks") {
      public ItemStack makeIcon() {
         return new ItemStack(Blocks.BRICKS);
      }
   }).setRecipeFolderName("building_blocks");
   public static final CreativeModeTab TAB_DECORATIONS = new CreativeModeTab(1, "decorations") {
      public ItemStack makeIcon() {
         return new ItemStack(Blocks.PEONY);
      }
   };
   public static final CreativeModeTab TAB_REDSTONE = new CreativeModeTab(2, "redstone") {
      public ItemStack makeIcon() {
         return new ItemStack(Items.REDSTONE);
      }
   };
   public static final CreativeModeTab TAB_TRANSPORTATION = new CreativeModeTab(3, "transportation") {
      public ItemStack makeIcon() {
         return new ItemStack(Blocks.POWERED_RAIL);
      }
   };
   public static final CreativeModeTab TAB_MISC = new CreativeModeTab(6, "misc") {
      public ItemStack makeIcon() {
         return new ItemStack(Items.LAVA_BUCKET);
      }
   };
   public static final CreativeModeTab TAB_SEARCH = (new CreativeModeTab(5, "search") {
      public ItemStack makeIcon() {
         return new ItemStack(Items.COMPASS);
      }
   }).setBackgroundSuffix("item_search.png");
   public static final CreativeModeTab TAB_FOOD = new CreativeModeTab(7, "food") {
      public ItemStack makeIcon() {
         return new ItemStack(Items.APPLE);
      }
   };
   public static final CreativeModeTab TAB_TOOLS = (new CreativeModeTab(8, "tools") {
      public ItemStack makeIcon() {
         return new ItemStack(Items.IRON_AXE);
      }
   }).setEnchantmentCategories(new EnchantmentCategory[]{EnchantmentCategory.VANISHABLE, EnchantmentCategory.DIGGER, EnchantmentCategory.FISHING_ROD, EnchantmentCategory.BREAKABLE});
   public static final CreativeModeTab TAB_COMBAT = (new CreativeModeTab(9, "combat") {
      public ItemStack makeIcon() {
         return new ItemStack(Items.GOLDEN_SWORD);
      }
   }).setEnchantmentCategories(new EnchantmentCategory[]{EnchantmentCategory.VANISHABLE, EnchantmentCategory.ARMOR, EnchantmentCategory.ARMOR_FEET, EnchantmentCategory.ARMOR_HEAD, EnchantmentCategory.ARMOR_LEGS, EnchantmentCategory.ARMOR_CHEST, EnchantmentCategory.BOW, EnchantmentCategory.WEAPON, EnchantmentCategory.WEARABLE, EnchantmentCategory.BREAKABLE, EnchantmentCategory.TRIDENT, EnchantmentCategory.CROSSBOW});
   public static final CreativeModeTab TAB_BREWING = new CreativeModeTab(10, "brewing") {
      public ItemStack makeIcon() {
         return PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER);
      }
   };
   public static final CreativeModeTab TAB_MATERIALS = TAB_MISC;
   public static final CreativeModeTab TAB_HOTBAR = new CreativeModeTab(4, "hotbar") {
      public ItemStack makeIcon() {
         return new ItemStack(Blocks.BOOKSHELF);
      }

      /**
       * Fills {@code items} with all items that are in this group.
       */
      public void fillItemList(NonNullList<ItemStack> p_40820_) {
         throw new RuntimeException("Implement exception client-side.");
      }

      public boolean isAlignedRight() {
         return true;
      }
   };
   public static final CreativeModeTab TAB_INVENTORY = (new CreativeModeTab(11, "inventory") {
      public ItemStack makeIcon() {
         return new ItemStack(Blocks.CHEST);
      }
   }).setBackgroundSuffix("inventory.png").hideScroll().hideTitle();
   private final int id;
   private final String langId;
   private final Component displayName;
   private String recipeFolderName;
   @Deprecated
   private String backgroundSuffix = "items.png";
   private net.minecraft.resources.ResourceLocation backgroundLocation;
   private boolean canScroll = true;
   private boolean showTitle = true;
   private EnchantmentCategory[] enchantmentCategories = new EnchantmentCategory[0];
   private ItemStack iconItemStack;

   public CreativeModeTab(String label) {
       this(-1, label);
   }

   public CreativeModeTab(int pId, String pLangId) {
      this.langId = pLangId;
      this.displayName = Component.translatable("itemGroup." + pLangId);
      this.iconItemStack = ItemStack.EMPTY;
      this.id = addGroupSafe(pId, this);
   }

   public int getId() {
      return this.id;
   }

   /**
    * Gets the name that's valid for use in a ResourceLocation's path. This should be set if the tabLabel contains
    * illegal characters.
    */
   public String getRecipeFolderName() {
      return this.recipeFolderName == null ? this.langId : this.recipeFolderName;
   }

   public Component getDisplayName() {
      return this.displayName;
   }

   public ItemStack getIconItem() {
      if (this.iconItemStack.isEmpty()) {
         this.iconItemStack = this.makeIcon();
      }

      return this.iconItemStack;
   }

   public abstract ItemStack makeIcon();

   /**
    * @deprecated Forge use {@link #getBackgroundImage()} instead
    */
   @Deprecated
   public String getBackgroundSuffix() {
      return this.backgroundSuffix;
   }

   /**
    * @deprecated Forge: use {@link #setBackgroundImage(net.minecraft.resources.ResourceLocation)} instead
    */
   @Deprecated
   public CreativeModeTab setBackgroundSuffix(String pBackgroundSuffix) {
      this.backgroundSuffix = pBackgroundSuffix;
      return this;
   }

   public CreativeModeTab setBackgroundImage(net.minecraft.resources.ResourceLocation texture) {
      this.backgroundLocation = texture;
      return this;
   }

   public CreativeModeTab setRecipeFolderName(String pRecipeFolderName) {
      this.recipeFolderName = pRecipeFolderName;
      return this;
   }

   public boolean showTitle() {
      return this.showTitle;
   }

   public CreativeModeTab hideTitle() {
      this.showTitle = false;
      return this;
   }

   public boolean canScroll() {
      return this.canScroll;
   }

   public CreativeModeTab hideScroll() {
      this.canScroll = false;
      return this;
   }

   /**
    * returns index % 6
    */
   public int getColumn() {
      if (id > 11) return ((id - 12) % 10) % 5;
      return this.id % 6;
   }

   /**
    * returns tabIndex < 6
    */
   public boolean isTopRow() {
      if (id > 11) return ((id - 12) % 10) < 5;
      return this.id < 6;
   }

   public boolean isAlignedRight() {
      return this.getColumn() == 5;
   }

   /**
    * Returns the enchantment types relevant to this tab
    */
   public EnchantmentCategory[] getEnchantmentCategories() {
      return this.enchantmentCategories;
   }

   /**
    * Sets the enchantment types for populating this tab with enchanting books
    */
   public CreativeModeTab setEnchantmentCategories(EnchantmentCategory... pEnchantmentCategories) {
      this.enchantmentCategories = pEnchantmentCategories;
      return this;
   }

   public boolean hasEnchantmentCategory(@Nullable EnchantmentCategory pCategory) {
      if (pCategory != null) {
         for(EnchantmentCategory enchantmentcategory : this.enchantmentCategories) {
            if (enchantmentcategory == pCategory) {
               return true;
            }
         }
      }

      return false;
   }

   /**
    * Fills {@code items} with all items that are in this group.
    */
   public void fillItemList(NonNullList<ItemStack> pItems) {
      for(Item item : Registry.ITEM) {
         item.fillItemCategory(this, pItems);
      }

   }

   public int getTabPage() {
      return id < 12 ? 0 : ((id - 12) / 10) + 1;
   }

   public boolean hasSearchBar() {
      return id == TAB_SEARCH.id;
   }

   /**
    * Gets the width of the search bar of the creative tab, use this if your
    * creative tab name overflows together with a custom texture.
    *
    * @return The width of the search bar, 89 by default
    */
   public int getSearchbarWidth() {
      return 89;
   }

   public net.minecraft.resources.ResourceLocation getBackgroundImage() {
      if (backgroundLocation != null) return backgroundLocation; //FORGE: allow custom namespace
      return new net.minecraft.resources.ResourceLocation("textures/gui/container/creative_inventory/tab_" + this.getBackgroundSuffix());
   }

   private static final net.minecraft.resources.ResourceLocation CREATIVE_INVENTORY_TABS = new net.minecraft.resources.ResourceLocation("textures/gui/container/creative_inventory/tabs.png");
   public net.minecraft.resources.ResourceLocation getTabsImage() {
      return CREATIVE_INVENTORY_TABS;
   }

   public int getLabelColor() {
      return 4210752;
   }

   public int getSlotColor() {
      return -2130706433;
   }

   public static synchronized int getGroupCountSafe() {
      return CreativeModeTab.TABS.length;
   }

   private static synchronized int addGroupSafe(int index, CreativeModeTab newGroup) {
      if(index == -1) {
         index = TABS.length;
      }
      if (index >= TABS.length) {
         CreativeModeTab[] tmp = new CreativeModeTab[index + 1];
         System.arraycopy(TABS, 0, tmp, 0, TABS.length);
         TABS = tmp;
      }
      TABS[index] = newGroup;
      return index;
   }
}
