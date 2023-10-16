package com.mojang.realmsclient.util;

import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.dto.GuardedSerializer;
import com.mojang.realmsclient.dto.ReflectionBasedSerialization;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsPersistence {
   private static final String FILE_NAME = "realms_persistence.json";
   private static final GuardedSerializer GSON = new GuardedSerializer();
   private static final Logger LOGGER = LogUtils.getLogger();

   public RealmsPersistence.RealmsPersistenceData read() {
      return readFile();
   }

   public void save(RealmsPersistence.RealmsPersistenceData pPersistenceData) {
      writeFile(pPersistenceData);
   }

   public static RealmsPersistence.RealmsPersistenceData readFile() {
      File file1 = getPathToData();

      try {
         String s = FileUtils.readFileToString(file1, StandardCharsets.UTF_8);
         RealmsPersistence.RealmsPersistenceData realmspersistence$realmspersistencedata = GSON.fromJson(s, RealmsPersistence.RealmsPersistenceData.class);
         if (realmspersistence$realmspersistencedata != null) {
            return realmspersistence$realmspersistencedata;
         }
      } catch (FileNotFoundException filenotfoundexception) {
      } catch (Exception exception) {
         LOGGER.warn("Failed to read Realms storage {}", file1, exception);
      }

      return new RealmsPersistence.RealmsPersistenceData();
   }

   public static void writeFile(RealmsPersistence.RealmsPersistenceData pPersistenceData) {
      File file1 = getPathToData();

      try {
         FileUtils.writeStringToFile(file1, GSON.toJson(pPersistenceData), StandardCharsets.UTF_8);
      } catch (IOException ioexception) {
      }

   }

   private static File getPathToData() {
      return new File(Minecraft.getInstance().gameDirectory, "realms_persistence.json");
   }

   @OnlyIn(Dist.CLIENT)
   public static class RealmsPersistenceData implements ReflectionBasedSerialization {
      @SerializedName("newsLink")
      public String newsLink;
      @SerializedName("hasUnreadNews")
      public boolean hasUnreadNews;
   }
}