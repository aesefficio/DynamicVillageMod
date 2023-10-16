package net.minecraft.client.sounds;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.audio.OggAudioStream;
import com.mojang.blaze3d.audio.SoundBuffer;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import net.minecraft.Util;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SoundBufferLibrary {
   private final ResourceManager resourceManager;
   private final Map<ResourceLocation, CompletableFuture<SoundBuffer>> cache = Maps.newHashMap();

   public SoundBufferLibrary(ResourceManager pResourceManager) {
      this.resourceManager = pResourceManager;
   }

   public CompletableFuture<SoundBuffer> getCompleteBuffer(ResourceLocation pSoundID) {
      return this.cache.computeIfAbsent(pSoundID, (p_120208_) -> {
         return CompletableFuture.supplyAsync(() -> {
            try {
               InputStream inputstream = this.resourceManager.open(p_120208_);

               SoundBuffer soundbuffer;
               try {
                  OggAudioStream oggaudiostream = new OggAudioStream(inputstream);

                  try {
                     ByteBuffer bytebuffer = oggaudiostream.readAll();
                     soundbuffer = new SoundBuffer(bytebuffer, oggaudiostream.getFormat());
                  } catch (Throwable throwable2) {
                     try {
                        oggaudiostream.close();
                     } catch (Throwable throwable1) {
                        throwable2.addSuppressed(throwable1);
                     }

                     throw throwable2;
                  }

                  oggaudiostream.close();
               } catch (Throwable throwable3) {
                  if (inputstream != null) {
                     try {
                        inputstream.close();
                     } catch (Throwable throwable) {
                        throwable3.addSuppressed(throwable);
                     }
                  }

                  throw throwable3;
               }

               if (inputstream != null) {
                  inputstream.close();
               }

               return soundbuffer;
            } catch (IOException ioexception) {
               throw new CompletionException(ioexception);
            }
         }, Util.backgroundExecutor());
      });
   }

   public CompletableFuture<AudioStream> getStream(ResourceLocation pResourceLocation, boolean pIsWrapper) {
      return CompletableFuture.supplyAsync(() -> {
         try {
            InputStream inputstream = this.resourceManager.open(pResourceLocation);
            return (AudioStream)(pIsWrapper ? new LoopingAudioStream(OggAudioStream::new, inputstream) : new OggAudioStream(inputstream));
         } catch (IOException ioexception) {
            throw new CompletionException(ioexception);
         }
      }, Util.backgroundExecutor());
   }

   public void clear() {
      this.cache.values().forEach((p_120201_) -> {
         p_120201_.thenAccept(SoundBuffer::discardAlBuffer);
      });
      this.cache.clear();
   }

   public CompletableFuture<?> preload(Collection<Sound> pSounds) {
      return CompletableFuture.allOf(pSounds.stream().map((p_120197_) -> {
         return this.getCompleteBuffer(p_120197_.getPath());
      }).toArray((p_120195_) -> {
         return new CompletableFuture[p_120195_];
      }));
   }
}