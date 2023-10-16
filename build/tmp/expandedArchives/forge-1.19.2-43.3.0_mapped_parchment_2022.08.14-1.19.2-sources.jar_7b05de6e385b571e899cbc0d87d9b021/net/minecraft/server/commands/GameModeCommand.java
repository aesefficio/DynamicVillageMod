package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.Collection;
import java.util.Collections;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;

public class GameModeCommand {
   public static final int PERMISSION_LEVEL = 2;

   public static void register(CommandDispatcher<CommandSourceStack> pDispatcher) {
      LiteralArgumentBuilder<CommandSourceStack> literalargumentbuilder = Commands.literal("gamemode").requires((p_137736_) -> {
         return p_137736_.hasPermission(2);
      });

      for(GameType gametype : GameType.values()) {
         literalargumentbuilder.then(Commands.literal(gametype.getName()).executes((p_137743_) -> {
            return setMode(p_137743_, Collections.singleton(p_137743_.getSource().getPlayerOrException()), gametype);
         }).then(Commands.argument("target", EntityArgument.players()).executes((p_137728_) -> {
            return setMode(p_137728_, EntityArgument.getPlayers(p_137728_, "target"), gametype);
         })));
      }

      pDispatcher.register(literalargumentbuilder);
   }

   private static void logGamemodeChange(CommandSourceStack pSource, ServerPlayer pPlayer, GameType pGameType) {
      Component component = Component.translatable("gameMode." + pGameType.getName());
      if (pSource.getEntity() == pPlayer) {
         pSource.sendSuccess(Component.translatable("commands.gamemode.success.self", component), true);
      } else {
         if (pSource.getLevel().getGameRules().getBoolean(GameRules.RULE_SENDCOMMANDFEEDBACK)) {
            pPlayer.sendSystemMessage(Component.translatable("gameMode.changed", component));
         }

         pSource.sendSuccess(Component.translatable("commands.gamemode.success.other", pPlayer.getDisplayName(), component), true);
      }

   }

   private static int setMode(CommandContext<CommandSourceStack> pSource, Collection<ServerPlayer> pPlayers, GameType pGameType) {
      int i = 0;

      for(ServerPlayer serverplayer : pPlayers) {
         if (serverplayer.setGameMode(pGameType)) {
            logGamemodeChange(pSource.getSource(), serverplayer, pGameType);
            ++i;
         }
      }

      return i;
   }
}