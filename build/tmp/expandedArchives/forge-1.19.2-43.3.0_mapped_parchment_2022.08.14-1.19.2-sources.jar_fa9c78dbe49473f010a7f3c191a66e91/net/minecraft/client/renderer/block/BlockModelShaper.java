package net.minecraft.client.renderer.block;

import com.google.common.collect.Maps;
import java.util.Map;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BlockModelShaper {
   private final Map<BlockState, BakedModel> modelByStateCache = Maps.newIdentityHashMap();
   private final ModelManager modelManager;

   public BlockModelShaper(ModelManager pModelManager) {
      this.modelManager = pModelManager;
   }

   @Deprecated
   public TextureAtlasSprite getParticleIcon(BlockState pState) {
      return this.getBlockModel(pState).getParticleIcon(net.minecraftforge.client.model.data.ModelData.EMPTY);
   }

   public TextureAtlasSprite getTexture(BlockState pState, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
      var data = level.getModelDataManager().getAt(pos);
      BakedModel model = this.getBlockModel(pState);
      return model.getParticleIcon(model.getModelData(level, pos, pState, data == null ? net.minecraftforge.client.model.data.ModelData.EMPTY : data));
   }

   public BakedModel getBlockModel(BlockState pState) {
      BakedModel bakedmodel = this.modelByStateCache.get(pState);
      if (bakedmodel == null) {
         bakedmodel = this.modelManager.getMissingModel();
      }

      return bakedmodel;
   }

   public ModelManager getModelManager() {
      return this.modelManager;
   }

   public void rebuildCache() {
      this.modelByStateCache.clear();

      for(Block block : Registry.BLOCK) {
         block.getStateDefinition().getPossibleStates().forEach((p_110898_) -> {
            this.modelByStateCache.put(p_110898_, this.modelManager.getModel(stateToModelLocation(p_110898_)));
         });
      }

   }

   public static ModelResourceLocation stateToModelLocation(BlockState pState) {
      return stateToModelLocation(Registry.BLOCK.getKey(pState.getBlock()), pState);
   }

   public static ModelResourceLocation stateToModelLocation(ResourceLocation pLocation, BlockState pState) {
      return new ModelResourceLocation(pLocation, statePropertiesToString(pState.getValues()));
   }

   public static String statePropertiesToString(Map<Property<?>, Comparable<?>> pPropertyValues) {
      StringBuilder stringbuilder = new StringBuilder();

      for(Map.Entry<Property<?>, Comparable<?>> entry : pPropertyValues.entrySet()) {
         if (stringbuilder.length() != 0) {
            stringbuilder.append(',');
         }

         Property<?> property = entry.getKey();
         stringbuilder.append(property.getName());
         stringbuilder.append('=');
         stringbuilder.append(getValue(property, entry.getValue()));
      }

      return stringbuilder.toString();
   }

   private static <T extends Comparable<T>> String getValue(Property<T> pProperty, Comparable<?> pValue) {
      return pProperty.getName((T)pValue);
   }
}
