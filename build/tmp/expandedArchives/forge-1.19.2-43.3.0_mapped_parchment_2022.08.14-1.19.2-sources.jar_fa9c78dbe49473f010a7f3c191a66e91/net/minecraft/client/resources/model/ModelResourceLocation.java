package net.minecraft.client.resources.model;

import com.google.common.annotations.VisibleForTesting;
import java.util.Locale;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ModelResourceLocation extends ResourceLocation {
   @VisibleForTesting
   static final char VARIANT_SEPARATOR = '#';
   private final String variant;

   protected ModelResourceLocation(String[] pDecomposedLocation) {
      super(pDecomposedLocation);
      this.variant = pDecomposedLocation[2].toLowerCase(Locale.ROOT);
   }

   public ModelResourceLocation(String pNamespace, String pLocation, String pPath) {
      this(new String[]{pNamespace, pLocation, pPath});
   }

   public ModelResourceLocation(String pLocation) {
      this(decompose(pLocation));
   }

   public ModelResourceLocation(ResourceLocation pNamespace, String pPath) {
      this(pNamespace.toString(), pPath);
   }

   public ModelResourceLocation(String pNamespace, String pPath) {
      this(decompose(pNamespace + "#" + pPath));
   }

   protected static String[] decompose(String pPath) {
      String[] astring = new String[]{null, pPath, ""};
      int i = pPath.indexOf(35);
      String s = pPath;
      if (i >= 0) {
         astring[2] = pPath.substring(i + 1, pPath.length());
         if (i > 1) {
            s = pPath.substring(0, i);
         }
      }

      System.arraycopy(ResourceLocation.decompose(s, ':'), 0, astring, 0, 2);
      return astring;
   }

   public String getVariant() {
      return this.variant;
   }

   public boolean equals(Object pOther) {
      if (this == pOther) {
         return true;
      } else if (pOther instanceof ModelResourceLocation && super.equals(pOther)) {
         ModelResourceLocation modelresourcelocation = (ModelResourceLocation)pOther;
         return this.variant.equals(modelresourcelocation.variant);
      } else {
         return false;
      }
   }

   public int hashCode() {
      return 31 * super.hashCode() + this.variant.hashCode();
   }

   public String toString() {
      return super.toString() + "#" + this.variant;
   }
}