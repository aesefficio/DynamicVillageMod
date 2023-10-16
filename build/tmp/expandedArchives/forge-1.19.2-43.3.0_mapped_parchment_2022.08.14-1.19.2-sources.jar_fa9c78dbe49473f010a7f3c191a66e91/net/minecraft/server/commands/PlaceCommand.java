package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Optional;
import net.minecraft.ResourceLocationException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.TemplateMirrorArgument;
import net.minecraft.commands.arguments.TemplateRotationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockRotProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public class PlaceCommand {
   private static final SimpleCommandExceptionType ERROR_FEATURE_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.place.feature.failed"));
   private static final SimpleCommandExceptionType ERROR_JIGSAW_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.place.jigsaw.failed"));
   private static final SimpleCommandExceptionType ERROR_STRUCTURE_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.place.structure.failed"));
   private static final DynamicCommandExceptionType ERROR_TEMPLATE_INVALID = new DynamicCommandExceptionType((p_214582_) -> {
      return Component.translatable("commands.place.template.invalid", p_214582_);
   });
   private static final SimpleCommandExceptionType ERROR_TEMPLATE_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.place.template.failed"));
   private static final SuggestionProvider<CommandSourceStack> SUGGEST_TEMPLATES = (p_214552_, p_214553_) -> {
      StructureTemplateManager structuretemplatemanager = p_214552_.getSource().getLevel().getStructureManager();
      return SharedSuggestionProvider.suggestResource(structuretemplatemanager.listTemplates(), p_214553_);
   };

   public static void register(CommandDispatcher<CommandSourceStack> pDispatcher) {
      pDispatcher.register(Commands.literal("place").requires((p_214560_) -> {
         return p_214560_.hasPermission(2);
      }).then(Commands.literal("feature").then(Commands.argument("feature", ResourceKeyArgument.key(Registry.CONFIGURED_FEATURE_REGISTRY)).executes((p_214610_) -> {
         return placeFeature(p_214610_.getSource(), ResourceKeyArgument.getConfiguredFeature(p_214610_, "feature"), new BlockPos(p_214610_.getSource().getPosition()));
      }).then(Commands.argument("pos", BlockPosArgument.blockPos()).executes((p_214608_) -> {
         return placeFeature(p_214608_.getSource(), ResourceKeyArgument.getConfiguredFeature(p_214608_, "feature"), BlockPosArgument.getLoadedBlockPos(p_214608_, "pos"));
      })))).then(Commands.literal("jigsaw").then(Commands.argument("pool", ResourceKeyArgument.key(Registry.TEMPLATE_POOL_REGISTRY)).then(Commands.argument("target", ResourceLocationArgument.id()).then(Commands.argument("max_depth", IntegerArgumentType.integer(1, 7)).executes((p_214606_) -> {
         return placeJigsaw(p_214606_.getSource(), ResourceKeyArgument.getStructureTemplatePool(p_214606_, "pool"), ResourceLocationArgument.getId(p_214606_, "target"), IntegerArgumentType.getInteger(p_214606_, "max_depth"), new BlockPos(p_214606_.getSource().getPosition()));
      }).then(Commands.argument("position", BlockPosArgument.blockPos()).executes((p_214604_) -> {
         return placeJigsaw(p_214604_.getSource(), ResourceKeyArgument.getStructureTemplatePool(p_214604_, "pool"), ResourceLocationArgument.getId(p_214604_, "target"), IntegerArgumentType.getInteger(p_214604_, "max_depth"), BlockPosArgument.getLoadedBlockPos(p_214604_, "position"));
      })))))).then(Commands.literal("structure").then(Commands.argument("structure", ResourceKeyArgument.key(Registry.STRUCTURE_REGISTRY)).executes((p_214602_) -> {
         return placeStructure(p_214602_.getSource(), ResourceKeyArgument.getStructure(p_214602_, "structure"), new BlockPos(p_214602_.getSource().getPosition()));
      }).then(Commands.argument("pos", BlockPosArgument.blockPos()).executes((p_214600_) -> {
         return placeStructure(p_214600_.getSource(), ResourceKeyArgument.getStructure(p_214600_, "structure"), BlockPosArgument.getLoadedBlockPos(p_214600_, "pos"));
      })))).then(Commands.literal("template").then(Commands.argument("template", ResourceLocationArgument.id()).suggests(SUGGEST_TEMPLATES).executes((p_214598_) -> {
         return placeTemplate(p_214598_.getSource(), ResourceLocationArgument.getId(p_214598_, "template"), new BlockPos(p_214598_.getSource().getPosition()), Rotation.NONE, Mirror.NONE, 1.0F, 0);
      }).then(Commands.argument("pos", BlockPosArgument.blockPos()).executes((p_214596_) -> {
         return placeTemplate(p_214596_.getSource(), ResourceLocationArgument.getId(p_214596_, "template"), BlockPosArgument.getLoadedBlockPos(p_214596_, "pos"), Rotation.NONE, Mirror.NONE, 1.0F, 0);
      }).then(Commands.argument("rotation", TemplateRotationArgument.templateRotation()).executes((p_214594_) -> {
         return placeTemplate(p_214594_.getSource(), ResourceLocationArgument.getId(p_214594_, "template"), BlockPosArgument.getLoadedBlockPos(p_214594_, "pos"), TemplateRotationArgument.getRotation(p_214594_, "rotation"), Mirror.NONE, 1.0F, 0);
      }).then(Commands.argument("mirror", TemplateMirrorArgument.templateMirror()).executes((p_214592_) -> {
         return placeTemplate(p_214592_.getSource(), ResourceLocationArgument.getId(p_214592_, "template"), BlockPosArgument.getLoadedBlockPos(p_214592_, "pos"), TemplateRotationArgument.getRotation(p_214592_, "rotation"), TemplateMirrorArgument.getMirror(p_214592_, "mirror"), 1.0F, 0);
      }).then(Commands.argument("integrity", FloatArgumentType.floatArg(0.0F, 1.0F)).executes((p_214586_) -> {
         return placeTemplate(p_214586_.getSource(), ResourceLocationArgument.getId(p_214586_, "template"), BlockPosArgument.getLoadedBlockPos(p_214586_, "pos"), TemplateRotationArgument.getRotation(p_214586_, "rotation"), TemplateMirrorArgument.getMirror(p_214586_, "mirror"), FloatArgumentType.getFloat(p_214586_, "integrity"), 0);
      }).then(Commands.argument("seed", IntegerArgumentType.integer()).executes((p_214550_) -> {
         return placeTemplate(p_214550_.getSource(), ResourceLocationArgument.getId(p_214550_, "template"), BlockPosArgument.getLoadedBlockPos(p_214550_, "pos"), TemplateRotationArgument.getRotation(p_214550_, "rotation"), TemplateMirrorArgument.getMirror(p_214550_, "mirror"), FloatArgumentType.getFloat(p_214550_, "integrity"), IntegerArgumentType.getInteger(p_214550_, "seed"));
      })))))))));
   }

   public static int placeFeature(CommandSourceStack pSource, Holder<ConfiguredFeature<?, ?>> pFeature, BlockPos pPos) throws CommandSyntaxException {
      ServerLevel serverlevel = pSource.getLevel();
      ConfiguredFeature<?, ?> configuredfeature = pFeature.value();
      ChunkPos chunkpos = new ChunkPos(pPos);
      checkLoaded(serverlevel, new ChunkPos(chunkpos.x - 1, chunkpos.z - 1), new ChunkPos(chunkpos.x + 1, chunkpos.z + 1));
      if (!configuredfeature.place(serverlevel, serverlevel.getChunkSource().getGenerator(), serverlevel.getRandom(), pPos)) {
         throw ERROR_FEATURE_FAILED.create();
      } else {
         String s = pFeature.unwrapKey().map((p_214584_) -> {
            return p_214584_.location().toString();
         }).orElse("[unregistered]");
         pSource.sendSuccess(Component.translatable("commands.place.feature.success", s, pPos.getX(), pPos.getY(), pPos.getZ()), true);
         return 1;
      }
   }

   public static int placeJigsaw(CommandSourceStack pSource, Holder<StructureTemplatePool> pTemplatePool, ResourceLocation p_214572_, int p_214573_, BlockPos pPos) throws CommandSyntaxException {
      ServerLevel serverlevel = pSource.getLevel();
      if (!JigsawPlacement.generateJigsaw(serverlevel, pTemplatePool, p_214572_, p_214573_, pPos, false)) {
         throw ERROR_JIGSAW_FAILED.create();
      } else {
         pSource.sendSuccess(Component.translatable("commands.place.jigsaw.success", pPos.getX(), pPos.getY(), pPos.getZ()), true);
         return 1;
      }
   }

   public static int placeStructure(CommandSourceStack pSource, Holder<Structure> pStructure, BlockPos pPos) throws CommandSyntaxException {
      ServerLevel serverlevel = pSource.getLevel();
      Structure structure = pStructure.value();
      ChunkGenerator chunkgenerator = serverlevel.getChunkSource().getGenerator();
      StructureStart structurestart = structure.generate(pSource.registryAccess(), chunkgenerator, chunkgenerator.getBiomeSource(), serverlevel.getChunkSource().randomState(), serverlevel.getStructureManager(), serverlevel.getSeed(), new ChunkPos(pPos), 0, serverlevel, (p_214580_) -> {
         return true;
      });
      if (!structurestart.isValid()) {
         throw ERROR_STRUCTURE_FAILED.create();
      } else {
         BoundingBox boundingbox = structurestart.getBoundingBox();
         ChunkPos chunkpos = new ChunkPos(SectionPos.blockToSectionCoord(boundingbox.minX()), SectionPos.blockToSectionCoord(boundingbox.minZ()));
         ChunkPos chunkpos1 = new ChunkPos(SectionPos.blockToSectionCoord(boundingbox.maxX()), SectionPos.blockToSectionCoord(boundingbox.maxZ()));
         checkLoaded(serverlevel, chunkpos, chunkpos1);
         ChunkPos.rangeClosed(chunkpos, chunkpos1).forEach((p_214558_) -> {
            structurestart.placeInChunk(serverlevel, serverlevel.structureManager(), chunkgenerator, serverlevel.getRandom(), new BoundingBox(p_214558_.getMinBlockX(), serverlevel.getMinBuildHeight(), p_214558_.getMinBlockZ(), p_214558_.getMaxBlockX(), serverlevel.getMaxBuildHeight(), p_214558_.getMaxBlockZ()), p_214558_);
         });
         String s = pStructure.unwrapKey().map((p_214539_) -> {
            return p_214539_.location().toString();
         }).orElse("[unregistered]");
         pSource.sendSuccess(Component.translatable("commands.place.structure.success", s, pPos.getX(), pPos.getY(), pPos.getZ()), true);
         return 1;
      }
   }

   public static int placeTemplate(CommandSourceStack p_214562_, ResourceLocation p_214563_, BlockPos p_214564_, Rotation p_214565_, Mirror p_214566_, float p_214567_, int p_214568_) throws CommandSyntaxException {
      ServerLevel serverlevel = p_214562_.getLevel();
      StructureTemplateManager structuretemplatemanager = serverlevel.getStructureManager();

      Optional<StructureTemplate> optional;
      try {
         optional = structuretemplatemanager.get(p_214563_);
      } catch (ResourceLocationException resourcelocationexception) {
         throw ERROR_TEMPLATE_INVALID.create(p_214563_);
      }

      if (optional.isEmpty()) {
         throw ERROR_TEMPLATE_INVALID.create(p_214563_);
      } else {
         StructureTemplate structuretemplate = optional.get();
         checkLoaded(serverlevel, new ChunkPos(p_214564_), new ChunkPos(p_214564_.offset(structuretemplate.getSize())));
         StructurePlaceSettings structureplacesettings = (new StructurePlaceSettings()).setMirror(p_214566_).setRotation(p_214565_);
         if (p_214567_ < 1.0F) {
            structureplacesettings.clearProcessors().addProcessor(new BlockRotProcessor(p_214567_)).setRandom(StructureBlockEntity.createRandom((long)p_214568_));
         }

         boolean flag = structuretemplate.placeInWorld(serverlevel, p_214564_, p_214564_, structureplacesettings, StructureBlockEntity.createRandom((long)p_214568_), 2);
         if (!flag) {
            throw ERROR_TEMPLATE_FAILED.create();
         } else {
            p_214562_.sendSuccess(Component.translatable("commands.place.template.success", p_214563_, p_214564_.getX(), p_214564_.getY(), p_214564_.getZ()), true);
            return 1;
         }
      }
   }

   private static void checkLoaded(ServerLevel p_214544_, ChunkPos p_214545_, ChunkPos p_214546_) throws CommandSyntaxException {
      if (ChunkPos.rangeClosed(p_214545_, p_214546_).filter((p_214542_) -> {
         return !p_214544_.isLoaded(p_214542_.getWorldPosition());
      }).findAny().isPresent()) {
         throw BlockPosArgument.ERROR_NOT_LOADED.create();
      }
   }
}