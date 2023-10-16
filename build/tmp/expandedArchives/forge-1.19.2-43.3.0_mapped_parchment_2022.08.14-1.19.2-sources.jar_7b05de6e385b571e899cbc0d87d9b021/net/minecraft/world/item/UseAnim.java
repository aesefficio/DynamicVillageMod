package net.minecraft.world.item;

public enum UseAnim {
   NONE,
   EAT,
   DRINK,
   BLOCK,
   BOW,
   SPEAR,
   CROSSBOW,
   SPYGLASS,
   TOOT_HORN,
   /**
    * Items with custom arm animation should return this in `Item#getUseAnimation`
    * to prevent vanilla from also trying to animate same item
    */
   CUSTOM
}
