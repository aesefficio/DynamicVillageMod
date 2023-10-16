package net.minecraft.client.multiplayer.chat;

import java.util.Arrays;
import java.util.Comparator;
import net.minecraft.util.Mth;
import net.minecraft.util.OptionEnum;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum ChatPreviewStatus implements OptionEnum {
   OFF(0, "options.off"),
   LIVE(1, "options.chatPreview.live"),
   CONFIRM(2, "options.chatPreview.confirm");

   private static final ChatPreviewStatus[] BY_ID = Arrays.stream(values()).sorted(Comparator.comparingInt(ChatPreviewStatus::getId)).toArray((p_242169_) -> {
      return new ChatPreviewStatus[p_242169_];
   });
   private final int id;
   private final String key;

   private ChatPreviewStatus(int p_242464_, String p_242202_) {
      this.id = p_242464_;
      this.key = p_242202_;
   }

   public String getKey() {
      return this.key;
   }

   public int getId() {
      return this.id;
   }

   public static ChatPreviewStatus byId(int p_242383_) {
      return BY_ID[Mth.positiveModulo(p_242383_, BY_ID.length)];
   }
}