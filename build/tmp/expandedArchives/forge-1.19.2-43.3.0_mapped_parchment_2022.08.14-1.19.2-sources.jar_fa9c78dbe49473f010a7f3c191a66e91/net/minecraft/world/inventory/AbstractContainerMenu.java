package net.minecraft.world.inventory;

import com.google.common.base.Suppliers;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;

public abstract class AbstractContainerMenu {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final int SLOT_CLICKED_OUTSIDE = -999;
   public static final int QUICKCRAFT_TYPE_CHARITABLE = 0;
   public static final int QUICKCRAFT_TYPE_GREEDY = 1;
   public static final int QUICKCRAFT_TYPE_CLONE = 2;
   public static final int QUICKCRAFT_HEADER_START = 0;
   public static final int QUICKCRAFT_HEADER_CONTINUE = 1;
   public static final int QUICKCRAFT_HEADER_END = 2;
   public static final int CARRIED_SLOT_SIZE = Integer.MAX_VALUE;
   private final NonNullList<ItemStack> lastSlots = NonNullList.create();
   public final NonNullList<Slot> slots = NonNullList.create();
   private final List<DataSlot> dataSlots = Lists.newArrayList();
   private ItemStack carried = ItemStack.EMPTY;
   private final NonNullList<ItemStack> remoteSlots = NonNullList.create();
   private final IntList remoteDataSlots = new IntArrayList();
   private ItemStack remoteCarried = ItemStack.EMPTY;
   private int stateId;
   @Nullable
   private final MenuType<?> menuType;
   public final int containerId;
   private int quickcraftType = -1;
   private int quickcraftStatus;
   private final Set<Slot> quickcraftSlots = Sets.newHashSet();
   private final List<ContainerListener> containerListeners = Lists.newArrayList();
   @Nullable
   private ContainerSynchronizer synchronizer;
   private boolean suppressRemoteUpdates;

   protected AbstractContainerMenu(@Nullable MenuType<?> pMenuType, int pContainerId) {
      this.menuType = pMenuType;
      this.containerId = pContainerId;
   }

   protected static boolean stillValid(ContainerLevelAccess pAccess, Player pPlayer, Block pTargetBlock) {
      return pAccess.evaluate((p_38916_, p_38917_) -> {
         return !p_38916_.getBlockState(p_38917_).is(pTargetBlock) ? false : pPlayer.distanceToSqr((double)p_38917_.getX() + 0.5D, (double)p_38917_.getY() + 0.5D, (double)p_38917_.getZ() + 0.5D) <= 64.0D;
      }, true);
   }

   public MenuType<?> getType() {
      if (this.menuType == null) {
         throw new UnsupportedOperationException("Unable to construct this menu by type");
      } else {
         return this.menuType;
      }
   }

   protected static void checkContainerSize(Container pContainer, int pMinSize) {
      int i = pContainer.getContainerSize();
      if (i < pMinSize) {
         throw new IllegalArgumentException("Container size " + i + " is smaller than expected " + pMinSize);
      }
   }

   protected static void checkContainerDataCount(ContainerData pIntArray, int pMinSize) {
      int i = pIntArray.getCount();
      if (i < pMinSize) {
         throw new IllegalArgumentException("Container data count " + i + " is smaller than expected " + pMinSize);
      }
   }

   public boolean isValidSlotIndex(int pSlotIndex) {
      return pSlotIndex == -1 || pSlotIndex == -999 || pSlotIndex < this.slots.size();
   }

   /**
    * Adds an item slot to this container
    */
   protected Slot addSlot(Slot pSlot) {
      pSlot.index = this.slots.size();
      this.slots.add(pSlot);
      this.lastSlots.add(ItemStack.EMPTY);
      this.remoteSlots.add(ItemStack.EMPTY);
      return pSlot;
   }

   protected DataSlot addDataSlot(DataSlot pIntValue) {
      this.dataSlots.add(pIntValue);
      this.remoteDataSlots.add(0);
      return pIntValue;
   }

   protected void addDataSlots(ContainerData pArray) {
      for(int i = 0; i < pArray.getCount(); ++i) {
         this.addDataSlot(DataSlot.forContainer(pArray, i));
      }

   }

   public void addSlotListener(ContainerListener pListener) {
      if (!this.containerListeners.contains(pListener)) {
         this.containerListeners.add(pListener);
         this.broadcastChanges();
      }
   }

   public void setSynchronizer(ContainerSynchronizer pSyncronizer) {
      this.synchronizer = pSyncronizer;
      this.sendAllDataToRemote();
   }

   public void sendAllDataToRemote() {
      int i = 0;

      for(int j = this.slots.size(); i < j; ++i) {
         this.remoteSlots.set(i, this.slots.get(i).getItem().copy());
      }

      this.remoteCarried = this.getCarried().copy();
      i = 0;

      for(int k = this.dataSlots.size(); i < k; ++i) {
         this.remoteDataSlots.set(i, this.dataSlots.get(i).get());
      }

      if (this.synchronizer != null) {
         this.synchronizer.sendInitialData(this, this.remoteSlots, this.remoteCarried, this.remoteDataSlots.toIntArray());
      }

   }

   /**
    * Remove the given Listener. Method name is for legacy.
    */
   public void removeSlotListener(ContainerListener pListener) {
      this.containerListeners.remove(pListener);
   }

   /**
    * returns a list if itemStacks, for each slot.
    */
   public NonNullList<ItemStack> getItems() {
      NonNullList<ItemStack> nonnulllist = NonNullList.create();

      for(Slot slot : this.slots) {
         nonnulllist.add(slot.getItem());
      }

      return nonnulllist;
   }

   /**
    * Looks for changes made in the container, sends them to every listener.
    */
   public void broadcastChanges() {
      for(int i = 0; i < this.slots.size(); ++i) {
         ItemStack itemstack = this.slots.get(i).getItem();
         Supplier<ItemStack> supplier = Suppliers.memoize(itemstack::copy);
         this.triggerSlotListeners(i, itemstack, supplier);
         this.synchronizeSlotToRemote(i, itemstack, supplier);
      }

      this.synchronizeCarriedToRemote();

      for(int j = 0; j < this.dataSlots.size(); ++j) {
         DataSlot dataslot = this.dataSlots.get(j);
         int k = dataslot.get();
         if (dataslot.checkAndClearUpdateFlag()) {
            this.updateDataSlotListeners(j, k);
         }

         this.synchronizeDataSlotToRemote(j, k);
      }

   }

   public void broadcastFullState() {
      for(int i = 0; i < this.slots.size(); ++i) {
         ItemStack itemstack = this.slots.get(i).getItem();
         this.triggerSlotListeners(i, itemstack, itemstack::copy);
      }

      for(int j = 0; j < this.dataSlots.size(); ++j) {
         DataSlot dataslot = this.dataSlots.get(j);
         if (dataslot.checkAndClearUpdateFlag()) {
            this.updateDataSlotListeners(j, dataslot.get());
         }
      }

      this.sendAllDataToRemote();
   }

   private void updateDataSlotListeners(int pSlotIndex, int pValue) {
      for(ContainerListener containerlistener : this.containerListeners) {
         containerlistener.dataChanged(this, pSlotIndex, pValue);
      }

   }

   private void triggerSlotListeners(int pSlotIndex, ItemStack pStack, Supplier<ItemStack> pSupplier) {
      ItemStack itemstack = this.lastSlots.get(pSlotIndex);
      if (!ItemStack.matches(itemstack, pStack)) {
         boolean clientStackChanged = !pStack.equals(itemstack, true);
         ItemStack itemstack1 = pSupplier.get();
         this.lastSlots.set(pSlotIndex, itemstack1);

         if(clientStackChanged)
         for(ContainerListener containerlistener : this.containerListeners) {
            containerlistener.slotChanged(this, pSlotIndex, itemstack1);
         }
      }

   }

   private void synchronizeSlotToRemote(int pSlotIndex, ItemStack pStack, Supplier<ItemStack> pSupplier) {
      if (!this.suppressRemoteUpdates) {
         ItemStack itemstack = this.remoteSlots.get(pSlotIndex);
         if (!ItemStack.matches(itemstack, pStack)) {
            ItemStack itemstack1 = pSupplier.get();
            this.remoteSlots.set(pSlotIndex, itemstack1);
            if (this.synchronizer != null) {
               this.synchronizer.sendSlotChange(this, pSlotIndex, itemstack1);
            }
         }

      }
   }

   private void synchronizeDataSlotToRemote(int pSlotIndex, int pValue) {
      if (!this.suppressRemoteUpdates) {
         int i = this.remoteDataSlots.getInt(pSlotIndex);
         if (i != pValue) {
            this.remoteDataSlots.set(pSlotIndex, pValue);
            if (this.synchronizer != null) {
               this.synchronizer.sendDataChange(this, pSlotIndex, pValue);
            }
         }

      }
   }

   private void synchronizeCarriedToRemote() {
      if (!this.suppressRemoteUpdates) {
         if (!ItemStack.matches(this.getCarried(), this.remoteCarried)) {
            this.remoteCarried = this.getCarried().copy();
            if (this.synchronizer != null) {
               this.synchronizer.sendCarriedChange(this, this.remoteCarried);
            }
         }

      }
   }

   public void setRemoteSlot(int pSlot, ItemStack pStack) {
      this.remoteSlots.set(pSlot, pStack.copy());
   }

   public void setRemoteSlotNoCopy(int pSlot, ItemStack pStack) {
      if (pSlot >= 0 && pSlot < this.remoteSlots.size()) {
         this.remoteSlots.set(pSlot, pStack);
      } else {
         LOGGER.debug("Incorrect slot index: {} available slots: {}", pSlot, this.remoteSlots.size());
      }
   }

   public void setRemoteCarried(ItemStack pRemoteCarried) {
      this.remoteCarried = pRemoteCarried.copy();
   }

   /**
    * Handles the given Button-click on the server, currently only used by enchanting. Name is for legacy.
    */
   public boolean clickMenuButton(Player pPlayer, int pId) {
      return false;
   }

   public Slot getSlot(int pSlotId) {
      return this.slots.get(pSlotId);
   }

   /**
    * Handle when the stack in slot {@code index} is shift-clicked. Normally this moves the stack between the player
    * inventory and the other inventory(s).
    */
   public abstract ItemStack quickMoveStack(Player pPlayer, int pIndex);

   public void clicked(int pMouseX, int pMouseY, ClickType pClickType, Player pPlayer) {
      try {
         this.doClick(pMouseX, pMouseY, pClickType, pPlayer);
      } catch (Exception exception) {
         CrashReport crashreport = CrashReport.forThrowable(exception, "Container click");
         CrashReportCategory crashreportcategory = crashreport.addCategory("Click info");
         crashreportcategory.setDetail("Menu Type", () -> {
            return this.menuType != null ? Registry.MENU.getKey(this.menuType).toString() : "<no type>";
         });
         crashreportcategory.setDetail("Menu Class", () -> {
            return this.getClass().getCanonicalName();
         });
         crashreportcategory.setDetail("Slot Count", this.slots.size());
         crashreportcategory.setDetail("Slot", pMouseX);
         crashreportcategory.setDetail("Button", pMouseY);
         crashreportcategory.setDetail("Type", pClickType);
         throw new ReportedException(crashreport);
      }
   }

   private void doClick(int pMouseX, int pMouseY, ClickType pClickType, Player pPlayer) {
      Inventory inventory = pPlayer.getInventory();
      if (pClickType == ClickType.QUICK_CRAFT) {
         int i = this.quickcraftStatus;
         this.quickcraftStatus = getQuickcraftHeader(pMouseY);
         if ((i != 1 || this.quickcraftStatus != 2) && i != this.quickcraftStatus) {
            this.resetQuickCraft();
         } else if (this.getCarried().isEmpty()) {
            this.resetQuickCraft();
         } else if (this.quickcraftStatus == 0) {
            this.quickcraftType = getQuickcraftType(pMouseY);
            if (isValidQuickcraftType(this.quickcraftType, pPlayer)) {
               this.quickcraftStatus = 1;
               this.quickcraftSlots.clear();
            } else {
               this.resetQuickCraft();
            }
         } else if (this.quickcraftStatus == 1) {
            Slot slot = this.slots.get(pMouseX);
            ItemStack itemstack = this.getCarried();
            if (canItemQuickReplace(slot, itemstack, true) && slot.mayPlace(itemstack) && (this.quickcraftType == 2 || itemstack.getCount() > this.quickcraftSlots.size()) && this.canDragTo(slot)) {
               this.quickcraftSlots.add(slot);
            }
         } else if (this.quickcraftStatus == 2) {
            if (!this.quickcraftSlots.isEmpty()) {
               if (this.quickcraftSlots.size() == 1) {
                  int l = (this.quickcraftSlots.iterator().next()).index;
                  this.resetQuickCraft();
                  this.doClick(l, this.quickcraftType, ClickType.PICKUP, pPlayer);
                  return;
               }

               ItemStack itemstack3 = this.getCarried().copy();
               int j1 = this.getCarried().getCount();

               for(Slot slot1 : this.quickcraftSlots) {
                  ItemStack itemstack1 = this.getCarried();
                  if (slot1 != null && canItemQuickReplace(slot1, itemstack1, true) && slot1.mayPlace(itemstack1) && (this.quickcraftType == 2 || itemstack1.getCount() >= this.quickcraftSlots.size()) && this.canDragTo(slot1)) {
                     ItemStack itemstack2 = itemstack3.copy();
                     int j = slot1.hasItem() ? slot1.getItem().getCount() : 0;
                     getQuickCraftSlotCount(this.quickcraftSlots, this.quickcraftType, itemstack2, j);
                     int k = Math.min(itemstack2.getMaxStackSize(), slot1.getMaxStackSize(itemstack2));
                     if (itemstack2.getCount() > k) {
                        itemstack2.setCount(k);
                     }

                     j1 -= itemstack2.getCount() - j;
                     slot1.set(itemstack2);
                  }
               }

               itemstack3.setCount(j1);
               this.setCarried(itemstack3);
            }

            this.resetQuickCraft();
         } else {
            this.resetQuickCraft();
         }
      } else if (this.quickcraftStatus != 0) {
         this.resetQuickCraft();
      } else if ((pClickType == ClickType.PICKUP || pClickType == ClickType.QUICK_MOVE) && (pMouseY == 0 || pMouseY == 1)) {
         ClickAction clickaction = pMouseY == 0 ? ClickAction.PRIMARY : ClickAction.SECONDARY;
         if (pMouseX == -999) {
            if (!this.getCarried().isEmpty()) {
               if (clickaction == ClickAction.PRIMARY) {
                  pPlayer.drop(this.getCarried(), true);
                  this.setCarried(ItemStack.EMPTY);
               } else {
                  pPlayer.drop(this.getCarried().split(1), true);
               }
            }
         } else if (pClickType == ClickType.QUICK_MOVE) {
            if (pMouseX < 0) {
               return;
            }

            Slot slot6 = this.slots.get(pMouseX);
            if (!slot6.mayPickup(pPlayer)) {
               return;
            }

            for(ItemStack itemstack9 = this.quickMoveStack(pPlayer, pMouseX); !itemstack9.isEmpty() && ItemStack.isSame(slot6.getItem(), itemstack9); itemstack9 = this.quickMoveStack(pPlayer, pMouseX)) {
            }
         } else {
            if (pMouseX < 0) {
               return;
            }

            Slot slot7 = this.slots.get(pMouseX);
            ItemStack itemstack10 = slot7.getItem();
            ItemStack itemstack11 = this.getCarried();
            pPlayer.updateTutorialInventoryAction(itemstack11, slot7.getItem(), clickaction);
            if (!itemstack11.overrideStackedOnOther(slot7, clickaction, pPlayer) && !itemstack10.overrideOtherStackedOnMe(itemstack11, slot7, clickaction, pPlayer, this.createCarriedSlotAccess())) {
            if (!net.minecraftforge.common.ForgeHooks.onItemStackedOn(itemstack11, itemstack10, slot7, clickaction, pPlayer, createCarriedSlotAccess()))
               if (itemstack10.isEmpty()) {
                  if (!itemstack11.isEmpty()) {
                     int l2 = clickaction == ClickAction.PRIMARY ? itemstack11.getCount() : 1;
                     this.setCarried(slot7.safeInsert(itemstack11, l2));
                  }
               } else if (slot7.mayPickup(pPlayer)) {
                  if (itemstack11.isEmpty()) {
                     int i3 = clickaction == ClickAction.PRIMARY ? itemstack10.getCount() : (itemstack10.getCount() + 1) / 2;
                     Optional<ItemStack> optional1 = slot7.tryRemove(i3, Integer.MAX_VALUE, pPlayer);
                     optional1.ifPresent((p_150421_) -> {
                        this.setCarried(p_150421_);
                        slot7.onTake(pPlayer, p_150421_);
                     });
                  } else if (slot7.mayPlace(itemstack11)) {
                     if (ItemStack.isSameItemSameTags(itemstack10, itemstack11)) {
                        int j3 = clickaction == ClickAction.PRIMARY ? itemstack11.getCount() : 1;
                        this.setCarried(slot7.safeInsert(itemstack11, j3));
                     } else if (itemstack11.getCount() <= slot7.getMaxStackSize(itemstack11)) {
                        this.setCarried(itemstack10);
                        slot7.set(itemstack11);
                     }
                  } else if (ItemStack.isSameItemSameTags(itemstack10, itemstack11)) {
                     Optional<ItemStack> optional = slot7.tryRemove(itemstack10.getCount(), itemstack11.getMaxStackSize() - itemstack11.getCount(), pPlayer);
                     optional.ifPresent((p_150428_) -> {
                        itemstack11.grow(p_150428_.getCount());
                        slot7.onTake(pPlayer, p_150428_);
                     });
                  }
               }
            }

            slot7.setChanged();
         }
      } else if (pClickType == ClickType.SWAP) {
         Slot slot2 = this.slots.get(pMouseX);
         ItemStack itemstack4 = inventory.getItem(pMouseY);
         ItemStack itemstack7 = slot2.getItem();
         if (!itemstack4.isEmpty() || !itemstack7.isEmpty()) {
            if (itemstack4.isEmpty()) {
               if (slot2.mayPickup(pPlayer)) {
                  inventory.setItem(pMouseY, itemstack7);
                  slot2.onSwapCraft(itemstack7.getCount());
                  slot2.set(ItemStack.EMPTY);
                  slot2.onTake(pPlayer, itemstack7);
               }
            } else if (itemstack7.isEmpty()) {
               if (slot2.mayPlace(itemstack4)) {
                  int l1 = slot2.getMaxStackSize(itemstack4);
                  if (itemstack4.getCount() > l1) {
                     slot2.set(itemstack4.split(l1));
                  } else {
                     inventory.setItem(pMouseY, ItemStack.EMPTY);
                     slot2.set(itemstack4);
                  }
               }
            } else if (slot2.mayPickup(pPlayer) && slot2.mayPlace(itemstack4)) {
               int i2 = slot2.getMaxStackSize(itemstack4);
               if (itemstack4.getCount() > i2) {
                  slot2.set(itemstack4.split(i2));
                  slot2.onTake(pPlayer, itemstack7);
                  if (!inventory.add(itemstack7)) {
                     pPlayer.drop(itemstack7, true);
                  }
               } else {
                  inventory.setItem(pMouseY, itemstack7);
                  slot2.set(itemstack4);
                  slot2.onTake(pPlayer, itemstack7);
               }
            }
         }
      } else if (pClickType == ClickType.CLONE && pPlayer.getAbilities().instabuild && this.getCarried().isEmpty() && pMouseX >= 0) {
         Slot slot5 = this.slots.get(pMouseX);
         if (slot5.hasItem()) {
            ItemStack itemstack6 = slot5.getItem().copy();
            itemstack6.setCount(itemstack6.getMaxStackSize());
            this.setCarried(itemstack6);
         }
      } else if (pClickType == ClickType.THROW && this.getCarried().isEmpty() && pMouseX >= 0) {
         Slot slot4 = this.slots.get(pMouseX);
         int i1 = pMouseY == 0 ? 1 : slot4.getItem().getCount();
         ItemStack itemstack8 = slot4.safeTake(i1, Integer.MAX_VALUE, pPlayer);
         pPlayer.drop(itemstack8, true);
      } else if (pClickType == ClickType.PICKUP_ALL && pMouseX >= 0) {
         Slot slot3 = this.slots.get(pMouseX);
         ItemStack itemstack5 = this.getCarried();
         if (!itemstack5.isEmpty() && (!slot3.hasItem() || !slot3.mayPickup(pPlayer))) {
            int k1 = pMouseY == 0 ? 0 : this.slots.size() - 1;
            int j2 = pMouseY == 0 ? 1 : -1;

            for(int k2 = 0; k2 < 2; ++k2) {
               for(int k3 = k1; k3 >= 0 && k3 < this.slots.size() && itemstack5.getCount() < itemstack5.getMaxStackSize(); k3 += j2) {
                  Slot slot8 = this.slots.get(k3);
                  if (slot8.hasItem() && canItemQuickReplace(slot8, itemstack5, true) && slot8.mayPickup(pPlayer) && this.canTakeItemForPickAll(itemstack5, slot8)) {
                     ItemStack itemstack12 = slot8.getItem();
                     if (k2 != 0 || itemstack12.getCount() != itemstack12.getMaxStackSize()) {
                        ItemStack itemstack13 = slot8.safeTake(itemstack12.getCount(), itemstack5.getMaxStackSize() - itemstack5.getCount(), pPlayer);
                        itemstack5.grow(itemstack13.getCount());
                     }
                  }
               }
            }
         }
      }

   }

   private SlotAccess createCarriedSlotAccess() {
      return new SlotAccess() {
         public ItemStack get() {
            return AbstractContainerMenu.this.getCarried();
         }

         public boolean set(ItemStack p_150452_) {
            AbstractContainerMenu.this.setCarried(p_150452_);
            return true;
         }
      };
   }

   /**
    * Called to determine if the current slot is valid for the stack merging (double-click) code. The stack passed in is
    * null for the initial slot that was double-clicked.
    */
   public boolean canTakeItemForPickAll(ItemStack pStack, Slot pSlot) {
      return true;
   }

   /**
    * Called when the container is closed.
    */
   public void removed(Player pPlayer) {
      if (pPlayer instanceof ServerPlayer) {
         ItemStack itemstack = this.getCarried();
         if (!itemstack.isEmpty()) {
            if (pPlayer.isAlive() && !((ServerPlayer)pPlayer).hasDisconnected()) {
               pPlayer.getInventory().placeItemBackInInventory(itemstack);
            } else {
               pPlayer.drop(itemstack, false);
            }

            this.setCarried(ItemStack.EMPTY);
         }
      }

   }

   protected void clearContainer(Player pPlayer, Container pContainer) {
      if (!pPlayer.isAlive() || pPlayer instanceof ServerPlayer && ((ServerPlayer)pPlayer).hasDisconnected()) {
         for(int j = 0; j < pContainer.getContainerSize(); ++j) {
            pPlayer.drop(pContainer.removeItemNoUpdate(j), false);
         }

      } else {
         for(int i = 0; i < pContainer.getContainerSize(); ++i) {
            Inventory inventory = pPlayer.getInventory();
            if (inventory.player instanceof ServerPlayer) {
               inventory.placeItemBackInInventory(pContainer.removeItemNoUpdate(i));
            }
         }

      }
   }

   /**
    * Callback for when the crafting matrix is changed.
    */
   public void slotsChanged(Container pContainer) {
      this.broadcastChanges();
   }

   /**
    * Puts an ItemStack in a slot.
    */
   public void setItem(int pSlotId, int pStateId, ItemStack pStack) {
      this.getSlot(pSlotId).set(pStack);
      this.stateId = pStateId;
   }

   public void initializeContents(int pStateId, List<ItemStack> pItems, ItemStack pCarried) {
      for(int i = 0; i < pItems.size(); ++i) {
         this.getSlot(i).initialize(pItems.get(i));
      }

      this.carried = pCarried;
      this.stateId = pStateId;
   }

   public void setData(int pId, int pData) {
      this.dataSlots.get(pId).set(pData);
   }

   /**
    * Determines whether supplied player can use this container
    */
   public abstract boolean stillValid(Player pPlayer);

   /**
    * Merges provided ItemStack with the first avaliable one in the container/player inventor between minIndex
    * (included) and maxIndex (excluded). Args : stack, minIndex, maxIndex, negativDirection. [!] the Container
    * implementation do not check if the item is valid for the slot
    */
   protected boolean moveItemStackTo(ItemStack pStack, int pStartIndex, int pEndIndex, boolean pReverseDirection) {
      boolean flag = false;
      int i = pStartIndex;
      if (pReverseDirection) {
         i = pEndIndex - 1;
      }

      if (pStack.isStackable()) {
         while(!pStack.isEmpty()) {
            if (pReverseDirection) {
               if (i < pStartIndex) {
                  break;
               }
            } else if (i >= pEndIndex) {
               break;
            }

            Slot slot = this.slots.get(i);
            ItemStack itemstack = slot.getItem();
            if (!itemstack.isEmpty() && ItemStack.isSameItemSameTags(pStack, itemstack)) {
               int j = itemstack.getCount() + pStack.getCount();
               int maxSize = Math.min(slot.getMaxStackSize(), pStack.getMaxStackSize());
               if (j <= maxSize) {
                  pStack.setCount(0);
                  itemstack.setCount(j);
                  slot.setChanged();
                  flag = true;
               } else if (itemstack.getCount() < maxSize) {
                  pStack.shrink(maxSize - itemstack.getCount());
                  itemstack.setCount(maxSize);
                  slot.setChanged();
                  flag = true;
               }
            }

            if (pReverseDirection) {
               --i;
            } else {
               ++i;
            }
         }
      }

      if (!pStack.isEmpty()) {
         if (pReverseDirection) {
            i = pEndIndex - 1;
         } else {
            i = pStartIndex;
         }

         while(true) {
            if (pReverseDirection) {
               if (i < pStartIndex) {
                  break;
               }
            } else if (i >= pEndIndex) {
               break;
            }

            Slot slot1 = this.slots.get(i);
            ItemStack itemstack1 = slot1.getItem();
            if (itemstack1.isEmpty() && slot1.mayPlace(pStack)) {
               if (pStack.getCount() > slot1.getMaxStackSize()) {
                  slot1.set(pStack.split(slot1.getMaxStackSize()));
               } else {
                  slot1.set(pStack.split(pStack.getCount()));
               }

               slot1.setChanged();
               flag = true;
               break;
            }

            if (pReverseDirection) {
               --i;
            } else {
               ++i;
            }
         }
      }

      return flag;
   }

   /**
    * Extracts the drag mode. Args : eventButton. Return (0 : evenly split, 1 : one item by slot, 2 : not used ?)
    */
   public static int getQuickcraftType(int pEventButton) {
      return pEventButton >> 2 & 3;
   }

   /**
    * Args : clickedButton, Returns (0 : start drag, 1 : add slot, 2 : end drag)
    */
   public static int getQuickcraftHeader(int pClickedButton) {
      return pClickedButton & 3;
   }

   public static int getQuickcraftMask(int pQuickCraftingHeader, int pQuickCraftingType) {
      return pQuickCraftingHeader & 3 | (pQuickCraftingType & 3) << 2;
   }

   public static boolean isValidQuickcraftType(int pDragMode, Player pPlayer) {
      if (pDragMode == 0) {
         return true;
      } else if (pDragMode == 1) {
         return true;
      } else {
         return pDragMode == 2 && pPlayer.getAbilities().instabuild;
      }
   }

   /**
    * Reset the drag fields
    */
   protected void resetQuickCraft() {
      this.quickcraftStatus = 0;
      this.quickcraftSlots.clear();
   }

   /**
    * Checks if it's possible to add the given itemstack to the given slot.
    */
   public static boolean canItemQuickReplace(@Nullable Slot pSlot, ItemStack pStack, boolean pStackSizeMatters) {
      boolean flag = pSlot == null || !pSlot.hasItem();
      if (!flag && ItemStack.isSameItemSameTags(pStack, pSlot.getItem())) {
         return pSlot.getItem().getCount() + (pStackSizeMatters ? 0 : pStack.getCount()) <= pStack.getMaxStackSize();
      } else {
         return flag;
      }
   }

   /**
    * Compute the new stack size, Returns the stack with the new size. Args : dragSlots, dragMode, dragStack,
    * slotStackSize
    */
   public static void getQuickCraftSlotCount(Set<Slot> pDragSlots, int pDragMode, ItemStack pStack, int pSlotStackSize) {
      switch (pDragMode) {
         case 0:
            pStack.setCount(Mth.floor((float)pStack.getCount() / (float)pDragSlots.size()));
            break;
         case 1:
            pStack.setCount(1);
            break;
         case 2:
         pStack.setCount(pStack.getMaxStackSize());
      }

      pStack.grow(pSlotStackSize);
   }

   /**
    * Returns true if the player can "drag-spilt" items into this slot,. returns true by default. Called to check if the
    * slot can be added to a list of Slots to split the held ItemStack across.
    */
   public boolean canDragTo(Slot pSlot) {
      return true;
   }

   /**
    * Like the version that takes an inventory. If the given TileEntity is not an Inventory, 0 is returned instead.
    */
   public static int getRedstoneSignalFromBlockEntity(@Nullable BlockEntity pBlockEntity) {
      return pBlockEntity instanceof Container ? getRedstoneSignalFromContainer((Container)pBlockEntity) : 0;
   }

   public static int getRedstoneSignalFromContainer(@Nullable Container pContainer) {
      if (pContainer == null) {
         return 0;
      } else {
         int i = 0;
         float f = 0.0F;

         for(int j = 0; j < pContainer.getContainerSize(); ++j) {
            ItemStack itemstack = pContainer.getItem(j);
            if (!itemstack.isEmpty()) {
               f += (float)itemstack.getCount() / (float)Math.min(pContainer.getMaxStackSize(), itemstack.getMaxStackSize());
               ++i;
            }
         }

         f /= (float)pContainer.getContainerSize();
         return Mth.floor(f * 14.0F) + (i > 0 ? 1 : 0);
      }
   }

   public void setCarried(ItemStack pStack) {
      this.carried = pStack;
   }

   public ItemStack getCarried() {
      return this.carried;
   }

   public void suppressRemoteUpdates() {
      this.suppressRemoteUpdates = true;
   }

   public void resumeRemoteUpdates() {
      this.suppressRemoteUpdates = false;
   }

   public void transferState(AbstractContainerMenu pMenu) {
      Table<Container, Integer, Integer> table = HashBasedTable.create();

      for(int i = 0; i < pMenu.slots.size(); ++i) {
         Slot slot = pMenu.slots.get(i);
         table.put(slot.container, slot.getContainerSlot(), i);
      }

      for(int j = 0; j < this.slots.size(); ++j) {
         Slot slot1 = this.slots.get(j);
         Integer integer = table.get(slot1.container, slot1.getContainerSlot());
         if (integer != null) {
            this.lastSlots.set(j, pMenu.lastSlots.get(integer));
            this.remoteSlots.set(j, pMenu.remoteSlots.get(integer));
         }
      }

   }

   public OptionalInt findSlot(Container pContainer, int pSlotIndex) {
      for(int i = 0; i < this.slots.size(); ++i) {
         Slot slot = this.slots.get(i);
         if (slot.container == pContainer && pSlotIndex == slot.getContainerSlot()) {
            return OptionalInt.of(i);
         }
      }

      return OptionalInt.empty();
   }

   public int getStateId() {
      return this.stateId;
   }

   public int incrementStateId() {
      this.stateId = this.stateId + 1 & 32767;
      return this.stateId;
   }
}
