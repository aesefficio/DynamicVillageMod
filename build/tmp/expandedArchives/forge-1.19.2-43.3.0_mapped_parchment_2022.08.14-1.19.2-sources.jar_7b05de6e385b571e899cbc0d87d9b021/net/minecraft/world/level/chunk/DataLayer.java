package net.minecraft.world.level.chunk;

import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.util.VisibleForDebug;

/**
 * A representation of a 16x16x16 cube of nibbles (half-bytes).
 */
public final class DataLayer {
   public static final int LAYER_COUNT = 16;
   public static final int LAYER_SIZE = 128;
   public static final int SIZE = 2048;
   private static final int NIBBLE_SIZE = 4;
   @Nullable
   protected byte[] data;

   public DataLayer() {
   }

   public DataLayer(byte[] pData) {
      this.data = pData;
      if (pData.length != 2048) {
         throw (IllegalArgumentException)Util.pauseInIde(new IllegalArgumentException("DataLayer should be 2048 bytes not: " + pData.length));
      }
   }

   protected DataLayer(int pSize) {
      this.data = new byte[pSize];
   }

   /**
    * @return The value of this data layer at the provided position.
    * Note all coordinates must be in the range [0, 16), they <strong>are not checked</strong>, and will either silently
    * overrun the array or throw an exception.
    */
   public int get(int pX, int pY, int pZ) {
      return this.get(getIndex(pX, pY, pZ));
   }

   /**
    * Sets the value of this data layer at the provided position.
    * Note all coordinates must be in the range [0, 16), they <strong>are not checked</strong>, and will either silently
    * overrun the array or throw an exception.
    */
   public void set(int pX, int pY, int pZ, int pValue) {
      this.set(getIndex(pX, pY, pZ), pValue);
   }

   private static int getIndex(int pX, int pY, int pZ) {
      return pY << 8 | pZ << 4 | pX;
   }

   private int get(int pIndex) {
      if (this.data == null) {
         return 0;
      } else {
         int i = getByteIndex(pIndex);
         int j = getNibbleIndex(pIndex);
         return this.data[i] >> 4 * j & 15;
      }
   }

   private void set(int pIndex, int pValue) {
      if (this.data == null) {
         this.data = new byte[2048];
      }

      int i = getByteIndex(pIndex);
      int j = getNibbleIndex(pIndex);
      int k = ~(15 << 4 * j);
      int l = (pValue & 15) << 4 * j;
      this.data[i] = (byte)(this.data[i] & k | l);
   }

   private static int getNibbleIndex(int pIndex) {
      return pIndex & 1;
   }

   private static int getByteIndex(int pIndex) {
      return pIndex >> 1;
   }

   public byte[] getData() {
      if (this.data == null) {
         this.data = new byte[2048];
      }

      return this.data;
   }

   public DataLayer copy() {
      return this.data == null ? new DataLayer() : new DataLayer((byte[])this.data.clone());
   }

   public String toString() {
      StringBuilder stringbuilder = new StringBuilder();

      for(int i = 0; i < 4096; ++i) {
         stringbuilder.append(Integer.toHexString(this.get(i)));
         if ((i & 15) == 15) {
            stringbuilder.append("\n");
         }

         if ((i & 255) == 255) {
            stringbuilder.append("\n");
         }
      }

      return stringbuilder.toString();
   }

   @VisibleForDebug
   public String layerToString(int pUnused) {
      StringBuilder stringbuilder = new StringBuilder();

      for(int i = 0; i < 256; ++i) {
         stringbuilder.append(Integer.toHexString(this.get(i)));
         if ((i & 15) == 15) {
            stringbuilder.append("\n");
         }
      }

      return stringbuilder.toString();
   }

   public boolean isEmpty() {
      return this.data == null;
   }
}