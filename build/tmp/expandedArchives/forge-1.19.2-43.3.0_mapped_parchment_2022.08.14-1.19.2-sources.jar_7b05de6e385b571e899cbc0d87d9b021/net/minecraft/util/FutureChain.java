package net.minecraft.util;

import com.mojang.logging.LogUtils;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import org.slf4j.Logger;

public class FutureChain implements TaskChainer {
   private static final Logger LOGGER = LogUtils.getLogger();
   private CompletableFuture<?> head = CompletableFuture.completedFuture((Object)null);
   private final Executor executor;

   public FutureChain(Executor p_242395_) {
      this.executor = p_242395_;
   }

   public void append(TaskChainer.DelayedTask p_242381_) {
      this.head = this.head.thenComposeAsync((p_242302_) -> {
         return p_242381_.get();
      }, this.executor).exceptionally((p_242215_) -> {
         if (p_242215_ instanceof CompletionException completionexception) {
            p_242215_ = completionexception.getCause();
         }

         if (p_242215_ instanceof CancellationException cancellationexception) {
            throw cancellationexception;
         } else {
            LOGGER.error("Chain link failed, continuing to next one", p_242215_);
            return null;
         }
      });
   }
}