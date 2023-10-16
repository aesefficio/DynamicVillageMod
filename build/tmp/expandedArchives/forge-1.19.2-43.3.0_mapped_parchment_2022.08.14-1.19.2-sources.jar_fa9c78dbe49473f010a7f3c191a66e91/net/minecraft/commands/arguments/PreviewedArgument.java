package net.minecraft.commands.arguments;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public interface PreviewedArgument<T> extends ArgumentType<T> {
   @Nullable
   default CompletableFuture<Component> resolvePreview(CommandSourceStack p_242896_, ParsedArgument<CommandSourceStack, ?> p_242879_) throws CommandSyntaxException {
      return this.getValueType().isInstance(p_242879_.getResult()) ? this.resolvePreview(p_242896_, this.getValueType().cast(p_242879_.getResult())) : null;
   }

   CompletableFuture<Component> resolvePreview(CommandSourceStack p_232864_, T p_232865_) throws CommandSyntaxException;

   Class<T> getValueType();
}