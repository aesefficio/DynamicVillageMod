package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LecternRenderer implements BlockEntityRenderer<LecternBlockEntity> {
   private final BookModel bookModel;

   public LecternRenderer(BlockEntityRendererProvider.Context pContext) {
      this.bookModel = new BookModel(pContext.bakeLayer(ModelLayers.BOOK));
   }

   public void render(LecternBlockEntity pBlockEntity, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pPackedOverlay) {
      BlockState blockstate = pBlockEntity.getBlockState();
      if (blockstate.getValue(LecternBlock.HAS_BOOK)) {
         pPoseStack.pushPose();
         pPoseStack.translate(0.5D, 1.0625D, 0.5D);
         float f = blockstate.getValue(LecternBlock.FACING).getClockWise().toYRot();
         pPoseStack.mulPose(Vector3f.YP.rotationDegrees(-f));
         pPoseStack.mulPose(Vector3f.ZP.rotationDegrees(67.5F));
         pPoseStack.translate(0.0D, -0.125D, 0.0D);
         this.bookModel.setupAnim(0.0F, 0.1F, 0.9F, 1.2F);
         VertexConsumer vertexconsumer = EnchantTableRenderer.BOOK_LOCATION.buffer(pBufferSource, RenderType::entitySolid);
         this.bookModel.render(pPoseStack, vertexconsumer, pPackedLight, pPackedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
         pPoseStack.popPose();
      }
   }
}