package net.minecraft.network.chat;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.SignatureValidator;
import net.minecraft.world.entity.player.ProfilePublicKey;

public record PlayerChatMessage(SignedMessageHeader signedHeader, MessageSignature headerSignature, SignedMessageBody signedBody, Optional<Component> unsignedContent, FilterMask filterMask) {
   public static final Duration MESSAGE_EXPIRES_AFTER_SERVER = Duration.ofMinutes(5L);
   public static final Duration MESSAGE_EXPIRES_AFTER_CLIENT = MESSAGE_EXPIRES_AFTER_SERVER.plus(Duration.ofMinutes(2L));

   public PlayerChatMessage(FriendlyByteBuf p_241419_) {
      this(new SignedMessageHeader(p_241419_), new MessageSignature(p_241419_), new SignedMessageBody(p_241419_), p_241419_.readOptional(FriendlyByteBuf::readComponent), FilterMask.read(p_241419_));
   }

   public static PlayerChatMessage system(ChatMessageContent p_242910_) {
      return unsigned(MessageSigner.system(), p_242910_);
   }

   public static PlayerChatMessage unsigned(MessageSigner p_243247_, ChatMessageContent p_243279_) {
      SignedMessageBody signedmessagebody = new SignedMessageBody(p_243279_, p_243247_.timeStamp(), p_243247_.salt(), LastSeenMessages.EMPTY);
      SignedMessageHeader signedmessageheader = new SignedMessageHeader((MessageSignature)null, p_243247_.profileId());
      return new PlayerChatMessage(signedmessageheader, MessageSignature.EMPTY, signedmessagebody, Optional.empty(), FilterMask.PASS_THROUGH);
   }

   public void write(FriendlyByteBuf p_241490_) {
      this.signedHeader.write(p_241490_);
      this.headerSignature.write(p_241490_);
      this.signedBody.write(p_241490_);
      p_241490_.writeOptional(this.unsignedContent, FriendlyByteBuf::writeComponent);
      FilterMask.write(p_241490_, this.filterMask);
   }

   public PlayerChatMessage withUnsignedContent(Component p_242164_) {
      Optional<Component> optional = !this.signedContent().decorated().equals(p_242164_) ? Optional.of(p_242164_) : Optional.empty();
      return new PlayerChatMessage(this.signedHeader, this.headerSignature, this.signedBody, optional, this.filterMask);
   }

   public PlayerChatMessage removeUnsignedContent() {
      return this.unsignedContent.isPresent() ? new PlayerChatMessage(this.signedHeader, this.headerSignature, this.signedBody, Optional.empty(), this.filterMask) : this;
   }

   public PlayerChatMessage filter(FilterMask p_243320_) {
      return this.filterMask.equals(p_243320_) ? this : new PlayerChatMessage(this.signedHeader, this.headerSignature, this.signedBody, this.unsignedContent, p_243320_);
   }

   public PlayerChatMessage filter(boolean p_243223_) {
      return this.filter(p_243223_ ? this.filterMask : FilterMask.PASS_THROUGH);
   }

   public boolean verify(SignatureValidator p_241442_) {
      return this.headerSignature.verify(p_241442_, this.signedHeader, this.signedBody);
   }

   public boolean verify(ProfilePublicKey p_237229_) {
      SignatureValidator signaturevalidator = p_237229_.createSignatureValidator();
      return this.verify(signaturevalidator);
   }

   public boolean verify(ChatSender p_241394_) {
      ProfilePublicKey profilepublickey = p_241394_.profilePublicKey();
      return profilepublickey != null && this.verify(profilepublickey);
   }

   public ChatMessageContent signedContent() {
      return this.signedBody.content();
   }

   public Component serverContent() {
      return this.unsignedContent().orElse(this.signedContent().decorated());
   }

   public Instant timeStamp() {
      return this.signedBody.timeStamp();
   }

   public long salt() {
      return this.signedBody.salt();
   }

   public boolean hasExpiredServer(Instant p_240573_) {
      return p_240573_.isAfter(this.timeStamp().plus(MESSAGE_EXPIRES_AFTER_SERVER));
   }

   public boolean hasExpiredClient(Instant p_240629_) {
      return p_240629_.isAfter(this.timeStamp().plus(MESSAGE_EXPIRES_AFTER_CLIENT));
   }

   public MessageSigner signer() {
      return new MessageSigner(this.signedHeader.sender(), this.timeStamp(), this.salt());
   }

   @Nullable
   public LastSeenMessages.Entry toLastSeenEntry() {
      MessageSigner messagesigner = this.signer();
      return !this.headerSignature.isEmpty() && !messagesigner.isSystem() ? new LastSeenMessages.Entry(messagesigner.profileId(), this.headerSignature) : null;
   }

   public boolean hasSignatureFrom(UUID p_243236_) {
      return !this.headerSignature.isEmpty() && this.signedHeader.sender().equals(p_243236_);
   }

   public boolean isFullyFiltered() {
      return this.filterMask.isFullyFiltered();
   }
}