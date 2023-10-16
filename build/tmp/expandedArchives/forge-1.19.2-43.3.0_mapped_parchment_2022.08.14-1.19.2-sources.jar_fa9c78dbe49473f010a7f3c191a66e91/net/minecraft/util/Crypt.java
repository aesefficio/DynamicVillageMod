package net.minecraft.util;

import com.google.common.primitives.Longs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.bytes.ByteArrays;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import net.minecraft.network.FriendlyByteBuf;

public class Crypt {
   private static final String SYMMETRIC_ALGORITHM = "AES";
   private static final int SYMMETRIC_BITS = 128;
   private static final String ASYMMETRIC_ALGORITHM = "RSA";
   private static final int ASYMMETRIC_BITS = 1024;
   private static final String BYTE_ENCODING = "ISO_8859_1";
   private static final String HASH_ALGORITHM = "SHA-1";
   public static final String SIGNING_ALGORITHM = "SHA256withRSA";
   private static final String PEM_RSA_PRIVATE_KEY_HEADER = "-----BEGIN RSA PRIVATE KEY-----";
   private static final String PEM_RSA_PRIVATE_KEY_FOOTER = "-----END RSA PRIVATE KEY-----";
   public static final String RSA_PUBLIC_KEY_HEADER = "-----BEGIN RSA PUBLIC KEY-----";
   private static final String RSA_PUBLIC_KEY_FOOTER = "-----END RSA PUBLIC KEY-----";
   public static final String MIME_LINE_SEPARATOR = "\n";
   public static final Base64.Encoder MIME_ENCODER = Base64.getMimeEncoder(76, "\n".getBytes(StandardCharsets.UTF_8));
   public static final Codec<PublicKey> PUBLIC_KEY_CODEC = Codec.STRING.comapFlatMap((p_216087_) -> {
      try {
         return DataResult.success(stringToRsaPublicKey(p_216087_));
      } catch (CryptException cryptexception) {
         return DataResult.error(cryptexception.getMessage());
      }
   }, Crypt::rsaPublicKeyToString);
   public static final Codec<PrivateKey> PRIVATE_KEY_CODEC = Codec.STRING.comapFlatMap((p_216085_) -> {
      try {
         return DataResult.success(stringToPemRsaPrivateKey(p_216085_));
      } catch (CryptException cryptexception) {
         return DataResult.error(cryptexception.getMessage());
      }
   }, Crypt::pemRsaPrivateKeyToString);

   /**
    * Generate a new shared secret AES key from a secure random source
    */
   public static SecretKey generateSecretKey() throws CryptException {
      try {
         KeyGenerator keygenerator = KeyGenerator.getInstance("AES");
         keygenerator.init(128);
         return keygenerator.generateKey();
      } catch (Exception exception) {
         throw new CryptException(exception);
      }
   }

   /**
    * Generates RSA KeyPair
    */
   public static KeyPair generateKeyPair() throws CryptException {
      try {
         KeyPairGenerator keypairgenerator = KeyPairGenerator.getInstance("RSA");
         keypairgenerator.initialize(1024);
         return keypairgenerator.generateKeyPair();
      } catch (Exception exception) {
         throw new CryptException(exception);
      }
   }

   /**
    * Compute a serverId hash for use by sendSessionRequest()
    */
   public static byte[] digestData(String pServerId, PublicKey pPublicKey, SecretKey pSecretKey) throws CryptException {
      try {
         return digestData(pServerId.getBytes("ISO_8859_1"), pSecretKey.getEncoded(), pPublicKey.getEncoded());
      } catch (Exception exception) {
         throw new CryptException(exception);
      }
   }

   private static byte[] digestData(byte[]... pData) throws Exception {
      MessageDigest messagedigest = MessageDigest.getInstance("SHA-1");

      for(byte[] abyte : pData) {
         messagedigest.update(abyte);
      }

      return messagedigest.digest();
   }

   private static <T extends Key> T rsaStringToKey(String p_216072_, String p_216073_, String p_216074_, Crypt.ByteArrayToKeyFunction<T> p_216075_) throws CryptException {
      int i = p_216072_.indexOf(p_216073_);
      if (i != -1) {
         i += p_216073_.length();
         int j = p_216072_.indexOf(p_216074_, i);
         p_216072_ = p_216072_.substring(i, j + 1);
      }

      try {
         return p_216075_.apply(Base64.getMimeDecoder().decode(p_216072_));
      } catch (IllegalArgumentException illegalargumentexception) {
         throw new CryptException(illegalargumentexception);
      }
   }

   public static PrivateKey stringToPemRsaPrivateKey(String p_216070_) throws CryptException {
      return rsaStringToKey(p_216070_, "-----BEGIN RSA PRIVATE KEY-----", "-----END RSA PRIVATE KEY-----", Crypt::byteToPrivateKey);
   }

   public static PublicKey stringToRsaPublicKey(String p_216081_) throws CryptException {
      return rsaStringToKey(p_216081_, "-----BEGIN RSA PUBLIC KEY-----", "-----END RSA PUBLIC KEY-----", Crypt::byteToPublicKey);
   }

   public static String rsaPublicKeyToString(PublicKey p_216079_) {
      if (!"RSA".equals(p_216079_.getAlgorithm())) {
         throw new IllegalArgumentException("Public key must be RSA");
      } else {
         return "-----BEGIN RSA PUBLIC KEY-----\n" + MIME_ENCODER.encodeToString(p_216079_.getEncoded()) + "\n-----END RSA PUBLIC KEY-----\n";
      }
   }

   public static String pemRsaPrivateKeyToString(PrivateKey p_216077_) {
      if (!"RSA".equals(p_216077_.getAlgorithm())) {
         throw new IllegalArgumentException("Private key must be RSA");
      } else {
         return "-----BEGIN RSA PRIVATE KEY-----\n" + MIME_ENCODER.encodeToString(p_216077_.getEncoded()) + "\n-----END RSA PRIVATE KEY-----\n";
      }
   }

   private static PrivateKey byteToPrivateKey(byte[] p_216083_) throws CryptException {
      try {
         EncodedKeySpec encodedkeyspec = new PKCS8EncodedKeySpec(p_216083_);
         KeyFactory keyfactory = KeyFactory.getInstance("RSA");
         return keyfactory.generatePrivate(encodedkeyspec);
      } catch (Exception exception) {
         throw new CryptException(exception);
      }
   }

   /**
    * Create a new PublicKey from encoded X.509 data
    */
   public static PublicKey byteToPublicKey(byte[] p_13601_) throws CryptException {
      try {
         EncodedKeySpec encodedkeyspec = new X509EncodedKeySpec(p_13601_);
         KeyFactory keyfactory = KeyFactory.getInstance("RSA");
         return keyfactory.generatePublic(encodedkeyspec);
      } catch (Exception exception) {
         throw new CryptException(exception);
      }
   }

   /**
    * Decrypt shared secret AES key using RSA private key
    */
   public static SecretKey decryptByteToSecretKey(PrivateKey pKey, byte[] pSecretKeyEncrypted) throws CryptException {
      byte[] abyte = decryptUsingKey(pKey, pSecretKeyEncrypted);

      try {
         return new SecretKeySpec(abyte, "AES");
      } catch (Exception exception) {
         throw new CryptException(exception);
      }
   }

   /**
    * Encrypt byte[] data with RSA public key
    */
   public static byte[] encryptUsingKey(Key pKey, byte[] pData) throws CryptException {
      return cipherData(1, pKey, pData);
   }

   /**
    * Decrypt byte[] data with RSA private key
    */
   public static byte[] decryptUsingKey(Key pKey, byte[] pData) throws CryptException {
      return cipherData(2, pKey, pData);
   }

   /**
    * Encrypt or decrypt byte[] data using the specified key
    */
   private static byte[] cipherData(int pOpMode, Key pKey, byte[] pData) throws CryptException {
      try {
         return setupCipher(pOpMode, pKey.getAlgorithm(), pKey).doFinal(pData);
      } catch (Exception exception) {
         throw new CryptException(exception);
      }
   }

   /**
    * Creates the Cipher Instance.
    */
   private static Cipher setupCipher(int pOpMode, String pTransformation, Key pKey) throws Exception {
      Cipher cipher = Cipher.getInstance(pTransformation);
      cipher.init(pOpMode, pKey);
      return cipher;
   }

   /**
    * Creates an Cipher instance using the AES/CFB8/NoPadding algorithm. Used for protocol encryption.
    */
   public static Cipher getCipher(int pOpMode, Key pKey) throws CryptException {
      try {
         Cipher cipher = Cipher.getInstance("AES/CFB8/NoPadding");
         cipher.init(pOpMode, pKey, new IvParameterSpec(pKey.getEncoded()));
         return cipher;
      } catch (Exception exception) {
         throw new CryptException(exception);
      }
   }

   interface ByteArrayToKeyFunction<T extends Key> {
      T apply(byte[] p_216089_) throws CryptException;
   }

   public static record SaltSignaturePair(long salt, byte[] signature) {
      public static final Crypt.SaltSignaturePair EMPTY = new Crypt.SaltSignaturePair(0L, ByteArrays.EMPTY_ARRAY);

      public SaltSignaturePair(FriendlyByteBuf p_216098_) {
         this(p_216098_.readLong(), p_216098_.readByteArray());
      }

      public boolean isValid() {
         return this.signature.length > 0;
      }

      public static void write(FriendlyByteBuf p_216101_, Crypt.SaltSignaturePair p_216102_) {
         p_216101_.writeLong(p_216102_.salt);
         p_216101_.writeByteArray(p_216102_.signature);
      }

      public byte[] saltAsBytes() {
         return Longs.toByteArray(this.salt);
      }
   }

   public static class SaltSupplier {
      private static final SecureRandom secureRandom = new SecureRandom();

      public static long getLong() {
         return secureRandom.nextLong();
      }
   }
}