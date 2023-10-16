package net.minecraft.core.dispenser;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockSource;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;

public class BoatDispenseItemBehavior extends DefaultDispenseItemBehavior {
   private final DefaultDispenseItemBehavior defaultDispenseItemBehavior = new DefaultDispenseItemBehavior();
   private final Boat.Type type;
   private final boolean isChestBoat;

   public BoatDispenseItemBehavior(Boat.Type pType) {
      this(pType, false);
   }

   public BoatDispenseItemBehavior(Boat.Type p_235891_, boolean p_235892_) {
      this.type = p_235891_;
      this.isChestBoat = p_235892_;
   }

   /**
    * Dispense the specified stack, play the dispense sound and spawn particles.
    */
   public ItemStack execute(BlockSource pSource, ItemStack pStack) {
      Direction direction = pSource.getBlockState().getValue(DispenserBlock.FACING);
      Level level = pSource.getLevel();
      double d0 = pSource.x() + (double)((float)direction.getStepX() * 1.125F);
      double d1 = pSource.y() + (double)((float)direction.getStepY() * 1.125F);
      double d2 = pSource.z() + (double)((float)direction.getStepZ() * 1.125F);
      BlockPos blockpos = pSource.getPos().relative(direction);
      Boat boat = (Boat)(this.isChestBoat ? new ChestBoat(level, d0, d1, d2) : new Boat(level, d0, d1, d2));
      boat.setType(this.type);
      boat.setYRot(direction.toYRot());
      double d3;
      if (boat.canBoatInFluid(level.getFluidState(blockpos))) {
         d3 = 1.0D;
      } else {
         if (!level.getBlockState(blockpos).isAir() || !boat.canBoatInFluid(level.getFluidState(blockpos.below()))) {
            return this.defaultDispenseItemBehavior.dispense(pSource, pStack);
         }

         d3 = 0.0D;
      }

      boat.setPos(d0, d1 + d3, d2);
      level.addFreshEntity(boat);
      pStack.shrink(1);
      return pStack;
   }

   /**
    * Play the dispense sound from the specified block.
    */
   protected void playSound(BlockSource pSource) {
      pSource.getLevel().levelEvent(1000, pSource.getPos(), 0);
   }
}
