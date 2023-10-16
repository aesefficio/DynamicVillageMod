package net.minecraft.client.renderer.block.model.multipart;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.datafixers.util.Pair;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.block.model.BlockModelDefinition;
import net.minecraft.client.renderer.block.model.MultiVariant;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.MultiPartBakedModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MultiPart implements UnbakedModel {
   private final StateDefinition<Block, BlockState> definition;
   private final List<Selector> selectors;

   public MultiPart(StateDefinition<Block, BlockState> pDefinition, List<Selector> pSelectors) {
      this.definition = pDefinition;
      this.selectors = pSelectors;
   }

   public List<Selector> getSelectors() {
      return this.selectors;
   }

   public Set<MultiVariant> getMultiVariants() {
      Set<MultiVariant> set = Sets.newHashSet();

      for(Selector selector : this.selectors) {
         set.add(selector.getVariant());
      }

      return set;
   }

   public boolean equals(Object pOther) {
      if (this == pOther) {
         return true;
      } else if (!(pOther instanceof MultiPart)) {
         return false;
      } else {
         MultiPart multipart = (MultiPart)pOther;
         return Objects.equals(this.definition, multipart.definition) && Objects.equals(this.selectors, multipart.selectors);
      }
   }

   public int hashCode() {
      return Objects.hash(this.definition, this.selectors);
   }

   public Collection<ResourceLocation> getDependencies() {
      return this.getSelectors().stream().flatMap((p_111969_) -> {
         return p_111969_.getVariant().getDependencies().stream();
      }).collect(Collectors.toSet());
   }

   public Collection<Material> getMaterials(Function<ResourceLocation, UnbakedModel> pModelGetter, Set<Pair<String, String>> pMissingTextureErrors) {
      return this.getSelectors().stream().flatMap((p_111981_) -> {
         return p_111981_.getVariant().getMaterials(pModelGetter, pMissingTextureErrors).stream();
      }).collect(Collectors.toSet());
   }

   @Nullable
   public BakedModel bake(ModelBakery pModelBakery, Function<Material, TextureAtlasSprite> pSpriteGetter, ModelState pTransform, ResourceLocation pLocation) {
      MultiPartBakedModel.Builder multipartbakedmodel$builder = new MultiPartBakedModel.Builder();

      for(Selector selector : this.getSelectors()) {
         BakedModel bakedmodel = selector.getVariant().bake(pModelBakery, pSpriteGetter, pTransform, pLocation);
         if (bakedmodel != null) {
            multipartbakedmodel$builder.add(selector.getPredicate(this.definition), bakedmodel);
         }
      }

      return multipartbakedmodel$builder.build();
   }

   @OnlyIn(Dist.CLIENT)
   public static class Deserializer implements JsonDeserializer<MultiPart> {
      private final BlockModelDefinition.Context context;

      public Deserializer(BlockModelDefinition.Context pContext) {
         this.context = pContext;
      }

      public MultiPart deserialize(JsonElement pJson, Type pType, JsonDeserializationContext pJsonContext) throws JsonParseException {
         return new MultiPart(this.context.getDefinition(), this.getSelectors(pJsonContext, pJson.getAsJsonArray()));
      }

      private List<Selector> getSelectors(JsonDeserializationContext pJsonContext, JsonArray pElements) {
         List<Selector> list = Lists.newArrayList();

         for(JsonElement jsonelement : pElements) {
            list.add(pJsonContext.deserialize(jsonelement, Selector.class));
         }

         return list;
      }
   }
}