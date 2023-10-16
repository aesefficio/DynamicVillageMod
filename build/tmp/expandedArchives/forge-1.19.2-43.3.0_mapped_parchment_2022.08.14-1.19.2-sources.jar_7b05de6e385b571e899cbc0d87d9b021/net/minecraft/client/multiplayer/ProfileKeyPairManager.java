package net.minecraft.client.multiplayer;

import com.google.common.base.Strings;
import com.google.gson.JsonParser;
import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.minecraft.InsecurePublicKeyException;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.response.KeyPairResponse;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PublicKey;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.util.Crypt;
import net.minecraft.util.CryptException;
import net.minecraft.util.Signer;
import net.minecraft.world.entity.player.ProfileKeyPair;
import net.minecraft.world.entity.player.ProfilePublicKey;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ProfileKeyPairManager {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Path PROFILE_KEY_PAIR_DIR = Path.of("profilekeys");
   private final UserApiService userApiService;
   private final Path profileKeyPairPath;
   private CompletableFuture<Optional<ProfileKeyPairManager.Result>> keyPair;

   public ProfileKeyPairManager(UserApiService p_233772_, UUID p_233773_, Path p_233774_) {
      this.userApiService = p_233772_;
      this.profileKeyPairPath = p_233774_.resolve(PROFILE_KEY_PAIR_DIR).resolve(p_233773_ + ".json");
      this.keyPair = CompletableFuture.supplyAsync(() -> {
         return this.readProfileKeyPair().filter((p_233788_) -> {
            return !p_233788_.publicKey().data().hasExpired();
         });
      }, Util.backgroundExecutor()).thenCompose(this::readOrFetchProfileKeyPair);
   }

   public CompletableFuture<Optional<ProfilePublicKey.Data>> preparePublicKey() {
      this.keyPair = this.keyPair.thenCompose((p_243400_) -> {
         Optional<ProfileKeyPair> optional = p_243400_.map(ProfileKeyPairManager.Result::keyPair);
         return this.readOrFetchProfileKeyPair(optional);
      });
      return this.keyPair.thenApply((p_233792_) -> {
         return p_233792_.map((p_243371_) -> {
            return p_243371_.keyPair().publicKey().data();
         });
      });
   }

   private CompletableFuture<Optional<ProfileKeyPairManager.Result>> readOrFetchProfileKeyPair(Optional<ProfileKeyPair> p_243401_) {
      return CompletableFuture.supplyAsync(() -> {
         if (p_243401_.isPresent() && !p_243401_.get().dueRefresh()) {
            if (SharedConstants.IS_RUNNING_IN_IDE) {
               return p_243401_;
            }

            this.writeProfileKeyPair((ProfileKeyPair)null);
         }

         try {
            ProfileKeyPair profilekeypair = this.fetchProfileKeyPair(this.userApiService);
            this.writeProfileKeyPair(profilekeypair);
            return Optional.of(profilekeypair);
         } catch (CryptException | MinecraftClientException | IOException ioexception) {
            // Forge: The offline user api service always returns a null profile key pair, so let's hide this useless exception if in dev
            if (net.minecraftforge.fml.loading.FMLLoader.isProduction() || this.userApiService != UserApiService.OFFLINE)
            LOGGER.error("Failed to retrieve profile key pair", (Throwable)ioexception);
            this.writeProfileKeyPair((ProfileKeyPair)null);
            return p_243401_;
         }
      }, Util.backgroundExecutor()).thenApply((p_233785_) -> {
         return p_233785_.map(ProfileKeyPairManager.Result::new);
      });
   }

   private Optional<ProfileKeyPair> readProfileKeyPair() {
      if (Files.notExists(this.profileKeyPairPath)) {
         return Optional.empty();
      } else {
         try {
            BufferedReader bufferedreader = Files.newBufferedReader(this.profileKeyPairPath);

            Optional optional;
            try {
               optional = ProfileKeyPair.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(bufferedreader)).result();
            } catch (Throwable throwable1) {
               if (bufferedreader != null) {
                  try {
                     bufferedreader.close();
                  } catch (Throwable throwable) {
                     throwable1.addSuppressed(throwable);
                  }
               }

               throw throwable1;
            }

            if (bufferedreader != null) {
               bufferedreader.close();
            }

            return optional;
         } catch (Exception exception) {
            LOGGER.error("Failed to read profile key pair file {}", this.profileKeyPairPath, exception);
            return Optional.empty();
         }
      }
   }

   private void writeProfileKeyPair(@Nullable ProfileKeyPair p_233777_) {
      try {
         Files.deleteIfExists(this.profileKeyPairPath);
      } catch (IOException ioexception) {
         LOGGER.error("Failed to delete profile key pair file {}", this.profileKeyPairPath, ioexception);
      }

      if (p_233777_ != null) {
         if (SharedConstants.IS_RUNNING_IN_IDE) {
            ProfileKeyPair.CODEC.encodeStart(JsonOps.INSTANCE, p_233777_).result().ifPresent((p_233779_) -> {
               try {
                  Files.createDirectories(this.profileKeyPairPath.getParent());
                  Files.writeString(this.profileKeyPairPath, p_233779_.toString());
               } catch (Exception exception) {
                  LOGGER.error("Failed to write profile key pair file {}", this.profileKeyPairPath, exception);
               }

            });
         }
      }
   }

   private ProfileKeyPair fetchProfileKeyPair(UserApiService p_233790_) throws CryptException, IOException {
      KeyPairResponse keypairresponse = p_233790_.getKeyPair();
      if (keypairresponse != null) {
         ProfilePublicKey.Data profilepublickey$data = parsePublicKey(keypairresponse);
         return new ProfileKeyPair(Crypt.stringToPemRsaPrivateKey(keypairresponse.getPrivateKey()), new ProfilePublicKey(profilepublickey$data), Instant.parse(keypairresponse.getRefreshedAfter()));
      } else {
         throw new IOException("Could not retrieve profile key pair");
      }
   }

   private static ProfilePublicKey.Data parsePublicKey(KeyPairResponse p_233783_) throws CryptException {
      if (!Strings.isNullOrEmpty(p_233783_.getPublicKey()) && p_233783_.getPublicKeySignature() != null && p_233783_.getPublicKeySignature().array().length != 0) {
         try {
            Instant instant = Instant.parse(p_233783_.getExpiresAt());
            PublicKey publickey = Crypt.stringToRsaPublicKey(p_233783_.getPublicKey());
            ByteBuffer bytebuffer = p_233783_.getPublicKeySignature();
            return new ProfilePublicKey.Data(instant, publickey, bytebuffer.array());
         } catch (IllegalArgumentException | DateTimeException datetimeexception) {
            throw new CryptException(datetimeexception);
         }
      } else {
         throw new CryptException(new InsecurePublicKeyException.MissingException());
      }
   }

   @Nullable
   public Signer signer() {
      return this.keyPair.join().map(ProfileKeyPairManager.Result::signer).orElse((Signer)null);
   }

   public Optional<ProfilePublicKey> profilePublicKey() {
      return this.keyPair.join().map((p_243372_) -> {
         return p_243372_.keyPair().publicKey();
      });
   }

   @OnlyIn(Dist.CLIENT)
   static record Result(ProfileKeyPair keyPair, Signer signer) {
      public Result(ProfileKeyPair p_243382_) {
         this(p_243382_, Signer.from(p_243382_.privateKey(), "SHA256withRSA"));
      }
   }
}
