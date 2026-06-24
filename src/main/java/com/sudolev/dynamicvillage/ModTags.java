package com.sudolev.dynamicvillage;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class ModTags {
   public static class Blocks {
      private static TagKey<Block> tag(String name) {
         return BlockTags.create(ResourceLocation.fromNamespaceAndPath("dynamicvillage", name));
      }

      private static TagKey<Block> forgeTag(String name) {
         return BlockTags.create(ResourceLocation.fromNamespaceAndPath("forge", name));
      }
   }
}
