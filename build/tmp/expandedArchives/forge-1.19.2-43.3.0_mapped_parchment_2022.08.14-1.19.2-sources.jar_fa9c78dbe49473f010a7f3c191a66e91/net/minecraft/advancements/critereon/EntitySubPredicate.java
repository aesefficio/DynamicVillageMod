package net.minecraft.advancements.critereon;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.CatVariant;
import net.minecraft.world.entity.animal.FrogVariant;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.phys.Vec3;

public interface EntitySubPredicate {
   EntitySubPredicate ANY = new EntitySubPredicate() {
      public boolean matches(Entity p_218841_, ServerLevel p_218842_, @Nullable Vec3 p_218843_) {
         return true;
      }

      public JsonObject serializeCustomData() {
         return new JsonObject();
      }

      public EntitySubPredicate.Type type() {
         return EntitySubPredicate.Types.ANY;
      }
   };

   static EntitySubPredicate fromJson(@Nullable JsonElement p_218836_) {
      if (p_218836_ != null && !p_218836_.isJsonNull()) {
         JsonObject jsonobject = GsonHelper.convertToJsonObject(p_218836_, "type_specific");
         String s = GsonHelper.getAsString(jsonobject, "type", (String)null);
         if (s == null) {
            return ANY;
         } else {
            EntitySubPredicate.Type entitysubpredicate$type = EntitySubPredicate.Types.TYPES.get(s);
            if (entitysubpredicate$type == null) {
               throw new JsonSyntaxException("Unknown sub-predicate type: " + s);
            } else {
               return entitysubpredicate$type.deserialize(jsonobject);
            }
         }
      } else {
         return ANY;
      }
   }

   boolean matches(Entity pEntity, ServerLevel pLevel, @Nullable Vec3 p_218830_);

   JsonObject serializeCustomData();

   default JsonElement serialize() {
      if (this.type() == EntitySubPredicate.Types.ANY) {
         return JsonNull.INSTANCE;
      } else {
         JsonObject jsonobject = this.serializeCustomData();
         String s = EntitySubPredicate.Types.TYPES.inverse().get(this.type());
         jsonobject.addProperty("type", s);
         return jsonobject;
      }
   }

   EntitySubPredicate.Type type();

   static EntitySubPredicate variant(CatVariant p_218832_) {
      return EntitySubPredicate.Types.CAT.createPredicate(p_218832_);
   }

   static EntitySubPredicate variant(FrogVariant p_218834_) {
      return EntitySubPredicate.Types.FROG.createPredicate(p_218834_);
   }

   public interface Type {
      EntitySubPredicate deserialize(JsonObject p_218846_);
   }

   public static final class Types {
      public static final EntitySubPredicate.Type ANY = (p_218860_) -> {
         return EntitySubPredicate.ANY;
      };
      public static final EntitySubPredicate.Type LIGHTNING = LighthingBoltPredicate::fromJson;
      public static final EntitySubPredicate.Type FISHING_HOOK = FishingHookPredicate::fromJson;
      public static final EntitySubPredicate.Type PLAYER = PlayerPredicate::fromJson;
      public static final EntitySubPredicate.Type SLIME = SlimePredicate::fromJson;
      public static final EntityVariantPredicate<CatVariant> CAT = EntityVariantPredicate.create(Registry.CAT_VARIANT, (p_218862_) -> {
         Optional optional;
         if (p_218862_ instanceof Cat cat) {
            optional = Optional.of(cat.getCatVariant());
         } else {
            optional = Optional.empty();
         }

         return optional;
      });
      public static final EntityVariantPredicate<FrogVariant> FROG = EntityVariantPredicate.create(Registry.FROG_VARIANT, (p_218858_) -> {
         Optional optional;
         if (p_218858_ instanceof Frog frog) {
            optional = Optional.of(frog.getVariant());
         } else {
            optional = Optional.empty();
         }

         return optional;
      });
      public static final BiMap<String, EntitySubPredicate.Type> TYPES = ImmutableBiMap.of("any", ANY, "lightning", LIGHTNING, "fishing_hook", FISHING_HOOK, "player", PLAYER, "slime", SLIME, "cat", CAT.type(), "frog", FROG.type());
   }
}