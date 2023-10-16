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

public class SimpleRenameFix extends DataFix {
   private final String fixerName;
   private final Map<String, String> nameMapping;
   private final DSL.TypeReference typeReference;

   public SimpleRenameFix(Schema p_216730_, DSL.TypeReference p_216731_, Map<String, String> p_216732_) {
      this(p_216730_, p_216731_, p_216731_.typeName() + "-renames at version: " + p_216730_.getVersionKey(), p_216732_);
   }

   public SimpleRenameFix(Schema p_216725_, DSL.TypeReference p_216726_, String p_216727_, Map<String, String> p_216728_) {
      super(p_216725_, false);
      this.nameMapping = p_216728_;
      this.fixerName = p_216727_;
      this.typeReference = p_216726_;
   }

   protected TypeRewriteRule makeRule() {
      Type<Pair<String, String>> type = DSL.named(this.typeReference.typeName(), NamespacedSchema.namespacedString());
      if (!Objects.equals(type, this.getInputSchema().getType(this.typeReference))) {
         throw new IllegalStateException("\"" + this.typeReference.typeName() + "\" type is not what was expected.");
      } else {
         return this.fixTypeEverywhere(this.fixerName, type, (p_216736_) -> {
            return (p_216734_) -> {
               return p_216734_.mapSecond((p_216738_) -> {
                  return this.nameMapping.getOrDefault(p_216738_, p_216738_);
               });
            };
         });
      }
   }
}