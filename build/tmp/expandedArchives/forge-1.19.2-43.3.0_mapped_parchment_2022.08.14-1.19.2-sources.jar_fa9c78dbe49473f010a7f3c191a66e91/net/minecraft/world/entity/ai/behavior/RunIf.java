package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class RunIf<E extends LivingEntity> extends Behavior<E> {
   private final Predicate<E> predicate;
   private final Behavior<? super E> wrappedBehavior;
   private final boolean checkWhileRunningAlso;

   public RunIf(Map<MemoryModuleType<?>, MemoryStatus> pBehaviorConditions, Predicate<E> pPredicate, Behavior<? super E> pWrappedBehavior, boolean pCheckWhileRunningAlso) {
      super(mergeMaps(pBehaviorConditions, pWrappedBehavior.entryCondition));
      this.predicate = pPredicate;
      this.wrappedBehavior = pWrappedBehavior;
      this.checkWhileRunningAlso = pCheckWhileRunningAlso;
   }

   private static Map<MemoryModuleType<?>, MemoryStatus> mergeMaps(Map<MemoryModuleType<?>, MemoryStatus> pMap, Map<MemoryModuleType<?>, MemoryStatus> pOther) {
      Map<MemoryModuleType<?>, MemoryStatus> map = Maps.newHashMap();
      map.putAll(pMap);
      map.putAll(pOther);
      return map;
   }

   public RunIf(Predicate<E> pPredicate, Behavior<? super E> pWrappedBehavior, boolean pCheckWhileRunningAlso) {
      this(ImmutableMap.of(), pPredicate, pWrappedBehavior, pCheckWhileRunningAlso);
   }

   public RunIf(Predicate<E> pPredicate, Behavior<? super E> pWrappedBehavior) {
      this(ImmutableMap.of(), pPredicate, pWrappedBehavior, false);
   }

   public RunIf(Map<MemoryModuleType<?>, MemoryStatus> pEntryConditions, Behavior<? super E> pWrappedBehavior) {
      this(pEntryConditions, (p_147872_) -> {
         return true;
      }, pWrappedBehavior, false);
   }

   protected boolean checkExtraStartConditions(ServerLevel pLevel, E pOwner) {
      return this.predicate.test(pOwner) && this.wrappedBehavior.checkExtraStartConditions(pLevel, pOwner);
   }

   protected boolean canStillUse(ServerLevel pLevel, E pEntity, long pGameTime) {
      return this.checkWhileRunningAlso && this.predicate.test(pEntity) && this.wrappedBehavior.canStillUse(pLevel, pEntity, pGameTime);
   }

   protected boolean timedOut(long pGameTime) {
      return false;
   }

   protected void start(ServerLevel pLevel, E pEntity, long pGameTime) {
      this.wrappedBehavior.start(pLevel, pEntity, pGameTime);
   }

   protected void tick(ServerLevel pLevel, E pOwner, long pGameTime) {
      this.wrappedBehavior.tick(pLevel, pOwner, pGameTime);
   }

   protected void stop(ServerLevel pLevel, E pEntity, long pGameTime) {
      this.wrappedBehavior.stop(pLevel, pEntity, pGameTime);
   }

   public String toString() {
      return "RunIf: " + this.wrappedBehavior;
   }
}