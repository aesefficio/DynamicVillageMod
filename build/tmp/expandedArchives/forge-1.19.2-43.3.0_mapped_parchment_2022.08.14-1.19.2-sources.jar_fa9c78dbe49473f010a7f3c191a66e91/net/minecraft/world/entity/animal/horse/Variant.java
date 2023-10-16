package net.minecraft.world.entity.animal.horse;

import java.util.Arrays;
import java.util.Comparator;

public enum Variant {
   WHITE(0),
   CREAMY(1),
   CHESTNUT(2),
   BROWN(3),
   BLACK(4),
   GRAY(5),
   DARKBROWN(6);

   private static final Variant[] BY_ID = Arrays.stream(values()).sorted(Comparator.comparingInt(Variant::getId)).toArray((p_30989_) -> {
      return new Variant[p_30989_];
   });
   private final int id;

   private Variant(int pId) {
      this.id = pId;
   }

   public int getId() {
      return this.id;
   }

   public static Variant byId(int pId) {
      return BY_ID[pId % BY_ID.length];
   }
}