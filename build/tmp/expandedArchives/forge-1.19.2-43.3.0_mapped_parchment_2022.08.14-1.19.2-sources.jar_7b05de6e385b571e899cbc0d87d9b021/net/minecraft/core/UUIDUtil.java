package net.minecraft.core;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;
import net.minecraft.Util;

public final class UUIDUtil {
   public static final Codec<UUID> CODEC = Codec.INT_STREAM.comapFlatMap((p_235884_) -> {
      return Util.fixedSize(p_235884_, 4).map(UUIDUtil::uuidFromIntArray);
   }, (p_235888_) -> {
      return Arrays.stream(uuidToIntArray(p_235888_));
   });
   public static final int UUID_BYTES = 16;
   private static final String UUID_PREFIX_OFFLINE_PLAYER = "OfflinePlayer:";

   private UUIDUtil() {
   }

   public static UUID uuidFromIntArray(int[] p_235886_) {
      return new UUID((long)p_235886_[0] << 32 | (long)p_235886_[1] & 4294967295L, (long)p_235886_[2] << 32 | (long)p_235886_[3] & 4294967295L);
   }

   public static int[] uuidToIntArray(UUID pUuid) {
      long i = pUuid.getMostSignificantBits();
      long j = pUuid.getLeastSignificantBits();
      return leastMostToIntArray(i, j);
   }

   private static int[] leastMostToIntArray(long pMost, long pLeast) {
      return new int[]{(int)(pMost >> 32), (int)pMost, (int)(pLeast >> 32), (int)pLeast};
   }

   public static byte[] uuidToByteArray(UUID pUuid) {
      byte[] abyte = new byte[16];
      ByteBuffer.wrap(abyte).order(ByteOrder.BIG_ENDIAN).putLong(pUuid.getMostSignificantBits()).putLong(pUuid.getLeastSignificantBits());
      return abyte;
   }

   public static UUID readUUID(Dynamic<?> pDynamic) {
      int[] aint = pDynamic.asIntStream().toArray();
      if (aint.length != 4) {
         throw new IllegalArgumentException("Could not read UUID. Expected int-array of length 4, got " + aint.length + ".");
      } else {
         return uuidFromIntArray(aint);
      }
   }

   public static UUID getOrCreatePlayerUUID(GameProfile pProfile) {
      UUID uuid = pProfile.getId();
      if (uuid == null) {
         uuid = createOfflinePlayerUUID(pProfile.getName());
      }

      return uuid;
   }

   public static UUID createOfflinePlayerUUID(String pUsername) {
      return UUID.nameUUIDFromBytes(("OfflinePlayer:" + pUsername).getBytes(StandardCharsets.UTF_8));
   }
}