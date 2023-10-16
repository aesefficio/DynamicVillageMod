package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class NetherrackBlock extends Block implements BonemealableBlock {
   public NetherrackBlock(BlockBehaviour.Properties pProperties) {
      super(pProperties);
   }

   /**
    * @return whether bonemeal can be used on this block
    */
   public boolean isValidBonemealTarget(BlockGetter pLevel, BlockPos pPos, BlockState pState, boolean pIsClient) {
      if (!pLevel.getBlockState(pPos.above()).propagatesSkylightDown(pLevel, pPos)) {
         return false;
      } else {
         for(BlockPos blockpos : BlockPos.betweenClosed(pPos.offset(-1, -1, -1), pPos.offset(1, 1, 1))) {
            if (pLevel.getBlockState(blockpos).is(BlockTags.NYLIUM)) {
               return true;
            }
         }

         return false;
      }
   }

   public boolean isBonemealSuccess(Level pLevel, RandomSource pRandom, BlockPos pPos, BlockState pState) {
      return true;
   }

   public void performBonemeal(ServerLevel pLevel, RandomSource pRandom, BlockPos pPos, BlockState pState) {
      boolean flag = false;
      boolean flag1 = false;

      for(BlockPos blockpos : BlockPos.betweenClosed(pPos.offset(-1, -1, -1), pPos.offset(1, 1, 1))) {
         BlockState blockstate = pLevel.getBlockState(blockpos);
         if (blockstate.is(Blocks.WARPED_NYLIUM)) {
            flag1 = true;
         }

         if (blockstate.is(Blocks.CRIMSON_NYLIUM)) {
            flag = true;
         }

         if (flag1 && flag) {
            break;
         }
      }

      if (flag1 && flag) {
         pLevel.setBlock(pPos, pRandom.nextBoolean() ? Blocks.WARPED_NYLIUM.defaultBlockState() : Blocks.CRIMSON_NYLIUM.defaultBlockState(), 3);
      } else if (flag1) {
         pLevel.setBlock(pPos, Blocks.WARPED_NYLIUM.defaultBlockState(), 3);
      } else if (flag) {
         pLevel.setBlock(pPos, Blocks.CRIMSON_NYLIUM.defaultBlockState(), 3);
      }

   }
}