package net.minecraft.world.item;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SignItem extends StandingAndWallBlockItem {
   public SignItem(Item.Properties pProperties, Block pStandingBlock, Block pWallBlock) {
      super(pStandingBlock, pWallBlock, pProperties);
   }

   protected boolean updateCustomBlockEntityTag(BlockPos pPos, Level pLevel, @Nullable Player pPlayer, ItemStack pStack, BlockState pState) {
      boolean flag = super.updateCustomBlockEntityTag(pPos, pLevel, pPlayer, pStack, pState);
      if (!pLevel.isClientSide && !flag && pPlayer != null) {
         pPlayer.openTextEdit((SignBlockEntity)pLevel.getBlockEntity(pPos));
      }

      return flag;
   }
}