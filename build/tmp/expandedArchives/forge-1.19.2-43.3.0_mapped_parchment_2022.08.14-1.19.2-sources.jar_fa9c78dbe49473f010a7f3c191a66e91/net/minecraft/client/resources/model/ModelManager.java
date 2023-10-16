package net.minecraft.client.resources.model;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.texture.AtlasSet;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ModelManager extends SimplePreparableReloadListener<ModelBakery> implements AutoCloseable {
   private Map<ResourceLocation, BakedModel> bakedRegistry = new java.util.HashMap<>();
   @Nullable
   private AtlasSet atlases;
   private final BlockModelShaper blockModelShaper;
   private final TextureManager textureManager;
   private final BlockColors blockColors;
   private int maxMipmapLevels;
   private BakedModel missingModel;
   private Object2IntMap<BlockState> modelGroups;
   private ModelBakery modelBakery;

   public ModelManager(TextureManager pTextureManager, BlockColors pBlockColors, int pMaxMipmapLevels) {
      this.textureManager = pTextureManager;
      this.blockColors = pBlockColors;
      this.maxMipmapLevels = pMaxMipmapLevels;
      this.blockModelShaper = new BlockModelShaper(this);
   }

   public BakedModel getModel(ResourceLocation modelLocation) {
      return this.bakedRegistry.getOrDefault(modelLocation, this.missingModel);
   }

   public BakedModel getModel(ModelResourceLocation pModelLocation) {
      return this.bakedRegistry.getOrDefault(pModelLocation, this.missingModel);
   }

   public BakedModel getMissingModel() {
      return this.missingModel;
   }

   public BlockModelShaper getBlockModelShaper() {
      return this.blockModelShaper;
   }

   /**
    * Performs any reloading that can be done off-thread, such as file IO
    */
   protected ModelBakery prepare(ResourceManager pResourceManager, ProfilerFiller pProfiler) {
      pProfiler.startTick();
      ModelBakery modelbakery = new ModelBakery(pResourceManager, this.blockColors, pProfiler, this.maxMipmapLevels);
      pProfiler.endTick();
      return modelbakery;
   }

   protected void apply(ModelBakery pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
      pProfiler.startTick();
      pProfiler.push("upload");
      if (this.atlases != null) {
         this.atlases.close();
      }

      this.atlases = pObject.uploadTextures(this.textureManager, pProfiler);
      this.bakedRegistry = pObject.getBakedTopLevelModels();
      this.modelGroups = pObject.getModelGroups();
      this.missingModel = this.bakedRegistry.get(ModelBakery.MISSING_MODEL_LOCATION);
      this.modelBakery = pObject;
      net.minecraftforge.client.ForgeHooksClient.onModelBake(this, this.bakedRegistry, pObject);
      pProfiler.popPush("cache");
      this.blockModelShaper.rebuildCache();
      pProfiler.pop();
      pProfiler.endTick();
   }

   public boolean requiresRender(BlockState pOldState, BlockState pNewState) {
      if (pOldState == pNewState) {
         return false;
      } else {
         int i = this.modelGroups.getInt(pOldState);
         if (i != -1) {
            int j = this.modelGroups.getInt(pNewState);
            if (i == j) {
               FluidState fluidstate = pOldState.getFluidState();
               FluidState fluidstate1 = pNewState.getFluidState();
               return fluidstate != fluidstate1;
            }
         }

         return true;
      }
   }

   public TextureAtlas getAtlas(ResourceLocation pLocation) {
      if (this.atlases == null) throw new RuntimeException("getAtlasTexture called too early!");
      return this.atlases.getAtlas(pLocation);
   }

   public void close() {
      if (this.atlases != null) {
         this.atlases.close();
      }

   }

   public void updateMaxMipLevel(int pLevel) {
      this.maxMipmapLevels = pLevel;
   }

   public ModelBakery getModelBakery() {
      return com.google.common.base.Preconditions.checkNotNull(modelBakery, "Attempted to query model bakery before it has been initialized.");
   }
}
