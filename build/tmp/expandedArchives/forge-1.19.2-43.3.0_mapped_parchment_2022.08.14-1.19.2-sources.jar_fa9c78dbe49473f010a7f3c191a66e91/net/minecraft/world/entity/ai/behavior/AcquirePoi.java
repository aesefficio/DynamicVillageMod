package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.pathfinder.Path;

public class AcquirePoi extends Behavior<PathfinderMob> {
   private static final int BATCH_SIZE = 5;
   private static final int RATE = 20;
   public static final int SCAN_RANGE = 48;
   private final Predicate<Holder<PoiType>> poiType;
   private final MemoryModuleType<GlobalPos> memoryToAcquire;
   private final boolean onlyIfAdult;
   private final Optional<Byte> onPoiAcquisitionEvent;
   private long nextScheduledStart;
   private final Long2ObjectMap<AcquirePoi.JitteredLinearRetry> batchCache = new Long2ObjectOpenHashMap<>();

   public AcquirePoi(Predicate<Holder<PoiType>> pPoiType, MemoryModuleType<GlobalPos> pMemoryKey, MemoryModuleType<GlobalPos> pMemoryToAcquire, boolean pOnlyIfAdult, Optional<Byte> pOnPoiAcquisitionEvent) {
      super(constructEntryConditionMap(pMemoryKey, pMemoryToAcquire));
      this.poiType = pPoiType;
      this.memoryToAcquire = pMemoryToAcquire;
      this.onlyIfAdult = pOnlyIfAdult;
      this.onPoiAcquisitionEvent = pOnPoiAcquisitionEvent;
   }

   public AcquirePoi(Predicate<Holder<PoiType>> pPoiType, MemoryModuleType<GlobalPos> pMemoryToAcquire, boolean pOnlyIfAdult, Optional<Byte> pOnPoiAcquisitionEvent) {
      this(pPoiType, pMemoryToAcquire, pMemoryToAcquire, pOnlyIfAdult, pOnPoiAcquisitionEvent);
   }

   private static ImmutableMap<MemoryModuleType<?>, MemoryStatus> constructEntryConditionMap(MemoryModuleType<GlobalPos> pMemoryKey, MemoryModuleType<GlobalPos> pMemoryToAcquire) {
      ImmutableMap.Builder<MemoryModuleType<?>, MemoryStatus> builder = ImmutableMap.builder();
      builder.put(pMemoryKey, MemoryStatus.VALUE_ABSENT);
      if (pMemoryToAcquire != pMemoryKey) {
         builder.put(pMemoryToAcquire, MemoryStatus.VALUE_ABSENT);
      }

      return builder.build();
   }

   protected boolean checkExtraStartConditions(ServerLevel pLevel, PathfinderMob pOwner) {
      if (this.onlyIfAdult && pOwner.isBaby()) {
         return false;
      } else if (this.nextScheduledStart == 0L) {
         this.nextScheduledStart = pOwner.level.getGameTime() + (long)pLevel.random.nextInt(20);
         return false;
      } else {
         return pLevel.getGameTime() >= this.nextScheduledStart;
      }
   }

   protected void start(ServerLevel pLevel, PathfinderMob pEntity, long pGameTime) {
      this.nextScheduledStart = pGameTime + 20L + (long)pLevel.getRandom().nextInt(20);
      PoiManager poimanager = pLevel.getPoiManager();
      this.batchCache.long2ObjectEntrySet().removeIf((p_22338_) -> {
         return !p_22338_.getValue().isStillValid(pGameTime);
      });
      Predicate<BlockPos> predicate = (p_22335_) -> {
         AcquirePoi.JitteredLinearRetry acquirepoi$jitteredlinearretry = this.batchCache.get(p_22335_.asLong());
         if (acquirepoi$jitteredlinearretry == null) {
            return true;
         } else if (!acquirepoi$jitteredlinearretry.shouldRetry(pGameTime)) {
            return false;
         } else {
            acquirepoi$jitteredlinearretry.markAttempt(pGameTime);
            return true;
         }
      };
      Set<Pair<Holder<PoiType>, BlockPos>> set = poimanager.findAllClosestFirstWithType(this.poiType, predicate, pEntity.blockPosition(), 48, PoiManager.Occupancy.HAS_SPACE).limit(5L).collect(Collectors.toSet());
      Path path = findPathToPois(pEntity, set);
      if (path != null && path.canReach()) {
         BlockPos blockpos = path.getTarget();
         poimanager.getType(blockpos).ifPresent((p_217105_) -> {
            poimanager.take(this.poiType, (p_217108_, p_217109_) -> {
               return p_217109_.equals(blockpos);
            }, blockpos, 1);
            pEntity.getBrain().setMemory(this.memoryToAcquire, GlobalPos.of(pLevel.dimension(), blockpos));
            this.onPoiAcquisitionEvent.ifPresent((p_147369_) -> {
               pLevel.broadcastEntityEvent(pEntity, p_147369_);
            });
            this.batchCache.clear();
            DebugPackets.sendPoiTicketCountPacket(pLevel, blockpos);
         });
      } else {
         for(Pair<Holder<PoiType>, BlockPos> pair : set) {
            this.batchCache.computeIfAbsent(pair.getSecond().asLong(), (p_22360_) -> {
               return new AcquirePoi.JitteredLinearRetry(pEntity.level.random, pGameTime);
            });
         }
      }

   }

   @Nullable
   public static Path findPathToPois(Mob p_217098_, Set<Pair<Holder<PoiType>, BlockPos>> p_217099_) {
      if (p_217099_.isEmpty()) {
         return null;
      } else {
         Set<BlockPos> set = new HashSet<>();
         int i = 1;

         for(Pair<Holder<PoiType>, BlockPos> pair : p_217099_) {
            i = Math.max(i, pair.getFirst().value().validRange());
            set.add(pair.getSecond());
         }

         return p_217098_.getNavigation().createPath(set, i);
      }
   }

   static class JitteredLinearRetry {
      private static final int MIN_INTERVAL_INCREASE = 40;
      private static final int MAX_INTERVAL_INCREASE = 80;
      private static final int MAX_RETRY_PATHFINDING_INTERVAL = 400;
      private final RandomSource random;
      private long previousAttemptTimestamp;
      private long nextScheduledAttemptTimestamp;
      private int currentDelay;

      JitteredLinearRetry(RandomSource pRandom, long pTimestamp) {
         this.random = pRandom;
         this.markAttempt(pTimestamp);
      }

      public void markAttempt(long pTimestamp) {
         this.previousAttemptTimestamp = pTimestamp;
         int i = this.currentDelay + this.random.nextInt(40) + 40;
         this.currentDelay = Math.min(i, 400);
         this.nextScheduledAttemptTimestamp = pTimestamp + (long)this.currentDelay;
      }

      public boolean isStillValid(long pTimestamp) {
         return pTimestamp - this.previousAttemptTimestamp < 400L;
      }

      public boolean shouldRetry(long pTimestamp) {
         return pTimestamp >= this.nextScheduledAttemptTimestamp;
      }

      public String toString() {
         return "RetryMarker{, previousAttemptAt=" + this.previousAttemptTimestamp + ", nextScheduledAttemptAt=" + this.nextScheduledAttemptTimestamp + ", currentDelay=" + this.currentDelay + "}";
      }
   }
}