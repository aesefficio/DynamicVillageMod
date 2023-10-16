package net.minecraft.world.level.block;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class SignBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   protected static final float AABB_OFFSET = 4.0F;
   protected static final VoxelShape SHAPE = Block.box(4.0D, 0.0D, 4.0D, 12.0D, 16.0D, 12.0D);
   private final WoodType type;

   protected SignBlock(BlockBehaviour.Properties pProperties, WoodType pType) {
      super(pProperties);
      this.type = pType;
   }

   /**
    * Update the provided state given the provided neighbor direction and neighbor state, returning a new state.
    * For example, fences make their connections to the passed in state if possible, and wet concrete powder immediately
    * returns its solidified counterpart.
    * Note that this method should ideally consider only the specific direction passed in.
    */
   public BlockState updateShape(BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos) {
      if (pState.getValue(WATERLOGGED)) {
         pLevel.scheduleTick(pCurrentPos, Fluids.WATER, Fluids.WATER.getTickDelay(pLevel));
      }

      return super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
   }

   public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return SHAPE;
   }

   /**
    * @return true if an entity can be spawned inside this block
    */
   public boolean isPossibleToRespawnInThis() {
      return true;
   }

   public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
      return new SignBlockEntity(pPos, pState);
   }

   public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
      ItemStack itemstack = pPlayer.getItemInHand(pHand);
      Item item = itemstack.getItem();
      boolean flag = item instanceof DyeItem;
      boolean flag1 = itemstack.is(Items.GLOW_INK_SAC);
      boolean flag2 = itemstack.is(Items.INK_SAC);
      boolean flag3 = (flag1 || flag || flag2) && pPlayer.getAbilities().mayBuild;
      if (pLevel.isClientSide) {
         return flag3 ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
      } else {
         BlockEntity blockentity = pLevel.getBlockEntity(pPos);
         if (!(blockentity instanceof SignBlockEntity)) {
            return InteractionResult.PASS;
         } else {
            SignBlockEntity signblockentity = (SignBlockEntity)blockentity;
            boolean flag4 = signblockentity.hasGlowingText();
            if ((!flag1 || !flag4) && (!flag2 || flag4)) {
               if (flag3) {
                  boolean flag5;
                  if (flag1) {
                     pLevel.playSound((Player)null, pPos, SoundEvents.GLOW_INK_SAC_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
                     flag5 = signblockentity.setHasGlowingText(true);
                     if (pPlayer instanceof ServerPlayer) {
                        CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger((ServerPlayer)pPlayer, pPos, itemstack);
                     }
                  } else if (flag2) {
                     pLevel.playSound((Player)null, pPos, SoundEvents.INK_SAC_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
                     flag5 = signblockentity.setHasGlowingText(false);
                  } else {
                     pLevel.playSound((Player)null, pPos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
                     flag5 = signblockentity.setColor(((DyeItem)item).getDyeColor());
                  }

                  if (flag5) {
                     if (!pPlayer.isCreative()) {
                        itemstack.shrink(1);
                     }

                     pPlayer.awardStat(Stats.ITEM_USED.get(item));
                  }
               }

               return signblockentity.executeClickCommands((ServerPlayer)pPlayer) ? InteractionResult.SUCCESS : InteractionResult.PASS;
            } else {
               return InteractionResult.PASS;
            }
         }
      }
   }

   public FluidState getFluidState(BlockState pState) {
      return pState.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(pState);
   }

   public WoodType type() {
      return this.type;
   }
}