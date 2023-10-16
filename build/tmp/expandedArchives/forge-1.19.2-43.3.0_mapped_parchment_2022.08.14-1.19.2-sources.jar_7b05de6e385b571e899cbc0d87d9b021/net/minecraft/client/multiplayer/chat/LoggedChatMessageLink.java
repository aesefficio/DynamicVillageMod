package net.minecraft.client.multiplayer.chat;

import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.SignedMessageHeader;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface LoggedChatMessageLink extends LoggedChatEvent {
   static LoggedChatMessageLink.Header header(SignedMessageHeader p_242461_, MessageSignature p_242167_, byte[] p_242320_) {
      return new LoggedChatMessageLink.Header(p_242461_, p_242167_, p_242320_);
   }

   SignedMessageHeader header();

   MessageSignature headerSignature();

   byte[] bodyDigest();

   @OnlyIn(Dist.CLIENT)
   public static record Header(SignedMessageHeader header, MessageSignature headerSignature, byte[] bodyDigest) implements LoggedChatMessageLink {
      public SignedMessageHeader header() {
         return this.header;
      }

      public MessageSignature headerSignature() {
         return this.headerSignature;
      }

      public byte[] bodyDigest() {
         return this.bodyDigest;
      }
   }
}