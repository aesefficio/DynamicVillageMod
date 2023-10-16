package net.minecraft.client.gui.chat;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ServerboundChatPreviewPacket;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ChatPreviewRequests {
   private static final long MIN_REQUEST_INTERVAL_MS = 100L;
   private static final long MAX_REQUEST_INTERVAL_MS = 1000L;
   private final Minecraft minecraft;
   private final ChatPreviewRequests.QueryIdGenerator queryIdGenerator = new ChatPreviewRequests.QueryIdGenerator();
   @Nullable
   private ChatPreviewRequests.PendingPreview pending;
   private long lastRequestTime;

   public ChatPreviewRequests(Minecraft p_232374_) {
      this.minecraft = p_232374_;
   }

   public boolean trySendRequest(String p_232381_, long p_232382_) {
      ClientPacketListener clientpacketlistener = this.minecraft.getConnection();
      if (clientpacketlistener == null) {
         this.clear();
         return true;
      } else if (this.pending != null && this.pending.matches(p_232381_)) {
         return true;
      } else if (!this.minecraft.isLocalServer() && !this.isRequestReady(p_232382_)) {
         return false;
      } else {
         ChatPreviewRequests.PendingPreview chatpreviewrequests$pendingpreview = new ChatPreviewRequests.PendingPreview(this.queryIdGenerator.next(), p_232381_);
         this.pending = chatpreviewrequests$pendingpreview;
         this.lastRequestTime = p_232382_;
         clientpacketlistener.send(new ServerboundChatPreviewPacket(chatpreviewrequests$pendingpreview.id(), chatpreviewrequests$pendingpreview.query()));
         return true;
      }
   }

   @Nullable
   public String handleResponse(int p_232377_) {
      if (this.pending != null && this.pending.matches(p_232377_)) {
         String s = this.pending.query;
         this.pending = null;
         return s;
      } else {
         return null;
      }
   }

   private boolean isRequestReady(long p_232379_) {
      long i = this.lastRequestTime + 100L;
      if (p_232379_ < i) {
         return false;
      } else {
         long j = this.lastRequestTime + 1000L;
         return this.pending == null || p_232379_ >= j;
      }
   }

   public void clear() {
      this.pending = null;
      this.lastRequestTime = 0L;
   }

   public boolean isPending() {
      return this.pending != null;
   }

   @OnlyIn(Dist.CLIENT)
   static record PendingPreview(int id, String query) {
      public boolean matches(int p_232391_) {
         return this.id == p_232391_;
      }

      public boolean matches(String p_232393_) {
         return this.query.equals(p_232393_);
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class QueryIdGenerator {
      private static final int MAX_STEP = 100;
      private final RandomSource random = RandomSource.createNewThreadLocalInstance();
      private int lastId;

      public int next() {
         int i = this.lastId + this.random.nextInt(100);
         this.lastId = i;
         return i;
      }
   }
}