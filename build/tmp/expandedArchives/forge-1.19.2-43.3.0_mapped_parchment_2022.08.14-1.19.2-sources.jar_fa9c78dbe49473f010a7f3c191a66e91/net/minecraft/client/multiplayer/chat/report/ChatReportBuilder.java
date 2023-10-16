package net.minecraft.client.multiplayer.chat.report;

import com.mojang.authlib.minecraft.report.AbuseReport;
import com.mojang.authlib.minecraft.report.AbuseReportLimits;
import com.mojang.authlib.minecraft.report.ReportChatMessage;
import com.mojang.authlib.minecraft.report.ReportChatMessageBody;
import com.mojang.authlib.minecraft.report.ReportChatMessageContent;
import com.mojang.authlib.minecraft.report.ReportChatMessageHeader;
import com.mojang.authlib.minecraft.report.ReportEvidence;
import com.mojang.authlib.minecraft.report.ReportedEntity;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayPriorityQueue;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntComparators;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntPriorityQueue;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.multiplayer.chat.ChatLog;
import net.minecraft.client.multiplayer.chat.LoggedChatEvent;
import net.minecraft.client.multiplayer.chat.LoggedChatMessage;
import net.minecraft.client.multiplayer.chat.LoggedChatMessageLink;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.LastSeenMessages;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.chat.SignedMessageBody;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ChatReportBuilder {
   private final UUID reportId;
   private final Instant createdAt;
   private final UUID reportedProfileId;
   private final AbuseReportLimits limits;
   private final IntSet reportedMessages = new IntOpenHashSet();
   private String comments = "";
   @Nullable
   private ReportReason reason;

   private ChatReportBuilder(UUID p_239204_, Instant p_239205_, UUID p_239206_, AbuseReportLimits p_239207_) {
      this.reportId = p_239204_;
      this.createdAt = p_239205_;
      this.reportedProfileId = p_239206_;
      this.limits = p_239207_;
   }

   public ChatReportBuilder(UUID p_239528_, AbuseReportLimits p_239529_) {
      this(UUID.randomUUID(), Instant.now(), p_239528_, p_239529_);
   }

   public void setComments(String p_239080_) {
      this.comments = p_239080_;
   }

   public void setReason(ReportReason p_239098_) {
      this.reason = p_239098_;
   }

   public void toggleReported(int p_239052_) {
      if (this.reportedMessages.contains(p_239052_)) {
         this.reportedMessages.remove(p_239052_);
      } else if (this.reportedMessages.size() < this.limits.maxReportedMessageCount()) {
         this.reportedMessages.add(p_239052_);
      }

   }

   public UUID reportedProfileId() {
      return this.reportedProfileId;
   }

   public IntSet reportedMessages() {
      return this.reportedMessages;
   }

   public String comments() {
      return this.comments;
   }

   @Nullable
   public ReportReason reason() {
      return this.reason;
   }

   public boolean isReported(int p_243333_) {
      return this.reportedMessages.contains(p_243333_);
   }

   @Nullable
   public ChatReportBuilder.CannotBuildReason checkBuildable() {
      if (this.reportedMessages.isEmpty()) {
         return ChatReportBuilder.CannotBuildReason.NO_REPORTED_MESSAGES;
      } else if (this.reportedMessages.size() > this.limits.maxReportedMessageCount()) {
         return ChatReportBuilder.CannotBuildReason.TOO_MANY_MESSAGES;
      } else if (this.reason == null) {
         return ChatReportBuilder.CannotBuildReason.NO_REASON;
      } else {
         return this.comments.length() > this.limits.maxOpinionCommentsLength() ? ChatReportBuilder.CannotBuildReason.COMMENTS_TOO_LONG : null;
      }
   }

   public Either<ChatReportBuilder.Result, ChatReportBuilder.CannotBuildReason> build(ReportingContext p_240129_) {
      ChatReportBuilder.CannotBuildReason chatreportbuilder$cannotbuildreason = this.checkBuildable();
      if (chatreportbuilder$cannotbuildreason != null) {
         return Either.right(chatreportbuilder$cannotbuildreason);
      } else {
         String s = Objects.requireNonNull(this.reason).backendName();
         ReportEvidence reportevidence = this.buildEvidence(p_240129_.chatLog());
         ReportedEntity reportedentity = new ReportedEntity(this.reportedProfileId);
         AbuseReport abusereport = new AbuseReport(this.comments, s, reportevidence, reportedentity, this.createdAt);
         return Either.left(new ChatReportBuilder.Result(this.reportId, abusereport));
      }
   }

   private ReportEvidence buildEvidence(ChatLog p_239183_) {
      Int2ObjectSortedMap<ReportChatMessage> int2objectsortedmap = new Int2ObjectRBTreeMap<>();
      this.reportedMessages.forEach((p_242060_) -> {
         Int2ObjectMap<LoggedChatMessage.Player> int2objectmap = collectReferencedContext(p_239183_, p_242060_, this.limits);
         Set<UUID> set = new ObjectOpenHashSet<>();

         for(Int2ObjectMap.Entry<LoggedChatMessage.Player> entry : Int2ObjectMaps.fastIterable(int2objectmap)) {
            int i = entry.getIntKey();
            LoggedChatMessage.Player loggedchatmessage$player = entry.getValue();
            int2objectsortedmap.put(i, this.buildReportedChatMessage(i, loggedchatmessage$player));
            set.add(loggedchatmessage$player.profileId());
         }

         for(UUID uuid : set) {
            this.chainForPlayer(p_239183_, int2objectmap, uuid).forEach((p_242067_) -> {
               LoggedChatMessageLink loggedchatmessagelink = p_242067_.event();
               if (loggedchatmessagelink instanceof LoggedChatMessage.Player loggedchatmessage$player1) {
                  int2objectsortedmap.putIfAbsent(p_242067_.id(), this.buildReportedChatMessage(p_242067_.id(), loggedchatmessage$player1));
               } else {
                  int2objectsortedmap.putIfAbsent(p_242067_.id(), this.buildReportedChatHeader(loggedchatmessagelink));
               }

            });
         }

      });
      return new ReportEvidence(new ArrayList<>(int2objectsortedmap.values()));
   }

   private Stream<ChatLog.Entry<LoggedChatMessageLink>> chainForPlayer(ChatLog p_242368_, Int2ObjectMap<LoggedChatMessage.Player> p_242153_, UUID p_242301_) {
      int i = Integer.MAX_VALUE;
      int j = Integer.MIN_VALUE;

      for(Int2ObjectMap.Entry<LoggedChatMessage.Player> entry : Int2ObjectMaps.fastIterable(p_242153_)) {
         LoggedChatMessage.Player loggedchatmessage$player = entry.getValue();
         if (loggedchatmessage$player.profileId().equals(p_242301_)) {
            int k = entry.getIntKey();
            i = Math.min(i, k);
            j = Math.max(j, k);
         }
      }

      return p_242368_.selectBetween(i, j).entries().map((p_242069_) -> {
         return p_242069_.tryCast(LoggedChatMessageLink.class);
      }).filter(Objects::nonNull).filter((p_242055_) -> {
         return p_242055_.event().header().sender().equals(p_242301_);
      });
   }

   private static Int2ObjectMap<LoggedChatMessage.Player> collectReferencedContext(ChatLog p_242227_, int p_242178_, AbuseReportLimits p_242421_) {
      int i = p_242421_.leadingContextMessageCount() + 1;
      Int2ObjectMap<LoggedChatMessage.Player> int2objectmap = new Int2ObjectOpenHashMap<>();
      walkMessageReferenceGraph(p_242227_, p_242178_, (p_242693_, p_242694_) -> {
         int2objectmap.put(p_242693_, p_242694_);
         return int2objectmap.size() < i;
      });
      trailingContext(p_242227_, p_242178_, p_242421_.trailingContextMessageCount()).forEach((p_242057_) -> {
         int2objectmap.put(p_242057_.id(), p_242057_.event());
      });
      return int2objectmap;
   }

   private static Stream<ChatLog.Entry<LoggedChatMessage.Player>> trailingContext(ChatLog p_242447_, int p_242340_, int p_242471_) {
      return p_242447_.selectAfter(p_242447_.after(p_242340_)).entries().map((p_242065_) -> {
         return p_242065_.tryCast(LoggedChatMessage.Player.class);
      }).filter(Objects::nonNull).limit((long)p_242471_);
   }

   private static void walkMessageReferenceGraph(ChatLog p_242430_, int p_242234_, ChatReportBuilder.ReferencedMessageVisitor p_242920_) {
      IntPriorityQueue intpriorityqueue = new IntArrayPriorityQueue(IntComparators.OPPOSITE_COMPARATOR);
      intpriorityqueue.enqueue(p_242234_);
      IntSet intset = new IntOpenHashSet();
      intset.add(p_242234_);

      while(!intpriorityqueue.isEmpty()) {
         int i = intpriorityqueue.dequeueInt();
         LoggedChatEvent loggedchatevent = p_242430_.lookup(i);
         if (loggedchatevent instanceof LoggedChatMessage.Player loggedchatmessage$player) {
            if (!p_242920_.accept(i, loggedchatmessage$player)) {
               break;
            }

            for(int j : messageReferences(p_242430_, i, loggedchatmessage$player.message())) {
               if (intset.add(j)) {
                  intpriorityqueue.enqueue(j);
               }
            }
         }
      }

   }

   private static IntCollection messageReferences(ChatLog p_242933_, int p_242860_, PlayerChatMessage p_242922_) {
      Set<MessageSignature> set = p_242922_.signedBody().lastSeen().entries().stream().map(LastSeenMessages.Entry::lastSignature).collect(Collectors.toCollection(ObjectOpenHashSet::new));
      MessageSignature messagesignature = p_242922_.signedHeader().previousSignature();
      if (messagesignature != null) {
         set.add(messagesignature);
      }

      IntList intlist = new IntArrayList();
      Iterator<ChatLog.Entry<LoggedChatEvent>> iterator = p_242933_.selectBefore(p_242860_).entries().iterator();

      while(iterator.hasNext() && !set.isEmpty()) {
         ChatLog.Entry<LoggedChatEvent> entry = iterator.next();
         LoggedChatEvent loggedchatevent = entry.event();
         if (loggedchatevent instanceof LoggedChatMessage.Player loggedchatmessage$player) {
            if (set.remove(loggedchatmessage$player.headerSignature())) {
               intlist.add(entry.id());
            }
         }
      }

      return intlist;
   }

   private ReportChatMessage buildReportedChatMessage(int p_242213_, LoggedChatMessage.Player p_242239_) {
      PlayerChatMessage playerchatmessage = p_242239_.message();
      SignedMessageBody signedmessagebody = playerchatmessage.signedBody();
      Instant instant = playerchatmessage.timeStamp();
      long i = playerchatmessage.salt();
      ByteBuffer bytebuffer = playerchatmessage.headerSignature().asByteBuffer();
      ByteBuffer bytebuffer1 = Util.mapNullable(playerchatmessage.signedHeader().previousSignature(), MessageSignature::asByteBuffer);
      ByteBuffer bytebuffer2 = ByteBuffer.wrap(signedmessagebody.hash().asBytes());
      ReportChatMessageContent reportchatmessagecontent = new ReportChatMessageContent(playerchatmessage.signedContent().plain(), playerchatmessage.signedContent().isDecorated() ? encodeComponent(playerchatmessage.signedContent().decorated()) : null);
      String s = playerchatmessage.unsignedContent().map(ChatReportBuilder::encodeComponent).orElse((String)null);
      List<ReportChatMessageBody.LastSeenSignature> list = signedmessagebody.lastSeen().entries().stream().map((p_242068_) -> {
         return new ReportChatMessageBody.LastSeenSignature(p_242068_.profileId(), p_242068_.lastSignature().asByteBuffer());
      }).toList();
      return new ReportChatMessage(new ReportChatMessageHeader(bytebuffer1, p_242239_.profileId(), bytebuffer2, bytebuffer), new ReportChatMessageBody(instant, i, list, reportchatmessagecontent), s, this.isReported(p_242213_));
   }

   private ReportChatMessage buildReportedChatHeader(LoggedChatMessageLink p_242212_) {
      ByteBuffer bytebuffer = p_242212_.headerSignature().asByteBuffer();
      ByteBuffer bytebuffer1 = Util.mapNullable(p_242212_.header().previousSignature(), MessageSignature::asByteBuffer);
      return new ReportChatMessage(new ReportChatMessageHeader(bytebuffer1, p_242212_.header().sender(), ByteBuffer.wrap(p_242212_.bodyDigest()), bytebuffer), (ReportChatMessageBody)null, (String)null, false);
   }

   private static String encodeComponent(Component p_239803_) {
      return Component.Serializer.toStableJson(p_239803_);
   }

   public ChatReportBuilder copy() {
      ChatReportBuilder chatreportbuilder = new ChatReportBuilder(this.reportId, this.createdAt, this.reportedProfileId, this.limits);
      chatreportbuilder.reportedMessages.addAll(this.reportedMessages);
      chatreportbuilder.comments = this.comments;
      chatreportbuilder.reason = this.reason;
      return chatreportbuilder;
   }

   @OnlyIn(Dist.CLIENT)
   public static record CannotBuildReason(Component message) {
      public static final ChatReportBuilder.CannotBuildReason NO_REASON = new ChatReportBuilder.CannotBuildReason(Component.translatable("gui.chatReport.send.no_reason"));
      public static final ChatReportBuilder.CannotBuildReason NO_REPORTED_MESSAGES = new ChatReportBuilder.CannotBuildReason(Component.translatable("gui.chatReport.send.no_reported_messages"));
      public static final ChatReportBuilder.CannotBuildReason TOO_MANY_MESSAGES = new ChatReportBuilder.CannotBuildReason(Component.translatable("gui.chatReport.send.too_many_messages"));
      public static final ChatReportBuilder.CannotBuildReason COMMENTS_TOO_LONG = new ChatReportBuilder.CannotBuildReason(Component.translatable("gui.chatReport.send.comments_too_long"));
   }

   @OnlyIn(Dist.CLIENT)
   interface ReferencedMessageVisitor {
      boolean accept(int p_243336_, LoggedChatMessage.Player p_242944_);
   }

   @OnlyIn(Dist.CLIENT)
   public static record Result(UUID id, AbuseReport report) {
   }
}