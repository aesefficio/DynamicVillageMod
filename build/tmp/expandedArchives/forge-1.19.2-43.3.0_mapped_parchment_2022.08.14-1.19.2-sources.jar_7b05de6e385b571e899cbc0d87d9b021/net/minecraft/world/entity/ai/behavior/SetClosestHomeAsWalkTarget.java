package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.pathfinder.Path;

public class SetClosestHomeAsWalkTarget extends Behavior<LivingEntity> {
   private static final int CACHE_TIMEOUT = 40;
   private static final int BATCH_SIZE = 5;
   private static final int RATE = 20;
   private static final int OK_DISTANCE_SQR = 4;
   private final float speedModifier;
   private final Long2LongMap batchCache = new Long2LongOpenHashMap();
   private int triedCount;
   private long lastUpdate;

   public SetClosestHomeAsWalkTarget(float pSpeedModifier) {
      super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.HOME, MemoryStatus.VALUE_ABSENT));
      this.speedModifier = pSpeedModifier;
   }

   protected boolean checkExtraStartConditions(ServerLevel pLevel, LivingEntity pOwner) {
      if (pLevel.getGameTime() - this.lastUpdate < 20L) {
         return false;
      } else {
         PathfinderMob pathfindermob = (PathfinderMob)pOwner;
         PoiManager poimanager = pLevel.getPoiManager();
         Optional<BlockPos> optional = poimanager.findClosest((p_217376_) -> {
            return p_217376_.is(PoiTypes.HOME);
         }, pOwner.blockPosition(), 48, PoiManager.Occupancy.ANY);
         return optional.isPresent() && !(optional.get().distSqr(pathfindermob.blockPosition()) <= 4.0D);
      }
   }

   protected void start(ServerLevel pLevel, LivingEntity pEntity, long pGameTime) {
      this.triedCount = 0;
      this.lastUpdate = pLevel.getGameTime() + (long)pLevel.getRandom().nextInt(20);
      PathfinderMob pathfindermob = (PathfinderMob)pEntity;
      PoiManager poimanager = pLevel.getPoiManager();
      Predicate<BlockPos> predicate = (p_217370_) -> {
         long i = p_217370_.asLong();
         if (this.batchCache.containsKey(i)) {
            return false;
         } else if (++this.triedCount >= 5) {
            return false;
         } else {
            this.batchCache.put(i, this.lastUpdate + 40L);
            return true;
         }
      };
      Set<Pair<Holder<PoiType>, BlockPos>> set = poimanager.findAllWithType((p_217372_) -> {
         return p_217372_.is(PoiTypes.HOME);
      }, predicate, pEntity.blockPosition(), 48, PoiManager.Occupancy.ANY).collect(Collectors.toSet());
      Path path = AcquirePoi.findPathToPois(pathfindermob, set);
      if (path != null && path.canReach()) {
         BlockPos blockpos = path.getTarget();
         Optional<Holder<PoiType>> optional = poimanager.getType(blockpos);
         if (optional.isPresent()) {
            pEntity.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(blockpos, this.speedModifier, 1));
            DebugPackets.sendPoiTicketCountPacket(pLevel, blockpos);
         }
      } else if (this.triedCount < 5) {
         this.batchCache.long2LongEntrySet().removeIf((p_217374_) -> {
            return p_217374_.getLongValue() < this.lastUpdate;
         });
      }

   }
}