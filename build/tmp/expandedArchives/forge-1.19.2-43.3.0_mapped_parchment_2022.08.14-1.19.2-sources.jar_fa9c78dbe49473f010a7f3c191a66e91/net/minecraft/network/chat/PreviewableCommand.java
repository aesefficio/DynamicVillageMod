package net.minecraft.network.chat;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.arguments.PreviewedArgument;

public record PreviewableCommand<S>(List<PreviewableCommand.Argument<S>> arguments) {
   public static <S> PreviewableCommand<S> of(ParseResults<S> pParseResults) {
      CommandContextBuilder<S> commandcontextbuilder = pParseResults.getContext();
      CommandContextBuilder<S> commandcontextbuilder1 = commandcontextbuilder;

      List<PreviewableCommand.Argument<S>> list;
      CommandContextBuilder<S> commandcontextbuilder2;
      for(list = collectArguments(commandcontextbuilder); (commandcontextbuilder2 = commandcontextbuilder1.getChild()) != null; commandcontextbuilder1 = commandcontextbuilder2) {
         boolean flag = commandcontextbuilder2.getRootNode() != commandcontextbuilder.getRootNode();
         if (!flag) {
            break;
         }

         list.addAll(collectArguments(commandcontextbuilder2));
      }

      return new PreviewableCommand<>(list);
   }

   private static <S> List<PreviewableCommand.Argument<S>> collectArguments(CommandContextBuilder<S> p_242893_) {
      List<PreviewableCommand.Argument<S>> list = new ArrayList<>();

      for(ParsedCommandNode<S> parsedcommandnode : p_242893_.getNodes()) {
         CommandNode<S> $$5 = parsedcommandnode.getNode();
         if ($$5 instanceof ArgumentCommandNode<S, ?> argumentcommandnode) {
            ArgumentType argumenttype = argumentcommandnode.getType();
            if (argumenttype instanceof PreviewedArgument<?> previewedargument) {
               ParsedArgument<S, ?> parsedargument = p_242893_.getArguments().get(argumentcommandnode.getName());
               if (parsedargument != null) {
                  list.add(new PreviewableCommand.Argument<>(argumentcommandnode, parsedargument, previewedargument));
               }
            }
         }
      }

      return list;
   }

   public boolean isPreviewed(CommandNode<?> p_242917_) {
      for(PreviewableCommand.Argument<S> argument : this.arguments) {
         if (argument.node() == p_242917_) {
            return true;
         }
      }

      return false;
   }

   public static record Argument<S>(ArgumentCommandNode<S, ?> node, ParsedArgument<S, ?> parsedValue, PreviewedArgument<?> previewType) {
      public String name() {
         return this.node.getName();
      }
   }
}