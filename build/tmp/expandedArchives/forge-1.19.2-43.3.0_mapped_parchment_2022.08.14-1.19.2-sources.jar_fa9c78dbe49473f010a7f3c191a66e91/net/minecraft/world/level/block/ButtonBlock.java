package net.minecraft.world.level.block;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class ButtonBlock extends FaceAttachedHorizontalDirectionalBlock {
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
   private static final int PRESSED_DEPTH = 1;
   private static final int UNPRESSED_DEPTH = 2;
   protected static final int HALF_AABB_HEIGHT = 2;
   protected static final int HALF_AABB_WIDTH = 3;
   protected static final VoxelShape CEILING_AABB_X = Block.box(6.0D, 14.0D, 5.0D, 10.0D, 16.0D, 11.0D);
   protected static final VoxelShape CEILING_AABB_Z = Block.box(5.0D, 14.0D, 6.0D, 11.0D, 16.0D, 10.0D);
   protected static final VoxelShape FLOOR_AABB_X = Block.box(6.0D, 0.0D, 5.0D, 10.0D, 2.0D, 11.0D);
   protected static final VoxelShape FLOOR_AABB_Z = Block.box(5.0D, 0.0D, 6.0D, 11.0D, 2.0D, 10.0D);
   protected static final VoxelShape NORTH_AABB = Block.box(5.0D, 6.0D, 14.0D, 11.0D, 10.0D, 16.0D);
   protected static final VoxelShape SOUTH_AABB = Block.box(5.0D, 6.0D, 0.0D, 11.0D, 10.0D, 2.0D);
   protected static final VoxelShape WEST_AABB = Block.box(14.0D, 6.0D, 5.0D, 16.0D, 10.0D, 11.0D);
   protected static final VoxelShape EAST_AABB = Block.box(0.0D, 6.0D, 5.0D, 2.0D, 10.0D, 11.0D);
   protected static final VoxelShape PRESSED_CEILING_AABB_X = Block.box(6.0D, 15.0D, 5.0D, 10.0D, 16.0D, 11.0D);
   protected static final VoxelShape PRESSED_CEILING_AABB_Z = Block.box(5.0D, 15.0D, 6.0D, 11.0D, 16.0D, 10.0D);
   protected static final VoxelShape PRESSED_FLOOR_AABB_X = Block.box(6.0D, 0.0D, 5.0D, 10.0D, 1.0D, 11.0D);
   protected static final VoxelShape PRESSED_FLOOR_AABB_Z = Block.box(5.0D, 0.0D, 6.0D, 11.0D, 1.0D, 10.0D);
   protected static final VoxelShape PRESSED_NORTH_AABB = Block.box(5.0D, 6.0D, 15.0D, 11.0D, 10.0D, 16.0D);
   protected static final VoxelShape PRESSED_SOUTH_AABB = Block.box(5.0D, 6.0D, 0.0D, 11.0D, 10.0D, 1.0D);
   protected static final VoxelShape PRESSED_WEST_AABB = Block.box(15.0D, 6.0D, 5.0D, 16.0D, 10.0D, 11.0D);
   protected static final VoxelShape PRESSED_EAST_AABB = Block.box(0.0D, 6.0D, 5.0D, 1.0D, 10.0D, 11.0D);
   private final boolean sensitive;

   protected ButtonBlock(boolean pSensitive, BlockBehaviour.Properties pProperties) {
      super(pProperties);
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, Boolean.valueOf(false)).setValue(FACE, AttachFace.WALL));
      this.sensitive = pSensitive;
   }

   private int getPressDuration() {
      return this.sensitive ? 30 : 20;
   }

   public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      Direction direction = pState.getValue(FACING);
      boolean flag = pState.getValue(POWERED);
      switch ((AttachFace)pState.getValue(FACE)) {
         case FLOOR:
            if (direction.getAxis() == Direction.Axis.X) {
               return flag ? PRESSED_FLOOR_AABB_X : FLOOR_AABB_X;
            }

            return flag ? PRESSED_FLOOR_AABB_Z : FLOOR_AABB_Z;
         case WALL:
            switch (direction) {
               case EAST:
                  return flag ? PRESSED_EAST_AABB : EAST_AABB;
               case WEST:
                  return flag ? PRESSED_WEST_AABB : WEST_AABB;
               case SOUTH:
                  return flag ? PRESSED_SOUTH_AABB : SOUTH_AABB;
               case NORTH:
               default:
                  return flag ? PRESSED_NORTH_AABB : NORTH_AABB;
            }
         case CEILING:
         default:
            if (direction.getAxis() == Direction.Axis.X) {
               return flag ? PRESSED_CEILING_AABB_X : CEILING_AABB_X;
            } else {
               return flag ? PRESSED_CEILING_AABB_Z : CEILING_AABB_Z;
            }
      }
   }

   public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
      if (pState.getValue(POWERED)) {
         return InteractionResult.CONSUME;
      } else {
         this.press(pState, pLevel, pPos);
         this.playSound(pPlayer, pLevel, pPos, true);
         pLevel.gameEvent(pPlayer, GameEvent.BLOCK_ACTIVATE, pPos);
         return InteractionResult.sidedSuccess(pLevel.isClientSide);
      }
   }

   public void press(BlockState pState, Level pLevel, BlockPos pPos) {
      pLevel.setBlock(pPos, pState.setValue(POWERED, Boolean.valueOf(true)), 3);
      this.updateNeighbours(pState, pLevel, pPos);
      pLevel.scheduleTick(pPos, this, this.getPressDuration());
   }

   protected void playSound(@Nullable Player pPlayer, LevelAccessor pLevel, BlockPos pPos, boolean pHitByArrow) {
      pLevel.playSound(pHitByArrow ? pPlayer : null, pPos, this.getSound(pHitByArrow), SoundSource.BLOCKS, 0.3F, pHitByArrow ? 0.6F : 0.5F);
   }

   protected abstract SoundEvent getSound(boolean pIsOn);

   public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
      if (!pIsMoving && !pState.is(pNewState.getBlock())) {
         if (pState.getValue(POWERED)) {
            this.updateNeighbours(pState, pLevel, pPos);
         }

         super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
      }
   }

   /**
    * @deprecated call via {@link net.minecraft.world.level.block.state.BlockBehavior.BlockStateBase#getSignal} whenever
    * possible. Implementing/overriding is fine.
    */
   public int getSignal(BlockState pBlockState, BlockGetter pBlockAccess, BlockPos pPos, Direction pSide) {
      return pBlockState.getValue(POWERED) ? 15 : 0;
   }

   /**
    * @deprecated call via {@link net.minecraft.world.level.block.state.BlockBehavior.BlockStateBase#getDirectSignal}
    * whenever possible. Implementing/overriding is fine.
    */
   public int getDirectSignal(BlockState pBlockState, BlockGetter pBlockAccess, BlockPos pPos, Direction pSide) {
      return pBlockState.getValue(POWERED) && getConnectedDirection(pBlockState) == pSide ? 15 : 0;
   }

   /**
    * Can this block provide power. Only wire currently seems to have this change based on its state.
    * @deprecated call via {@link net.minecraft.world.level.block.state.BlockBehavior.BlockStateBase#isSignalSource}
    * whenever possible. Implementing/overriding is fine.
    */
   public boolean isSignalSource(BlockState pState) {
      return true;
   }

   public void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
      if (pState.getValue(POWERED)) {
         if (this.sensitive) {
            this.checkPressed(pState, pLevel, pPos);
         } else {
            pLevel.setBlock(pPos, pState.setValue(POWERED, Boolean.valueOf(false)), 3);
            this.updateNeighbours(pState, pLevel, pPos);
            this.playSound((Player)null, pLevel, pPos, false);
            pLevel.gameEvent((Entity)null, GameEvent.BLOCK_DEACTIVATE, pPos);
         }

      }
   }

   public void entityInside(BlockState pState, Level pLevel, BlockPos pPos, Entity pEntity) {
      if (!pLevel.isClientSide && this.sensitive && !pState.getValue(POWERED)) {
         this.checkPressed(pState, pLevel, pPos);
      }
   }

   private void checkPressed(BlockState pState, Level pLevel, BlockPos pPos) {
      List<? extends Entity> list = pLevel.getEntitiesOfClass(AbstractArrow.class, pState.getShape(pLevel, pPos).bounds().move(pPos));
      boolean flag = !list.isEmpty();
      boolean flag1 = pState.getValue(POWERED);
      if (flag != flag1) {
         pLevel.setBlock(pPos, pState.setValue(POWERED, Boolean.valueOf(flag)), 3);
         this.updateNeighbours(pState, pLevel, pPos);
         this.playSound((Player)null, pLevel, pPos, flag);
         pLevel.gameEvent(list.stream().findFirst().orElse(null), flag ? GameEvent.BLOCK_ACTIVATE : GameEvent.BLOCK_DEACTIVATE, pPos);
      }

      if (flag) {
         pLevel.scheduleTick(new BlockPos(pPos), this, this.getPressDuration());
      }

   }

   private void updateNeighbours(BlockState pState, Level pLevel, BlockPos pPos) {
      pLevel.updateNeighborsAt(pPos, this);
      pLevel.updateNeighborsAt(pPos.relative(getConnectedDirection(pState).getOpposite()), this);
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
      pBuilder.add(FACING, POWERED, FACE);
   }
}