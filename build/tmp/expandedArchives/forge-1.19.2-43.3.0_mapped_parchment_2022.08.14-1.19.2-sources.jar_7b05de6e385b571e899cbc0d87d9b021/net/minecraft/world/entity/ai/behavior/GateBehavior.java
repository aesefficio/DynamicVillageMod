package net.minecraft.world.entity.ai.behavior;

import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class GateBehavior<E extends LivingEntity> extends Behavior<E> {
   private final Set<MemoryModuleType<?>> exitErasedMemories;
   private final GateBehavior.OrderPolicy orderPolicy;
   private final GateBehavior.RunningPolicy runningPolicy;
   private final ShufflingList<Behavior<? super E>> behaviors = new ShufflingList<>();

   public GateBehavior(Map<MemoryModuleType<?>, MemoryStatus> pEntryCondition, Set<MemoryModuleType<?>> pExitErasedMemories, GateBehavior.OrderPolicy pOrderPolicy, GateBehavior.RunningPolicy pRunningPolicy, List<Pair<Behavior<? super E>, Integer>> pDurations) {
      super(pEntryCondition);
      this.exitErasedMemories = pExitErasedMemories;
      this.orderPolicy = pOrderPolicy;
      this.runningPolicy = pRunningPolicy;
      pDurations.forEach((p_22892_) -> {
         this.behaviors.add(p_22892_.getFirst(), p_22892_.getSecond());
      });
   }

   protected boolean canStillUse(ServerLevel pLevel, E pEntity, long pGameTime) {
      return this.behaviors.stream().filter((p_22920_) -> {
         return p_22920_.getStatus() == Behavior.Status.RUNNING;
      }).anyMatch((p_22912_) -> {
         return p_22912_.canStillUse(pLevel, pEntity, pGameTime);
      });
   }

   protected boolean timedOut(long pGameTime) {
      return false;
   }

   protected void start(ServerLevel pLevel, E pEntity, long pGameTime) {
      this.orderPolicy.apply(this.behaviors);
      this.runningPolicy.apply(this.behaviors.stream(), pLevel, pEntity, pGameTime);
   }

   protected void tick(ServerLevel pLevel, E pOwner, long pGameTime) {
      this.behaviors.stream().filter((p_22914_) -> {
         return p_22914_.getStatus() == Behavior.Status.RUNNING;
      }).forEach((p_22901_) -> {
         p_22901_.tickOrStop(pLevel, pOwner, pGameTime);
      });
   }

   protected void stop(ServerLevel pLevel, E pEntity, long pGameTime) {
      this.behaviors.stream().filter((p_22903_) -> {
         return p_22903_.getStatus() == Behavior.Status.RUNNING;
      }).forEach((p_22888_) -> {
         p_22888_.doStop(pLevel, pEntity, pGameTime);
      });
      this.exitErasedMemories.forEach(pEntity.getBrain()::eraseMemory);
   }

   public String toString() {
      Set<? extends Behavior<? super E>> set = this.behaviors.stream().filter((p_22890_) -> {
         return p_22890_.getStatus() == Behavior.Status.RUNNING;
      }).collect(Collectors.toSet());
      return "(" + this.getClass().getSimpleName() + "): " + set;
   }

   public static enum OrderPolicy {
      ORDERED((p_147530_) -> {
      }),
      SHUFFLED(ShufflingList::shuffle);

      private final Consumer<ShufflingList<?>> consumer;

      private OrderPolicy(Consumer<ShufflingList<?>> pConsumer) {
         this.consumer = pConsumer;
      }

      public void apply(ShufflingList<?> pList) {
         this.consumer.accept(pList);
      }
   }

   public static enum RunningPolicy {
      RUN_ONE {
         public <E extends LivingEntity> void apply(Stream<Behavior<? super E>> p_147537_, ServerLevel p_147538_, E p_147539_, long p_147540_) {
            p_147537_.filter((p_22965_) -> {
               return p_22965_.getStatus() == Behavior.Status.STOPPED;
            }).filter((p_22963_) -> {
               return p_22963_.tryStart(p_147538_, p_147539_, p_147540_);
            }).findFirst();
         }
      },
      TRY_ALL {
         public <E extends LivingEntity> void apply(Stream<Behavior<? super E>> p_147542_, ServerLevel p_147543_, E p_147544_, long p_147545_) {
            p_147542_.filter((p_22980_) -> {
               return p_22980_.getStatus() == Behavior.Status.STOPPED;
            }).forEach((p_22978_) -> {
               p_22978_.tryStart(p_147543_, p_147544_, p_147545_);
            });
         }
      };

      public abstract <E extends LivingEntity> void apply(Stream<Behavior<? super E>> pBehaviors, ServerLevel pLevel, E pOwner, long pGameTime);
   }
}