package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

public class PlayTagWithOtherKids extends Behavior<PathfinderMob> {
   private static final int MAX_FLEE_XZ_DIST = 20;
   private static final int MAX_FLEE_Y_DIST = 8;
   private static final float FLEE_SPEED_MODIFIER = 0.6F;
   private static final float CHASE_SPEED_MODIFIER = 0.6F;
   private static final int MAX_CHASERS_PER_TARGET = 5;
   private static final int AVERAGE_WAIT_TIME_BETWEEN_RUNS = 10;

   public PlayTagWithOtherKids() {
      super(ImmutableMap.of(MemoryModuleType.VISIBLE_VILLAGER_BABIES, MemoryStatus.VALUE_PRESENT, MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.INTERACTION_TARGET, MemoryStatus.REGISTERED));
   }

   protected boolean checkExtraStartConditions(ServerLevel pLevel, PathfinderMob pOwner) {
      return pLevel.getRandom().nextInt(10) == 0 && this.hasFriendsNearby(pOwner);
   }

   protected void start(ServerLevel pLevel, PathfinderMob pEntity, long pGameTime) {
      LivingEntity livingentity = this.seeIfSomeoneIsChasingMe(pEntity);
      if (livingentity != null) {
         this.fleeFromChaser(pLevel, pEntity, livingentity);
      } else {
         Optional<LivingEntity> optional = this.findSomeoneBeingChased(pEntity);
         if (optional.isPresent()) {
            chaseKid(pEntity, optional.get());
         } else {
            this.findSomeoneToChase(pEntity).ifPresent((p_23666_) -> {
               chaseKid(pEntity, p_23666_);
            });
         }
      }
   }

   private void fleeFromChaser(ServerLevel pLevel, PathfinderMob pPathfinder, LivingEntity pEntity) {
      for(int i = 0; i < 10; ++i) {
         Vec3 vec3 = LandRandomPos.getPos(pPathfinder, 20, 8);
         if (vec3 != null && pLevel.isVillage(new BlockPos(vec3))) {
            pPathfinder.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(vec3, 0.6F, 0));
            return;
         }
      }

   }

   private static void chaseKid(PathfinderMob pEntity, LivingEntity pInteractionTarget) {
      Brain<?> brain = pEntity.getBrain();
      brain.setMemory(MemoryModuleType.INTERACTION_TARGET, pInteractionTarget);
      brain.setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(pInteractionTarget, true));
      brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(new EntityTracker(pInteractionTarget, false), 0.6F, 1));
   }

   private Optional<LivingEntity> findSomeoneToChase(PathfinderMob pPathfinder) {
      return this.getFriendsNearby(pPathfinder).stream().findAny();
   }

   private Optional<LivingEntity> findSomeoneBeingChased(PathfinderMob pPathfinder) {
      Map<LivingEntity, Integer> map = this.checkHowManyChasersEachFriendHas(pPathfinder);
      return map.entrySet().stream().sorted(Comparator.comparingInt(Map.Entry::getValue)).filter((p_23653_) -> {
         return p_23653_.getValue() > 0 && p_23653_.getValue() <= 5;
      }).map(Map.Entry::getKey).findFirst();
   }

   private Map<LivingEntity, Integer> checkHowManyChasersEachFriendHas(PathfinderMob pPathfinder) {
      Map<LivingEntity, Integer> map = Maps.newHashMap();
      this.getFriendsNearby(pPathfinder).stream().filter(this::isChasingSomeone).forEach((p_23656_) -> {
         map.compute(this.whoAreYouChasing(p_23656_), (p_147707_, p_147708_) -> {
            return p_147708_ == null ? 1 : p_147708_ + 1;
         });
      });
      return map;
   }

   private List<LivingEntity> getFriendsNearby(PathfinderMob pPathfinder) {
      return pPathfinder.getBrain().getMemory(MemoryModuleType.VISIBLE_VILLAGER_BABIES).get();
   }

   private LivingEntity whoAreYouChasing(LivingEntity pEntity) {
      return pEntity.getBrain().getMemory(MemoryModuleType.INTERACTION_TARGET).get();
   }

   @Nullable
   private LivingEntity seeIfSomeoneIsChasingMe(LivingEntity pEntity) {
      return pEntity.getBrain().getMemory(MemoryModuleType.VISIBLE_VILLAGER_BABIES).get().stream().filter((p_23671_) -> {
         return this.isFriendChasingMe(pEntity, p_23671_);
      }).findAny().orElse((LivingEntity)null);
   }

   private boolean isChasingSomeone(LivingEntity p_23668_) {
      return p_23668_.getBrain().getMemory(MemoryModuleType.INTERACTION_TARGET).isPresent();
   }

   private boolean isFriendChasingMe(LivingEntity pEntity, LivingEntity pChaser) {
      return pChaser.getBrain().getMemory(MemoryModuleType.INTERACTION_TARGET).filter((p_23661_) -> {
         return p_23661_ == pEntity;
      }).isPresent();
   }

   private boolean hasFriendsNearby(PathfinderMob pPathfinder) {
      return pPathfinder.getBrain().hasMemoryValue(MemoryModuleType.VISIBLE_VILLAGER_BABIES);
   }
}