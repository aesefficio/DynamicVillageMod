package net.minecraft.network.chat;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import net.minecraft.network.FriendlyByteBuf;

public record SignedMessageBody(ChatMessageContent content, Instant timeStamp, long salt, LastSeenMessages lastSeen) {
   public static final byte HASH_SEPARATOR_BYTE = 70;

   public SignedMessageBody(FriendlyByteBuf p_241548_) {
      this(ChatMessageContent.read(p_241548_), p_241548_.readInstant(), p_241548_.readLong(), new LastSeenMessages(p_241548_));
   }

   public void write(FriendlyByteBuf p_241357_) {
      ChatMessageContent.write(p_241357_, this.content);
      p_241357_.writeInstant(this.timeStamp);
      p_241357_.writeLong(this.salt);
      this.lastSeen.write(p_241357_);
   }

   public HashCode hash() {
      HashingOutputStream hashingoutputstream = new HashingOutputStream(Hashing.sha256(), OutputStream.nullOutputStream());

      try {
         DataOutputStream dataoutputstream = new DataOutputStream(hashingoutputstream);
         dataoutputstream.writeLong(this.salt);
         dataoutputstream.writeLong(this.timeStamp.getEpochSecond());
         OutputStreamWriter outputstreamwriter = new OutputStreamWriter(dataoutputstream, StandardCharsets.UTF_8);
         outputstreamwriter.write(this.content.plain());
         outputstreamwriter.flush();
         dataoutputstream.write(70);
         if (this.content.isDecorated()) {
            outputstreamwriter.write(Component.Serializer.toStableJson(this.content.decorated()));
            outputstreamwriter.flush();
         }

         this.lastSeen.updateHash(dataoutputstream);
      } catch (IOException ioexception) {
      }

      return hashingoutputstream.hash();
   }

   public SignedMessageBody withContent(ChatMessageContent p_242907_) {
      return new SignedMessageBody(p_242907_, this.timeStamp, this.salt, this.lastSeen);
   }
}