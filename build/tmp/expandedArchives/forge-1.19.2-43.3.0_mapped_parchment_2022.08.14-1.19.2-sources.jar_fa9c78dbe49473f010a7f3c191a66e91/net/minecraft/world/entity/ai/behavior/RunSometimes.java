package net.minecraft.world.entity.ai.behavior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.LivingEntity;

public class RunSometimes<E extends LivingEntity> extends Behavior<E> {
   private boolean resetTicks;
   private boolean wasRunning;
   private final UniformInt interval;
   private final Behavior<? super E> wrappedBehavior;
   private int ticksUntilNextStart;

   public RunSometimes(Behavior<? super E> pWrappedBehavior, UniformInt pInterval) {
      this(pWrappedBehavior, false, pInterval);
   }

   public RunSometimes(Behavior<? super E> pWrappedBehavior, boolean pKeepTicks, UniformInt pInterval) {
      super(pWrappedBehavior.entryCondition);
      this.wrappedBehavior = pWrappedBehavior;
      this.resetTicks = !pKeepTicks;
      this.interval = pInterval;
   }

   protected boolean checkExtraStartConditions(ServerLevel pLevel, E pOwner) {
      if (!this.wrappedBehavior.checkExtraStartConditions(pLevel, pOwner)) {
         return false;
      } else {
         if (this.resetTicks) {
            this.resetTicksUntilNextStart(pLevel);
            this.resetTicks = false;
         }

         if (this.ticksUntilNextStart > 0) {
            --this.ticksUntilNextStart;
         }

         return !this.wasRunning && this.ticksUntilNextStart == 0;
      }
   }

   protected void start(ServerLevel pLevel, E pEntity, long pGameTime) {
      this.wrappedBehavior.start(pLevel, pEntity, pGameTime);
   }

   protected boolean canStillUse(ServerLevel pLevel, E pEntity, long pGameTime) {
      return this.wrappedBehavior.canStillUse(pLevel, pEntity, pGameTime);
   }

   protected void tick(ServerLevel pLevel, E pOwner, long pGameTime) {
      this.wrappedBehavior.tick(pLevel, pOwner, pGameTime);
      this.wasRunning = this.wrappedBehavior.getStatus() == Behavior.Status.RUNNING;
   }

   protected void stop(ServerLevel pLevel, E pEntity, long pGameTime) {
      this.resetTicksUntilNextStart(pLevel);
      this.wrappedBehavior.stop(pLevel, pEntity, pGameTime);
   }

   private void resetTicksUntilNextStart(ServerLevel pLevel) {
      this.ticksUntilNextStart = this.interval.sample(pLevel.random);
   }

   protected boolean timedOut(long pGameTime) {
      return false;
   }

   public String toString() {
      return "RunSometimes: " + this.wrappedBehavior;
   }
}