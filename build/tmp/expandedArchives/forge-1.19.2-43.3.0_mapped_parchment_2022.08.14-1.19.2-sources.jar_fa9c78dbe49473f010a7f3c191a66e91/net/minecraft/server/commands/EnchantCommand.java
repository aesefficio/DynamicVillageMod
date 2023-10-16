package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ItemEnchantmentArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public class EnchantCommand {
   private static final DynamicCommandExceptionType ERROR_NOT_LIVING_ENTITY = new DynamicCommandExceptionType((p_137029_) -> {
      return Component.translatable("commands.enchant.failed.entity", p_137029_);
   });
   private static final DynamicCommandExceptionType ERROR_NO_ITEM = new DynamicCommandExceptionType((p_137027_) -> {
      return Component.translatable("commands.enchant.failed.itemless", p_137027_);
   });
   private static final DynamicCommandExceptionType ERROR_INCOMPATIBLE = new DynamicCommandExceptionType((p_137020_) -> {
      return Component.translatable("commands.enchant.failed.incompatible", p_137020_);
   });
   private static final Dynamic2CommandExceptionType ERROR_LEVEL_TOO_HIGH = new Dynamic2CommandExceptionType((p_137022_, p_137023_) -> {
      return Component.translatable("commands.enchant.failed.level", p_137022_, p_137023_);
   });
   private static final SimpleCommandExceptionType ERROR_NOTHING_HAPPENED = new SimpleCommandExceptionType(Component.translatable("commands.enchant.failed"));

   public static void register(CommandDispatcher<CommandSourceStack> pDispatcher) {
      pDispatcher.register(Commands.literal("enchant").requires((p_137013_) -> {
         return p_137013_.hasPermission(2);
      }).then(Commands.argument("targets", EntityArgument.entities()).then(Commands.argument("enchantment", ItemEnchantmentArgument.enchantment()).executes((p_137025_) -> {
         return enchant(p_137025_.getSource(), EntityArgument.getEntities(p_137025_, "targets"), ItemEnchantmentArgument.getEnchantment(p_137025_, "enchantment"), 1);
      }).then(Commands.argument("level", IntegerArgumentType.integer(0)).executes((p_137011_) -> {
         return enchant(p_137011_.getSource(), EntityArgument.getEntities(p_137011_, "targets"), ItemEnchantmentArgument.getEnchantment(p_137011_, "enchantment"), IntegerArgumentType.getInteger(p_137011_, "level"));
      })))));
   }

   private static int enchant(CommandSourceStack pSource, Collection<? extends Entity> pTargets, Enchantment pEnchantment, int pLevel) throws CommandSyntaxException {
      if (pLevel > pEnchantment.getMaxLevel()) {
         throw ERROR_LEVEL_TOO_HIGH.create(pLevel, pEnchantment.getMaxLevel());
      } else {
         int i = 0;

         for(Entity entity : pTargets) {
            if (entity instanceof LivingEntity) {
               LivingEntity livingentity = (LivingEntity)entity;
               ItemStack itemstack = livingentity.getMainHandItem();
               if (!itemstack.isEmpty()) {
                  if (pEnchantment.canEnchant(itemstack) && EnchantmentHelper.isEnchantmentCompatible(EnchantmentHelper.getEnchantments(itemstack).keySet(), pEnchantment)) {
                     itemstack.enchant(pEnchantment, pLevel);
                     ++i;
                  } else if (pTargets.size() == 1) {
                     throw ERROR_INCOMPATIBLE.create(itemstack.getItem().getName(itemstack).getString());
                  }
               } else if (pTargets.size() == 1) {
                  throw ERROR_NO_ITEM.create(livingentity.getName().getString());
               }
            } else if (pTargets.size() == 1) {
               throw ERROR_NOT_LIVING_ENTITY.create(entity.getName().getString());
            }
         }

         if (i == 0) {
            throw ERROR_NOTHING_HAPPENED.create();
         } else {
            if (pTargets.size() == 1) {
               pSource.sendSuccess(Component.translatable("commands.enchant.success.single", pEnchantment.getFullname(pLevel), pTargets.iterator().next().getDisplayName()), true);
            } else {
               pSource.sendSuccess(Component.translatable("commands.enchant.success.multiple", pEnchantment.getFullname(pLevel), pTargets.size()), true);
            }

            return i;
         }
      }
   }
}