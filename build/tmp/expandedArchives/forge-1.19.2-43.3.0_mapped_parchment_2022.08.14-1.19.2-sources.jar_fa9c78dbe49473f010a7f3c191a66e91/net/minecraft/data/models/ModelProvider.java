package net.minecraft.data.models;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.models.blockstates.BlockStateGenerator;
import net.minecraft.data.models.model.DelegatedModel;
import net.minecraft.data.models.model.ModelLocationUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.slf4j.Logger;

public class ModelProvider implements DataProvider {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final DataGenerator.PathProvider blockStatePathProvider;
   private final DataGenerator.PathProvider modelPathProvider;

   public ModelProvider(DataGenerator pGenerator) {
      this.blockStatePathProvider = pGenerator.createPathProvider(DataGenerator.Target.RESOURCE_PACK, "blockstates");
      this.modelPathProvider = pGenerator.createPathProvider(DataGenerator.Target.RESOURCE_PACK, "models");
   }

   public void run(CachedOutput pOutput) {
      Map<Block, BlockStateGenerator> map = Maps.newHashMap();
      Consumer<BlockStateGenerator> consumer = (p_125120_) -> {
         Block block = p_125120_.getBlock();
         BlockStateGenerator blockstategenerator = map.put(block, p_125120_);
         if (blockstategenerator != null) {
            throw new IllegalStateException("Duplicate blockstate definition for " + block);
         }
      };
      Map<ResourceLocation, Supplier<JsonElement>> map1 = Maps.newHashMap();
      Set<Item> set = Sets.newHashSet();
      BiConsumer<ResourceLocation, Supplier<JsonElement>> biconsumer = (p_125123_, p_125124_) -> {
         Supplier<JsonElement> supplier = map1.put(p_125123_, p_125124_);
         if (supplier != null) {
            throw new IllegalStateException("Duplicate model definition for " + p_125123_);
         }
      };
      Consumer<Item> consumer1 = set::add;
      (new BlockModelGenerators(consumer, biconsumer, consumer1)).run();
      (new ItemModelGenerators(biconsumer)).run();
      List<Block> list = Registry.BLOCK.stream().filter((p_125117_) -> {
         return !map.containsKey(p_125117_);
      }).toList();
      if (!list.isEmpty()) {
         throw new IllegalStateException("Missing blockstate definitions for: " + list);
      } else {
         Registry.BLOCK.forEach((p_125128_) -> {
            Item item = Item.BY_BLOCK.get(p_125128_);
            if (item != null) {
               if (set.contains(item)) {
                  return;
               }

               ResourceLocation resourcelocation = ModelLocationUtils.getModelLocation(item);
               if (!map1.containsKey(resourcelocation)) {
                  map1.put(resourcelocation, new DelegatedModel(ModelLocationUtils.getModelLocation(p_125128_)));
               }
            }

         });
         this.saveCollection(pOutput, map, (p_236328_) -> {
            return this.blockStatePathProvider.json(p_236328_.builtInRegistryHolder().key().location());
         });
         this.saveCollection(pOutput, map1, this.modelPathProvider::json);
      }
   }

   private <T> void saveCollection(CachedOutput pOutput, Map<T, ? extends Supplier<JsonElement>> pObjectToJsonMap, Function<T, Path> pResolveObjectPath) {
      pObjectToJsonMap.forEach((p_236338_, p_236339_) -> {
         Path path = pResolveObjectPath.apply(p_236338_);

         try {
            DataProvider.saveStable(pOutput, p_236339_.get(), path);
         } catch (Exception exception) {
            LOGGER.error("Couldn't save {}", path, exception);
         }

      });
   }

   /**
    * Gets a name for this provider, to use in logging.
    */
   public String getName() {
      return "Block State Definitions";
   }
}