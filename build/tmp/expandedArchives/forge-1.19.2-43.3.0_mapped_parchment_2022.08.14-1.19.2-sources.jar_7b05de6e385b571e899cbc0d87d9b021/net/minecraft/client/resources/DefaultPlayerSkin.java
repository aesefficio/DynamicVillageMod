package net.minecraft.client.resources;

import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DefaultPlayerSkin {
   /** The default skin for the Steve model. */
   private static final ResourceLocation STEVE_SKIN_LOCATION = new ResourceLocation("textures/entity/steve.png");
   /** The default skin for the Alex model. */
   private static final ResourceLocation ALEX_SKIN_LOCATION = new ResourceLocation("textures/entity/alex.png");
   private static final String STEVE_MODEL = "default";
   private static final String ALEX_MODEL = "slim";

   /**
    * Returns the default skind for versions prior to 1.8, which is always the Steve texture.
    */
   public static ResourceLocation getDefaultSkin() {
      return STEVE_SKIN_LOCATION;
   }

   /**
    * Retrieves the default skin for this player. Depending on the model used this will be Alex or Steve.
    */
   public static ResourceLocation getDefaultSkin(UUID pPlayerUUID) {
      return isAlexDefault(pPlayerUUID) ? ALEX_SKIN_LOCATION : STEVE_SKIN_LOCATION;
   }

   /**
    * Retrieves the type of skin that a player is using. The Alex model is slim while the Steve model is default.
    */
   public static String getSkinModelName(UUID pPlayerUUID) {
      return isAlexDefault(pPlayerUUID) ? "slim" : "default";
   }

   /**
    * Checks if a players skin model is slim or the default. The Alex model is slime while the Steve model is default.
    */
   private static boolean isAlexDefault(UUID pPlayerUUID) {
      return (pPlayerUUID.hashCode() & 1) == 1;
   }
}