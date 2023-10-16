package net.minecraft.network.chat;

import java.util.Objects;
import net.minecraft.network.FriendlyByteBuf;

public record ChatMessageContent(String plain, Component decorated) {
   public ChatMessageContent(String pPlain) {
      this(pPlain, Component.literal(pPlain));
   }

   public boolean isDecorated() {
      return !this.decorated.equals(Component.literal(this.plain));
   }

   public static ChatMessageContent read(FriendlyByteBuf pBuffer) {
      String s = pBuffer.readUtf(256);
      Component component = pBuffer.readNullable(FriendlyByteBuf::readComponent);
      return new ChatMessageContent(s, Objects.requireNonNullElse(component, Component.literal(s)));
   }

   public static void write(FriendlyByteBuf pBuffer, ChatMessageContent pContent) {
      pBuffer.writeUtf(pContent.plain(), 256);
      Component component = pContent.isDecorated() ? pContent.decorated() : null;
      pBuffer.writeNullable(component, FriendlyByteBuf::writeComponent);
   }
}