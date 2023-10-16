package net.minecraft.server.commands;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.MobEffectArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class EffectCommands {
   private static final SimpleCommandExceptionType ERROR_GIVE_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.effect.give.failed"));
   private static final SimpleCommandExceptionType ERROR_CLEAR_EVERYTHING_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.effect.clear.everything.failed"));
   private static final SimpleCommandExceptionType ERROR_CLEAR_SPECIFIC_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.effect.clear.specific.failed"));

   public static void register(CommandDispatcher<CommandSourceStack> pDispatcher) {
      pDispatcher.register(Commands.literal("effect").requires((p_136958_) -> {
         return p_136958_.hasPermission(2);
      }).then(Commands.literal("clear").executes((p_136984_) -> {
         return clearEffects(p_136984_.getSource(), ImmutableList.of(p_136984_.getSource().getEntityOrException()));
      }).then(Commands.argument("targets", EntityArgument.entities()).executes((p_136982_) -> {
         return clearEffects(p_136982_.getSource(), EntityArgument.getEntities(p_136982_, "targets"));
      }).then(Commands.argument("effect", MobEffectArgument.effect()).executes((p_136980_) -> {
         return clearEffect(p_136980_.getSource(), EntityArgument.getEntities(p_136980_, "targets"), MobEffectArgument.getEffect(p_136980_, "effect"));
      })))).then(Commands.literal("give").then(Commands.argument("targets", EntityArgument.entities()).then(Commands.argument("effect", MobEffectArgument.effect()).executes((p_136978_) -> {
         return giveEffect(p_136978_.getSource(), EntityArgument.getEntities(p_136978_, "targets"), MobEffectArgument.getEffect(p_136978_, "effect"), (Integer)null, 0, true);
      }).then(Commands.argument("seconds", IntegerArgumentType.integer(1, 1000000)).executes((p_136976_) -> {
         return giveEffect(p_136976_.getSource(), EntityArgument.getEntities(p_136976_, "targets"), MobEffectArgument.getEffect(p_136976_, "effect"), IntegerArgumentType.getInteger(p_136976_, "seconds"), 0, true);
      }).then(Commands.argument("amplifier", IntegerArgumentType.integer(0, 255)).executes((p_136974_) -> {
         return giveEffect(p_136974_.getSource(), EntityArgument.getEntities(p_136974_, "targets"), MobEffectArgument.getEffect(p_136974_, "effect"), IntegerArgumentType.getInteger(p_136974_, "seconds"), IntegerArgumentType.getInteger(p_136974_, "amplifier"), true);
      }).then(Commands.argument("hideParticles", BoolArgumentType.bool()).executes((p_136956_) -> {
         return giveEffect(p_136956_.getSource(), EntityArgument.getEntities(p_136956_, "targets"), MobEffectArgument.getEffect(p_136956_, "effect"), IntegerArgumentType.getInteger(p_136956_, "seconds"), IntegerArgumentType.getInteger(p_136956_, "amplifier"), !BoolArgumentType.getBool(p_136956_, "hideParticles"));
      }))))))));
   }

   private static int giveEffect(CommandSourceStack pSource, Collection<? extends Entity> pTargets, MobEffect pEffect, @Nullable Integer pSeconds, int pAmplifier, boolean pShowParticles) throws CommandSyntaxException {
      int i = 0;
      int j;
      if (pSeconds != null) {
         if (pEffect.isInstantenous()) {
            j = pSeconds;
         } else {
            j = pSeconds * 20;
         }
      } else if (pEffect.isInstantenous()) {
         j = 1;
      } else {
         j = 600;
      }

      for(Entity entity : pTargets) {
         if (entity instanceof LivingEntity) {
            MobEffectInstance mobeffectinstance = new MobEffectInstance(pEffect, j, pAmplifier, false, pShowParticles);
            if (((LivingEntity)entity).addEffect(mobeffectinstance, pSource.getEntity())) {
               ++i;
            }
         }
      }

      if (i == 0) {
         throw ERROR_GIVE_FAILED.create();
      } else {
         if (pTargets.size() == 1) {
            pSource.sendSuccess(Component.translatable("commands.effect.give.success.single", pEffect.getDisplayName(), pTargets.iterator().next().getDisplayName(), j / 20), true);
         } else {
            pSource.sendSuccess(Component.translatable("commands.effect.give.success.multiple", pEffect.getDisplayName(), pTargets.size(), j / 20), true);
         }

         return i;
      }
   }

   private static int clearEffects(CommandSourceStack pSource, Collection<? extends Entity> pTargets) throws CommandSyntaxException {
      int i = 0;

      for(Entity entity : pTargets) {
         if (entity instanceof LivingEntity && ((LivingEntity)entity).removeAllEffects()) {
            ++i;
         }
      }

      if (i == 0) {
         throw ERROR_CLEAR_EVERYTHING_FAILED.create();
      } else {
         if (pTargets.size() == 1) {
            pSource.sendSuccess(Component.translatable("commands.effect.clear.everything.success.single", pTargets.iterator().next().getDisplayName()), true);
         } else {
            pSource.sendSuccess(Component.translatable("commands.effect.clear.everything.success.multiple", pTargets.size()), true);
         }

         return i;
      }
   }

   private static int clearEffect(CommandSourceStack pSource, Collection<? extends Entity> pTargets, MobEffect pEffect) throws CommandSyntaxException {
      int i = 0;

      for(Entity entity : pTargets) {
         if (entity instanceof LivingEntity && ((LivingEntity)entity).removeEffect(pEffect)) {
            ++i;
         }
      }

      if (i == 0) {
         throw ERROR_CLEAR_SPECIFIC_FAILED.create();
      } else {
         if (pTargets.size() == 1) {
            pSource.sendSuccess(Component.translatable("commands.effect.clear.specific.success.single", pEffect.getDisplayName(), pTargets.iterator().next().getDisplayName()), true);
         } else {
            pSource.sendSuccess(Component.translatable("commands.effect.clear.specific.success.multiple", pEffect.getDisplayName(), pTargets.size()), true);
         }

         return i;
      }
   }
}