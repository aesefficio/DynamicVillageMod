package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ItemFrameRenderer<T extends ItemFrame> extends EntityRenderer<T> {
   private static final ModelResourceLocation FRAME_LOCATION = new ModelResourceLocation("item_frame", "map=false");
   private static final ModelResourceLocation MAP_FRAME_LOCATION = new ModelResourceLocation("item_frame", "map=true");
   private static final ModelResourceLocation GLOW_FRAME_LOCATION = new ModelResourceLocation("glow_item_frame", "map=false");
   private static final ModelResourceLocation GLOW_MAP_FRAME_LOCATION = new ModelResourceLocation("glow_item_frame", "map=true");
   public static final int GLOW_FRAME_BRIGHTNESS = 5;
   public static final int BRIGHT_MAP_LIGHT_ADJUSTMENT = 30;
   private final ItemRenderer itemRenderer;
   private final BlockRenderDispatcher blockRenderer;

   public ItemFrameRenderer(EntityRendererProvider.Context pContext) {
      super(pContext);
      this.itemRenderer = pContext.getItemRenderer();
      this.blockRenderer = pContext.getBlockRenderDispatcher();
   }

   protected int getBlockLightLevel(T pEntity, BlockPos pPos) {
      return pEntity.getType() == EntityType.GLOW_ITEM_FRAME ? Math.max(5, super.getBlockLightLevel(pEntity, pPos)) : super.getBlockLightLevel(pEntity, pPos);
   }

   public void render(T pEntity, float pEntityYaw, float pPartialTicks, PoseStack pMatrixStack, MultiBufferSource pBuffer, int pPackedLight) {
      super.render(pEntity, pEntityYaw, pPartialTicks, pMatrixStack, pBuffer, pPackedLight);
      pMatrixStack.pushPose();
      Direction direction = pEntity.getDirection();
      Vec3 vec3 = this.getRenderOffset(pEntity, pPartialTicks);
      pMatrixStack.translate(-vec3.x(), -vec3.y(), -vec3.z());
      double d0 = 0.46875D;
      pMatrixStack.translate((double)direction.getStepX() * 0.46875D, (double)direction.getStepY() * 0.46875D, (double)direction.getStepZ() * 0.46875D);
      pMatrixStack.mulPose(Vector3f.XP.rotationDegrees(pEntity.getXRot()));
      pMatrixStack.mulPose(Vector3f.YP.rotationDegrees(180.0F - pEntity.getYRot()));
      boolean flag = pEntity.isInvisible();
      ItemStack itemstack = pEntity.getItem();
      if (!flag) {
         ModelManager modelmanager = this.blockRenderer.getBlockModelShaper().getModelManager();
         ModelResourceLocation modelresourcelocation = pEntity.getItem().getItem() instanceof MapItem ? MAP_FRAME_LOCATION : FRAME_LOCATION;
         pMatrixStack.pushPose();
         pMatrixStack.translate(-0.5D, -0.5D, -0.5D);
         this.blockRenderer.getModelRenderer().renderModel(pMatrixStack.last(), pBuffer.getBuffer(Sheets.solidBlockSheet()), (BlockState)null, modelmanager.getModel(modelresourcelocation), 1.0F, 1.0F, 1.0F, pPackedLight, OverlayTexture.NO_OVERLAY);
         pMatrixStack.popPose();
      }

      if (!itemstack.isEmpty()) {
         MapItemSavedData mapitemsaveddata = MapItem.getSavedData(itemstack, pEntity.level);
         if (flag) {
            pMatrixStack.translate(0.0D, 0.0D, 0.5D);
         } else {
            pMatrixStack.translate(0.0D, 0.0D, 0.4375D);
         }

         int j = mapitemsaveddata != null ? pEntity.getRotation() % 4 * 2 : pEntity.getRotation();
         pMatrixStack.mulPose(Vector3f.ZP.rotationDegrees((float)j * 360.0F / 8.0F));
         if (!net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.RenderItemInFrameEvent(pEntity, this, pMatrixStack, pBuffer, pPackedLight))) {
         if (mapitemsaveddata != null) {
            pMatrixStack.mulPose(Vector3f.ZP.rotationDegrees(180.0F));
            float f = 0.0078125F;
            pMatrixStack.scale(0.0078125F, 0.0078125F, 0.0078125F);
            pMatrixStack.translate(-64.0D, -64.0D, 0.0D);
            pMatrixStack.translate(0.0D, 0.0D, -1.0D);
            if (mapitemsaveddata != null) {
               int i = this.getLightVal(pEntity, 15728850, pPackedLight);
               Minecraft.getInstance().gameRenderer.getMapRenderer().render(pMatrixStack, pBuffer, pEntity.getFramedMapId().getAsInt(), mapitemsaveddata, true, i);
            }
         } else {
            int k = this.getLightVal(pEntity, 15728880, pPackedLight);
            pMatrixStack.scale(0.5F, 0.5F, 0.5F);
            this.itemRenderer.renderStatic(itemstack, ItemTransforms.TransformType.FIXED, k, OverlayTexture.NO_OVERLAY, pMatrixStack, pBuffer, pEntity.getId());
         }
         }
      }

      pMatrixStack.popPose();
   }

   private int getLightVal(T p_174209_, int p_174210_, int p_174211_) {
      return p_174209_.getType() == EntityType.GLOW_ITEM_FRAME ? p_174210_ : p_174211_;
   }

   private ModelResourceLocation getFrameModelResourceLoc(T pEntity, ItemStack pItem) {
      boolean flag = pEntity.getType() == EntityType.GLOW_ITEM_FRAME;
      if (pItem.is(Items.FILLED_MAP)) {
         return flag ? GLOW_MAP_FRAME_LOCATION : MAP_FRAME_LOCATION;
      } else {
         return flag ? GLOW_FRAME_LOCATION : FRAME_LOCATION;
      }
   }

   public Vec3 getRenderOffset(T pEntity, float pPartialTicks) {
      return new Vec3((double)((float)pEntity.getDirection().getStepX() * 0.3F), -0.25D, (double)((float)pEntity.getDirection().getStepZ() * 0.3F));
   }

   /**
    * Returns the location of an entity's texture.
    */
   public ResourceLocation getTextureLocation(T pEntity) {
      return TextureAtlas.LOCATION_BLOCKS;
   }

   protected boolean shouldShowName(T pEntity) {
      if (Minecraft.renderNames() && !pEntity.getItem().isEmpty() && pEntity.getItem().hasCustomHoverName() && this.entityRenderDispatcher.crosshairPickEntity == pEntity) {
         double d0 = this.entityRenderDispatcher.distanceToSqr(pEntity);
         float f = pEntity.isDiscrete() ? 32.0F : 64.0F;
         return d0 < (double)(f * f);
      } else {
         return false;
      }
   }

   protected void renderNameTag(T pEntity, Component pDisplayName, PoseStack pMatrixStack, MultiBufferSource pBuffer, int pPackedLight) {
      super.renderNameTag(pEntity, pEntity.getItem().getHoverName(), pMatrixStack, pBuffer, pPackedLight);
   }
}
