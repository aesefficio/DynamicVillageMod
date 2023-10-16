package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V3081 extends NamespacedSchema {
   public V3081(int p_216784_, Schema p_216785_) {
      super(p_216784_, p_216785_);
   }

   protected static void registerMob(Schema p_216789_, Map<String, Supplier<TypeTemplate>> p_216790_, String p_216791_) {
      p_216789_.register(p_216790_, p_216791_, () -> {
         return V100.equipment(p_216789_);
      });
      p_216789_.register(p_216790_, "minecraft:warden", () -> {
         return DSL.optionalFields("ArmorItems", DSL.list(References.ITEM_STACK.in(p_216789_)), "HandItems", DSL.list(References.ITEM_STACK.in(p_216789_)), "listener", DSL.optionalFields("event", DSL.optionalFields("game_event", References.GAME_EVENT_NAME.in(p_216789_))));
      });
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_216795_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerEntities(p_216795_);
      registerMob(p_216795_, map, "minecraft:warden");
      return map;
   }
}