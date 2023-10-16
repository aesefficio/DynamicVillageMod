package com.mojang.realmsclient.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Date;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class JsonUtils {
   public static String getStringOr(String pKey, JsonObject pJson, String pDefaultValue) {
      JsonElement jsonelement = pJson.get(pKey);
      if (jsonelement != null) {
         return jsonelement.isJsonNull() ? pDefaultValue : jsonelement.getAsString();
      } else {
         return pDefaultValue;
      }
   }

   public static int getIntOr(String pKey, JsonObject pJson, int pDefaultValue) {
      JsonElement jsonelement = pJson.get(pKey);
      if (jsonelement != null) {
         return jsonelement.isJsonNull() ? pDefaultValue : jsonelement.getAsInt();
      } else {
         return pDefaultValue;
      }
   }

   public static long getLongOr(String pKey, JsonObject pJson, long pDefaultValue) {
      JsonElement jsonelement = pJson.get(pKey);
      if (jsonelement != null) {
         return jsonelement.isJsonNull() ? pDefaultValue : jsonelement.getAsLong();
      } else {
         return pDefaultValue;
      }
   }

   public static boolean getBooleanOr(String pKey, JsonObject pJson, boolean pDefaultValue) {
      JsonElement jsonelement = pJson.get(pKey);
      if (jsonelement != null) {
         return jsonelement.isJsonNull() ? pDefaultValue : jsonelement.getAsBoolean();
      } else {
         return pDefaultValue;
      }
   }

   public static Date getDateOr(String pKey, JsonObject pJson) {
      JsonElement jsonelement = pJson.get(pKey);
      return jsonelement != null ? new Date(Long.parseLong(jsonelement.getAsString())) : new Date();
   }
}