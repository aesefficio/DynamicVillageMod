package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;

public class LocateHidingPlace extends Behavior<LivingEntity> {
   private final float speedModifier;
   private final int radius;
   private final int closeEnoughDist;
   private Optional<BlockPos> currentPos = Optional.empty();

   public LocateHidingPlace(int pRadius, float pSpeedModifier, int pCloseEnoughDist) {
      super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.HOME, MemoryStatus.REGISTERED, MemoryModuleType.HIDING_PLACE, MemoryStatus.REGISTERED));
      this.radius = pRadius;
      this.speedModifier = pSpeedModifier;
      this.closeEnoughDist = pCloseEnoughDist;
   }

   protected boolean checkExtraStartConditions(ServerLevel pLevel, LivingEntity pOwner) {
      Optional<BlockPos> optional = pLevel.getPoiManager().find((p_217258_) -> {
         return p_217258_.is(PoiTypes.HOME);
      }, (p_23425_) -> {
         return true;
      }, pOwner.blockPosition(), this.closeEnoughDist + 1, PoiManager.Occupancy.ANY);
      if (optional.isPresent() && optional.get().closerToCenterThan(pOwner.position(), (double)this.closeEnoughDist)) {
         this.currentPos = optional;
      } else {
         this.currentPos = Optional.empty();
      }

      return true;
   }

   protected void start(ServerLevel pLevel, LivingEntity pEntity, long pGameTime) {
      Brain<?> brain = pEntity.getBrain();
      Optional<BlockPos> optional = this.currentPos;
      if (optional.isEmpty()) {
         optional = pLevel.getPoiManager().getRandom((p_217256_) -> {
            return p_217256_.is(PoiTypes.HOME);
         }, (p_23421_) -> {
            return true;
         }, PoiManager.Occupancy.ANY, pEntity.blockPosition(), this.radius, pEntity.getRandom());
         if (optional.isEmpty()) {
            Optional<GlobalPos> optional1 = brain.getMemory(MemoryModuleType.HOME);
            if (optional1.isPresent()) {
               optional = Optional.of(optional1.get().pos());
            }
         }
      }

      if (optional.isPresent()) {
         brain.eraseMemory(MemoryModuleType.PATH);
         brain.eraseMemory(MemoryModuleType.LOOK_TARGET);
         brain.eraseMemory(MemoryModuleType.BREED_TARGET);
         brain.eraseMemory(MemoryModuleType.INTERACTION_TARGET);
         brain.setMemory(MemoryModuleType.HIDING_PLACE, GlobalPos.of(pLevel.dimension(), optional.get()));
         if (!optional.get().closerToCenterThan(pEntity.position(), (double)this.closeEnoughDist)) {
            brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(optional.get(), this.speedModifier, this.closeEnoughDist));
         }
      }

   }
}