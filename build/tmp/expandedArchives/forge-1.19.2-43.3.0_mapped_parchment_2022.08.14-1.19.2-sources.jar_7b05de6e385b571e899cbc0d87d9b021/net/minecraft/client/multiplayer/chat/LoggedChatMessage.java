package net.minecraft.client.multiplayer.chat;

import com.mojang.authlib.GameProfile;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.chat.SignedMessageHeader;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface LoggedChatMessage extends LoggedChatEvent {
   static LoggedChatMessage.Player player(GameProfile p_242244_, Component p_242277_, PlayerChatMessage p_242412_, ChatTrustLevel p_242155_) {
      return new LoggedChatMessage.Player(p_242244_, p_242277_, p_242412_, p_242155_);
   }

   static LoggedChatMessage.System system(Component p_242325_, Instant p_242334_) {
      return new LoggedChatMessage.System(p_242325_, p_242334_);
   }

   Component toContentComponent();

   default Component toNarrationComponent() {
      return this.toContentComponent();
   }

   boolean canReport(UUID p_242315_);

   @OnlyIn(Dist.CLIENT)
   public static record Player(GameProfile profile, Component displayName, PlayerChatMessage message, ChatTrustLevel trustLevel) implements LoggedChatMessage, LoggedChatMessageLink {
      private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT);

      public Component toContentComponent() {
         if (!this.message.filterMask().isEmpty()) {
            Component component = this.message.filterMask().apply(this.message.signedContent());
            return Objects.requireNonNullElse(component, CommonComponents.EMPTY);
         } else {
            return this.message.serverContent();
         }
      }

      public Component toNarrationComponent() {
         Component component = this.toContentComponent();
         Component component1 = this.getTimeComponent();
         return Component.translatable("gui.chatSelection.message.narrate", this.displayName, component, component1);
      }

      public Component toHeadingComponent() {
         Component component = this.getTimeComponent();
         return Component.translatable("gui.chatSelection.heading", this.displayName, component);
      }

      private Component getTimeComponent() {
         LocalDateTime localdatetime = LocalDateTime.ofInstant(this.message.timeStamp(), ZoneOffset.systemDefault());
         return Component.literal(localdatetime.format(TIME_FORMATTER)).withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY);
      }

      public boolean canReport(UUID p_242210_) {
         return this.message.hasSignatureFrom(p_242210_);
      }

      public SignedMessageHeader header() {
         return this.message.signedHeader();
      }

      public byte[] bodyDigest() {
         return this.message.signedBody().hash().asBytes();
      }

      public MessageSignature headerSignature() {
         return this.message.headerSignature();
      }

      public UUID profileId() {
         return this.profile.getId();
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static record System(Component message, Instant timeStamp) implements LoggedChatMessage {
      public Component toContentComponent() {
         return this.message;
      }

      public boolean canReport(UUID p_242173_) {
         return false;
      }
   }
}