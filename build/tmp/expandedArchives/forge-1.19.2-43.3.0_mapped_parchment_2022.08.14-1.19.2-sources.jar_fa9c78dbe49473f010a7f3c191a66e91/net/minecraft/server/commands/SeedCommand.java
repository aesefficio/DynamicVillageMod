package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;

public class SeedCommand {
   public static void register(CommandDispatcher<CommandSourceStack> pDispatcher, boolean p_138591_) {
      pDispatcher.register(Commands.literal("seed").requires((p_138596_) -> {
         return !p_138591_ || p_138596_.hasPermission(2);
      }).executes((p_138593_) -> {
         long i = p_138593_.getSource().getLevel().getSeed();
         Component component = ComponentUtils.wrapInSquareBrackets(Component.literal(String.valueOf(i)).withStyle((p_180514_) -> {
            return p_180514_.withColor(ChatFormatting.GREEN).withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, String.valueOf(i))).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.copy.click"))).withInsertion(String.valueOf(i));
         }));
         p_138593_.getSource().sendSuccess(Component.translatable("commands.seed.success", component), false);
         return (int)i;
      }));
   }
}