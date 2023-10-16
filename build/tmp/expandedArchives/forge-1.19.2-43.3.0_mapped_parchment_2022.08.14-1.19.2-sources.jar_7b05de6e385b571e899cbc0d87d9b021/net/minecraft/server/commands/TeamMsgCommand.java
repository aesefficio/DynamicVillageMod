package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.OutgoingPlayerChatMessage;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.PlayerTeam;

public class TeamMsgCommand {
   private static final Style SUGGEST_STYLE = Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.type.team.hover"))).withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/teammsg "));
   private static final SimpleCommandExceptionType ERROR_NOT_ON_TEAM = new SimpleCommandExceptionType(Component.translatable("commands.teammsg.failed.noteam"));

   public static void register(CommandDispatcher<CommandSourceStack> pDispatcher) {
      LiteralCommandNode<CommandSourceStack> literalcommandnode = pDispatcher.register(Commands.literal("teammsg").then(Commands.argument("message", MessageArgument.message()).executes((p_139002_) -> {
         MessageArgument.ChatMessage messageargument$chatmessage = MessageArgument.getChatMessage(p_139002_, "message");

         try {
            return sendMessage(p_139002_.getSource(), messageargument$chatmessage);
         } catch (Exception exception) {
            messageargument$chatmessage.consume(p_139002_.getSource());
            throw exception;
         }
      })));
      pDispatcher.register(Commands.literal("tm").redirect(literalcommandnode));
   }

   private static int sendMessage(CommandSourceStack pSource, MessageArgument.ChatMessage pChatMessage) throws CommandSyntaxException {
      Entity entity = pSource.getEntityOrException();
      PlayerTeam playerteam = (PlayerTeam)entity.getTeam();
      if (playerteam == null) {
         throw ERROR_NOT_ON_TEAM.create();
      } else {
         Component component = playerteam.getFormattedDisplayName().withStyle(SUGGEST_STYLE);
         ChatType.Bound chattype$bound = ChatType.bind(ChatType.TEAM_MSG_COMMAND_INCOMING, pSource).withTargetName(component);
         ChatType.Bound chattype$bound1 = ChatType.bind(ChatType.TEAM_MSG_COMMAND_OUTGOING, pSource).withTargetName(component);
         List<ServerPlayer> list = pSource.getServer().getPlayerList().getPlayers().stream().filter((p_242725_) -> {
            return p_242725_ == entity || p_242725_.getTeam() == playerteam;
         }).toList();
         pChatMessage.resolve(pSource, (p_243187_) -> {
            OutgoingPlayerChatMessage outgoingplayerchatmessage = OutgoingPlayerChatMessage.create(p_243187_);
            boolean flag = p_243187_.isFullyFiltered();
            boolean flag1 = false;

            for(ServerPlayer serverplayer : list) {
               ChatType.Bound chattype$bound2 = serverplayer == entity ? chattype$bound1 : chattype$bound;
               boolean flag2 = pSource.shouldFilterMessageTo(serverplayer);
               serverplayer.sendChatMessage(outgoingplayerchatmessage, flag2, chattype$bound2);
               flag1 |= flag && flag2 && serverplayer != entity;
            }

            if (flag1) {
               pSource.sendSystemMessage(PlayerList.CHAT_FILTERED_FULL);
            }

            outgoingplayerchatmessage.sendHeadersToRemainingPlayers(pSource.getServer().getPlayerList());
         });
         return list.size();
      }
   }
}