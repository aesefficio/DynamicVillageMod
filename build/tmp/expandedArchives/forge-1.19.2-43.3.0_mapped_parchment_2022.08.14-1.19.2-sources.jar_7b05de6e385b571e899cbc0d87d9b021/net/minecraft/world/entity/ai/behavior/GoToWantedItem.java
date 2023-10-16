package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.item.ItemEntity;

public class GoToWantedItem<E extends LivingEntity> extends Behavior<E> {
   private final Predicate<E> predicate;
   private final int maxDistToWalk;
   private final float speedModifier;

   public GoToWantedItem(float pSpeedModifier, boolean pHasTarget, int pMaxDistToWalk) {
      this((p_23158_) -> {
         return true;
      }, pSpeedModifier, pHasTarget, pMaxDistToWalk);
   }

   public GoToWantedItem(Predicate<E> pPredicate, float pSpeedModifier, boolean pHasTarget, int pMaxDistToWalk) {
      super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.WALK_TARGET, pHasTarget ? MemoryStatus.REGISTERED : MemoryStatus.VALUE_ABSENT, MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM, MemoryStatus.VALUE_PRESENT));
      this.predicate = pPredicate;
      this.maxDistToWalk = pMaxDistToWalk;
      this.speedModifier = pSpeedModifier;
   }

   protected boolean checkExtraStartConditions(ServerLevel pLevel, E pOwner) {
      return !this.isOnPickupCooldown(pOwner) && this.predicate.test(pOwner) && this.getClosestLovedItem(pOwner).closerThan(pOwner, (double)this.maxDistToWalk);
   }

   protected void start(ServerLevel pLevel, E pEntity, long pGameTime) {
      BehaviorUtils.setWalkAndLookTargetMemories(pEntity, this.getClosestLovedItem(pEntity), this.speedModifier, 0);
   }

   private boolean isOnPickupCooldown(E p_217254_) {
      return p_217254_.getBrain().checkMemory(MemoryModuleType.ITEM_PICKUP_COOLDOWN_TICKS, MemoryStatus.VALUE_PRESENT);
   }

   private ItemEntity getClosestLovedItem(E p_23156_) {
      return p_23156_.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM).get();
   }
}