package net.minecraft.client.renderer.entity.layers;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import java.util.Map;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.SkullModelBase;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CustomHeadLayer<T extends LivingEntity, M extends EntityModel<T> & HeadedModel> extends RenderLayer<T, M> {
   private final float scaleX;
   private final float scaleY;
   private final float scaleZ;
   private final Map<SkullBlock.Type, SkullModelBase> skullModels;
   private final ItemInHandRenderer itemInHandRenderer;

   public CustomHeadLayer(RenderLayerParent<T, M> pRenderer, EntityModelSet pModelSet, ItemInHandRenderer pItemInHandRenderer) {
      this(pRenderer, pModelSet, 1.0F, 1.0F, 1.0F, pItemInHandRenderer);
   }

   public CustomHeadLayer(RenderLayerParent<T, M> pRenderer, EntityModelSet pModelSet, float pScaleX, float pScaleY, float pScaleZ, ItemInHandRenderer pItemInHandRenderer) {
      super(pRenderer);
      this.scaleX = pScaleX;
      this.scaleY = pScaleY;
      this.scaleZ = pScaleZ;
      this.skullModels = SkullBlockRenderer.createSkullRenderers(pModelSet);
      this.itemInHandRenderer = pItemInHandRenderer;
   }

   public void render(PoseStack pMatrixStack, MultiBufferSource pBuffer, int pPackedLight, T pLivingEntity, float pLimbSwing, float pLimbSwingAmount, float pPartialTicks, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch) {
      ItemStack itemstack = pLivingEntity.getItemBySlot(EquipmentSlot.HEAD);
      if (!itemstack.isEmpty()) {
         Item item = itemstack.getItem();
         pMatrixStack.pushPose();
         pMatrixStack.scale(this.scaleX, this.scaleY, this.scaleZ);
         boolean flag = pLivingEntity instanceof Villager || pLivingEntity instanceof ZombieVillager;
         if (pLivingEntity.isBaby() && !(pLivingEntity instanceof Villager)) {
            float f = 2.0F;
            float f1 = 1.4F;
            pMatrixStack.translate(0.0D, 0.03125D, 0.0D);
            pMatrixStack.scale(0.7F, 0.7F, 0.7F);
            pMatrixStack.translate(0.0D, 1.0D, 0.0D);
         }

         this.getParentModel().getHead().translateAndRotate(pMatrixStack);
         if (item instanceof BlockItem && ((BlockItem)item).getBlock() instanceof AbstractSkullBlock) {
            float f2 = 1.1875F;
            pMatrixStack.scale(1.1875F, -1.1875F, -1.1875F);
            if (flag) {
               pMatrixStack.translate(0.0D, 0.0625D, 0.0D);
            }

            GameProfile gameprofile = null;
            if (itemstack.hasTag()) {
               CompoundTag compoundtag = itemstack.getTag();
               if (compoundtag.contains("SkullOwner", 10)) {
                  gameprofile = NbtUtils.readGameProfile(compoundtag.getCompound("SkullOwner"));
               }
            }

            pMatrixStack.translate(-0.5D, 0.0D, -0.5D);
            SkullBlock.Type skullblock$type = ((AbstractSkullBlock)((BlockItem)item).getBlock()).getType();
            SkullModelBase skullmodelbase = this.skullModels.get(skullblock$type);
            RenderType rendertype = SkullBlockRenderer.getRenderType(skullblock$type, gameprofile);
            SkullBlockRenderer.renderSkull((Direction)null, 180.0F, pLimbSwing, pMatrixStack, pBuffer, pPackedLight, skullmodelbase, rendertype);
         } else if (!(item instanceof ArmorItem) || ((ArmorItem)item).getSlot() != EquipmentSlot.HEAD) {
            translateToHead(pMatrixStack, flag);
            this.itemInHandRenderer.renderItem(pLivingEntity, itemstack, ItemTransforms.TransformType.HEAD, false, pMatrixStack, pBuffer, pPackedLight);
         }

         pMatrixStack.popPose();
      }
   }

   public static void translateToHead(PoseStack pPoseStack, boolean p_174485_) {
      float f = 0.625F;
      pPoseStack.translate(0.0D, -0.25D, 0.0D);
      pPoseStack.mulPose(Vector3f.YP.rotationDegrees(180.0F));
      pPoseStack.scale(0.625F, -0.625F, -0.625F);
      if (p_174485_) {
         pPoseStack.translate(0.0D, 0.1875D, 0.0D);
      }

   }
}