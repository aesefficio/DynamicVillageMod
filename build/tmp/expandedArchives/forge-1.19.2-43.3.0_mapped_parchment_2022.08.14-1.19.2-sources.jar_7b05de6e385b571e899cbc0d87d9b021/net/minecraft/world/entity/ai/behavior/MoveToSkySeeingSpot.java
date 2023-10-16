package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public class MoveToSkySeeingSpot extends Behavior<LivingEntity> {
   private final float speedModifier;

   public MoveToSkySeeingSpot(float pSpeedModifier) {
      super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT));
      this.speedModifier = pSpeedModifier;
   }

   protected void start(ServerLevel pLevel, LivingEntity pEntity, long pGameTime) {
      Optional<Vec3> optional = Optional.ofNullable(this.getOutdoorPosition(pLevel, pEntity));
      if (optional.isPresent()) {
         pEntity.getBrain().setMemory(MemoryModuleType.WALK_TARGET, optional.map((p_23563_) -> {
            return new WalkTarget(p_23563_, this.speedModifier, 0);
         }));
      }

   }

   protected boolean checkExtraStartConditions(ServerLevel pLevel, LivingEntity pOwner) {
      return !pLevel.canSeeSky(pOwner.blockPosition());
   }

   @Nullable
   private Vec3 getOutdoorPosition(ServerLevel pLevel, LivingEntity pWalker) {
      RandomSource randomsource = pWalker.getRandom();
      BlockPos blockpos = pWalker.blockPosition();

      for(int i = 0; i < 10; ++i) {
         BlockPos blockpos1 = blockpos.offset(randomsource.nextInt(20) - 10, randomsource.nextInt(6) - 3, randomsource.nextInt(20) - 10);
         if (hasNoBlocksAbove(pLevel, pWalker, blockpos1)) {
            return Vec3.atBottomCenterOf(blockpos1);
         }
      }

      return null;
   }

   public static boolean hasNoBlocksAbove(ServerLevel p_23559_, LivingEntity p_23560_, BlockPos p_23561_) {
      return p_23559_.canSeeSky(p_23561_) && (double)p_23559_.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, p_23561_).getY() <= p_23560_.getY();
   }
}