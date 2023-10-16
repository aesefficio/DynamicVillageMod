package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ResultConsumer;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.IntFunction;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.commands.arguments.ObjectiveArgument;
import net.minecraft.commands.arguments.RangeArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.ScoreHolderArgument;
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.arguments.coordinates.SwizzleArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.bossevents.CustomBossEvent;
import net.minecraft.server.commands.data.DataAccessor;
import net.minecraft.server.commands.data.DataCommands;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.PredicateManager;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;

public class ExecuteCommand {
   private static final int MAX_TEST_AREA = 32768;
   private static final Dynamic2CommandExceptionType ERROR_AREA_TOO_LARGE = new Dynamic2CommandExceptionType((p_137129_, p_137130_) -> {
      return Component.translatable("commands.execute.blocks.toobig", p_137129_, p_137130_);
   });
   private static final SimpleCommandExceptionType ERROR_CONDITIONAL_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.execute.conditional.fail"));
   private static final DynamicCommandExceptionType ERROR_CONDITIONAL_FAILED_COUNT = new DynamicCommandExceptionType((p_137127_) -> {
      return Component.translatable("commands.execute.conditional.fail_count", p_137127_);
   });
   private static final BinaryOperator<ResultConsumer<CommandSourceStack>> CALLBACK_CHAINER = (p_137045_, p_137046_) -> {
      return (p_180160_, p_180161_, p_180162_) -> {
         p_137045_.onCommandComplete(p_180160_, p_180161_, p_180162_);
         p_137046_.onCommandComplete(p_180160_, p_180161_, p_180162_);
      };
   };
   private static final SuggestionProvider<CommandSourceStack> SUGGEST_PREDICATE = (p_137062_, p_137063_) -> {
      PredicateManager predicatemanager = p_137062_.getSource().getServer().getPredicateManager();
      return SharedSuggestionProvider.suggestResource(predicatemanager.getKeys(), p_137063_);
   };

   public static void register(CommandDispatcher<CommandSourceStack> pDispatcher, CommandBuildContext pContext) {
      LiteralCommandNode<CommandSourceStack> literalcommandnode = pDispatcher.register(Commands.literal("execute").requires((p_137197_) -> {
         return p_137197_.hasPermission(2);
      }));
      pDispatcher.register(Commands.literal("execute").requires((p_137103_) -> {
         return p_137103_.hasPermission(2);
      }).then(Commands.literal("run").redirect(pDispatcher.getRoot())).then(addConditionals(literalcommandnode, Commands.literal("if"), true, pContext)).then(addConditionals(literalcommandnode, Commands.literal("unless"), false, pContext)).then(Commands.literal("as").then(Commands.argument("targets", EntityArgument.entities()).fork(literalcommandnode, (p_137299_) -> {
         List<CommandSourceStack> list = Lists.newArrayList();

         for(Entity entity : EntityArgument.getOptionalEntities(p_137299_, "targets")) {
            list.add(p_137299_.getSource().withEntity(entity));
         }

         return list;
      }))).then(Commands.literal("at").then(Commands.argument("targets", EntityArgument.entities()).fork(literalcommandnode, (p_137297_) -> {
         List<CommandSourceStack> list = Lists.newArrayList();

         for(Entity entity : EntityArgument.getOptionalEntities(p_137297_, "targets")) {
            list.add(p_137297_.getSource().withLevel((ServerLevel)entity.level).withPosition(entity.position()).withRotation(entity.getRotationVector()));
         }

         return list;
      }))).then(Commands.literal("store").then(wrapStores(literalcommandnode, Commands.literal("result"), true)).then(wrapStores(literalcommandnode, Commands.literal("success"), false))).then(Commands.literal("positioned").then(Commands.argument("pos", Vec3Argument.vec3()).redirect(literalcommandnode, (p_137295_) -> {
         return p_137295_.getSource().withPosition(Vec3Argument.getVec3(p_137295_, "pos")).withAnchor(EntityAnchorArgument.Anchor.FEET);
      })).then(Commands.literal("as").then(Commands.argument("targets", EntityArgument.entities()).fork(literalcommandnode, (p_137293_) -> {
         List<CommandSourceStack> list = Lists.newArrayList();

         for(Entity entity : EntityArgument.getOptionalEntities(p_137293_, "targets")) {
            list.add(p_137293_.getSource().withPosition(entity.position()));
         }

         return list;
      })))).then(Commands.literal("rotated").then(Commands.argument("rot", RotationArgument.rotation()).redirect(literalcommandnode, (p_137291_) -> {
         return p_137291_.getSource().withRotation(RotationArgument.getRotation(p_137291_, "rot").getRotation(p_137291_.getSource()));
      })).then(Commands.literal("as").then(Commands.argument("targets", EntityArgument.entities()).fork(literalcommandnode, (p_137289_) -> {
         List<CommandSourceStack> list = Lists.newArrayList();

         for(Entity entity : EntityArgument.getOptionalEntities(p_137289_, "targets")) {
            list.add(p_137289_.getSource().withRotation(entity.getRotationVector()));
         }

         return list;
      })))).then(Commands.literal("facing").then(Commands.literal("entity").then(Commands.argument("targets", EntityArgument.entities()).then(Commands.argument("anchor", EntityAnchorArgument.anchor()).fork(literalcommandnode, (p_137287_) -> {
         List<CommandSourceStack> list = Lists.newArrayList();
         EntityAnchorArgument.Anchor entityanchorargument$anchor = EntityAnchorArgument.getAnchor(p_137287_, "anchor");

         for(Entity entity : EntityArgument.getOptionalEntities(p_137287_, "targets")) {
            list.add(p_137287_.getSource().facing(entity, entityanchorargument$anchor));
         }

         return list;
      })))).then(Commands.argument("pos", Vec3Argument.vec3()).redirect(literalcommandnode, (p_137285_) -> {
         return p_137285_.getSource().facing(Vec3Argument.getVec3(p_137285_, "pos"));
      }))).then(Commands.literal("align").then(Commands.argument("axes", SwizzleArgument.swizzle()).redirect(literalcommandnode, (p_137283_) -> {
         return p_137283_.getSource().withPosition(p_137283_.getSource().getPosition().align(SwizzleArgument.getSwizzle(p_137283_, "axes")));
      }))).then(Commands.literal("anchored").then(Commands.argument("anchor", EntityAnchorArgument.anchor()).redirect(literalcommandnode, (p_137281_) -> {
         return p_137281_.getSource().withAnchor(EntityAnchorArgument.getAnchor(p_137281_, "anchor"));
      }))).then(Commands.literal("in").then(Commands.argument("dimension", DimensionArgument.dimension()).redirect(literalcommandnode, (p_137279_) -> {
         return p_137279_.getSource().withLevel(DimensionArgument.getDimension(p_137279_, "dimension"));
      }))));
   }

   private static ArgumentBuilder<CommandSourceStack, ?> wrapStores(LiteralCommandNode<CommandSourceStack> pParent, LiteralArgumentBuilder<CommandSourceStack> pLiteral, boolean pStoringResult) {
      pLiteral.then(Commands.literal("score").then(Commands.argument("targets", ScoreHolderArgument.scoreHolders()).suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS).then(Commands.argument("objective", ObjectiveArgument.objective()).redirect(pParent, (p_137271_) -> {
         return storeValue(p_137271_.getSource(), ScoreHolderArgument.getNamesWithDefaultWildcard(p_137271_, "targets"), ObjectiveArgument.getObjective(p_137271_, "objective"), pStoringResult);
      }))));
      pLiteral.then(Commands.literal("bossbar").then(Commands.argument("id", ResourceLocationArgument.id()).suggests(BossBarCommands.SUGGEST_BOSS_BAR).then(Commands.literal("value").redirect(pParent, (p_137259_) -> {
         return storeValue(p_137259_.getSource(), BossBarCommands.getBossBar(p_137259_), true, pStoringResult);
      })).then(Commands.literal("max").redirect(pParent, (p_137247_) -> {
         return storeValue(p_137247_.getSource(), BossBarCommands.getBossBar(p_137247_), false, pStoringResult);
      }))));

      for(DataCommands.DataProvider datacommands$dataprovider : DataCommands.TARGET_PROVIDERS) {
         datacommands$dataprovider.wrap(pLiteral, (p_137101_) -> {
            return p_137101_.then(Commands.argument("path", NbtPathArgument.nbtPath()).then(Commands.literal("int").then(Commands.argument("scale", DoubleArgumentType.doubleArg()).redirect(pParent, (p_180216_) -> {
               return storeData(p_180216_.getSource(), datacommands$dataprovider.access(p_180216_), NbtPathArgument.getPath(p_180216_, "path"), (p_180219_) -> {
                  return IntTag.valueOf((int)((double)p_180219_ * DoubleArgumentType.getDouble(p_180216_, "scale")));
               }, pStoringResult);
            }))).then(Commands.literal("float").then(Commands.argument("scale", DoubleArgumentType.doubleArg()).redirect(pParent, (p_180209_) -> {
               return storeData(p_180209_.getSource(), datacommands$dataprovider.access(p_180209_), NbtPathArgument.getPath(p_180209_, "path"), (p_180212_) -> {
                  return FloatTag.valueOf((float)((double)p_180212_ * DoubleArgumentType.getDouble(p_180209_, "scale")));
               }, pStoringResult);
            }))).then(Commands.literal("short").then(Commands.argument("scale", DoubleArgumentType.doubleArg()).redirect(pParent, (p_180199_) -> {
               return storeData(p_180199_.getSource(), datacommands$dataprovider.access(p_180199_), NbtPathArgument.getPath(p_180199_, "path"), (p_180202_) -> {
                  return ShortTag.valueOf((short)((int)((double)p_180202_ * DoubleArgumentType.getDouble(p_180199_, "scale"))));
               }, pStoringResult);
            }))).then(Commands.literal("long").then(Commands.argument("scale", DoubleArgumentType.doubleArg()).redirect(pParent, (p_180189_) -> {
               return storeData(p_180189_.getSource(), datacommands$dataprovider.access(p_180189_), NbtPathArgument.getPath(p_180189_, "path"), (p_180192_) -> {
                  return LongTag.valueOf((long)((double)p_180192_ * DoubleArgumentType.getDouble(p_180189_, "scale")));
               }, pStoringResult);
            }))).then(Commands.literal("double").then(Commands.argument("scale", DoubleArgumentType.doubleArg()).redirect(pParent, (p_180179_) -> {
               return storeData(p_180179_.getSource(), datacommands$dataprovider.access(p_180179_), NbtPathArgument.getPath(p_180179_, "path"), (p_180182_) -> {
                  return DoubleTag.valueOf((double)p_180182_ * DoubleArgumentType.getDouble(p_180179_, "scale"));
               }, pStoringResult);
            }))).then(Commands.literal("byte").then(Commands.argument("scale", DoubleArgumentType.doubleArg()).redirect(pParent, (p_180156_) -> {
               return storeData(p_180156_.getSource(), datacommands$dataprovider.access(p_180156_), NbtPathArgument.getPath(p_180156_, "path"), (p_180165_) -> {
                  return ByteTag.valueOf((byte)((int)((double)p_180165_ * DoubleArgumentType.getDouble(p_180156_, "scale"))));
               }, pStoringResult);
            }))));
         });
      }

      return pLiteral;
   }

   private static CommandSourceStack storeValue(CommandSourceStack pSource, Collection<String> pTargets, Objective pObjective, boolean pStoringResult) {
      Scoreboard scoreboard = pSource.getServer().getScoreboard();
      return pSource.withCallback((p_137136_, p_137137_, p_137138_) -> {
         for(String s : pTargets) {
            Score score = scoreboard.getOrCreatePlayerScore(s, pObjective);
            int i = pStoringResult ? p_137138_ : (p_137137_ ? 1 : 0);
            score.setScore(i);
         }

      }, CALLBACK_CHAINER);
   }

   private static CommandSourceStack storeValue(CommandSourceStack pSource, CustomBossEvent pBar, boolean pStoringValue, boolean pStoringResult) {
      return pSource.withCallback((p_137185_, p_137186_, p_137187_) -> {
         int i = pStoringResult ? p_137187_ : (p_137186_ ? 1 : 0);
         if (pStoringValue) {
            pBar.setValue(i);
         } else {
            pBar.setMax(i);
         }

      }, CALLBACK_CHAINER);
   }

   private static CommandSourceStack storeData(CommandSourceStack pSource, DataAccessor pAccessor, NbtPathArgument.NbtPath pPath, IntFunction<Tag> pTagConverter, boolean pStoringResult) {
      return pSource.withCallback((p_137153_, p_137154_, p_137155_) -> {
         try {
            CompoundTag compoundtag = pAccessor.getData();
            int i = pStoringResult ? p_137155_ : (p_137154_ ? 1 : 0);
            pPath.set(compoundtag, () -> {
               return pTagConverter.apply(i);
            });
            pAccessor.setData(compoundtag);
         } catch (CommandSyntaxException commandsyntaxexception) {
         }

      }, CALLBACK_CHAINER);
   }

   private static ArgumentBuilder<CommandSourceStack, ?> addConditionals(CommandNode<CommandSourceStack> pParent, LiteralArgumentBuilder<CommandSourceStack> pLiteral, boolean pIsIf, CommandBuildContext pContext) {
      pLiteral.then(Commands.literal("block").then(Commands.argument("pos", BlockPosArgument.blockPos()).then(addConditional(pParent, Commands.argument("block", BlockPredicateArgument.blockPredicate(pContext)), pIsIf, (p_137277_) -> {
         return BlockPredicateArgument.getBlockPredicate(p_137277_, "block").test(new BlockInWorld(p_137277_.getSource().getLevel(), BlockPosArgument.getLoadedBlockPos(p_137277_, "pos"), true));
      })))).then(Commands.literal("score").then(Commands.argument("target", ScoreHolderArgument.scoreHolder()).suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS).then(Commands.argument("targetObjective", ObjectiveArgument.objective()).then(Commands.literal("=").then(Commands.argument("source", ScoreHolderArgument.scoreHolder()).suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS).then(addConditional(pParent, Commands.argument("sourceObjective", ObjectiveArgument.objective()), pIsIf, (p_137275_) -> {
         return checkScore(p_137275_, Integer::equals);
      })))).then(Commands.literal("<").then(Commands.argument("source", ScoreHolderArgument.scoreHolder()).suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS).then(addConditional(pParent, Commands.argument("sourceObjective", ObjectiveArgument.objective()), pIsIf, (p_137273_) -> {
         return checkScore(p_137273_, (p_180204_, p_180205_) -> {
            return p_180204_ < p_180205_;
         });
      })))).then(Commands.literal("<=").then(Commands.argument("source", ScoreHolderArgument.scoreHolder()).suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS).then(addConditional(pParent, Commands.argument("sourceObjective", ObjectiveArgument.objective()), pIsIf, (p_137261_) -> {
         return checkScore(p_137261_, (p_180194_, p_180195_) -> {
            return p_180194_ <= p_180195_;
         });
      })))).then(Commands.literal(">").then(Commands.argument("source", ScoreHolderArgument.scoreHolder()).suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS).then(addConditional(pParent, Commands.argument("sourceObjective", ObjectiveArgument.objective()), pIsIf, (p_137249_) -> {
         return checkScore(p_137249_, (p_180184_, p_180185_) -> {
            return p_180184_ > p_180185_;
         });
      })))).then(Commands.literal(">=").then(Commands.argument("source", ScoreHolderArgument.scoreHolder()).suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS).then(addConditional(pParent, Commands.argument("sourceObjective", ObjectiveArgument.objective()), pIsIf, (p_137234_) -> {
         return checkScore(p_137234_, (p_180167_, p_180168_) -> {
            return p_180167_ >= p_180168_;
         });
      })))).then(Commands.literal("matches").then(addConditional(pParent, Commands.argument("range", RangeArgument.intRange()), pIsIf, (p_137216_) -> {
         return checkScore(p_137216_, RangeArgument.Ints.getRange(p_137216_, "range"));
      })))))).then(Commands.literal("blocks").then(Commands.argument("start", BlockPosArgument.blockPos()).then(Commands.argument("end", BlockPosArgument.blockPos()).then(Commands.argument("destination", BlockPosArgument.blockPos()).then(addIfBlocksConditional(pParent, Commands.literal("all"), pIsIf, false)).then(addIfBlocksConditional(pParent, Commands.literal("masked"), pIsIf, true)))))).then(Commands.literal("entity").then(Commands.argument("entities", EntityArgument.entities()).fork(pParent, (p_137232_) -> {
         return expect(p_137232_, pIsIf, !EntityArgument.getOptionalEntities(p_137232_, "entities").isEmpty());
      }).executes(createNumericConditionalHandler(pIsIf, (p_137189_) -> {
         return EntityArgument.getOptionalEntities(p_137189_, "entities").size();
      })))).then(Commands.literal("predicate").then(addConditional(pParent, Commands.argument("predicate", ResourceLocationArgument.id()).suggests(SUGGEST_PREDICATE), pIsIf, (p_137054_) -> {
         return checkCustomPredicate(p_137054_.getSource(), ResourceLocationArgument.getPredicate(p_137054_, "predicate"));
      })));

      for(DataCommands.DataProvider datacommands$dataprovider : DataCommands.SOURCE_PROVIDERS) {
         pLiteral.then(datacommands$dataprovider.wrap(Commands.literal("data"), (p_137092_) -> {
            return p_137092_.then(Commands.argument("path", NbtPathArgument.nbtPath()).fork(pParent, (p_180175_) -> {
               return expect(p_180175_, pIsIf, checkMatchingData(datacommands$dataprovider.access(p_180175_), NbtPathArgument.getPath(p_180175_, "path")) > 0);
            }).executes(createNumericConditionalHandler(pIsIf, (p_180152_) -> {
               return checkMatchingData(datacommands$dataprovider.access(p_180152_), NbtPathArgument.getPath(p_180152_, "path"));
            })));
         }));
      }

      return pLiteral;
   }

   private static Command<CommandSourceStack> createNumericConditionalHandler(boolean p_137167_, ExecuteCommand.CommandNumericPredicate p_137168_) {
      return p_137167_ ? (p_137203_) -> {
         int i = p_137168_.test(p_137203_);
         if (i > 0) {
            p_137203_.getSource().sendSuccess(Component.translatable("commands.execute.conditional.pass_count", i), false);
            return i;
         } else {
            throw ERROR_CONDITIONAL_FAILED.create();
         }
      } : (p_137144_) -> {
         int i = p_137168_.test(p_137144_);
         if (i == 0) {
            p_137144_.getSource().sendSuccess(Component.translatable("commands.execute.conditional.pass"), false);
            return 1;
         } else {
            throw ERROR_CONDITIONAL_FAILED_COUNT.create(i);
         }
      };
   }

   private static int checkMatchingData(DataAccessor pAccessor, NbtPathArgument.NbtPath pPath) throws CommandSyntaxException {
      return pPath.countMatching(pAccessor.getData());
   }

   private static boolean checkScore(CommandContext<CommandSourceStack> pContext, BiPredicate<Integer, Integer> pComparison) throws CommandSyntaxException {
      String s = ScoreHolderArgument.getName(pContext, "target");
      Objective objective = ObjectiveArgument.getObjective(pContext, "targetObjective");
      String s1 = ScoreHolderArgument.getName(pContext, "source");
      Objective objective1 = ObjectiveArgument.getObjective(pContext, "sourceObjective");
      Scoreboard scoreboard = pContext.getSource().getServer().getScoreboard();
      if (scoreboard.hasPlayerScore(s, objective) && scoreboard.hasPlayerScore(s1, objective1)) {
         Score score = scoreboard.getOrCreatePlayerScore(s, objective);
         Score score1 = scoreboard.getOrCreatePlayerScore(s1, objective1);
         return pComparison.test(score.getScore(), score1.getScore());
      } else {
         return false;
      }
   }

   private static boolean checkScore(CommandContext<CommandSourceStack> pContext, MinMaxBounds.Ints pBounds) throws CommandSyntaxException {
      String s = ScoreHolderArgument.getName(pContext, "target");
      Objective objective = ObjectiveArgument.getObjective(pContext, "targetObjective");
      Scoreboard scoreboard = pContext.getSource().getServer().getScoreboard();
      return !scoreboard.hasPlayerScore(s, objective) ? false : pBounds.matches(scoreboard.getOrCreatePlayerScore(s, objective).getScore());
   }

   private static boolean checkCustomPredicate(CommandSourceStack pSource, LootItemCondition pPredicate) {
      ServerLevel serverlevel = pSource.getLevel();
      LootContext.Builder lootcontext$builder = (new LootContext.Builder(serverlevel)).withParameter(LootContextParams.ORIGIN, pSource.getPosition()).withOptionalParameter(LootContextParams.THIS_ENTITY, pSource.getEntity());
      return pPredicate.test(lootcontext$builder.create(LootContextParamSets.COMMAND));
   }

   /**
    * If actual and expected match, returns a collection containing only the source player.
    */
   private static Collection<CommandSourceStack> expect(CommandContext<CommandSourceStack> pContext, boolean pActual, boolean pExpected) {
      return (Collection<CommandSourceStack>)(pExpected == pActual ? Collections.singleton(pContext.getSource()) : Collections.emptyList());
   }

   private static ArgumentBuilder<CommandSourceStack, ?> addConditional(CommandNode<CommandSourceStack> pContext, ArgumentBuilder<CommandSourceStack, ?> pBuilder, boolean pValue, ExecuteCommand.CommandPredicate pTest) {
      return pBuilder.fork(pContext, (p_137214_) -> {
         return expect(p_137214_, pValue, pTest.test(p_137214_));
      }).executes((p_137172_) -> {
         if (pValue == pTest.test(p_137172_)) {
            p_137172_.getSource().sendSuccess(Component.translatable("commands.execute.conditional.pass"), false);
            return 1;
         } else {
            throw ERROR_CONDITIONAL_FAILED.create();
         }
      });
   }

   private static ArgumentBuilder<CommandSourceStack, ?> addIfBlocksConditional(CommandNode<CommandSourceStack> pParent, ArgumentBuilder<CommandSourceStack, ?> pLiteral, boolean pIsIf, boolean pIsMasked) {
      return pLiteral.fork(pParent, (p_137180_) -> {
         return expect(p_137180_, pIsIf, checkRegions(p_137180_, pIsMasked).isPresent());
      }).executes(pIsIf ? (p_137210_) -> {
         return checkIfRegions(p_137210_, pIsMasked);
      } : (p_137165_) -> {
         return checkUnlessRegions(p_137165_, pIsMasked);
      });
   }

   private static int checkIfRegions(CommandContext<CommandSourceStack> pContext, boolean pIsMasked) throws CommandSyntaxException {
      OptionalInt optionalint = checkRegions(pContext, pIsMasked);
      if (optionalint.isPresent()) {
         pContext.getSource().sendSuccess(Component.translatable("commands.execute.conditional.pass_count", optionalint.getAsInt()), false);
         return optionalint.getAsInt();
      } else {
         throw ERROR_CONDITIONAL_FAILED.create();
      }
   }

   private static int checkUnlessRegions(CommandContext<CommandSourceStack> pContext, boolean pIsMasked) throws CommandSyntaxException {
      OptionalInt optionalint = checkRegions(pContext, pIsMasked);
      if (optionalint.isPresent()) {
         throw ERROR_CONDITIONAL_FAILED_COUNT.create(optionalint.getAsInt());
      } else {
         pContext.getSource().sendSuccess(Component.translatable("commands.execute.conditional.pass"), false);
         return 1;
      }
   }

   private static OptionalInt checkRegions(CommandContext<CommandSourceStack> pContext, boolean pIsMasked) throws CommandSyntaxException {
      return checkRegions(pContext.getSource().getLevel(), BlockPosArgument.getLoadedBlockPos(pContext, "start"), BlockPosArgument.getLoadedBlockPos(pContext, "end"), BlockPosArgument.getLoadedBlockPos(pContext, "destination"), pIsMasked);
   }

   private static OptionalInt checkRegions(ServerLevel pLevel, BlockPos pBegin, BlockPos pEnd, BlockPos pDestination, boolean pIsMasked) throws CommandSyntaxException {
      BoundingBox boundingbox = BoundingBox.fromCorners(pBegin, pEnd);
      BoundingBox boundingbox1 = BoundingBox.fromCorners(pDestination, pDestination.offset(boundingbox.getLength()));
      BlockPos blockpos = new BlockPos(boundingbox1.minX() - boundingbox.minX(), boundingbox1.minY() - boundingbox.minY(), boundingbox1.minZ() - boundingbox.minZ());
      int i = boundingbox.getXSpan() * boundingbox.getYSpan() * boundingbox.getZSpan();
      if (i > 32768) {
         throw ERROR_AREA_TOO_LARGE.create(32768, i);
      } else {
         int j = 0;

         for(int k = boundingbox.minZ(); k <= boundingbox.maxZ(); ++k) {
            for(int l = boundingbox.minY(); l <= boundingbox.maxY(); ++l) {
               for(int i1 = boundingbox.minX(); i1 <= boundingbox.maxX(); ++i1) {
                  BlockPos blockpos1 = new BlockPos(i1, l, k);
                  BlockPos blockpos2 = blockpos1.offset(blockpos);
                  BlockState blockstate = pLevel.getBlockState(blockpos1);
                  if (!pIsMasked || !blockstate.is(Blocks.AIR)) {
                     if (blockstate != pLevel.getBlockState(blockpos2)) {
                        return OptionalInt.empty();
                     }

                     BlockEntity blockentity = pLevel.getBlockEntity(blockpos1);
                     BlockEntity blockentity1 = pLevel.getBlockEntity(blockpos2);
                     if (blockentity != null) {
                        if (blockentity1 == null) {
                           return OptionalInt.empty();
                        }

                        if (blockentity1.getType() != blockentity.getType()) {
                           return OptionalInt.empty();
                        }

                        CompoundTag compoundtag = blockentity.saveWithoutMetadata();
                        CompoundTag compoundtag1 = blockentity1.saveWithoutMetadata();
                        if (!compoundtag.equals(compoundtag1)) {
                           return OptionalInt.empty();
                        }
                     }

                     ++j;
                  }
               }
            }
         }

         return OptionalInt.of(j);
      }
   }

   @FunctionalInterface
   interface CommandNumericPredicate {
      int test(CommandContext<CommandSourceStack> pContext) throws CommandSyntaxException;
   }

   @FunctionalInterface
   interface CommandPredicate {
      boolean test(CommandContext<CommandSourceStack> pContext) throws CommandSyntaxException;
   }
}