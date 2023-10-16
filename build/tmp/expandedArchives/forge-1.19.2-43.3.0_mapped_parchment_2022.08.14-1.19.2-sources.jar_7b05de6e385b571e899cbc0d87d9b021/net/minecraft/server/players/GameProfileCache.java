package net.minecraft.server.players;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.authlib.Agent;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.UUIDUtil;
import org.slf4j.Logger;

public class GameProfileCache {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int GAMEPROFILES_MRU_LIMIT = 1000;
   private static final int GAMEPROFILES_EXPIRATION_MONTHS = 1;
   private static boolean usesAuthentication;
   /** A map between player usernames and */
   private final Map<String, GameProfileCache.GameProfileInfo> profilesByName = Maps.newConcurrentMap();
   /** A map between and */
   private final Map<UUID, GameProfileCache.GameProfileInfo> profilesByUUID = Maps.newConcurrentMap();
   private final Map<String, CompletableFuture<Optional<GameProfile>>> requests = Maps.newConcurrentMap();
   private final GameProfileRepository profileRepository;
   private final Gson gson = (new GsonBuilder()).create();
   private final File file;
   private final AtomicLong operationCount = new AtomicLong();
   @Nullable
   private Executor executor;

   public GameProfileCache(GameProfileRepository p_10974_, File p_10975_) {
      this.profileRepository = p_10974_;
      this.file = p_10975_;
      Lists.reverse(this.load()).forEach(this::safeAdd);
   }

   private void safeAdd(GameProfileCache.GameProfileInfo p_10980_) {
      GameProfile gameprofile = p_10980_.getProfile();
      p_10980_.setLastAccess(this.getNextOperation());
      String s = gameprofile.getName();
      if (s != null) {
         this.profilesByName.put(s.toLowerCase(Locale.ROOT), p_10980_);
      }

      UUID uuid = gameprofile.getId();
      if (uuid != null) {
         this.profilesByUUID.put(uuid, p_10980_);
      }

   }

   private static Optional<GameProfile> lookupGameProfile(GameProfileRepository pProfileRepo, String pName) {
      final AtomicReference<GameProfile> atomicreference = new AtomicReference<>();
      ProfileLookupCallback profilelookupcallback = new ProfileLookupCallback() {
         public void onProfileLookupSucceeded(GameProfile p_11017_) {
            atomicreference.set(p_11017_);
         }

         public void onProfileLookupFailed(GameProfile p_11014_, Exception p_11015_) {
            atomicreference.set((GameProfile)null);
         }
      };
      pProfileRepo.findProfilesByNames(new String[]{pName}, Agent.MINECRAFT, profilelookupcallback);
      GameProfile gameprofile = atomicreference.get();
      if (!usesAuthentication() && gameprofile == null) {
         UUID uuid = UUIDUtil.getOrCreatePlayerUUID(new GameProfile((UUID)null, pName));
         return Optional.of(new GameProfile(uuid, pName));
      } else {
         return Optional.ofNullable(gameprofile);
      }
   }

   public static void setUsesAuthentication(boolean pOnlineMode) {
      usesAuthentication = pOnlineMode;
   }

   private static boolean usesAuthentication() {
      return usesAuthentication;
   }

   /**
    * Add an entry to this cache
    */
   public void add(GameProfile pGameProfile) {
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(new Date());
      calendar.add(2, 1);
      Date date = calendar.getTime();
      GameProfileCache.GameProfileInfo gameprofilecache$gameprofileinfo = new GameProfileCache.GameProfileInfo(pGameProfile, date);
      this.safeAdd(gameprofilecache$gameprofileinfo);
      this.save();
   }

   private long getNextOperation() {
      return this.operationCount.incrementAndGet();
   }

   /**
    * Get a player's GameProfile given their username. Mojang's server's will be contacted if the entry is not cached
    * locally.
    */
   public Optional<GameProfile> get(String pName) {
      String s = pName.toLowerCase(Locale.ROOT);
      GameProfileCache.GameProfileInfo gameprofilecache$gameprofileinfo = this.profilesByName.get(s);
      boolean flag = false;
      if (gameprofilecache$gameprofileinfo != null && (new Date()).getTime() >= gameprofilecache$gameprofileinfo.expirationDate.getTime()) {
         this.profilesByUUID.remove(gameprofilecache$gameprofileinfo.getProfile().getId());
         this.profilesByName.remove(gameprofilecache$gameprofileinfo.getProfile().getName().toLowerCase(Locale.ROOT));
         flag = true;
         gameprofilecache$gameprofileinfo = null;
      }

      Optional<GameProfile> optional;
      if (gameprofilecache$gameprofileinfo != null) {
         gameprofilecache$gameprofileinfo.setLastAccess(this.getNextOperation());
         optional = Optional.of(gameprofilecache$gameprofileinfo.getProfile());
      } else {
         optional = lookupGameProfile(this.profileRepository, s);
         if (optional.isPresent()) {
            this.add(optional.get());
            flag = false;
         }
      }

      if (flag) {
         this.save();
      }

      return optional;
   }

   public void getAsync(String p_143968_, Consumer<Optional<GameProfile>> p_143969_) {
      if (this.executor == null) {
         throw new IllegalStateException("No executor");
      } else {
         CompletableFuture<Optional<GameProfile>> completablefuture = this.requests.get(p_143968_);
         if (completablefuture != null) {
            this.requests.put(p_143968_, completablefuture.whenCompleteAsync((p_143984_, p_143985_) -> {
               p_143969_.accept(p_143984_);
            }, this.executor));
         } else {
            this.requests.put(p_143968_, CompletableFuture.supplyAsync(() -> {
               return this.get(p_143968_);
            }, Util.backgroundExecutor()).whenCompleteAsync((p_143965_, p_143966_) -> {
               this.requests.remove(p_143968_);
            }, this.executor).whenCompleteAsync((p_143978_, p_143979_) -> {
               p_143969_.accept(p_143978_);
            }, this.executor));
         }

      }
   }

   /**
    * 
    * @param pUuid Get a player's {@link GameProfile} given their UUID
    */
   public Optional<GameProfile> get(UUID pUuid) {
      GameProfileCache.GameProfileInfo gameprofilecache$gameprofileinfo = this.profilesByUUID.get(pUuid);
      if (gameprofilecache$gameprofileinfo == null) {
         return Optional.empty();
      } else {
         gameprofilecache$gameprofileinfo.setLastAccess(this.getNextOperation());
         return Optional.of(gameprofilecache$gameprofileinfo.getProfile());
      }
   }

   public void setExecutor(Executor p_143975_) {
      this.executor = p_143975_;
   }

   public void clearExecutor() {
      this.executor = null;
   }

   private static DateFormat createDateFormat() {
      return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.ROOT);
   }

   public List<GameProfileCache.GameProfileInfo> load() {
      List<GameProfileCache.GameProfileInfo> list = Lists.newArrayList();

      try {
         Reader reader = Files.newReader(this.file, StandardCharsets.UTF_8);

         Object object;
         label61: {
            try {
               JsonArray jsonarray = this.gson.fromJson(reader, JsonArray.class);
               if (jsonarray == null) {
                  object = list;
                  break label61;
               }

               DateFormat dateformat = createDateFormat();
               jsonarray.forEach((p_143973_) -> {
                  readGameProfile(p_143973_, dateformat).ifPresent(list::add);
               });
            } catch (Throwable throwable1) {
               if (reader != null) {
                  try {
                     reader.close();
                  } catch (Throwable throwable) {
                     throwable1.addSuppressed(throwable);
                  }
               }

               throw throwable1;
            }

            if (reader != null) {
               reader.close();
            }

            return list;
         }

         if (reader != null) {
            reader.close();
         }

         return (List<GameProfileCache.GameProfileInfo>)object;
      } catch (FileNotFoundException filenotfoundexception) {
      } catch (JsonParseException | IOException ioexception) {
         LOGGER.warn("Failed to load profile cache {}", this.file, ioexception);
      }

      return list;
   }

   /**
    * Save the cached profiles to disk
    */
   public void save() {
      JsonArray jsonarray = new JsonArray();
      DateFormat dateformat = createDateFormat();
      this.getTopMRUProfiles(1000).forEach((p_143962_) -> {
         jsonarray.add(writeGameProfile(p_143962_, dateformat));
      });
      String s = this.gson.toJson((JsonElement)jsonarray);

      try {
         Writer writer = Files.newWriter(this.file, StandardCharsets.UTF_8);

         try {
            writer.write(s);
         } catch (Throwable throwable1) {
            if (writer != null) {
               try {
                  writer.close();
               } catch (Throwable throwable) {
                  throwable1.addSuppressed(throwable);
               }
            }

            throw throwable1;
         }

         if (writer != null) {
            writer.close();
         }
      } catch (IOException ioexception) {
      }

   }

   private Stream<GameProfileCache.GameProfileInfo> getTopMRUProfiles(int p_10978_) {
      return ImmutableList.copyOf(this.profilesByUUID.values()).stream().sorted(Comparator.comparing(GameProfileCache.GameProfileInfo::getLastAccess).reversed()).limit((long)p_10978_);
   }

   private static JsonElement writeGameProfile(GameProfileCache.GameProfileInfo p_10982_, DateFormat p_10983_) {
      JsonObject jsonobject = new JsonObject();
      jsonobject.addProperty("name", p_10982_.getProfile().getName());
      UUID uuid = p_10982_.getProfile().getId();
      jsonobject.addProperty("uuid", uuid == null ? "" : uuid.toString());
      jsonobject.addProperty("expiresOn", p_10983_.format(p_10982_.getExpirationDate()));
      return jsonobject;
   }

   private static Optional<GameProfileCache.GameProfileInfo> readGameProfile(JsonElement p_10989_, DateFormat p_10990_) {
      if (p_10989_.isJsonObject()) {
         JsonObject jsonobject = p_10989_.getAsJsonObject();
         JsonElement jsonelement = jsonobject.get("name");
         JsonElement jsonelement1 = jsonobject.get("uuid");
         JsonElement jsonelement2 = jsonobject.get("expiresOn");
         if (jsonelement != null && jsonelement1 != null) {
            String s = jsonelement1.getAsString();
            String s1 = jsonelement.getAsString();
            Date date = null;
            if (jsonelement2 != null) {
               try {
                  date = p_10990_.parse(jsonelement2.getAsString());
               } catch (ParseException parseexception) {
               }
            }

            if (s1 != null && s != null && date != null) {
               UUID uuid;
               try {
                  uuid = UUID.fromString(s);
               } catch (Throwable throwable) {
                  return Optional.empty();
               }

               return Optional.of(new GameProfileCache.GameProfileInfo(new GameProfile(uuid, s1), date));
            } else {
               return Optional.empty();
            }
         } else {
            return Optional.empty();
         }
      } else {
         return Optional.empty();
      }
   }

   static class GameProfileInfo {
      /** The player's GameProfile */
      private final GameProfile profile;
      /** The date that this entry will expire */
      final Date expirationDate;
      private volatile long lastAccess;

      GameProfileInfo(GameProfile p_11022_, Date p_11023_) {
         this.profile = p_11022_;
         this.expirationDate = p_11023_;
      }

      /**
       * Get the player's GameProfile
       */
      public GameProfile getProfile() {
         return this.profile;
      }

      /**
       * Get the date that this entry will expire
       */
      public Date getExpirationDate() {
         return this.expirationDate;
      }

      public void setLastAccess(long p_11030_) {
         this.lastAccess = p_11030_;
      }

      public long getLastAccess() {
         return this.lastAccess;
      }
   }
}