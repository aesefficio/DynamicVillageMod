package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import net.minecraft.client.Camera;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.ConduitBlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ConduitRenderer implements BlockEntityRenderer<ConduitBlockEntity> {
   public static final Material SHELL_TEXTURE = new Material(TextureAtlas.LOCATION_BLOCKS, new ResourceLocation("entity/conduit/base"));
   public static final Material ACTIVE_SHELL_TEXTURE = new Material(TextureAtlas.LOCATION_BLOCKS, new ResourceLocation("entity/conduit/cage"));
   public static final Material WIND_TEXTURE = new Material(TextureAtlas.LOCATION_BLOCKS, new ResourceLocation("entity/conduit/wind"));
   public static final Material VERTICAL_WIND_TEXTURE = new Material(TextureAtlas.LOCATION_BLOCKS, new ResourceLocation("entity/conduit/wind_vertical"));
   public static final Material OPEN_EYE_TEXTURE = new Material(TextureAtlas.LOCATION_BLOCKS, new ResourceLocation("entity/conduit/open_eye"));
   public static final Material CLOSED_EYE_TEXTURE = new Material(TextureAtlas.LOCATION_BLOCKS, new ResourceLocation("entity/conduit/closed_eye"));
   private final ModelPart eye;
   private final ModelPart wind;
   private final ModelPart shell;
   private final ModelPart cage;
   private final BlockEntityRenderDispatcher renderer;

   public ConduitRenderer(BlockEntityRendererProvider.Context pContext) {
      this.renderer = pContext.getBlockEntityRenderDispatcher();
      this.eye = pContext.bakeLayer(ModelLayers.CONDUIT_EYE);
      this.wind = pContext.bakeLayer(ModelLayers.CONDUIT_WIND);
      this.shell = pContext.bakeLayer(ModelLayers.CONDUIT_SHELL);
      this.cage = pContext.bakeLayer(ModelLayers.CONDUIT_CAGE);
   }

   public static LayerDefinition createEyeLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      partdefinition.addOrReplaceChild("eye", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, 0.0F, 8.0F, 8.0F, 0.0F, new CubeDeformation(0.01F)), PartPose.ZERO);
      return LayerDefinition.create(meshdefinition, 16, 16);
   }

   public static LayerDefinition createWindLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      partdefinition.addOrReplaceChild("wind", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -8.0F, -8.0F, 16.0F, 16.0F, 16.0F), PartPose.ZERO);
      return LayerDefinition.create(meshdefinition, 64, 32);
   }

   public static LayerDefinition createShellLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      partdefinition.addOrReplaceChild("shell", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F), PartPose.ZERO);
      return LayerDefinition.create(meshdefinition, 32, 16);
   }

   public static LayerDefinition createCageLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      partdefinition.addOrReplaceChild("shell", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.ZERO);
      return LayerDefinition.create(meshdefinition, 32, 16);
   }

   public void render(ConduitBlockEntity pBlockEntity, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pPackedOverlay) {
      float f = (float)pBlockEntity.tickCount + pPartialTick;
      if (!pBlockEntity.isActive()) {
         float f5 = pBlockEntity.getActiveRotation(0.0F);
         VertexConsumer vertexconsumer1 = SHELL_TEXTURE.buffer(pBufferSource, RenderType::entitySolid);
         pPoseStack.pushPose();
         pPoseStack.translate(0.5D, 0.5D, 0.5D);
         pPoseStack.mulPose(Vector3f.YP.rotationDegrees(f5));
         this.shell.render(pPoseStack, vertexconsumer1, pPackedLight, pPackedOverlay);
         pPoseStack.popPose();
      } else {
         float f1 = pBlockEntity.getActiveRotation(pPartialTick) * (180F / (float)Math.PI);
         float f2 = Mth.sin(f * 0.1F) / 2.0F + 0.5F;
         f2 = f2 * f2 + f2;
         pPoseStack.pushPose();
         pPoseStack.translate(0.5D, (double)(0.3F + f2 * 0.2F), 0.5D);
         Vector3f vector3f = new Vector3f(0.5F, 1.0F, 0.5F);
         vector3f.normalize();
         pPoseStack.mulPose(vector3f.rotationDegrees(f1));
         this.cage.render(pPoseStack, ACTIVE_SHELL_TEXTURE.buffer(pBufferSource, RenderType::entityCutoutNoCull), pPackedLight, pPackedOverlay);
         pPoseStack.popPose();
         int i = pBlockEntity.tickCount / 66 % 3;
         pPoseStack.pushPose();
         pPoseStack.translate(0.5D, 0.5D, 0.5D);
         if (i == 1) {
            pPoseStack.mulPose(Vector3f.XP.rotationDegrees(90.0F));
         } else if (i == 2) {
            pPoseStack.mulPose(Vector3f.ZP.rotationDegrees(90.0F));
         }

         VertexConsumer vertexconsumer = (i == 1 ? VERTICAL_WIND_TEXTURE : WIND_TEXTURE).buffer(pBufferSource, RenderType::entityCutoutNoCull);
         this.wind.render(pPoseStack, vertexconsumer, pPackedLight, pPackedOverlay);
         pPoseStack.popPose();
         pPoseStack.pushPose();
         pPoseStack.translate(0.5D, 0.5D, 0.5D);
         pPoseStack.scale(0.875F, 0.875F, 0.875F);
         pPoseStack.mulPose(Vector3f.XP.rotationDegrees(180.0F));
         pPoseStack.mulPose(Vector3f.ZP.rotationDegrees(180.0F));
         this.wind.render(pPoseStack, vertexconsumer, pPackedLight, pPackedOverlay);
         pPoseStack.popPose();
         Camera camera = this.renderer.camera;
         pPoseStack.pushPose();
         pPoseStack.translate(0.5D, (double)(0.3F + f2 * 0.2F), 0.5D);
         pPoseStack.scale(0.5F, 0.5F, 0.5F);
         float f3 = -camera.getYRot();
         pPoseStack.mulPose(Vector3f.YP.rotationDegrees(f3));
         pPoseStack.mulPose(Vector3f.XP.rotationDegrees(camera.getXRot()));
         pPoseStack.mulPose(Vector3f.ZP.rotationDegrees(180.0F));
         float f4 = 1.3333334F;
         pPoseStack.scale(1.3333334F, 1.3333334F, 1.3333334F);
         this.eye.render(pPoseStack, (pBlockEntity.isHunting() ? OPEN_EYE_TEXTURE : CLOSED_EYE_TEXTURE).buffer(pBufferSource, RenderType::entityCutoutNoCull), pPackedLight, pPackedOverlay);
         pPoseStack.popPose();
      }
   }
}