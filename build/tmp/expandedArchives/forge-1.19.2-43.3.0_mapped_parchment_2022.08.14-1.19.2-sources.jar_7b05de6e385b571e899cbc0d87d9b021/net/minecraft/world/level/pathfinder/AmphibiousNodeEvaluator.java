package net.minecraft.world.level.pathfinder;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.PathNavigationRegion;

public class AmphibiousNodeEvaluator extends WalkNodeEvaluator {
   private final boolean prefersShallowSwimming;
   private float oldWalkableCost;
   private float oldWaterBorderCost;

   public AmphibiousNodeEvaluator(boolean pPrefersShallowSwimming) {
      this.prefersShallowSwimming = pPrefersShallowSwimming;
   }

   public void prepare(PathNavigationRegion pLevel, Mob pMob) {
      super.prepare(pLevel, pMob);
      pMob.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
      this.oldWalkableCost = pMob.getPathfindingMalus(BlockPathTypes.WALKABLE);
      pMob.setPathfindingMalus(BlockPathTypes.WALKABLE, 6.0F);
      this.oldWaterBorderCost = pMob.getPathfindingMalus(BlockPathTypes.WATER_BORDER);
      pMob.setPathfindingMalus(BlockPathTypes.WATER_BORDER, 4.0F);
   }

   /**
    * This method is called when all nodes have been processed and PathEntity is created.
    * {@link net.minecraft.world.pathfinder.WalkNodeProcessor WalkNodeProcessor} uses this to change its field {@link
    * net.minecraft.world.pathfinder.WalkNodeProcessor#avoidsWater avoidsWater}
    */
   public void done() {
      this.mob.setPathfindingMalus(BlockPathTypes.WALKABLE, this.oldWalkableCost);
      this.mob.setPathfindingMalus(BlockPathTypes.WATER_BORDER, this.oldWaterBorderCost);
      super.done();
   }

   @Nullable
   public Node getStart() {
      return this.getStartNode(new BlockPos(Mth.floor(this.mob.getBoundingBox().minX), Mth.floor(this.mob.getBoundingBox().minY + 0.5D), Mth.floor(this.mob.getBoundingBox().minZ)));
   }

   @Nullable
   public Target getGoal(double pX, double pY, double pZ) {
      return this.getTargetFromNode(this.getNode(Mth.floor(pX), Mth.floor(pY + 0.5D), Mth.floor(pZ)));
   }

   public int getNeighbors(Node[] p_164676_, Node p_164677_) {
      int i = super.getNeighbors(p_164676_, p_164677_);
      BlockPathTypes blockpathtypes = this.getCachedBlockType(this.mob, p_164677_.x, p_164677_.y + 1, p_164677_.z);
      BlockPathTypes blockpathtypes1 = this.getCachedBlockType(this.mob, p_164677_.x, p_164677_.y, p_164677_.z);
      int j;
      if (this.mob.getPathfindingMalus(blockpathtypes) >= 0.0F && blockpathtypes1 != BlockPathTypes.STICKY_HONEY) {
         j = Mth.floor(Math.max(1.0F, this.mob.getStepHeight()));
      } else {
         j = 0;
      }

      double d0 = this.getFloorLevel(new BlockPos(p_164677_.x, p_164677_.y, p_164677_.z));
      Node node = this.findAcceptedNode(p_164677_.x, p_164677_.y + 1, p_164677_.z, Math.max(0, j - 1), d0, Direction.UP, blockpathtypes1);
      Node node1 = this.findAcceptedNode(p_164677_.x, p_164677_.y - 1, p_164677_.z, j, d0, Direction.DOWN, blockpathtypes1);
      if (this.isVerticalNeighborValid(node, p_164677_)) {
         p_164676_[i++] = node;
      }

      if (this.isVerticalNeighborValid(node1, p_164677_) && blockpathtypes1 != BlockPathTypes.TRAPDOOR) {
         p_164676_[i++] = node1;
      }

      for(int k = 0; k < i; ++k) {
         Node node2 = p_164676_[k];
         if (node2.type == BlockPathTypes.WATER && this.prefersShallowSwimming && node2.y < this.mob.level.getSeaLevel() - 10) {
            ++node2.costMalus;
         }
      }

      return i;
   }

   private boolean isVerticalNeighborValid(@Nullable Node p_230611_, Node p_230612_) {
      return this.isNeighborValid(p_230611_, p_230612_) && p_230611_.type == BlockPathTypes.WATER;
   }

   protected double getFloorLevel(BlockPos pPos) {
      return this.mob.isInWater() ? (double)pPos.getY() + 0.5D : super.getFloorLevel(pPos);
   }

   protected boolean isAmphibious() {
      return true;
   }

   /**
    * Returns the node type at the specified postion taking the block below into account
    */
   public BlockPathTypes getBlockPathType(BlockGetter pLevel, int pX, int pY, int pZ) {
      BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
      BlockPathTypes blockpathtypes = getBlockPathTypeRaw(pLevel, blockpos$mutableblockpos.set(pX, pY, pZ));
      if (blockpathtypes == BlockPathTypes.WATER) {
         for(Direction direction : Direction.values()) {
            BlockPathTypes blockpathtypes1 = getBlockPathTypeRaw(pLevel, blockpos$mutableblockpos.set(pX, pY, pZ).move(direction));
            if (blockpathtypes1 == BlockPathTypes.BLOCKED) {
               return BlockPathTypes.WATER_BORDER;
            }
         }

         return BlockPathTypes.WATER;
      } else {
         return getBlockPathTypeStatic(pLevel, blockpos$mutableblockpos);
      }
   }
}
