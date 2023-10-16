package net.minecraft.network.chat;

import javax.annotation.Nullable;
import net.minecraft.util.SignatureValidator;
import net.minecraft.world.entity.player.ProfilePublicKey;

public interface SignedMessageValidator {
   static SignedMessageValidator create(@Nullable ProfilePublicKey p_242975_, boolean p_242976_) {
      return (SignedMessageValidator)(p_242975_ != null ? new SignedMessageValidator.KeyBased(p_242975_.createSignatureValidator()) : new SignedMessageValidator.Unsigned(p_242976_));
   }

   SignedMessageValidator.State validateHeader(SignedMessageHeader p_241472_, MessageSignature p_241508_, byte[] p_241320_);

   SignedMessageValidator.State validateMessage(PlayerChatMessage p_241552_);

   public static class KeyBased implements SignedMessageValidator {
      private final SignatureValidator validator;
      @Nullable
      private MessageSignature lastSignature;
      private boolean isChainConsistent = true;

      public KeyBased(SignatureValidator p_241517_) {
         this.validator = p_241517_;
      }

      private boolean validateChain(SignedMessageHeader p_243280_, MessageSignature p_243215_, boolean p_243312_) {
         if (p_243215_.isEmpty()) {
            return false;
         } else if (p_243312_ && p_243215_.equals(this.lastSignature)) {
            return true;
         } else {
            return this.lastSignature == null || this.lastSignature.equals(p_243280_.previousSignature());
         }
      }

      private boolean validateContents(SignedMessageHeader p_243269_, MessageSignature p_243259_, byte[] p_243265_, boolean p_243221_) {
         return this.validateChain(p_243269_, p_243259_, p_243221_) && p_243259_.verify(this.validator, p_243269_, p_243265_);
      }

      private SignedMessageValidator.State updateAndValidate(SignedMessageHeader p_243211_, MessageSignature p_243274_, byte[] p_243209_, boolean p_243324_) {
         this.isChainConsistent = this.isChainConsistent && this.validateContents(p_243211_, p_243274_, p_243209_, p_243324_);
         if (!this.isChainConsistent) {
            return SignedMessageValidator.State.BROKEN_CHAIN;
         } else {
            this.lastSignature = p_243274_;
            return SignedMessageValidator.State.SECURE;
         }
      }

      public SignedMessageValidator.State validateHeader(SignedMessageHeader p_242886_, MessageSignature p_242853_, byte[] p_242869_) {
         return this.updateAndValidate(p_242886_, p_242853_, p_242869_, false);
      }

      public SignedMessageValidator.State validateMessage(PlayerChatMessage p_242943_) {
         byte[] abyte = p_242943_.signedBody().hash().asBytes();
         return this.updateAndValidate(p_242943_.signedHeader(), p_242943_.headerSignature(), abyte, true);
      }
   }

   public static enum State {
      SECURE,
      NOT_SECURE,
      BROKEN_CHAIN;
   }

   public static class Unsigned implements SignedMessageValidator {
      private final boolean enforcesSecureChat;

      public Unsigned(boolean p_243256_) {
         this.enforcesSecureChat = p_243256_;
      }

      private SignedMessageValidator.State validate(MessageSignature p_243292_) {
         if (!p_243292_.isEmpty()) {
            return SignedMessageValidator.State.BROKEN_CHAIN;
         } else {
            return this.enforcesSecureChat ? SignedMessageValidator.State.BROKEN_CHAIN : SignedMessageValidator.State.NOT_SECURE;
         }
      }

      public SignedMessageValidator.State validateHeader(SignedMessageHeader p_243299_, MessageSignature p_243315_, byte[] p_243252_) {
         return this.validate(p_243315_);
      }

      public SignedMessageValidator.State validateMessage(PlayerChatMessage p_243296_) {
         return this.validate(p_243296_.headerSignature());
      }
   }
}