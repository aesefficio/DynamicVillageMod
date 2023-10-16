package net.minecraft.client.multiplayer.chat.report;

import com.mojang.authlib.minecraft.UserApiService;
import java.util.Objects;
import net.minecraft.client.multiplayer.chat.ChatLog;
import net.minecraft.client.multiplayer.chat.RollingMemoryChatLog;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record ReportingContext(AbuseReportSender sender, ReportEnvironment environment, ChatLog chatLog) {
   private static final int LOG_CAPACITY = 1024;

   public static ReportingContext create(ReportEnvironment p_239686_, UserApiService p_239687_) {
      RollingMemoryChatLog rollingmemorychatlog = new RollingMemoryChatLog(1024);
      AbuseReportSender abusereportsender = AbuseReportSender.create(p_239686_, p_239687_);
      return new ReportingContext(abusereportsender, p_239686_, rollingmemorychatlog);
   }

   public boolean matches(ReportEnvironment p_239734_) {
      return Objects.equals(this.environment, p_239734_);
   }
}