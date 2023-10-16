package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import java.util.Objects;
import java.util.function.Function;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class RecipesRenameFix extends DataFix {
   private final String name;
   private final Function<String, String> renamer;

   public RecipesRenameFix(Schema pOutputSchema, boolean pChangesType, String pName, Function<String, String> pRenamer) {
      super(pOutputSchema, pChangesType);
      this.name = pName;
      this.renamer = pRenamer;
   }

   protected TypeRewriteRule makeRule() {
      Type<Pair<String, String>> type = DSL.named(References.RECIPE.typeName(), NamespacedSchema.namespacedString());
      if (!Objects.equals(type, this.getInputSchema().getType(References.RECIPE))) {
         throw new IllegalStateException("Recipe type is not what was expected.");
      } else {
         return this.fixTypeEverywhere(this.name, type, (p_16739_) -> {
            return (p_145615_) -> {
               return p_145615_.mapSecond(this.renamer);
            };
         });
      }
   }
}