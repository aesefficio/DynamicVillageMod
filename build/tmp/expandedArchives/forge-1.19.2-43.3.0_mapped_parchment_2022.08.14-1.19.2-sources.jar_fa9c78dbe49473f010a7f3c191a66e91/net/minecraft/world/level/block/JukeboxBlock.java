package net.minecraft.world.level.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

public class JukeboxBlock extends BaseEntityBlock {
   public static final BooleanProperty HAS_RECORD = BlockStateProperties.HAS_RECORD;

   public JukeboxBlock(BlockBehaviour.Properties pProperties) {
      super(pProperties);
      this.registerDefaultState(this.stateDefinition.any().setValue(HAS_RECORD, Boolean.valueOf(false)));
   }

   /**
    * Called by BlockItem after this block has been placed.
    */
   public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, @Nullable LivingEntity pPlacer, ItemStack pStack) {
      super.setPlacedBy(pLevel, pPos, pState, pPlacer, pStack);
      CompoundTag compoundtag = BlockItem.getBlockEntityData(pStack);
      if (compoundtag != null && compoundtag.contains("RecordItem")) {
         pLevel.setBlock(pPos, pState.setValue(HAS_RECORD, Boolean.valueOf(true)), 2);
      }

   }

   public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
      if (pState.getValue(HAS_RECORD)) {
         this.dropRecording(pLevel, pPos);
         pState = pState.setValue(HAS_RECORD, Boolean.valueOf(false));
         pLevel.gameEvent(GameEvent.JUKEBOX_STOP_PLAY, pPos, GameEvent.Context.of(pState));
         pLevel.setBlock(pPos, pState, 2);
         pLevel.gameEvent(GameEvent.BLOCK_CHANGE, pPos, GameEvent.Context.of(pPlayer, pState));
         return InteractionResult.sidedSuccess(pLevel.isClientSide);
      } else {
         return InteractionResult.PASS;
      }
   }

   public void setRecord(@Nullable Entity pEntity, LevelAccessor pLevel, BlockPos pPos, BlockState pState, ItemStack pStack) {
      BlockEntity blockentity = pLevel.getBlockEntity(pPos);
      if (blockentity instanceof JukeboxBlockEntity jukeboxblockentity) {
         jukeboxblockentity.setRecord(pStack.copy());
         jukeboxblockentity.playRecord();
         pLevel.setBlock(pPos, pState.setValue(HAS_RECORD, Boolean.valueOf(true)), 2);
         pLevel.gameEvent(GameEvent.BLOCK_CHANGE, pPos, GameEvent.Context.of(pEntity, pState));
      }

   }

   private void dropRecording(Level pLevel, BlockPos pPos) {
      if (!pLevel.isClientSide) {
         BlockEntity blockentity = pLevel.getBlockEntity(pPos);
         if (blockentity instanceof JukeboxBlockEntity) {
            JukeboxBlockEntity jukeboxblockentity = (JukeboxBlockEntity)blockentity;
            ItemStack itemstack = jukeboxblockentity.getRecord();
            if (!itemstack.isEmpty()) {
               pLevel.levelEvent(1010, pPos, 0);
               jukeboxblockentity.clearContent();
               float f = 0.7F;
               double d0 = (double)(pLevel.random.nextFloat() * 0.7F) + (double)0.15F;
               double d1 = (double)(pLevel.random.nextFloat() * 0.7F) + (double)0.060000002F + 0.6D;
               double d2 = (double)(pLevel.random.nextFloat() * 0.7F) + (double)0.15F;
               ItemStack itemstack1 = itemstack.copy();
               ItemEntity itementity = new ItemEntity(pLevel, (double)pPos.getX() + d0, (double)pPos.getY() + d1, (double)pPos.getZ() + d2, itemstack1);
               itementity.setDefaultPickUpDelay();
               pLevel.addFreshEntity(itementity);
            }
         }
      }
   }

   public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
      if (!pState.is(pNewState.getBlock())) {
         this.dropRecording(pLevel, pPos);
         super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
      }
   }

   public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
      return new JukeboxBlockEntity(pPos, pState);
   }

   /**
    * @deprecated call via {@link
    * net.minecraft.world.level.block.state.BlockBehavior.BlockStateBase#hasAnalogOutputSignal} whenever possible.
    * Implementing/overriding is fine.
    */
   public boolean hasAnalogOutputSignal(BlockState pState) {
      return true;
   }

   /**
    * @deprecated call via {@link
    * net.minecraft.world.level.block.state.BlockBehavior.BlockStateBase#getAnalogOutputSignal} whenever possible.
    * Implementing/overriding is fine.
    */
   public int getAnalogOutputSignal(BlockState pBlockState, Level pLevel, BlockPos pPos) {
      BlockEntity blockentity = pLevel.getBlockEntity(pPos);
      if (blockentity instanceof JukeboxBlockEntity) {
         Item item = ((JukeboxBlockEntity)blockentity).getRecord().getItem();
         if (item instanceof RecordItem) {
            return ((RecordItem)item).getAnalogOutput();
         }
      }

      return 0;
   }

   /**
    * The type of render function called. MODEL for mixed tesr and static model, MODELBLOCK_ANIMATED for TESR-only,
    * LIQUID for vanilla liquids, INVISIBLE to skip all rendering
    * @deprecated call via {@link net.minecraft.world.level.block.state.BlockBehavior.BlockStateBase#getRenderShape}
    * whenever possible. Implementing/overriding is fine.
    */
   public RenderShape getRenderShape(BlockState pState) {
      return RenderShape.MODEL;
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
      pBuilder.add(HAS_RECORD);
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
      return pState.getValue(HAS_RECORD) ? createTickerHelper(pBlockEntityType, BlockEntityType.JUKEBOX, JukeboxBlockEntity::playRecordTick) : null;
   }
}