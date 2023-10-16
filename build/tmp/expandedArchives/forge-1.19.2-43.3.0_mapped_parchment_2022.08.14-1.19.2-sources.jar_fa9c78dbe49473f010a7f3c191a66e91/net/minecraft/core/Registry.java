package net.minecraft.core;

import com.google.common.collect.Maps;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Keyable;
import com.mojang.serialization.Lifecycle;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.network.chat.ChatType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.StatType;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.FloatProviderType;
import net.minecraft.util.valueproviders.IntProviderType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.animal.CatVariant;
import net.minecraft.world.entity.animal.FrogVariant;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraft.world.entity.decoration.PaintingVariants;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.schedule.Schedule;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Instrument;
import net.minecraft.world.item.Instruments;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.BiomeSources;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatterns;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGenerators;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.PositionSourceType;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicateType;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.featuresize.FeatureSizeType;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacerType;
import net.minecraft.world.level.levelgen.feature.rootplacers.RootPlacerType;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProviderType;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerType;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorPreset;
import net.minecraft.world.level.levelgen.heightproviders.HeightProviderType;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.PosRuleTestType;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTestType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntries;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryType;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditions;
import net.minecraft.world.level.storage.loot.providers.nbt.LootNbtProviderType;
import net.minecraft.world.level.storage.loot.providers.nbt.NbtProviders;
import net.minecraft.world.level.storage.loot.providers.number.LootNumberProviderType;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;
import net.minecraft.world.level.storage.loot.providers.score.LootScoreProviderType;
import net.minecraft.world.level.storage.loot.providers.score.ScoreboardNameProviders;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;

/*
 * Attention Modders: This SHOULD NOT be used, you should use ForgeRegistries instead. As it has a cleaner modder facing API.
 * We will be wrapping all of these in our API as necessary for syncing and management.
 */
public abstract class Registry<T> implements Keyable, IdMap<T> {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Map<ResourceLocation, Supplier<?>> LOADERS = Maps.newLinkedHashMap();
   public static final ResourceLocation ROOT_REGISTRY_NAME = new ResourceLocation("root");
   protected static final WritableRegistry<WritableRegistry<?>> WRITABLE_REGISTRY = new MappedRegistry<>(createRegistryKey("root"), Lifecycle.experimental(), (Function<WritableRegistry<?>, Holder.Reference<WritableRegistry<?>>>)null);
   public static final Registry<? extends Registry<?>> REGISTRY = WRITABLE_REGISTRY;
   public static final ResourceKey<Registry<SoundEvent>> SOUND_EVENT_REGISTRY = createRegistryKey("sound_event");
   public static final ResourceKey<Registry<Fluid>> FLUID_REGISTRY = createRegistryKey("fluid");
   public static final ResourceKey<Registry<MobEffect>> MOB_EFFECT_REGISTRY = createRegistryKey("mob_effect");
   public static final ResourceKey<Registry<Block>> BLOCK_REGISTRY = createRegistryKey("block");
   public static final ResourceKey<Registry<Enchantment>> ENCHANTMENT_REGISTRY = createRegistryKey("enchantment");
   public static final ResourceKey<Registry<EntityType<?>>> ENTITY_TYPE_REGISTRY = createRegistryKey("entity_type");
   public static final ResourceKey<Registry<Item>> ITEM_REGISTRY = createRegistryKey("item");
   public static final ResourceKey<Registry<Potion>> POTION_REGISTRY = createRegistryKey("potion");
   public static final ResourceKey<Registry<ParticleType<?>>> PARTICLE_TYPE_REGISTRY = createRegistryKey("particle_type");
   public static final ResourceKey<Registry<BlockEntityType<?>>> BLOCK_ENTITY_TYPE_REGISTRY = createRegistryKey("block_entity_type");
   public static final ResourceKey<Registry<PaintingVariant>> PAINTING_VARIANT_REGISTRY = createRegistryKey("painting_variant");
   public static final ResourceKey<Registry<ResourceLocation>> CUSTOM_STAT_REGISTRY = createRegistryKey("custom_stat");
   public static final ResourceKey<Registry<ChunkStatus>> CHUNK_STATUS_REGISTRY = createRegistryKey("chunk_status");
   public static final ResourceKey<Registry<RuleTestType<?>>> RULE_TEST_REGISTRY = createRegistryKey("rule_test");
   public static final ResourceKey<Registry<PosRuleTestType<?>>> POS_RULE_TEST_REGISTRY = createRegistryKey("pos_rule_test");
   public static final ResourceKey<Registry<MenuType<?>>> MENU_REGISTRY = createRegistryKey("menu");
   public static final ResourceKey<Registry<RecipeType<?>>> RECIPE_TYPE_REGISTRY = createRegistryKey("recipe_type");
   public static final ResourceKey<Registry<RecipeSerializer<?>>> RECIPE_SERIALIZER_REGISTRY = createRegistryKey("recipe_serializer");
   public static final ResourceKey<Registry<Attribute>> ATTRIBUTE_REGISTRY = createRegistryKey("attribute");
   public static final ResourceKey<Registry<GameEvent>> GAME_EVENT_REGISTRY = createRegistryKey("game_event");
   public static final ResourceKey<Registry<PositionSourceType<?>>> POSITION_SOURCE_TYPE_REGISTRY = createRegistryKey("position_source_type");
   public static final ResourceKey<Registry<StatType<?>>> STAT_TYPE_REGISTRY = createRegistryKey("stat_type");
   public static final ResourceKey<Registry<VillagerType>> VILLAGER_TYPE_REGISTRY = createRegistryKey("villager_type");
   public static final ResourceKey<Registry<VillagerProfession>> VILLAGER_PROFESSION_REGISTRY = createRegistryKey("villager_profession");
   public static final ResourceKey<Registry<PoiType>> POINT_OF_INTEREST_TYPE_REGISTRY = createRegistryKey("point_of_interest_type");
   public static final ResourceKey<Registry<MemoryModuleType<?>>> MEMORY_MODULE_TYPE_REGISTRY = createRegistryKey("memory_module_type");
   public static final ResourceKey<Registry<SensorType<?>>> SENSOR_TYPE_REGISTRY = createRegistryKey("sensor_type");
   public static final ResourceKey<Registry<Schedule>> SCHEDULE_REGISTRY = createRegistryKey("schedule");
   public static final ResourceKey<Registry<Activity>> ACTIVITY_REGISTRY = createRegistryKey("activity");
   public static final ResourceKey<Registry<LootPoolEntryType>> LOOT_ENTRY_REGISTRY = createRegistryKey("loot_pool_entry_type");
   public static final ResourceKey<Registry<LootItemFunctionType>> LOOT_FUNCTION_REGISTRY = createRegistryKey("loot_function_type");
   public static final ResourceKey<Registry<LootItemConditionType>> LOOT_ITEM_REGISTRY = createRegistryKey("loot_condition_type");
   public static final ResourceKey<Registry<LootNumberProviderType>> LOOT_NUMBER_PROVIDER_REGISTRY = createRegistryKey("loot_number_provider_type");
   public static final ResourceKey<Registry<LootNbtProviderType>> LOOT_NBT_PROVIDER_REGISTRY = createRegistryKey("loot_nbt_provider_type");
   public static final ResourceKey<Registry<LootScoreProviderType>> LOOT_SCORE_PROVIDER_REGISTRY = createRegistryKey("loot_score_provider_type");
   public static final ResourceKey<Registry<ArgumentTypeInfo<?, ?>>> COMMAND_ARGUMENT_TYPE_REGISTRY = createRegistryKey("command_argument_type");
   public static final ResourceKey<Registry<DimensionType>> DIMENSION_TYPE_REGISTRY = createRegistryKey("dimension_type");
   public static final ResourceKey<Registry<Level>> DIMENSION_REGISTRY = createRegistryKey("dimension");
   public static final ResourceKey<Registry<LevelStem>> LEVEL_STEM_REGISTRY = createRegistryKey("dimension");
   public static final DefaultedRegistry<GameEvent> GAME_EVENT = registerDefaulted(GAME_EVENT_REGISTRY, "step", GameEvent::builtInRegistryHolder, (p_206044_) -> {
      return GameEvent.STEP;
   });
   @Deprecated public static final Registry<SoundEvent> SOUND_EVENT = forge(SOUND_EVENT_REGISTRY, (registry) -> {
      return SoundEvents.ITEM_PICKUP;
   });
   @Deprecated public static final DefaultedRegistry<Fluid> FLUID = forge(FLUID_REGISTRY, "empty", (registry) -> {
      return Fluids.EMPTY;
   });
   @Deprecated public static final Registry<MobEffect> MOB_EFFECT = forge(MOB_EFFECT_REGISTRY, (registry) -> {
      return MobEffects.LUCK;
   });
   @Deprecated public static final DefaultedRegistry<Block> BLOCK = forge(BLOCK_REGISTRY, "air", (registry) -> {
      return Blocks.AIR;
   });
   @Deprecated public static final Registry<Enchantment> ENCHANTMENT = forge(ENCHANTMENT_REGISTRY, (registry) -> {
      return Enchantments.BLOCK_FORTUNE;
   });
   @Deprecated public static final DefaultedRegistry<EntityType<?>> ENTITY_TYPE = forge(ENTITY_TYPE_REGISTRY, "pig", (registry) -> {
      return EntityType.PIG;
   });
   @Deprecated public static final DefaultedRegistry<Item> ITEM = forge(ITEM_REGISTRY, "air", (registry) -> {
      return Items.AIR;
   });
   @Deprecated public static final DefaultedRegistry<Potion> POTION = forge(POTION_REGISTRY, "empty", (registry) -> {
      return Potions.EMPTY;
   });
   @Deprecated public static final Registry<ParticleType<?>> PARTICLE_TYPE = forge(PARTICLE_TYPE_REGISTRY, (registry) -> {
      return ParticleTypes.BLOCK;
   });
   @Deprecated public static final Registry<BlockEntityType<?>> BLOCK_ENTITY_TYPE = forge(BLOCK_ENTITY_TYPE_REGISTRY, (registry) -> {
      return BlockEntityType.FURNACE;
   });
   @Deprecated public static final DefaultedRegistry<PaintingVariant> PAINTING_VARIANT = forge(PAINTING_VARIANT_REGISTRY, "kebab", PaintingVariants::bootstrap);
   public static final Registry<ResourceLocation> CUSTOM_STAT = registerSimple(CUSTOM_STAT_REGISTRY, (p_235777_) -> {
      return Stats.JUMP;
   });
   @Deprecated public static final DefaultedRegistry<ChunkStatus> CHUNK_STATUS = forge(CHUNK_STATUS_REGISTRY, "empty", (registry) -> {
      return ChunkStatus.EMPTY;
   });
   public static final Registry<RuleTestType<?>> RULE_TEST = registerSimple(RULE_TEST_REGISTRY, (p_235773_) -> {
      return RuleTestType.ALWAYS_TRUE_TEST;
   });
   public static final Registry<PosRuleTestType<?>> POS_RULE_TEST = registerSimple(POS_RULE_TEST_REGISTRY, (p_235771_) -> {
      return PosRuleTestType.ALWAYS_TRUE_TEST;
   });
   @Deprecated public static final Registry<MenuType<?>> MENU = forge(MENU_REGISTRY, (registry) -> {
      return MenuType.ANVIL;
   });
   @Deprecated public static final Registry<RecipeType<?>> RECIPE_TYPE = forge(RECIPE_TYPE_REGISTRY, (p_235767_) -> {
      return RecipeType.CRAFTING;
   });
   @Deprecated public static final Registry<RecipeSerializer<?>> RECIPE_SERIALIZER = forge(RECIPE_SERIALIZER_REGISTRY, (registry) -> {
      return RecipeSerializer.SHAPELESS_RECIPE;
   });
   @Deprecated public static final Registry<Attribute> ATTRIBUTE = forge(ATTRIBUTE_REGISTRY, (registry) -> {
      return Attributes.LUCK;
   });
   public static final Registry<PositionSourceType<?>> POSITION_SOURCE_TYPE = registerSimple(POSITION_SOURCE_TYPE_REGISTRY, (p_235761_) -> {
      return PositionSourceType.BLOCK;
   });
   @Deprecated public static final Registry<ArgumentTypeInfo<?, ?>> COMMAND_ARGUMENT_TYPE = forge(COMMAND_ARGUMENT_TYPE_REGISTRY, (RegistryBootstrap)ArgumentTypeInfos::bootstrap);
   @Deprecated public static final Registry<StatType<?>> STAT_TYPE = forge(STAT_TYPE_REGISTRY, (registry) -> {
      return Stats.ITEM_USED;
   });
   public static final DefaultedRegistry<VillagerType> VILLAGER_TYPE = registerDefaulted(VILLAGER_TYPE_REGISTRY, "plains", (p_235757_) -> {
      return VillagerType.PLAINS;
   });
   @Deprecated public static final DefaultedRegistry<VillagerProfession> VILLAGER_PROFESSION = forge(VILLAGER_PROFESSION_REGISTRY, "none", (registry) -> {
      return VillagerProfession.NONE;
   });
   @Deprecated public static final Registry<PoiType> POINT_OF_INTEREST_TYPE = forge(POINT_OF_INTEREST_TYPE_REGISTRY, PoiTypes::bootstrap);
   @Deprecated public static final DefaultedRegistry<MemoryModuleType<?>> MEMORY_MODULE_TYPE = forge(MEMORY_MODULE_TYPE_REGISTRY, "dummy", (registry) -> {
      return MemoryModuleType.DUMMY;
   });
   @Deprecated public static final DefaultedRegistry<SensorType<?>> SENSOR_TYPE = forge(SENSOR_TYPE_REGISTRY, "dummy", (registry) -> {
      return SensorType.DUMMY;
   });
   @Deprecated public static final Registry<Schedule> SCHEDULE = forge(SCHEDULE_REGISTRY, (registry) -> {
      return Schedule.EMPTY;
   });
   @Deprecated public static final Registry<Activity> ACTIVITY = forge(ACTIVITY_REGISTRY, (registry) -> {
      return Activity.IDLE;
   });
   public static final Registry<LootPoolEntryType> LOOT_POOL_ENTRY_TYPE = registerSimple(LOOT_ENTRY_REGISTRY, (p_235745_) -> {
      return LootPoolEntries.EMPTY;
   });
   public static final Registry<LootItemFunctionType> LOOT_FUNCTION_TYPE = registerSimple(LOOT_FUNCTION_REGISTRY, (p_235860_) -> {
      return LootItemFunctions.SET_COUNT;
   });
   public static final Registry<LootItemConditionType> LOOT_CONDITION_TYPE = registerSimple(LOOT_ITEM_REGISTRY, (p_235858_) -> {
      return LootItemConditions.INVERTED;
   });
   public static final Registry<LootNumberProviderType> LOOT_NUMBER_PROVIDER_TYPE = registerSimple(LOOT_NUMBER_PROVIDER_REGISTRY, (p_235856_) -> {
      return NumberProviders.CONSTANT;
   });
   public static final Registry<LootNbtProviderType> LOOT_NBT_PROVIDER_TYPE = registerSimple(LOOT_NBT_PROVIDER_REGISTRY, (p_235854_) -> {
      return NbtProviders.CONTEXT;
   });
   public static final Registry<LootScoreProviderType> LOOT_SCORE_PROVIDER_TYPE = registerSimple(LOOT_SCORE_PROVIDER_REGISTRY, (p_235852_) -> {
      return ScoreboardNameProviders.CONTEXT;
   });
   public static final ResourceKey<Registry<FloatProviderType<?>>> FLOAT_PROVIDER_TYPE_REGISTRY = createRegistryKey("float_provider_type");
   public static final Registry<FloatProviderType<?>> FLOAT_PROVIDER_TYPES = registerSimple(FLOAT_PROVIDER_TYPE_REGISTRY, (p_235850_) -> {
      return FloatProviderType.CONSTANT;
   });
   public static final ResourceKey<Registry<IntProviderType<?>>> INT_PROVIDER_TYPE_REGISTRY = createRegistryKey("int_provider_type");
   public static final Registry<IntProviderType<?>> INT_PROVIDER_TYPES = registerSimple(INT_PROVIDER_TYPE_REGISTRY, (p_235848_) -> {
      return IntProviderType.CONSTANT;
   });
   public static final ResourceKey<Registry<HeightProviderType<?>>> HEIGHT_PROVIDER_TYPE_REGISTRY = createRegistryKey("height_provider_type");
   public static final Registry<HeightProviderType<?>> HEIGHT_PROVIDER_TYPES = registerSimple(HEIGHT_PROVIDER_TYPE_REGISTRY, (p_235846_) -> {
      return HeightProviderType.CONSTANT;
   });
   public static final ResourceKey<Registry<BlockPredicateType<?>>> BLOCK_PREDICATE_TYPE_REGISTRY = createRegistryKey("block_predicate_type");
   public static final Registry<BlockPredicateType<?>> BLOCK_PREDICATE_TYPES = registerSimple(BLOCK_PREDICATE_TYPE_REGISTRY, (p_235844_) -> {
      return BlockPredicateType.NOT;
   });
   public static final ResourceKey<Registry<NoiseGeneratorSettings>> NOISE_GENERATOR_SETTINGS_REGISTRY = createRegistryKey("worldgen/noise_settings");
   public static final ResourceKey<Registry<ConfiguredWorldCarver<?>>> CONFIGURED_CARVER_REGISTRY = createRegistryKey("worldgen/configured_carver");
   public static final ResourceKey<Registry<ConfiguredFeature<?, ?>>> CONFIGURED_FEATURE_REGISTRY = createRegistryKey("worldgen/configured_feature");
   public static final ResourceKey<Registry<PlacedFeature>> PLACED_FEATURE_REGISTRY = createRegistryKey("worldgen/placed_feature");
   public static final ResourceKey<Registry<Structure>> STRUCTURE_REGISTRY = createRegistryKey("worldgen/structure");
   public static final ResourceKey<Registry<StructureSet>> STRUCTURE_SET_REGISTRY = createRegistryKey("worldgen/structure_set");
   public static final ResourceKey<Registry<StructureProcessorList>> PROCESSOR_LIST_REGISTRY = createRegistryKey("worldgen/processor_list");
   public static final ResourceKey<Registry<StructureTemplatePool>> TEMPLATE_POOL_REGISTRY = createRegistryKey("worldgen/template_pool");
   public static final ResourceKey<Registry<Biome>> BIOME_REGISTRY = createRegistryKey("worldgen/biome");
   public static final ResourceKey<Registry<NormalNoise.NoiseParameters>> NOISE_REGISTRY = createRegistryKey("worldgen/noise");
   public static final ResourceKey<Registry<DensityFunction>> DENSITY_FUNCTION_REGISTRY = createRegistryKey("worldgen/density_function");
   public static final ResourceKey<Registry<WorldPreset>> WORLD_PRESET_REGISTRY = createRegistryKey("worldgen/world_preset");
   public static final ResourceKey<Registry<FlatLevelGeneratorPreset>> FLAT_LEVEL_GENERATOR_PRESET_REGISTRY = createRegistryKey("worldgen/flat_level_generator_preset");
   public static final ResourceKey<Registry<WorldCarver<?>>> CARVER_REGISTRY = createRegistryKey("worldgen/carver");
   @Deprecated public static final Registry<WorldCarver<?>> CARVER = forge(CARVER_REGISTRY, (registry) -> {
      return WorldCarver.CAVE;
   });
   public static final ResourceKey<Registry<Feature<?>>> FEATURE_REGISTRY = createRegistryKey("worldgen/feature");
   @Deprecated public static final Registry<Feature<?>> FEATURE = forge(FEATURE_REGISTRY, (registry) -> {
      return Feature.ORE;
   });
   public static final ResourceKey<Registry<StructurePlacementType<?>>> STRUCTURE_PLACEMENT_TYPE_REGISTRY = createRegistryKey("worldgen/structure_placement");
   public static final Registry<StructurePlacementType<?>> STRUCTURE_PLACEMENT_TYPE = registerSimple(STRUCTURE_PLACEMENT_TYPE_REGISTRY, (p_235838_) -> {
         return StructurePlacementType.RANDOM_SPREAD;
   });
   public static final ResourceKey<Registry<StructurePieceType>> STRUCTURE_PIECE_REGISTRY = createRegistryKey("worldgen/structure_piece");
   public static final Registry<StructurePieceType> STRUCTURE_PIECE = registerSimple(STRUCTURE_PIECE_REGISTRY, (p_235836_) -> {
      return StructurePieceType.MINE_SHAFT_ROOM;
   });
   public static final ResourceKey<Registry<StructureType<?>>> STRUCTURE_TYPE_REGISTRY = createRegistryKey("worldgen/structure_type");
   public static final Registry<StructureType<?>> STRUCTURE_TYPES = registerSimple(STRUCTURE_TYPE_REGISTRY, (p_235834_) -> {
      return StructureType.JIGSAW;
   });
   public static final ResourceKey<Registry<PlacementModifierType<?>>> PLACEMENT_MODIFIER_REGISTRY = createRegistryKey("worldgen/placement_modifier_type");
   public static final Registry<PlacementModifierType<?>> PLACEMENT_MODIFIERS = registerSimple(PLACEMENT_MODIFIER_REGISTRY, (p_235832_) -> {
      return PlacementModifierType.COUNT;
   });
   public static final ResourceKey<Registry<BlockStateProviderType<?>>> BLOCK_STATE_PROVIDER_TYPE_REGISTRY = createRegistryKey("worldgen/block_state_provider_type");
   public static final ResourceKey<Registry<FoliagePlacerType<?>>> FOLIAGE_PLACER_TYPE_REGISTRY = createRegistryKey("worldgen/foliage_placer_type");
   public static final ResourceKey<Registry<TrunkPlacerType<?>>> TRUNK_PLACER_TYPE_REGISTRY = createRegistryKey("worldgen/trunk_placer_type");
   public static final ResourceKey<Registry<TreeDecoratorType<?>>> TREE_DECORATOR_TYPE_REGISTRY = createRegistryKey("worldgen/tree_decorator_type");
   public static final ResourceKey<Registry<RootPlacerType<?>>> ROOT_PLACER_TYPE_REGISTRY = createRegistryKey("worldgen/root_placer_type");
   public static final ResourceKey<Registry<FeatureSizeType<?>>> FEATURE_SIZE_TYPE_REGISTRY = createRegistryKey("worldgen/feature_size_type");
   public static final ResourceKey<Registry<Codec<? extends BiomeSource>>> BIOME_SOURCE_REGISTRY = createRegistryKey("worldgen/biome_source");
   public static final ResourceKey<Registry<Codec<? extends ChunkGenerator>>> CHUNK_GENERATOR_REGISTRY = createRegistryKey("worldgen/chunk_generator");
   public static final ResourceKey<Registry<Codec<? extends SurfaceRules.ConditionSource>>> CONDITION_REGISTRY = createRegistryKey("worldgen/material_condition");
   public static final ResourceKey<Registry<Codec<? extends SurfaceRules.RuleSource>>> RULE_REGISTRY = createRegistryKey("worldgen/material_rule");
   public static final ResourceKey<Registry<Codec<? extends DensityFunction>>> DENSITY_FUNCTION_TYPE_REGISTRY = createRegistryKey("worldgen/density_function_type");
   public static final ResourceKey<Registry<StructureProcessorType<?>>> STRUCTURE_PROCESSOR_REGISTRY = createRegistryKey("worldgen/structure_processor");
   public static final ResourceKey<Registry<StructurePoolElementType<?>>> STRUCTURE_POOL_ELEMENT_REGISTRY = createRegistryKey("worldgen/structure_pool_element");
   @Deprecated public static final Registry<BlockStateProviderType<?>> BLOCKSTATE_PROVIDER_TYPES = forge(BLOCK_STATE_PROVIDER_TYPE_REGISTRY, (registry) -> {
      return BlockStateProviderType.SIMPLE_STATE_PROVIDER;
   });
   @Deprecated public static final Registry<FoliagePlacerType<?>> FOLIAGE_PLACER_TYPES = forge(FOLIAGE_PLACER_TYPE_REGISTRY, (registry) -> {
      return FoliagePlacerType.BLOB_FOLIAGE_PLACER;
   });
   public static final Registry<TrunkPlacerType<?>> TRUNK_PLACER_TYPES = registerSimple(TRUNK_PLACER_TYPE_REGISTRY, (p_235822_) -> {
      return TrunkPlacerType.STRAIGHT_TRUNK_PLACER;
   });
   public static final Registry<RootPlacerType<?>> ROOT_PLACER_TYPES = registerSimple(ROOT_PLACER_TYPE_REGISTRY, (p_235818_) -> {
      return RootPlacerType.MANGROVE_ROOT_PLACER;
   });
   @Deprecated public static final Registry<TreeDecoratorType<?>> TREE_DECORATOR_TYPES = forge(TREE_DECORATOR_TYPE_REGISTRY, (registry) -> {
      return TreeDecoratorType.LEAVE_VINE;
   });
   public static final Registry<FeatureSizeType<?>> FEATURE_SIZE_TYPES = registerSimple(FEATURE_SIZE_TYPE_REGISTRY, (p_235808_) -> {
      return FeatureSizeType.TWO_LAYERS_FEATURE_SIZE;
   });
   public static final Registry<Codec<? extends BiomeSource>> BIOME_SOURCE = registerSimple(BIOME_SOURCE_REGISTRY, Lifecycle.stable(), (RegistryBootstrap)BiomeSources::bootstrap);
   public static final Registry<Codec<? extends ChunkGenerator>> CHUNK_GENERATOR = registerSimple(CHUNK_GENERATOR_REGISTRY, Lifecycle.stable(), (RegistryBootstrap)ChunkGenerators::bootstrap);
   public static final Registry<Codec<? extends SurfaceRules.ConditionSource>> CONDITION = registerSimple(CONDITION_REGISTRY, (RegistryBootstrap)SurfaceRules.ConditionSource::bootstrap);
   public static final Registry<Codec<? extends SurfaceRules.RuleSource>> RULE = registerSimple(RULE_REGISTRY, (RegistryBootstrap)SurfaceRules.RuleSource::bootstrap);
   public static final Registry<Codec<? extends DensityFunction>> DENSITY_FUNCTION_TYPES = registerSimple(DENSITY_FUNCTION_TYPE_REGISTRY, (RegistryBootstrap)DensityFunctions::bootstrap);
   public static final Registry<StructureProcessorType<?>> STRUCTURE_PROCESSOR = registerSimple(STRUCTURE_PROCESSOR_REGISTRY, (p_235806_) -> {
      return StructureProcessorType.BLOCK_IGNORE;
   });
   public static final Registry<StructurePoolElementType<?>> STRUCTURE_POOL_ELEMENT = registerSimple(STRUCTURE_POOL_ELEMENT_REGISTRY, (p_235802_) -> {
      return StructurePoolElementType.EMPTY;
   });
   public static final ResourceKey<Registry<ChatType>> CHAT_TYPE_REGISTRY = createRegistryKey("chat_type");
   public static final ResourceKey<Registry<CatVariant>> CAT_VARIANT_REGISTRY = createRegistryKey("cat_variant");
   public static final Registry<CatVariant> CAT_VARIANT = registerSimple(CAT_VARIANT_REGISTRY, (p_235797_) -> {
      return CatVariant.BLACK;
   });
   public static final ResourceKey<Registry<FrogVariant>> FROG_VARIANT_REGISTRY = createRegistryKey("frog_variant");
   public static final Registry<FrogVariant> FROG_VARIANT = registerSimple(FROG_VARIANT_REGISTRY, (p_235794_) -> {
      return FrogVariant.TEMPERATE;
   });
   public static final ResourceKey<Registry<BannerPattern>> BANNER_PATTERN_REGISTRY = createRegistryKey("banner_pattern");
   public static final Registry<BannerPattern> BANNER_PATTERN = registerSimple(BANNER_PATTERN_REGISTRY, BannerPatterns::bootstrap);
   public static final ResourceKey<Registry<Instrument>> INSTRUMENT_REGISTRY = createRegistryKey("instrument");
   public static final Registry<Instrument> INSTRUMENT = registerSimple(INSTRUMENT_REGISTRY, Instruments::bootstrap);
   private final ResourceKey<? extends Registry<T>> key;
   private final Lifecycle lifecycle;

   private static <T> ResourceKey<Registry<T>> createRegistryKey(String pRegistryName) {
      return ResourceKey.createRegistryKey(new ResourceLocation(pRegistryName));
   }

   public static <T extends Registry<?>> void checkRegistry(Registry<T> pMetaRegistry) {
      pMetaRegistry.forEach((p_235790_) -> {
         if (p_235790_.keySet().isEmpty()) {
            Util.logAndPauseIfInIde("Registry '" + pMetaRegistry.getKey(p_235790_) + "' was empty after loading");
         }

         if (p_235790_ instanceof DefaultedRegistry) {
            ResourceLocation resourcelocation = ((DefaultedRegistry)p_235790_).getDefaultKey();
            Validate.notNull(p_235790_.get(resourcelocation), "Missing default of DefaultedMappedRegistry: " + pMetaRegistry.key + ", Entry: " + resourcelocation);
         }

      });
   }

   /**
    * Makes a simple registry with the default lifecycle of "experimental" and registers it
    */
   private static <T> Registry<T> registerSimple(ResourceKey<? extends Registry<T>> pRegistryKey, Registry.RegistryBootstrap<T> pLoader) {
      return registerSimple(pRegistryKey, Lifecycle.experimental(), pLoader);
   }

   private static <T> Registry<T> forge(ResourceKey<? extends Registry<T>> key, Registry.RegistryBootstrap<T> def) {
      return forge(key, Lifecycle.experimental(), def);
   }

   private static <T> DefaultedRegistry<T> registerDefaulted(ResourceKey<? extends Registry<T>> pRegistryKey, String pDefaultValueName, Registry.RegistryBootstrap<T> pLoader) {
      return registerDefaulted(pRegistryKey, pDefaultValueName, Lifecycle.experimental(), pLoader);
   }

   private static <T> DefaultedRegistry<T> forge(ResourceKey<? extends Registry<T>> key, String defKey, Registry.RegistryBootstrap<T> def) {
      return forge(key, defKey, Lifecycle.experimental(), def);
   }

   private static <T> DefaultedRegistry<T> registerDefaulted(ResourceKey<? extends Registry<T>> pRegistryKey, String pDefaultValueName, Function<T, Holder.Reference<T>> pCustomHolderProvider, Registry.RegistryBootstrap<T> pBootstrap) {
      return registerDefaulted(pRegistryKey, pDefaultValueName, Lifecycle.experimental(), pCustomHolderProvider, pBootstrap);
   }

   private static <T> Registry<T> forge(ResourceKey<? extends Registry<T>> key, Lifecycle cycle, Registry.RegistryBootstrap<T> def) {
      return internalRegister(key, net.minecraftforge.registries.GameData.getWrapper(key, cycle), def, cycle);
   }

   /**
    * Makes a simple registry and registers it
    */
   private static <T> Registry<T> registerSimple(ResourceKey<? extends Registry<T>> pRegistryKey, Lifecycle pLifecycle, Registry.RegistryBootstrap<T> pLoader) {
      return internalRegister(pRegistryKey, new MappedRegistry<>(pRegistryKey, pLifecycle, (Function<T, Holder.Reference<T>>)null), pLoader, pLifecycle);
   }

   private static <T> DefaultedRegistry<T> forge(ResourceKey<? extends Registry<T>> key, String defKey, Lifecycle cycle, Registry.RegistryBootstrap<T> def) {
      return internalRegister(key, net.minecraftforge.registries.GameData.getWrapper(key, cycle, defKey), def, cycle);
   }

   private static <T> Registry<T> registerSimple(ResourceKey<? extends Registry<T>> pKey, Lifecycle pLifecycle, Function<T, Holder.Reference<T>> pCustomHolderProvider, Registry.RegistryBootstrap<T> pBootstrap) {
      return internalRegister(pKey, new MappedRegistry<>(pKey, pLifecycle, pCustomHolderProvider), pBootstrap, pLifecycle);
   }

   private static <T> DefaultedRegistry<T> registerDefaulted(ResourceKey<? extends Registry<T>> pRegistryKey, String pDefaultValueName, Lifecycle pLifecycle, Registry.RegistryBootstrap<T> pLoader) {
      return internalRegister(pRegistryKey, new DefaultedRegistry<>(pDefaultValueName, pRegistryKey, pLifecycle, (Function<T, Holder.Reference<T>>)null), pLoader, pLifecycle);
   }

   private static <T> DefaultedRegistry<T> registerDefaulted(ResourceKey<? extends Registry<T>> pKey, String pDefaultName, Lifecycle pElementsLifecycle, Function<T, Holder.Reference<T>> pCustomHolderProvider, Registry.RegistryBootstrap<T> pBootstrap) {
      return internalRegister(pKey, new DefaultedRegistry<>(pDefaultName, pKey, pElementsLifecycle, pCustomHolderProvider), pBootstrap, pElementsLifecycle);
   }

   private static <T, R extends WritableRegistry<T>> R internalRegister(ResourceKey<? extends Registry<T>> pRegistryKey, R pRegistry, Registry.RegistryBootstrap<T> pLoadaer, Lifecycle pLifecycle) {
      ResourceLocation resourcelocation = pRegistryKey.location();
      LOADERS.put(resourcelocation, () -> {
         return pLoadaer.run(pRegistry);
      });
      WRITABLE_REGISTRY.register((ResourceKey)pRegistryKey, pRegistry, pLifecycle);
      return pRegistry;
   }

   protected Registry(ResourceKey<? extends Registry<T>> pKey, Lifecycle pLifecycle) {
      Bootstrap.checkBootstrapCalled(() -> {
         return "registry " + pKey;
      });
      this.key = pKey;
      this.lifecycle = pLifecycle;
   }

   public static void freezeBuiltins() {
      for(Registry<?> registry : REGISTRY) {
         registry.freeze();
      }

   }

   public ResourceKey<? extends Registry<T>> key() {
      return this.key;
   }

   public Lifecycle lifecycle() {
      return this.lifecycle;
   }

   public String toString() {
      return "Registry[" + this.key + " (" + this.lifecycle + ")]";
   }

   public Codec<T> byNameCodec() {
      Codec<T> codec = ResourceLocation.CODEC.flatXmap((p_206084_) -> {
         return Optional.ofNullable(this.get(p_206084_)).map(DataResult::success).orElseGet(() -> {
            return DataResult.error("Unknown registry key in " + this.key + ": " + p_206084_);
         });
      }, (p_206094_) -> {
         return this.getResourceKey(p_206094_).map(ResourceKey::location).map(DataResult::success).orElseGet(() -> {
            return DataResult.error("Unknown registry element in " + this.key + ":" + p_206094_);
         });
      });
      Codec<T> codec1 = ExtraCodecs.idResolverCodec((p_235816_) -> {
         return this.getResourceKey(p_235816_).isPresent() ? this.getId(p_235816_) : -1;
      }, this::byId, -1);
      return ExtraCodecs.overrideLifecycle(ExtraCodecs.orCompressed(codec, codec1), this::lifecycle, (p_235810_) -> {
         return this.lifecycle;
      });
   }

   public Codec<Holder<T>> holderByNameCodec() {
      Codec<Holder<T>> codec = ResourceLocation.CODEC.flatXmap((p_206070_) -> {
         return this.getHolder(ResourceKey.create(this.key, p_206070_)).map(DataResult::success).orElseGet(() -> {
            return DataResult.error("Unknown registry key in " + this.key + ": " + p_206070_);
         });
      }, (p_206061_) -> {
         return p_206061_.unwrapKey().map(ResourceKey::location).map(DataResult::success).orElseGet(() -> {
            return DataResult.error("Unknown registry element in " + this.key + ":" + p_206061_);
         });
      });
      return ExtraCodecs.overrideLifecycle(codec, (p_235792_) -> {
         return this.lifecycle(p_235792_.value());
      }, (p_206047_) -> {
         return this.lifecycle;
      });
   }

   public <U> Stream<U> keys(DynamicOps<U> pOps) {
      return this.keySet().stream().map((p_235784_) -> {
         return pOps.createString(p_235784_.toString());
      });
   }

   /**
    * @return the name used to identify the given object within this registry or {@code null} if the object is not
    * within this registry
    */
   @Nullable
   public abstract ResourceLocation getKey(T pValue);

   public abstract Optional<ResourceKey<T>> getResourceKey(T pValue);

   /**
    * @return the integer ID used to identify the given object
    */
   public abstract int getId(@Nullable T pValue);

   @Nullable
   public abstract T get(@Nullable ResourceKey<T> pKey);

   @Nullable
   public abstract T get(@Nullable ResourceLocation pName);

   public abstract Lifecycle lifecycle(T p_123012_);

   public abstract Lifecycle elementsLifecycle();

   public Optional<T> getOptional(@Nullable ResourceLocation pName) {
      return Optional.ofNullable(this.get(pName));
   }

   public Optional<T> getOptional(@Nullable ResourceKey<T> pRegistryKey) {
      return Optional.ofNullable(this.get(pRegistryKey));
   }

   public T getOrThrow(ResourceKey<T> pKey) {
      T t = this.get(pKey);
      if (t == null) {
         throw new IllegalStateException("Missing key in " + this.key + ": " + pKey);
      } else {
         return t;
      }
   }

   /**
    * @return all keys in this registry
    */
   public abstract Set<ResourceLocation> keySet();

   public abstract Set<Map.Entry<ResourceKey<T>, T>> entrySet();

   public abstract Set<ResourceKey<T>> registryKeySet();

   public abstract Optional<Holder<T>> getRandom(RandomSource pRandom);

   public Stream<T> stream() {
      return StreamSupport.stream(this.spliterator(), false);
   }

   public abstract boolean containsKey(ResourceLocation pName);

   public abstract boolean containsKey(ResourceKey<T> pKey);

   public static <T> T register(Registry<? super T> pRegistry, String pName, T pValue) {
      return register(pRegistry, new ResourceLocation(pName), pValue);
   }

   public static <V, T extends V> T register(Registry<V> pRegistry, ResourceLocation pName, T pValue) {
      return register(pRegistry, ResourceKey.create(pRegistry.key, pName), pValue);
   }

   public static <V, T extends V> T register(Registry<V> pRegistry, ResourceKey<V> pKey, T pValue) {
      ((WritableRegistry)pRegistry).register(pKey, (V)pValue, Lifecycle.stable());
      return pValue;
   }

   public static <V, T extends V> T registerMapping(Registry<V> pRegistry, int pId, String pName, T pValue) {
      ((WritableRegistry)pRegistry).registerMapping(pId, ResourceKey.create(pRegistry.key, new ResourceLocation(pName)), (V)pValue, Lifecycle.stable());
      return pValue;
   }

   public abstract Registry<T> freeze();

   public abstract Holder<T> getOrCreateHolderOrThrow(ResourceKey<T> pKey);

   public abstract DataResult<Holder<T>> getOrCreateHolder(ResourceKey<T> pKey);

   public abstract Holder.Reference<T> createIntrusiveHolder(T pValue);

   public abstract Optional<Holder<T>> getHolder(int pId);

   public abstract Optional<Holder<T>> getHolder(ResourceKey<T> pKey);

   public Holder<T> getHolderOrThrow(ResourceKey<T> pKey) {
      return this.getHolder(pKey).orElseThrow(() -> {
         return new IllegalStateException("Missing key in " + this.key + ": " + pKey);
      });
   }

   public abstract Stream<Holder.Reference<T>> holders();

   public abstract Optional<HolderSet.Named<T>> getTag(TagKey<T> pKey);

   public Iterable<Holder<T>> getTagOrEmpty(TagKey<T> pKey) {
      return DataFixUtils.orElse(this.getTag(pKey), List.of());
   }

   public abstract HolderSet.Named<T> getOrCreateTag(TagKey<T> pKey);

   public abstract Stream<Pair<TagKey<T>, HolderSet.Named<T>>> getTags();

   public abstract Stream<TagKey<T>> getTagNames();

   public abstract boolean isKnownTagName(TagKey<T> pKey);

   public abstract void resetTags();

   public abstract void bindTags(Map<TagKey<T>, List<Holder<T>>> pTagMap);

   public IdMap<Holder<T>> asHolderIdMap() {
      return new IdMap<Holder<T>>() {
         /**
          * @return the integer ID used to identify the given object
          */
         public int getId(Holder<T> p_206142_) {
            return Registry.this.getId(p_206142_.value());
         }

         @Nullable
         public Holder<T> byId(int p_206147_) {
            return (Holder)Registry.this.getHolder(p_206147_).orElse(null);
         }

         public int size() {
            return Registry.this.size();
         }

         public Iterator<Holder<T>> iterator() {
            return Registry.this.holders().map((p_206140_) -> {
               return (Holder<T>)p_206140_;
            }).iterator();
         }
      };
   }

   static {
      BuiltinRegistries.bootstrap();
      LOADERS.forEach((p_235779_, p_235780_) -> {
         if (p_235780_.get() == null) {
            LOGGER.error("Unable to bootstrap registry '{}'", (Object)p_235779_);
         }

      });
      checkRegistry(WRITABLE_REGISTRY);
   }

   @FunctionalInterface
   interface RegistryBootstrap<T> {
      T run(Registry<T> pRegistry);
   }
}
