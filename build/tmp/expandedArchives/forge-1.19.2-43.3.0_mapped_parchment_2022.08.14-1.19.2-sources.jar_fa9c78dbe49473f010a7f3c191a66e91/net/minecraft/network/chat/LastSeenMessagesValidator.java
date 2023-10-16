package net.minecraft.network.chat;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;

public class LastSeenMessagesValidator {
   private static final int NOT_FOUND = Integer.MIN_VALUE;
   private LastSeenMessages lastSeenMessages = LastSeenMessages.EMPTY;
   private final ObjectList<LastSeenMessages.Entry> pendingEntries = new ObjectArrayList<>();

   public void addPending(LastSeenMessages.Entry p_242384_) {
      this.pendingEntries.add(p_242384_);
   }

   public int pendingMessagesCount() {
      return this.pendingEntries.size();
   }

   private boolean hasDuplicateProfiles(LastSeenMessages p_242422_) {
      Set<UUID> set = new HashSet<>(p_242422_.entries().size());

      for(LastSeenMessages.Entry lastseenmessages$entry : p_242422_.entries()) {
         if (!set.add(lastseenmessages$entry.profileId())) {
            return true;
         }
      }

      return false;
   }

   private int calculateIndices(List<LastSeenMessages.Entry> p_242209_, int[] p_242285_, @Nullable LastSeenMessages.Entry p_242264_) {
      Arrays.fill(p_242285_, Integer.MIN_VALUE);
      List<LastSeenMessages.Entry> list = this.lastSeenMessages.entries();
      int i = list.size();

      for(int j = i - 1; j >= 0; --j) {
         int k = p_242209_.indexOf(list.get(j));
         if (k != -1) {
            p_242285_[k] = -j - 1;
         }
      }

      int j1 = Integer.MIN_VALUE;
      int k1 = this.pendingEntries.size();

      for(int l = 0; l < k1; ++l) {
         LastSeenMessages.Entry lastseenmessages$entry = this.pendingEntries.get(l);
         int i1 = p_242209_.indexOf(lastseenmessages$entry);
         if (i1 != -1) {
            p_242285_[i1] = l;
         }

         if (lastseenmessages$entry.equals(p_242264_)) {
            j1 = l;
         }
      }

      return j1;
   }

   public Set<LastSeenMessagesValidator.ErrorCondition> validateAndUpdate(LastSeenMessages.Update p_242403_) {
      EnumSet<LastSeenMessagesValidator.ErrorCondition> enumset = EnumSet.noneOf(LastSeenMessagesValidator.ErrorCondition.class);
      LastSeenMessages lastseenmessages = p_242403_.lastSeen();
      LastSeenMessages.Entry lastseenmessages$entry = p_242403_.lastReceived().orElse((LastSeenMessages.Entry)null);
      List<LastSeenMessages.Entry> list = lastseenmessages.entries();
      int i = this.lastSeenMessages.entries().size();
      int j = Integer.MIN_VALUE;
      int k = list.size();
      if (k < i) {
         enumset.add(LastSeenMessagesValidator.ErrorCondition.REMOVED_MESSAGES);
      }

      int[] aint = new int[k];
      int l = this.calculateIndices(list, aint, lastseenmessages$entry);

      for(int i1 = k - 1; i1 >= 0; --i1) {
         int j1 = aint[i1];
         if (j1 != Integer.MIN_VALUE) {
            if (j1 < j) {
               enumset.add(LastSeenMessagesValidator.ErrorCondition.OUT_OF_ORDER);
            } else {
               j = j1;
            }
         } else {
            enumset.add(LastSeenMessagesValidator.ErrorCondition.UNKNOWN_MESSAGES);
         }
      }

      if (lastseenmessages$entry != null) {
         if (l != Integer.MIN_VALUE && l >= j) {
            j = l;
         } else {
            enumset.add(LastSeenMessagesValidator.ErrorCondition.UNKNOWN_MESSAGES);
         }
      }

      if (j >= 0) {
         this.pendingEntries.removeElements(0, j + 1);
      }

      if (this.hasDuplicateProfiles(lastseenmessages)) {
         enumset.add(LastSeenMessagesValidator.ErrorCondition.DUPLICATED_PROFILES);
      }

      this.lastSeenMessages = lastseenmessages;
      return enumset;
   }

   public static enum ErrorCondition {
      OUT_OF_ORDER("messages received out of order"),
      DUPLICATED_PROFILES("multiple entries for single profile"),
      UNKNOWN_MESSAGES("unknown message"),
      REMOVED_MESSAGES("previously present messages removed from context");

      private final String message;

      private ErrorCondition(String p_242324_) {
         this.message = p_242324_;
      }

      public String message() {
         return this.message;
      }
   }
}