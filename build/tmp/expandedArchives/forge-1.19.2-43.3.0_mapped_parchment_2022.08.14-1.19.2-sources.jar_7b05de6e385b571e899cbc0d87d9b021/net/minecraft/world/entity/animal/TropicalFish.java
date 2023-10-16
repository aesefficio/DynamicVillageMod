package net.minecraft.world.entity.animal;

import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;

public class TropicalFish extends AbstractSchoolingFish {
   public static final String BUCKET_VARIANT_TAG = "BucketVariantTag";
   private static final EntityDataAccessor<Integer> DATA_ID_TYPE_VARIANT = SynchedEntityData.defineId(TropicalFish.class, EntityDataSerializers.INT);
   public static final int BASE_SMALL = 0;
   public static final int BASE_LARGE = 1;
   private static final int BASES = 2;
   private static final ResourceLocation[] BASE_TEXTURE_LOCATIONS = new ResourceLocation[]{new ResourceLocation("textures/entity/fish/tropical_a.png"), new ResourceLocation("textures/entity/fish/tropical_b.png")};
   private static final ResourceLocation[] PATTERN_A_TEXTURE_LOCATIONS = new ResourceLocation[]{new ResourceLocation("textures/entity/fish/tropical_a_pattern_1.png"), new ResourceLocation("textures/entity/fish/tropical_a_pattern_2.png"), new ResourceLocation("textures/entity/fish/tropical_a_pattern_3.png"), new ResourceLocation("textures/entity/fish/tropical_a_pattern_4.png"), new ResourceLocation("textures/entity/fish/tropical_a_pattern_5.png"), new ResourceLocation("textures/entity/fish/tropical_a_pattern_6.png")};
   private static final ResourceLocation[] PATTERN_B_TEXTURE_LOCATIONS = new ResourceLocation[]{new ResourceLocation("textures/entity/fish/tropical_b_pattern_1.png"), new ResourceLocation("textures/entity/fish/tropical_b_pattern_2.png"), new ResourceLocation("textures/entity/fish/tropical_b_pattern_3.png"), new ResourceLocation("textures/entity/fish/tropical_b_pattern_4.png"), new ResourceLocation("textures/entity/fish/tropical_b_pattern_5.png"), new ResourceLocation("textures/entity/fish/tropical_b_pattern_6.png")};
   private static final int PATTERNS = 6;
   private static final int COLORS = 15;
   public static final int[] COMMON_VARIANTS = new int[]{calculateVariant(TropicalFish.Pattern.STRIPEY, DyeColor.ORANGE, DyeColor.GRAY), calculateVariant(TropicalFish.Pattern.FLOPPER, DyeColor.GRAY, DyeColor.GRAY), calculateVariant(TropicalFish.Pattern.FLOPPER, DyeColor.GRAY, DyeColor.BLUE), calculateVariant(TropicalFish.Pattern.CLAYFISH, DyeColor.WHITE, DyeColor.GRAY), calculateVariant(TropicalFish.Pattern.SUNSTREAK, DyeColor.BLUE, DyeColor.GRAY), calculateVariant(TropicalFish.Pattern.KOB, DyeColor.ORANGE, DyeColor.WHITE), calculateVariant(TropicalFish.Pattern.SPOTTY, DyeColor.PINK, DyeColor.LIGHT_BLUE), calculateVariant(TropicalFish.Pattern.BLOCKFISH, DyeColor.PURPLE, DyeColor.YELLOW), calculateVariant(TropicalFish.Pattern.CLAYFISH, DyeColor.WHITE, DyeColor.RED), calculateVariant(TropicalFish.Pattern.SPOTTY, DyeColor.WHITE, DyeColor.YELLOW), calculateVariant(TropicalFish.Pattern.GLITTER, DyeColor.WHITE, DyeColor.GRAY), calculateVariant(TropicalFish.Pattern.CLAYFISH, DyeColor.WHITE, DyeColor.ORANGE), calculateVariant(TropicalFish.Pattern.DASHER, DyeColor.CYAN, DyeColor.PINK), calculateVariant(TropicalFish.Pattern.BRINELY, DyeColor.LIME, DyeColor.LIGHT_BLUE), calculateVariant(TropicalFish.Pattern.BETTY, DyeColor.RED, DyeColor.WHITE), calculateVariant(TropicalFish.Pattern.SNOOPER, DyeColor.GRAY, DyeColor.RED), calculateVariant(TropicalFish.Pattern.BLOCKFISH, DyeColor.RED, DyeColor.WHITE), calculateVariant(TropicalFish.Pattern.FLOPPER, DyeColor.WHITE, DyeColor.YELLOW), calculateVariant(TropicalFish.Pattern.KOB, DyeColor.RED, DyeColor.WHITE), calculateVariant(TropicalFish.Pattern.SUNSTREAK, DyeColor.GRAY, DyeColor.WHITE), calculateVariant(TropicalFish.Pattern.DASHER, DyeColor.CYAN, DyeColor.YELLOW), calculateVariant(TropicalFish.Pattern.FLOPPER, DyeColor.YELLOW, DyeColor.YELLOW)};
   private boolean isSchool = true;

   private static int calculateVariant(TropicalFish.Pattern pPattern, DyeColor pBaseColor, DyeColor pPatternColor) {
      return pPattern.getBase() & 255 | (pPattern.getIndex() & 255) << 8 | (pBaseColor.getId() & 255) << 16 | (pPatternColor.getId() & 255) << 24;
   }

   public TropicalFish(EntityType<? extends TropicalFish> pEntityType, Level pLevel) {
      super(pEntityType, pLevel);
   }

   public static String getPredefinedName(int pVariantId) {
      return "entity.minecraft.tropical_fish.predefined." + pVariantId;
   }

   public static DyeColor getBaseColor(int pVariantId) {
      return DyeColor.byId(getBaseColorIdx(pVariantId));
   }

   public static DyeColor getPatternColor(int pVariantId) {
      return DyeColor.byId(getPatternColorIdx(pVariantId));
   }

   public static String getFishTypeName(int pVariantId) {
      int i = getBaseVariant(pVariantId);
      int j = getPatternVariant(pVariantId);
      return "entity.minecraft.tropical_fish.type." + TropicalFish.Pattern.getPatternName(i, j);
   }

   protected void defineSynchedData() {
      super.defineSynchedData();
      this.entityData.define(DATA_ID_TYPE_VARIANT, 0);
   }

   public void addAdditionalSaveData(CompoundTag pCompound) {
      super.addAdditionalSaveData(pCompound);
      pCompound.putInt("Variant", this.getVariant());
   }

   /**
    * (abstract) Protected helper method to read subclass entity data from NBT.
    */
   public void readAdditionalSaveData(CompoundTag pCompound) {
      super.readAdditionalSaveData(pCompound);
      this.setVariant(pCompound.getInt("Variant"));
   }

   public void setVariant(int pVariantId) {
      this.entityData.set(DATA_ID_TYPE_VARIANT, pVariantId);
   }

   public boolean isMaxGroupSizeReached(int pSize) {
      return !this.isSchool;
   }

   public int getVariant() {
      return this.entityData.get(DATA_ID_TYPE_VARIANT);
   }

   public void saveToBucketTag(ItemStack pStack) {
      super.saveToBucketTag(pStack);
      CompoundTag compoundtag = pStack.getOrCreateTag();
      compoundtag.putInt("BucketVariantTag", this.getVariant());
   }

   public ItemStack getBucketItemStack() {
      return new ItemStack(Items.TROPICAL_FISH_BUCKET);
   }

   protected SoundEvent getAmbientSound() {
      return SoundEvents.TROPICAL_FISH_AMBIENT;
   }

   protected SoundEvent getDeathSound() {
      return SoundEvents.TROPICAL_FISH_DEATH;
   }

   protected SoundEvent getHurtSound(DamageSource pDamageSource) {
      return SoundEvents.TROPICAL_FISH_HURT;
   }

   protected SoundEvent getFlopSound() {
      return SoundEvents.TROPICAL_FISH_FLOP;
   }

   private static int getBaseColorIdx(int pVariantId) {
      return (pVariantId & 16711680) >> 16;
   }

   public float[] getBaseColor() {
      return DyeColor.byId(getBaseColorIdx(this.getVariant())).getTextureDiffuseColors();
   }

   private static int getPatternColorIdx(int pVariantId) {
      return (pVariantId & -16777216) >> 24;
   }

   public float[] getPatternColor() {
      return DyeColor.byId(getPatternColorIdx(this.getVariant())).getTextureDiffuseColors();
   }

   public static int getBaseVariant(int pVariantId) {
      return Math.min(pVariantId & 255, 1);
   }

   public int getBaseVariant() {
      return getBaseVariant(this.getVariant());
   }

   private static int getPatternVariant(int pVariantId) {
      return Math.min((pVariantId & '\uff00') >> 8, 5);
   }

   public ResourceLocation getPatternTextureLocation() {
      return getBaseVariant(this.getVariant()) == 0 ? PATTERN_A_TEXTURE_LOCATIONS[getPatternVariant(this.getVariant())] : PATTERN_B_TEXTURE_LOCATIONS[getPatternVariant(this.getVariant())];
   }

   public ResourceLocation getBaseTextureLocation() {
      return BASE_TEXTURE_LOCATIONS[getBaseVariant(this.getVariant())];
   }

   @Nullable
   public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData, @Nullable CompoundTag pDataTag) {
      pSpawnData = super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData, pDataTag);
      if (pReason == MobSpawnType.BUCKET && pDataTag != null && pDataTag.contains("BucketVariantTag", 3)) {
         this.setVariant(pDataTag.getInt("BucketVariantTag"));
         return pSpawnData;
      } else {
         RandomSource randomsource = pLevel.getRandom();
         int i;
         int j;
         int k;
         int l;
         if (pSpawnData instanceof TropicalFish.TropicalFishGroupData) {
            TropicalFish.TropicalFishGroupData tropicalfish$tropicalfishgroupdata = (TropicalFish.TropicalFishGroupData)pSpawnData;
            i = tropicalfish$tropicalfishgroupdata.base;
            j = tropicalfish$tropicalfishgroupdata.pattern;
            k = tropicalfish$tropicalfishgroupdata.baseColor;
            l = tropicalfish$tropicalfishgroupdata.patternColor;
         } else if ((double)randomsource.nextFloat() < 0.9D) {
            int i1 = Util.getRandom(COMMON_VARIANTS, randomsource);
            i = i1 & 255;
            j = (i1 & '\uff00') >> 8;
            k = (i1 & 16711680) >> 16;
            l = (i1 & -16777216) >> 24;
            pSpawnData = new TropicalFish.TropicalFishGroupData(this, i, j, k, l);
         } else {
            this.isSchool = false;
            i = randomsource.nextInt(2);
            j = randomsource.nextInt(6);
            k = randomsource.nextInt(15);
            l = randomsource.nextInt(15);
         }

         this.setVariant(i | j << 8 | k << 16 | l << 24);
         return pSpawnData;
      }
   }

   public static boolean checkTropicalFishSpawnRules(EntityType<TropicalFish> pTropicalFish, LevelAccessor pLevel, MobSpawnType pSpawnType, BlockPos pPos, RandomSource pRandom) {
      return pLevel.getFluidState(pPos.below()).is(FluidTags.WATER) && pLevel.getBlockState(pPos.above()).is(Blocks.WATER) && (pLevel.getBiome(pPos).is(BiomeTags.ALLOWS_TROPICAL_FISH_SPAWNS_AT_ANY_HEIGHT) || WaterAnimal.checkSurfaceWaterAnimalSpawnRules(pTropicalFish, pLevel, pSpawnType, pPos, pRandom));
   }

   static enum Pattern {
      KOB(0, 0),
      SUNSTREAK(0, 1),
      SNOOPER(0, 2),
      DASHER(0, 3),
      BRINELY(0, 4),
      SPOTTY(0, 5),
      FLOPPER(1, 0),
      STRIPEY(1, 1),
      GLITTER(1, 2),
      BLOCKFISH(1, 3),
      BETTY(1, 4),
      CLAYFISH(1, 5);

      private final int base;
      private final int index;
      private static final TropicalFish.Pattern[] VALUES = values();

      private Pattern(int pBase, int pIndex) {
         this.base = pBase;
         this.index = pIndex;
      }

      public int getBase() {
         return this.base;
      }

      public int getIndex() {
         return this.index;
      }

      public static String getPatternName(int pBaseVariant, int pPatternVariant) {
         return VALUES[pPatternVariant + 6 * pBaseVariant].getName();
      }

      public String getName() {
         return this.name().toLowerCase(Locale.ROOT);
      }
   }

   static class TropicalFishGroupData extends AbstractSchoolingFish.SchoolSpawnGroupData {
      final int base;
      final int pattern;
      final int baseColor;
      final int patternColor;

      TropicalFishGroupData(TropicalFish pFish, int pBase, int pPattern, int pBaseColor, int pPatternColor) {
         super(pFish);
         this.base = pBase;
         this.pattern = pPattern;
         this.baseColor = pBaseColor;
         this.patternColor = pPatternColor;
      }
   }
}