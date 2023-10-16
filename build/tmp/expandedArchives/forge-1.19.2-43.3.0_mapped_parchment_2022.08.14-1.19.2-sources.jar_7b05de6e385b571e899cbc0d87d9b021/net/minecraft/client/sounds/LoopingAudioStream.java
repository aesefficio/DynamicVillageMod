package net.minecraft.client.sounds;

import java.io.BufferedInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import javax.sound.sampled.AudioFormat;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LoopingAudioStream implements AudioStream {
   private final LoopingAudioStream.AudioStreamProvider provider;
   private AudioStream stream;
   private final BufferedInputStream bufferedInputStream;

   public LoopingAudioStream(LoopingAudioStream.AudioStreamProvider pProvider, InputStream pInputStream) throws IOException {
      this.provider = pProvider;
      this.bufferedInputStream = new BufferedInputStream(pInputStream);
      this.bufferedInputStream.mark(Integer.MAX_VALUE);
      this.stream = pProvider.create(new LoopingAudioStream.NoCloseBuffer(this.bufferedInputStream));
   }

   public AudioFormat getFormat() {
      return this.stream.getFormat();
   }

   public ByteBuffer read(int pSize) throws IOException {
      ByteBuffer bytebuffer = this.stream.read(pSize);
      if (!bytebuffer.hasRemaining()) {
         this.stream.close();
         this.bufferedInputStream.reset();
         this.stream = this.provider.create(new LoopingAudioStream.NoCloseBuffer(this.bufferedInputStream));
         bytebuffer = this.stream.read(pSize);
      }

      return bytebuffer;
   }

   public void close() throws IOException {
      this.stream.close();
      this.bufferedInputStream.close();
   }

   @FunctionalInterface
   @OnlyIn(Dist.CLIENT)
   public interface AudioStreamProvider {
      AudioStream create(InputStream pInputStream) throws IOException;
   }

   @OnlyIn(Dist.CLIENT)
   static class NoCloseBuffer extends FilterInputStream {
      NoCloseBuffer(InputStream pInputStream) {
         super(pInputStream);
      }

      public void close() {
      }
   }
}