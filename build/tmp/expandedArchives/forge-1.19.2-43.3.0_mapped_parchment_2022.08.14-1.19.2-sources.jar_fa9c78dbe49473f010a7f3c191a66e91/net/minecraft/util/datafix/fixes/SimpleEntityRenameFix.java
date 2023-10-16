package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;

public abstract class SimpleEntityRenameFix extends EntityRenameFix {
   public SimpleEntityRenameFix(String pName, Schema pOutputSchema, boolean pChangesType) {
      super(pName, pOutputSchema, pChangesType);
   }

   protected Pair<String, Typed<?>> fix(String p_16905_, Typed<?> p_16906_) {
      Pair<String, Dynamic<?>> pair = this.getNewNameAndTag(p_16905_, p_16906_.getOrCreate(DSL.remainderFinder()));
      return Pair.of(pair.getFirst(), p_16906_.set(DSL.remainderFinder(), pair.getSecond()));
   }

   protected abstract Pair<String, Dynamic<?>> getNewNameAndTag(String pName, Dynamic<?> pTag);
}