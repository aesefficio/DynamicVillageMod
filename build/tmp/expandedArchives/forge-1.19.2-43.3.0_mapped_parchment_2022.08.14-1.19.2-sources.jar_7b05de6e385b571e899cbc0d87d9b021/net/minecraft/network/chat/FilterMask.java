package net.minecraft.network.chat;

import java.util.BitSet;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.network.FriendlyByteBuf;

public class FilterMask {
   public static final FilterMask FULLY_FILTERED = new FilterMask(new BitSet(0), FilterMask.Type.FULLY_FILTERED);
   public static final FilterMask PASS_THROUGH = new FilterMask(new BitSet(0), FilterMask.Type.PASS_THROUGH);
   private static final char HASH = '#';
   private final BitSet mask;
   private final FilterMask.Type type;

   private FilterMask(BitSet p_243243_, FilterMask.Type p_243249_) {
      this.mask = p_243243_;
      this.type = p_243249_;
   }

   public FilterMask(int p_243210_) {
      this(new BitSet(p_243210_), FilterMask.Type.PARTIALLY_FILTERED);
   }

   public static FilterMask read(FriendlyByteBuf p_243205_) {
      FilterMask.Type filtermask$type = p_243205_.readEnum(FilterMask.Type.class);
      FilterMask filtermask;
      switch (filtermask$type) {
         case PASS_THROUGH:
            filtermask = PASS_THROUGH;
            break;
         case FULLY_FILTERED:
            filtermask = FULLY_FILTERED;
            break;
         case PARTIALLY_FILTERED:
            filtermask = new FilterMask(p_243205_.readBitSet(), FilterMask.Type.PARTIALLY_FILTERED);
            break;
         default:
            throw new IncompatibleClassChangeError();
      }

      return filtermask;
   }

   public static void write(FriendlyByteBuf p_243308_, FilterMask p_243231_) {
      p_243308_.writeEnum(p_243231_.type);
      if (p_243231_.type == FilterMask.Type.PARTIALLY_FILTERED) {
         p_243308_.writeBitSet(p_243231_.mask);
      }

   }

   public void setFiltered(int p_243202_) {
      this.mask.set(p_243202_);
   }

   @Nullable
   public String apply(String p_243317_) {
      String s;
      switch (this.type) {
         case PASS_THROUGH:
            s = p_243317_;
            break;
         case FULLY_FILTERED:
            s = null;
            break;
         case PARTIALLY_FILTERED:
            char[] achar = p_243317_.toCharArray();

            for(int i = 0; i < achar.length && i < this.mask.length(); ++i) {
               if (this.mask.get(i)) {
                  achar[i] = '#';
               }
            }

            s = new String(achar);
            break;
         default:
            throw new IncompatibleClassChangeError();
      }

      return s;
   }

   @Nullable
   public Component apply(ChatMessageContent p_243242_) {
      String s = p_243242_.plain();
      return Util.mapNullable(this.apply(s), Component::literal);
   }

   public boolean isEmpty() {
      return this.type == FilterMask.Type.PASS_THROUGH;
   }

   public boolean isFullyFiltered() {
      return this.type == FilterMask.Type.FULLY_FILTERED;
   }

   static enum Type {
      PASS_THROUGH,
      FULLY_FILTERED,
      PARTIALLY_FILTERED;
   }
}