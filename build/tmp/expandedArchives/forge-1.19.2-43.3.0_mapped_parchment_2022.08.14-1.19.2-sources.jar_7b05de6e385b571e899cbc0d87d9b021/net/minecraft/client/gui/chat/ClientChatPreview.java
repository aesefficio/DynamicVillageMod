package net.minecraft.client.gui.chat;

import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;

@OnlyIn(Dist.CLIENT)
public class ClientChatPreview {
   private static final long PREVIEW_VALID_AFTER_MS = 200L;
   @Nullable
   private String lastQuery;
   @Nullable
   private String scheduledRequest;
   private final ChatPreviewRequests requests;
   @Nullable
   private ClientChatPreview.Preview preview;

   public ClientChatPreview(Minecraft p_232411_) {
      this.requests = new ChatPreviewRequests(p_232411_);
   }

   public void tick() {
      String s = this.scheduledRequest;
      if (s != null && this.requests.trySendRequest(s, Util.getMillis())) {
         this.scheduledRequest = null;
      }

   }

   public void update(String p_232417_) {
      p_232417_ = normalizeQuery(p_232417_);
      if (!p_232417_.isEmpty()) {
         if (!p_232417_.equals(this.lastQuery)) {
            this.lastQuery = p_232417_;
            this.sendOrScheduleRequest(p_232417_);
         }
      } else {
         this.clear();
      }

   }

   private void sendOrScheduleRequest(String p_232423_) {
      if (!this.requests.trySendRequest(p_232423_, Util.getMillis())) {
         this.scheduledRequest = p_232423_;
      } else {
         this.scheduledRequest = null;
      }

   }

   public void disable() {
      this.clear();
   }

   private void clear() {
      this.lastQuery = null;
      this.scheduledRequest = null;
      this.preview = null;
      this.requests.clear();
   }

   public void handleResponse(int p_232414_, @Nullable Component p_232415_) {
      String s = this.requests.handleResponse(p_232414_);
      if (s != null) {
         this.preview = new ClientChatPreview.Preview(Util.getMillis(), s, p_232415_);
      }

   }

   public boolean hasScheduledRequest() {
      return this.scheduledRequest != null || this.preview != null && !this.preview.isPreviewValid();
   }

   public boolean queryEquals(String p_242426_) {
      return normalizeQuery(p_242426_).equals(this.lastQuery);
   }

   @Nullable
   public ClientChatPreview.Preview peek() {
      return this.preview;
   }

   @Nullable
   public ClientChatPreview.Preview pull(String p_242462_) {
      if (this.preview != null && this.preview.canPull(p_242462_)) {
         ClientChatPreview.Preview clientchatpreview$preview = this.preview;
         this.preview = null;
         return clientchatpreview$preview;
      } else {
         return null;
      }
   }

   static String normalizeQuery(String p_232426_) {
      return StringUtils.normalizeSpace(p_232426_.trim());
   }

   @OnlyIn(Dist.CLIENT)
   public static record Preview(long receivedTimeStamp, String query, @Nullable Component response) {
      public Preview {
         query = ClientChatPreview.normalizeQuery(query);
      }

      private boolean queryEquals(String p_242232_) {
         return this.query.equals(ClientChatPreview.normalizeQuery(p_242232_));
      }

      boolean canPull(String p_232437_) {
         return this.queryEquals(p_232437_) ? this.isPreviewValid() : false;
      }

      boolean isPreviewValid() {
         long i = this.receivedTimeStamp + 200L;
         return Util.getMillis() >= i;
      }
   }
}