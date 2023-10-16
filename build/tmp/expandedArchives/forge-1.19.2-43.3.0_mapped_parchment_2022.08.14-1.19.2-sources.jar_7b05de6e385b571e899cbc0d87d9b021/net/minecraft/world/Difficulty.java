package net.minecraft.world;

import java.util.Arrays;
import java.util.Comparator;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;

public enum Difficulty {
   PEACEFUL(0, "peaceful"),
   EASY(1, "easy"),
   NORMAL(2, "normal"),
   HARD(3, "hard");

   private static final Difficulty[] BY_ID = Arrays.stream(values()).sorted(Comparator.comparingInt(Difficulty::getId)).toArray((p_19035_) -> {
      return new Difficulty[p_19035_];
   });
   private final int id;
   private final String key;

   private Difficulty(int pId, String pKey) {
      this.id = pId;
      this.key = pKey;
   }

   public int getId() {
      return this.id;
   }

   public Component getDisplayName() {
      return Component.translatable("options.difficulty." + this.key);
   }

   public static Difficulty byId(int pId) {
      return BY_ID[pId % BY_ID.length];
   }

   @Nullable
   public static Difficulty byName(String pName) {
      for(Difficulty difficulty : values()) {
         if (difficulty.key.equals(pName)) {
            return difficulty;
         }
      }

      return null;
   }

   public String getKey() {
      return this.key;
   }
}