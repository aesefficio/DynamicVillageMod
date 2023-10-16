package net.minecraft.world.level.chunk;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import net.minecraft.util.profiling.jfr.callback.ProfiledDuration;
import net.minecraft.world.level.levelgen.BelowZeroRetrogen;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * The statuses that chunks go through during different phases of generation and loading.
 * Each status has an asynchronous task that is completed to generate a chunk, and one to load a chunk up to that
 * status.
 * Chunks are generated in sequential stages, some of which rely on nearby chunks from the previous stage. To this
 * respect, tasks define a "range" that they require chunks to be generated up to the previous stage. This is
 * responsible for the concentric squares seen in the chunk loading screen.
 */
public class ChunkStatus {
   public static final int MAX_STRUCTURE_DISTANCE = 8;
   private static final EnumSet<Heightmap.Types> PRE_FEATURES = EnumSet.of(Heightmap.Types.OCEAN_FLOOR_WG, Heightmap.Types.WORLD_SURFACE_WG);
   public static final EnumSet<Heightmap.Types> POST_FEATURES = EnumSet.of(Heightmap.Types.OCEAN_FLOOR, Heightmap.Types.WORLD_SURFACE, Heightmap.Types.MOTION_BLOCKING, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES);
   private static final ChunkStatus.LoadingTask PASSTHROUGH_LOAD_TASK = (p_223343_, p_223344_, p_223345_, p_223346_, p_223347_, p_223348_) -> {
      if (p_223348_ instanceof ProtoChunk protochunk) {
         if (!p_223348_.getStatus().isOrAfter(p_223343_)) {
            protochunk.setStatus(p_223343_);
         }
      }

      return CompletableFuture.completedFuture(Either.left(p_223348_));
   };
   public static final ChunkStatus EMPTY = registerSimple("empty", (ChunkStatus)null, -1, PRE_FEATURES, ChunkStatus.ChunkType.PROTOCHUNK, (p_156307_, p_156308_, p_156309_, p_156310_, p_156311_) -> {
   });
   public static final ChunkStatus STRUCTURE_STARTS = register("structure_starts", EMPTY, 0, PRE_FEATURES, ChunkStatus.ChunkType.PROTOCHUNK, (p_223361_, p_223362_, p_223363_, p_223364_, p_223365_, p_223366_, p_223367_, p_223368_, p_223369_, p_223370_) -> {
      if (!p_223369_.getStatus().isOrAfter(p_223361_)) {
         if (p_223363_.getServer().getWorldData().worldGenSettings().generateStructures()) {
            p_223364_.createStructures(p_223363_.registryAccess(), p_223363_.getChunkSource().randomState(), p_223363_.structureManager(), p_223369_, p_223365_, p_223363_.getSeed());
         }

         if (p_223369_ instanceof ProtoChunk) {
            ProtoChunk protochunk = (ProtoChunk)p_223369_;
            protochunk.setStatus(p_223361_);
         }

         p_223363_.onStructureStartsAvailable(p_223369_);
      }

      return CompletableFuture.completedFuture(Either.left(p_223369_));
   }, (p_223325_, p_223326_, p_223327_, p_223328_, p_223329_, p_223330_) -> {
      if (!p_223330_.getStatus().isOrAfter(p_223325_)) {
         if (p_223330_ instanceof ProtoChunk) {
            ProtoChunk protochunk = (ProtoChunk)p_223330_;
            protochunk.setStatus(p_223325_);
         }

         p_223326_.onStructureStartsAvailable(p_223330_);
      }

      return CompletableFuture.completedFuture(Either.left(p_223330_));
   });
   public static final ChunkStatus STRUCTURE_REFERENCES = registerSimple("structure_references", STRUCTURE_STARTS, 8, PRE_FEATURES, ChunkStatus.ChunkType.PROTOCHUNK, (p_196843_, p_196844_, p_196845_, p_196846_, p_196847_) -> {
      WorldGenRegion worldgenregion = new WorldGenRegion(p_196844_, p_196846_, p_196843_, -1);
      p_196845_.createReferences(worldgenregion, p_196844_.structureManager().forWorldGenRegion(worldgenregion), p_196847_);
   });
   public static final ChunkStatus BIOMES = register("biomes", STRUCTURE_REFERENCES, 8, PRE_FEATURES, ChunkStatus.ChunkType.PROTOCHUNK, (p_223350_, p_223351_, p_223352_, p_223353_, p_223354_, p_223355_, p_223356_, p_223357_, p_223358_, p_223359_) -> {
      if (!p_223359_ && p_223358_.getStatus().isOrAfter(p_223350_)) {
         return CompletableFuture.completedFuture(Either.left(p_223358_));
      } else {
         WorldGenRegion worldgenregion = new WorldGenRegion(p_223352_, p_223357_, p_223350_, -1);
         return p_223353_.createBiomes(p_223352_.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY), p_223351_, p_223352_.getChunkSource().randomState(), Blender.of(worldgenregion), p_223352_.structureManager().forWorldGenRegion(worldgenregion), p_223358_).thenApply((p_196819_) -> {
            if (p_196819_ instanceof ProtoChunk) {
               ((ProtoChunk)p_196819_).setStatus(p_223350_);
            }

            return Either.left(p_196819_);
         });
      }
   });
   public static final ChunkStatus NOISE = register("noise", BIOMES, 8, PRE_FEATURES, ChunkStatus.ChunkType.PROTOCHUNK, (p_223332_, p_223333_, p_223334_, p_223335_, p_223336_, p_223337_, p_223338_, p_223339_, p_223340_, p_223341_) -> {
      if (!p_223341_ && p_223340_.getStatus().isOrAfter(p_223332_)) {
         return CompletableFuture.completedFuture(Either.left(p_223340_));
      } else {
         WorldGenRegion worldgenregion = new WorldGenRegion(p_223334_, p_223339_, p_223332_, 0);
         return p_223335_.fillFromNoise(p_223333_, Blender.of(worldgenregion), p_223334_.getChunkSource().randomState(), p_223334_.structureManager().forWorldGenRegion(worldgenregion), p_223340_).thenApply((p_196792_) -> {
            if (p_196792_ instanceof ProtoChunk protochunk) {
               BelowZeroRetrogen belowzeroretrogen = protochunk.getBelowZeroRetrogen();
               if (belowzeroretrogen != null) {
                  BelowZeroRetrogen.replaceOldBedrock(protochunk);
                  if (belowzeroretrogen.hasBedrockHoles()) {
                     belowzeroretrogen.applyBedrockMask(protochunk);
                  }
               }

               protochunk.setStatus(p_223332_);
            }

            return Either.left(p_196792_);
         });
      }
   });
   public static final ChunkStatus SURFACE = registerSimple("surface", NOISE, 8, PRE_FEATURES, ChunkStatus.ChunkType.PROTOCHUNK, (p_156247_, p_156248_, p_156249_, p_156250_, p_156251_) -> {
      WorldGenRegion worldgenregion = new WorldGenRegion(p_156248_, p_156250_, p_156247_, 0);
      p_156249_.buildSurface(worldgenregion, p_156248_.structureManager().forWorldGenRegion(worldgenregion), p_156248_.getChunkSource().randomState(), p_156251_);
   });
   public static final ChunkStatus CARVERS = registerSimple("carvers", SURFACE, 8, PRE_FEATURES, ChunkStatus.ChunkType.PROTOCHUNK, (p_187819_, p_187820_, p_187821_, p_187822_, p_187823_) -> {
      WorldGenRegion worldgenregion = new WorldGenRegion(p_187820_, p_187822_, p_187819_, 0);
      if (p_187823_ instanceof ProtoChunk protochunk) {
         Blender.addAroundOldChunksCarvingMaskFilter(worldgenregion, protochunk);
      }

      p_187821_.applyCarvers(worldgenregion, p_187820_.getSeed(), p_187820_.getChunkSource().randomState(), p_187820_.getBiomeManager(), p_187820_.structureManager().forWorldGenRegion(worldgenregion), p_187823_, GenerationStep.Carving.AIR);
   });
   public static final ChunkStatus LIQUID_CARVERS = registerSimple("liquid_carvers", CARVERS, 8, POST_FEATURES, ChunkStatus.ChunkType.PROTOCHUNK, (p_196805_, p_196806_, p_196807_, p_196808_, p_196809_) -> {
   });
   public static final ChunkStatus FEATURES = register("features", LIQUID_CARVERS, 8, POST_FEATURES, ChunkStatus.ChunkType.PROTOCHUNK, (p_223314_, p_223315_, p_223316_, p_223317_, p_223318_, p_223319_, p_223320_, p_223321_, p_223322_, p_223323_) -> {
      ProtoChunk protochunk = (ProtoChunk)p_223322_;
      protochunk.setLightEngine(p_223319_);
      if (p_223323_ || !p_223322_.getStatus().isOrAfter(p_223314_)) {
         Heightmap.primeHeightmaps(p_223322_, EnumSet.of(Heightmap.Types.MOTION_BLOCKING, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Heightmap.Types.OCEAN_FLOOR, Heightmap.Types.WORLD_SURFACE));
         WorldGenRegion worldgenregion = new WorldGenRegion(p_223316_, p_223321_, p_223314_, 1);
         p_223317_.applyBiomeDecoration(worldgenregion, p_223322_, p_223316_.structureManager().forWorldGenRegion(worldgenregion));
         Blender.generateBorderTicks(worldgenregion, p_223322_);
         protochunk.setStatus(p_223314_);
      }

      return p_223319_.retainData(p_223322_).thenApply(Either::left);
   }, (p_223307_, p_223308_, p_223309_, p_223310_, p_223311_, p_223312_) -> {
      return p_223310_.retainData(p_223312_).thenApply(Either::left);
   });
   public static final ChunkStatus LIGHT = register("light", FEATURES, 1, POST_FEATURES, ChunkStatus.ChunkType.PROTOCHUNK, (p_223296_, p_223297_, p_223298_, p_223299_, p_223300_, p_223301_, p_223302_, p_223303_, p_223304_, p_223305_) -> {
      return lightChunk(p_223296_, p_223301_, p_223304_);
   }, (p_223289_, p_223290_, p_223291_, p_223292_, p_223293_, p_223294_) -> {
      return lightChunk(p_223289_, p_223292_, p_223294_);
   });
   public static final ChunkStatus SPAWN = registerSimple("spawn", LIGHT, 0, POST_FEATURES, ChunkStatus.ChunkType.PROTOCHUNK, (p_196758_, p_196759_, p_196760_, p_196761_, p_196762_) -> {
      if (!p_196762_.isUpgrading()) {
         p_196760_.spawnOriginalMobs(new WorldGenRegion(p_196759_, p_196761_, p_196758_, -1));
      }

   });
   public static final ChunkStatus HEIGHTMAPS = registerSimple("heightmaps", SPAWN, 0, POST_FEATURES, ChunkStatus.ChunkType.PROTOCHUNK, (p_223254_, p_223255_, p_223256_, p_223257_, p_223258_) -> {
   });
   public static final ChunkStatus FULL = register("full", HEIGHTMAPS, 0, POST_FEATURES, ChunkStatus.ChunkType.LEVELCHUNK, (p_223267_, p_223268_, p_223269_, p_223270_, p_223271_, p_223272_, p_223273_, p_223274_, p_223275_, p_223276_) -> {
      return p_223273_.apply(p_223275_);
   }, (p_223260_, p_223261_, p_223262_, p_223263_, p_223264_, p_223265_) -> {
      return p_223264_.apply(p_223265_);
   });
   private static final List<ChunkStatus> STATUS_BY_RANGE = ImmutableList.of(FULL, FEATURES, LIQUID_CARVERS, BIOMES, STRUCTURE_STARTS, STRUCTURE_STARTS, STRUCTURE_STARTS, STRUCTURE_STARTS, STRUCTURE_STARTS, STRUCTURE_STARTS, STRUCTURE_STARTS, STRUCTURE_STARTS);
   private static final IntList RANGE_BY_STATUS = Util.make(new IntArrayList(getStatusList().size()), (p_223278_) -> {
      int i = 0;

      for(int j = getStatusList().size() - 1; j >= 0; --j) {
         while(i + 1 < STATUS_BY_RANGE.size() && j <= STATUS_BY_RANGE.get(i + 1).getIndex()) {
            ++i;
         }

         p_223278_.add(0, i);
      }

   });
   private final String name;
   private final int index;
   private final ChunkStatus parent;
   private final ChunkStatus.GenerationTask generationTask;
   private final ChunkStatus.LoadingTask loadingTask;
   private final int range;
   private final ChunkStatus.ChunkType chunkType;
   private final EnumSet<Heightmap.Types> heightmapsAfter;

   private static CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> lightChunk(ChunkStatus pStatus, ThreadedLevelLightEngine pLightEngine, ChunkAccess pChunk) {
      boolean flag = isLighted(pStatus, pChunk);
      if (!pChunk.getStatus().isOrAfter(pStatus)) {
         ((ProtoChunk)pChunk).setStatus(pStatus);
      }

      return pLightEngine.lightChunk(pChunk, flag).thenApply(Either::left);
   }

   private static ChunkStatus registerSimple(String pKey, @Nullable ChunkStatus pParent, int pTaskRange, EnumSet<Heightmap.Types> pHeightmaps, ChunkStatus.ChunkType pType, ChunkStatus.SimpleGenerationTask pGenerationTask) {
      return register(pKey, pParent, pTaskRange, pHeightmaps, pType, pGenerationTask);
   }

   private static ChunkStatus register(String pKey, @Nullable ChunkStatus pParent, int pTaskRange, EnumSet<Heightmap.Types> pHeightmaps, ChunkStatus.ChunkType pType, ChunkStatus.GenerationTask pGenerationTask) {
      return register(pKey, pParent, pTaskRange, pHeightmaps, pType, pGenerationTask, PASSTHROUGH_LOAD_TASK);
   }

   private static ChunkStatus register(String pKey, @Nullable ChunkStatus pParent, int pTaskRange, EnumSet<Heightmap.Types> pHeightmaps, ChunkStatus.ChunkType pType, ChunkStatus.GenerationTask pGenerationTask, ChunkStatus.LoadingTask pLoadingTask) {
      return Registry.register(Registry.CHUNK_STATUS, pKey, new ChunkStatus(pKey, pParent, pTaskRange, pHeightmaps, pType, pGenerationTask, pLoadingTask));
   }

   public static List<ChunkStatus> getStatusList() {
      List<ChunkStatus> list = Lists.newArrayList();

      ChunkStatus chunkstatus;
      for(chunkstatus = FULL; chunkstatus.getParent() != chunkstatus; chunkstatus = chunkstatus.getParent()) {
         list.add(chunkstatus);
      }

      list.add(chunkstatus);
      Collections.reverse(list);
      return list;
   }

   private static boolean isLighted(ChunkStatus pStatus, ChunkAccess pChunk) {
      return pChunk.getStatus().isOrAfter(pStatus) && pChunk.isLightCorrect();
   }

   public static ChunkStatus getStatusAroundFullChunk(int pRadius) {
      if (pRadius >= STATUS_BY_RANGE.size()) {
         return EMPTY;
      } else {
         return pRadius < 0 ? FULL : STATUS_BY_RANGE.get(pRadius);
      }
   }

   public static int maxDistance() {
      return STATUS_BY_RANGE.size();
   }

   public static int getDistance(ChunkStatus pStatus) {
      return RANGE_BY_STATUS.getInt(pStatus.getIndex());
   }

   public ChunkStatus(String pName, @Nullable ChunkStatus pParent, int pRange, EnumSet<Heightmap.Types> pHeightmapsAfter, ChunkStatus.ChunkType pChunkType, ChunkStatus.GenerationTask pGenerationTask, ChunkStatus.LoadingTask pLoadingTask) {
      this.name = pName;
      this.parent = pParent == null ? this : pParent;
      this.generationTask = pGenerationTask;
      this.loadingTask = pLoadingTask;
      this.range = pRange;
      this.chunkType = pChunkType;
      this.heightmapsAfter = pHeightmapsAfter;
      this.index = pParent == null ? 0 : pParent.getIndex() + 1;
   }

   public int getIndex() {
      return this.index;
   }

   public String getName() {
      return this.name;
   }

   public ChunkStatus getParent() {
      return this.parent;
   }

   public CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> generate(Executor pExecutor, ServerLevel pLevel, ChunkGenerator pGenerator, StructureTemplateManager pStructureTemplateManager, ThreadedLevelLightEngine pLightEngine, Function<ChunkAccess, CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> pTask, List<ChunkAccess> pNeighboringChunks, boolean p_223287_) {
      ChunkAccess chunkaccess = pNeighboringChunks.get(pNeighboringChunks.size() / 2);
      ProfiledDuration profiledduration = JvmProfiler.INSTANCE.onChunkGenerate(chunkaccess.getPos(), pLevel.dimension(), this.name);
      CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> completablefuture = this.generationTask.doWork(this, pExecutor, pLevel, pGenerator, pStructureTemplateManager, pLightEngine, pTask, pNeighboringChunks, chunkaccess, p_223287_);
      return profiledduration != null ? completablefuture.thenApply((p_223252_) -> {
         profiledduration.finish();
         return p_223252_;
      }) : completablefuture;
   }

   public CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> load(ServerLevel pLevel, StructureTemplateManager pStructureTemplateManager, ThreadedLevelLightEngine pLightEngine, Function<ChunkAccess, CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> pTask, ChunkAccess pLoadingChunk) {
      return this.loadingTask.doWork(this, pLevel, pStructureTemplateManager, pLightEngine, pTask, pLoadingChunk);
   }

   /**
    * Distance in chunks between the edge of the center chunk and the edge of the chunk region needed for the task. The
    * task will only affect the center chunk, only reading from the chunks in the margin.
    */
   public int getRange() {
      return this.range;
   }

   public ChunkStatus.ChunkType getChunkType() {
      return this.chunkType;
   }

   public static ChunkStatus byName(String pKey) {
      return Registry.CHUNK_STATUS.get(ResourceLocation.tryParse(pKey));
   }

   public EnumSet<Heightmap.Types> heightmapsAfter() {
      return this.heightmapsAfter;
   }

   public boolean isOrAfter(ChunkStatus pStatus) {
      return this.getIndex() >= pStatus.getIndex();
   }

   public String toString() {
      return Registry.CHUNK_STATUS.getKey(this).toString();
   }

   public static enum ChunkType {
      PROTOCHUNK,
      LEVELCHUNK;
   }

   interface GenerationTask {
      CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> doWork(ChunkStatus pStatus, Executor pExecutor, ServerLevel pLevel, ChunkGenerator pGenerator, StructureTemplateManager pStructureTemplateManager, ThreadedLevelLightEngine pLightEngine, Function<ChunkAccess, CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> pTask, List<ChunkAccess> pNeighboringChunks, ChunkAccess pChunk, boolean p_223380_);
   }

   interface LoadingTask {
      CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> doWork(ChunkStatus pStatus, ServerLevel pLevel, StructureTemplateManager pStructureTemplateManager, ThreadedLevelLightEngine pLightEngine, Function<ChunkAccess, CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> pTask, ChunkAccess pChunk);
   }

   /**
    * A {@link GenerationTask} which completes all work synchronously.
    */
   interface SimpleGenerationTask extends ChunkStatus.GenerationTask {
      default CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> doWork(ChunkStatus pStatus, Executor pExecutor, ServerLevel pLevel, ChunkGenerator pGenerator, StructureTemplateManager pStructureTemplateManager, ThreadedLevelLightEngine pLightEngine, Function<ChunkAccess, CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> pTask, List<ChunkAccess> pNeighboringChunks, ChunkAccess pChunk, boolean p_223398_) {
         if (p_223398_ || !pChunk.getStatus().isOrAfter(pStatus)) {
            this.doWork(pStatus, pLevel, pGenerator, pNeighboringChunks, pChunk);
            if (pChunk instanceof ProtoChunk) {
               ProtoChunk protochunk = (ProtoChunk)pChunk;
               protochunk.setStatus(pStatus);
            }
         }

         return CompletableFuture.completedFuture(Either.left(pChunk));
      }

      void doWork(ChunkStatus pStatus, ServerLevel pLevel, ChunkGenerator pGenerator, List<ChunkAccess> pCache, ChunkAccess pLoadingChunk);
   }
}