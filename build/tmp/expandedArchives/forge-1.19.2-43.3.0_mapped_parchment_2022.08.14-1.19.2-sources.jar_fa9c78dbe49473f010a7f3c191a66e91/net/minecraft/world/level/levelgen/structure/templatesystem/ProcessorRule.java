package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

public class ProcessorRule {
   public static final Codec<ProcessorRule> CODEC = RecordCodecBuilder.create((p_74246_) -> {
      return p_74246_.group(RuleTest.CODEC.fieldOf("input_predicate").forGetter((p_163747_) -> {
         return p_163747_.inputPredicate;
      }), RuleTest.CODEC.fieldOf("location_predicate").forGetter((p_163745_) -> {
         return p_163745_.locPredicate;
      }), PosRuleTest.CODEC.optionalFieldOf("position_predicate", PosAlwaysTrueTest.INSTANCE).forGetter((p_163743_) -> {
         return p_163743_.posPredicate;
      }), BlockState.CODEC.fieldOf("output_state").forGetter((p_163741_) -> {
         return p_163741_.outputState;
      }), CompoundTag.CODEC.optionalFieldOf("output_nbt").forGetter((p_163739_) -> {
         return Optional.ofNullable(p_163739_.outputTag);
      })).apply(p_74246_, ProcessorRule::new);
   });
   private final RuleTest inputPredicate;
   private final RuleTest locPredicate;
   private final PosRuleTest posPredicate;
   private final BlockState outputState;
   @Nullable
   private final CompoundTag outputTag;

   public ProcessorRule(RuleTest pInputPredicate, RuleTest pLocPredicate, BlockState pOutputState) {
      this(pInputPredicate, pLocPredicate, PosAlwaysTrueTest.INSTANCE, pOutputState, Optional.empty());
   }

   public ProcessorRule(RuleTest pInputPredicate, RuleTest pLocPredicate, PosRuleTest pPosPredicate, BlockState pOutputState) {
      this(pInputPredicate, pLocPredicate, pPosPredicate, pOutputState, Optional.empty());
   }

   public ProcessorRule(RuleTest p_74232_, RuleTest p_74233_, PosRuleTest p_74234_, BlockState p_74235_, Optional<CompoundTag> p_74236_) {
      this.inputPredicate = p_74232_;
      this.locPredicate = p_74233_;
      this.posPredicate = p_74234_;
      this.outputState = p_74235_;
      this.outputTag = p_74236_.orElse((CompoundTag)null);
   }

   /**
    * 
    * @param pInputState The incoming state from the structure.
    * @param pExistingState The current state in the world.
    * @param pLocalPos The local position of the target state, relative to the structure origin.
    * @param pRelativePos The actual position of the target state. {@code existingState} is the current in world state
    * at this position.
    * @param pStructurePos The origin position of the structure.
    */
   public boolean test(BlockState pInputState, BlockState pExistingState, BlockPos pLocalPos, BlockPos pRelativePos, BlockPos pStructurePos, RandomSource pRandom) {
      return this.inputPredicate.test(pInputState, pRandom) && this.locPredicate.test(pExistingState, pRandom) && this.posPredicate.test(pLocalPos, pRelativePos, pStructurePos, pRandom);
   }

   public BlockState getOutputState() {
      return this.outputState;
   }

   @Nullable
   public CompoundTag getOutputTag() {
      return this.outputTag;
   }
}