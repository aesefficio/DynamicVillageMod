package net.minecraft.world.item.enchantment;

import net.minecraft.world.entity.EquipmentSlot;

public class SwiftSneakEnchantment extends Enchantment {
   public SwiftSneakEnchantment(Enchantment.Rarity p_220306_, EquipmentSlot... p_220307_) {
      super(p_220306_, EnchantmentCategory.ARMOR_LEGS, p_220307_);
   }

   /**
    * Returns the minimal value of enchantability needed on the enchantment level passed.
    */
   public int getMinCost(int p_220310_) {
      return p_220310_ * 25;
   }

   public int getMaxCost(int p_220313_) {
      return this.getMinCost(p_220313_) + 50;
   }

   /**
    * Checks if the enchantment should be considered a treasure enchantment. These enchantments can not be obtained
    * using the enchantment table. The mending enchantment is an example of a treasure enchantment.
    * @return Whether or not the enchantment is a treasure enchantment.
    */
   public boolean isTreasureOnly() {
      return true;
   }

   /**
    * Checks if the enchantment can be sold by villagers in their trades.
    */
   public boolean isTradeable() {
      return false;
   }

   /**
    * Checks if the enchantment can be applied to loot table drops.
    */
   public boolean isDiscoverable() {
      return false;
   }

   /**
    * Returns the maximum level that the enchantment can have.
    */
   public int getMaxLevel() {
      return 3;
   }
}