package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V2684 extends NamespacedSchema {
   public V2684(int pVersionKey, Schema pParent) {
      super(pVersionKey, pParent);
   }

   public void registerTypes(Schema p_216760_, Map<String, Supplier<TypeTemplate>> p_216761_, Map<String, Supplier<TypeTemplate>> p_216762_) {
      super.registerTypes(p_216760_, p_216761_, p_216762_);
      p_216760_.registerType(false, References.GAME_EVENT_NAME, () -> {
         return DSL.constType(namespacedString());
      });
   }

   public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema pSchema) {
      Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(pSchema);
      pSchema.register(map, "minecraft:sculk_sensor", () -> {
         return DSL.optionalFields("listener", DSL.optionalFields("event", DSL.optionalFields("game_event", References.GAME_EVENT_NAME.in(pSchema))));
      });
      return map;
   }
}