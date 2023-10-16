package net.minecraft.commands.arguments;

import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.PreviewableCommand;

public record ArgumentSignatures(List<ArgumentSignatures.Entry> entries) {
   public static final ArgumentSignatures EMPTY = new ArgumentSignatures(List.of());
   private static final int MAX_ARGUMENT_COUNT = 8;
   private static final int MAX_ARGUMENT_NAME_LENGTH = 16;

   public ArgumentSignatures(FriendlyByteBuf p_231052_) {
      this(p_231052_.<ArgumentSignatures.Entry, List<ArgumentSignatures.Entry>>readCollection(FriendlyByteBuf.limitValue(ArrayList::new, 8), ArgumentSignatures.Entry::new));
   }

   public MessageSignature get(String p_241493_) {
      for(ArgumentSignatures.Entry argumentsignatures$entry : this.entries) {
         if (argumentsignatures$entry.name.equals(p_241493_)) {
            return argumentsignatures$entry.signature;
         }
      }

      return MessageSignature.EMPTY;
   }

   public void write(FriendlyByteBuf p_231062_) {
      p_231062_.writeCollection(this.entries, (p_241214_, p_241215_) -> {
         p_241215_.write(p_241214_);
      });
   }

   public static boolean hasSignableArguments(PreviewableCommand<?> p_242912_) {
      return p_242912_.arguments().stream().anyMatch((p_242699_) -> {
         return p_242699_.previewType() instanceof SignedArgument;
      });
   }

   public static ArgumentSignatures signCommand(PreviewableCommand<?> p_242877_, ArgumentSignatures.Signer p_242891_) {
      List<ArgumentSignatures.Entry> list = collectPlainSignableArguments(p_242877_).stream().map((p_242081_) -> {
         MessageSignature messagesignature = p_242891_.sign(p_242081_.getFirst(), p_242081_.getSecond());
         return new ArgumentSignatures.Entry(p_242081_.getFirst(), messagesignature);
      }).toList();
      return new ArgumentSignatures(list);
   }

   public static List<Pair<String, String>> collectPlainSignableArguments(PreviewableCommand<?> p_242870_) {
      List<Pair<String, String>> list = new ArrayList<>();

      for(PreviewableCommand.Argument<?> argument : p_242870_.arguments()) {
         PreviewedArgument $$4 = argument.previewType();
         if ($$4 instanceof SignedArgument<?> signedargument) {
            String s = getSignableText(signedargument, argument.parsedValue());
            list.add(Pair.of(argument.name(), s));
         }
      }

      return list;
   }

   private static <T> String getSignableText(SignedArgument<T> p_242374_, ParsedArgument<?, ?> p_242172_) {
      return p_242374_.getSignableText((T)p_242172_.getResult());
   }

   public static record Entry(String name, MessageSignature signature) {
      public Entry(FriendlyByteBuf p_241305_) {
         this(p_241305_.readUtf(16), new MessageSignature(p_241305_));
      }

      public void write(FriendlyByteBuf p_241403_) {
         p_241403_.writeUtf(this.name, 16);
         this.signature.write(p_241403_);
      }
   }

   @FunctionalInterface
   public interface Signer {
      MessageSignature sign(String p_241389_, String p_242287_);
   }
}