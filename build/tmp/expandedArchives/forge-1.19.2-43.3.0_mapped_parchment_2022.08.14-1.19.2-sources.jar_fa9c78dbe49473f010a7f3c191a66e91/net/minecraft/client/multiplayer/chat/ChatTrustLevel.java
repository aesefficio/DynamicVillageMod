package net.minecraft.client.multiplayer.chat;

import java.time.Instant;
import javax.annotation.Nullable;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.chat.SignedMessageValidator;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum ChatTrustLevel {
   SECURE,
   MODIFIED,
   FILTERED,
   NOT_SECURE,
   BROKEN_CHAIN;

   public static ChatTrustLevel evaluate(PlayerChatMessage p_240613_, Component p_240570_, @Nullable PlayerInfo p_240623_, Instant p_242386_) {
      if (p_240623_ == null) {
         return NOT_SECURE;
      } else {
         SignedMessageValidator.State signedmessagevalidator$state = p_240623_.getMessageValidator().validateMessage(p_240613_);
         if (signedmessagevalidator$state == SignedMessageValidator.State.BROKEN_CHAIN) {
            return BROKEN_CHAIN;
         } else if (signedmessagevalidator$state == SignedMessageValidator.State.NOT_SECURE) {
            return NOT_SECURE;
         } else if (p_240613_.hasExpiredClient(p_242386_)) {
            return NOT_SECURE;
         } else if (!p_240613_.filterMask().isEmpty()) {
            return FILTERED;
         } else if (p_240613_.unsignedContent().isPresent()) {
            return MODIFIED;
         } else {
            return !p_240570_.contains(p_240613_.signedContent().decorated()) ? MODIFIED : SECURE;
         }
      }
   }

   public boolean isNotSecure() {
      return this == NOT_SECURE || this == BROKEN_CHAIN;
   }

   @Nullable
   public GuiMessageTag createTag(PlayerChatMessage p_240632_) {
      GuiMessageTag guimessagetag;
      switch (this) {
         case MODIFIED:
            guimessagetag = GuiMessageTag.chatModified(p_240632_.signedContent().plain());
            break;
         case FILTERED:
            guimessagetag = GuiMessageTag.chatFiltered();
            break;
         case NOT_SECURE:
            guimessagetag = GuiMessageTag.chatNotSecure();
            break;
         default:
            guimessagetag = null;
      }

      return guimessagetag;
   }
}