package net.minecraft.world.entity.monster.piglin;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.item.ItemEntity;

public class StopAdmiringIfItemTooFarAway<E extends Piglin> extends Behavior<E> {
   private final int maxDistanceToItem;

   public StopAdmiringIfItemTooFarAway(int pMaxDistanceToItem) {
      super(ImmutableMap.of(MemoryModuleType.ADMIRING_ITEM, MemoryStatus.VALUE_PRESENT, MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM, MemoryStatus.REGISTERED));
      this.maxDistanceToItem = pMaxDistanceToItem;
   }

   protected boolean checkExtraStartConditions(ServerLevel pLevel, E pOwner) {
      if (!pOwner.getOffhandItem().isEmpty()) {
         return false;
      } else {
         Optional<ItemEntity> optional = pOwner.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM);
         if (!optional.isPresent()) {
            return true;
         } else {
            return !optional.get().closerThan(pOwner, (double)this.maxDistanceToItem);
         }
      }
   }

   protected void start(ServerLevel pLevel, E pEntity, long pGameTime) {
      pEntity.getBrain().eraseMemory(MemoryModuleType.ADMIRING_ITEM);
   }
}