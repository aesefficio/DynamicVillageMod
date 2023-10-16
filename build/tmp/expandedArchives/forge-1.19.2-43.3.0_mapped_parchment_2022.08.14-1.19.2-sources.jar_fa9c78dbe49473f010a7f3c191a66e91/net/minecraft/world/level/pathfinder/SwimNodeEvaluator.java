package net.minecraft.world.level.pathfinder;

import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class SwimNodeEvaluator extends NodeEvaluator {
   private final boolean allowBreaching;
   private final Long2ObjectMap<BlockPathTypes> pathTypesByPosCache = new Long2ObjectOpenHashMap<>();

   public SwimNodeEvaluator(boolean pAllowBreaching) {
      this.allowBreaching = pAllowBreaching;
   }

   public void prepare(PathNavigationRegion pLevel, Mob pMob) {
      super.prepare(pLevel, pMob);
      this.pathTypesByPosCache.clear();
   }

   /**
    * This method is called when all nodes have been processed and PathEntity is created.
    * {@link net.minecraft.world.pathfinder.WalkNodeProcessor WalkNodeProcessor} uses this to change its field {@link
    * net.minecraft.world.pathfinder.WalkNodeProcessor#avoidsWater avoidsWater}
    */
   public void done() {
      super.done();
      this.pathTypesByPosCache.clear();
   }

   @Nullable
   public Node getStart() {
      return super.getNode(Mth.floor(this.mob.getBoundingBox().minX), Mth.floor(this.mob.getBoundingBox().minY + 0.5D), Mth.floor(this.mob.getBoundingBox().minZ));
   }

   @Nullable
   public Target getGoal(double pX, double pY, double pZ) {
      return this.getTargetFromNode(super.getNode(Mth.floor(pX), Mth.floor(pY), Mth.floor(pZ)));
   }

   public int getNeighbors(Node[] p_77483_, Node p_77484_) {
      int i = 0;
      Map<Direction, Node> map = Maps.newEnumMap(Direction.class);

      for(Direction direction : Direction.values()) {
         Node node = this.getNode(p_77484_.x + direction.getStepX(), p_77484_.y + direction.getStepY(), p_77484_.z + direction.getStepZ());
         map.put(direction, node);
         if (this.isNodeValid(node)) {
            p_77483_[i++] = node;
         }
      }

      for(Direction direction1 : Direction.Plane.HORIZONTAL) {
         Direction direction2 = direction1.getClockWise();
         Node node1 = this.getNode(p_77484_.x + direction1.getStepX() + direction2.getStepX(), p_77484_.y, p_77484_.z + direction1.getStepZ() + direction2.getStepZ());
         if (this.isDiagonalNodeValid(node1, map.get(direction1), map.get(direction2))) {
            p_77483_[i++] = node1;
         }
      }

      return i;
   }

   protected boolean isNodeValid(@Nullable Node pNode) {
      return pNode != null && !pNode.closed;
   }

   protected boolean isDiagonalNodeValid(@Nullable Node p_192964_, @Nullable Node p_192965_, @Nullable Node p_192966_) {
      return this.isNodeValid(p_192964_) && p_192965_ != null && p_192965_.costMalus >= 0.0F && p_192966_ != null && p_192966_.costMalus >= 0.0F;
   }

   /**
    * Returns a mapped point or creates and adds one
    */
   @Nullable
   protected Node getNode(int pX, int pY, int pZ) {
      Node node = null;
      BlockPathTypes blockpathtypes = this.getCachedBlockType(pX, pY, pZ);
      if (this.allowBreaching && blockpathtypes == BlockPathTypes.BREACH || blockpathtypes == BlockPathTypes.WATER) {
         float f = this.mob.getPathfindingMalus(blockpathtypes);
         if (f >= 0.0F) {
            node = super.getNode(pX, pY, pZ);
            if (node != null) {
               node.type = blockpathtypes;
               node.costMalus = Math.max(node.costMalus, f);
               if (this.level.getFluidState(new BlockPos(pX, pY, pZ)).isEmpty()) {
                  node.costMalus += 8.0F;
               }
            }
         }
      }

      return node;
   }

   protected BlockPathTypes getCachedBlockType(int pX, int pY, int pZ) {
      return this.pathTypesByPosCache.computeIfAbsent(BlockPos.asLong(pX, pY, pZ), (p_192957_) -> {
         return this.getBlockPathType(this.level, pX, pY, pZ);
      });
   }

   /**
    * Returns the node type at the specified postion taking the block below into account
    */
   public BlockPathTypes getBlockPathType(BlockGetter pLevel, int pX, int pY, int pZ) {
      return this.getBlockPathType(pLevel, pX, pY, pZ, this.mob, this.entityWidth, this.entityHeight, this.entityDepth, this.canOpenDoors(), this.canPassDoors());
   }

   /**
    * Returns the significant (e.g LAVA if the entity were half in lava) node type at the location taking the
    * surroundings and the entity size in account
    */
   public BlockPathTypes getBlockPathType(BlockGetter pBlockaccess, int pX, int pY, int pZ, Mob pEntityliving, int pXSize, int pYSize, int pZSize, boolean pCanBreakDoors, boolean pCanEnterDoors) {
      BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

      for(int i = pX; i < pX + pXSize; ++i) {
         for(int j = pY; j < pY + pYSize; ++j) {
            for(int k = pZ; k < pZ + pZSize; ++k) {
               FluidState fluidstate = pBlockaccess.getFluidState(blockpos$mutableblockpos.set(i, j, k));
               BlockState blockstate = pBlockaccess.getBlockState(blockpos$mutableblockpos.set(i, j, k));
               if (fluidstate.isEmpty() && blockstate.isPathfindable(pBlockaccess, blockpos$mutableblockpos.below(), PathComputationType.WATER) && blockstate.isAir()) {
                  return BlockPathTypes.BREACH;
               }

               if (!fluidstate.is(FluidTags.WATER)) {
                  return BlockPathTypes.BLOCKED;
               }
            }
         }
      }

      BlockState blockstate1 = pBlockaccess.getBlockState(blockpos$mutableblockpos);
      return blockstate1.isPathfindable(pBlockaccess, blockpos$mutableblockpos, PathComputationType.WATER) ? BlockPathTypes.WATER : BlockPathTypes.BLOCKED;
   }
}