package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;

public class ValidateNearbyPoi extends Behavior<LivingEntity> {
   private static final int MAX_DISTANCE = 16;
   private final MemoryModuleType<GlobalPos> memoryType;
   private final Predicate<Holder<PoiType>> poiPredicate;

   public ValidateNearbyPoi(Predicate<Holder<PoiType>> pPoiPredicate, MemoryModuleType<GlobalPos> pMemoryType) {
      super(ImmutableMap.of(pMemoryType, MemoryStatus.VALUE_PRESENT));
      this.poiPredicate = pPoiPredicate;
      this.memoryType = pMemoryType;
   }

   protected boolean checkExtraStartConditions(ServerLevel pLevel, LivingEntity pOwner) {
      GlobalPos globalpos = pOwner.getBrain().getMemory(this.memoryType).get();
      return pLevel.dimension() == globalpos.dimension() && globalpos.pos().closerToCenterThan(pOwner.position(), 16.0D);
   }

   protected void start(ServerLevel pLevel, LivingEntity pEntity, long pGameTime) {
      Brain<?> brain = pEntity.getBrain();
      GlobalPos globalpos = brain.getMemory(this.memoryType).get();
      BlockPos blockpos = globalpos.pos();
      ServerLevel serverlevel = pLevel.getServer().getLevel(globalpos.dimension());
      if (serverlevel != null && !this.poiDoesntExist(serverlevel, blockpos)) {
         if (this.bedIsOccupied(serverlevel, blockpos, pEntity)) {
            brain.eraseMemory(this.memoryType);
            pLevel.getPoiManager().release(blockpos);
            DebugPackets.sendPoiTicketCountPacket(pLevel, blockpos);
         }
      } else {
         brain.eraseMemory(this.memoryType);
      }

   }

   private boolean bedIsOccupied(ServerLevel pLevel, BlockPos pPos, LivingEntity pSleeper) {
      BlockState blockstate = pLevel.getBlockState(pPos);
      return blockstate.is(BlockTags.BEDS) && blockstate.getValue(BedBlock.OCCUPIED) && !pSleeper.isSleeping();
   }

   private boolean poiDoesntExist(ServerLevel pLevel, BlockPos pPos) {
      return !pLevel.getPoiManager().exists(pPos, this.poiPredicate);
   }
}