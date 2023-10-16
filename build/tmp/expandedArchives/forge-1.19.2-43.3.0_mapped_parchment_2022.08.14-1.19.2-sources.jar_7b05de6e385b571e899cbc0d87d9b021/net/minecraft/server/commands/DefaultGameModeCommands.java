package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

public class DefaultGameModeCommands {
   public static void register(CommandDispatcher<CommandSourceStack> pDispatcher) {
      LiteralArgumentBuilder<CommandSourceStack> literalargumentbuilder = Commands.literal("defaultgamemode").requires((p_136929_) -> {
         return p_136929_.hasPermission(2);
      });

      for(GameType gametype : GameType.values()) {
         literalargumentbuilder.then(Commands.literal(gametype.getName()).executes((p_136925_) -> {
            return setMode(p_136925_.getSource(), gametype);
         }));
      }

      pDispatcher.register(literalargumentbuilder);
   }

   /**
    * Sets the {@link net.minecraft.world.level.GameType} of the player who ran the command.
    */
   private static int setMode(CommandSourceStack pCommandSource, GameType pGamemode) {
      int i = 0;
      MinecraftServer minecraftserver = pCommandSource.getServer();
      minecraftserver.setDefaultGameType(pGamemode);
      GameType gametype = minecraftserver.getForcedGameType();
      if (gametype != null) {
         for(ServerPlayer serverplayer : minecraftserver.getPlayerList().getPlayers()) {
            if (serverplayer.setGameMode(gametype)) {
               ++i;
            }
         }
      }

      pCommandSource.sendSuccess(Component.translatable("commands.defaultgamemode.success", pGamemode.getLongDisplayName()), true);
      return i;
   }
}