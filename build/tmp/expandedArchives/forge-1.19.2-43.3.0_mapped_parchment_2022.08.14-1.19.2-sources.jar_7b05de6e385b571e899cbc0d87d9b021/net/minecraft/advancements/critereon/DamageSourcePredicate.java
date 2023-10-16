package net.minecraft.advancements.critereon;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;

public class DamageSourcePredicate {
   public static final DamageSourcePredicate ANY = DamageSourcePredicate.Builder.damageType().build();
   @Nullable
   private final Boolean isProjectile;
   @Nullable
   private final Boolean isExplosion;
   @Nullable
   private final Boolean bypassesArmor;
   @Nullable
   private final Boolean bypassesInvulnerability;
   @Nullable
   private final Boolean bypassesMagic;
   @Nullable
   private final Boolean isFire;
   @Nullable
   private final Boolean isMagic;
   @Nullable
   private final Boolean isLightning;
   private final EntityPredicate directEntity;
   private final EntityPredicate sourceEntity;

   public DamageSourcePredicate(@Nullable Boolean pIsProjectile, @Nullable Boolean pIsExplosion, @Nullable Boolean pBypassesArmor, @Nullable Boolean pBypassesInvulnerability, @Nullable Boolean pBypassesMagic, @Nullable Boolean pIsFire, @Nullable Boolean pIsMagic, @Nullable Boolean pIsLightning, EntityPredicate pDirectEntity, EntityPredicate pSourceEntity) {
      this.isProjectile = pIsProjectile;
      this.isExplosion = pIsExplosion;
      this.bypassesArmor = pBypassesArmor;
      this.bypassesInvulnerability = pBypassesInvulnerability;
      this.bypassesMagic = pBypassesMagic;
      this.isFire = pIsFire;
      this.isMagic = pIsMagic;
      this.isLightning = pIsLightning;
      this.directEntity = pDirectEntity;
      this.sourceEntity = pSourceEntity;
   }

   public boolean matches(ServerPlayer pPlayer, DamageSource pSource) {
      return this.matches(pPlayer.getLevel(), pPlayer.position(), pSource);
   }

   public boolean matches(ServerLevel pLevel, Vec3 pPosition, DamageSource pSource) {
      if (this == ANY) {
         return true;
      } else if (this.isProjectile != null && this.isProjectile != pSource.isProjectile()) {
         return false;
      } else if (this.isExplosion != null && this.isExplosion != pSource.isExplosion()) {
         return false;
      } else if (this.bypassesArmor != null && this.bypassesArmor != pSource.isBypassArmor()) {
         return false;
      } else if (this.bypassesInvulnerability != null && this.bypassesInvulnerability != pSource.isBypassInvul()) {
         return false;
      } else if (this.bypassesMagic != null && this.bypassesMagic != pSource.isBypassMagic()) {
         return false;
      } else if (this.isFire != null && this.isFire != pSource.isFire()) {
         return false;
      } else if (this.isMagic != null && this.isMagic != pSource.isMagic()) {
         return false;
      } else if (this.isLightning != null && this.isLightning != (pSource == DamageSource.LIGHTNING_BOLT)) {
         return false;
      } else if (!this.directEntity.matches(pLevel, pPosition, pSource.getDirectEntity())) {
         return false;
      } else {
         return this.sourceEntity.matches(pLevel, pPosition, pSource.getEntity());
      }
   }

   public static DamageSourcePredicate fromJson(@Nullable JsonElement pJson) {
      if (pJson != null && !pJson.isJsonNull()) {
         JsonObject jsonobject = GsonHelper.convertToJsonObject(pJson, "damage type");
         Boolean obool = getOptionalBoolean(jsonobject, "is_projectile");
         Boolean obool1 = getOptionalBoolean(jsonobject, "is_explosion");
         Boolean obool2 = getOptionalBoolean(jsonobject, "bypasses_armor");
         Boolean obool3 = getOptionalBoolean(jsonobject, "bypasses_invulnerability");
         Boolean obool4 = getOptionalBoolean(jsonobject, "bypasses_magic");
         Boolean obool5 = getOptionalBoolean(jsonobject, "is_fire");
         Boolean obool6 = getOptionalBoolean(jsonobject, "is_magic");
         Boolean obool7 = getOptionalBoolean(jsonobject, "is_lightning");
         EntityPredicate entitypredicate = EntityPredicate.fromJson(jsonobject.get("direct_entity"));
         EntityPredicate entitypredicate1 = EntityPredicate.fromJson(jsonobject.get("source_entity"));
         return new DamageSourcePredicate(obool, obool1, obool2, obool3, obool4, obool5, obool6, obool7, entitypredicate, entitypredicate1);
      } else {
         return ANY;
      }
   }

   @Nullable
   private static Boolean getOptionalBoolean(JsonObject pJson, String pProperty) {
      return pJson.has(pProperty) ? GsonHelper.getAsBoolean(pJson, pProperty) : null;
   }

   public JsonElement serializeToJson() {
      if (this == ANY) {
         return JsonNull.INSTANCE;
      } else {
         JsonObject jsonobject = new JsonObject();
         this.addOptionally(jsonobject, "is_projectile", this.isProjectile);
         this.addOptionally(jsonobject, "is_explosion", this.isExplosion);
         this.addOptionally(jsonobject, "bypasses_armor", this.bypassesArmor);
         this.addOptionally(jsonobject, "bypasses_invulnerability", this.bypassesInvulnerability);
         this.addOptionally(jsonobject, "bypasses_magic", this.bypassesMagic);
         this.addOptionally(jsonobject, "is_fire", this.isFire);
         this.addOptionally(jsonobject, "is_magic", this.isMagic);
         this.addOptionally(jsonobject, "is_lightning", this.isLightning);
         jsonobject.add("direct_entity", this.directEntity.serializeToJson());
         jsonobject.add("source_entity", this.sourceEntity.serializeToJson());
         return jsonobject;
      }
   }

   private void addOptionally(JsonObject pJson, String pProperty, @Nullable Boolean pValue) {
      if (pValue != null) {
         pJson.addProperty(pProperty, pValue);
      }

   }

   public static class Builder {
      @Nullable
      private Boolean isProjectile;
      @Nullable
      private Boolean isExplosion;
      @Nullable
      private Boolean bypassesArmor;
      @Nullable
      private Boolean bypassesInvulnerability;
      @Nullable
      private Boolean bypassesMagic;
      @Nullable
      private Boolean isFire;
      @Nullable
      private Boolean isMagic;
      @Nullable
      private Boolean isLightning;
      private EntityPredicate directEntity = EntityPredicate.ANY;
      private EntityPredicate sourceEntity = EntityPredicate.ANY;

      public static DamageSourcePredicate.Builder damageType() {
         return new DamageSourcePredicate.Builder();
      }

      public DamageSourcePredicate.Builder isProjectile(Boolean pIsProjectile) {
         this.isProjectile = pIsProjectile;
         return this;
      }

      public DamageSourcePredicate.Builder isExplosion(Boolean pIsExplosion) {
         this.isExplosion = pIsExplosion;
         return this;
      }

      public DamageSourcePredicate.Builder bypassesArmor(Boolean pBypassesArmor) {
         this.bypassesArmor = pBypassesArmor;
         return this;
      }

      public DamageSourcePredicate.Builder bypassesInvulnerability(Boolean pBypassesInvulnerability) {
         this.bypassesInvulnerability = pBypassesInvulnerability;
         return this;
      }

      public DamageSourcePredicate.Builder bypassesMagic(Boolean pBypassesMagic) {
         this.bypassesMagic = pBypassesMagic;
         return this;
      }

      public DamageSourcePredicate.Builder isFire(Boolean pIsFire) {
         this.isFire = pIsFire;
         return this;
      }

      public DamageSourcePredicate.Builder isMagic(Boolean pIsMagic) {
         this.isMagic = pIsMagic;
         return this;
      }

      public DamageSourcePredicate.Builder isLightning(Boolean pIsLightning) {
         this.isLightning = pIsLightning;
         return this;
      }

      public DamageSourcePredicate.Builder direct(EntityPredicate pDirectEntity) {
         this.directEntity = pDirectEntity;
         return this;
      }

      public DamageSourcePredicate.Builder direct(EntityPredicate.Builder pDirectEntity) {
         this.directEntity = pDirectEntity.build();
         return this;
      }

      public DamageSourcePredicate.Builder source(EntityPredicate pSourceEntity) {
         this.sourceEntity = pSourceEntity;
         return this;
      }

      public DamageSourcePredicate.Builder source(EntityPredicate.Builder pSourceEntity) {
         this.sourceEntity = pSourceEntity.build();
         return this;
      }

      public DamageSourcePredicate build() {
         return new DamageSourcePredicate(this.isProjectile, this.isExplosion, this.bypassesArmor, this.bypassesInvulnerability, this.bypassesMagic, this.isFire, this.isMagic, this.isLightning, this.directEntity, this.sourceEntity);
      }
   }
}