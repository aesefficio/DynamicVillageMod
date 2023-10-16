package net.minecraft.world.level.block;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.AABB;

public class PressurePlateBlock extends BasePressurePlateBlock {
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
   private final PressurePlateBlock.Sensitivity sensitivity;

   public PressurePlateBlock(PressurePlateBlock.Sensitivity pSensitivity, BlockBehaviour.Properties pProperties) {
      super(pProperties);
      this.registerDefaultState(this.stateDefinition.any().setValue(POWERED, Boolean.valueOf(false)));
      this.sensitivity = pSensitivity;
   }

   protected int getSignalForState(BlockState pState) {
      return pState.getValue(POWERED) ? 15 : 0;
   }

   protected BlockState setSignalForState(BlockState pState, int pStrength) {
      return pState.setValue(POWERED, Boolean.valueOf(pStrength > 0));
   }

   protected void playOnSound(LevelAccessor pLevel, BlockPos pPos) {
      if (this.material != Material.WOOD && this.material != Material.NETHER_WOOD) {
         pLevel.playSound((Player)null, pPos, SoundEvents.STONE_PRESSURE_PLATE_CLICK_ON, SoundSource.BLOCKS, 0.3F, 0.6F);
      } else {
         pLevel.playSound((Player)null, pPos, SoundEvents.WOODEN_PRESSURE_PLATE_CLICK_ON, SoundSource.BLOCKS, 0.3F, 0.8F);
      }

   }

   protected void playOffSound(LevelAccessor pLevel, BlockPos pPos) {
      if (this.material != Material.WOOD && this.material != Material.NETHER_WOOD) {
         pLevel.playSound((Player)null, pPos, SoundEvents.STONE_PRESSURE_PLATE_CLICK_OFF, SoundSource.BLOCKS, 0.3F, 0.5F);
      } else {
         pLevel.playSound((Player)null, pPos, SoundEvents.WOODEN_PRESSURE_PLATE_CLICK_OFF, SoundSource.BLOCKS, 0.3F, 0.7F);
      }

   }

   protected int getSignalStrength(Level pLevel, BlockPos pPos) {
      AABB aabb = TOUCH_AABB.move(pPos);
      List<? extends Entity> list;
      switch (this.sensitivity) {
         case EVERYTHING:
            list = pLevel.getEntities((Entity)null, aabb);
            break;
         case MOBS:
            list = pLevel.getEntitiesOfClass(LivingEntity.class, aabb);
            break;
         default:
            return 0;
      }

      if (!list.isEmpty()) {
         for(Entity entity : list) {
            if (!entity.isIgnoringBlockTriggers()) {
               return 15;
            }
         }
      }

      return 0;
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
      pBuilder.add(POWERED);
   }

   public static enum Sensitivity {
      EVERYTHING,
      MOBS;
   }
}