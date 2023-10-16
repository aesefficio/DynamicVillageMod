package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class BecomePassiveIfMemoryPresent extends Behavior<LivingEntity> {
   private final int pacifyDuration;

   public BecomePassiveIfMemoryPresent(MemoryModuleType<?> pModuleType, int pPacifyDuration) {
      super(ImmutableMap.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.PACIFIED, MemoryStatus.VALUE_ABSENT, pModuleType, MemoryStatus.VALUE_PRESENT));
      this.pacifyDuration = pPacifyDuration;
   }

   protected void start(ServerLevel pLevel, LivingEntity pEntity, long pGameTime) {
      pEntity.getBrain().setMemoryWithExpiry(MemoryModuleType.PACIFIED, true, (long)this.pacifyDuration);
      pEntity.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
   }
}