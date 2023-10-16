package net.minecraft.data.structures;

import com.google.common.collect.Lists;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

public class SnbtToNbt implements DataProvider {
   @Nullable
   private static final Path DUMP_SNBT_TO = null;
   private static final Logger LOGGER = LogUtils.getLogger();
   private final DataGenerator generator;
   private final List<SnbtToNbt.Filter> filters = Lists.newArrayList();

   public SnbtToNbt(DataGenerator pGenerator) {
      this.generator = pGenerator;
   }

   public SnbtToNbt addFilter(SnbtToNbt.Filter pFilter) {
      this.filters.add(pFilter);
      return this;
   }

   private CompoundTag applyFilters(String pFileName, CompoundTag pTag) {
      CompoundTag compoundtag = pTag;

      for(SnbtToNbt.Filter snbttonbt$filter : this.filters) {
         compoundtag = snbttonbt$filter.apply(pFileName, compoundtag);
      }

      return compoundtag;
   }

   public void run(CachedOutput pOutput) throws IOException {
      Path path = this.generator.getOutputFolder();
      List<CompletableFuture<SnbtToNbt.TaskResult>> list = Lists.newArrayList();

      for(Path path1 : this.generator.getInputFolders()) {
         Files.walk(path1).filter((p_126464_) -> {
            return p_126464_.toString().endsWith(".snbt");
         }).forEach((p_126474_) -> {
            list.add(CompletableFuture.supplyAsync(() -> {
               return this.readStructure(p_126474_, this.getName(path1, p_126474_));
            }, Util.backgroundExecutor()));
         });
      }

      boolean flag = false;

      for(CompletableFuture<SnbtToNbt.TaskResult> completablefuture : list) {
         try {
            this.storeStructureIfChanged(pOutput, completablefuture.get(), path);
         } catch (Exception exception) {
            LOGGER.error("Failed to process structure", (Throwable)exception);
            flag = true;
         }
      }

      if (flag) {
         throw new IllegalStateException("Failed to convert all structures, aborting");
      }
   }

   /**
    * Gets a name for this provider, to use in logging.
    */
   public String getName() {
      return "SNBT -> NBT";
   }

   /**
    * Gets the name of the given SNBT file, based on its path and the input directory. The result does not have the
    * ".snbt" extension.
    */
   private String getName(Path pInputFolder, Path pFile) {
      String s = pInputFolder.relativize(pFile).toString().replaceAll("\\\\", "/");
      return s.substring(0, s.length() - ".snbt".length());
   }

   private SnbtToNbt.TaskResult readStructure(Path pFilePath, String pFileName) {
      try {
         BufferedReader bufferedreader = Files.newBufferedReader(pFilePath);

         SnbtToNbt.TaskResult snbttonbt$taskresult;
         try {
            String s = IOUtils.toString((Reader)bufferedreader);
            CompoundTag compoundtag = this.applyFilters(pFileName, NbtUtils.snbtToStructure(s));
            ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream();
            HashingOutputStream hashingoutputstream = new HashingOutputStream(Hashing.sha1(), bytearrayoutputstream);
            NbtIo.writeCompressed(compoundtag, hashingoutputstream);
            byte[] abyte = bytearrayoutputstream.toByteArray();
            HashCode hashcode = hashingoutputstream.hash();
            String s1;
            if (DUMP_SNBT_TO != null) {
               s1 = NbtUtils.structureToSnbt(compoundtag);
            } else {
               s1 = null;
            }

            snbttonbt$taskresult = new SnbtToNbt.TaskResult(pFileName, abyte, s1, hashcode);
         } catch (Throwable throwable1) {
            if (bufferedreader != null) {
               try {
                  bufferedreader.close();
               } catch (Throwable throwable) {
                  throwable1.addSuppressed(throwable);
               }
            }

            throw throwable1;
         }

         if (bufferedreader != null) {
            bufferedreader.close();
         }

         return snbttonbt$taskresult;
      } catch (Throwable throwable2) {
         throw new SnbtToNbt.StructureConversionException(pFilePath, throwable2);
      }
   }

   private void storeStructureIfChanged(CachedOutput pOutput, SnbtToNbt.TaskResult pTaskResult, Path pDirectoryPath) {
      if (pTaskResult.snbtPayload != null) {
         Path path = DUMP_SNBT_TO.resolve(pTaskResult.name + ".snbt");

         try {
            NbtToSnbt.writeSnbt(CachedOutput.NO_CACHE, path, pTaskResult.snbtPayload);
         } catch (IOException ioexception1) {
            LOGGER.error("Couldn't write structure SNBT {} at {}", pTaskResult.name, path, ioexception1);
         }
      }

      Path path1 = pDirectoryPath.resolve(pTaskResult.name + ".nbt");

      try {
         pOutput.writeIfNeeded(path1, pTaskResult.payload, pTaskResult.hash);
      } catch (IOException ioexception) {
         LOGGER.error("Couldn't write structure {} at {}", pTaskResult.name, path1, ioexception);
      }

   }

   @FunctionalInterface
   public interface Filter {
      CompoundTag apply(String pStructureLocationPath, CompoundTag pTag);
   }

   /**
    * Wraps exceptions thrown while reading structures to include the path of the structure in the exception message.
    */
   static class StructureConversionException extends RuntimeException {
      public StructureConversionException(Path pPath, Throwable pCause) {
         super(pPath.toAbsolutePath().toString(), pCause);
      }
   }

   static record TaskResult(String name, byte[] payload, @Nullable String snbtPayload, HashCode hash) {
   }
}