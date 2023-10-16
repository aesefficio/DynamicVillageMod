package net.minecraft.client.sounds;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.audio.Channel;
import com.mojang.blaze3d.audio.Library;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ChannelAccess {
   private final Set<ChannelAccess.ChannelHandle> channels = Sets.newIdentityHashSet();
   final Library library;
   final Executor executor;

   public ChannelAccess(Library pLibrary, Executor pExecutor) {
      this.library = pLibrary;
      this.executor = pExecutor;
   }

   public CompletableFuture<ChannelAccess.ChannelHandle> createHandle(Library.Pool pSystemMode) {
      CompletableFuture<ChannelAccess.ChannelHandle> completablefuture = new CompletableFuture<>();
      this.executor.execute(() -> {
         Channel channel = this.library.acquireChannel(pSystemMode);
         if (channel != null) {
            ChannelAccess.ChannelHandle channelaccess$channelhandle = new ChannelAccess.ChannelHandle(channel);
            this.channels.add(channelaccess$channelhandle);
            completablefuture.complete(channelaccess$channelhandle);
         } else {
            completablefuture.complete((ChannelAccess.ChannelHandle)null);
         }

      });
      return completablefuture;
   }

   public void executeOnChannels(Consumer<Stream<Channel>> pSourceStreamConsumer) {
      this.executor.execute(() -> {
         pSourceStreamConsumer.accept(this.channels.stream().map((p_174978_) -> {
            return p_174978_.channel;
         }).filter(Objects::nonNull));
      });
   }

   public void scheduleTick() {
      this.executor.execute(() -> {
         Iterator<ChannelAccess.ChannelHandle> iterator = this.channels.iterator();

         while(iterator.hasNext()) {
            ChannelAccess.ChannelHandle channelaccess$channelhandle = iterator.next();
            channelaccess$channelhandle.channel.updateStream();
            if (channelaccess$channelhandle.channel.stopped()) {
               channelaccess$channelhandle.release();
               iterator.remove();
            }
         }

      });
   }

   public void clear() {
      this.channels.forEach(ChannelAccess.ChannelHandle::release);
      this.channels.clear();
   }

   @OnlyIn(Dist.CLIENT)
   public class ChannelHandle {
      @Nullable
      Channel channel;
      private boolean stopped;

      public boolean isStopped() {
         return this.stopped;
      }

      public ChannelHandle(Channel pChannel) {
         this.channel = pChannel;
      }

      public void execute(Consumer<Channel> pSoundConsumer) {
         ChannelAccess.this.executor.execute(() -> {
            if (this.channel != null) {
               pSoundConsumer.accept(this.channel);
            }

         });
      }

      public void release() {
         this.stopped = true;
         ChannelAccess.this.library.releaseChannel(this.channel);
         this.channel = null;
      }
   }
}