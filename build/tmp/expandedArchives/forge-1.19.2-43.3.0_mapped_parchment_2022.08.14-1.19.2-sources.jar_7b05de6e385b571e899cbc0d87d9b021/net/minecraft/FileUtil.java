package net.minecraft;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FilenameUtils;

public class FileUtil {
   private static final Pattern COPY_COUNTER_PATTERN = Pattern.compile("(<name>.*) \\((<count>\\d*)\\)", 66);
   private static final int MAX_FILE_NAME = 255;
   private static final Pattern RESERVED_WINDOWS_FILENAMES = Pattern.compile(".*\\.|(?:COM|CLOCK\\$|CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(?:\\..*)?", 2);

   public static String findAvailableName(Path pDirPath, String pFileName, String pFileFormat) throws IOException {
      for(char c0 : SharedConstants.ILLEGAL_FILE_CHARACTERS) {
         pFileName = pFileName.replace(c0, '_');
      }

      pFileName = pFileName.replaceAll("[./\"]", "_");
      if (RESERVED_WINDOWS_FILENAMES.matcher(pFileName).matches()) {
         pFileName = "_" + pFileName + "_";
      }

      Matcher matcher = COPY_COUNTER_PATTERN.matcher(pFileName);
      int j = 0;
      if (matcher.matches()) {
         pFileName = matcher.group("name");
         j = Integer.parseInt(matcher.group("count"));
      }

      if (pFileName.length() > 255 - pFileFormat.length()) {
         pFileName = pFileName.substring(0, 255 - pFileFormat.length());
      }

      while(true) {
         String s = pFileName;
         if (j != 0) {
            String s1 = " (" + j + ")";
            int i = 255 - s1.length();
            if (pFileName.length() > i) {
               s = pFileName.substring(0, i);
            }

            s = s + s1;
         }

         s = s + pFileFormat;
         Path path = pDirPath.resolve(s);

         try {
            Path path1 = Files.createDirectory(path);
            Files.deleteIfExists(path1);
            return pDirPath.relativize(path1).toString();
         } catch (FileAlreadyExistsException filealreadyexistsexception) {
            ++j;
         }
      }
   }

   public static boolean isPathNormalized(Path pPath) {
      Path path = pPath.normalize();
      return path.equals(pPath);
   }

   public static boolean isPathPortable(Path pPath) {
      for(Path path : pPath) {
         if (RESERVED_WINDOWS_FILENAMES.matcher(path.toString()).matches()) {
            return false;
         }
      }

      return true;
   }

   public static Path createPathToResource(Path pDirPath, String pLocationPath, String pFileFormat) {
      String s = pLocationPath + pFileFormat;
      Path path = Paths.get(s);
      if (path.endsWith(pFileFormat)) {
         throw new InvalidPathException(s, "empty resource name");
      } else {
         return pDirPath.resolve(path);
      }
   }

   public static String getFullResourcePath(String pPath) {
      return FilenameUtils.getFullPath(pPath).replace(File.separator, "/");
   }

   public static String normalizeResourcePath(String pPath) {
      return FilenameUtils.normalize(pPath).replace(File.separator, "/");
   }
}