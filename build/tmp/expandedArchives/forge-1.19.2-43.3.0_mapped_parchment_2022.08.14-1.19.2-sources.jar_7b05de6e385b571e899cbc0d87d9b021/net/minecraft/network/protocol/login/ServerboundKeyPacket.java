package net.minecraft.network.protocol.login;

import com.mojang.datafixers.util.Either;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Optional;
import javax.crypto.SecretKey;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.util.Crypt;
import net.minecraft.util.CryptException;
import net.minecraft.world.entity.player.ProfilePublicKey;

public class ServerboundKeyPacket implements Packet<ServerLoginPacketListener> {
   private final byte[] keybytes;
   private final Either<byte[], Crypt.SaltSignaturePair> nonceOrSaltSignature;

   public ServerboundKeyPacket(SecretKey pSecretKey, PublicKey pPublicKey, byte[] pNonce) throws CryptException {
      this.keybytes = Crypt.encryptUsingKey(pPublicKey, pSecretKey.getEncoded());
      this.nonceOrSaltSignature = Either.left(Crypt.encryptUsingKey(pPublicKey, pNonce));
   }

   public ServerboundKeyPacket(SecretKey p_238057_, PublicKey p_238058_, long p_238059_, byte[] p_238060_) throws CryptException {
      this.keybytes = Crypt.encryptUsingKey(p_238058_, p_238057_.getEncoded());
      this.nonceOrSaltSignature = Either.right(new Crypt.SaltSignaturePair(p_238059_, p_238060_));
   }

   public ServerboundKeyPacket(FriendlyByteBuf pBuffer) {
      this.keybytes = pBuffer.readByteArray();
      this.nonceOrSaltSignature = pBuffer.readEither(FriendlyByteBuf::readByteArray, Crypt.SaltSignaturePair::new);
   }

   /**
    * Writes the raw packet data to the data stream.
    */
   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeByteArray(this.keybytes);
      pBuffer.writeEither(this.nonceOrSaltSignature, FriendlyByteBuf::writeByteArray, Crypt.SaltSignaturePair::write);
   }

   /**
    * Passes this Packet on to the NetHandler for processing.
    */
   public void handle(ServerLoginPacketListener pHandler) {
      pHandler.handleKey(this);
   }

   public SecretKey getSecretKey(PrivateKey pKey) throws CryptException {
      return Crypt.decryptByteToSecretKey(pKey, this.keybytes);
   }

   public boolean isChallengeSignatureValid(byte[] p_238072_, ProfilePublicKey p_238073_) {
      return this.nonceOrSaltSignature.map((p_238066_) -> {
         return false;
      }, (p_238064_) -> {
         return p_238073_.createSignatureValidator().validate((p_238070_) -> {
            p_238070_.update(p_238072_);
            p_238070_.update(p_238064_.saltAsBytes());
         }, p_238064_.signature());
      });
   }

   public boolean isNonceValid(byte[] pNonce, PrivateKey pKey) {
      Optional<byte[]> optional = this.nonceOrSaltSignature.left();

      try {
         return optional.isPresent() && Arrays.equals(pNonce, Crypt.decryptUsingKey(pKey, optional.get()));
      } catch (CryptException cryptexception) {
         return false;
      }
   }
}