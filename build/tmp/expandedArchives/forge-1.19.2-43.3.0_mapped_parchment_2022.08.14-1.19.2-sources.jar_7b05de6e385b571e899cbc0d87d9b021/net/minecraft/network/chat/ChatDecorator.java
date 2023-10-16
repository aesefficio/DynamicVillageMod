package net.minecraft.network.chat;

import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface ChatDecorator {
   ChatDecorator PLAIN = (p_236950_, p_236951_) -> {
      return CompletableFuture.completedFuture(p_236951_);
   };

   CompletableFuture<Component> decorate(@Nullable ServerPlayer p_236962_, Component p_236963_);

   default CompletableFuture<PlayerChatMessage> decorate(@Nullable ServerPlayer p_243328_, PlayerChatMessage p_243294_) {
      return p_243294_.signedContent().isDecorated() ? CompletableFuture.completedFuture(p_243294_) : this.decorate(p_243328_, p_243294_.serverContent()).thenApply(p_243294_::withUnsignedContent);
   }

   static PlayerChatMessage attachIfNotDecorated(PlayerChatMessage p_243303_, Component p_243232_) {
      return !p_243303_.signedContent().isDecorated() ? p_243303_.withUnsignedContent(p_243232_) : p_243303_;
   }
}