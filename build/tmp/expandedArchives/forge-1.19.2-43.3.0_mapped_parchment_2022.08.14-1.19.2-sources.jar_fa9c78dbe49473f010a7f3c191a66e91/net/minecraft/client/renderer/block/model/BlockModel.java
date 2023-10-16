package net.minecraft.client.renderer.block.model;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BuiltInModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class BlockModel implements UnbakedModel {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final FaceBakery FACE_BAKERY = new FaceBakery();
   @VisibleForTesting
   static final Gson GSON = (new GsonBuilder()).registerTypeAdapter(BlockModel.class, new BlockModel.Deserializer()).registerTypeAdapter(BlockElement.class, new BlockElement.Deserializer()).registerTypeAdapter(BlockElementFace.class, new BlockElementFace.Deserializer()).registerTypeAdapter(BlockFaceUV.class, new BlockFaceUV.Deserializer()).registerTypeAdapter(ItemTransform.class, new ItemTransform.Deserializer()).registerTypeAdapter(ItemTransforms.class, new ItemTransforms.Deserializer()).registerTypeAdapter(ItemOverride.class, new ItemOverride.Deserializer()).create();
   private static final char REFERENCE_CHAR = '#';
   public static final String PARTICLE_TEXTURE_REFERENCE = "particle";
   private final List<BlockElement> elements;
   @Nullable
   private final BlockModel.GuiLight guiLight;
   public final boolean hasAmbientOcclusion;
   private final ItemTransforms transforms;
   private final List<ItemOverride> overrides;
   public String name = "";
   @VisibleForTesting
   public final Map<String, Either<Material, String>> textureMap;
   @Nullable
   public BlockModel parent;
   @Nullable
   protected ResourceLocation parentLocation;
   public final net.minecraftforge.client.model.geometry.BlockGeometryBakingContext customData = new net.minecraftforge.client.model.geometry.BlockGeometryBakingContext(this);

   public static BlockModel fromStream(Reader pReader) {
      return GsonHelper.fromJson(net.minecraftforge.client.model.ExtendedBlockModelDeserializer.INSTANCE, pReader, BlockModel.class);
   }

   public static BlockModel fromString(String pJsonString) {
      return fromStream(new StringReader(pJsonString));
   }

   public BlockModel(@Nullable ResourceLocation pParentLocation, List<BlockElement> pElements, Map<String, Either<Material, String>> pTextureMap, boolean pHasAmbientOcclusion, @Nullable BlockModel.GuiLight pGuiLight, ItemTransforms pTransforms, List<ItemOverride> pOverrides) {
      this.elements = pElements;
      this.hasAmbientOcclusion = pHasAmbientOcclusion;
      this.guiLight = pGuiLight;
      this.textureMap = pTextureMap;
      this.parentLocation = pParentLocation;
      this.transforms = pTransforms;
      this.overrides = pOverrides;
   }

   @Deprecated
   public List<BlockElement> getElements() {
      if (customData.hasCustomGeometry()) return java.util.Collections.emptyList();
      return this.elements.isEmpty() && this.parent != null ? this.parent.getElements() : this.elements;
   }

   @Nullable
   public ResourceLocation getParentLocation() { return parentLocation; }

   public boolean hasAmbientOcclusion() {
      return this.parent != null ? this.parent.hasAmbientOcclusion() : this.hasAmbientOcclusion;
   }

   public BlockModel.GuiLight getGuiLight() {
      if (this.guiLight != null) {
         return this.guiLight;
      } else {
         return this.parent != null ? this.parent.getGuiLight() : BlockModel.GuiLight.SIDE;
      }
   }

   public boolean isResolved() {
      return this.parentLocation == null || this.parent != null && this.parent.isResolved();
   }

   public List<ItemOverride> getOverrides() {
      return this.overrides;
   }

   private ItemOverrides getItemOverrides(ModelBakery pModelBakery, BlockModel pModel) {
      return this.overrides.isEmpty() ? ItemOverrides.EMPTY : new ItemOverrides(pModelBakery, pModel, pModelBakery::getModel, this.overrides);
   }

   public ItemOverrides getOverrides(ModelBakery pModelBakery, BlockModel pModel, Function<Material, TextureAtlasSprite> textureGetter) {
      return this.overrides.isEmpty() ? ItemOverrides.EMPTY : new ItemOverrides(pModelBakery, pModel, pModelBakery::getModel, textureGetter, this.overrides);
   }

   public Collection<ResourceLocation> getDependencies() {
      Set<ResourceLocation> set = Sets.newHashSet();

      for(ItemOverride itemoverride : this.overrides) {
         set.add(itemoverride.getModel());
      }

      if (this.parentLocation != null) {
         set.add(this.parentLocation);
      }

      return set;
   }

   public Collection<Material> getMaterials(Function<ResourceLocation, UnbakedModel> pModelGetter, Set<Pair<String, String>> pMissingTextureErrors) {
      Set<UnbakedModel> set = Sets.newLinkedHashSet();

      for(BlockModel blockmodel = this; blockmodel.parentLocation != null && blockmodel.parent == null; blockmodel = blockmodel.parent) {
         set.add(blockmodel);
         UnbakedModel unbakedmodel = pModelGetter.apply(blockmodel.parentLocation);
         if (unbakedmodel == null) {
            LOGGER.warn("No parent '{}' while loading model '{}'", this.parentLocation, blockmodel);
         }

         if (set.contains(unbakedmodel)) {
            LOGGER.warn("Found 'parent' loop while loading model '{}' in chain: {} -> {}", blockmodel, set.stream().map(Object::toString).collect(Collectors.joining(" -> ")), this.parentLocation);
            unbakedmodel = null;
         }

         if (unbakedmodel == null) {
            blockmodel.parentLocation = ModelBakery.MISSING_MODEL_LOCATION;
            unbakedmodel = pModelGetter.apply(blockmodel.parentLocation);
         }

         if (!(unbakedmodel instanceof BlockModel)) {
            throw new IllegalStateException("BlockModel parent has to be a block model.");
         }

         blockmodel.parent = (BlockModel)unbakedmodel;
      }

      Set<Material> set1 = Sets.newHashSet(this.getMaterial("particle"));

      if (customData.hasCustomGeometry())
         set1.addAll(customData.getTextureDependencies(pModelGetter, pMissingTextureErrors));
      else
      for(BlockElement blockelement : this.getElements()) {
         for(BlockElementFace blockelementface : blockelement.faces.values()) {
            Material material = this.getMaterial(blockelementface.texture);
            if (Objects.equals(material.texture(), MissingTextureAtlasSprite.getLocation())) {
               pMissingTextureErrors.add(Pair.of(blockelementface.texture, this.name));
            }

            set1.add(material);
         }
      }

      this.overrides.forEach((p_111475_) -> {
         UnbakedModel unbakedmodel1 = pModelGetter.apply(p_111475_.getModel());
         if (!Objects.equals(unbakedmodel1, this)) {
            set1.addAll(unbakedmodel1.getMaterials(pModelGetter, pMissingTextureErrors));
         }
      });
      if (this.getRootModel() == ModelBakery.GENERATION_MARKER) {
         ItemModelGenerator.LAYERS.forEach((p_111467_) -> {
            set1.add(this.getMaterial(p_111467_));
         });
      }

      return set1;
   }

   @Deprecated //Forge: Use Boolean variant
   public BakedModel bake(ModelBakery pModelBakery, Function<Material, TextureAtlasSprite> pSpriteGetter, ModelState pTransform, ResourceLocation pLocation) {
      return this.bake(pModelBakery, this, pSpriteGetter, pTransform, pLocation, true);
   }

   public BakedModel bake(ModelBakery pBakery, BlockModel pModel, Function<Material, TextureAtlasSprite> pSpriteGetter, ModelState pTransform, ResourceLocation pLocation, boolean pGuiLight3d) {
      return net.minecraftforge.client.model.geometry.UnbakedGeometryHelper.bake(this, pBakery, pModel, pSpriteGetter, pTransform, pLocation, pGuiLight3d);
   }

   @Deprecated(forRemoval = true, since = "1.19.2") // TODO: We cannot remove it since it's vanilla code, but make it private in 1.20
   public BakedModel bakeVanilla(ModelBakery pBakery, BlockModel pModel, Function<Material, TextureAtlasSprite> pSpriteGetter, ModelState pTransform, ResourceLocation pLocation, boolean pGuiLight3d, net.minecraftforge.client.RenderTypeGroup renderTypes) {
      if (true) return net.minecraftforge.client.model.geometry.UnbakedGeometryHelper.bakeVanilla(this, pBakery, pModel, pSpriteGetter, pTransform, pLocation); // Redirected, in case anyone actually uses this
      TextureAtlasSprite textureatlassprite = pSpriteGetter.apply(this.getMaterial("particle"));
      if (this.getRootModel() == ModelBakery.BLOCK_ENTITY_MARKER) {
         return new BuiltInModel(this.getTransforms(), this.getItemOverrides(pBakery, pModel), textureatlassprite, this.getGuiLight().lightLikeBlock());
      } else {
         SimpleBakedModel.Builder simplebakedmodel$builder = (new SimpleBakedModel.Builder(this, this.getItemOverrides(pBakery, pModel), pGuiLight3d)).particle(textureatlassprite);

         for(BlockElement blockelement : this.getElements()) {
            for(Direction direction : blockelement.faces.keySet()) {
               BlockElementFace blockelementface = blockelement.faces.get(direction);
               TextureAtlasSprite textureatlassprite1 = pSpriteGetter.apply(this.getMaterial(blockelementface.texture));
               if (blockelementface.cullForDirection == null) {
                  simplebakedmodel$builder.addUnculledFace(bakeFace(blockelement, blockelementface, textureatlassprite1, direction, pTransform, pLocation));
               } else {
                  simplebakedmodel$builder.addCulledFace(Direction.rotate(pTransform.getRotation().getMatrix(), blockelementface.cullForDirection), bakeFace(blockelement, blockelementface, textureatlassprite1, direction, pTransform, pLocation));
               }
            }
         }

         return simplebakedmodel$builder.build();
      }
   }

   public static BakedQuad bakeFace(BlockElement pPart, BlockElementFace pPartFace, TextureAtlasSprite pSprite, Direction pDirection, ModelState pTransform, ResourceLocation pLocation) {
      return FACE_BAKERY.bakeQuad(pPart.from, pPart.to, pPartFace, pSprite, pDirection, pTransform, pPart.rotation, pPart.shade, pLocation);
   }

   public boolean hasTexture(String pTextureName) {
      return !MissingTextureAtlasSprite.getLocation().equals(this.getMaterial(pTextureName).texture());
   }

   public Material getMaterial(String pName) {
      if (isTextureReference(pName)) {
         pName = pName.substring(1);
      }

      List<String> list = Lists.newArrayList();

      while(true) {
         Either<Material, String> either = this.findTextureEntry(pName);
         Optional<Material> optional = either.left();
         if (optional.isPresent()) {
            return optional.get();
         }

         pName = either.right().get();
         if (list.contains(pName)) {
            LOGGER.warn("Unable to resolve texture due to reference chain {}->{} in {}", Joiner.on("->").join(list), pName, this.name);
            return new Material(TextureAtlas.LOCATION_BLOCKS, MissingTextureAtlasSprite.getLocation());
         }

         list.add(pName);
      }
   }

   private Either<Material, String> findTextureEntry(String pName) {
      for(BlockModel blockmodel = this; blockmodel != null; blockmodel = blockmodel.parent) {
         Either<Material, String> either = blockmodel.textureMap.get(pName);
         if (either != null) {
            return either;
         }
      }

      return Either.left(new Material(TextureAtlas.LOCATION_BLOCKS, MissingTextureAtlasSprite.getLocation()));
   }

   static boolean isTextureReference(String pStr) {
      return pStr.charAt(0) == '#';
   }

   public BlockModel getRootModel() {
      return this.parent == null ? this : this.parent.getRootModel();
   }

   public ItemTransforms getTransforms() {
      ItemTransform itemtransform = this.getTransform(ItemTransforms.TransformType.THIRD_PERSON_LEFT_HAND);
      ItemTransform itemtransform1 = this.getTransform(ItemTransforms.TransformType.THIRD_PERSON_RIGHT_HAND);
      ItemTransform itemtransform2 = this.getTransform(ItemTransforms.TransformType.FIRST_PERSON_LEFT_HAND);
      ItemTransform itemtransform3 = this.getTransform(ItemTransforms.TransformType.FIRST_PERSON_RIGHT_HAND);
      ItemTransform itemtransform4 = this.getTransform(ItemTransforms.TransformType.HEAD);
      ItemTransform itemtransform5 = this.getTransform(ItemTransforms.TransformType.GUI);
      ItemTransform itemtransform6 = this.getTransform(ItemTransforms.TransformType.GROUND);
      ItemTransform itemtransform7 = this.getTransform(ItemTransforms.TransformType.FIXED);

      var builder = com.google.common.collect.ImmutableMap.<ItemTransforms.TransformType, ItemTransform>builder();
      for(ItemTransforms.TransformType type : ItemTransforms.TransformType.values()) {
         if (type.isModded()) {
            var transform = this.getTransform(type);
            if (transform != ItemTransform.NO_TRANSFORM) {
               builder.put(type, transform);
            }
         }
      }

      return new ItemTransforms(itemtransform, itemtransform1, itemtransform2, itemtransform3, itemtransform4, itemtransform5, itemtransform6, itemtransform7, builder.build());
   }

   private ItemTransform getTransform(ItemTransforms.TransformType pType) {
      return this.parent != null && !this.transforms.hasTransform(pType) ? this.parent.getTransform(pType) : this.transforms.getTransform(pType);
   }

   public String toString() {
      return this.name;
   }

   @OnlyIn(Dist.CLIENT)
   public static class Deserializer implements JsonDeserializer<BlockModel> {
      private static final boolean DEFAULT_AMBIENT_OCCLUSION = true;

      public BlockModel deserialize(JsonElement pJson, Type pType, JsonDeserializationContext pContext) throws JsonParseException {
         JsonObject jsonobject = pJson.getAsJsonObject();
         List<BlockElement> list = this.getElements(pContext, jsonobject);
         String s = this.getParentName(jsonobject);
         Map<String, Either<Material, String>> map = this.getTextureMap(jsonobject);
         boolean flag = this.getAmbientOcclusion(jsonobject);
         ItemTransforms itemtransforms = ItemTransforms.NO_TRANSFORMS;
         if (jsonobject.has("display")) {
            JsonObject jsonobject1 = GsonHelper.getAsJsonObject(jsonobject, "display");
            itemtransforms = pContext.deserialize(jsonobject1, ItemTransforms.class);
         }

         List<ItemOverride> list1 = this.getOverrides(pContext, jsonobject);
         BlockModel.GuiLight blockmodel$guilight = null;
         if (jsonobject.has("gui_light")) {
            blockmodel$guilight = BlockModel.GuiLight.getByName(GsonHelper.getAsString(jsonobject, "gui_light"));
         }

         ResourceLocation resourcelocation = s.isEmpty() ? null : new ResourceLocation(s);
         return new BlockModel(resourcelocation, list, map, flag, blockmodel$guilight, itemtransforms, list1);
      }

      protected List<ItemOverride> getOverrides(JsonDeserializationContext pContext, JsonObject pJson) {
         List<ItemOverride> list = Lists.newArrayList();
         if (pJson.has("overrides")) {
            for(JsonElement jsonelement : GsonHelper.getAsJsonArray(pJson, "overrides")) {
               list.add(pContext.deserialize(jsonelement, ItemOverride.class));
            }
         }

         return list;
      }

      private Map<String, Either<Material, String>> getTextureMap(JsonObject pJson) {
         ResourceLocation resourcelocation = TextureAtlas.LOCATION_BLOCKS;
         Map<String, Either<Material, String>> map = Maps.newHashMap();
         if (pJson.has("textures")) {
            JsonObject jsonobject = GsonHelper.getAsJsonObject(pJson, "textures");

            for(Map.Entry<String, JsonElement> entry : jsonobject.entrySet()) {
               map.put(entry.getKey(), parseTextureLocationOrReference(resourcelocation, entry.getValue().getAsString()));
            }
         }

         return map;
      }

      private static Either<Material, String> parseTextureLocationOrReference(ResourceLocation pLocation, String pName) {
         if (BlockModel.isTextureReference(pName)) {
            return Either.right(pName.substring(1));
         } else {
            ResourceLocation resourcelocation = ResourceLocation.tryParse(pName);
            if (resourcelocation == null) {
               throw new JsonParseException(pName + " is not valid resource location");
            } else {
               return Either.left(new Material(pLocation, resourcelocation));
            }
         }
      }

      private String getParentName(JsonObject pJson) {
         return GsonHelper.getAsString(pJson, "parent", "");
      }

      protected boolean getAmbientOcclusion(JsonObject pJson) {
         return GsonHelper.getAsBoolean(pJson, "ambientocclusion", true);
      }

      protected List<BlockElement> getElements(JsonDeserializationContext pContext, JsonObject pJson) {
         List<BlockElement> list = Lists.newArrayList();
         if (pJson.has("elements")) {
            for(JsonElement jsonelement : GsonHelper.getAsJsonArray(pJson, "elements")) {
               list.add(pContext.deserialize(jsonelement, BlockElement.class));
            }
         }

         return list;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static enum GuiLight {
      FRONT("front"),
      SIDE("side");

      private final String name;

      private GuiLight(String pName) {
         this.name = pName;
      }

      public static BlockModel.GuiLight getByName(String pName) {
         for(BlockModel.GuiLight blockmodel$guilight : values()) {
            if (blockmodel$guilight.name.equals(pName)) {
               return blockmodel$guilight;
            }
         }

         throw new IllegalArgumentException("Invalid gui light: " + pName);
      }

      public boolean lightLikeBlock() {
         return this == SIDE;
      }

      public String getSerializedName() { return name; }
   }

   @OnlyIn(Dist.CLIENT)
   public static class LoopException extends RuntimeException {
      public LoopException(String pMessage) {
         super(pMessage);
      }
   }
}
