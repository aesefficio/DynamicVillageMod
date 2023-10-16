package net.minecraft.world.level.chunk;

import com.google.common.base.Stopwatch;
import com.google.common.base.Suppliers;
import com.mojang.datafixers.Products;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.FeatureSorter;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.slf4j.Logger;

public abstract class ChunkGenerator {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final Codec<ChunkGenerator> CODEC = Registry.CHUNK_GENERATOR.byNameCodec().dispatchStable(ChunkGenerator::codec, Function.identity());
   protected final Registry<StructureSet> structureSets;
   protected final BiomeSource biomeSource;
   private final Supplier<List<FeatureSorter.StepFeatureData>> featuresPerStep;
   protected final Optional<HolderSet<StructureSet>> structureOverrides;
   private final Function<Holder<Biome>, BiomeGenerationSettings> generationSettingsGetter;
   private final Map<Structure, List<StructurePlacement>> placementsForStructure = new Object2ObjectOpenHashMap<>();
   private final Map<ConcentricRingsStructurePlacement, CompletableFuture<List<ChunkPos>>> ringPositions = new Object2ObjectArrayMap<>();
   private boolean hasGeneratedPositions;

   protected static <T extends ChunkGenerator> Products.P1<RecordCodecBuilder.Mu<T>, Registry<StructureSet>> commonCodec(RecordCodecBuilder.Instance<T> pInstance) {
      return pInstance.group(RegistryOps.retrieveRegistry(Registry.STRUCTURE_SET_REGISTRY).forGetter((p_208008_) -> {
         return p_208008_.structureSets;
      }));
   }

   public ChunkGenerator(Registry<StructureSet> pStructureSets, Optional<HolderSet<StructureSet>> pStructureOverrides, BiomeSource pBiomeSource) {
      this(pStructureSets, pStructureOverrides, pBiomeSource, (p_223234_) -> {
         return p_223234_.value().getGenerationSettings();
      });
   }

   public ChunkGenerator(Registry<StructureSet> pStructureSets, Optional<HolderSet<StructureSet>> pStructureOverrides, BiomeSource pBiomeSource, Function<Holder<Biome>, BiomeGenerationSettings> pGenerationSettingsGetter) {
      this.structureSets = pStructureSets;
      this.biomeSource = pBiomeSource;
      this.generationSettingsGetter = pGenerationSettingsGetter;
      this.structureOverrides = pStructureOverrides;
      this.featuresPerStep = Suppliers.memoize(() -> {
         return FeatureSorter.buildFeaturesPerStep(List.copyOf(pBiomeSource.possibleBiomes()), (p_223216_) -> {
            return pGenerationSettingsGetter.apply(p_223216_).features();
         }, true);
      });
   }

   public Stream<Holder<StructureSet>> possibleStructureSets() {
      return this.structureOverrides.isPresent() ? this.structureOverrides.get().stream() : this.structureSets.holders().map(Holder::hackyErase);
   }

   private void generatePositions(RandomState pRandom) {
      Set<Holder<Biome>> set = this.biomeSource.possibleBiomes();
      this.possibleStructureSets().forEach((p_223208_) -> {
         StructureSet structureset = p_223208_.value();
         boolean flag = false;

         for(StructureSet.StructureSelectionEntry structureset$structureselectionentry : structureset.structures()) {
            Structure structure = structureset$structureselectionentry.structure().value();
            if (structure.biomes().stream().anyMatch(set::contains)) {
               this.placementsForStructure.computeIfAbsent(structure, (p_223230_) -> {
                  return new ArrayList();
               }).add(structureset.placement());
               flag = true;
            }
         }

         if (flag) {
            StructurePlacement structureplacement = structureset.placement();
            if (structureplacement instanceof ConcentricRingsStructurePlacement) {
               ConcentricRingsStructurePlacement concentricringsstructureplacement = (ConcentricRingsStructurePlacement)structureplacement;
               this.ringPositions.put(concentricringsstructureplacement, this.generateRingPositions(p_223208_, pRandom, concentricringsstructureplacement));
            }
         }

      });
   }

   private CompletableFuture<List<ChunkPos>> generateRingPositions(Holder<StructureSet> pStructureSet, RandomState pRandom, ConcentricRingsStructurePlacement pPlacement) {
      return pPlacement.count() == 0 ? CompletableFuture.completedFuture(List.of()) : CompletableFuture.supplyAsync(Util.wrapThreadWithTaskName("placement calculation", () -> {
         Stopwatch stopwatch = Stopwatch.createStarted(Util.TICKER);
         List<ChunkPos> list = new ArrayList<>();
         int i = pPlacement.distance();
         int j = pPlacement.count();
         int k = pPlacement.spread();
         HolderSet<Biome> holderset = pPlacement.preferredBiomes();
         RandomSource randomsource = RandomSource.create();
         randomsource.setSeed(this instanceof FlatLevelSource ? 0L : pRandom.legacyLevelSeed());
         double d0 = randomsource.nextDouble() * Math.PI * 2.0D;
         int l = 0;
         int i1 = 0;

         for(int j1 = 0; j1 < j; ++j1) {
            double d1 = (double)(4 * i + i * i1 * 6) + (randomsource.nextDouble() - 0.5D) * (double)i * 2.5D;
            int k1 = (int)Math.round(Math.cos(d0) * d1);
            int l1 = (int)Math.round(Math.sin(d0) * d1);
            Pair<BlockPos, Holder<Biome>> pair = this.biomeSource.findBiomeHorizontal(SectionPos.sectionToBlockCoord(k1, 8), 0, SectionPos.sectionToBlockCoord(l1, 8), 112, holderset::contains, randomsource, pRandom.sampler());
            if (pair != null) {
               BlockPos blockpos = pair.getFirst();
               k1 = SectionPos.blockToSectionCoord(blockpos.getX());
               l1 = SectionPos.blockToSectionCoord(blockpos.getZ());
            }

            list.add(new ChunkPos(k1, l1));
            d0 += (Math.PI * 2D) / (double)k;
            ++l;
            if (l == k) {
               ++i1;
               l = 0;
               k += 2 * k / (i1 + 1);
               k = Math.min(k, j - j1);
               d0 += randomsource.nextDouble() * Math.PI * 2.0D;
            }
         }

         double d2 = (double)stopwatch.stop().elapsed(TimeUnit.MILLISECONDS) / 1000.0D;
         LOGGER.debug("Calculation for {} took {}s", pStructureSet, d2);
         return list;
      }), Util.backgroundExecutor());
   }

   protected abstract Codec<? extends ChunkGenerator> codec();

   public Optional<ResourceKey<Codec<? extends ChunkGenerator>>> getTypeNameForDataFixer() {
      return Registry.CHUNK_GENERATOR.getResourceKey(this.codec());
   }

   public CompletableFuture<ChunkAccess> createBiomes(Registry<Biome> pBiomeRegistry, Executor pExecutor, RandomState pRandom, Blender pBlender, StructureManager pStructureManager, ChunkAccess pChunk) {
      return CompletableFuture.supplyAsync(Util.wrapThreadWithTaskName("init_biomes", () -> {
         pChunk.fillBiomesFromNoise(this.biomeSource, pRandom.sampler());
         return pChunk;
      }), Util.backgroundExecutor());
   }

   public abstract void applyCarvers(WorldGenRegion pLevel, long pSeed, RandomState pRandom, BiomeManager pBiomeManager, StructureManager pStructureManager, ChunkAccess pChunk, GenerationStep.Carving pStep);

   @Nullable
   public Pair<BlockPos, Holder<Structure>> findNearestMapStructure(ServerLevel pLeve, HolderSet<Structure> pStructure, BlockPos pPos, int pSearchRadius, boolean pSkipKnownStructures) {
      Map<StructurePlacement, Set<Holder<Structure>>> map = new Object2ObjectArrayMap<>();

      for(Holder<Structure> holder : pStructure) {
         for(StructurePlacement structureplacement : this.getPlacementsForStructure(holder, pLeve.getChunkSource().randomState())) {
            map.computeIfAbsent(structureplacement, (p_223127_) -> {
               return new ObjectArraySet();
            }).add(holder);
         }
      }

      if (map.isEmpty()) {
         return null;
      } else {
         Pair<BlockPos, Holder<Structure>> pair2 = null;
         double d2 = Double.MAX_VALUE;
         StructureManager structuremanager = pLeve.structureManager();
         List<Map.Entry<StructurePlacement, Set<Holder<Structure>>>> list = new ArrayList<>(map.size());

         for(Map.Entry<StructurePlacement, Set<Holder<Structure>>> entry : map.entrySet()) {
            StructurePlacement structureplacement1 = entry.getKey();
            if (structureplacement1 instanceof ConcentricRingsStructurePlacement) {
               ConcentricRingsStructurePlacement concentricringsstructureplacement = (ConcentricRingsStructurePlacement)structureplacement1;
               Pair<BlockPos, Holder<Structure>> pair = this.getNearestGeneratedStructure(entry.getValue(), pLeve, structuremanager, pPos, pSkipKnownStructures, concentricringsstructureplacement);
               if (pair != null) {
                  BlockPos blockpos = pair.getFirst();
                  double d0 = pPos.distSqr(blockpos);
                  if (d0 < d2) {
                     d2 = d0;
                     pair2 = pair;
                  }
               }
            } else if (structureplacement1 instanceof RandomSpreadStructurePlacement) {
               list.add(entry);
            }
         }

         if (!list.isEmpty()) {
            int i = SectionPos.blockToSectionCoord(pPos.getX());
            int j = SectionPos.blockToSectionCoord(pPos.getZ());

            for(int k = 0; k <= pSearchRadius; ++k) {
               boolean flag = false;

               for(Map.Entry<StructurePlacement, Set<Holder<Structure>>> entry1 : list) {
                  RandomSpreadStructurePlacement randomspreadstructureplacement = (RandomSpreadStructurePlacement)entry1.getKey();
                  Pair<BlockPos, Holder<Structure>> pair1 = getNearestGeneratedStructure(entry1.getValue(), pLeve, structuremanager, i, j, k, pSkipKnownStructures, pLeve.getSeed(), randomspreadstructureplacement);
                  if (pair1 != null) {
                     flag = true;
                     double d1 = pPos.distSqr(pair1.getFirst());
                     if (d1 < d2) {
                        d2 = d1;
                        pair2 = pair1;
                     }
                  }
               }

               if (flag) {
                  return pair2;
               }
            }
         }

         return pair2;
      }
   }

   @Nullable
   private Pair<BlockPos, Holder<Structure>> getNearestGeneratedStructure(Set<Holder<Structure>> pStructureHoldersSet, ServerLevel pLevel, StructureManager pStructureManager, BlockPos pPos, boolean pSkipKnownStructures, ConcentricRingsStructurePlacement pPlacement) {
      List<ChunkPos> list = this.getRingPositionsFor(pPlacement, pLevel.getChunkSource().randomState());
      if (list == null) {
         throw new IllegalStateException("Somehow tried to find structures for a placement that doesn't exist");
      } else {
         Pair<BlockPos, Holder<Structure>> pair = null;
         double d0 = Double.MAX_VALUE;
         BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

         for(ChunkPos chunkpos : list) {
            blockpos$mutableblockpos.set(SectionPos.sectionToBlockCoord(chunkpos.x, 8), 32, SectionPos.sectionToBlockCoord(chunkpos.z, 8));
            double d1 = blockpos$mutableblockpos.distSqr(pPos);
            boolean flag = pair == null || d1 < d0;
            if (flag) {
               Pair<BlockPos, Holder<Structure>> pair1 = getStructureGeneratingAt(pStructureHoldersSet, pLevel, pStructureManager, pSkipKnownStructures, pPlacement, chunkpos);
               if (pair1 != null) {
                  pair = pair1;
                  d0 = d1;
               }
            }
         }

         return pair;
      }
   }

   @Nullable
   private static Pair<BlockPos, Holder<Structure>> getNearestGeneratedStructure(Set<Holder<Structure>> pStructureHoldersSet, LevelReader pLevel, StructureManager pStructureManager, int pX, int pY, int pZ, boolean pSkipKnownStructures, long pSeed, RandomSpreadStructurePlacement pSpreadPlacement) {
      int i = pSpreadPlacement.spacing();

      for(int j = -pZ; j <= pZ; ++j) {
         boolean flag = j == -pZ || j == pZ;

         for(int k = -pZ; k <= pZ; ++k) {
            boolean flag1 = k == -pZ || k == pZ;
            if (flag || flag1) {
               int l = pX + i * j;
               int i1 = pY + i * k;
               ChunkPos chunkpos = pSpreadPlacement.getPotentialStructureChunk(pSeed, l, i1);
               Pair<BlockPos, Holder<Structure>> pair = getStructureGeneratingAt(pStructureHoldersSet, pLevel, pStructureManager, pSkipKnownStructures, pSpreadPlacement, chunkpos);
               if (pair != null) {
                  return pair;
               }
            }
         }
      }

      return null;
   }

   @Nullable
   private static Pair<BlockPos, Holder<Structure>> getStructureGeneratingAt(Set<Holder<Structure>> pStructureHoldersSet, LevelReader pLevel, StructureManager pStructureManager, boolean pSkipKnownStructures, StructurePlacement pPlacement, ChunkPos pChunkPos) {
      for(Holder<Structure> holder : pStructureHoldersSet) {
         StructureCheckResult structurecheckresult = pStructureManager.checkStructurePresence(pChunkPos, holder.value(), pSkipKnownStructures);
         if (structurecheckresult != StructureCheckResult.START_NOT_PRESENT) {
            if (!pSkipKnownStructures && structurecheckresult == StructureCheckResult.START_PRESENT) {
               return Pair.of(pPlacement.getLocatePos(pChunkPos), holder);
            }

            ChunkAccess chunkaccess = pLevel.getChunk(pChunkPos.x, pChunkPos.z, ChunkStatus.STRUCTURE_STARTS);
            StructureStart structurestart = pStructureManager.getStartForStructure(SectionPos.bottomOf(chunkaccess), holder.value(), chunkaccess);
            if (structurestart != null && structurestart.isValid() && (!pSkipKnownStructures || tryAddReference(pStructureManager, structurestart))) {
               return Pair.of(pPlacement.getLocatePos(structurestart.getChunkPos()), holder);
            }
         }
      }

      return null;
   }

   private static boolean tryAddReference(StructureManager pStructureManager, StructureStart pStructureStrart) {
      if (pStructureStrart.canBeReferenced()) {
         pStructureManager.addReference(pStructureStrart);
         return true;
      } else {
         return false;
      }
   }

   public void applyBiomeDecoration(WorldGenLevel pLevel, ChunkAccess pChunk, StructureManager pStructureManager) {
      ChunkPos chunkpos = pChunk.getPos();
      if (!SharedConstants.debugVoidTerrain(chunkpos)) {
         SectionPos sectionpos = SectionPos.of(chunkpos, pLevel.getMinSection());
         BlockPos blockpos = sectionpos.origin();
         Registry<Structure> registry = pLevel.registryAccess().registryOrThrow(Registry.STRUCTURE_REGISTRY);
         Map<Integer, List<Structure>> map = registry.stream().collect(Collectors.groupingBy((p_223103_) -> {
            return p_223103_.step().ordinal();
         }));
         List<FeatureSorter.StepFeatureData> list = this.featuresPerStep.get();
         WorldgenRandom worldgenrandom = new WorldgenRandom(new XoroshiroRandomSource(RandomSupport.generateUniqueSeed()));
         long i = worldgenrandom.setDecorationSeed(pLevel.getSeed(), blockpos.getX(), blockpos.getZ());
         Set<Holder<Biome>> set = new ObjectArraySet<>();
         ChunkPos.rangeClosed(sectionpos.chunk(), 1).forEach((p_223093_) -> {
            ChunkAccess chunkaccess = pLevel.getChunk(p_223093_.x, p_223093_.z);

            for(LevelChunkSection levelchunksection : chunkaccess.getSections()) {
               levelchunksection.getBiomes().getAll(set::add);
            }

         });
         set.retainAll(this.biomeSource.possibleBiomes());
         int j = list.size();

         try {
            Registry<PlacedFeature> registry1 = pLevel.registryAccess().registryOrThrow(Registry.PLACED_FEATURE_REGISTRY);
            int i1 = Math.max(GenerationStep.Decoration.values().length, j);

            for(int k = 0; k < i1; ++k) {
               int l = 0;
               if (pStructureManager.shouldGenerateStructures()) {
                  for(Structure structure : map.getOrDefault(k, Collections.emptyList())) {
                     worldgenrandom.setFeatureSeed(i, l, k);
                     Supplier<String> supplier = () -> {
                        return registry.getResourceKey(structure).map(Object::toString).orElseGet(structure::toString);
                     };

                     try {
                        pLevel.setCurrentlyGenerating(supplier);
                        pStructureManager.startsForStructure(sectionpos, structure).forEach((p_223086_) -> {
                           p_223086_.placeInChunk(pLevel, pStructureManager, this, worldgenrandom, getWritableArea(pChunk), chunkpos);
                        });
                     } catch (Exception exception) {
                        CrashReport crashreport1 = CrashReport.forThrowable(exception, "Feature placement");
                        crashreport1.addCategory("Feature").setDetail("Description", supplier::get);
                        throw new ReportedException(crashreport1);
                     }

                     ++l;
                  }
               }

               if (k < j) {
                  IntSet intset = new IntArraySet();

                  for(Holder<Biome> holder : set) {
                     List<HolderSet<PlacedFeature>> list1 = this.generationSettingsGetter.apply(holder).features();
                     if (k < list1.size()) {
                        HolderSet<PlacedFeature> holderset = list1.get(k);
                        FeatureSorter.StepFeatureData featuresorter$stepfeaturedata1 = list.get(k);
                        holderset.stream().map(Holder::value).forEach((p_223174_) -> {
                           intset.add(featuresorter$stepfeaturedata1.indexMapping().applyAsInt(p_223174_));
                        });
                     }
                  }

                  int j1 = intset.size();
                  int[] aint = intset.toIntArray();
                  Arrays.sort(aint);
                  FeatureSorter.StepFeatureData featuresorter$stepfeaturedata = list.get(k);

                  for(int k1 = 0; k1 < j1; ++k1) {
                     int l1 = aint[k1];
                     PlacedFeature placedfeature = featuresorter$stepfeaturedata.features().get(l1);
                     Supplier<String> supplier1 = () -> {
                        return registry1.getResourceKey(placedfeature).map(Object::toString).orElseGet(placedfeature::toString);
                     };
                     worldgenrandom.setFeatureSeed(i, l1, k);

                     try {
                        pLevel.setCurrentlyGenerating(supplier1);
                        placedfeature.placeWithBiomeCheck(pLevel, this, worldgenrandom, blockpos);
                     } catch (Exception exception1) {
                        CrashReport crashreport2 = CrashReport.forThrowable(exception1, "Feature placement");
                        crashreport2.addCategory("Feature").setDetail("Description", supplier1::get);
                        throw new ReportedException(crashreport2);
                     }
                  }
               }
            }

            pLevel.setCurrentlyGenerating((Supplier<String>)null);
         } catch (Exception exception2) {
            CrashReport crashreport = CrashReport.forThrowable(exception2, "Biome decoration");
            crashreport.addCategory("Generation").setDetail("CenterX", chunkpos.x).setDetail("CenterZ", chunkpos.z).setDetail("Seed", i);
            throw new ReportedException(crashreport);
         }
      }
   }

   public boolean hasStructureChunkInRange(Holder<StructureSet> pStructureSet, RandomState pRandom, long pSeed, int pX, int pZ, int pOffset) {
      StructureSet structureset = pStructureSet.value();
      if (structureset == null) {
         return false;
      } else {
         StructurePlacement structureplacement = structureset.placement();

         for(int i = pX - pOffset; i <= pX + pOffset; ++i) {
            for(int j = pZ - pOffset; j <= pZ + pOffset; ++j) {
               if (structureplacement.isStructureChunk(this, pRandom, pSeed, i, j)) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   private static BoundingBox getWritableArea(ChunkAccess pChunk) {
      ChunkPos chunkpos = pChunk.getPos();
      int i = chunkpos.getMinBlockX();
      int j = chunkpos.getMinBlockZ();
      LevelHeightAccessor levelheightaccessor = pChunk.getHeightAccessorForGeneration();
      int k = levelheightaccessor.getMinBuildHeight() + 1;
      int l = levelheightaccessor.getMaxBuildHeight() - 1;
      return new BoundingBox(i, k, j, i + 15, l, j + 15);
   }

   public abstract void buildSurface(WorldGenRegion pLevel, StructureManager pStructureManager, RandomState pRandom, ChunkAccess pChunk);

   public abstract void spawnOriginalMobs(WorldGenRegion pLevel);

   public int getSpawnHeight(LevelHeightAccessor pLevel) {
      return 64;
   }

   public BiomeSource getBiomeSource() {
      return this.biomeSource;
   }

   public abstract int getGenDepth();

   public WeightedRandomList<MobSpawnSettings.SpawnerData> getMobsAt(Holder<Biome> pBiome, StructureManager pStructureManager, MobCategory pCategory, BlockPos pPos) {
      Map<Structure, LongSet> map = pStructureManager.getAllStructuresAt(pPos);

      for(Map.Entry<Structure, LongSet> entry : map.entrySet()) {
         Structure structure = entry.getKey();
         StructureSpawnOverride structurespawnoverride = structure.spawnOverrides().get(pCategory);
         if (structurespawnoverride != null) {
            MutableBoolean mutableboolean = new MutableBoolean(false);
            Predicate<StructureStart> predicate = structurespawnoverride.boundingBox() == StructureSpawnOverride.BoundingBoxType.PIECE ? (p_223065_) -> {
               return pStructureManager.structureHasPieceAt(pPos, p_223065_);
            } : (p_223130_) -> {
               return p_223130_.getBoundingBox().isInside(pPos);
            };
            pStructureManager.fillStartsForStructure(structure, entry.getValue(), (p_223220_) -> {
               if (mutableboolean.isFalse() && predicate.test(p_223220_)) {
                  mutableboolean.setTrue();
               }

            });
            if (mutableboolean.isTrue()) {
               return structurespawnoverride.spawns();
            }
         }
      }

      return pBiome.value().getMobSettings().getMobs(pCategory);
   }

   public void createStructures(RegistryAccess pRegistryAccess, RandomState pRandom, StructureManager pStructureManager, ChunkAccess pChunk, StructureTemplateManager pStructureTemplateManager, long pSeed) {
      ChunkPos chunkpos = pChunk.getPos();
      SectionPos sectionpos = SectionPos.bottomOf(pChunk);
      this.possibleStructureSets().forEach((p_223075_) -> {
         StructurePlacement structureplacement = p_223075_.value().placement();
         List<StructureSet.StructureSelectionEntry> list = p_223075_.value().structures();

         for(StructureSet.StructureSelectionEntry structureset$structureselectionentry : list) {
            StructureStart structurestart = pStructureManager.getStartForStructure(sectionpos, structureset$structureselectionentry.structure().value(), pChunk);
            if (structurestart != null && structurestart.isValid()) {
               return;
            }
         }

         if (structureplacement.isStructureChunk(this, pRandom, pSeed, chunkpos.x, chunkpos.z)) {
            if (list.size() == 1) {
               this.tryGenerateStructure(list.get(0), pStructureManager, pRegistryAccess, pRandom, pStructureTemplateManager, pSeed, pChunk, chunkpos, sectionpos);
            } else {
               ArrayList<StructureSet.StructureSelectionEntry> arraylist = new ArrayList<>(list.size());
               arraylist.addAll(list);
               WorldgenRandom worldgenrandom = new WorldgenRandom(new LegacyRandomSource(0L));
               worldgenrandom.setLargeFeatureSeed(pSeed, chunkpos.x, chunkpos.z);
               int i = 0;

               for(StructureSet.StructureSelectionEntry structureset$structureselectionentry1 : arraylist) {
                  i += structureset$structureselectionentry1.weight();
               }

               while(!arraylist.isEmpty()) {
                  int j = worldgenrandom.nextInt(i);
                  int k = 0;

                  for(StructureSet.StructureSelectionEntry structureset$structureselectionentry2 : arraylist) {
                     j -= structureset$structureselectionentry2.weight();
                     if (j < 0) {
                        break;
                     }

                     ++k;
                  }

                  StructureSet.StructureSelectionEntry structureset$structureselectionentry3 = arraylist.get(k);
                  if (this.tryGenerateStructure(structureset$structureselectionentry3, pStructureManager, pRegistryAccess, pRandom, pStructureTemplateManager, pSeed, pChunk, chunkpos, sectionpos)) {
                     return;
                  }

                  arraylist.remove(k);
                  i -= structureset$structureselectionentry3.weight();
               }

            }
         }
      });
   }

   private boolean tryGenerateStructure(StructureSet.StructureSelectionEntry pStructureSelectionEntry, StructureManager pStructureManager, RegistryAccess pRegistryAccess, RandomState pRandom, StructureTemplateManager pStructureTemplateManager, long pSeed, ChunkAccess pChunk, ChunkPos pChunkPos, SectionPos pSectionPos) {
      Structure structure = pStructureSelectionEntry.structure().value();
      int i = fetchReferences(pStructureManager, pChunk, pSectionPos, structure);
      HolderSet<Biome> holderset = structure.biomes();
      Predicate<Holder<Biome>> predicate = holderset::contains;
      StructureStart structurestart = structure.generate(pRegistryAccess, this, this.biomeSource, pRandom, pStructureTemplateManager, pSeed, pChunkPos, i, pChunk, predicate);
      if (structurestart.isValid()) {
         pStructureManager.setStartForStructure(pSectionPos, structure, structurestart, pChunk);
         return true;
      } else {
         return false;
      }
   }

   private static int fetchReferences(StructureManager pStructureManager, ChunkAccess pChunk, SectionPos pSectionPos, Structure pStructure) {
      StructureStart structurestart = pStructureManager.getStartForStructure(pSectionPos, pStructure, pChunk);
      return structurestart != null ? structurestart.getReferences() : 0;
   }

   public void createReferences(WorldGenLevel pLevel, StructureManager pStructureManager, ChunkAccess pChunk) {
      int i = 8;
      ChunkPos chunkpos = pChunk.getPos();
      int j = chunkpos.x;
      int k = chunkpos.z;
      int l = chunkpos.getMinBlockX();
      int i1 = chunkpos.getMinBlockZ();
      SectionPos sectionpos = SectionPos.bottomOf(pChunk);

      for(int j1 = j - 8; j1 <= j + 8; ++j1) {
         for(int k1 = k - 8; k1 <= k + 8; ++k1) {
            long l1 = ChunkPos.asLong(j1, k1);

            for(StructureStart structurestart : pLevel.getChunk(j1, k1).getAllStarts().values()) {
               try {
                  if (structurestart.isValid() && structurestart.getBoundingBox().intersects(l, i1, l + 15, i1 + 15)) {
                     pStructureManager.addReferenceForStructure(sectionpos, structurestart.getStructure(), l1, pChunk);
                     DebugPackets.sendStructurePacket(pLevel, structurestart);
                  }
               } catch (Exception exception) {
                  CrashReport crashreport = CrashReport.forThrowable(exception, "Generating structure reference");
                  CrashReportCategory crashreportcategory = crashreport.addCategory("Structure");
                  Optional<? extends Registry<Structure>> optional = pLevel.registryAccess().registry(Registry.STRUCTURE_REGISTRY);
                  crashreportcategory.setDetail("Id", () -> {
                     return optional.map((p_223118_) -> {
                        return p_223118_.getKey(structurestart.getStructure()).toString();
                     }).orElse("UNKNOWN");
                  });
                  crashreportcategory.setDetail("Name", () -> {
                     return Registry.STRUCTURE_TYPES.getKey(structurestart.getStructure().type()).toString();
                  });
                  crashreportcategory.setDetail("Class", () -> {
                     return structurestart.getStructure().getClass().getCanonicalName();
                  });
                  throw new ReportedException(crashreport);
               }
            }
         }
      }

   }

   public abstract CompletableFuture<ChunkAccess> fillFromNoise(Executor pExecutor, Blender pBlender, RandomState pRandom, StructureManager pStructureManager, ChunkAccess pChunk);

   public abstract int getSeaLevel();

   public abstract int getMinY();

   public abstract int getBaseHeight(int pX, int pZ, Heightmap.Types pType, LevelHeightAccessor pLevel, RandomState pRandom);

   public abstract NoiseColumn getBaseColumn(int pX, int pZ, LevelHeightAccessor pHeight, RandomState pRandom);

   public int getFirstFreeHeight(int pX, int pZ, Heightmap.Types pType, LevelHeightAccessor pLevel, RandomState pRandom) {
      return this.getBaseHeight(pX, pZ, pType, pLevel, pRandom);
   }

   public int getFirstOccupiedHeight(int pX, int pZ, Heightmap.Types pTypes, LevelHeightAccessor pLevel, RandomState pRandom) {
      return this.getBaseHeight(pX, pZ, pTypes, pLevel, pRandom) - 1;
   }

   public void ensureStructuresGenerated(RandomState pRandom) {
      if (!this.hasGeneratedPositions) {
         this.generatePositions(pRandom);
         this.hasGeneratedPositions = true;
      }

   }

   @Nullable
   public List<ChunkPos> getRingPositionsFor(ConcentricRingsStructurePlacement pPlacement, RandomState pRandom) {
      this.ensureStructuresGenerated(pRandom);
      CompletableFuture<List<ChunkPos>> completablefuture = this.ringPositions.get(pPlacement);
      return completablefuture != null ? completablefuture.join() : null;
   }

   private List<StructurePlacement> getPlacementsForStructure(Holder<Structure> pStructure, RandomState pRandom) {
      this.ensureStructuresGenerated(pRandom);
      return this.placementsForStructure.getOrDefault(pStructure.value(), List.of());
   }

   public abstract void addDebugScreenInfo(List<String> pInfo, RandomState pRandom, BlockPos pPos);

   /** @deprecated */
   @Deprecated
   public BiomeGenerationSettings getBiomeGenerationSettings(Holder<Biome> pBiome) {
      return this.generationSettingsGetter.apply(pBiome);
   }
}