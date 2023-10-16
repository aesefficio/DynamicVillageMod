package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.entity.raid.Raids;

public class RaidCommand {
   public static void register(CommandDispatcher<CommandSourceStack> pDispatcher) {
      pDispatcher.register(Commands.literal("raid").requires((p_180498_) -> {
         return p_180498_.hasPermission(3);
      }).then(Commands.literal("start").then(Commands.argument("omenlvl", IntegerArgumentType.integer(0)).executes((p_180502_) -> {
         return start(p_180502_.getSource(), IntegerArgumentType.getInteger(p_180502_, "omenlvl"));
      }))).then(Commands.literal("stop").executes((p_180500_) -> {
         return stop(p_180500_.getSource());
      })).then(Commands.literal("check").executes((p_180496_) -> {
         return check(p_180496_.getSource());
      })).then(Commands.literal("sound").then(Commands.argument("type", ComponentArgument.textComponent()).executes((p_180492_) -> {
         return playSound(p_180492_.getSource(), ComponentArgument.getComponent(p_180492_, "type"));
      }))).then(Commands.literal("spawnleader").executes((p_180488_) -> {
         return spawnLeader(p_180488_.getSource());
      })).then(Commands.literal("setomen").then(Commands.argument("level", IntegerArgumentType.integer(0)).executes((p_180481_) -> {
         return setBadOmenLevel(p_180481_.getSource(), IntegerArgumentType.getInteger(p_180481_, "level"));
      }))).then(Commands.literal("glow").executes((p_180471_) -> {
         return glow(p_180471_.getSource());
      })));
   }

   private static int glow(CommandSourceStack pSource) throws CommandSyntaxException {
      Raid raid = getRaid(pSource.getPlayerOrException());
      if (raid != null) {
         for(Raider raider : raid.getAllRaiders()) {
            raider.addEffect(new MobEffectInstance(MobEffects.GLOWING, 1000, 1));
         }
      }

      return 1;
   }

   private static int setBadOmenLevel(CommandSourceStack pSource, int pLevel) throws CommandSyntaxException {
      Raid raid = getRaid(pSource.getPlayerOrException());
      if (raid != null) {
         int i = raid.getMaxBadOmenLevel();
         if (pLevel > i) {
            pSource.sendFailure(Component.literal("Sorry, the max bad omen level you can set is " + i));
         } else {
            int j = raid.getBadOmenLevel();
            raid.setBadOmenLevel(pLevel);
            pSource.sendSuccess(Component.literal("Changed village's bad omen level from " + j + " to " + pLevel), false);
         }
      } else {
         pSource.sendFailure(Component.literal("No raid found here"));
      }

      return 1;
   }

   private static int spawnLeader(CommandSourceStack pSource) {
      pSource.sendSuccess(Component.literal("Spawned a raid captain"), false);
      Raider raider = EntityType.PILLAGER.create(pSource.getLevel());
      raider.setPatrolLeader(true);
      raider.setItemSlot(EquipmentSlot.HEAD, Raid.getLeaderBannerInstance());
      raider.setPos(pSource.getPosition().x, pSource.getPosition().y, pSource.getPosition().z);
      raider.finalizeSpawn(pSource.getLevel(), pSource.getLevel().getCurrentDifficultyAt(new BlockPos(pSource.getPosition())), MobSpawnType.COMMAND, (SpawnGroupData)null, (CompoundTag)null);
      pSource.getLevel().addFreshEntityWithPassengers(raider);
      return 1;
   }

   private static int playSound(CommandSourceStack pSource, Component p_180479_) {
      if (p_180479_ != null && p_180479_.getString().equals("local")) {
         pSource.getLevel().playSound((Player)null, new BlockPos(pSource.getPosition().add(5.0D, 0.0D, 0.0D)), SoundEvents.RAID_HORN, SoundSource.NEUTRAL, 2.0F, 1.0F);
      }

      return 1;
   }

   private static int start(CommandSourceStack pSource, int pBadOmenLevel) throws CommandSyntaxException {
      ServerPlayer serverplayer = pSource.getPlayerOrException();
      BlockPos blockpos = serverplayer.blockPosition();
      if (serverplayer.getLevel().isRaided(blockpos)) {
         pSource.sendFailure(Component.literal("Raid already started close by"));
         return -1;
      } else {
         Raids raids = serverplayer.getLevel().getRaids();
         Raid raid = raids.createOrExtendRaid(serverplayer);
         if (raid != null) {
            raid.setBadOmenLevel(pBadOmenLevel);
            raids.setDirty();
            pSource.sendSuccess(Component.literal("Created a raid in your local village"), false);
         } else {
            pSource.sendFailure(Component.literal("Failed to create a raid in your local village"));
         }

         return 1;
      }
   }

   private static int stop(CommandSourceStack pSource) throws CommandSyntaxException {
      ServerPlayer serverplayer = pSource.getPlayerOrException();
      BlockPos blockpos = serverplayer.blockPosition();
      Raid raid = serverplayer.getLevel().getRaidAt(blockpos);
      if (raid != null) {
         raid.stop();
         pSource.sendSuccess(Component.literal("Stopped raid"), false);
         return 1;
      } else {
         pSource.sendFailure(Component.literal("No raid here"));
         return -1;
      }
   }

   private static int check(CommandSourceStack pSource) throws CommandSyntaxException {
      Raid raid = getRaid(pSource.getPlayerOrException());
      if (raid != null) {
         StringBuilder stringbuilder = new StringBuilder();
         stringbuilder.append("Found a started raid! ");
         pSource.sendSuccess(Component.literal(stringbuilder.toString()), false);
         stringbuilder = new StringBuilder();
         stringbuilder.append("Num groups spawned: ");
         stringbuilder.append(raid.getGroupsSpawned());
         stringbuilder.append(" Bad omen level: ");
         stringbuilder.append(raid.getBadOmenLevel());
         stringbuilder.append(" Num mobs: ");
         stringbuilder.append(raid.getTotalRaidersAlive());
         stringbuilder.append(" Raid health: ");
         stringbuilder.append(raid.getHealthOfLivingRaiders());
         stringbuilder.append(" / ");
         stringbuilder.append(raid.getTotalHealth());
         pSource.sendSuccess(Component.literal(stringbuilder.toString()), false);
         return 1;
      } else {
         pSource.sendFailure(Component.literal("Found no started raids"));
         return 0;
      }
   }

   @Nullable
   private static Raid getRaid(ServerPlayer pPlayer) {
      return pPlayer.getLevel().getRaidAt(pPlayer.blockPosition());
   }
}