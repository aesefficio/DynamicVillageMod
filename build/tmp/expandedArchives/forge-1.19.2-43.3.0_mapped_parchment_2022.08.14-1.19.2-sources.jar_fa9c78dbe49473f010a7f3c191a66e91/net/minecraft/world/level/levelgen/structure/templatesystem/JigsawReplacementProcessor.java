package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import javax.annotation.Nullable;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class JigsawReplacementProcessor extends StructureProcessor {
   public static final Codec<JigsawReplacementProcessor> CODEC = Codec.unit(() -> {
      return JigsawReplacementProcessor.INSTANCE;
   });
   public static final JigsawReplacementProcessor INSTANCE = new JigsawReplacementProcessor();

   private JigsawReplacementProcessor() {
   }

   @Nullable
   public StructureTemplate.StructureBlockInfo processBlock(LevelReader p_74127_, BlockPos p_74128_, BlockPos p_74129_, StructureTemplate.StructureBlockInfo p_74130_, StructureTemplate.StructureBlockInfo p_74131_, StructurePlaceSettings p_74132_) {
      BlockState blockstate = p_74131_.state;
      if (blockstate.is(Blocks.JIGSAW)) {
         String s = p_74131_.nbt.getString("final_state");

         BlockState blockstate1;
         try {
            BlockStateParser.BlockResult blockstateparser$blockresult = BlockStateParser.parseForBlock(Registry.BLOCK, s, true);
            blockstate1 = blockstateparser$blockresult.blockState();
         } catch (CommandSyntaxException commandsyntaxexception) {
            throw new RuntimeException(commandsyntaxexception);
         }

         return blockstate1.is(Blocks.STRUCTURE_VOID) ? null : new StructureTemplate.StructureBlockInfo(p_74131_.pos, blockstate1, (CompoundTag)null);
      } else {
         return p_74131_;
      }
   }

   protected StructureProcessorType<?> getType() {
      return StructureProcessorType.JIGSAW_REPLACEMENT;
   }
}