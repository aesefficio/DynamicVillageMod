package net.minecraft.world.level.block.state.predicate;

import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;

public class BlockMaterialPredicate implements Predicate<BlockState> {
   private static final BlockMaterialPredicate AIR = new BlockMaterialPredicate(Material.AIR) {
      public boolean test(@Nullable BlockState p_61269_) {
         return p_61269_ != null && p_61269_.isAir();
      }
   };
   private final Material material;

   BlockMaterialPredicate(Material pMaterial) {
      this.material = pMaterial;
   }

   public static BlockMaterialPredicate forMaterial(Material pMaterial) {
      return pMaterial == Material.AIR ? AIR : new BlockMaterialPredicate(pMaterial);
   }

   public boolean test(@Nullable BlockState pState) {
      return pState != null && pState.getMaterial() == this.material;
   }
}