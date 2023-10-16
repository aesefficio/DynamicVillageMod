package net.minecraft.network.chat;

import it.unimi.dsi.fastutil.bytes.ByteArrays;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Base64;
import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.SignatureValidator;

public record MessageSignature(byte[] bytes) {
   public static final MessageSignature EMPTY = new MessageSignature(ByteArrays.EMPTY_ARRAY);

   public MessageSignature(FriendlyByteBuf p_241519_) {
      this(p_241519_.readByteArray());
   }

   public void write(FriendlyByteBuf p_241393_) {
      p_241393_.writeByteArray(this.bytes);
   }

   public boolean verify(SignatureValidator p_241501_, SignedMessageHeader p_241273_, SignedMessageBody p_241556_) {
      if (!this.isEmpty()) {
         byte[] abyte = p_241556_.hash().asBytes();
         return p_241501_.validate((p_241242_) -> {
            p_241273_.updateSignature(p_241242_, abyte);
         }, this.bytes);
      } else {
         return false;
      }
   }

   public boolean verify(SignatureValidator p_241537_, SignedMessageHeader p_241482_, byte[] p_241502_) {
      return !this.isEmpty() ? p_241537_.validate((p_241245_) -> {
         p_241482_.updateSignature(p_241245_, p_241502_);
      }, this.bytes) : false;
   }

   public boolean isEmpty() {
      return this.bytes.length == 0;
   }

   @Nullable
   public ByteBuffer asByteBuffer() {
      return !this.isEmpty() ? ByteBuffer.wrap(this.bytes) : null;
   }

   public boolean equals(Object p_237166_) {
      if (this != p_237166_) {
         if (p_237166_ instanceof MessageSignature) {
            MessageSignature messagesignature = (MessageSignature)p_237166_;
            if (Arrays.equals(this.bytes, messagesignature.bytes)) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   public int hashCode() {
      return Arrays.hashCode(this.bytes);
   }

   public String toString() {
      return !this.isEmpty() ? Base64.getEncoder().encodeToString(this.bytes) : "empty";
   }
}