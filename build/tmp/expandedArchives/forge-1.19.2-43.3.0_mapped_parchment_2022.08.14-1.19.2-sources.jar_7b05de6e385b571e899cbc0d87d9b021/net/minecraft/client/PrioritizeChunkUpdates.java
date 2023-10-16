package net.minecraft.client;

import java.util.Arrays;
import java.util.Comparator;
import net.minecraft.util.Mth;
import net.minecraft.util.OptionEnum;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum PrioritizeChunkUpdates implements OptionEnum {
   NONE(0, "options.prioritizeChunkUpdates.none"),
   PLAYER_AFFECTED(1, "options.prioritizeChunkUpdates.byPlayer"),
   NEARBY(2, "options.prioritizeChunkUpdates.nearby");

   private static final PrioritizeChunkUpdates[] BY_ID = Arrays.stream(values()).sorted(Comparator.comparingInt(PrioritizeChunkUpdates::getId)).toArray((p_193791_) -> {
      return new PrioritizeChunkUpdates[p_193791_];
   });
   private final int id;
   private final String key;

   private PrioritizeChunkUpdates(int pId, String pKey) {
      this.id = pId;
      this.key = pKey;
   }

   public int getId() {
      return this.id;
   }

   public String getKey() {
      return this.key;
   }

   public static PrioritizeChunkUpdates byId(int pId) {
      return BY_ID[Mth.positiveModulo(pId, BY_ID.length)];
   }
}