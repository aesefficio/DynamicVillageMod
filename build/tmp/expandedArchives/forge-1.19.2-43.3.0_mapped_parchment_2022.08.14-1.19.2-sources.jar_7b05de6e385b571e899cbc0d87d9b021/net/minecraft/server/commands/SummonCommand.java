package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.EntitySummonArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class SummonCommand {
   private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.summon.failed"));
   private static final SimpleCommandExceptionType ERROR_DUPLICATE_UUID = new SimpleCommandExceptionType(Component.translatable("commands.summon.failed.uuid"));
   private static final SimpleCommandExceptionType INVALID_POSITION = new SimpleCommandExceptionType(Component.translatable("commands.summon.invalidPosition"));

   public static void register(CommandDispatcher<CommandSourceStack> pDispatcher) {
      pDispatcher.register(Commands.literal("summon").requires((p_138819_) -> {
         return p_138819_.hasPermission(2);
      }).then(Commands.argument("entity", EntitySummonArgument.id()).suggests(SuggestionProviders.SUMMONABLE_ENTITIES).executes((p_138832_) -> {
         return spawnEntity(p_138832_.getSource(), EntitySummonArgument.getSummonableEntity(p_138832_, "entity"), p_138832_.getSource().getPosition(), new CompoundTag(), true);
      }).then(Commands.argument("pos", Vec3Argument.vec3()).executes((p_138830_) -> {
         return spawnEntity(p_138830_.getSource(), EntitySummonArgument.getSummonableEntity(p_138830_, "entity"), Vec3Argument.getVec3(p_138830_, "pos"), new CompoundTag(), true);
      }).then(Commands.argument("nbt", CompoundTagArgument.compoundTag()).executes((p_138817_) -> {
         return spawnEntity(p_138817_.getSource(), EntitySummonArgument.getSummonableEntity(p_138817_, "entity"), Vec3Argument.getVec3(p_138817_, "pos"), CompoundTagArgument.getCompoundTag(p_138817_, "nbt"), false);
      })))));
   }

   private static int spawnEntity(CommandSourceStack pSource, ResourceLocation pType, Vec3 pPos, CompoundTag pNbt, boolean pRandomizeProperties) throws CommandSyntaxException {
      BlockPos blockpos = new BlockPos(pPos);
      if (!Level.isInSpawnableBounds(blockpos)) {
         throw INVALID_POSITION.create();
      } else {
         CompoundTag compoundtag = pNbt.copy();
         compoundtag.putString("id", pType.toString());
         ServerLevel serverlevel = pSource.getLevel();
         Entity entity = EntityType.loadEntityRecursive(compoundtag, serverlevel, (p_138828_) -> {
            p_138828_.moveTo(pPos.x, pPos.y, pPos.z, p_138828_.getYRot(), p_138828_.getXRot());
            return p_138828_;
         });
         if (entity == null) {
            throw ERROR_FAILED.create();
         } else {
            if (pRandomizeProperties && entity instanceof Mob) {
               if (!net.minecraftforge.event.ForgeEventFactory.doSpecialSpawn((Mob)entity, pSource.getLevel(), (float)entity.getX(), (float)entity.getY(), (float)entity.getZ(), null, MobSpawnType.COMMAND))
               ((Mob)entity).finalizeSpawn(pSource.getLevel(), pSource.getLevel().getCurrentDifficultyAt(entity.blockPosition()), MobSpawnType.COMMAND, (SpawnGroupData)null, (CompoundTag)null);
            }

            if (!serverlevel.tryAddFreshEntityWithPassengers(entity)) {
               throw ERROR_DUPLICATE_UUID.create();
            } else {
               pSource.sendSuccess(Component.translatable("commands.summon.success", entity.getDisplayName()), true);
               return 1;
            }
         }
      }
   }
}
