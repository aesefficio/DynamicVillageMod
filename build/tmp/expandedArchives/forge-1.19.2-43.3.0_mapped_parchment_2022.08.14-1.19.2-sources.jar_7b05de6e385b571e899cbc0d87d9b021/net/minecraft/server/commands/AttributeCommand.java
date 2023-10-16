package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class AttributeCommand {
   private static final DynamicCommandExceptionType ERROR_NOT_LIVING_ENTITY = new DynamicCommandExceptionType((p_212443_) -> {
      return Component.translatable("commands.attribute.failed.entity", p_212443_);
   });
   private static final Dynamic2CommandExceptionType ERROR_NO_SUCH_ATTRIBUTE = new Dynamic2CommandExceptionType((p_212445_, p_212446_) -> {
      return Component.translatable("commands.attribute.failed.no_attribute", p_212445_, p_212446_);
   });
   private static final Dynamic3CommandExceptionType ERROR_NO_SUCH_MODIFIER = new Dynamic3CommandExceptionType((p_212448_, p_212449_, p_212450_) -> {
      return Component.translatable("commands.attribute.failed.no_modifier", p_212449_, p_212448_, p_212450_);
   });
   private static final Dynamic3CommandExceptionType ERROR_MODIFIER_ALREADY_PRESENT = new Dynamic3CommandExceptionType((p_136497_, p_136498_, p_136499_) -> {
      return Component.translatable("commands.attribute.failed.modifier_already_present", p_136499_, p_136498_, p_136497_);
   });

   public static void register(CommandDispatcher<CommandSourceStack> pDispatcher) {
      pDispatcher.register(Commands.literal("attribute").requires((p_212441_) -> {
         return p_212441_.hasPermission(2);
      }).then(Commands.argument("target", EntityArgument.entity()).then(Commands.argument("attribute", ResourceKeyArgument.key(Registry.ATTRIBUTE_REGISTRY)).then(Commands.literal("get").executes((p_212452_) -> {
         return getAttributeValue(p_212452_.getSource(), EntityArgument.getEntity(p_212452_, "target"), ResourceKeyArgument.getAttribute(p_212452_, "attribute"), 1.0D);
      }).then(Commands.argument("scale", DoubleArgumentType.doubleArg()).executes((p_136522_) -> {
         return getAttributeValue(p_136522_.getSource(), EntityArgument.getEntity(p_136522_, "target"), ResourceKeyArgument.getAttribute(p_136522_, "attribute"), DoubleArgumentType.getDouble(p_136522_, "scale"));
      }))).then(Commands.literal("base").then(Commands.literal("set").then(Commands.argument("value", DoubleArgumentType.doubleArg()).executes((p_136520_) -> {
         return setAttributeBase(p_136520_.getSource(), EntityArgument.getEntity(p_136520_, "target"), ResourceKeyArgument.getAttribute(p_136520_, "attribute"), DoubleArgumentType.getDouble(p_136520_, "value"));
      }))).then(Commands.literal("get").executes((p_136518_) -> {
         return getAttributeBase(p_136518_.getSource(), EntityArgument.getEntity(p_136518_, "target"), ResourceKeyArgument.getAttribute(p_136518_, "attribute"), 1.0D);
      }).then(Commands.argument("scale", DoubleArgumentType.doubleArg()).executes((p_136516_) -> {
         return getAttributeBase(p_136516_.getSource(), EntityArgument.getEntity(p_136516_, "target"), ResourceKeyArgument.getAttribute(p_136516_, "attribute"), DoubleArgumentType.getDouble(p_136516_, "scale"));
      })))).then(Commands.literal("modifier").then(Commands.literal("add").then(Commands.argument("uuid", UuidArgument.uuid()).then(Commands.argument("name", StringArgumentType.string()).then(Commands.argument("value", DoubleArgumentType.doubleArg()).then(Commands.literal("add").executes((p_136514_) -> {
         return addModifier(p_136514_.getSource(), EntityArgument.getEntity(p_136514_, "target"), ResourceKeyArgument.getAttribute(p_136514_, "attribute"), UuidArgument.getUuid(p_136514_, "uuid"), StringArgumentType.getString(p_136514_, "name"), DoubleArgumentType.getDouble(p_136514_, "value"), AttributeModifier.Operation.ADDITION);
      })).then(Commands.literal("multiply").executes((p_136512_) -> {
         return addModifier(p_136512_.getSource(), EntityArgument.getEntity(p_136512_, "target"), ResourceKeyArgument.getAttribute(p_136512_, "attribute"), UuidArgument.getUuid(p_136512_, "uuid"), StringArgumentType.getString(p_136512_, "name"), DoubleArgumentType.getDouble(p_136512_, "value"), AttributeModifier.Operation.MULTIPLY_TOTAL);
      })).then(Commands.literal("multiply_base").executes((p_136510_) -> {
         return addModifier(p_136510_.getSource(), EntityArgument.getEntity(p_136510_, "target"), ResourceKeyArgument.getAttribute(p_136510_, "attribute"), UuidArgument.getUuid(p_136510_, "uuid"), StringArgumentType.getString(p_136510_, "name"), DoubleArgumentType.getDouble(p_136510_, "value"), AttributeModifier.Operation.MULTIPLY_BASE);
      })))))).then(Commands.literal("remove").then(Commands.argument("uuid", UuidArgument.uuid()).executes((p_136508_) -> {
         return removeModifier(p_136508_.getSource(), EntityArgument.getEntity(p_136508_, "target"), ResourceKeyArgument.getAttribute(p_136508_, "attribute"), UuidArgument.getUuid(p_136508_, "uuid"));
      }))).then(Commands.literal("value").then(Commands.literal("get").then(Commands.argument("uuid", UuidArgument.uuid()).executes((p_136501_) -> {
         return getAttributeModifier(p_136501_.getSource(), EntityArgument.getEntity(p_136501_, "target"), ResourceKeyArgument.getAttribute(p_136501_, "attribute"), UuidArgument.getUuid(p_136501_, "uuid"), 1.0D);
      }).then(Commands.argument("scale", DoubleArgumentType.doubleArg()).executes((p_136490_) -> {
         return getAttributeModifier(p_136490_.getSource(), EntityArgument.getEntity(p_136490_, "target"), ResourceKeyArgument.getAttribute(p_136490_, "attribute"), UuidArgument.getUuid(p_136490_, "uuid"), DoubleArgumentType.getDouble(p_136490_, "scale"));
      })))))))));
   }

   private static AttributeInstance getAttributeInstance(Entity pTarget, Attribute pAttribute) throws CommandSyntaxException {
      AttributeInstance attributeinstance = getLivingEntity(pTarget).getAttributes().getInstance(pAttribute);
      if (attributeinstance == null) {
         throw ERROR_NO_SUCH_ATTRIBUTE.create(pTarget.getName(), Component.translatable(pAttribute.getDescriptionId()));
      } else {
         return attributeinstance;
      }
   }

   private static LivingEntity getLivingEntity(Entity pTarget) throws CommandSyntaxException {
      if (!(pTarget instanceof LivingEntity)) {
         throw ERROR_NOT_LIVING_ENTITY.create(pTarget.getName());
      } else {
         return (LivingEntity)pTarget;
      }
   }

   private static LivingEntity getEntityWithAttribute(Entity pTarget, Attribute pAttribute) throws CommandSyntaxException {
      LivingEntity livingentity = getLivingEntity(pTarget);
      if (!livingentity.getAttributes().hasAttribute(pAttribute)) {
         throw ERROR_NO_SUCH_ATTRIBUTE.create(pTarget.getName(), Component.translatable(pAttribute.getDescriptionId()));
      } else {
         return livingentity;
      }
   }

   private static int getAttributeValue(CommandSourceStack pSource, Entity pTarget, Attribute pAttribute, double pScale) throws CommandSyntaxException {
      LivingEntity livingentity = getEntityWithAttribute(pTarget, pAttribute);
      double d0 = livingentity.getAttributeValue(pAttribute);
      pSource.sendSuccess(Component.translatable("commands.attribute.value.get.success", Component.translatable(pAttribute.getDescriptionId()), pTarget.getName(), d0), false);
      return (int)(d0 * pScale);
   }

   private static int getAttributeBase(CommandSourceStack pSource, Entity pTarget, Attribute pAttribute, double pScale) throws CommandSyntaxException {
      LivingEntity livingentity = getEntityWithAttribute(pTarget, pAttribute);
      double d0 = livingentity.getAttributeBaseValue(pAttribute);
      pSource.sendSuccess(Component.translatable("commands.attribute.base_value.get.success", Component.translatable(pAttribute.getDescriptionId()), pTarget.getName(), d0), false);
      return (int)(d0 * pScale);
   }

   private static int getAttributeModifier(CommandSourceStack pSource, Entity pTarget, Attribute pAttribute, UUID pUuid, double pScale) throws CommandSyntaxException {
      LivingEntity livingentity = getEntityWithAttribute(pTarget, pAttribute);
      AttributeMap attributemap = livingentity.getAttributes();
      if (!attributemap.hasModifier(pAttribute, pUuid)) {
         throw ERROR_NO_SUCH_MODIFIER.create(pTarget.getName(), Component.translatable(pAttribute.getDescriptionId()), pUuid);
      } else {
         double d0 = attributemap.getModifierValue(pAttribute, pUuid);
         pSource.sendSuccess(Component.translatable("commands.attribute.modifier.value.get.success", pUuid, Component.translatable(pAttribute.getDescriptionId()), pTarget.getName(), d0), false);
         return (int)(d0 * pScale);
      }
   }

   private static int setAttributeBase(CommandSourceStack pSource, Entity pTarget, Attribute pAttribute, double pValue) throws CommandSyntaxException {
      getAttributeInstance(pTarget, pAttribute).setBaseValue(pValue);
      pSource.sendSuccess(Component.translatable("commands.attribute.base_value.set.success", Component.translatable(pAttribute.getDescriptionId()), pTarget.getName(), pValue), false);
      return 1;
   }

   private static int addModifier(CommandSourceStack pSource, Entity pTarget, Attribute pAttribute, UUID pUuid, String pName, double pValue, AttributeModifier.Operation pOperation) throws CommandSyntaxException {
      AttributeInstance attributeinstance = getAttributeInstance(pTarget, pAttribute);
      AttributeModifier attributemodifier = new AttributeModifier(pUuid, pName, pValue, pOperation);
      if (attributeinstance.hasModifier(attributemodifier)) {
         throw ERROR_MODIFIER_ALREADY_PRESENT.create(pTarget.getName(), Component.translatable(pAttribute.getDescriptionId()), pUuid);
      } else {
         attributeinstance.addPermanentModifier(attributemodifier);
         pSource.sendSuccess(Component.translatable("commands.attribute.modifier.add.success", pUuid, Component.translatable(pAttribute.getDescriptionId()), pTarget.getName()), false);
         return 1;
      }
   }

   private static int removeModifier(CommandSourceStack pSource, Entity pTarget, Attribute pAttribute, UUID pUuid) throws CommandSyntaxException {
      AttributeInstance attributeinstance = getAttributeInstance(pTarget, pAttribute);
      if (attributeinstance.removePermanentModifier(pUuid)) {
         pSource.sendSuccess(Component.translatable("commands.attribute.modifier.remove.success", pUuid, Component.translatable(pAttribute.getDescriptionId()), pTarget.getName()), false);
         return 1;
      } else {
         throw ERROR_NO_SUCH_MODIFIER.create(pTarget.getName(), Component.translatable(pAttribute.getDescriptionId()), pUuid);
      }
   }
}