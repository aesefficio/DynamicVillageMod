package net.minecraft.world.level.levelgen.feature.foliageplacers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;

public class BushFoliagePlacer extends BlobFoliagePlacer {
   public static final Codec<BushFoliagePlacer> CODEC = RecordCodecBuilder.create((p_68454_) -> {
      return blobParts(p_68454_).apply(p_68454_, BushFoliagePlacer::new);
   });

   public BushFoliagePlacer(IntProvider p_161370_, IntProvider p_161371_, int p_161372_) {
      super(p_161370_, p_161371_, p_161372_);
   }

   protected FoliagePlacerType<?> type() {
      return FoliagePlacerType.BUSH_FOLIAGE_PLACER;
   }

   protected void createFoliage(LevelSimulatedReader p_225537_, BiConsumer<BlockPos, BlockState> p_225538_, RandomSource p_225539_, TreeConfiguration p_225540_, int p_225541_, FoliagePlacer.FoliageAttachment p_225542_, int p_225543_, int p_225544_, int p_225545_) {
      for(int i = p_225545_; i >= p_225545_ - p_225543_; --i) {
         int j = p_225544_ + p_225542_.radiusOffset() - 1 - i;
         this.placeLeavesRow(p_225537_, p_225538_, p_225539_, p_225540_, p_225542_.pos(), j, i, p_225542_.doubleTrunk());
      }

   }

   /**
    * Skips certain positions based on the provided shape, such as rounding corners randomly.
    * The coordinates are passed in as absolute value, and should be within [0, {@code range}].
    */
   protected boolean shouldSkipLocation(RandomSource p_225530_, int p_225531_, int p_225532_, int p_225533_, int p_225534_, boolean p_225535_) {
      return p_225531_ == p_225534_ && p_225533_ == p_225534_ && p_225530_.nextInt(2) == 0;
   }
}