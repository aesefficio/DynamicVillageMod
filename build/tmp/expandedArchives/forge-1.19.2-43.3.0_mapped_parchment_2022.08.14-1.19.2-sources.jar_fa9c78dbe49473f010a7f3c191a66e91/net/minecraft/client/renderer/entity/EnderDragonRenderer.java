package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import javax.annotation.Nullable;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EnderDragonRenderer extends EntityRenderer<EnderDragon> {
   public static final ResourceLocation CRYSTAL_BEAM_LOCATION = new ResourceLocation("textures/entity/end_crystal/end_crystal_beam.png");
   private static final ResourceLocation DRAGON_EXPLODING_LOCATION = new ResourceLocation("textures/entity/enderdragon/dragon_exploding.png");
   private static final ResourceLocation DRAGON_LOCATION = new ResourceLocation("textures/entity/enderdragon/dragon.png");
   private static final ResourceLocation DRAGON_EYES_LOCATION = new ResourceLocation("textures/entity/enderdragon/dragon_eyes.png");
   private static final RenderType RENDER_TYPE = RenderType.entityCutoutNoCull(DRAGON_LOCATION);
   private static final RenderType DECAL = RenderType.entityDecal(DRAGON_LOCATION);
   private static final RenderType EYES = RenderType.eyes(DRAGON_EYES_LOCATION);
   private static final RenderType BEAM = RenderType.entitySmoothCutout(CRYSTAL_BEAM_LOCATION);
   private static final float HALF_SQRT_3 = (float)(Math.sqrt(3.0D) / 2.0D);
   private final EnderDragonRenderer.DragonModel model;

   public EnderDragonRenderer(EntityRendererProvider.Context pContext) {
      super(pContext);
      this.shadowRadius = 0.5F;
      this.model = new EnderDragonRenderer.DragonModel(pContext.bakeLayer(ModelLayers.ENDER_DRAGON));
   }

   public void render(EnderDragon pEntity, float pEntityYaw, float pPartialTicks, PoseStack pMatrixStack, MultiBufferSource pBuffer, int pPackedLight) {
      pMatrixStack.pushPose();
      float f = (float)pEntity.getLatencyPos(7, pPartialTicks)[0];
      float f1 = (float)(pEntity.getLatencyPos(5, pPartialTicks)[1] - pEntity.getLatencyPos(10, pPartialTicks)[1]);
      pMatrixStack.mulPose(Vector3f.YP.rotationDegrees(-f));
      pMatrixStack.mulPose(Vector3f.XP.rotationDegrees(f1 * 10.0F));
      pMatrixStack.translate(0.0D, 0.0D, 1.0D);
      pMatrixStack.scale(-1.0F, -1.0F, 1.0F);
      pMatrixStack.translate(0.0D, (double)-1.501F, 0.0D);
      boolean flag = pEntity.hurtTime > 0;
      this.model.prepareMobModel(pEntity, 0.0F, 0.0F, pPartialTicks);
      if (pEntity.dragonDeathTime > 0) {
         float f2 = (float)pEntity.dragonDeathTime / 200.0F;
         VertexConsumer vertexconsumer = pBuffer.getBuffer(RenderType.dragonExplosionAlpha(DRAGON_EXPLODING_LOCATION));
         this.model.renderToBuffer(pMatrixStack, vertexconsumer, pPackedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, f2);
         VertexConsumer vertexconsumer1 = pBuffer.getBuffer(DECAL);
         this.model.renderToBuffer(pMatrixStack, vertexconsumer1, pPackedLight, OverlayTexture.pack(0.0F, flag), 1.0F, 1.0F, 1.0F, 1.0F);
      } else {
         VertexConsumer vertexconsumer3 = pBuffer.getBuffer(RENDER_TYPE);
         this.model.renderToBuffer(pMatrixStack, vertexconsumer3, pPackedLight, OverlayTexture.pack(0.0F, flag), 1.0F, 1.0F, 1.0F, 1.0F);
      }

      VertexConsumer vertexconsumer4 = pBuffer.getBuffer(EYES);
      this.model.renderToBuffer(pMatrixStack, vertexconsumer4, pPackedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
      if (pEntity.dragonDeathTime > 0) {
         float f5 = ((float)pEntity.dragonDeathTime + pPartialTicks) / 200.0F;
         float f7 = Math.min(f5 > 0.8F ? (f5 - 0.8F) / 0.2F : 0.0F, 1.0F);
         RandomSource randomsource = RandomSource.create(432L);
         VertexConsumer vertexconsumer2 = pBuffer.getBuffer(RenderType.lightning());
         pMatrixStack.pushPose();
         pMatrixStack.translate(0.0D, -1.0D, -2.0D);

         for(int i = 0; (float)i < (f5 + f5 * f5) / 2.0F * 60.0F; ++i) {
            pMatrixStack.mulPose(Vector3f.XP.rotationDegrees(randomsource.nextFloat() * 360.0F));
            pMatrixStack.mulPose(Vector3f.YP.rotationDegrees(randomsource.nextFloat() * 360.0F));
            pMatrixStack.mulPose(Vector3f.ZP.rotationDegrees(randomsource.nextFloat() * 360.0F));
            pMatrixStack.mulPose(Vector3f.XP.rotationDegrees(randomsource.nextFloat() * 360.0F));
            pMatrixStack.mulPose(Vector3f.YP.rotationDegrees(randomsource.nextFloat() * 360.0F));
            pMatrixStack.mulPose(Vector3f.ZP.rotationDegrees(randomsource.nextFloat() * 360.0F + f5 * 90.0F));
            float f3 = randomsource.nextFloat() * 20.0F + 5.0F + f7 * 10.0F;
            float f4 = randomsource.nextFloat() * 2.0F + 1.0F + f7 * 2.0F;
            Matrix4f matrix4f = pMatrixStack.last().pose();
            int j = (int)(255.0F * (1.0F - f7));
            vertex01(vertexconsumer2, matrix4f, j);
            vertex2(vertexconsumer2, matrix4f, f3, f4);
            vertex3(vertexconsumer2, matrix4f, f3, f4);
            vertex01(vertexconsumer2, matrix4f, j);
            vertex3(vertexconsumer2, matrix4f, f3, f4);
            vertex4(vertexconsumer2, matrix4f, f3, f4);
            vertex01(vertexconsumer2, matrix4f, j);
            vertex4(vertexconsumer2, matrix4f, f3, f4);
            vertex2(vertexconsumer2, matrix4f, f3, f4);
         }

         pMatrixStack.popPose();
      }

      pMatrixStack.popPose();
      if (pEntity.nearestCrystal != null) {
         pMatrixStack.pushPose();
         float f6 = (float)(pEntity.nearestCrystal.getX() - Mth.lerp((double)pPartialTicks, pEntity.xo, pEntity.getX()));
         float f8 = (float)(pEntity.nearestCrystal.getY() - Mth.lerp((double)pPartialTicks, pEntity.yo, pEntity.getY()));
         float f9 = (float)(pEntity.nearestCrystal.getZ() - Mth.lerp((double)pPartialTicks, pEntity.zo, pEntity.getZ()));
         renderCrystalBeams(f6, f8 + EndCrystalRenderer.getY(pEntity.nearestCrystal, pPartialTicks), f9, pPartialTicks, pEntity.tickCount, pMatrixStack, pBuffer, pPackedLight);
         pMatrixStack.popPose();
      }

      super.render(pEntity, pEntityYaw, pPartialTicks, pMatrixStack, pBuffer, pPackedLight);
   }

   private static void vertex01(VertexConsumer p_114220_, Matrix4f p_114221_, int p_114222_) {
      p_114220_.vertex(p_114221_, 0.0F, 0.0F, 0.0F).color(255, 255, 255, p_114222_).endVertex();
   }

   private static void vertex2(VertexConsumer p_114215_, Matrix4f p_114216_, float p_114217_, float p_114218_) {
      p_114215_.vertex(p_114216_, -HALF_SQRT_3 * p_114218_, p_114217_, -0.5F * p_114218_).color(255, 0, 255, 0).endVertex();
   }

   private static void vertex3(VertexConsumer p_114224_, Matrix4f p_114225_, float p_114226_, float p_114227_) {
      p_114224_.vertex(p_114225_, HALF_SQRT_3 * p_114227_, p_114226_, -0.5F * p_114227_).color(255, 0, 255, 0).endVertex();
   }

   private static void vertex4(VertexConsumer p_114229_, Matrix4f p_114230_, float p_114231_, float p_114232_) {
      p_114229_.vertex(p_114230_, 0.0F, p_114231_, 1.0F * p_114232_).color(255, 0, 255, 0).endVertex();
   }

   public static void renderCrystalBeams(float p_114188_, float p_114189_, float p_114190_, float p_114191_, int p_114192_, PoseStack pPoseStack, MultiBufferSource p_114194_, int p_114195_) {
      float f = Mth.sqrt(p_114188_ * p_114188_ + p_114190_ * p_114190_);
      float f1 = Mth.sqrt(p_114188_ * p_114188_ + p_114189_ * p_114189_ + p_114190_ * p_114190_);
      pPoseStack.pushPose();
      pPoseStack.translate(0.0D, 2.0D, 0.0D);
      pPoseStack.mulPose(Vector3f.YP.rotation((float)(-Math.atan2((double)p_114190_, (double)p_114188_)) - ((float)Math.PI / 2F)));
      pPoseStack.mulPose(Vector3f.XP.rotation((float)(-Math.atan2((double)f, (double)p_114189_)) - ((float)Math.PI / 2F)));
      VertexConsumer vertexconsumer = p_114194_.getBuffer(BEAM);
      float f2 = 0.0F - ((float)p_114192_ + p_114191_) * 0.01F;
      float f3 = Mth.sqrt(p_114188_ * p_114188_ + p_114189_ * p_114189_ + p_114190_ * p_114190_) / 32.0F - ((float)p_114192_ + p_114191_) * 0.01F;
      int i = 8;
      float f4 = 0.0F;
      float f5 = 0.75F;
      float f6 = 0.0F;
      PoseStack.Pose posestack$pose = pPoseStack.last();
      Matrix4f matrix4f = posestack$pose.pose();
      Matrix3f matrix3f = posestack$pose.normal();

      for(int j = 1; j <= 8; ++j) {
         float f7 = Mth.sin((float)j * ((float)Math.PI * 2F) / 8.0F) * 0.75F;
         float f8 = Mth.cos((float)j * ((float)Math.PI * 2F) / 8.0F) * 0.75F;
         float f9 = (float)j / 8.0F;
         vertexconsumer.vertex(matrix4f, f4 * 0.2F, f5 * 0.2F, 0.0F).color(0, 0, 0, 255).uv(f6, f2).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(p_114195_).normal(matrix3f, 0.0F, -1.0F, 0.0F).endVertex();
         vertexconsumer.vertex(matrix4f, f4, f5, f1).color(255, 255, 255, 255).uv(f6, f3).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(p_114195_).normal(matrix3f, 0.0F, -1.0F, 0.0F).endVertex();
         vertexconsumer.vertex(matrix4f, f7, f8, f1).color(255, 255, 255, 255).uv(f9, f3).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(p_114195_).normal(matrix3f, 0.0F, -1.0F, 0.0F).endVertex();
         vertexconsumer.vertex(matrix4f, f7 * 0.2F, f8 * 0.2F, 0.0F).color(0, 0, 0, 255).uv(f9, f2).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(p_114195_).normal(matrix3f, 0.0F, -1.0F, 0.0F).endVertex();
         f4 = f7;
         f5 = f8;
         f6 = f9;
      }

      pPoseStack.popPose();
   }

   /**
    * Returns the location of an entity's texture.
    */
   public ResourceLocation getTextureLocation(EnderDragon pEntity) {
      return DRAGON_LOCATION;
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition meshdefinition = new MeshDefinition();
      PartDefinition partdefinition = meshdefinition.getRoot();
      float f = -16.0F;
      PartDefinition partdefinition1 = partdefinition.addOrReplaceChild("head", CubeListBuilder.create().addBox("upperlip", -6.0F, -1.0F, -24.0F, 12, 5, 16, 176, 44).addBox("upperhead", -8.0F, -8.0F, -10.0F, 16, 16, 16, 112, 30).mirror().addBox("scale", -5.0F, -12.0F, -4.0F, 2, 4, 6, 0, 0).addBox("nostril", -5.0F, -3.0F, -22.0F, 2, 2, 4, 112, 0).mirror().addBox("scale", 3.0F, -12.0F, -4.0F, 2, 4, 6, 0, 0).addBox("nostril", 3.0F, -3.0F, -22.0F, 2, 2, 4, 112, 0), PartPose.ZERO);
      partdefinition1.addOrReplaceChild("jaw", CubeListBuilder.create().addBox("jaw", -6.0F, 0.0F, -16.0F, 12, 4, 16, 176, 65), PartPose.offset(0.0F, 4.0F, -8.0F));
      partdefinition.addOrReplaceChild("neck", CubeListBuilder.create().addBox("box", -5.0F, -5.0F, -5.0F, 10, 10, 10, 192, 104).addBox("scale", -1.0F, -9.0F, -3.0F, 2, 4, 6, 48, 0), PartPose.ZERO);
      partdefinition.addOrReplaceChild("body", CubeListBuilder.create().addBox("body", -12.0F, 0.0F, -16.0F, 24, 24, 64, 0, 0).addBox("scale", -1.0F, -6.0F, -10.0F, 2, 6, 12, 220, 53).addBox("scale", -1.0F, -6.0F, 10.0F, 2, 6, 12, 220, 53).addBox("scale", -1.0F, -6.0F, 30.0F, 2, 6, 12, 220, 53), PartPose.offset(0.0F, 4.0F, 8.0F));
      PartDefinition partdefinition2 = partdefinition.addOrReplaceChild("left_wing", CubeListBuilder.create().mirror().addBox("bone", 0.0F, -4.0F, -4.0F, 56, 8, 8, 112, 88).addBox("skin", 0.0F, 0.0F, 2.0F, 56, 0, 56, -56, 88), PartPose.offset(12.0F, 5.0F, 2.0F));
      partdefinition2.addOrReplaceChild("left_wing_tip", CubeListBuilder.create().mirror().addBox("bone", 0.0F, -2.0F, -2.0F, 56, 4, 4, 112, 136).addBox("skin", 0.0F, 0.0F, 2.0F, 56, 0, 56, -56, 144), PartPose.offset(56.0F, 0.0F, 0.0F));
      PartDefinition partdefinition3 = partdefinition.addOrReplaceChild("left_front_leg", CubeListBuilder.create().addBox("main", -4.0F, -4.0F, -4.0F, 8, 24, 8, 112, 104), PartPose.offset(12.0F, 20.0F, 2.0F));
      PartDefinition partdefinition4 = partdefinition3.addOrReplaceChild("left_front_leg_tip", CubeListBuilder.create().addBox("main", -3.0F, -1.0F, -3.0F, 6, 24, 6, 226, 138), PartPose.offset(0.0F, 20.0F, -1.0F));
      partdefinition4.addOrReplaceChild("left_front_foot", CubeListBuilder.create().addBox("main", -4.0F, 0.0F, -12.0F, 8, 4, 16, 144, 104), PartPose.offset(0.0F, 23.0F, 0.0F));
      PartDefinition partdefinition5 = partdefinition.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().addBox("main", -8.0F, -4.0F, -8.0F, 16, 32, 16, 0, 0), PartPose.offset(16.0F, 16.0F, 42.0F));
      PartDefinition partdefinition6 = partdefinition5.addOrReplaceChild("left_hind_leg_tip", CubeListBuilder.create().addBox("main", -6.0F, -2.0F, 0.0F, 12, 32, 12, 196, 0), PartPose.offset(0.0F, 32.0F, -4.0F));
      partdefinition6.addOrReplaceChild("left_hind_foot", CubeListBuilder.create().addBox("main", -9.0F, 0.0F, -20.0F, 18, 6, 24, 112, 0), PartPose.offset(0.0F, 31.0F, 4.0F));
      PartDefinition partdefinition7 = partdefinition.addOrReplaceChild("right_wing", CubeListBuilder.create().addBox("bone", -56.0F, -4.0F, -4.0F, 56, 8, 8, 112, 88).addBox("skin", -56.0F, 0.0F, 2.0F, 56, 0, 56, -56, 88), PartPose.offset(-12.0F, 5.0F, 2.0F));
      partdefinition7.addOrReplaceChild("right_wing_tip", CubeListBuilder.create().addBox("bone", -56.0F, -2.0F, -2.0F, 56, 4, 4, 112, 136).addBox("skin", -56.0F, 0.0F, 2.0F, 56, 0, 56, -56, 144), PartPose.offset(-56.0F, 0.0F, 0.0F));
      PartDefinition partdefinition8 = partdefinition.addOrReplaceChild("right_front_leg", CubeListBuilder.create().addBox("main", -4.0F, -4.0F, -4.0F, 8, 24, 8, 112, 104), PartPose.offset(-12.0F, 20.0F, 2.0F));
      PartDefinition partdefinition9 = partdefinition8.addOrReplaceChild("right_front_leg_tip", CubeListBuilder.create().addBox("main", -3.0F, -1.0F, -3.0F, 6, 24, 6, 226, 138), PartPose.offset(0.0F, 20.0F, -1.0F));
      partdefinition9.addOrReplaceChild("right_front_foot", CubeListBuilder.create().addBox("main", -4.0F, 0.0F, -12.0F, 8, 4, 16, 144, 104), PartPose.offset(0.0F, 23.0F, 0.0F));
      PartDefinition partdefinition10 = partdefinition.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().addBox("main", -8.0F, -4.0F, -8.0F, 16, 32, 16, 0, 0), PartPose.offset(-16.0F, 16.0F, 42.0F));
      PartDefinition partdefinition11 = partdefinition10.addOrReplaceChild("right_hind_leg_tip", CubeListBuilder.create().addBox("main", -6.0F, -2.0F, 0.0F, 12, 32, 12, 196, 0), PartPose.offset(0.0F, 32.0F, -4.0F));
      partdefinition11.addOrReplaceChild("right_hind_foot", CubeListBuilder.create().addBox("main", -9.0F, 0.0F, -20.0F, 18, 6, 24, 112, 0), PartPose.offset(0.0F, 31.0F, 4.0F));
      return LayerDefinition.create(meshdefinition, 256, 256);
   }

   @OnlyIn(Dist.CLIENT)
   public static class DragonModel extends EntityModel<EnderDragon> {
      private final ModelPart head;
      private final ModelPart neck;
      private final ModelPart jaw;
      private final ModelPart body;
      private final ModelPart leftWing;
      private final ModelPart leftWingTip;
      private final ModelPart leftFrontLeg;
      private final ModelPart leftFrontLegTip;
      private final ModelPart leftFrontFoot;
      private final ModelPart leftRearLeg;
      private final ModelPart leftRearLegTip;
      private final ModelPart leftRearFoot;
      private final ModelPart rightWing;
      private final ModelPart rightWingTip;
      private final ModelPart rightFrontLeg;
      private final ModelPart rightFrontLegTip;
      private final ModelPart rightFrontFoot;
      private final ModelPart rightRearLeg;
      private final ModelPart rightRearLegTip;
      private final ModelPart rightRearFoot;
      @Nullable
      private EnderDragon entity;
      private float a;

      public DragonModel(ModelPart pRoot) {
         this.head = pRoot.getChild("head");
         this.jaw = this.head.getChild("jaw");
         this.neck = pRoot.getChild("neck");
         this.body = pRoot.getChild("body");
         this.leftWing = pRoot.getChild("left_wing");
         this.leftWingTip = this.leftWing.getChild("left_wing_tip");
         this.leftFrontLeg = pRoot.getChild("left_front_leg");
         this.leftFrontLegTip = this.leftFrontLeg.getChild("left_front_leg_tip");
         this.leftFrontFoot = this.leftFrontLegTip.getChild("left_front_foot");
         this.leftRearLeg = pRoot.getChild("left_hind_leg");
         this.leftRearLegTip = this.leftRearLeg.getChild("left_hind_leg_tip");
         this.leftRearFoot = this.leftRearLegTip.getChild("left_hind_foot");
         this.rightWing = pRoot.getChild("right_wing");
         this.rightWingTip = this.rightWing.getChild("right_wing_tip");
         this.rightFrontLeg = pRoot.getChild("right_front_leg");
         this.rightFrontLegTip = this.rightFrontLeg.getChild("right_front_leg_tip");
         this.rightFrontFoot = this.rightFrontLegTip.getChild("right_front_foot");
         this.rightRearLeg = pRoot.getChild("right_hind_leg");
         this.rightRearLegTip = this.rightRearLeg.getChild("right_hind_leg_tip");
         this.rightRearFoot = this.rightRearLegTip.getChild("right_hind_foot");
      }

      public void prepareMobModel(EnderDragon pEntity, float pLimbSwing, float pLimbSwingAmount, float pPartialTick) {
         this.entity = pEntity;
         this.a = pPartialTick;
      }

      /**
       * Sets this entity's model rotation angles
       */
      public void setupAnim(EnderDragon pEntity, float pLimbSwing, float pLimbSwingAmount, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch) {
      }

      public void renderToBuffer(PoseStack pMatrixStack, VertexConsumer pBuffer, int pPackedLight, int pPackedOverlay, float pRed, float pGreen, float pBlue, float pAlpha) {
         pMatrixStack.pushPose();
         float f = Mth.lerp(this.a, this.entity.oFlapTime, this.entity.flapTime);
         this.jaw.xRot = (float)(Math.sin((double)(f * ((float)Math.PI * 2F))) + 1.0D) * 0.2F;
         float f1 = (float)(Math.sin((double)(f * ((float)Math.PI * 2F) - 1.0F)) + 1.0D);
         f1 = (f1 * f1 + f1 * 2.0F) * 0.05F;
         pMatrixStack.translate(0.0D, (double)(f1 - 2.0F), -3.0D);
         pMatrixStack.mulPose(Vector3f.XP.rotationDegrees(f1 * 2.0F));
         float f2 = 0.0F;
         float f3 = 20.0F;
         float f4 = -12.0F;
         float f5 = 1.5F;
         double[] adouble = this.entity.getLatencyPos(6, this.a);
         float f6 = Mth.rotWrap(this.entity.getLatencyPos(5, this.a)[0] - this.entity.getLatencyPos(10, this.a)[0]);
         float f7 = Mth.rotWrap(this.entity.getLatencyPos(5, this.a)[0] + (double)(f6 / 2.0F));
         float f8 = f * ((float)Math.PI * 2F);

         for(int i = 0; i < 5; ++i) {
            double[] adouble1 = this.entity.getLatencyPos(5 - i, this.a);
            float f9 = (float)Math.cos((double)((float)i * 0.45F + f8)) * 0.15F;
            this.neck.yRot = Mth.rotWrap(adouble1[0] - adouble[0]) * ((float)Math.PI / 180F) * 1.5F;
            this.neck.xRot = f9 + this.entity.getHeadPartYOffset(i, adouble, adouble1) * ((float)Math.PI / 180F) * 1.5F * 5.0F;
            this.neck.zRot = -Mth.rotWrap(adouble1[0] - (double)f7) * ((float)Math.PI / 180F) * 1.5F;
            this.neck.y = f3;
            this.neck.z = f4;
            this.neck.x = f2;
            f3 += Mth.sin(this.neck.xRot) * 10.0F;
            f4 -= Mth.cos(this.neck.yRot) * Mth.cos(this.neck.xRot) * 10.0F;
            f2 -= Mth.sin(this.neck.yRot) * Mth.cos(this.neck.xRot) * 10.0F;
            this.neck.render(pMatrixStack, pBuffer, pPackedLight, pPackedOverlay, 1.0F, 1.0F, 1.0F, pAlpha);
         }

         this.head.y = f3;
         this.head.z = f4;
         this.head.x = f2;
         double[] adouble2 = this.entity.getLatencyPos(0, this.a);
         this.head.yRot = Mth.rotWrap(adouble2[0] - adouble[0]) * ((float)Math.PI / 180F);
         this.head.xRot = Mth.rotWrap((double)this.entity.getHeadPartYOffset(6, adouble, adouble2)) * ((float)Math.PI / 180F) * 1.5F * 5.0F;
         this.head.zRot = -Mth.rotWrap(adouble2[0] - (double)f7) * ((float)Math.PI / 180F);
         this.head.render(pMatrixStack, pBuffer, pPackedLight, pPackedOverlay, 1.0F, 1.0F, 1.0F, pAlpha);
         pMatrixStack.pushPose();
         pMatrixStack.translate(0.0D, 1.0D, 0.0D);
         pMatrixStack.mulPose(Vector3f.ZP.rotationDegrees(-f6 * 1.5F));
         pMatrixStack.translate(0.0D, -1.0D, 0.0D);
         this.body.zRot = 0.0F;
         this.body.render(pMatrixStack, pBuffer, pPackedLight, pPackedOverlay, 1.0F, 1.0F, 1.0F, pAlpha);
         float f10 = f * ((float)Math.PI * 2F);
         this.leftWing.xRot = 0.125F - (float)Math.cos((double)f10) * 0.2F;
         this.leftWing.yRot = -0.25F;
         this.leftWing.zRot = -((float)(Math.sin((double)f10) + 0.125D)) * 0.8F;
         this.leftWingTip.zRot = (float)(Math.sin((double)(f10 + 2.0F)) + 0.5D) * 0.75F;
         this.rightWing.xRot = this.leftWing.xRot;
         this.rightWing.yRot = -this.leftWing.yRot;
         this.rightWing.zRot = -this.leftWing.zRot;
         this.rightWingTip.zRot = -this.leftWingTip.zRot;
         this.renderSide(pMatrixStack, pBuffer, pPackedLight, pPackedOverlay, f1, this.leftWing, this.leftFrontLeg, this.leftFrontLegTip, this.leftFrontFoot, this.leftRearLeg, this.leftRearLegTip, this.leftRearFoot, pAlpha);
         this.renderSide(pMatrixStack, pBuffer, pPackedLight, pPackedOverlay, f1, this.rightWing, this.rightFrontLeg, this.rightFrontLegTip, this.rightFrontFoot, this.rightRearLeg, this.rightRearLegTip, this.rightRearFoot, pAlpha);
         pMatrixStack.popPose();
         float f11 = -Mth.sin(f * ((float)Math.PI * 2F)) * 0.0F;
         f8 = f * ((float)Math.PI * 2F);
         f3 = 10.0F;
         f4 = 60.0F;
         f2 = 0.0F;
         adouble = this.entity.getLatencyPos(11, this.a);

         for(int j = 0; j < 12; ++j) {
            adouble2 = this.entity.getLatencyPos(12 + j, this.a);
            f11 += Mth.sin((float)j * 0.45F + f8) * 0.05F;
            this.neck.yRot = (Mth.rotWrap(adouble2[0] - adouble[0]) * 1.5F + 180.0F) * ((float)Math.PI / 180F);
            this.neck.xRot = f11 + (float)(adouble2[1] - adouble[1]) * ((float)Math.PI / 180F) * 1.5F * 5.0F;
            this.neck.zRot = Mth.rotWrap(adouble2[0] - (double)f7) * ((float)Math.PI / 180F) * 1.5F;
            this.neck.y = f3;
            this.neck.z = f4;
            this.neck.x = f2;
            f3 += Mth.sin(this.neck.xRot) * 10.0F;
            f4 -= Mth.cos(this.neck.yRot) * Mth.cos(this.neck.xRot) * 10.0F;
            f2 -= Mth.sin(this.neck.yRot) * Mth.cos(this.neck.xRot) * 10.0F;
            this.neck.render(pMatrixStack, pBuffer, pPackedLight, pPackedOverlay, 1.0F, 1.0F, 1.0F, pAlpha);
         }

         pMatrixStack.popPose();
      }

      private void renderSide(PoseStack pPoseStack, VertexConsumer pBuffer, int pPackedLight, int pPackedOverlay, float p_173982_, ModelPart pWing, ModelPart pFrontLeg, ModelPart pFrontLegTip, ModelPart pFrontFoot, ModelPart pRearLeg, ModelPart pRearLegTip, ModelPart pRearFoot, float pAlpha) {
         pRearLeg.xRot = 1.0F + p_173982_ * 0.1F;
         pRearLegTip.xRot = 0.5F + p_173982_ * 0.1F;
         pRearFoot.xRot = 0.75F + p_173982_ * 0.1F;
         pFrontLeg.xRot = 1.3F + p_173982_ * 0.1F;
         pFrontLegTip.xRot = -0.5F - p_173982_ * 0.1F;
         pFrontFoot.xRot = 0.75F + p_173982_ * 0.1F;
         pWing.render(pPoseStack, pBuffer, pPackedLight, pPackedOverlay, 1.0F, 1.0F, 1.0F, pAlpha);
         pFrontLeg.render(pPoseStack, pBuffer, pPackedLight, pPackedOverlay, 1.0F, 1.0F, 1.0F, pAlpha);
         pRearLeg.render(pPoseStack, pBuffer, pPackedLight, pPackedOverlay, 1.0F, 1.0F, 1.0F, pAlpha);
      }
   }
}