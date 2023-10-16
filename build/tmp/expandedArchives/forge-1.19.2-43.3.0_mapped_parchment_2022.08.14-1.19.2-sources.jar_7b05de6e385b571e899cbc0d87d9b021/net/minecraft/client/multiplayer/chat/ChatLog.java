package net.minecraft.client.multiplayer.chat;

import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Collection;
import java.util.Objects;
import java.util.PrimitiveIterator;
import java.util.Spliterators;
import java.util.function.IntUnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface ChatLog {
   int NO_MESSAGE = -1;

   void push(LoggedChatEvent p_242319_);

   @Nullable
   LoggedChatEvent lookup(int p_239050_);

   @Nullable
   default ChatLog.Entry<LoggedChatEvent> lookupEntry(int p_242449_) {
      LoggedChatEvent loggedchatevent = this.lookup(p_242449_);
      return loggedchatevent != null ? new ChatLog.Entry<>(p_242449_, loggedchatevent) : null;
   }

   default boolean contains(int p_238951_) {
      return this.lookup(p_238951_) != null;
   }

   int offset(int p_239142_, int p_239143_);

   default int before(int p_239680_) {
      return this.offset(p_239680_, -1);
   }

   default int after(int p_239584_) {
      return this.offset(p_239584_, 1);
   }

   int newest();

   int oldest();

   default ChatLog.Selection selectAll() {
      return this.selectAfter(this.oldest());
   }

   default ChatLog.Selection selectAllDescending() {
      return this.selectBefore(this.newest());
   }

   default ChatLog.Selection selectAfter(int p_239515_) {
      return this.selectSequence(p_239515_, this::after);
   }

   default ChatLog.Selection selectBefore(int p_238954_) {
      return this.selectSequence(p_238954_, this::before);
   }

   default ChatLog.Selection selectBetween(int p_239413_, int p_239414_) {
      return this.contains(p_239413_) && this.contains(p_239414_) ? this.selectSequence(p_239413_, (p_239928_) -> {
         return p_239928_ == p_239414_ ? -1 : this.after(p_239928_);
      }) : this.selectNone();
   }

   default ChatLog.Selection selectSequence(final int p_239757_, final IntUnaryOperator p_239758_) {
      return !this.contains(p_239757_) ? this.selectNone() : new ChatLog.Selection(this, new PrimitiveIterator.OfInt() {
         private int nextId = p_239757_;

         public int nextInt() {
            int i = this.nextId;
            this.nextId = p_239758_.applyAsInt(i);
            return i;
         }

         public boolean hasNext() {
            return this.nextId != -1;
         }
      });
   }

   private ChatLog.Selection selectNone() {
      return new ChatLog.Selection(this, IntList.of().iterator());
   }

   @OnlyIn(Dist.CLIENT)
   public static record Entry<T extends LoggedChatEvent>(int id, T event) {
      @Nullable
      public <U extends LoggedChatEvent> ChatLog.Entry<U> tryCast(Class<U> p_242327_) {
         return p_242327_.isInstance(this.event) ? new ChatLog.Entry<>(this.id, p_242327_.cast(this.event)) : null;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class Selection {
      private static final int CHARACTERISTICS = 1041;
      private final ChatLog log;
      private final PrimitiveIterator.OfInt ids;

      Selection(ChatLog p_239661_, PrimitiveIterator.OfInt p_239662_) {
         this.log = p_239661_;
         this.ids = p_239662_;
      }

      public IntStream ids() {
         return StreamSupport.intStream(Spliterators.spliteratorUnknownSize(this.ids, 1041), false);
      }

      public Stream<LoggedChatEvent> events() {
         return this.ids().mapToObj(this.log::lookup).filter(Objects::nonNull);
      }

      public Collection<GameProfile> reportableGameProfiles() {
         return this.events().map((p_243150_) -> {
            if (p_243150_ instanceof LoggedChatMessage.Player loggedchatmessage$player) {
               if (loggedchatmessage$player.canReport(loggedchatmessage$player.profile().getId())) {
                  return loggedchatmessage$player.profile();
               }
            }

            return null;
         }).filter(Objects::nonNull).distinct().toList();
      }

      public Stream<ChatLog.Entry<LoggedChatEvent>> entries() {
         return this.ids().mapToObj(this.log::lookupEntry).filter(Objects::nonNull);
      }
   }
}