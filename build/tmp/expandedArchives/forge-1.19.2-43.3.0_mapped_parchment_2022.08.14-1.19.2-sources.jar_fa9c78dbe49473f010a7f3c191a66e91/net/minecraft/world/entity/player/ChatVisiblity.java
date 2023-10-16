package net.minecraft.world.entity.player;

import java.util.Arrays;
import java.util.Comparator;
import net.minecraft.util.Mth;
import net.minecraft.util.OptionEnum;

public enum ChatVisiblity implements OptionEnum {
   FULL(0, "options.chat.visibility.full"),
   SYSTEM(1, "options.chat.visibility.system"),
   HIDDEN(2, "options.chat.visibility.hidden");

   private static final ChatVisiblity[] BY_ID = Arrays.stream(values()).sorted(Comparator.comparingInt(ChatVisiblity::getId)).toArray((p_35970_) -> {
      return new ChatVisiblity[p_35970_];
   });
   private final int id;
   private final String key;

   private ChatVisiblity(int pId, String pKey) {
      this.id = pId;
      this.key = pKey;
   }

   public int getId() {
      return this.id;
   }

   public String getKey() {
      return this.key;
   }

   public static ChatVisiblity byId(int pId) {
      return BY_ID[Mth.positiveModulo(pId, BY_ID.length)];
   }
}