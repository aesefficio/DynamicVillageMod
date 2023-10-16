package net.minecraft.world.level.block.state;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.MapCodec;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class BlockBehaviour {
   protected static final Direction[] UPDATE_SHAPE_ORDER = new Direction[]{Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH, Direction.DOWN, Direction.UP};
   protected final Material material;
   protected final boolean hasCollision;
   protected final float explosionResistance;
   /** Whether this blocks receives random ticks */
   protected final boolean isRandomlyTicking;
   protected final SoundType soundType;
   /** Determines how much velocity is maintained while moving on top of this block */
   protected final float friction;
   protected final float speedFactor;
   protected final float jumpFactor;
   protected final boolean dynamicShape;
   protected final BlockBehaviour.Properties properties;
   @Nullable
   protected ResourceLocation drops;

   public BlockBehaviour(BlockBehaviour.Properties pProperties) {
      this.material = pProperties.material;
      this.hasCollision = pProperties.hasCollision;
      this.drops = pProperties.drops;
      this.explosionResistance = pProperties.explosionResistance;
      this.isRandomlyTicking = pProperties.isRandomlyTicking;
      this.soundType = pProperties.soundType;
      this.friction = pProperties.friction;
      this.speedFactor = pProperties.speedFactor;
      this.jumpFactor = pProperties.jumpFactor;
      this.dynamicShape = pProperties.dynamicShape;
      this.properties = pProperties;
      final ResourceLocation lootTableCache = pProperties.drops;
      if (lootTableCache != null) {
         this.lootTableSupplier = () -> lootTableCache;
      } else if (pProperties.lootTableSupplier != null) {
         this.lootTableSupplier = pProperties.lootTableSupplier;
      } else {
         this.lootTableSupplier = () -> {
            ResourceLocation registryName = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey((Block) this);
            return new ResourceLocation(registryName.getNamespace(), "blocks/" + registryName.getPath());
         };
      }
   }

   /** @deprecated */
   /**
    * Performs updates on diagonal neighbors of the target position and passes in the flags.
    * The flags are equivalent to {@link net.minecraft.world.level.Level#setBlock}.
    */
   @Deprecated
   public void updateIndirectNeighbourShapes(BlockState pState, LevelAccessor pLevel, BlockPos pPos, int pFlags, int pRecursionLeft) {
   }

   /** @deprecated */
   @Deprecated
   public boolean isPathfindable(BlockState pState, BlockGetter pLevel, BlockPos pPos, PathComputationType pType) {
      switch (pType) {
         case LAND:
            return !pState.isCollisionShapeFullBlock(pLevel, pPos);
         case WATER:
            return pLevel.getFluidState(pPos).is(FluidTags.WATER);
         case AIR:
            return !pState.isCollisionShapeFullBlock(pLevel, pPos);
         default:
            return false;
      }
   }

   /** @deprecated */
   /**
    * Update the provided state given the provided neighbor direction and neighbor state, returning a new state.
    * For example, fences make their connections to the passed in state if possible, and wet concrete powder immediately
    * returns its solidified counterpart.
    * Note that this method should ideally consider only the specific direction passed in.
    */
   @Deprecated
   public BlockState updateShape(BlockState pState, Direction pDirection, BlockState pNeighborState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pNeighborPos) {
      return pState;
   }

   /** @deprecated */
   @Deprecated
   public boolean skipRendering(BlockState pState, BlockState pAdjacentBlockState, Direction pDirection) {
      return false;
   }

   /** @deprecated */
   @Deprecated
   public void neighborChanged(BlockState pState, Level pLevel, BlockPos pPos, Block pBlock, BlockPos pFromPos, boolean pIsMoving) {
      DebugPackets.sendNeighborsUpdatePacket(pLevel, pPos);
   }

   /** @deprecated */
   @Deprecated
   public void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
   }

   /** @deprecated */
   @Deprecated
   public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
      if (pState.hasBlockEntity() && (!pState.is(pNewState.getBlock()) || !pNewState.hasBlockEntity())) {
         pLevel.removeBlockEntity(pPos);
      }

   }

   /** @deprecated */
   @Deprecated
   public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
      return InteractionResult.PASS;
   }

   /** @deprecated */
   /**
    * Called on server when {@link net.minecraft.world.level.Level#blockEvent} is called. If server returns true, then
    * also called on the client. On the Server, this may perform additional changes to the world, like pistons replacing
    * the block with an extended base. On the client, the update may involve replacing tile entities or effects such as
    * sounds or particles
    * @deprecated call via {@link
    * net.minecraft.world.level.block.state.BlockBehavior.BlockStateBase#onBlockEventReceived} whenever possible.
    * Implementing/overriding is fine.
    */
   @Deprecated
   public boolean triggerEvent(BlockState pState, Level pLevel, BlockPos pPos, int pId, int pParam) {
      return false;
   }

   /** @deprecated */
   /**
    * The type of render function called. MODEL for mixed tesr and static model, MODELBLOCK_ANIMATED for TESR-only,
    * LIQUID for vanilla liquids, INVISIBLE to skip all rendering
    * @deprecated call via {@link net.minecraft.world.level.block.state.BlockBehavior.BlockStateBase#getRenderShape}
    * whenever possible. Implementing/overriding is fine.
    */
   @Deprecated
   public RenderShape getRenderShape(BlockState pState) {
      return RenderShape.MODEL;
   }

   /** @deprecated */
   @Deprecated
   public boolean useShapeForLightOcclusion(BlockState pState) {
      return false;
   }

   /** @deprecated */
   /**
    * Can this block provide power. Only wire currently seems to have this change based on its state.
    * @deprecated call via {@link net.minecraft.world.level.block.state.BlockBehavior.BlockStateBase#isSignalSource}
    * whenever possible. Implementing/overriding is fine.
    */
   @Deprecated
   public boolean isSignalSource(BlockState pState) {
      return false;
   }

   /** @deprecated */
   /**
    * @deprecated call via {@link
    * net.minecraft.world.level.block.state.BlockBehavior.BlockStateBase#getPistonPushReaction} whenever possible.
    * Implementing/overriding is fine.
    */
   @Deprecated
   public PushReaction getPistonPushReaction(BlockState pState) {
      return this.material.getPushReaction();
   }

   /** @deprecated */
   @Deprecated
   public FluidState getFluidState(BlockState pState) {
      return Fluids.EMPTY.defaultFluidState();
   }

   /** @deprecated */
   /**
    * @deprecated call via {@link
    * net.minecraft.world.level.block.state.BlockBehavior.BlockStateBase#hasAnalogOutputSignal} whenever possible.
    * Implementing/overriding is fine.
    */
   @Deprecated
   public boolean hasAnalogOutputSignal(BlockState pState) {
      return false;
   }

   public float getMaxHorizontalOffset() {
      return 0.25F;
   }

   public float getMaxVerticalOffset() {
      return 0.2F;
   }

   /** @deprecated */
   /**
    * Returns the blockstate with the given rotation from the passed blockstate. If inapplicable, returns the passed
    * blockstate.
    * @deprecated call via {@link net.minecraft.world.level.block.state.BlockBehavior.BlockStateBase#rotate} whenever
    * possible. Implementing/overriding is fine.
    */
   @Deprecated
   public BlockState rotate(BlockState pState, Rotation pRotation) {
      return pState;
   }

   /** @deprecated */
   /**
    * Returns the blockstate with the given mirror of the passed blockstate. If inapplicable, returns the passed
    * blockstate.
    * @deprecated call via {@link net.minecraft.world.level.block.state.BlockBehavior.BlockStateBase#mirror} whenever
    * possible. Implementing/overriding is fine.
    */
   @Deprecated
   public BlockState mirror(BlockState pState, Mirror pMirror) {
      return pState;
   }

   /** @deprecated */
   @Deprecated
   public boolean canBeReplaced(BlockState pState, BlockPlaceContext pUseContext) {
      return pState.getMaterial().isReplaceable() && (pUseContext.getItemInHand().isEmpty() || pUseContext.getItemInHand().getItem() != this.asItem());
   }

   /** @deprecated */
   @Deprecated
   public boolean canBeReplaced(BlockState pState, Fluid pFluid) {
      return this.material.isReplaceable() || !this.material.isSolid();
   }

   /** @deprecated */
   @Deprecated
   public List<ItemStack> getDrops(BlockState pState, LootContext.Builder pBuilder) {
      ResourceLocation resourcelocation = this.getLootTable();
      if (resourcelocation == BuiltInLootTables.EMPTY) {
         return Collections.emptyList();
      } else {
         LootContext lootcontext = pBuilder.withParameter(LootContextParams.BLOCK_STATE, pState).create(LootContextParamSets.BLOCK);
         ServerLevel serverlevel = lootcontext.getLevel();
         LootTable loottable = serverlevel.getServer().getLootTables().get(resourcelocation);
         return loottable.getRandomItems(lootcontext);
      }
   }

   /** @deprecated */
   /**
    * Return a random long to be passed to {@link net.minecraft.client.resources.model.BakedModel#getQuads}, used for
    * random model rotations
    */
   @Deprecated
   public long getSeed(BlockState pState, BlockPos pPos) {
      return Mth.getSeed(pPos);
   }

   /** @deprecated */
   @Deprecated
   public VoxelShape getOcclusionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
      return pState.getShape(pLevel, pPos);
   }

   /** @deprecated */
   @Deprecated
   public VoxelShape getBlockSupportShape(BlockState pState, BlockGetter pReader, BlockPos pPos) {
      return this.getCollisionShape(pState, pReader, pPos, CollisionContext.empty());
   }

   /** @deprecated */
   @Deprecated
   public VoxelShape getInteractionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
      return Shapes.empty();
   }

   /** @deprecated */
   @Deprecated
   public int getLightBlock(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
      if (pState.isSolidRender(pLevel, pPos)) {
         return pLevel.getMaxLightLevel();
      } else {
         return pState.propagatesSkylightDown(pLevel, pPos) ? 0 : 1;
      }
   }

   /** @deprecated */
   @Nullable
   @Deprecated
   public MenuProvider getMenuProvider(BlockState pState, Level pLevel, BlockPos pPos) {
      return null;
   }

   /** @deprecated */
   @Deprecated
   public boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
      return true;
   }

   /** @deprecated */
   @Deprecated
   public float getShadeBrightness(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
      return pState.isCollisionShapeFullBlock(pLevel, pPos) ? 0.2F : 1.0F;
   }

   /** @deprecated */
   /**
    * @deprecated call via {@link
    * net.minecraft.world.level.block.state.BlockBehavior.BlockStateBase#getAnalogOutputSignal} whenever possible.
    * Implementing/overriding is fine.
    */
   @Deprecated
   public int getAnalogOutputSignal(BlockState pState, Level pLevel, BlockPos pPos) {
      return 0;
   }

   /** @deprecated */
   @Deprecated
   public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return Shapes.block();
   }

   /** @deprecated */
   @Deprecated
   public VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return this.hasCollision ? pState.getShape(pLevel, pPos) : Shapes.empty();
   }

   /** @deprecated */
   @Deprecated
   public boolean isCollisionShapeFullBlock(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
      return Block.isShapeFullBlock(pState.getCollisionShape(pLevel, pPos));
   }

   /** @deprecated */
   @Deprecated
   public boolean isOcclusionShapeFullBlock(BlockState p_222959_, BlockGetter p_222960_, BlockPos p_222961_) {
      return Block.isShapeFullBlock(p_222959_.getOcclusionShape(p_222960_, p_222961_));
   }

   /** @deprecated */
   @Deprecated
   public VoxelShape getVisualShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return this.getCollisionShape(pState, pLevel, pPos, pContext);
   }

   /** @deprecated */
   /**
    * Performs a random tick on a block.
    */
   @Deprecated
   public void randomTick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
      this.tick(pState, pLevel, pPos, pRandom);
   }

   /** @deprecated */
   @Deprecated
   public void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
   }

   /** @deprecated */
   /**
    * Get the hardness of this Block relative to the ability of the given player
    * @deprecated call via {@link net.minecraft.world.level.block.state.BlockBehavior.BlockStateBase#getDestroyProgress}
    * whenever possible. Implementing/overriding is fine.
    */
   @Deprecated
   public float getDestroyProgress(BlockState pState, Player pPlayer, BlockGetter pLevel, BlockPos pPos) {
      float f = pState.getDestroySpeed(pLevel, pPos);
      if (f == -1.0F) {
         return 0.0F;
      } else {
         int i = net.minecraftforge.common.ForgeHooks.isCorrectToolForDrops(pState, pPlayer) ? 30 : 100;
         return pPlayer.getDigSpeed(pState, pPos) / f / (float)i;
      }
   }

   /** @deprecated */
   /**
    * Perform side-effects from block dropping, such as creating silverfish
    */
   @Deprecated
   public void spawnAfterBreak(BlockState pState, ServerLevel pLevel, BlockPos pPos, ItemStack pStack, boolean p_222953_) {
   }

   /** @deprecated */
   @Deprecated
   public void attack(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer) {
   }

   /** @deprecated */
   /**
    * @deprecated call via {@link net.minecraft.world.level.block.state.BlockBehavior.BlockStateBase#getSignal} whenever
    * possible. Implementing/overriding is fine.
    */
   @Deprecated
   public int getSignal(BlockState pState, BlockGetter pLevel, BlockPos pPos, Direction pDirection) {
      return 0;
   }

   /** @deprecated */
   @Deprecated
   public void entityInside(BlockState pState, Level pLevel, BlockPos pPos, Entity pEntity) {
   }

   /** @deprecated */
   /**
    * @deprecated call via {@link net.minecraft.world.level.block.state.BlockBehavior.BlockStateBase#getDirectSignal}
    * whenever possible. Implementing/overriding is fine.
    */
   @Deprecated
   public int getDirectSignal(BlockState pState, BlockGetter pLevel, BlockPos pPos, Direction pDirection) {
      return 0;
   }

   public final ResourceLocation getLootTable() {
      if (this.drops == null) {
         this.drops = this.lootTableSupplier.get();
      }

      return this.drops;
   }

   /** @deprecated */
   @Deprecated
   public void onProjectileHit(Level pLevel, BlockState pState, BlockHitResult pHit, Projectile pProjectile) {
   }

   public abstract Item asItem();

   protected abstract Block asBlock();

   public MaterialColor defaultMaterialColor() {
      return this.properties.materialColor.apply(this.asBlock().defaultBlockState());
   }

   public float defaultDestroyTime() {
      return this.properties.destroyTime;
   }

   protected boolean isAir(BlockState state) {
      return ((BlockStateBase)state).isAir;
   }

   /* ======================================== FORGE START ===================================== */
   private final java.util.function.Supplier<ResourceLocation> lootTableSupplier;
   /* ========================================= FORGE END ====================================== */

   public abstract static class BlockStateBase extends StateHolder<Block, BlockState> {
      private final int lightEmission;
      private final boolean useShapeForLightOcclusion;
      private final boolean isAir;
      private final Material material;
      private final MaterialColor materialColor;
      private final float destroySpeed;
      private final boolean requiresCorrectToolForDrops;
      private final boolean canOcclude;
      private final BlockBehaviour.StatePredicate isRedstoneConductor;
      private final BlockBehaviour.StatePredicate isSuffocating;
      private final BlockBehaviour.StatePredicate isViewBlocking;
      private final BlockBehaviour.StatePredicate hasPostProcess;
      private final BlockBehaviour.StatePredicate emissiveRendering;
      private final BlockBehaviour.OffsetType offsetType;
      @Nullable
      protected BlockBehaviour.BlockStateBase.Cache cache;

      protected BlockStateBase(Block pOwner, ImmutableMap<Property<?>, Comparable<?>> pValues, MapCodec<BlockState> pPropertiesCodec) {
         super(pOwner, pValues, pPropertiesCodec);
         BlockBehaviour.Properties blockbehaviour$properties = pOwner.properties;
         this.lightEmission = blockbehaviour$properties.lightEmission.applyAsInt(this.asState());
         this.useShapeForLightOcclusion = pOwner.useShapeForLightOcclusion(this.asState());
         this.isAir = blockbehaviour$properties.isAir;
         this.material = blockbehaviour$properties.material;
         this.materialColor = blockbehaviour$properties.materialColor.apply(this.asState());
         this.destroySpeed = blockbehaviour$properties.destroyTime;
         this.requiresCorrectToolForDrops = blockbehaviour$properties.requiresCorrectToolForDrops;
         this.canOcclude = blockbehaviour$properties.canOcclude;
         this.isRedstoneConductor = blockbehaviour$properties.isRedstoneConductor;
         this.isSuffocating = blockbehaviour$properties.isSuffocating;
         this.isViewBlocking = blockbehaviour$properties.isViewBlocking;
         this.hasPostProcess = blockbehaviour$properties.hasPostProcess;
         this.emissiveRendering = blockbehaviour$properties.emissiveRendering;
         this.offsetType = blockbehaviour$properties.offsetType.apply(this.asState());
      }

      public void initCache() {
         if (!this.getBlock().hasDynamicShape()) {
            this.cache = new BlockBehaviour.BlockStateBase.Cache(this.asState());
         }

      }

      public Block getBlock() {
         return this.owner;
      }

      public Holder<Block> getBlockHolder() {
         return this.owner.builtInRegistryHolder();
      }

      public Material getMaterial() {
         return this.material;
      }

      public boolean isValidSpawn(BlockGetter pLevel, BlockPos pPos, EntityType<?> pEntityType) {
         return this.getBlock().properties.isValidSpawn.test(this.asState(), pLevel, pPos, pEntityType);
      }

      public boolean propagatesSkylightDown(BlockGetter pLevel, BlockPos pPos) {
         return this.cache != null ? this.cache.propagatesSkylightDown : this.getBlock().propagatesSkylightDown(this.asState(), pLevel, pPos);
      }

      public int getLightBlock(BlockGetter pLevel, BlockPos pPos) {
         return this.cache != null ? this.cache.lightBlock : this.getBlock().getLightBlock(this.asState(), pLevel, pPos);
      }

      public VoxelShape getFaceOcclusionShape(BlockGetter pLevel, BlockPos pPos, Direction pDirection) {
         return this.cache != null && this.cache.occlusionShapes != null ? this.cache.occlusionShapes[pDirection.ordinal()] : Shapes.getFaceShape(this.getOcclusionShape(pLevel, pPos), pDirection);
      }

      public VoxelShape getOcclusionShape(BlockGetter pLevel, BlockPos pPos) {
         return this.getBlock().getOcclusionShape(this.asState(), pLevel, pPos);
      }

      public boolean hasLargeCollisionShape() {
         return this.cache == null || this.cache.largeCollisionShape;
      }

      public boolean useShapeForLightOcclusion() {
         return this.useShapeForLightOcclusion;
      }

      /** @deprecated use {@link BlockState#getLightEmission(BlockGetter, BlockPos)} */
      @Deprecated
      public int getLightEmission() {
         return this.lightEmission;
      }

      public boolean isAir() {
         return this.getBlock().isAir((BlockState)this);
      }

      public MaterialColor getMapColor(BlockGetter pLevel, BlockPos pPos) {
         return getBlock().getMapColor(this.asState(), pLevel, pPos, this.materialColor);
      }

      /** @deprecated use {@link BlockState#rotate(LevelAccessor, BlockPos, Rotation)} */
      /**
       * @return the blockstate with the given rotation. If inapplicable, returns itself.
       */
      @Deprecated
      public BlockState rotate(Rotation pRotation) {
         return this.getBlock().rotate(this.asState(), pRotation);
      }

      /**
       * @return the blockstate mirrored in the given way. If inapplicable, returns itself.
       */
      public BlockState mirror(Mirror pMirror) {
         return this.getBlock().mirror(this.asState(), pMirror);
      }

      public RenderShape getRenderShape() {
         return this.getBlock().getRenderShape(this.asState());
      }

      public boolean emissiveRendering(BlockGetter pLevel, BlockPos pPos) {
         return this.emissiveRendering.test(this.asState(), pLevel, pPos);
      }

      public float getShadeBrightness(BlockGetter pLevel, BlockPos pPos) {
         return this.getBlock().getShadeBrightness(this.asState(), pLevel, pPos);
      }

      public boolean isRedstoneConductor(BlockGetter pLevel, BlockPos pPos) {
         return this.isRedstoneConductor.test(this.asState(), pLevel, pPos);
      }

      public boolean isSignalSource() {
         return this.getBlock().isSignalSource(this.asState());
      }

      public int getSignal(BlockGetter pLevel, BlockPos pPos, Direction pDirection) {
         return this.getBlock().getSignal(this.asState(), pLevel, pPos, pDirection);
      }

      public boolean hasAnalogOutputSignal() {
         return this.getBlock().hasAnalogOutputSignal(this.asState());
      }

      public int getAnalogOutputSignal(Level pLevel, BlockPos pPos) {
         return this.getBlock().getAnalogOutputSignal(this.asState(), pLevel, pPos);
      }

      public float getDestroySpeed(BlockGetter pLevel, BlockPos pPos) {
         return this.destroySpeed;
      }

      public float getDestroyProgress(Player pPlayer, BlockGetter pLevel, BlockPos pPos) {
         return this.getBlock().getDestroyProgress(this.asState(), pPlayer, pLevel, pPos);
      }

      public int getDirectSignal(BlockGetter pLevel, BlockPos pPos, Direction pDirection) {
         return this.getBlock().getDirectSignal(this.asState(), pLevel, pPos, pDirection);
      }

      public PushReaction getPistonPushReaction() {
         return this.getBlock().getPistonPushReaction(this.asState());
      }

      public boolean isSolidRender(BlockGetter pLevel, BlockPos pPos) {
         if (this.cache != null) {
            return this.cache.solidRender;
         } else {
            BlockState blockstate = this.asState();
            return blockstate.canOcclude() ? Block.isShapeFullBlock(blockstate.getOcclusionShape(pLevel, pPos)) : false;
         }
      }

      public boolean canOcclude() {
         return this.canOcclude;
      }

      public boolean skipRendering(BlockState pState, Direction pFace) {
         return this.getBlock().skipRendering(this.asState(), pState, pFace);
      }

      public VoxelShape getShape(BlockGetter pLevel, BlockPos pPos) {
         return this.getShape(pLevel, pPos, CollisionContext.empty());
      }

      public VoxelShape getShape(BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
         return this.getBlock().getShape(this.asState(), pLevel, pPos, pContext);
      }

      public VoxelShape getCollisionShape(BlockGetter pLevel, BlockPos pPos) {
         return this.cache != null ? this.cache.collisionShape : this.getCollisionShape(pLevel, pPos, CollisionContext.empty());
      }

      public VoxelShape getCollisionShape(BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
         return this.getBlock().getCollisionShape(this.asState(), pLevel, pPos, pContext);
      }

      public VoxelShape getBlockSupportShape(BlockGetter pLevel, BlockPos pPos) {
         return this.getBlock().getBlockSupportShape(this.asState(), pLevel, pPos);
      }

      public VoxelShape getVisualShape(BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
         return this.getBlock().getVisualShape(this.asState(), pLevel, pPos, pContext);
      }

      public VoxelShape getInteractionShape(BlockGetter pLevel, BlockPos pPos) {
         return this.getBlock().getInteractionShape(this.asState(), pLevel, pPos);
      }

      public final boolean entityCanStandOn(BlockGetter pLevel, BlockPos pPos, Entity pEntity) {
         return this.entityCanStandOnFace(pLevel, pPos, pEntity, Direction.UP);
      }

      /**
       * @return true if the collision box of this state covers the entire upper face of the blockspace
       */
      public final boolean entityCanStandOnFace(BlockGetter pLevel, BlockPos pPos, Entity pEntity, Direction pFace) {
         return Block.isFaceFull(this.getCollisionShape(pLevel, pPos, CollisionContext.of(pEntity)), pFace);
      }

      public Vec3 getOffset(BlockGetter pLevel, BlockPos pPos) {
         if (this.offsetType == BlockBehaviour.OffsetType.NONE) {
            return Vec3.ZERO;
         } else {
            Block block = this.getBlock();
            long i = Mth.getSeed(pPos.getX(), 0, pPos.getZ());
            float f = block.getMaxHorizontalOffset();
            double d0 = Mth.clamp(((double)((float)(i & 15L) / 15.0F) - 0.5D) * 0.5D, (double)(-f), (double)f);
            double d1 = this.offsetType == BlockBehaviour.OffsetType.XYZ ? ((double)((float)(i >> 4 & 15L) / 15.0F) - 1.0D) * (double)block.getMaxVerticalOffset() : 0.0D;
            double d2 = Mth.clamp(((double)((float)(i >> 8 & 15L) / 15.0F) - 0.5D) * 0.5D, (double)(-f), (double)f);
            return new Vec3(d0, d1, d2);
         }
      }

      public boolean triggerEvent(Level pLevel, BlockPos pPos, int pId, int pParam) {
         return this.getBlock().triggerEvent(this.asState(), pLevel, pPos, pId, pParam);
      }

      /** @deprecated */
      @Deprecated
      public void neighborChanged(Level pLevel, BlockPos pPos, Block pBlock, BlockPos pFromPos, boolean pIsMoving) {
         this.getBlock().neighborChanged(this.asState(), pLevel, pPos, pBlock, pFromPos, pIsMoving);
      }

      public final void updateNeighbourShapes(LevelAccessor pLevel, BlockPos pPos, int pFlag) {
         this.updateNeighbourShapes(pLevel, pPos, pFlag, 512);
      }

      public final void updateNeighbourShapes(LevelAccessor pLevel, BlockPos pPos, int pFlag, int pRecursionLeft) {
         this.getBlock();
         BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

         for(Direction direction : BlockBehaviour.UPDATE_SHAPE_ORDER) {
            blockpos$mutableblockpos.setWithOffset(pPos, direction);
            pLevel.neighborShapeChanged(direction.getOpposite(), this.asState(), blockpos$mutableblockpos, pPos, pFlag, pRecursionLeft);
         }

      }

      /**
       * Performs validations on the block state and possibly neighboring blocks to validate whether the incoming state
       * is valid to stay in the world. Currently used only by redstone wire to update itself if neighboring blocks have
       * changed and to possibly break itself.
       */
      public final void updateIndirectNeighbourShapes(LevelAccessor pLevel, BlockPos pPos, int pFlags) {
         this.updateIndirectNeighbourShapes(pLevel, pPos, pFlags, 512);
      }

      public void updateIndirectNeighbourShapes(LevelAccessor pLevel, BlockPos pPos, int pFlags, int pRecursionLeft) {
         this.getBlock().updateIndirectNeighbourShapes(this.asState(), pLevel, pPos, pFlags, pRecursionLeft);
      }

      public void onPlace(Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
         this.getBlock().onPlace(this.asState(), pLevel, pPos, pOldState, pIsMoving);
      }

      public void onRemove(Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
         this.getBlock().onRemove(this.asState(), pLevel, pPos, pNewState, pIsMoving);
      }

      public void tick(ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
         this.getBlock().tick(this.asState(), pLevel, pPos, pRandom);
      }

      public void randomTick(ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
         this.getBlock().randomTick(this.asState(), pLevel, pPos, pRandom);
      }

      public void entityInside(Level pLevel, BlockPos pPos, Entity pEntity) {
         this.getBlock().entityInside(this.asState(), pLevel, pPos, pEntity);
      }

      public void spawnAfterBreak(ServerLevel pLevel, BlockPos pPos, ItemStack pStack, boolean p_222971_) {
         this.getBlock().spawnAfterBreak(this.asState(), pLevel, pPos, pStack, p_222971_);
      }

      public List<ItemStack> getDrops(LootContext.Builder pBuilder) {
         return this.getBlock().getDrops(this.asState(), pBuilder);
      }

      public InteractionResult use(Level pLevel, Player pPlayer, InteractionHand pHand, BlockHitResult pResult) {
         return this.getBlock().use(this.asState(), pLevel, pResult.getBlockPos(), pPlayer, pHand, pResult);
      }

      public void attack(Level pLevel, BlockPos pPos, Player pPlayer) {
         this.getBlock().attack(this.asState(), pLevel, pPos, pPlayer);
      }

      public boolean isSuffocating(BlockGetter pLevel, BlockPos pPos) {
         return this.isSuffocating.test(this.asState(), pLevel, pPos);
      }

      public boolean isViewBlocking(BlockGetter pLevel, BlockPos pPos) {
         return this.isViewBlocking.test(this.asState(), pLevel, pPos);
      }

      public BlockState updateShape(Direction pDirection, BlockState pQueried, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pOffsetPos) {
         return this.getBlock().updateShape(this.asState(), pDirection, pQueried, pLevel, pCurrentPos, pOffsetPos);
      }

      public boolean isPathfindable(BlockGetter pLevel, BlockPos pPos, PathComputationType pType) {
         return this.getBlock().isPathfindable(this.asState(), pLevel, pPos, pType);
      }

      public boolean canBeReplaced(BlockPlaceContext pUseContext) {
         return this.getBlock().canBeReplaced(this.asState(), pUseContext);
      }

      public boolean canBeReplaced(Fluid pFluid) {
         return this.getBlock().canBeReplaced(this.asState(), pFluid);
      }

      public boolean canSurvive(LevelReader pLevel, BlockPos pPos) {
         return this.getBlock().canSurvive(this.asState(), pLevel, pPos);
      }

      public boolean hasPostProcess(BlockGetter pLevel, BlockPos pPos) {
         return this.hasPostProcess.test(this.asState(), pLevel, pPos);
      }

      @Nullable
      public MenuProvider getMenuProvider(Level pLevel, BlockPos pPos) {
         return this.getBlock().getMenuProvider(this.asState(), pLevel, pPos);
      }

      public boolean is(TagKey<Block> pTag) {
         return this.getBlock().builtInRegistryHolder().is(pTag);
      }

      public boolean is(TagKey<Block> pTag, Predicate<BlockBehaviour.BlockStateBase> pPredicate) {
         return this.is(pTag) && pPredicate.test(this);
      }

      public boolean is(HolderSet<Block> pHolder) {
         return pHolder.contains(this.getBlock().builtInRegistryHolder());
      }

      public Stream<TagKey<Block>> getTags() {
         return this.getBlock().builtInRegistryHolder().tags();
      }

      public boolean hasBlockEntity() {
         return this.getBlock() instanceof EntityBlock;
      }

      @Nullable
      public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockEntityType<T> pBlockEntityType) {
         return this.getBlock() instanceof EntityBlock ? ((EntityBlock)this.getBlock()).getTicker(pLevel, this.asState(), pBlockEntityType) : null;
      }

      public boolean is(Block pBlock) {
         return this.getBlock() == pBlock;
      }

      public FluidState getFluidState() {
         return this.getBlock().getFluidState(this.asState());
      }

      public boolean isRandomlyTicking() {
         return this.getBlock().isRandomlyTicking(this.asState());
      }

      public long getSeed(BlockPos pPos) {
         return this.getBlock().getSeed(this.asState(), pPos);
      }

      public SoundType getSoundType() {
         return this.getBlock().getSoundType(this.asState());
      }

      public void onProjectileHit(Level pLevel, BlockState pState, BlockHitResult pHit, Projectile pProjectile) {
         this.getBlock().onProjectileHit(pLevel, pState, pHit, pProjectile);
      }

      public boolean isFaceSturdy(BlockGetter pLevel, BlockPos pPos, Direction pDirection) {
         return this.isFaceSturdy(pLevel, pPos, pDirection, SupportType.FULL);
      }

      public boolean isFaceSturdy(BlockGetter pLevel, BlockPos pPos, Direction pFace, SupportType pSupportType) {
         return this.cache != null ? this.cache.isFaceSturdy(pFace, pSupportType) : pSupportType.isSupporting(this.asState(), pLevel, pPos, pFace);
      }

      public boolean isCollisionShapeFullBlock(BlockGetter pLevel, BlockPos pPos) {
         return this.cache != null ? this.cache.isCollisionShapeFullBlock : this.getBlock().isCollisionShapeFullBlock(this.asState(), pLevel, pPos);
      }

      protected abstract BlockState asState();

      public boolean requiresCorrectToolForDrops() {
         return this.requiresCorrectToolForDrops;
      }

      /**
       * Get the OffsetType for this Block. Determines if the model is rendered slightly offset.
       */
      public BlockBehaviour.OffsetType getOffsetType() {
         return this.offsetType;
      }

      static final class Cache {
         private static final Direction[] DIRECTIONS = Direction.values();
         private static final int SUPPORT_TYPE_COUNT = SupportType.values().length;
         protected final boolean solidRender;
         final boolean propagatesSkylightDown;
         final int lightBlock;
         @Nullable
         final VoxelShape[] occlusionShapes;
         protected final VoxelShape collisionShape;
         protected final boolean largeCollisionShape;
         private final boolean[] faceSturdy;
         protected final boolean isCollisionShapeFullBlock;

         Cache(BlockState pState) {
            Block block = pState.getBlock();
            this.solidRender = pState.isSolidRender(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
            this.propagatesSkylightDown = block.propagatesSkylightDown(pState, EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
            this.lightBlock = block.getLightBlock(pState, EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
            if (!pState.canOcclude()) {
               this.occlusionShapes = null;
            } else {
               this.occlusionShapes = new VoxelShape[DIRECTIONS.length];
               VoxelShape voxelshape = block.getOcclusionShape(pState, EmptyBlockGetter.INSTANCE, BlockPos.ZERO);

               for(Direction direction : DIRECTIONS) {
                  this.occlusionShapes[direction.ordinal()] = Shapes.getFaceShape(voxelshape, direction);
               }
            }

            this.collisionShape = block.getCollisionShape(pState, EmptyBlockGetter.INSTANCE, BlockPos.ZERO, CollisionContext.empty());
            if (!this.collisionShape.isEmpty() && pState.getOffsetType() != BlockBehaviour.OffsetType.NONE) {
               throw new IllegalStateException(String.format(Locale.ROOT, "%s has a collision shape and an offset type, but is not marked as dynamicShape in its properties.", Registry.BLOCK.getKey(block)));
            } else {
               this.largeCollisionShape = Arrays.stream(Direction.Axis.values()).anyMatch((p_60860_) -> {
                  return this.collisionShape.min(p_60860_) < 0.0D || this.collisionShape.max(p_60860_) > 1.0D;
               });
               this.faceSturdy = new boolean[DIRECTIONS.length * SUPPORT_TYPE_COUNT];

               for(Direction direction1 : DIRECTIONS) {
                  for(SupportType supporttype : SupportType.values()) {
                     this.faceSturdy[getFaceSupportIndex(direction1, supporttype)] = supporttype.isSupporting(pState, EmptyBlockGetter.INSTANCE, BlockPos.ZERO, direction1);
                  }
               }

               this.isCollisionShapeFullBlock = Block.isShapeFullBlock(pState.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
            }
         }

         public boolean isFaceSturdy(Direction pDirection, SupportType pSupportType) {
            return this.faceSturdy[getFaceSupportIndex(pDirection, pSupportType)];
         }

         private static int getFaceSupportIndex(Direction pDirection, SupportType pSupportType) {
            return pDirection.ordinal() * SUPPORT_TYPE_COUNT + pSupportType.ordinal();
         }
      }
   }

   public static enum OffsetType {
      NONE,
      XZ,
      XYZ;
   }

   public static class Properties {
      Material material;
      Function<BlockState, MaterialColor> materialColor;
      boolean hasCollision = true;
      SoundType soundType = SoundType.STONE;
      ToIntFunction<BlockState> lightEmission = (p_60929_) -> {
         return 0;
      };
      float explosionResistance;
      float destroyTime;
      boolean requiresCorrectToolForDrops;
      boolean isRandomlyTicking;
      float friction = 0.6F;
      float speedFactor = 1.0F;
      float jumpFactor = 1.0F;
      /** Sets loot table information */
      ResourceLocation drops;
      boolean canOcclude = true;
      boolean isAir;
      private java.util.function.Supplier<ResourceLocation> lootTableSupplier;
      BlockBehaviour.StateArgumentPredicate<EntityType<?>> isValidSpawn = (p_60935_, p_60936_, p_60937_, p_60938_) -> {
         return p_60935_.isFaceSturdy(p_60936_, p_60937_, Direction.UP) && p_60935_.getLightEmission(p_60936_, p_60937_) < 14;
      };
      BlockBehaviour.StatePredicate isRedstoneConductor = (p_60985_, p_60986_, p_60987_) -> {
         return p_60985_.getMaterial().isSolidBlocking() && p_60985_.isCollisionShapeFullBlock(p_60986_, p_60987_);
      };
      BlockBehaviour.StatePredicate isSuffocating = (p_60974_, p_60975_, p_60976_) -> {
         return this.material.blocksMotion() && p_60974_.isCollisionShapeFullBlock(p_60975_, p_60976_);
      };
      /** If it blocks vision on the client side. */
      BlockBehaviour.StatePredicate isViewBlocking = this.isSuffocating;
      BlockBehaviour.StatePredicate hasPostProcess = (p_60963_, p_60964_, p_60965_) -> {
         return false;
      };
      BlockBehaviour.StatePredicate emissiveRendering = (p_60931_, p_60932_, p_60933_) -> {
         return false;
      };
      boolean dynamicShape;
      Function<BlockState, BlockBehaviour.OffsetType> offsetType = (p_222985_) -> {
         return BlockBehaviour.OffsetType.NONE;
      };

      private Properties(Material pMaterial, MaterialColor pMaterialColor) {
         this(pMaterial, (p_222993_) -> {
            return pMaterialColor;
         });
      }

      private Properties(Material pMaterial, Function<BlockState, MaterialColor> pMaterialColor) {
         this.material = pMaterial;
         this.materialColor = pMaterialColor;
      }

      public static BlockBehaviour.Properties of(Material pMaterial) {
         return of(pMaterial, pMaterial.getColor());
      }

      public static BlockBehaviour.Properties of(Material pMaterial, DyeColor pColor) {
         return of(pMaterial, pColor.getMaterialColor());
      }

      public static BlockBehaviour.Properties of(Material pMaterial, MaterialColor pMaterialColor) {
         return new BlockBehaviour.Properties(pMaterial, pMaterialColor);
      }

      public static BlockBehaviour.Properties of(Material pMaterial, Function<BlockState, MaterialColor> pMaterialColor) {
         return new BlockBehaviour.Properties(pMaterial, pMaterialColor);
      }

      public static BlockBehaviour.Properties copy(BlockBehaviour pBlockBehaviour) {
         BlockBehaviour.Properties blockbehaviour$properties = new BlockBehaviour.Properties(pBlockBehaviour.material, pBlockBehaviour.properties.materialColor);
         blockbehaviour$properties.material = pBlockBehaviour.properties.material;
         blockbehaviour$properties.destroyTime = pBlockBehaviour.properties.destroyTime;
         blockbehaviour$properties.explosionResistance = pBlockBehaviour.properties.explosionResistance;
         blockbehaviour$properties.hasCollision = pBlockBehaviour.properties.hasCollision;
         blockbehaviour$properties.isRandomlyTicking = pBlockBehaviour.properties.isRandomlyTicking;
         blockbehaviour$properties.lightEmission = pBlockBehaviour.properties.lightEmission;
         blockbehaviour$properties.materialColor = pBlockBehaviour.properties.materialColor;
         blockbehaviour$properties.soundType = pBlockBehaviour.properties.soundType;
         blockbehaviour$properties.friction = pBlockBehaviour.properties.friction;
         blockbehaviour$properties.speedFactor = pBlockBehaviour.properties.speedFactor;
         blockbehaviour$properties.dynamicShape = pBlockBehaviour.properties.dynamicShape;
         blockbehaviour$properties.canOcclude = pBlockBehaviour.properties.canOcclude;
         blockbehaviour$properties.isAir = pBlockBehaviour.properties.isAir;
         blockbehaviour$properties.requiresCorrectToolForDrops = pBlockBehaviour.properties.requiresCorrectToolForDrops;
         blockbehaviour$properties.offsetType = pBlockBehaviour.properties.offsetType;
         return blockbehaviour$properties;
      }

      public BlockBehaviour.Properties noCollission() {
         this.hasCollision = false;
         this.canOcclude = false;
         return this;
      }

      public BlockBehaviour.Properties noOcclusion() {
         this.canOcclude = false;
         return this;
      }

      public BlockBehaviour.Properties friction(float pFriction) {
         this.friction = pFriction;
         return this;
      }

      public BlockBehaviour.Properties speedFactor(float pSpeedFactor) {
         this.speedFactor = pSpeedFactor;
         return this;
      }

      public BlockBehaviour.Properties jumpFactor(float pJumpFactor) {
         this.jumpFactor = pJumpFactor;
         return this;
      }

      public BlockBehaviour.Properties sound(SoundType pSoundType) {
         this.soundType = pSoundType;
         return this;
      }

      public BlockBehaviour.Properties lightLevel(ToIntFunction<BlockState> pLightEmission) {
         this.lightEmission = pLightEmission;
         return this;
      }

      public BlockBehaviour.Properties strength(float pDestroyTime, float pExplosionResistance) {
         return this.destroyTime(pDestroyTime).explosionResistance(pExplosionResistance);
      }

      public BlockBehaviour.Properties instabreak() {
         return this.strength(0.0F);
      }

      public BlockBehaviour.Properties strength(float pStrength) {
         this.strength(pStrength, pStrength);
         return this;
      }

      public BlockBehaviour.Properties randomTicks() {
         this.isRandomlyTicking = true;
         return this;
      }

      public BlockBehaviour.Properties dynamicShape() {
         this.dynamicShape = true;
         return this;
      }

      public BlockBehaviour.Properties noLootTable() {
         this.drops = BuiltInLootTables.EMPTY;
         return this;
      }

      @Deprecated // FORGE: Use the variant that takes a Supplier below
      public BlockBehaviour.Properties dropsLike(Block pBlock) {
         this.lootTableSupplier = () -> net.minecraftforge.registries.ForgeRegistries.BLOCKS.getDelegateOrThrow(pBlock).get().getLootTable();
         return this;
      }

      public BlockBehaviour.Properties lootFrom(java.util.function.Supplier<? extends Block> blockIn) {
          this.lootTableSupplier = () -> blockIn.get().getLootTable();
          return this;
      }

      public BlockBehaviour.Properties air() {
         this.isAir = true;
         return this;
      }

      public BlockBehaviour.Properties isValidSpawn(BlockBehaviour.StateArgumentPredicate<EntityType<?>> pIsValidSpawn) {
         this.isValidSpawn = pIsValidSpawn;
         return this;
      }

      public BlockBehaviour.Properties isRedstoneConductor(BlockBehaviour.StatePredicate pIsRedstoneConductor) {
         this.isRedstoneConductor = pIsRedstoneConductor;
         return this;
      }

      public BlockBehaviour.Properties isSuffocating(BlockBehaviour.StatePredicate pIsSuffocating) {
         this.isSuffocating = pIsSuffocating;
         return this;
      }

      /**
       * If it blocks vision on the client side.
       */
      public BlockBehaviour.Properties isViewBlocking(BlockBehaviour.StatePredicate pIsViewBlocking) {
         this.isViewBlocking = pIsViewBlocking;
         return this;
      }

      public BlockBehaviour.Properties hasPostProcess(BlockBehaviour.StatePredicate pHasPostProcess) {
         this.hasPostProcess = pHasPostProcess;
         return this;
      }

      public BlockBehaviour.Properties emissiveRendering(BlockBehaviour.StatePredicate pEmissiveRendering) {
         this.emissiveRendering = pEmissiveRendering;
         return this;
      }

      public BlockBehaviour.Properties requiresCorrectToolForDrops() {
         this.requiresCorrectToolForDrops = true;
         return this;
      }

      public BlockBehaviour.Properties color(MaterialColor pMaterialColor) {
         this.materialColor = (p_222988_) -> {
            return pMaterialColor;
         };
         return this;
      }

      public BlockBehaviour.Properties destroyTime(float pDestroyTime) {
         this.destroyTime = pDestroyTime;
         return this;
      }

      public BlockBehaviour.Properties explosionResistance(float pExplosionResistance) {
         this.explosionResistance = Math.max(0.0F, pExplosionResistance);
         return this;
      }

      public BlockBehaviour.Properties offsetType(BlockBehaviour.OffsetType pOffsetType) {
         return this.offsetType((p_222983_) -> {
            return pOffsetType;
         });
      }

      public BlockBehaviour.Properties offsetType(Function<BlockState, BlockBehaviour.OffsetType> pOffsetType) {
         this.offsetType = pOffsetType;
         return this;
      }
   }

   public interface StateArgumentPredicate<A> {
      boolean test(BlockState pState, BlockGetter pLevel, BlockPos pPos, A pValue);
   }

   public interface StatePredicate {
      boolean test(BlockState pState, BlockGetter pLevel, BlockPos pPos);
   }
}
