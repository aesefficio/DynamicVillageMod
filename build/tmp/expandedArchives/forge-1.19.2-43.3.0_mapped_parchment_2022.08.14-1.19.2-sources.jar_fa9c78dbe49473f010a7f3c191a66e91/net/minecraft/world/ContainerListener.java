package net.minecraft.world;

public interface ContainerListener {
   /**
    * Called by InventoryBasic.onInventoryChanged() on a array that is never filled.
    */
   void containerChanged(Container pContainer);
}