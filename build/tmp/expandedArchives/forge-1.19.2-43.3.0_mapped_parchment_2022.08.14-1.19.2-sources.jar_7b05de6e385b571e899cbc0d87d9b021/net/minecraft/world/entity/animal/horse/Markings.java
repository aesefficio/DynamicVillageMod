package net.minecraft.world.entity.animal.horse;

import java.util.Arrays;
import java.util.Comparator;

public enum Markings {
   NONE(0),
   WHITE(1),
   WHITE_FIELD(2),
   WHITE_DOTS(3),
   BLACK_DOTS(4);

   private static final Markings[] BY_ID = Arrays.stream(values()).sorted(Comparator.comparingInt(Markings::getId)).toArray((p_30873_) -> {
      return new Markings[p_30873_];
   });
   private final int id;

   private Markings(int pId) {
      this.id = pId;
   }

   public int getId() {
      return this.id;
   }

   public static Markings byId(int pId) {
      return BY_ID[pId % BY_ID.length];
   }
}