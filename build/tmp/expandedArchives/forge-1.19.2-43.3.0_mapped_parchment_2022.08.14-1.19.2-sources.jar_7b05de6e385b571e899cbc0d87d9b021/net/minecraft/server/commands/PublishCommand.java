package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.util.HttpUtil;
import net.minecraft.world.level.GameType;

public class PublishCommand {
   private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.publish.failed"));
   private static final DynamicCommandExceptionType ERROR_ALREADY_PUBLISHED = new DynamicCommandExceptionType((p_138194_) -> {
      return Component.translatable("commands.publish.alreadyPublished", p_138194_);
   });

   public static void register(CommandDispatcher<CommandSourceStack> pDispatcher) {
      pDispatcher.register(Commands.literal("publish").requires((p_138189_) -> {
         return p_138189_.hasPermission(4);
      }).executes((p_138196_) -> {
         return publish(p_138196_.getSource(), HttpUtil.getAvailablePort());
      }).then(Commands.argument("port", IntegerArgumentType.integer(0, 65535)).executes((p_138187_) -> {
         return publish(p_138187_.getSource(), IntegerArgumentType.getInteger(p_138187_, "port"));
      })));
   }

   private static int publish(CommandSourceStack pSource, int pPort) throws CommandSyntaxException {
      if (pSource.getServer().isPublished()) {
         throw ERROR_ALREADY_PUBLISHED.create(pSource.getServer().getPort());
      } else if (!pSource.getServer().publishServer((GameType)null, false, pPort)) {
         throw ERROR_FAILED.create();
      } else {
         pSource.sendSuccess(Component.translatable("commands.publish.success", pPort), true);
         return pPort;
      }
   }
}