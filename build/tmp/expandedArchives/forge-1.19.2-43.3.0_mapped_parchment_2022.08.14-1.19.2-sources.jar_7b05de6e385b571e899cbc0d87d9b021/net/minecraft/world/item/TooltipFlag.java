package net.minecraft.world.item;

public interface TooltipFlag {
   boolean isAdvanced();

   public static enum Default implements TooltipFlag {
      NORMAL(false),
      ADVANCED(true);

      private final boolean advanced;

      private Default(boolean pAdvanced) {
         this.advanced = pAdvanced;
      }

      public boolean isAdvanced() {
         return this.advanced;
      }
   }
}