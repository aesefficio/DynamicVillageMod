package net.minecraft.world.entity.ai.behavior;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class LongJumpToPreferredBlock<E extends Mob> extends LongJumpToRandomPos<E> {
   private final TagKey<Block> preferredBlockTag;
   private final float preferredBlocksChance;
   private final List<LongJumpToRandomPos.PossibleJump> notPrefferedJumpCandidates = new ArrayList<>();
   private boolean currentlyWantingPreferredOnes;

   public LongJumpToPreferredBlock(UniformInt p_217264_, int p_217265_, int p_217266_, float p_217267_, Function<E, SoundEvent> p_217268_, TagKey<Block> p_217269_, float p_217270_, Predicate<BlockState> p_217271_) {
      super(p_217264_, p_217265_, p_217266_, p_217267_, p_217268_, p_217271_);
      this.preferredBlockTag = p_217269_;
      this.preferredBlocksChance = p_217270_;
   }

   protected void start(ServerLevel p_217279_, E p_217280_, long p_217281_) {
      super.start(p_217279_, p_217280_, p_217281_);
      this.notPrefferedJumpCandidates.clear();
      this.currentlyWantingPreferredOnes = p_217280_.getRandom().nextFloat() < this.preferredBlocksChance;
   }

   protected Optional<LongJumpToRandomPos.PossibleJump> getJumpCandidate(ServerLevel p_217273_) {
      if (!this.currentlyWantingPreferredOnes) {
         return super.getJumpCandidate(p_217273_);
      } else {
         BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

         while(!this.jumpCandidates.isEmpty()) {
            Optional<LongJumpToRandomPos.PossibleJump> optional = super.getJumpCandidate(p_217273_);
            if (optional.isPresent()) {
               LongJumpToRandomPos.PossibleJump longjumptorandompos$possiblejump = optional.get();
               if (p_217273_.getBlockState(blockpos$mutableblockpos.setWithOffset(longjumptorandompos$possiblejump.getJumpTarget(), Direction.DOWN)).is(this.preferredBlockTag)) {
                  return optional;
               }

               this.notPrefferedJumpCandidates.add(longjumptorandompos$possiblejump);
            }
         }

         return !this.notPrefferedJumpCandidates.isEmpty() ? Optional.of(this.notPrefferedJumpCandidates.remove(0)) : Optional.empty();
      }
   }

   protected boolean isAcceptableLandingPosition(ServerLevel p_217283_, E p_217284_, BlockPos p_217285_) {
      return super.isAcceptableLandingPosition(p_217283_, p_217284_, p_217285_) && this.willNotLandInFluid(p_217283_, p_217285_);
   }

   private boolean willNotLandInFluid(ServerLevel p_217287_, BlockPos p_217288_) {
      return p_217287_.getFluidState(p_217288_).isEmpty() && p_217287_.getFluidState(p_217288_.below()).isEmpty();
   }
}