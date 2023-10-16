package net.minecraft.world.item;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;

public class AdventureModeCheck {
   private final String tagName;
   @Nullable
   private BlockInWorld lastCheckedBlock;
   private boolean lastResult;
   private boolean checksBlockEntity;

   public AdventureModeCheck(String pTagName) {
      this.tagName = pTagName;
   }

   private static boolean areSameBlocks(BlockInWorld pBlock, @Nullable BlockInWorld pOther, boolean pOverride) {
      if (pOther != null && pBlock.getState() == pOther.getState()) {
         if (!pOverride) {
            return true;
         } else if (pBlock.getEntity() == null && pOther.getEntity() == null) {
            return true;
         } else {
            return pBlock.getEntity() != null && pOther.getEntity() != null ? Objects.equals(pBlock.getEntity().saveWithId(), pOther.getEntity().saveWithId()) : false;
         }
      } else {
         return false;
      }
   }

   public boolean test(ItemStack p_204086_, Registry<Block> p_204087_, BlockInWorld p_204088_) {
      if (areSameBlocks(p_204088_, this.lastCheckedBlock, this.checksBlockEntity)) {
         return this.lastResult;
      } else {
         this.lastCheckedBlock = p_204088_;
         this.checksBlockEntity = false;
         CompoundTag compoundtag = p_204086_.getTag();
         if (compoundtag != null && compoundtag.contains(this.tagName, 9)) {
            ListTag listtag = compoundtag.getList(this.tagName, 8);

            for(int i = 0; i < listtag.size(); ++i) {
               String s = listtag.getString(i);

               try {
                  BlockPredicateArgument.Result blockpredicateargument$result = BlockPredicateArgument.parse(HolderLookup.forRegistry(p_204087_), new StringReader(s));
                  this.checksBlockEntity |= blockpredicateargument$result.requiresNbt();
                  if (blockpredicateargument$result.test(p_204088_)) {
                     this.lastResult = true;
                     return true;
                  }
               } catch (CommandSyntaxException commandsyntaxexception) {
               }
            }
         }

         this.lastResult = false;
         return false;
      }
   }
}