package net.minecraft.network.chat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;

public class ChatPreviewThrottler {
   private final AtomicReference<ChatPreviewThrottler.Request> scheduledRequest = new AtomicReference<>();
   @Nullable
   private CompletableFuture<?> runningRequest;

   public void tick() {
      if (this.runningRequest != null && this.runningRequest.isDone()) {
         this.runningRequest = null;
      }

      if (this.runningRequest == null) {
         this.tickIdle();
      }

   }

   private void tickIdle() {
      ChatPreviewThrottler.Request chatpreviewthrottler$request = this.scheduledRequest.getAndSet((ChatPreviewThrottler.Request)null);
      if (chatpreviewthrottler$request != null) {
         this.runningRequest = chatpreviewthrottler$request.run();
      }

   }

   public void schedule(ChatPreviewThrottler.Request p_236977_) {
      this.scheduledRequest.set(p_236977_);
   }

   @FunctionalInterface
   public interface Request {
      CompletableFuture<?> run();
   }
}