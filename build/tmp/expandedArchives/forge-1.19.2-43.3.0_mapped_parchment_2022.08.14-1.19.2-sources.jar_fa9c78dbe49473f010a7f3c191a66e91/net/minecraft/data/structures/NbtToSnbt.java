package net.minecraft.data.structures;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.mojang.logging.LogUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.annotation.Nullable;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import org.slf4j.Logger;

public class NbtToSnbt implements DataProvider {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final DataGenerator generator;

   public NbtToSnbt(DataGenerator pGenerator) {
      this.generator = pGenerator;
   }

   public void run(CachedOutput pOutput) throws IOException {
      Path path = this.generator.getOutputFolder();

      for(Path path1 : this.generator.getInputFolders()) {
         Files.walk(path1).filter((p_126430_) -> {
            return p_126430_.toString().endsWith(".nbt");
         }).forEach((p_236390_) -> {
            convertStructure(pOutput, p_236390_, this.getName(path1, p_236390_), path);
         });
      }

   }

   /**
    * Gets a name for this provider, to use in logging.
    */
   public String getName() {
      return "NBT to SNBT";
   }

   /**
    * Gets the name of the given NBT file, based on its path and the input directory. The result does not have the
    * ".nbt" extension.
    */
   private String getName(Path pInputFolder, Path pFile) {
      String s = pInputFolder.relativize(pFile).toString().replaceAll("\\\\", "/");
      return s.substring(0, s.length() - ".nbt".length());
   }

   @Nullable
   public static Path convertStructure(CachedOutput pOutput, Path pSnbtPath, String pName, Path pDirectoryPath) {
      try {
         InputStream inputstream = Files.newInputStream(pSnbtPath);

         Path path1;
         try {
            Path path = pDirectoryPath.resolve(pName + ".snbt");
            writeSnbt(pOutput, path, NbtUtils.structureToSnbt(NbtIo.readCompressed(inputstream)));
            LOGGER.info("Converted {} from NBT to SNBT", (Object)pName);
            path1 = path;
         } catch (Throwable throwable1) {
            if (inputstream != null) {
               try {
                  inputstream.close();
               } catch (Throwable throwable) {
                  throwable1.addSuppressed(throwable);
               }
            }

            throw throwable1;
         }

         if (inputstream != null) {
            inputstream.close();
         }

         return path1;
      } catch (IOException ioexception) {
         LOGGER.error("Couldn't convert {} from NBT to SNBT at {}", pName, pSnbtPath, ioexception);
         return null;
      }
   }

   public static void writeSnbt(CachedOutput pOutput, Path pPath, String pContents) throws IOException {
      ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream();
      HashingOutputStream hashingoutputstream = new HashingOutputStream(Hashing.sha1(), bytearrayoutputstream);
      hashingoutputstream.write(pContents.getBytes(StandardCharsets.UTF_8));
      hashingoutputstream.write(10);
      pOutput.writeIfNeeded(pPath, bytearrayoutputstream.toByteArray(), hashingoutputstream.hash());
   }
}