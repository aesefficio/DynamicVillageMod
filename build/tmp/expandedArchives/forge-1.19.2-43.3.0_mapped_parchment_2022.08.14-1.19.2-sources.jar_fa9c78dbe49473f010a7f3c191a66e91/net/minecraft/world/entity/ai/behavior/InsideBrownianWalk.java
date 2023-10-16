package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public class InsideBrownianWalk extends Behavior<PathfinderMob> {
   private final float speedModifier;

   public InsideBrownianWalk(float pSpeedModifier) {
      super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT));
      this.speedModifier = pSpeedModifier;
   }

   protected boolean checkExtraStartConditions(ServerLevel pLevel, PathfinderMob pOwner) {
      return !pLevel.canSeeSky(pOwner.blockPosition());
   }

   protected void start(ServerLevel pLevel, PathfinderMob pEntity, long pGameTime) {
      BlockPos blockpos = pEntity.blockPosition();
      List<BlockPos> list = BlockPos.betweenClosedStream(blockpos.offset(-1, -1, -1), blockpos.offset(1, 1, 1)).map(BlockPos::immutable).collect(Collectors.toList());
      Collections.shuffle(list);
      Optional<BlockPos> optional = list.stream().filter((p_23230_) -> {
         return !pLevel.canSeeSky(p_23230_);
      }).filter((p_23237_) -> {
         return pLevel.loadedAndEntityCanStandOn(p_23237_, pEntity);
      }).filter((p_23227_) -> {
         return pLevel.noCollision(pEntity);
      }).findFirst();
      optional.ifPresent((p_23233_) -> {
         pEntity.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(p_23233_, this.speedModifier, 0));
      });
   }
}