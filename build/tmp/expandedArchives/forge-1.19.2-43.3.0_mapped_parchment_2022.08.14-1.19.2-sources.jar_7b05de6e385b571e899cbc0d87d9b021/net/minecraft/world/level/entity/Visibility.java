package net.minecraft.world.level.entity;

import net.minecraft.server.level.ChunkHolder;

public enum Visibility {
   HIDDEN(false, false),
   TRACKED(true, false),
   TICKING(true, true);

   private final boolean accessible;
   private final boolean ticking;

   private Visibility(boolean pAccessible, boolean pTicking) {
      this.accessible = pAccessible;
      this.ticking = pTicking;
   }

   public boolean isTicking() {
      return this.ticking;
   }

   public boolean isAccessible() {
      return this.accessible;
   }

   public static Visibility fromFullChunkStatus(ChunkHolder.FullChunkStatus pChunkStatus) {
      if (pChunkStatus.isOrAfter(ChunkHolder.FullChunkStatus.ENTITY_TICKING)) {
         return TICKING;
      } else {
         return pChunkStatus.isOrAfter(ChunkHolder.FullChunkStatus.BORDER) ? TRACKED : HIDDEN;
      }
   }
}