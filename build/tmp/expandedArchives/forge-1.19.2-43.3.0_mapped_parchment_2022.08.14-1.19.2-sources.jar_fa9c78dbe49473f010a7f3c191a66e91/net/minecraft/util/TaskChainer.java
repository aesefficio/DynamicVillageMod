package net.minecraft.util;

import com.mojang.logging.LogUtils;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import org.slf4j.Logger;

@FunctionalInterface
public interface TaskChainer {
   Logger LOGGER = LogUtils.getLogger();
   TaskChainer IMMEDIATE = (p_242298_) -> {
      p_242298_.get().exceptionally((p_242314_) -> {
         LOGGER.error("Task failed", p_242314_);
         return null;
      });
   };

   void append(TaskChainer.DelayedTask p_242206_);

   public interface DelayedTask extends Supplier<CompletableFuture<?>> {
   }
}