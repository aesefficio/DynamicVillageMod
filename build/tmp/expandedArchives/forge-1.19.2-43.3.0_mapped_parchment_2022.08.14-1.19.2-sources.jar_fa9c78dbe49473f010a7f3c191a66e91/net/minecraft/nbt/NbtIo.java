package net.minecraft.nbt;

import java.io.BufferedOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.util.FastBufferedInputStream;

public class NbtIo {
   public static CompoundTag readCompressed(File pFile) throws IOException {
      InputStream inputstream = new FileInputStream(pFile);

      CompoundTag compoundtag;
      try {
         compoundtag = readCompressed(inputstream);
      } catch (Throwable throwable1) {
         try {
            inputstream.close();
         } catch (Throwable throwable) {
            throwable1.addSuppressed(throwable);
         }

         throw throwable1;
      }

      inputstream.close();
      return compoundtag;
   }

   private static DataInputStream createDecompressorStream(InputStream pZippedStream) throws IOException {
      return new DataInputStream(new FastBufferedInputStream(new GZIPInputStream(pZippedStream)));
   }

   /**
    * Reads a compressed compound tag from a GNU zipped file.
    * @see #readCompressed(File)
    */
   public static CompoundTag readCompressed(InputStream pZippedStream) throws IOException {
      DataInputStream datainputstream = createDecompressorStream(pZippedStream);

      CompoundTag compoundtag;
      try {
         compoundtag = read(datainputstream, NbtAccounter.UNLIMITED);
      } catch (Throwable throwable1) {
         if (datainputstream != null) {
            try {
               datainputstream.close();
            } catch (Throwable throwable) {
               throwable1.addSuppressed(throwable);
            }
         }

         throw throwable1;
      }

      if (datainputstream != null) {
         datainputstream.close();
      }

      return compoundtag;
   }

   public static void parseCompressed(File pFile, StreamTagVisitor pVisitor) throws IOException {
      InputStream inputstream = new FileInputStream(pFile);

      try {
         parseCompressed(inputstream, pVisitor);
      } catch (Throwable throwable1) {
         try {
            inputstream.close();
         } catch (Throwable throwable) {
            throwable1.addSuppressed(throwable);
         }

         throw throwable1;
      }

      inputstream.close();
   }

   public static void parseCompressed(InputStream pZippedStream, StreamTagVisitor pVisitor) throws IOException {
      DataInputStream datainputstream = createDecompressorStream(pZippedStream);

      try {
         parse(datainputstream, pVisitor);
      } catch (Throwable throwable1) {
         if (datainputstream != null) {
            try {
               datainputstream.close();
            } catch (Throwable throwable) {
               throwable1.addSuppressed(throwable);
            }
         }

         throw throwable1;
      }

      if (datainputstream != null) {
         datainputstream.close();
      }

   }

   public static void writeCompressed(CompoundTag pCompoundTag, File pFile) throws IOException {
      OutputStream outputstream = new FileOutputStream(pFile);

      try {
         writeCompressed(pCompoundTag, outputstream);
      } catch (Throwable throwable1) {
         try {
            outputstream.close();
         } catch (Throwable throwable) {
            throwable1.addSuppressed(throwable);
         }

         throw throwable1;
      }

      outputstream.close();
   }

   /**
    * Writes and compresses a compound tag to a GNU zipped file.
    * @see #writeCompressed(CompoundTag, File)
    */
   public static void writeCompressed(CompoundTag pCompoundTag, OutputStream pOutputStream) throws IOException {
      DataOutputStream dataoutputstream = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(pOutputStream)));

      try {
         write(pCompoundTag, dataoutputstream);
      } catch (Throwable throwable1) {
         try {
            dataoutputstream.close();
         } catch (Throwable throwable) {
            throwable1.addSuppressed(throwable);
         }

         throw throwable1;
      }

      dataoutputstream.close();
   }

   public static void write(CompoundTag pCompoundTag, File pFile) throws IOException {
      FileOutputStream fileoutputstream = new FileOutputStream(pFile);

      try {
         DataOutputStream dataoutputstream = new DataOutputStream(fileoutputstream);

         try {
            write(pCompoundTag, dataoutputstream);
         } catch (Throwable throwable2) {
            try {
               dataoutputstream.close();
            } catch (Throwable throwable1) {
               throwable2.addSuppressed(throwable1);
            }

            throw throwable2;
         }

         dataoutputstream.close();
      } catch (Throwable throwable3) {
         try {
            fileoutputstream.close();
         } catch (Throwable throwable) {
            throwable3.addSuppressed(throwable);
         }

         throw throwable3;
      }

      fileoutputstream.close();
   }

   @Nullable
   public static CompoundTag read(File pFile) throws IOException {
      if (!pFile.exists()) {
         return null;
      } else {
         FileInputStream fileinputstream = new FileInputStream(pFile);

         CompoundTag compoundtag;
         try {
            DataInputStream datainputstream = new DataInputStream(fileinputstream);

            try {
               compoundtag = read(datainputstream, NbtAccounter.UNLIMITED);
            } catch (Throwable throwable2) {
               try {
                  datainputstream.close();
               } catch (Throwable throwable1) {
                  throwable2.addSuppressed(throwable1);
               }

               throw throwable2;
            }

            datainputstream.close();
         } catch (Throwable throwable3) {
            try {
               fileinputstream.close();
            } catch (Throwable throwable) {
               throwable3.addSuppressed(throwable);
            }

            throw throwable3;
         }

         fileinputstream.close();
         return compoundtag;
      }
   }

   /**
    * Reads a compound tag from a file. The size of the file can be infinite.
    */
   public static CompoundTag read(DataInput pInput) throws IOException {
      return read(pInput, NbtAccounter.UNLIMITED);
   }

   /**
    * Reads a compound tag from a file. The size of the file is limited by the {@code accounter}.
    * @throws RuntimeException if the size of the file is larger than the maximum amount of bytes specified by the
    * {@code accounter}
    */
   public static CompoundTag read(DataInput pInput, NbtAccounter pAccounter) throws IOException {
      Tag tag = readUnnamedTag(pInput, 0, pAccounter);
      if (tag instanceof CompoundTag) {
         return (CompoundTag)tag;
      } else {
         throw new IOException("Root tag must be a named compound tag");
      }
   }

   public static void write(CompoundTag pCompoundTag, DataOutput pOutput) throws IOException {
      writeUnnamedTag(pCompoundTag, pOutput);
   }

   public static void parse(DataInput pInput, StreamTagVisitor pVisitor) throws IOException {
      TagType<?> tagtype = TagTypes.getType(pInput.readByte());
      if (tagtype == EndTag.TYPE) {
         if (pVisitor.visitRootEntry(EndTag.TYPE) == StreamTagVisitor.ValueResult.CONTINUE) {
            pVisitor.visitEnd();
         }

      } else {
         switch (pVisitor.visitRootEntry(tagtype)) {
            case HALT:
            default:
               break;
            case BREAK:
               StringTag.skipString(pInput);
               tagtype.skip(pInput);
               break;
            case CONTINUE:
               StringTag.skipString(pInput);
               tagtype.parse(pInput, pVisitor);
         }

      }
   }

   public static void writeUnnamedTag(Tag pTag, DataOutput pOutput) throws IOException {
      pOutput.writeByte(pTag.getId());
      if (pTag.getId() != 0) {
         pOutput.writeUTF("");
         pTag.write(pOutput);
      }
   }

   private static Tag readUnnamedTag(DataInput pInput, int pDepth, NbtAccounter pAccounter) throws IOException {
      byte b0 = pInput.readByte();
      pAccounter.accountBits(8); // Forge: Count everything!
      if (b0 == 0) {
         return EndTag.INSTANCE;
      } else {
         pAccounter.readUTF(pInput.readUTF()); //Forge: Count this string.
         pAccounter.accountBits(32); //Forge: 4 extra bytes for the object allocation.

         try {
            return TagTypes.getType(b0).load(pInput, pDepth, pAccounter);
         } catch (IOException ioexception) {
            CrashReport crashreport = CrashReport.forThrowable(ioexception, "Loading NBT data");
            CrashReportCategory crashreportcategory = crashreport.addCategory("NBT Tag");
            crashreportcategory.setDetail("Tag type", b0);
            throw new ReportedException(crashreport);
         }
      }
   }
}
