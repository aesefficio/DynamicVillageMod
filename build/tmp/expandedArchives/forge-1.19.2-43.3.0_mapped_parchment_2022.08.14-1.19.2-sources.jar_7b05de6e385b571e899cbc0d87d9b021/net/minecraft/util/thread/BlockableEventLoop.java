package net.minecraft.util.thread;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Queues;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.util.profiling.metrics.MetricCategory;
import net.minecraft.util.profiling.metrics.MetricSampler;
import net.minecraft.util.profiling.metrics.MetricsRegistry;
import net.minecraft.util.profiling.metrics.ProfilerMeasured;
import org.slf4j.Logger;

public abstract class BlockableEventLoop<R extends Runnable> implements ProfilerMeasured, ProcessorHandle<R>, Executor {
   private final String name;
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Queue<R> pendingRunnables = Queues.newConcurrentLinkedQueue();
   private int blockingCount;

   protected BlockableEventLoop(String pName) {
      this.name = pName;
      MetricsRegistry.INSTANCE.add(this);
   }

   protected abstract R wrapRunnable(Runnable pRunnable);

   protected abstract boolean shouldRun(R pRunnable);

   public boolean isSameThread() {
      return Thread.currentThread() == this.getRunningThread();
   }

   protected abstract Thread getRunningThread();

   protected boolean scheduleExecutables() {
      return !this.isSameThread();
   }

   public int getPendingTasksCount() {
      return this.pendingRunnables.size();
   }

   public String name() {
      return this.name;
   }

   public <V> CompletableFuture<V> submit(Supplier<V> pSupplier) {
      return this.scheduleExecutables() ? CompletableFuture.supplyAsync(pSupplier, this) : CompletableFuture.completedFuture(pSupplier.get());
   }

   public CompletableFuture<Void> submitAsync(Runnable pTask) {
      return CompletableFuture.supplyAsync(() -> {
         pTask.run();
         return null;
      }, this);
   }

   public CompletableFuture<Void> submit(Runnable pTask) {
      if (this.scheduleExecutables()) {
         return this.submitAsync(pTask);
      } else {
         pTask.run();
         return CompletableFuture.completedFuture((Void)null);
      }
   }

   public void executeBlocking(Runnable pTask) {
      if (!this.isSameThread()) {
         this.submitAsync(pTask).join();
      } else {
         pTask.run();
      }

   }

   public void tell(R pTask) {
      this.pendingRunnables.add(pTask);
      LockSupport.unpark(this.getRunningThread());
   }

   public void execute(Runnable pTask) {
      if (this.scheduleExecutables()) {
         this.tell(this.wrapRunnable(pTask));
      } else {
         pTask.run();
      }

   }

   public void executeIfPossible(Runnable pTask) {
      this.execute(pTask);
   }

   protected void dropAllTasks() {
      this.pendingRunnables.clear();
   }

   protected void runAllTasks() {
      while(this.pollTask()) {
      }

   }

   public boolean pollTask() {
      R r = this.pendingRunnables.peek();
      if (r == null) {
         return false;
      } else if (this.blockingCount == 0 && !this.shouldRun(r)) {
         return false;
      } else {
         this.doRunTask(this.pendingRunnables.remove());
         return true;
      }
   }

   /**
    * Drive the executor until the given BooleanSupplier returns true
    */
   public void managedBlock(BooleanSupplier pIsDone) {
      ++this.blockingCount;

      try {
         while(!pIsDone.getAsBoolean()) {
            if (!this.pollTask()) {
               this.waitForTasks();
            }
         }
      } finally {
         --this.blockingCount;
      }

   }

   protected void waitForTasks() {
      Thread.yield();
      LockSupport.parkNanos("waiting for tasks", 100000L);
   }

   protected void doRunTask(R pTask) {
      try {
         pTask.run();
      } catch (Exception exception) {
         LOGGER.error(LogUtils.FATAL_MARKER, "Error executing task on {}", this.name(), exception);
      }

   }

   public List<MetricSampler> profiledMetrics() {
      return ImmutableList.of(MetricSampler.create(this.name + "-pending-tasks", MetricCategory.EVENT_LOOPS, this::getPendingTasksCount));
   }
}