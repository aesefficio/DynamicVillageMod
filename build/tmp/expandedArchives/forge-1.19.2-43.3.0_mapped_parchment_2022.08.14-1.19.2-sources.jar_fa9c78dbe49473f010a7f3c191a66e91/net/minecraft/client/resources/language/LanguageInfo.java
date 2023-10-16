package net.minecraft.client.resources.language;

import com.mojang.bridge.game.Language;
import java.util.Locale;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LanguageInfo implements Language, Comparable<LanguageInfo> {
   private final String code;
   private final String region;
   private final String name;
   private final boolean bidirectional;

   public LanguageInfo(String pCode, String pRegion, String pName, boolean pBidirectional) {
      this.code = pCode;
      this.region = pRegion;
      this.name = pName;
      this.bidirectional = pBidirectional;
      String[] splitLangCode = code.split("_", 2);
      if (splitLangCode.length == 1) { // Vanilla has some languages without underscores
         this.javaLocale = new java.util.Locale(code);
      } else {
         this.javaLocale = new java.util.Locale(splitLangCode[0], splitLangCode[1]);
      }
   }

   public String getCode() {
      return this.code;
   }

   public String getName() {
      return this.name;
   }

   public String getRegion() {
      return this.region;
   }

   public boolean isBidirectional() {
      return this.bidirectional;
   }

   public String toString() {
      return String.format(Locale.ROOT, "%s (%s)", this.name, this.region);
   }

   public boolean equals(Object pOther) {
      if (this == pOther) {
         return true;
      } else {
         return !(pOther instanceof LanguageInfo) ? false : this.code.equals(((LanguageInfo)pOther).code);
      }
   }

   public int hashCode() {
      return this.code.hashCode();
   }

   public int compareTo(LanguageInfo p_118954_) {
      return this.code.compareTo(p_118954_.code);
   }

   // Forge: add access to Locale so modders can create correct string and number formatters
   private final java.util.Locale javaLocale;
   public java.util.Locale getJavaLocale() { return javaLocale; }
}
