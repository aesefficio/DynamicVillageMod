package net.minecraft.data;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.world.level.block.Block;
import org.apache.commons.lang3.StringUtils;

public class BlockFamily {
   private final Block baseBlock;
   final Map<BlockFamily.Variant, Block> variants = Maps.newHashMap();
   boolean generateModel = true;
   boolean generateRecipe = true;
   @Nullable
   String recipeGroupPrefix;
   @Nullable
   String recipeUnlockedBy;

   BlockFamily(Block pBaseBlock) {
      this.baseBlock = pBaseBlock;
   }

   public Block getBaseBlock() {
      return this.baseBlock;
   }

   public Map<BlockFamily.Variant, Block> getVariants() {
      return this.variants;
   }

   public Block get(BlockFamily.Variant pVariant) {
      return this.variants.get(pVariant);
   }

   public boolean shouldGenerateModel() {
      return this.generateModel;
   }

   public boolean shouldGenerateRecipe() {
      return this.generateRecipe;
   }

   public Optional<String> getRecipeGroupPrefix() {
      return StringUtils.isBlank(this.recipeGroupPrefix) ? Optional.empty() : Optional.of(this.recipeGroupPrefix);
   }

   public Optional<String> getRecipeUnlockedBy() {
      return StringUtils.isBlank(this.recipeUnlockedBy) ? Optional.empty() : Optional.of(this.recipeUnlockedBy);
   }

   public static class Builder {
      private final BlockFamily family;

      public Builder(Block pBaseBlock) {
         this.family = new BlockFamily(pBaseBlock);
      }

      public BlockFamily getFamily() {
         return this.family;
      }

      public BlockFamily.Builder button(Block pButtonBlock) {
         this.family.variants.put(BlockFamily.Variant.BUTTON, pButtonBlock);
         return this;
      }

      public BlockFamily.Builder chiseled(Block pChiseledBlock) {
         this.family.variants.put(BlockFamily.Variant.CHISELED, pChiseledBlock);
         return this;
      }

      public BlockFamily.Builder cracked(Block pCrackedBlock) {
         this.family.variants.put(BlockFamily.Variant.CRACKED, pCrackedBlock);
         return this;
      }

      public BlockFamily.Builder cut(Block pCutBlock) {
         this.family.variants.put(BlockFamily.Variant.CUT, pCutBlock);
         return this;
      }

      public BlockFamily.Builder door(Block pDoorBlock) {
         this.family.variants.put(BlockFamily.Variant.DOOR, pDoorBlock);
         return this;
      }

      public BlockFamily.Builder fence(Block pFenceBlock) {
         this.family.variants.put(BlockFamily.Variant.FENCE, pFenceBlock);
         return this;
      }

      public BlockFamily.Builder fenceGate(Block pFenceGateBlock) {
         this.family.variants.put(BlockFamily.Variant.FENCE_GATE, pFenceGateBlock);
         return this;
      }

      public BlockFamily.Builder sign(Block pSignBlock, Block pWallSignBlock) {
         this.family.variants.put(BlockFamily.Variant.SIGN, pSignBlock);
         this.family.variants.put(BlockFamily.Variant.WALL_SIGN, pWallSignBlock);
         return this;
      }

      public BlockFamily.Builder slab(Block pSlabBlock) {
         this.family.variants.put(BlockFamily.Variant.SLAB, pSlabBlock);
         return this;
      }

      public BlockFamily.Builder stairs(Block pStairsBlock) {
         this.family.variants.put(BlockFamily.Variant.STAIRS, pStairsBlock);
         return this;
      }

      public BlockFamily.Builder pressurePlate(Block pPressurePlateBlock) {
         this.family.variants.put(BlockFamily.Variant.PRESSURE_PLATE, pPressurePlateBlock);
         return this;
      }

      public BlockFamily.Builder polished(Block pPolishedBlock) {
         this.family.variants.put(BlockFamily.Variant.POLISHED, pPolishedBlock);
         return this;
      }

      public BlockFamily.Builder trapdoor(Block pTrapdoorBlock) {
         this.family.variants.put(BlockFamily.Variant.TRAPDOOR, pTrapdoorBlock);
         return this;
      }

      public BlockFamily.Builder wall(Block pWallBlock) {
         this.family.variants.put(BlockFamily.Variant.WALL, pWallBlock);
         return this;
      }

      public BlockFamily.Builder dontGenerateModel() {
         this.family.generateModel = false;
         return this;
      }

      public BlockFamily.Builder dontGenerateRecipe() {
         this.family.generateRecipe = false;
         return this;
      }

      public BlockFamily.Builder recipeGroupPrefix(String pRecipeGroupPrefix) {
         this.family.recipeGroupPrefix = pRecipeGroupPrefix;
         return this;
      }

      public BlockFamily.Builder recipeUnlockedBy(String pRecipeUnlockedBy) {
         this.family.recipeUnlockedBy = pRecipeUnlockedBy;
         return this;
      }
   }

   public static enum Variant {
      BUTTON("button"),
      CHISELED("chiseled"),
      CRACKED("cracked"),
      CUT("cut"),
      DOOR("door"),
      FENCE("fence"),
      FENCE_GATE("fence_gate"),
      SIGN("sign"),
      SLAB("slab"),
      STAIRS("stairs"),
      PRESSURE_PLATE("pressure_plate"),
      POLISHED("polished"),
      TRAPDOOR("trapdoor"),
      WALL("wall"),
      WALL_SIGN("wall_sign");

      private final String name;

      private Variant(String pVariantName) {
         this.name = pVariantName;
      }

      public String getName() {
         return this.name;
      }
   }
}