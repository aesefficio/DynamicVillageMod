package net.minecraft.world.level.pathfinder;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class FlyNodeEvaluator extends WalkNodeEvaluator {
   private final Long2ObjectMap<BlockPathTypes> pathTypeByPosCache = new Long2ObjectOpenHashMap<>();

   public void prepare(PathNavigationRegion pLevel, Mob pMob) {
      super.prepare(pLevel, pMob);
      this.pathTypeByPosCache.clear();
      this.oldWaterCost = pMob.getPathfindingMalus(BlockPathTypes.WATER);
   }

   /**
    * This method is called when all nodes have been processed and PathEntity is created.
    * {@link net.minecraft.world.pathfinder.WalkNodeProcessor WalkNodeProcessor} uses this to change its field {@link
    * net.minecraft.world.pathfinder.WalkNodeProcessor#avoidsWater avoidsWater}
    */
   public void done() {
      this.mob.setPathfindingMalus(BlockPathTypes.WATER, this.oldWaterCost);
      this.pathTypeByPosCache.clear();
      super.done();
   }

   @Nullable
   public Node getStart() {
      int i;
      if (this.canFloat() && this.mob.isInWater()) {
         i = this.mob.getBlockY();
         BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos(this.mob.getX(), (double)i, this.mob.getZ());

         for(BlockState blockstate = this.level.getBlockState(blockpos$mutableblockpos); blockstate.is(Blocks.WATER); blockstate = this.level.getBlockState(blockpos$mutableblockpos)) {
            ++i;
            blockpos$mutableblockpos.set(this.mob.getX(), (double)i, this.mob.getZ());
         }
      } else {
         i = Mth.floor(this.mob.getY() + 0.5D);
      }

      BlockPos blockpos1 = this.mob.blockPosition();
      BlockPathTypes blockpathtypes1 = this.getCachedBlockPathType(blockpos1.getX(), i, blockpos1.getZ());
      if (this.mob.getPathfindingMalus(blockpathtypes1) < 0.0F) {
         for(BlockPos blockpos : this.mob.iteratePathfindingStartNodeCandidatePositions()) {
            BlockPathTypes blockpathtypes = this.getCachedBlockPathType(blockpos.getX(), blockpos.getY(), blockpos.getZ());
            if (this.mob.getPathfindingMalus(blockpathtypes) >= 0.0F) {
               return super.getStartNode(blockpos);
            }
         }
      }

      return super.getStartNode(new BlockPos(blockpos1.getX(), i, blockpos1.getZ()));
   }

   public Target getGoal(double pX, double pY, double pZ) {
      return this.getTargetFromNode(super.getNode(Mth.floor(pX), Mth.floor(pY), Mth.floor(pZ)));
   }

   public int getNeighbors(Node[] p_77266_, Node p_77267_) {
      int i = 0;
      Node node = this.getNode(p_77267_.x, p_77267_.y, p_77267_.z + 1);
      if (this.isOpen(node)) {
         p_77266_[i++] = node;
      }

      Node node1 = this.getNode(p_77267_.x - 1, p_77267_.y, p_77267_.z);
      if (this.isOpen(node1)) {
         p_77266_[i++] = node1;
      }

      Node node2 = this.getNode(p_77267_.x + 1, p_77267_.y, p_77267_.z);
      if (this.isOpen(node2)) {
         p_77266_[i++] = node2;
      }

      Node node3 = this.getNode(p_77267_.x, p_77267_.y, p_77267_.z - 1);
      if (this.isOpen(node3)) {
         p_77266_[i++] = node3;
      }

      Node node4 = this.getNode(p_77267_.x, p_77267_.y + 1, p_77267_.z);
      if (this.isOpen(node4)) {
         p_77266_[i++] = node4;
      }

      Node node5 = this.getNode(p_77267_.x, p_77267_.y - 1, p_77267_.z);
      if (this.isOpen(node5)) {
         p_77266_[i++] = node5;
      }

      Node node6 = this.getNode(p_77267_.x, p_77267_.y + 1, p_77267_.z + 1);
      if (this.isOpen(node6) && this.hasMalus(node) && this.hasMalus(node4)) {
         p_77266_[i++] = node6;
      }

      Node node7 = this.getNode(p_77267_.x - 1, p_77267_.y + 1, p_77267_.z);
      if (this.isOpen(node7) && this.hasMalus(node1) && this.hasMalus(node4)) {
         p_77266_[i++] = node7;
      }

      Node node8 = this.getNode(p_77267_.x + 1, p_77267_.y + 1, p_77267_.z);
      if (this.isOpen(node8) && this.hasMalus(node2) && this.hasMalus(node4)) {
         p_77266_[i++] = node8;
      }

      Node node9 = this.getNode(p_77267_.x, p_77267_.y + 1, p_77267_.z - 1);
      if (this.isOpen(node9) && this.hasMalus(node3) && this.hasMalus(node4)) {
         p_77266_[i++] = node9;
      }

      Node node10 = this.getNode(p_77267_.x, p_77267_.y - 1, p_77267_.z + 1);
      if (this.isOpen(node10) && this.hasMalus(node) && this.hasMalus(node5)) {
         p_77266_[i++] = node10;
      }

      Node node11 = this.getNode(p_77267_.x - 1, p_77267_.y - 1, p_77267_.z);
      if (this.isOpen(node11) && this.hasMalus(node1) && this.hasMalus(node5)) {
         p_77266_[i++] = node11;
      }

      Node node12 = this.getNode(p_77267_.x + 1, p_77267_.y - 1, p_77267_.z);
      if (this.isOpen(node12) && this.hasMalus(node2) && this.hasMalus(node5)) {
         p_77266_[i++] = node12;
      }

      Node node13 = this.getNode(p_77267_.x, p_77267_.y - 1, p_77267_.z - 1);
      if (this.isOpen(node13) && this.hasMalus(node3) && this.hasMalus(node5)) {
         p_77266_[i++] = node13;
      }

      Node node14 = this.getNode(p_77267_.x + 1, p_77267_.y, p_77267_.z - 1);
      if (this.isOpen(node14) && this.hasMalus(node3) && this.hasMalus(node2)) {
         p_77266_[i++] = node14;
      }

      Node node15 = this.getNode(p_77267_.x + 1, p_77267_.y, p_77267_.z + 1);
      if (this.isOpen(node15) && this.hasMalus(node) && this.hasMalus(node2)) {
         p_77266_[i++] = node15;
      }

      Node node16 = this.getNode(p_77267_.x - 1, p_77267_.y, p_77267_.z - 1);
      if (this.isOpen(node16) && this.hasMalus(node3) && this.hasMalus(node1)) {
         p_77266_[i++] = node16;
      }

      Node node17 = this.getNode(p_77267_.x - 1, p_77267_.y, p_77267_.z + 1);
      if (this.isOpen(node17) && this.hasMalus(node) && this.hasMalus(node1)) {
         p_77266_[i++] = node17;
      }

      Node node18 = this.getNode(p_77267_.x + 1, p_77267_.y + 1, p_77267_.z - 1);
      if (this.isOpen(node18) && this.hasMalus(node14) && this.hasMalus(node3) && this.hasMalus(node2) && this.hasMalus(node4) && this.hasMalus(node9) && this.hasMalus(node8)) {
         p_77266_[i++] = node18;
      }

      Node node19 = this.getNode(p_77267_.x + 1, p_77267_.y + 1, p_77267_.z + 1);
      if (this.isOpen(node19) && this.hasMalus(node15) && this.hasMalus(node) && this.hasMalus(node2) && this.hasMalus(node4) && this.hasMalus(node6) && this.hasMalus(node8)) {
         p_77266_[i++] = node19;
      }

      Node node20 = this.getNode(p_77267_.x - 1, p_77267_.y + 1, p_77267_.z - 1);
      if (this.isOpen(node20) && this.hasMalus(node16) && this.hasMalus(node3) && this.hasMalus(node1) && this.hasMalus(node4) && this.hasMalus(node9) && this.hasMalus(node7)) {
         p_77266_[i++] = node20;
      }

      Node node21 = this.getNode(p_77267_.x - 1, p_77267_.y + 1, p_77267_.z + 1);
      if (this.isOpen(node21) && this.hasMalus(node17) && this.hasMalus(node) && this.hasMalus(node1) && this.hasMalus(node4) && this.hasMalus(node6) && this.hasMalus(node7)) {
         p_77266_[i++] = node21;
      }

      Node node22 = this.getNode(p_77267_.x + 1, p_77267_.y - 1, p_77267_.z - 1);
      if (this.isOpen(node22) && this.hasMalus(node14) && this.hasMalus(node3) && this.hasMalus(node2) && this.hasMalus(node5) && this.hasMalus(node13) && this.hasMalus(node12)) {
         p_77266_[i++] = node22;
      }

      Node node23 = this.getNode(p_77267_.x + 1, p_77267_.y - 1, p_77267_.z + 1);
      if (this.isOpen(node23) && this.hasMalus(node15) && this.hasMalus(node) && this.hasMalus(node2) && this.hasMalus(node5) && this.hasMalus(node10) && this.hasMalus(node12)) {
         p_77266_[i++] = node23;
      }

      Node node24 = this.getNode(p_77267_.x - 1, p_77267_.y - 1, p_77267_.z - 1);
      if (this.isOpen(node24) && this.hasMalus(node16) && this.hasMalus(node3) && this.hasMalus(node1) && this.hasMalus(node5) && this.hasMalus(node13) && this.hasMalus(node11)) {
         p_77266_[i++] = node24;
      }

      Node node25 = this.getNode(p_77267_.x - 1, p_77267_.y - 1, p_77267_.z + 1);
      if (this.isOpen(node25) && this.hasMalus(node17) && this.hasMalus(node) && this.hasMalus(node1) && this.hasMalus(node5) && this.hasMalus(node10) && this.hasMalus(node11)) {
         p_77266_[i++] = node25;
      }

      return i;
   }

   private boolean hasMalus(@Nullable Node pNode) {
      return pNode != null && pNode.costMalus >= 0.0F;
   }

   private boolean isOpen(@Nullable Node pNode) {
      return pNode != null && !pNode.closed;
   }

   /**
    * Returns a mapped point or creates and adds one
    */
   @Nullable
   protected Node getNode(int pX, int pY, int pZ) {
      Node node = null;
      BlockPathTypes blockpathtypes = this.getCachedBlockPathType(pX, pY, pZ);
      float f = this.mob.getPathfindingMalus(blockpathtypes);
      if (f >= 0.0F) {
         node = super.getNode(pX, pY, pZ);
         if (node != null) {
            node.type = blockpathtypes;
            node.costMalus = Math.max(node.costMalus, f);
            if (blockpathtypes == BlockPathTypes.WALKABLE) {
               ++node.costMalus;
            }
         }
      }

      return node;
   }

   private BlockPathTypes getCachedBlockPathType(int pX, int pY, int pZ) {
      return this.pathTypeByPosCache.computeIfAbsent(BlockPos.asLong(pX, pY, pZ), (p_164692_) -> {
         return this.getBlockPathType(this.level, pX, pY, pZ, this.mob, this.entityWidth, this.entityHeight, this.entityDepth, this.canOpenDoors(), this.canPassDoors());
      });
   }

   /**
    * Returns the significant (e.g LAVA if the entity were half in lava) node type at the location taking the
    * surroundings and the entity size in account
    */
   public BlockPathTypes getBlockPathType(BlockGetter pBlockaccess, int pX, int pY, int pZ, Mob pEntityliving, int pXSize, int pYSize, int pZSize, boolean pCanBreakDoors, boolean pCanEnterDoors) {
      EnumSet<BlockPathTypes> enumset = EnumSet.noneOf(BlockPathTypes.class);
      BlockPathTypes blockpathtypes = BlockPathTypes.BLOCKED;
      BlockPos blockpos = pEntityliving.blockPosition();
      blockpathtypes = super.getBlockPathTypes(pBlockaccess, pX, pY, pZ, pXSize, pYSize, pZSize, pCanBreakDoors, pCanEnterDoors, enumset, blockpathtypes, blockpos);
      if (enumset.contains(BlockPathTypes.FENCE)) {
         return BlockPathTypes.FENCE;
      } else {
         BlockPathTypes blockpathtypes1 = BlockPathTypes.BLOCKED;

         for(BlockPathTypes blockpathtypes2 : enumset) {
            if (pEntityliving.getPathfindingMalus(blockpathtypes2) < 0.0F) {
               return blockpathtypes2;
            }

            if (pEntityliving.getPathfindingMalus(blockpathtypes2) >= pEntityliving.getPathfindingMalus(blockpathtypes1)) {
               blockpathtypes1 = blockpathtypes2;
            }
         }

         return blockpathtypes == BlockPathTypes.OPEN && pEntityliving.getPathfindingMalus(blockpathtypes1) == 0.0F ? BlockPathTypes.OPEN : blockpathtypes1;
      }
   }

   /**
    * Returns the node type at the specified postion taking the block below into account
    */
   public BlockPathTypes getBlockPathType(BlockGetter pLevel, int pX, int pY, int pZ) {
      BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
      BlockPathTypes blockpathtypes = getBlockPathTypeRaw(pLevel, blockpos$mutableblockpos.set(pX, pY, pZ));
      if (blockpathtypes == BlockPathTypes.OPEN && pY >= pLevel.getMinBuildHeight() + 1) {
         BlockPathTypes blockpathtypes1 = getBlockPathTypeRaw(pLevel, blockpos$mutableblockpos.set(pX, pY - 1, pZ));
         if (blockpathtypes1 != BlockPathTypes.DAMAGE_FIRE && blockpathtypes1 != BlockPathTypes.LAVA) {
            if (blockpathtypes1 == BlockPathTypes.DAMAGE_CACTUS) {
               blockpathtypes = BlockPathTypes.DAMAGE_CACTUS;
            } else if (blockpathtypes1 == BlockPathTypes.DAMAGE_OTHER) {
               blockpathtypes = BlockPathTypes.DAMAGE_OTHER;
            } else if (blockpathtypes1 == BlockPathTypes.COCOA) {
               blockpathtypes = BlockPathTypes.COCOA;
            } else if (blockpathtypes1 == BlockPathTypes.FENCE) {
               if (!blockpos$mutableblockpos.equals(this.mob.blockPosition())) {
                  blockpathtypes = BlockPathTypes.FENCE;
               }
            } else {
               blockpathtypes = blockpathtypes1 != BlockPathTypes.WALKABLE && blockpathtypes1 != BlockPathTypes.OPEN && blockpathtypes1 != BlockPathTypes.WATER ? BlockPathTypes.WALKABLE : BlockPathTypes.OPEN;
            }
         } else {
            blockpathtypes = BlockPathTypes.DAMAGE_FIRE;
         }
      }

      if (blockpathtypes == BlockPathTypes.WALKABLE || blockpathtypes == BlockPathTypes.OPEN) {
         blockpathtypes = checkNeighbourBlocks(pLevel, blockpos$mutableblockpos.set(pX, pY, pZ), blockpathtypes);
      }

      return blockpathtypes;
   }
}