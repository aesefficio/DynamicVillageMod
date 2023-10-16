package net.minecraft.world.level.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.PushReaction;

public class IceBlock extends HalfTransparentBlock {
   public IceBlock(BlockBehaviour.Properties pProperties) {
      super(pProperties);
   }

   /**
    * Called after a player has successfully harvested this block. This method will only be called if the player has
    * used the correct tool and drops should be spawned.
    */
   public void playerDestroy(Level pLevel, Player pPlayer, BlockPos pPos, BlockState pState, @Nullable BlockEntity pTe, ItemStack pStack) {
      super.playerDestroy(pLevel, pPlayer, pPos, pState, pTe, pStack);
      if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, pStack) == 0) {
         if (pLevel.dimensionType().ultraWarm()) {
            pLevel.removeBlock(pPos, false);
            return;
         }

         Material material = pLevel.getBlockState(pPos.below()).getMaterial();
         if (material.blocksMotion() || material.isLiquid()) {
            pLevel.setBlockAndUpdate(pPos, Blocks.WATER.defaultBlockState());
         }
      }

   }

   /**
    * Performs a random tick on a block.
    */
   public void randomTick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
      if (pLevel.getBrightness(LightLayer.BLOCK, pPos) > 11 - pState.getLightBlock(pLevel, pPos)) {
         this.melt(pState, pLevel, pPos);
      }

   }

   protected void melt(BlockState pState, Level pLevel, BlockPos pPos) {
      if (pLevel.dimensionType().ultraWarm()) {
         pLevel.removeBlock(pPos, false);
      } else {
         pLevel.setBlockAndUpdate(pPos, Blocks.WATER.defaultBlockState());
         pLevel.neighborChanged(pPos, Blocks.WATER, pPos);
      }
   }

   /**
    * @deprecated call via {@link
    * net.minecraft.world.level.block.state.BlockBehavior.BlockStateBase#getPistonPushReaction} whenever possible.
    * Implementing/overriding is fine.
    */
   public PushReaction getPistonPushReaction(BlockState pState) {
      return PushReaction.NORMAL;
   }
}