package net.minecraft.world.level;

import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Abilities;

public enum GameType {
   SURVIVAL(0, "survival"),
   CREATIVE(1, "creative"),
   ADVENTURE(2, "adventure"),
   SPECTATOR(3, "spectator");

   public static final GameType DEFAULT_MODE = SURVIVAL;
   private static final int NOT_SET = -1;
   private final int id;
   private final String name;
   private final Component shortName;
   private final Component longName;

   private GameType(int pId, String pName) {
      this.id = pId;
      this.name = pName;
      this.shortName = Component.translatable("selectWorld.gameMode." + pName);
      this.longName = Component.translatable("gameMode." + pName);
   }

   /**
    * Returns the ID of this game type
    */
   public int getId() {
      return this.id;
   }

   /**
    * Returns the name of this game type
    */
   public String getName() {
      return this.name;
   }

   public Component getLongDisplayName() {
      return this.longName;
   }

   public Component getShortDisplayName() {
      return this.shortName;
   }

   /**
    * Configures the player abilities based on the game type
    */
   public void updatePlayerAbilities(Abilities pAbilities) {
      if (this == CREATIVE) {
         pAbilities.mayfly = true;
         pAbilities.instabuild = true;
         pAbilities.invulnerable = true;
      } else if (this == SPECTATOR) {
         pAbilities.mayfly = true;
         pAbilities.instabuild = false;
         pAbilities.invulnerable = true;
         pAbilities.flying = true;
      } else {
         pAbilities.mayfly = false;
         pAbilities.instabuild = false;
         pAbilities.invulnerable = false;
         pAbilities.flying = false;
      }

      pAbilities.mayBuild = !this.isBlockPlacingRestricted();
   }

   /**
    * Returns true if this is the ADVENTURE game type
    */
   public boolean isBlockPlacingRestricted() {
      return this == ADVENTURE || this == SPECTATOR;
   }

   /**
    * Returns true if this is the CREATIVE game type
    */
   public boolean isCreative() {
      return this == CREATIVE;
   }

   /**
    * Returns true if this is the SURVIVAL or ADVENTURE game type
    */
   public boolean isSurvival() {
      return this == SURVIVAL || this == ADVENTURE;
   }

   /**
    * Gets the game type by it's ID. Will be survival if none was found.
    */
   public static GameType byId(int pId) {
      return byId(pId, DEFAULT_MODE);
   }

   public static GameType byId(int pTargetId, GameType pFallback) {
      for(GameType gametype : values()) {
         if (gametype.id == pTargetId) {
            return gametype;
         }
      }

      return pFallback;
   }

   /**
    * Gets the game type registered with the specified name. If no matches were found, survival will be returned.
    */
   public static GameType byName(String pGamemodeName) {
      return byName(pGamemodeName, SURVIVAL);
   }

   public static GameType byName(String pTargetName, GameType pFallback) {
      for(GameType gametype : values()) {
         if (gametype.name.equals(pTargetName)) {
            return gametype;
         }
      }

      return pFallback;
   }

   public static int getNullableId(@Nullable GameType pGameType) {
      return pGameType != null ? pGameType.id : -1;
   }

   @Nullable
   public static GameType byNullableId(int pId) {
      return pId == -1 ? null : byId(pId);
   }
}