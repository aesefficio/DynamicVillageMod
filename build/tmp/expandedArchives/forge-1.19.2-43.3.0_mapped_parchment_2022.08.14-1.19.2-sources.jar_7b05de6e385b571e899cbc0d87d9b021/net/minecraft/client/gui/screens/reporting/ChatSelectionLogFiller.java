package net.minecraft.client.gui.screens.reporting;

import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.Predicate;
import net.minecraft.client.multiplayer.chat.ChatLog;
import net.minecraft.client.multiplayer.chat.LoggedChatMessage;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ChatSelectionLogFiller<T extends LoggedChatMessage> {
   private static final int CONTEXT_FOLDED_SIZE = 4;
   private final ChatLog log;
   private final Predicate<T> canReport;
   private int nextMessageId;
   final Class<T> tClass;

   public ChatSelectionLogFiller(ChatLog pLog, Predicate<T> pCanReport, Class<T> pTClass) {
      this.log = pLog;
      this.canReport = pCanReport;
      this.nextMessageId = pLog.newest();
      this.tClass = pTClass;
   }

   public void fillNextPage(int p_239016_, ChatSelectionLogFiller.Output<T> pOutput) {
      int i = 0;

      while(i < p_239016_) {
         ChatLogSegmenter.Results<T> results = this.nextSegment();
         if (results == null) {
            break;
         }

         if (results.type().foldable()) {
            i += this.addFoldedMessagesTo(results.messages(), pOutput);
         } else {
            pOutput.acceptMessages(results.messages());
            i += results.messages().size();
         }
      }

   }

   private int addFoldedMessagesTo(List<ChatLog.Entry<T>> pEntries, ChatSelectionLogFiller.Output<T> pOutput) {
      int i = 8;
      if (pEntries.size() > 8) {
         int j = pEntries.size() - 8;
         pOutput.acceptMessages(pEntries.subList(0, 4));
         pOutput.acceptDivider(Component.translatable("gui.chatSelection.fold", j));
         pOutput.acceptMessages(pEntries.subList(pEntries.size() - 4, pEntries.size()));
         return 9;
      } else {
         pOutput.acceptMessages(pEntries);
         return pEntries.size();
      }
   }

   @Nullable
   private ChatLogSegmenter.@Nullable Results<T> nextSegment() {
      ChatLogSegmenter<T> chatlogsegmenter = new ChatLogSegmenter<>((p_242051_) -> {
         return this.getMessageType(p_242051_.event());
      });
      OptionalInt optionalint = this.log.selectBefore(this.nextMessageId).entries().map((p_242687_) -> {
         return p_242687_.tryCast(this.tClass);
      }).filter(Objects::nonNull).takeWhile(chatlogsegmenter::accept).mapToInt(ChatLog.Entry::id).reduce((p_240038_, p_240039_) -> {
         return p_240039_;
      });
      if (optionalint.isPresent()) {
         this.nextMessageId = this.log.before(optionalint.getAsInt());
      }

      return chatlogsegmenter.build();
   }

   private ChatLogSegmenter.MessageType getMessageType(T p_242252_) {
      return this.canReport.test(p_242252_) ? ChatLogSegmenter.MessageType.REPORTABLE : ChatLogSegmenter.MessageType.CONTEXT;
   }

   @OnlyIn(Dist.CLIENT)
   public interface Output<T extends LoggedChatMessage> {
      default void acceptMessages(Iterable<ChatLog.Entry<T>> pEntries) {
         for(ChatLog.Entry<T> entry : pEntries) {
            this.acceptMessage(entry.id(), entry.event());
         }

      }

      void acceptMessage(int pChatId, T pPlayer);

      void acceptDivider(Component pText);
   }
}