package net.minecraft.data;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import net.minecraft.WorldVersion;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import org.slf4j.Logger;

public class DataGenerator {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Collection<Path> inputFolders;
   private final Path outputFolder;
   private final List<DataProvider> allProviders = Lists.newArrayList();
   private final List<DataProvider> providersToRun = Lists.newArrayList();
   private final WorldVersion version;
   private final boolean alwaysGenerate;
   private final List<DataProvider> providerView = java.util.Collections.unmodifiableList(allProviders);

   public DataGenerator(Path pOutputFolder, Collection<Path> pInputFolders, WorldVersion pVersion, boolean pAlwaysGenerate) {
      this.outputFolder = pOutputFolder;
      this.version = pVersion;
      this.alwaysGenerate = pAlwaysGenerate;
      this.inputFolders = Lists.newArrayList(pInputFolders);
   }

   /**
    * Gets a collection of folders to look for data to convert in
    */
   public Collection<Path> getInputFolders() {
      return this.inputFolders;
   }

   /**
    * Gets the location to put generated data into
    */
   public Path getOutputFolder() {
      return this.outputFolder;
   }

   public Path getOutputFolder(DataGenerator.Target pTarget) {
      return this.getOutputFolder().resolve(pTarget.directory);
   }

   /**
    * Runs all the previously registered data providers.
    */
   public void run() throws IOException {
      HashCache hashcache = new HashCache(this.outputFolder, this.allProviders, this.version);
      Stopwatch stopwatch = Stopwatch.createStarted();
      Stopwatch stopwatch1 = Stopwatch.createUnstarted();

      for(DataProvider dataprovider : this.providersToRun) {
         if (!this.alwaysGenerate && !hashcache.shouldRunInThisVersion(dataprovider)) {
            LOGGER.debug("Generator {} already run for version {}", dataprovider.getName(), this.version.getName());
         } else {
            LOGGER.info("Starting provider: {}", (Object)dataprovider.getName());
            net.minecraftforge.fml.StartupMessageManager.addModMessage("Generating: " + dataprovider.getName());
            stopwatch1.start();
            dataprovider.run(hashcache.getUpdater(dataprovider));
            stopwatch1.stop();
            LOGGER.info("{} finished after {} ms", dataprovider.getName(), stopwatch1.elapsed(TimeUnit.MILLISECONDS));
            stopwatch1.reset();
         }
      }

      LOGGER.info("All providers took: {} ms", (long)stopwatch.elapsed(TimeUnit.MILLISECONDS));
      hashcache.purgeStaleAndWrite();
   }

   public void addProvider(boolean pRun, DataProvider pProvider) {
      if (pRun) {
         this.providersToRun.add(pProvider);
      }

      this.allProviders.add(pProvider);
   }

   public DataGenerator.PathProvider createPathProvider(DataGenerator.Target pTarget, String pKind) {
      return new DataGenerator.PathProvider(this, pTarget, pKind);
   }

   public List<DataProvider> getProviders() {
       return this.providerView;
   }

   public void addInput(Path value) {
      this.inputFolders.add(value);
   }

   static {
      Bootstrap.bootStrap();
   }

   public static class PathProvider {
      private final Path root;
      private final String kind;

      PathProvider(DataGenerator pGenerator, DataGenerator.Target pTarget, String pKind) {
         this.root = pGenerator.getOutputFolder(pTarget);
         this.kind = pKind;
      }

      public Path file(ResourceLocation pLocation, String pExtension) {
         return this.root.resolve(pLocation.getNamespace()).resolve(this.kind).resolve(pLocation.getPath() + "." + pExtension);
      }

      public Path json(ResourceLocation pLocation) {
         return this.root.resolve(pLocation.getNamespace()).resolve(this.kind).resolve(pLocation.getPath() + ".json");
      }
   }

   public static enum Target {
      DATA_PACK("data"),
      RESOURCE_PACK("assets"),
      REPORTS("reports");

      final String directory;

      private Target(String pDirectory) {
         this.directory = pDirectory;
      }
   }
}
