package net.minecraft.client.renderer.block.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ItemTransforms {
   public static final ItemTransforms NO_TRANSFORMS = new ItemTransforms();
   public final ItemTransform thirdPersonLeftHand;
   public final ItemTransform thirdPersonRightHand;
   public final ItemTransform firstPersonLeftHand;
   public final ItemTransform firstPersonRightHand;
   public final ItemTransform head;
   public final ItemTransform gui;
   public final ItemTransform ground;
   public final ItemTransform fixed;
   public final com.google.common.collect.ImmutableMap<TransformType, ItemTransform> moddedTransforms;

   private ItemTransforms() {
      this(ItemTransform.NO_TRANSFORM, ItemTransform.NO_TRANSFORM, ItemTransform.NO_TRANSFORM, ItemTransform.NO_TRANSFORM, ItemTransform.NO_TRANSFORM, ItemTransform.NO_TRANSFORM, ItemTransform.NO_TRANSFORM, ItemTransform.NO_TRANSFORM);
   }

   @Deprecated
   public ItemTransforms(ItemTransforms pTransforms) {
      this.thirdPersonLeftHand = pTransforms.thirdPersonLeftHand;
      this.thirdPersonRightHand = pTransforms.thirdPersonRightHand;
      this.firstPersonLeftHand = pTransforms.firstPersonLeftHand;
      this.firstPersonRightHand = pTransforms.firstPersonRightHand;
      this.head = pTransforms.head;
      this.gui = pTransforms.gui;
      this.ground = pTransforms.ground;
      this.fixed = pTransforms.fixed;
      this.moddedTransforms = pTransforms.moddedTransforms;
   }

   @Deprecated
   public ItemTransforms(ItemTransform pThirdPersonLeftHand, ItemTransform pThirdPersonRightHand, ItemTransform pFirstPersonLeftHand, ItemTransform pFirstPersonRightHand, ItemTransform pHead, ItemTransform pGui, ItemTransform pGround, ItemTransform pFixed) {
      this(pThirdPersonLeftHand, pThirdPersonRightHand, pFirstPersonLeftHand, pFirstPersonRightHand, pHead, pGui, pGround, pFixed, com.google.common.collect.ImmutableMap.of());
   }

   public ItemTransforms(ItemTransform pThirdPersonLeftHand, ItemTransform pThirdPersonRightHand, ItemTransform pFirstPersonLeftHand, ItemTransform pFirstPersonRightHand, ItemTransform pHead, ItemTransform pGui, ItemTransform pGround, ItemTransform pFixed,
           com.google.common.collect.ImmutableMap<TransformType, ItemTransform> moddedTransforms) {
      this.thirdPersonLeftHand = pThirdPersonLeftHand;
      this.thirdPersonRightHand = pThirdPersonRightHand;
      this.firstPersonLeftHand = pFirstPersonLeftHand;
      this.firstPersonRightHand = pFirstPersonRightHand;
      this.head = pHead;
      this.gui = pGui;
      this.ground = pGround;
      this.fixed = pFixed;
      this.moddedTransforms = moddedTransforms;
   }

   public ItemTransform getTransform(ItemTransforms.TransformType pType) {
      switch (pType) {
         case THIRD_PERSON_LEFT_HAND:
            return this.thirdPersonLeftHand;
         case THIRD_PERSON_RIGHT_HAND:
            return this.thirdPersonRightHand;
         case FIRST_PERSON_LEFT_HAND:
            return this.firstPersonLeftHand;
         case FIRST_PERSON_RIGHT_HAND:
            return this.firstPersonRightHand;
         case HEAD:
            return this.head;
         case GUI:
            return this.gui;
         case GROUND:
            return this.ground;
         case FIXED:
            return this.fixed;
         default:
            return moddedTransforms.getOrDefault(pType, ItemTransform.NO_TRANSFORM);
      }
   }

   public boolean hasTransform(ItemTransforms.TransformType pType) {
      return this.getTransform(pType) != ItemTransform.NO_TRANSFORM;
   }

   @OnlyIn(Dist.CLIENT)
   public static class Deserializer implements JsonDeserializer<ItemTransforms> {
      public ItemTransforms deserialize(JsonElement pJson, Type pType, JsonDeserializationContext pContext) throws JsonParseException {
         JsonObject jsonobject = pJson.getAsJsonObject();
         ItemTransform itemtransform = this.getTransform(pContext, jsonobject, "thirdperson_righthand");
         ItemTransform itemtransform1 = this.getTransform(pContext, jsonobject, "thirdperson_lefthand");
         if (itemtransform1 == ItemTransform.NO_TRANSFORM) {
            itemtransform1 = itemtransform;
         }

         ItemTransform itemtransform2 = this.getTransform(pContext, jsonobject, "firstperson_righthand");
         ItemTransform itemtransform3 = this.getTransform(pContext, jsonobject, "firstperson_lefthand");
         if (itemtransform3 == ItemTransform.NO_TRANSFORM) {
            itemtransform3 = itemtransform2;
         }

         ItemTransform itemtransform4 = this.getTransform(pContext, jsonobject, "head");
         ItemTransform itemtransform5 = this.getTransform(pContext, jsonobject, "gui");
         ItemTransform itemtransform6 = this.getTransform(pContext, jsonobject, "ground");
         ItemTransform itemtransform7 = this.getTransform(pContext, jsonobject, "fixed");

         var builder = com.google.common.collect.ImmutableMap.<TransformType, ItemTransform>builder();
         for (TransformType type : TransformType.values()) {
            if (type.isModded()) {
               var transform = this.getTransform(pContext, jsonobject, type.getSerializeName());
               var fallbackType = type;
               while (transform == ItemTransform.NO_TRANSFORM && fallbackType.fallback != null) {
                  fallbackType = fallbackType.fallback;
                  transform = this.getTransform(pContext, jsonobject, fallbackType.getSerializeName());
               }
               if (transform != ItemTransform.NO_TRANSFORM){
                  builder.put(type, transform);
               }
            }
         }

         return new ItemTransforms(itemtransform1, itemtransform, itemtransform3, itemtransform2, itemtransform4, itemtransform5, itemtransform6, itemtransform7, builder.build());
      }

      private ItemTransform getTransform(JsonDeserializationContext pContext, JsonObject pJson, String pName) {
         return pJson.has(pName) ? pContext.deserialize(pJson.get(pName), ItemTransform.class) : ItemTransform.NO_TRANSFORM;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static enum TransformType implements net.minecraftforge.common.IExtensibleEnum {
      NONE("none"),
      THIRD_PERSON_LEFT_HAND("thirdperson_lefthand"),
      THIRD_PERSON_RIGHT_HAND("thirdperson_righthand"),
      FIRST_PERSON_LEFT_HAND("firstperson_lefthand"),
      FIRST_PERSON_RIGHT_HAND("firstperson_righthand"),
      HEAD("head"),
      GUI("gui"),
      GROUND("ground"),
      FIXED("fixed");

      public boolean firstPerson() {
         return this == FIRST_PERSON_LEFT_HAND || this == FIRST_PERSON_RIGHT_HAND;
      }

      // Forge start
      private final String serializeName;
      private final boolean isModded;
      @javax.annotation.Nullable
      private final TransformType fallback;

      private TransformType(String name) {
         serializeName = name;
         isModded = false;
         fallback = null;
      }

      private TransformType(net.minecraft.resources.ResourceLocation serializeName) {
         this.serializeName = java.util.Objects.requireNonNull(serializeName, "Modded TransformTypes must have a non-null serializeName").toString();
         isModded = true;
         fallback = null;
      }

      private TransformType(net.minecraft.resources.ResourceLocation serializeName, TransformType fallback) {
         this.serializeName = java.util.Objects.requireNonNull(serializeName, "Modded TransformTypes must have a non-null serializeName").toString();
         isModded = true;
         this.fallback = fallback;
      }

      public boolean isModded() {
         return isModded;
      }

      @javax.annotation.Nullable
      public TransformType fallback() {
         return fallback;
      }

      public String getSerializeName() {
         return serializeName;
      }

      public static TransformType create(String keyName, net.minecraft.resources.ResourceLocation serializeName) {
         throw new IllegalStateException("Enum not extended!");
      }
      public static TransformType create(String keyName, net.minecraft.resources.ResourceLocation serializeName, TransformType fallback) {
         throw new IllegalStateException("Enum not extended!");
      }
   }
}
