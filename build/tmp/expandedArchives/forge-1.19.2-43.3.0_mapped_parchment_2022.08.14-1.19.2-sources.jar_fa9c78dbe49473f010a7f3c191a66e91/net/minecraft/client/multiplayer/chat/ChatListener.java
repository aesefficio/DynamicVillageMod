package net.minecraft.client.multiplayer.chat;

import com.google.common.collect.Queues;
import com.mojang.authlib.GameProfile;
import java.time.Instant;
import java.util.Deque;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FilterMask;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.MessageSigner;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.chat.SignedMessageHeader;
import net.minecraft.network.chat.SignedMessageValidator;
import net.minecraft.util.StringDecomposer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;

@OnlyIn(Dist.CLIENT)
public class ChatListener {
   private static final Component CHAT_VALIDATION_FAILED_ERROR = Component.translatable("multiplayer.disconnect.chat_validation_failed");
   private final Minecraft minecraft;
   private final Deque<ChatListener.Message> delayedMessageQueue = Queues.newArrayDeque();
   private long messageDelay;
   private long previousMessageTime;

   public ChatListener(Minecraft pMinecraft) {
      this.minecraft = pMinecraft;
   }

   public void tick() {
      if (this.messageDelay != 0L) {
         if (Util.getMillis() >= this.previousMessageTime + this.messageDelay) {
            for(ChatListener.Message chatlistener$message = this.delayedMessageQueue.poll(); chatlistener$message != null && !chatlistener$message.accept(); chatlistener$message = this.delayedMessageQueue.poll()) {
            }
         }

      }
   }

   public void setMessageDelay(double pDelaySeconds) {
      long i = (long)(pDelaySeconds * 1000.0D);
      if (i == 0L && this.messageDelay > 0L) {
         this.delayedMessageQueue.forEach(ChatListener.Message::accept);
         this.delayedMessageQueue.clear();
      }

      this.messageDelay = i;
   }

   public void acceptNextDelayedMessage() {
      this.delayedMessageQueue.remove().accept();
   }

   public long queueSize() {
      return this.delayedMessageQueue.stream().filter(ChatListener.Message::isVisible).count();
   }

   public void clearQueue() {
      this.delayedMessageQueue.forEach((p_242052_) -> {
         p_242052_.remove();
         p_242052_.accept();
      });
      this.delayedMessageQueue.clear();
   }

   public boolean removeFromDelayedMessageQueue(MessageSignature p_241445_) {
      for(ChatListener.Message chatlistener$message : this.delayedMessageQueue) {
         if (chatlistener$message.removeIfSignatureMatches(p_241445_)) {
            return true;
         }
      }

      return false;
   }

   private boolean willDelayMessages() {
      return this.messageDelay > 0L && Util.getMillis() < this.previousMessageTime + this.messageDelay;
   }

   private void handleMessage(ChatListener.Message p_241312_) {
      if (this.willDelayMessages()) {
         this.delayedMessageQueue.add(p_241312_);
      } else {
         p_241312_.accept();
      }

   }

   public void handleChatMessage(final PlayerChatMessage p_241568_, final ChatType.Bound p_241361_) {
      final boolean flag = this.minecraft.options.onlyShowSecureChat().get();
      final PlayerChatMessage playerchatmessage = flag ? p_241568_.removeUnsignedContent() : p_241568_;
      final Component component = p_241361_.decorate(playerchatmessage.serverContent());
      MessageSigner messagesigner = p_241568_.signer();
      if (!messagesigner.isSystem()) {
         final PlayerInfo playerinfo = this.resolveSenderPlayer(messagesigner.profileId());
         final Instant instant = Instant.now();
         this.handleMessage(new ChatListener.Message() {
            private boolean removed;

            public boolean accept() {
               if (this.removed) {
                  byte[] abyte = p_241568_.signedBody().hash().asBytes();
                  ChatListener.this.processPlayerChatHeader(p_241568_.signedHeader(), p_241568_.headerSignature(), abyte);
                  return false;
               } else {
                  return ChatListener.this.processPlayerChatMessage(p_241361_, p_241568_, component, playerinfo, flag, instant);
               }
            }

            public boolean removeIfSignatureMatches(MessageSignature p_242335_) {
               if (p_241568_.headerSignature().equals(p_242335_)) {
                  this.removed = true;
                  return true;
               } else {
                  return false;
               }
            }

            public void remove() {
               this.removed = true;
            }

            public boolean isVisible() {
               return !this.removed;
            }
         });
      } else {
         this.handleMessage(new ChatListener.Message() {
            public boolean accept() {
               return ChatListener.this.processNonPlayerChatMessage(p_241361_, playerchatmessage, component);
            }

            public boolean isVisible() {
               return true;
            }
         });
      }

   }

   public void handleChatHeader(SignedMessageHeader p_241319_, MessageSignature p_241390_, byte[] p_241463_) {
      this.handleMessage(() -> {
         return this.processPlayerChatHeader(p_241319_, p_241390_, p_241463_);
      });
   }

   boolean processPlayerChatMessage(ChatType.Bound p_242406_, PlayerChatMessage p_242174_, Component p_242417_, @Nullable PlayerInfo p_242459_, boolean p_242346_, Instant p_242392_) {
      boolean flag = this.showMessageToPlayer(p_242406_, p_242174_, p_242417_, p_242459_, p_242346_, p_242392_);
      ClientPacketListener clientpacketlistener = this.minecraft.getConnection();
      if (clientpacketlistener != null) {
         clientpacketlistener.markMessageAsProcessed(p_242174_, flag);
      }

      return flag;
   }

   private boolean showMessageToPlayer(ChatType.Bound p_242290_, PlayerChatMessage p_242317_, Component p_243337_, @Nullable PlayerInfo p_242267_, boolean p_242247_, Instant p_242230_) {
      ChatTrustLevel chattrustlevel = this.evaluateTrustLevel(p_242317_, p_243337_, p_242267_, p_242230_);
      if (chattrustlevel == ChatTrustLevel.BROKEN_CHAIN) {
         this.onChatChainBroken();
         return true;
      } else if (p_242247_ && chattrustlevel.isNotSecure()) {
         return false;
      } else if (!this.minecraft.isBlocked(p_242317_.signer().profileId()) && !p_242317_.isFullyFiltered()) {
         GuiMessageTag guimessagetag = chattrustlevel.createTag(p_242317_);
         MessageSignature messagesignature = p_242317_.headerSignature();
         FilterMask filtermask = p_242317_.filterMask();
         if (filtermask.isEmpty()) {
            Component forgeComponent = net.minecraftforge.client.ForgeHooksClient.onClientChat(p_242290_, p_243337_, p_242317_, p_242317_.signer());
            if (forgeComponent == null) return false;
            this.minecraft.gui.getChat().addMessage(forgeComponent, messagesignature, guimessagetag);
            this.narrateChatMessage(p_242290_, p_242317_.serverContent());
         } else {
            Component component = filtermask.apply(p_242317_.signedContent());
            if (component != null) {
               Component forgeComponent = net.minecraftforge.client.ForgeHooksClient.onClientChat(p_242290_, p_242290_.decorate(component), p_242317_, p_242317_.signer());
               if (forgeComponent == null) return false;
               this.minecraft.gui.getChat().addMessage(forgeComponent, messagesignature, guimessagetag);
               this.narrateChatMessage(p_242290_, component);
            }
         }

         this.logPlayerMessage(p_242317_, p_242290_, p_242267_, chattrustlevel);
         this.previousMessageTime = Util.getMillis();
         return true;
      } else {
         return false;
      }
   }

   boolean processNonPlayerChatMessage(ChatType.Bound p_241518_, PlayerChatMessage p_241542_, Component p_241510_) {
      Component forgeComponent = net.minecraftforge.client.ForgeHooksClient.onClientChat(p_241518_, p_241510_, p_241542_, p_241542_.signer());
      if (forgeComponent == null) return false;
      this.minecraft.gui.getChat().addMessage(forgeComponent);
      this.narrateChatMessage(p_241518_, p_241542_.serverContent());
      this.logSystemMessage(p_241510_, p_241542_.timeStamp());
      this.previousMessageTime = Util.getMillis();
      return true;
   }

   boolean processPlayerChatHeader(SignedMessageHeader p_241363_, MessageSignature p_241535_, byte[] p_241500_) {
      PlayerInfo playerinfo = this.resolveSenderPlayer(p_241363_.sender());
      if (playerinfo != null) {
         SignedMessageValidator.State signedmessagevalidator$state = playerinfo.getMessageValidator().validateHeader(p_241363_, p_241535_, p_241500_);
         if (signedmessagevalidator$state == SignedMessageValidator.State.BROKEN_CHAIN) {
            this.onChatChainBroken();
            return true;
         }
      }

      this.logPlayerHeader(p_241363_, p_241535_, p_241500_);
      return false;
   }

   private void onChatChainBroken() {
      ClientPacketListener clientpacketlistener = this.minecraft.getConnection();
      if (clientpacketlistener != null) {
         clientpacketlistener.getConnection().disconnect(CHAT_VALIDATION_FAILED_ERROR);
      }

   }

   private void narrateChatMessage(ChatType.Bound p_241352_, Component p_243262_) {
      this.minecraft.getNarrator().sayChatNow(() -> {
         return p_241352_.decorateNarration(p_243262_);
      });
   }

   private ChatTrustLevel evaluateTrustLevel(PlayerChatMessage p_242369_, Component p_242452_, @Nullable PlayerInfo p_242405_, Instant p_242401_) {
      return this.isSenderLocalPlayer(p_242369_.signer().profileId()) ? ChatTrustLevel.SECURE : ChatTrustLevel.evaluate(p_242369_, p_242452_, p_242405_, p_242401_);
   }

   private void logPlayerMessage(PlayerChatMessage p_241337_, ChatType.Bound p_241355_, @Nullable PlayerInfo p_241489_, ChatTrustLevel p_241528_) {
      GameProfile gameprofile;
      if (p_241489_ != null) {
         gameprofile = p_241489_.getProfile();
      } else {
         gameprofile = new GameProfile(p_241337_.signer().profileId(), p_241355_.name().getString());
      }

      ChatLog chatlog = this.minecraft.getReportingContext().chatLog();
      chatlog.push(LoggedChatMessage.player(gameprofile, p_241355_.name(), p_241337_, p_241528_));
   }

   private void logSystemMessage(Component p_240609_, Instant p_240541_) {
      ChatLog chatlog = this.minecraft.getReportingContext().chatLog();
      chatlog.push(LoggedChatMessage.system(p_240609_, p_240541_));
   }

   private void logPlayerHeader(SignedMessageHeader p_241328_, MessageSignature p_241317_, byte[] p_241565_) {
      ChatLog chatlog = this.minecraft.getReportingContext().chatLog();
      chatlog.push(LoggedChatMessageLink.header(p_241328_, p_241317_, p_241565_));
   }

   @Nullable
   private PlayerInfo resolveSenderPlayer(UUID p_241471_) {
      ClientPacketListener clientpacketlistener = this.minecraft.getConnection();
      return clientpacketlistener != null ? clientpacketlistener.getPlayerInfo(p_241471_) : null;
   }

   public void handleSystemMessage(Component p_240522_, boolean p_240642_) {
      if (!this.minecraft.options.hideMatchedNames().get() || !this.minecraft.isBlocked(this.guessChatUUID(p_240522_))) {
         p_240522_ = net.minecraftforge.client.ForgeHooksClient.onClientSystemChat(p_240522_, p_240642_);
         if (p_240522_ == null) return;
         if (p_240642_) {
            this.minecraft.gui.setOverlayMessage(p_240522_, false);
         } else {
            this.minecraft.gui.getChat().addMessage(p_240522_);
            this.logSystemMessage(p_240522_, Instant.now());
         }

         this.minecraft.getNarrator().sayNow(p_240522_);
      }
   }

   private UUID guessChatUUID(Component p_240595_) {
      String s = StringDecomposer.getPlainText(p_240595_);
      String s1 = StringUtils.substringBetween(s, "<", ">");
      return s1 == null ? Util.NIL_UUID : this.minecraft.getPlayerSocialManager().getDiscoveredUUID(s1);
   }

   private boolean isSenderLocalPlayer(UUID p_241343_) {
      if (this.minecraft.isLocalServer() && this.minecraft.player != null) {
         UUID uuid = this.minecraft.player.getGameProfile().getId();
         return uuid.equals(p_241343_);
      } else {
         return false;
      }
   }

   @OnlyIn(Dist.CLIENT)
   interface Message {
      default boolean removeIfSignatureMatches(MessageSignature p_242379_) {
         return false;
      }

      default void remove() {
      }

      boolean accept();

      default boolean isVisible() {
         return false;
      }
   }
}
