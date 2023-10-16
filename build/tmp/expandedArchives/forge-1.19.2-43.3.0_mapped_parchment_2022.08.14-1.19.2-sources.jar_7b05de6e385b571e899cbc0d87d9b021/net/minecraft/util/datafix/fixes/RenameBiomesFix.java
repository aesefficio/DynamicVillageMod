package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import java.util.Map;
import java.util.Objects;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class RenameBiomesFix extends DataFix {
   private final String name;
   private final Map<String, String> biomes;

   public RenameBiomesFix(Schema pOutputSchema, boolean pChangesType, String pName, Map<String, String> pBiomes) {
      super(pOutputSchema, pChangesType);
      this.biomes = pBiomes;
      this.name = pName;
   }

   protected TypeRewriteRule makeRule() {
      Type<Pair<String, String>> type = DSL.named(References.BIOME.typeName(), NamespacedSchema.namespacedString());
      if (!Objects.equals(type, this.getInputSchema().getType(References.BIOME))) {
         throw new IllegalStateException("Biome type is not what was expected.");
      } else {
         return this.fixTypeEverywhere(this.name, type, (p_16844_) -> {
            return (p_145634_) -> {
               return p_145634_.mapSecond((p_145636_) -> {
                  return this.biomes.getOrDefault(p_145636_, p_145636_);
               });
            };
         });
      }
   }
}