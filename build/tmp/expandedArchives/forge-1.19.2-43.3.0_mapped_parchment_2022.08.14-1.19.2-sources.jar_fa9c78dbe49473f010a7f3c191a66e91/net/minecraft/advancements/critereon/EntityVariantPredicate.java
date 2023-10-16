package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class EntityVariantPredicate<V> {
   private static final String VARIANT_KEY = "variant";
   final Registry<V> registry;
   final Function<Entity, Optional<V>> getter;
   final EntitySubPredicate.Type type;

   public static <V> EntityVariantPredicate<V> create(Registry<V> p_219094_, Function<Entity, Optional<V>> p_219095_) {
      return new EntityVariantPredicate<>(p_219094_, p_219095_);
   }

   private EntityVariantPredicate(Registry<V> p_219087_, Function<Entity, Optional<V>> p_219088_) {
      this.registry = p_219087_;
      this.getter = p_219088_;
      this.type = (p_219092_) -> {
         String s = GsonHelper.getAsString(p_219092_, "variant");
         V v = p_219087_.get(ResourceLocation.tryParse(s));
         if (v == null) {
            throw new JsonSyntaxException("Unknown variant: " + s);
         } else {
            return this.createPredicate(v);
         }
      };
   }

   public EntitySubPredicate.Type type() {
      return this.type;
   }

   public EntitySubPredicate createPredicate(final V p_219097_) {
      return new EntitySubPredicate() {
         public boolean matches(Entity p_219105_, ServerLevel p_219106_, @Nullable Vec3 p_219107_) {
            return EntityVariantPredicate.this.getter.apply(p_219105_).filter((p_219110_) -> {
               return p_219110_.equals(p_219097_);
            }).isPresent();
         }

         public JsonObject serializeCustomData() {
            JsonObject jsonobject = new JsonObject();
            jsonobject.addProperty("variant", EntityVariantPredicate.this.registry.getKey(p_219097_).toString());
            return jsonobject;
         }

         public EntitySubPredicate.Type type() {
            return EntityVariantPredicate.this.type;
         }
      };
   }
}