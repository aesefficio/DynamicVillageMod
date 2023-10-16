package net.minecraft.commands.arguments;

import com.google.common.collect.Lists;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSigningContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.network.chat.ChatDecorator;
import net.minecraft.network.chat.ChatMessageContent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.FilteredText;
import net.minecraft.server.players.PlayerList;
import org.slf4j.Logger;

public class MessageArgument implements SignedArgument<MessageArgument.Message> {
   private static final Collection<String> EXAMPLES = Arrays.asList("Hello world!", "foo", "@e", "Hello @p :)");
   private static final Logger LOGGER = LogUtils.getLogger();

   public static MessageArgument message() {
      return new MessageArgument();
   }

   public static Component getMessage(CommandContext<CommandSourceStack> pContext, String pName) throws CommandSyntaxException {
      MessageArgument.Message messageargument$message = pContext.getArgument(pName, MessageArgument.Message.class);
      return messageargument$message.resolveComponent(pContext.getSource());
   }

   public static MessageArgument.ChatMessage getChatMessage(CommandContext<CommandSourceStack> p_232164_, String p_232165_) throws CommandSyntaxException {
      MessageArgument.Message messageargument$message = p_232164_.getArgument(p_232165_, MessageArgument.Message.class);
      Component component = messageargument$message.resolveComponent(p_232164_.getSource());
      CommandSigningContext commandsigningcontext = p_232164_.getSource().getSigningContext();
      PlayerChatMessage playerchatmessage = commandsigningcontext.getArgument(p_232165_);
      if (playerchatmessage == null) {
         ChatMessageContent chatmessagecontent = new ChatMessageContent(messageargument$message.text, component);
         return new MessageArgument.ChatMessage(PlayerChatMessage.system(chatmessagecontent));
      } else {
         return new MessageArgument.ChatMessage(ChatDecorator.attachIfNotDecorated(playerchatmessage, component));
      }
   }

   public MessageArgument.Message parse(StringReader pReader) throws CommandSyntaxException {
      return MessageArgument.Message.parseText(pReader, true);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   public String getSignableText(MessageArgument.Message p_242423_) {
      return p_242423_.getText();
   }

   public CompletableFuture<Component> resolvePreview(CommandSourceStack p_232147_, MessageArgument.Message p_232148_) throws CommandSyntaxException {
      return p_232148_.resolveDecoratedComponent(p_232147_);
   }

   public Class<MessageArgument.Message> getValueType() {
      return MessageArgument.Message.class;
   }

   static void logResolutionFailure(CommandSourceStack p_232156_, CompletableFuture<?> p_232157_) {
      p_232157_.exceptionally((p_232154_) -> {
         LOGGER.error("Encountered unexpected exception while resolving chat message argument from '{}'", p_232156_.getDisplayName().getString(), p_232154_);
         return null;
      });
   }

   public static record ChatMessage(PlayerChatMessage signedArgument) {
      public void resolve(CommandSourceStack p_242313_, Consumer<PlayerChatMessage> p_242409_) {
         MinecraftServer minecraftserver = p_242313_.getServer();
         p_242313_.getChatMessageChainer().append(() -> {
            CompletableFuture<FilteredText> completablefuture = this.filterPlainText(p_242313_, this.signedArgument.signedContent().plain());
            CompletableFuture<PlayerChatMessage> completablefuture1 = minecraftserver.getChatDecorator().decorate(p_242313_.getPlayer(), this.signedArgument);
            return CompletableFuture.allOf(completablefuture, completablefuture1).thenAcceptAsync((p_243162_) -> {
               PlayerChatMessage playerchatmessage = completablefuture1.join().filter(completablefuture.join().mask());
               p_242409_.accept(playerchatmessage);
            }, minecraftserver);
         });
      }

      private CompletableFuture<FilteredText> filterPlainText(CommandSourceStack p_241399_, String p_241465_) {
         ServerPlayer serverplayer = p_241399_.getPlayer();
         return serverplayer != null && this.signedArgument.hasSignatureFrom(serverplayer.getUUID()) ? serverplayer.getTextFilter().processStreamMessage(p_241465_) : CompletableFuture.completedFuture(FilteredText.passThrough(p_241465_));
      }

      public void consume(CommandSourceStack p_241491_) {
         if (!this.signedArgument.signer().isSystem()) {
            this.resolve(p_241491_, (p_243158_) -> {
               PlayerList playerlist = p_241491_.getServer().getPlayerList();
               playerlist.broadcastMessageHeader(p_243158_, Set.of());
            });
         }

      }
   }

   public static class Message {
      final String text;
      private final MessageArgument.Part[] parts;

      public Message(String pText, MessageArgument.Part[] pParts) {
         this.text = pText;
         this.parts = pParts;
      }

      public String getText() {
         return this.text;
      }

      public MessageArgument.Part[] getParts() {
         return this.parts;
      }

      CompletableFuture<Component> resolveDecoratedComponent(CommandSourceStack p_232195_) throws CommandSyntaxException {
         Component component = this.resolveComponent(p_232195_);
         CompletableFuture<Component> completablefuture = p_232195_.getServer().getChatDecorator().decorate(p_232195_.getPlayer(), component);
         MessageArgument.logResolutionFailure(p_232195_, completablefuture);
         return completablefuture;
      }

      Component resolveComponent(CommandSourceStack p_232197_) throws CommandSyntaxException {
         return this.toComponent(p_232197_, net.minecraftforge.common.ForgeHooks.canUseEntitySelectors(p_232197_));
      }

      /**
       * Converts this message into a text component, replacing any selectors in the text with the actual evaluated
       * selector.
       */
      public Component toComponent(CommandSourceStack pSource, boolean pAllowSelectors) throws CommandSyntaxException {
         if (this.parts.length != 0 && pAllowSelectors) {
            MutableComponent mutablecomponent = Component.literal(this.text.substring(0, this.parts[0].getStart()));
            int i = this.parts[0].getStart();

            for(MessageArgument.Part messageargument$part : this.parts) {
               Component component = messageargument$part.toComponent(pSource);
               if (i < messageargument$part.getStart()) {
                  mutablecomponent.append(this.text.substring(i, messageargument$part.getStart()));
               }

               if (component != null) {
                  mutablecomponent.append(component);
               }

               i = messageargument$part.getEnd();
            }

            if (i < this.text.length()) {
               mutablecomponent.append(this.text.substring(i));
            }

            return mutablecomponent;
         } else {
            return Component.literal(this.text);
         }
      }

      /**
       * Parses a message. The algorithm for this is simply to run though and look for selectors, ignoring any invalid
       * selectors in the text (since players may type e.g. "[@]").
       */
      public static MessageArgument.Message parseText(StringReader pReader, boolean pAllowSelectors) throws CommandSyntaxException {
         String s = pReader.getString().substring(pReader.getCursor(), pReader.getTotalLength());
         if (!pAllowSelectors) {
            pReader.setCursor(pReader.getTotalLength());
            return new MessageArgument.Message(s, new MessageArgument.Part[0]);
         } else {
            List<MessageArgument.Part> list = Lists.newArrayList();
            int i = pReader.getCursor();

            while(true) {
               int j;
               EntitySelector entityselector;
               while(true) {
                  if (!pReader.canRead()) {
                     return new MessageArgument.Message(s, list.toArray(new MessageArgument.Part[0]));
                  }

                  if (pReader.peek() == '@') {
                     j = pReader.getCursor();

                     try {
                        EntitySelectorParser entityselectorparser = new EntitySelectorParser(pReader);
                        entityselector = entityselectorparser.parse();
                        break;
                     } catch (CommandSyntaxException commandsyntaxexception) {
                        if (commandsyntaxexception.getType() != EntitySelectorParser.ERROR_MISSING_SELECTOR_TYPE && commandsyntaxexception.getType() != EntitySelectorParser.ERROR_UNKNOWN_SELECTOR_TYPE) {
                           throw commandsyntaxexception;
                        }

                        pReader.setCursor(j + 1);
                     }
                  } else {
                     pReader.skip();
                  }
               }

               list.add(new MessageArgument.Part(j - i, pReader.getCursor() - i, entityselector));
            }
         }
      }
   }

   public static class Part {
      private final int start;
      private final int end;
      private final EntitySelector selector;

      public Part(int pStart, int pEnd, EntitySelector pSelector) {
         this.start = pStart;
         this.end = pEnd;
         this.selector = pSelector;
      }

      public int getStart() {
         return this.start;
      }

      public int getEnd() {
         return this.end;
      }

      public EntitySelector getSelector() {
         return this.selector;
      }

      /**
       * Runs the selector and returns the component produced by it. This method does not actually appear to ever return
       * null.
       */
      @Nullable
      public Component toComponent(CommandSourceStack pSource) throws CommandSyntaxException {
         return EntitySelector.joinNames(this.selector.findEntities(pSource));
      }
   }
}
