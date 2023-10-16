package net.minecraft.world.level.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SculkCatalystBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEventListener;

public class SculkCatalystBlock extends BaseEntityBlock {
   public static final int PULSE_TICKS = 8;
   public static final BooleanProperty PULSE = BlockStateProperties.BLOOM;
   private final IntProvider xpRange = ConstantInt.of(5);

   public SculkCatalystBlock(BlockBehaviour.Properties p_222090_) {
      super(p_222090_);
      this.registerDefaultState(this.stateDefinition.any().setValue(PULSE, Boolean.valueOf(false)));
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_222115_) {
      p_222115_.add(PULSE);
   }

   public void tick(BlockState p_222104_, ServerLevel p_222105_, BlockPos p_222106_, RandomSource p_222107_) {
      if (p_222104_.getValue(PULSE)) {
         p_222105_.setBlock(p_222106_, p_222104_.setValue(PULSE, Boolean.valueOf(false)), 3);
      }

   }

   public static void bloom(ServerLevel p_222095_, BlockPos p_222096_, BlockState p_222097_, RandomSource p_222098_) {
      p_222095_.setBlock(p_222096_, p_222097_.setValue(PULSE, Boolean.valueOf(true)), 3);
      p_222095_.scheduleTick(p_222096_, p_222097_.getBlock(), 8);
      p_222095_.sendParticles(ParticleTypes.SCULK_SOUL, (double)p_222096_.getX() + 0.5D, (double)p_222096_.getY() + 1.15D, (double)p_222096_.getZ() + 0.5D, 2, 0.2D, 0.0D, 0.2D, 0.0D);
      p_222095_.playSound((Player)null, p_222096_, SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.BLOCKS, 2.0F, 0.6F + p_222098_.nextFloat() * 0.4F);
   }

   @Nullable
   public BlockEntity newBlockEntity(BlockPos p_222117_, BlockState p_222118_) {
      return new SculkCatalystBlockEntity(p_222117_, p_222118_);
   }

   @Nullable
   public <T extends BlockEntity> GameEventListener getListener(ServerLevel p_222092_, T p_222093_) {
      return p_222093_ instanceof SculkCatalystBlockEntity ? (SculkCatalystBlockEntity)p_222093_ : null;
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level p_222100_, BlockState p_222101_, BlockEntityType<T> p_222102_) {
      return p_222100_.isClientSide ? null : createTickerHelper(p_222102_, BlockEntityType.SCULK_CATALYST, SculkCatalystBlockEntity::serverTick);
   }

   /**
    * The type of render function called. MODEL for mixed tesr and static model, MODELBLOCK_ANIMATED for TESR-only,
    * LIQUID for vanilla liquids, INVISIBLE to skip all rendering
    * @deprecated call via {@link net.minecraft.world.level.block.state.BlockBehavior.BlockStateBase#getRenderShape}
    * whenever possible. Implementing/overriding is fine.
    */
   public RenderShape getRenderShape(BlockState p_222120_) {
      return RenderShape.MODEL;
   }

   /**
    * Perform side-effects from block dropping, such as creating silverfish
    */
   public void spawnAfterBreak(BlockState p_222109_, ServerLevel p_222110_, BlockPos p_222111_, ItemStack p_222112_, boolean p_222113_) {
      super.spawnAfterBreak(p_222109_, p_222110_, p_222111_, p_222112_, p_222113_);

   }

   @Override
   public int getExpDrop(BlockState state, net.minecraft.world.level.LevelReader level, RandomSource randomSource, BlockPos pos, int fortuneLevel, int silkTouchLevel) {
      return silkTouchLevel == 0 ? this.xpRange.sample(randomSource) : 0;
   }
}
