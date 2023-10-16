package net.minecraft.world.level.storage;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.annotation.Nullable;
import net.minecraft.FileUtil;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.visitors.FieldSelector;
import net.minecraft.nbt.visitors.SkipFields;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.DirectoryLock;
import net.minecraft.util.MemoryReserve;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.slf4j.Logger;

public class LevelStorageSource {
   static final Logger LOGGER = LogUtils.getLogger();
   static final DateTimeFormatter FORMATTER = (new DateTimeFormatterBuilder()).appendValue(ChronoField.YEAR, 4, 10, SignStyle.EXCEEDS_PAD).appendLiteral('-').appendValue(ChronoField.MONTH_OF_YEAR, 2).appendLiteral('-').appendValue(ChronoField.DAY_OF_MONTH, 2).appendLiteral('_').appendValue(ChronoField.HOUR_OF_DAY, 2).appendLiteral('-').appendValue(ChronoField.MINUTE_OF_HOUR, 2).appendLiteral('-').appendValue(ChronoField.SECOND_OF_MINUTE, 2).toFormatter();
   private static final ImmutableList<String> OLD_SETTINGS_KEYS = ImmutableList.of("RandomSeed", "generatorName", "generatorOptions", "generatorVersion", "legacy_custom_options", "MapFeatures", "BonusChest");
   private static final String TAG_DATA = "Data";
   final Path baseDir;
   private final Path backupDir;
   final DataFixer fixerUpper;

   public LevelStorageSource(Path pBaseDir, Path pBackupDir, DataFixer pFixerUpper) {
      this.fixerUpper = pFixerUpper;

      try {
         Files.createDirectories(Files.exists(pBaseDir) ? pBaseDir.toRealPath() : pBaseDir);
      } catch (IOException ioexception) {
         throw new RuntimeException(ioexception);
      }

      this.baseDir = pBaseDir;
      this.backupDir = pBackupDir;
   }

   public static LevelStorageSource createDefault(Path pSavesDir) {
      return new LevelStorageSource(pSavesDir, pSavesDir.resolve("../backups"), DataFixers.getDataFixer());
   }

   private static <T> Pair<WorldGenSettings, Lifecycle> readWorldGenSettings(Dynamic<T> pNbt, DataFixer pFixer, int pVersion) {
      Dynamic<T> dynamic = pNbt.get("WorldGenSettings").orElseEmptyMap();

      for(String s : OLD_SETTINGS_KEYS) {
         Optional<? extends Dynamic<?>> optional = pNbt.get(s).result();
         if (optional.isPresent()) {
            dynamic = dynamic.set(s, optional.get());
         }
      }

      Dynamic<T> dynamic1 = pFixer.update(References.WORLD_GEN_SETTINGS, dynamic, pVersion, SharedConstants.getCurrentVersion().getWorldVersion());
      // The above line is modified (via the patch in V2832), such that the data fixer upper will retain dimensions that don't exist in the schema, in the dynamic.
      // When we parse the WorldGenSettings below, in vanilla, this uses the same call paths that can cause things to register (such as dimension types).
      // However, since at this point all resource data will have been loaded, anything that *would* register now is going to error later, because it'll create a Holder<> reference, that is not bound to anything. This unbound reference will then throw an error when the registries are frozen.
      // So, we freeze the registries before this call to parse(). This prevents them from leaking any new unbound holders into the registry.
      // Attempting to register to a frozen registry throws an exception, which we resolve via a patch in RegistryLoader, causing it to return DataResult.error() instead, and prevent the faulty dimension from registering without aborting the parsing of the WorldGenSettings
      // Finally, the resultOrPartial() promotes only the dimensions which successfully parsed, and the LenientUnboundedMapCodec which is patched into the WorldGenSettings codec ensures that *only* the invalid dimensions will fail to parse (see MC-197860), and not discard other dimensions as well.
      if (pNbt.getOps() instanceof net.minecraft.resources.RegistryOps<T> ops) ops.registryAccess.ownedRegistries().forEach(e -> e.value().freeze());
      DataResult<WorldGenSettings> dataresult = WorldGenSettings.CODEC.parse(dynamic1);
      return Pair.of(dataresult.resultOrPartial(Util.prefix("WorldGenSettings: ", LOGGER::error)).orElseGet(() -> {
         RegistryAccess registryaccess = RegistryAccess.readFromDisk(dynamic1);
         return WorldPresets.createNormalWorldFromPreset(registryaccess);
      }), dataresult.lifecycle());
   }

   private static DataPackConfig readDataPackConfig(Dynamic<?> p_78203_) {
      return DataPackConfig.CODEC.parse(p_78203_).resultOrPartial(LOGGER::error).orElse(DataPackConfig.DEFAULT);
   }

   public String getName() {
      return "Anvil";
   }

   public LevelStorageSource.LevelCandidates findLevelCandidates() throws LevelStorageException {
      if (!Files.isDirectory(this.baseDir)) {
         throw new LevelStorageException(Component.translatable("selectWorld.load_folder_access"));
      } else {
         try {
            List<LevelStorageSource.LevelDirectory> list = Files.list(this.baseDir).filter((p_230839_) -> {
               return Files.isDirectory(p_230839_);
            }).map(LevelStorageSource.LevelDirectory::new).filter((p_230835_) -> {
               return Files.isRegularFile(p_230835_.dataFile()) || Files.isRegularFile(p_230835_.oldDataFile());
            }).toList();
            return new LevelStorageSource.LevelCandidates(list);
         } catch (IOException ioexception) {
            throw new LevelStorageException(Component.translatable("selectWorld.load_folder_access"));
         }
      }
   }

   public CompletableFuture<List<LevelSummary>> loadLevelSummaries(LevelStorageSource.LevelCandidates pCandidates) {
      List<CompletableFuture<LevelSummary>> list = new ArrayList<>(pCandidates.levels.size());

      for(LevelStorageSource.LevelDirectory levelstoragesource$leveldirectory : pCandidates.levels) {
         list.add(CompletableFuture.supplyAsync(() -> {
            boolean flag;
            try {
               flag = DirectoryLock.isLocked(levelstoragesource$leveldirectory.path());
            } catch (Exception exception) {
               LOGGER.warn("Failed to read {} lock", levelstoragesource$leveldirectory.path(), exception);
               return null;
            }

            try {
               LevelSummary levelsummary = this.readLevelData(levelstoragesource$leveldirectory, this.levelSummaryReader(levelstoragesource$leveldirectory, flag));
               return levelsummary != null ? levelsummary : null;
            } catch (OutOfMemoryError outofmemoryerror) {
               MemoryReserve.release();
               System.gc();
               LOGGER.error(LogUtils.FATAL_MARKER, "Ran out of memory trying to read summary of {}", (Object)levelstoragesource$leveldirectory.directoryName());
               throw outofmemoryerror;
            } catch (StackOverflowError stackoverflowerror) {
               LOGGER.error(LogUtils.FATAL_MARKER, "Ran out of stack trying to read summary of {}. Assuming corruption; attempting to restore from from level.dat_old.", (Object)levelstoragesource$leveldirectory.directoryName());
               Util.safeReplaceOrMoveFile(levelstoragesource$leveldirectory.dataFile(), levelstoragesource$leveldirectory.oldDataFile(), levelstoragesource$leveldirectory.corruptedDataFile(LocalDateTime.now()), true);
               throw stackoverflowerror;
            }
         }, Util.backgroundExecutor()));
      }

      return Util.sequenceFailFastAndCancel(list).thenApply((p_230832_) -> {
         return p_230832_.stream().filter(Objects::nonNull).sorted().toList();
      });
   }

   private int getStorageVersion() {
      return 19133;
   }

   @Nullable
   <T> T readLevelData(LevelStorageSource.LevelDirectory pLevelDirectory, BiFunction<Path, DataFixer, T> pLevelDatReader) {
      if (!Files.exists(pLevelDirectory.path())) {
         return (T)null;
      } else {
         Path path = pLevelDirectory.dataFile();
         if (Files.exists(path)) {
            T t = pLevelDatReader.apply(path, this.fixerUpper);
            if (t != null) {
               return t;
            }
         }

         path = pLevelDirectory.oldDataFile();
         return (T)(Files.exists(path) ? pLevelDatReader.apply(path, this.fixerUpper) : null);
      }
   }

   @Nullable
   private static DataPackConfig getDataPacks(Path pLevelDat, DataFixer pFixerUpper) {
      try {
         Tag tag = readLightweightData(pLevelDat);
         if (tag instanceof CompoundTag compoundtag) {
            CompoundTag compoundtag1 = compoundtag.getCompound("Data");
            int i = compoundtag1.contains("DataVersion", 99) ? compoundtag1.getInt("DataVersion") : -1;
            Dynamic<Tag> dynamic = pFixerUpper.update(DataFixTypes.LEVEL.getType(), new Dynamic<>(NbtOps.INSTANCE, compoundtag1), i, SharedConstants.getCurrentVersion().getWorldVersion());
            return dynamic.get("DataPacks").result().map(LevelStorageSource::readDataPackConfig).orElse(DataPackConfig.DEFAULT);
         }
      } catch (Exception exception) {
         LOGGER.error("Exception reading {}", pLevelDat, exception);
      }

      return null;
   }

   static BiFunction<Path, DataFixer, PrimaryLevelData> getLevelData(DynamicOps<Tag> pOps, DataPackConfig pDatapackConfig, Lifecycle pLifecycle) {
      return (p_230811_, p_230812_) -> {
         try {
            CompoundTag compoundtag = NbtIo.readCompressed(p_230811_.toFile());
            CompoundTag compoundtag1 = compoundtag.getCompound("Data");
            CompoundTag compoundtag2 = compoundtag1.contains("Player", 10) ? compoundtag1.getCompound("Player") : null;
            compoundtag1.remove("Player");
            int i = compoundtag1.contains("DataVersion", 99) ? compoundtag1.getInt("DataVersion") : -1;
            Dynamic<Tag> dynamic = p_230812_.update(DataFixTypes.LEVEL.getType(), new Dynamic<>(pOps, compoundtag1), i, SharedConstants.getCurrentVersion().getWorldVersion());
            Pair<WorldGenSettings, Lifecycle> pair = readWorldGenSettings(dynamic, p_230812_, i);
            LevelVersion levelversion = LevelVersion.parse(dynamic);
            LevelSettings levelsettings = LevelSettings.parse(dynamic, pDatapackConfig);
            Lifecycle lifecycle = pair.getSecond().add(pLifecycle);
            return PrimaryLevelData.parse(dynamic, p_230812_, i, compoundtag2, levelsettings, levelversion, pair.getFirst(), lifecycle);
         } catch (Exception exception) {
            LOGGER.error("Exception reading {}", p_230811_, exception);
            return null;
         }
      };
   }

   BiFunction<Path, DataFixer, LevelSummary> levelSummaryReader(LevelStorageSource.LevelDirectory pLevelDirectory, boolean pLocked) {
      return (p_230826_, p_230827_) -> {
         try {
            Tag tag = readLightweightData(p_230826_);
            if (tag instanceof CompoundTag compoundtag) {
               CompoundTag compoundtag1 = compoundtag.getCompound("Data");
               int i = compoundtag1.contains("DataVersion", 99) ? compoundtag1.getInt("DataVersion") : -1;
               Dynamic<Tag> dynamic = p_230827_.update(DataFixTypes.LEVEL.getType(), new Dynamic<>(NbtOps.INSTANCE, compoundtag1), i, SharedConstants.getCurrentVersion().getWorldVersion());
               LevelVersion levelversion = LevelVersion.parse(dynamic);
               int j = levelversion.levelDataVersion();
               if (j == 19132 || j == 19133) {
                  boolean flag = j != this.getStorageVersion();
                  Path path = pLevelDirectory.iconFile();
                  DataPackConfig datapackconfig = dynamic.get("DataPacks").result().map(LevelStorageSource::readDataPackConfig).orElse(DataPackConfig.DEFAULT);
                  LevelSettings levelsettings = LevelSettings.parse(dynamic, datapackconfig);
                  return new LevelSummary(levelsettings, levelversion, pLevelDirectory.directoryName(), flag, pLocked, path);
               }
            } else {
               LOGGER.warn("Invalid root tag in {}", (Object)p_230826_);
            }

            return null;
         } catch (Exception exception) {
            LOGGER.error("Exception reading {}", p_230826_, exception);
            return null;
         }
      };
   }

   @Nullable
   private static Tag readLightweightData(Path p_230837_) throws IOException {
      SkipFields skipfields = new SkipFields(new FieldSelector("Data", CompoundTag.TYPE, "Player"), new FieldSelector("Data", CompoundTag.TYPE, "WorldGenSettings"));
      NbtIo.parseCompressed(p_230837_.toFile(), skipfields);
      return skipfields.getResult();
   }

   public boolean isNewLevelIdAcceptable(String pSaveName) {
      try {
         Path path = this.baseDir.resolve(pSaveName);
         Files.createDirectory(path);
         Files.deleteIfExists(path);
         return true;
      } catch (IOException ioexception) {
         return false;
      }
   }

   /**
    * Return whether the given world can be loaded.
    */
   public boolean levelExists(String pSaveName) {
      return Files.isDirectory(this.baseDir.resolve(pSaveName));
   }

   public Path getBaseDir() {
      return this.baseDir;
   }

   /**
    * Gets the folder where backups are stored
    */
   public Path getBackupPath() {
      return this.backupDir;
   }

   public LevelStorageSource.LevelStorageAccess createAccess(String pSaveName) throws IOException {
      return new LevelStorageSource.LevelStorageAccess(pSaveName);
   }

   public static record LevelCandidates(List<LevelStorageSource.LevelDirectory> levels) implements Iterable<LevelStorageSource.LevelDirectory> {
      public boolean isEmpty() {
         return this.levels.isEmpty();
      }

      public Iterator<LevelStorageSource.LevelDirectory> iterator() {
         return this.levels.iterator();
      }
   }

   public static record LevelDirectory(Path path) {
      public String directoryName() {
         return this.path.getFileName().toString();
      }

      public Path dataFile() {
         return this.resourcePath(LevelResource.LEVEL_DATA_FILE);
      }

      public Path oldDataFile() {
         return this.resourcePath(LevelResource.OLD_LEVEL_DATA_FILE);
      }

      public Path corruptedDataFile(LocalDateTime p_230857_) {
         return this.path.resolve(LevelResource.LEVEL_DATA_FILE.getId() + "_corrupted_" + p_230857_.format(LevelStorageSource.FORMATTER));
      }

      public Path iconFile() {
         return this.resourcePath(LevelResource.ICON_FILE);
      }

      public Path lockFile() {
         return this.resourcePath(LevelResource.LOCK_FILE);
      }

      public Path resourcePath(LevelResource p_230855_) {
         return this.path.resolve(p_230855_.getId());
      }
   }

   public class LevelStorageAccess implements AutoCloseable {
      final DirectoryLock lock;
      final LevelStorageSource.LevelDirectory levelDirectory;
      private final String levelId;
      private final Map<LevelResource, Path> resources = Maps.newHashMap();

      public LevelStorageAccess(String pLevelId) throws IOException {
         this.levelId = pLevelId;
         this.levelDirectory = new LevelStorageSource.LevelDirectory(LevelStorageSource.this.baseDir.resolve(pLevelId));
         this.lock = DirectoryLock.create(this.levelDirectory.path());
      }

      public String getLevelId() {
         return this.levelId;
      }

      public Path getLevelPath(LevelResource pFolderName) {
         return this.resources.computeIfAbsent(pFolderName, this.levelDirectory::resourcePath);
      }

      public Path getDimensionPath(ResourceKey<Level> pDimensionPath) {
         return DimensionType.getStorageFolder(pDimensionPath, this.levelDirectory.path());
      }

      private void checkLock() {
         if (!this.lock.isValid()) {
            throw new IllegalStateException("Lock is no longer valid");
         }
      }

      public PlayerDataStorage createPlayerStorage() {
         this.checkLock();
         return new PlayerDataStorage(this, LevelStorageSource.this.fixerUpper);
      }

      @Nullable
      public LevelSummary getSummary() {
         this.checkLock();
         return LevelStorageSource.this.readLevelData(this.levelDirectory, LevelStorageSource.this.levelSummaryReader(this.levelDirectory, false));
      }

      @Nullable
      public WorldData getDataTag(DynamicOps<Tag> pOps, DataPackConfig pDataPackConfig, Lifecycle pLifecycle) {
         this.checkLock();
         return LevelStorageSource.this.readLevelData(this.levelDirectory, LevelStorageSource.getLevelData(pOps, pDataPackConfig, pLifecycle));
      }

      public void readAdditionalLevelSaveData() {
         checkLock();
         LevelStorageSource.this.readLevelData(this.levelDirectory, (path, dataFixer) -> {
            try {
               CompoundTag compoundTag = NbtIo.readCompressed(path.toFile());
               net.minecraftforge.common.ForgeHooks.readAdditionalLevelSaveData(compoundTag, this.levelDirectory);
            } catch (Exception e) {
                LOGGER.error("Exception reading {}", path, e);
            }
            return ""; // Return non-null to prevent level.dat-old inject
         });
      }

      @Nullable
      public DataPackConfig getDataPacks() {
         this.checkLock();
         return LevelStorageSource.this.readLevelData(this.levelDirectory, LevelStorageSource::getDataPacks);
      }

      public void saveDataTag(RegistryAccess pRegistries, WorldData pServerConfiguration) {
         this.saveDataTag(pRegistries, pServerConfiguration, (CompoundTag)null);
      }

      public void saveDataTag(RegistryAccess pRegistries, WorldData pServerConfiguration, @Nullable CompoundTag pHostPlayerNBT) {
         File file1 = this.levelDirectory.path().toFile();
         CompoundTag compoundtag = pServerConfiguration.createTag(pRegistries, pHostPlayerNBT);
         CompoundTag compoundtag1 = new CompoundTag();
         compoundtag1.put("Data", compoundtag);

         net.minecraftforge.common.ForgeHooks.writeAdditionalLevelSaveData(pServerConfiguration, compoundtag1);

         try {
            File file2 = File.createTempFile("level", ".dat", file1);
            NbtIo.writeCompressed(compoundtag1, file2);
            File file3 = this.levelDirectory.oldDataFile().toFile();
            File file4 = this.levelDirectory.dataFile().toFile();
            Util.safeReplaceFile(file4, file2, file3);
         } catch (Exception exception) {
            LevelStorageSource.LOGGER.error("Failed to save level {}", file1, exception);
         }

      }

      public Optional<Path> getIconFile() {
         return !this.lock.isValid() ? Optional.empty() : Optional.of(this.levelDirectory.iconFile());
      }

      public Path getWorldDir() {
         return baseDir;
      }

      public void deleteLevel() throws IOException {
         this.checkLock();
         final Path path = this.levelDirectory.lockFile();
         LevelStorageSource.LOGGER.info("Deleting level {}", (Object)this.levelId);

         for(int i = 1; i <= 5; ++i) {
            LevelStorageSource.LOGGER.info("Attempt {}...", (int)i);

            try {
               Files.walkFileTree(this.levelDirectory.path(), new SimpleFileVisitor<Path>() {
                  public FileVisitResult visitFile(Path p_78323_, BasicFileAttributes p_78324_) throws IOException {
                     if (!p_78323_.equals(path)) {
                        LevelStorageSource.LOGGER.debug("Deleting {}", (Object)p_78323_);
                        Files.delete(p_78323_);
                     }

                     return FileVisitResult.CONTINUE;
                  }

                  public FileVisitResult postVisitDirectory(Path p_78320_, IOException p_78321_) throws IOException {
                     if (p_78321_ != null) {
                        throw p_78321_;
                     } else {
                        if (p_78320_.equals(LevelStorageAccess.this.levelDirectory.path())) {
                           LevelStorageAccess.this.lock.close();
                           Files.deleteIfExists(path);
                        }

                        Files.delete(p_78320_);
                        return FileVisitResult.CONTINUE;
                     }
                  }
               });
               break;
            } catch (IOException ioexception) {
               if (i >= 5) {
                  throw ioexception;
               }

               LevelStorageSource.LOGGER.warn("Failed to delete {}", this.levelDirectory.path(), ioexception);

               try {
                  Thread.sleep(500L);
               } catch (InterruptedException interruptedexception) {
               }
            }
         }

      }

      public void renameLevel(String pSaveName) throws IOException {
         this.checkLock();
         Path path = this.levelDirectory.dataFile();
         if (Files.exists(path)) {
            CompoundTag compoundtag = NbtIo.readCompressed(path.toFile());
            CompoundTag compoundtag1 = compoundtag.getCompound("Data");
            compoundtag1.putString("LevelName", pSaveName);
            NbtIo.writeCompressed(compoundtag, path.toFile());
         }

      }

      public long makeWorldBackup() throws IOException {
         this.checkLock();
         String s = LocalDateTime.now().format(LevelStorageSource.FORMATTER) + "_" + this.levelId;
         Path path = LevelStorageSource.this.getBackupPath();

         try {
            Files.createDirectories(Files.exists(path) ? path.toRealPath() : path);
         } catch (IOException ioexception) {
            throw new RuntimeException(ioexception);
         }

         Path path1 = path.resolve(FileUtil.findAvailableName(path, s, ".zip"));
         final ZipOutputStream zipoutputstream = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(path1)));

         try {
            final Path path2 = Paths.get(this.levelId);
            Files.walkFileTree(this.levelDirectory.path(), new SimpleFileVisitor<Path>() {
               public FileVisitResult visitFile(Path p_78339_, BasicFileAttributes p_78340_) throws IOException {
                  if (p_78339_.endsWith("session.lock")) {
                     return FileVisitResult.CONTINUE;
                  } else {
                     String s1 = path2.resolve(LevelStorageAccess.this.levelDirectory.path().relativize(p_78339_)).toString().replace('\\', '/');
                     ZipEntry zipentry = new ZipEntry(s1);
                     zipoutputstream.putNextEntry(zipentry);
                     com.google.common.io.Files.asByteSource(p_78339_.toFile()).copyTo(zipoutputstream);
                     zipoutputstream.closeEntry();
                     return FileVisitResult.CONTINUE;
                  }
               }
            });
         } catch (Throwable throwable1) {
            try {
               zipoutputstream.close();
            } catch (Throwable throwable) {
               throwable1.addSuppressed(throwable);
            }

            throw throwable1;
         }

         zipoutputstream.close();
         return Files.size(path1);
      }

      public void close() throws IOException {
         this.lock.close();
      }
   }
}
