package net.minecraft.client.renderer.entity;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.ItemModelShaper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ItemRenderer implements ResourceManagerReloadListener {
   public static final ResourceLocation ENCHANT_GLINT_LOCATION = new ResourceLocation("textures/misc/enchanted_item_glint.png");
   private static final Set<Item> IGNORED = Sets.newHashSet(Items.AIR);
   private static final int GUI_SLOT_CENTER_X = 8;
   private static final int GUI_SLOT_CENTER_Y = 8;
   public static final int ITEM_COUNT_BLIT_OFFSET = 200;
   public static final float COMPASS_FOIL_UI_SCALE = 0.5F;
   public static final float COMPASS_FOIL_FIRST_PERSON_SCALE = 0.75F;
   public float blitOffset;
   private final ItemModelShaper itemModelShaper;
   private final TextureManager textureManager;
   private final ItemColors itemColors;
   private final BlockEntityWithoutLevelRenderer blockEntityRenderer;

   public ItemRenderer(TextureManager pTextureManager, ModelManager pModelManager, ItemColors pItemColors, BlockEntityWithoutLevelRenderer pBlockEntityRenderer) {
      this.textureManager = pTextureManager;
      this.itemModelShaper = new net.minecraftforge.client.model.ForgeItemModelShaper(pModelManager);
      this.blockEntityRenderer = pBlockEntityRenderer;

      for(Item item : Registry.ITEM) {
         if (!IGNORED.contains(item)) {
            this.itemModelShaper.register(item, new ModelResourceLocation(Registry.ITEM.getKey(item), "inventory"));
         }
      }

      this.itemColors = pItemColors;
   }

   public ItemModelShaper getItemModelShaper() {
      return this.itemModelShaper;
   }

   public void renderModelLists(BakedModel pModel, ItemStack pStack, int pCombinedLight, int pCombinedOverlay, PoseStack pMatrixStack, VertexConsumer pBuffer) {
      RandomSource randomsource = RandomSource.create();
      long i = 42L;

      for(Direction direction : Direction.values()) {
         randomsource.setSeed(42L);
         this.renderQuadList(pMatrixStack, pBuffer, pModel.getQuads((BlockState)null, direction, randomsource), pStack, pCombinedLight, pCombinedOverlay);
      }

      randomsource.setSeed(42L);
      this.renderQuadList(pMatrixStack, pBuffer, pModel.getQuads((BlockState)null, (Direction)null, randomsource), pStack, pCombinedLight, pCombinedOverlay);
   }

   public void render(ItemStack pItemStack, ItemTransforms.TransformType pTransformType, boolean pLeftHand, PoseStack pPoseStack, MultiBufferSource pBuffer, int pCombinedLight, int pCombinedOverlay, BakedModel pModel) {
      if (!pItemStack.isEmpty()) {
         pPoseStack.pushPose();
         boolean flag = pTransformType == ItemTransforms.TransformType.GUI || pTransformType == ItemTransforms.TransformType.GROUND || pTransformType == ItemTransforms.TransformType.FIXED;
         if (flag) {
            if (pItemStack.is(Items.TRIDENT)) {
               pModel = this.itemModelShaper.getModelManager().getModel(new ModelResourceLocation("minecraft:trident#inventory"));
            } else if (pItemStack.is(Items.SPYGLASS)) {
               pModel = this.itemModelShaper.getModelManager().getModel(new ModelResourceLocation("minecraft:spyglass#inventory"));
            }
         }

         pModel = net.minecraftforge.client.ForgeHooksClient.handleCameraTransforms(pPoseStack, pModel, pTransformType, pLeftHand);
         pPoseStack.translate(-0.5D, -0.5D, -0.5D);
         if (!pModel.isCustomRenderer() && (!pItemStack.is(Items.TRIDENT) || flag)) {
            boolean flag1;
            if (pTransformType != ItemTransforms.TransformType.GUI && !pTransformType.firstPerson() && pItemStack.getItem() instanceof BlockItem) {
               Block block = ((BlockItem)pItemStack.getItem()).getBlock();
               flag1 = !(block instanceof HalfTransparentBlock) && !(block instanceof StainedGlassPaneBlock);
            } else {
               flag1 = true;
            }
            for (var model : pModel.getRenderPasses(pItemStack, flag1)) {
            for (var rendertype : model.getRenderTypes(pItemStack, flag1)) {
            VertexConsumer vertexconsumer;
            if (pItemStack.is(ItemTags.COMPASSES) && pItemStack.hasFoil()) {
               pPoseStack.pushPose();
               PoseStack.Pose posestack$pose = pPoseStack.last();
               if (pTransformType == ItemTransforms.TransformType.GUI) {
                  posestack$pose.pose().multiply(0.5F);
               } else if (pTransformType.firstPerson()) {
                  posestack$pose.pose().multiply(0.75F);
               }

               if (flag1) {
                  vertexconsumer = getCompassFoilBufferDirect(pBuffer, rendertype, posestack$pose);
               } else {
                  vertexconsumer = getCompassFoilBuffer(pBuffer, rendertype, posestack$pose);
               }

               pPoseStack.popPose();
            } else if (flag1) {
               vertexconsumer = getFoilBufferDirect(pBuffer, rendertype, true, pItemStack.hasFoil());
            } else {
               vertexconsumer = getFoilBuffer(pBuffer, rendertype, true, pItemStack.hasFoil());
            }

            this.renderModelLists(model, pItemStack, pCombinedLight, pCombinedOverlay, pPoseStack, vertexconsumer);
            }
            }
         } else {
            net.minecraftforge.client.extensions.common.IClientItemExtensions.of(pItemStack).getCustomRenderer().renderByItem(pItemStack, pTransformType, pPoseStack, pBuffer, pCombinedLight, pCombinedOverlay);
         }

         pPoseStack.popPose();
      }
   }

   public static VertexConsumer getArmorFoilBuffer(MultiBufferSource pBuffer, RenderType pRenderType, boolean pNoEntity, boolean pWithGlint) {
      return pWithGlint ? VertexMultiConsumer.create(pBuffer.getBuffer(pNoEntity ? RenderType.armorGlint() : RenderType.armorEntityGlint()), pBuffer.getBuffer(pRenderType)) : pBuffer.getBuffer(pRenderType);
   }

   public static VertexConsumer getCompassFoilBuffer(MultiBufferSource pBuffer, RenderType pRenderType, PoseStack.Pose pMatrixEntry) {
      return VertexMultiConsumer.create(new SheetedDecalTextureGenerator(pBuffer.getBuffer(RenderType.glint()), pMatrixEntry.pose(), pMatrixEntry.normal()), pBuffer.getBuffer(pRenderType));
   }

   public static VertexConsumer getCompassFoilBufferDirect(MultiBufferSource pBuffer, RenderType pRenderType, PoseStack.Pose pMatrixEntry) {
      return VertexMultiConsumer.create(new SheetedDecalTextureGenerator(pBuffer.getBuffer(RenderType.glintDirect()), pMatrixEntry.pose(), pMatrixEntry.normal()), pBuffer.getBuffer(pRenderType));
   }

   public static VertexConsumer getFoilBuffer(MultiBufferSource pBuffer, RenderType pRenderType, boolean pIsItem, boolean pGlint) {
      if (pGlint) {
         return Minecraft.useShaderTransparency() && pRenderType == Sheets.translucentItemSheet() ? VertexMultiConsumer.create(pBuffer.getBuffer(RenderType.glintTranslucent()), pBuffer.getBuffer(pRenderType)) : VertexMultiConsumer.create(pBuffer.getBuffer(pIsItem ? RenderType.glint() : RenderType.entityGlint()), pBuffer.getBuffer(pRenderType));
      } else {
         return pBuffer.getBuffer(pRenderType);
      }
   }

   public static VertexConsumer getFoilBufferDirect(MultiBufferSource pBuffer, RenderType pRenderType, boolean pNoEntity, boolean pWithGlint) {
      return pWithGlint ? VertexMultiConsumer.create(pBuffer.getBuffer(pNoEntity ? RenderType.glintDirect() : RenderType.entityGlintDirect()), pBuffer.getBuffer(pRenderType)) : pBuffer.getBuffer(pRenderType);
   }

   public void renderQuadList(PoseStack pPoseStack, VertexConsumer pBuffer, List<BakedQuad> pQuads, ItemStack pItemStack, int pCombinedLight, int pCombinedOverlay) {
      boolean flag = !pItemStack.isEmpty();
      PoseStack.Pose posestack$pose = pPoseStack.last();

      for(BakedQuad bakedquad : pQuads) {
         int i = -1;
         if (flag && bakedquad.isTinted()) {
            i = this.itemColors.getColor(pItemStack, bakedquad.getTintIndex());
         }

         float f = (float)(i >> 16 & 255) / 255.0F;
         float f1 = (float)(i >> 8 & 255) / 255.0F;
         float f2 = (float)(i & 255) / 255.0F;
         pBuffer.putBulkData(posestack$pose, bakedquad, f, f1, f2, 1.0F, pCombinedLight, pCombinedOverlay, true);
      }

   }

   public BakedModel getModel(ItemStack p_174265_, @Nullable Level pLevel, @Nullable LivingEntity p_174267_, int p_174268_) {
      BakedModel bakedmodel;
      if (p_174265_.is(Items.TRIDENT)) {
         bakedmodel = this.itemModelShaper.getModelManager().getModel(new ModelResourceLocation("minecraft:trident_in_hand#inventory"));
      } else if (p_174265_.is(Items.SPYGLASS)) {
         bakedmodel = this.itemModelShaper.getModelManager().getModel(new ModelResourceLocation("minecraft:spyglass_in_hand#inventory"));
      } else {
         bakedmodel = this.itemModelShaper.getItemModel(p_174265_);
      }

      ClientLevel clientlevel = pLevel instanceof ClientLevel ? (ClientLevel)pLevel : null;
      BakedModel bakedmodel1 = bakedmodel.getOverrides().resolve(bakedmodel, p_174265_, clientlevel, p_174267_, p_174268_);
      return bakedmodel1 == null ? this.itemModelShaper.getModelManager().getMissingModel() : bakedmodel1;
   }

   public void renderStatic(ItemStack pItemStack, ItemTransforms.TransformType pTransformType, int pCombinedLight, int pCombinedOverlay, PoseStack pPoseStack, MultiBufferSource pBuffer, int p_174276_) {
      this.renderStatic((LivingEntity)null, pItemStack, pTransformType, false, pPoseStack, pBuffer, (Level)null, pCombinedLight, pCombinedOverlay, p_174276_);
   }

   public void renderStatic(@Nullable LivingEntity p_174243_, ItemStack pItemStack, ItemTransforms.TransformType pTransformType, boolean pLeftHand, PoseStack pPoseStack, MultiBufferSource pBuffer, @Nullable Level pLevel, int pCombinedLight, int pCombinedOverlay, int p_174252_) {
      if (!pItemStack.isEmpty()) {
         BakedModel bakedmodel = this.getModel(pItemStack, pLevel, p_174243_, p_174252_);
         this.render(pItemStack, pTransformType, pLeftHand, pPoseStack, pBuffer, pCombinedLight, pCombinedOverlay, bakedmodel);
      }
   }

   public void renderGuiItem(ItemStack pStack, int pX, int pY) {
      this.renderGuiItem(pStack, pX, pY, this.getModel(pStack, (Level)null, (LivingEntity)null, 0));
   }

   protected void renderGuiItem(ItemStack pStack, int pX, int pY, BakedModel pBakedModel) {
      this.textureManager.getTexture(TextureAtlas.LOCATION_BLOCKS).setFilter(false, false);
      RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      PoseStack posestack = RenderSystem.getModelViewStack();
      posestack.pushPose();
      posestack.translate((double)pX, (double)pY, (double)(100.0F + this.blitOffset));
      posestack.translate(8.0D, 8.0D, 0.0D);
      posestack.scale(1.0F, -1.0F, 1.0F);
      posestack.scale(16.0F, 16.0F, 16.0F);
      RenderSystem.applyModelViewMatrix();
      PoseStack posestack1 = new PoseStack();
      MultiBufferSource.BufferSource multibuffersource$buffersource = Minecraft.getInstance().renderBuffers().bufferSource();
      boolean flag = !pBakedModel.usesBlockLight();
      if (flag) {
         Lighting.setupForFlatItems();
      }

      this.render(pStack, ItemTransforms.TransformType.GUI, false, posestack1, multibuffersource$buffersource, 15728880, OverlayTexture.NO_OVERLAY, pBakedModel);
      multibuffersource$buffersource.endBatch();
      RenderSystem.enableDepthTest();
      if (flag) {
         Lighting.setupFor3DItems();
      }

      posestack.popPose();
      RenderSystem.applyModelViewMatrix();
   }

   public void renderAndDecorateItem(ItemStack pStack, int pXPosition, int pYPosition) {
      this.tryRenderGuiItem(Minecraft.getInstance().player, pStack, pXPosition, pYPosition, 0);
   }

   public void renderAndDecorateItem(ItemStack pStack, int pX, int pY, int p_174257_) {
      this.tryRenderGuiItem(Minecraft.getInstance().player, pStack, pX, pY, p_174257_);
   }

   public void renderAndDecorateItem(ItemStack pStack, int pX, int pY, int p_174262_, int p_174263_) {
      this.tryRenderGuiItem(Minecraft.getInstance().player, pStack, pX, pY, p_174262_, p_174263_);
   }

   public void renderAndDecorateFakeItem(ItemStack pStack, int pX, int pY) {
      this.tryRenderGuiItem((LivingEntity)null, pStack, pX, pY, 0);
   }

   public void renderAndDecorateItem(LivingEntity p_174230_, ItemStack pStack, int pX, int pY, int p_174234_) {
      this.tryRenderGuiItem(p_174230_, pStack, pX, pY, p_174234_);
   }

   private void tryRenderGuiItem(@Nullable LivingEntity p_174278_, ItemStack pStack, int pX, int pY, int p_174282_) {
      this.tryRenderGuiItem(p_174278_, pStack, pX, pY, p_174282_, 0);
   }

   private void tryRenderGuiItem(@Nullable LivingEntity p_174236_, ItemStack pStack, int pX, int pY, int p_174240_, int p_174241_) {
      if (!pStack.isEmpty()) {
         BakedModel bakedmodel = this.getModel(pStack, (Level)null, p_174236_, p_174240_);
         this.blitOffset = bakedmodel.isGui3d() ? this.blitOffset + 50.0F + (float)p_174241_ : this.blitOffset + 50.0F;

         try {
            this.renderGuiItem(pStack, pX, pY, bakedmodel);
         } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Rendering item");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Item being rendered");
            crashreportcategory.setDetail("Item Type", () -> {
               return String.valueOf((Object)pStack.getItem());
            });
            crashreportcategory.setDetail("Registry Name", () -> String.valueOf(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(pStack.getItem())));
            crashreportcategory.setDetail("Item Damage", () -> {
               return String.valueOf(pStack.getDamageValue());
            });
            crashreportcategory.setDetail("Item NBT", () -> {
               return String.valueOf((Object)pStack.getTag());
            });
            crashreportcategory.setDetail("Item Foil", () -> {
               return String.valueOf(pStack.hasFoil());
            });
            throw new ReportedException(crashreport);
         }

         this.blitOffset = bakedmodel.isGui3d() ? this.blitOffset - 50.0F - (float)p_174241_ : this.blitOffset - 50.0F;
      }
   }

   public void renderGuiItemDecorations(Font pFr, ItemStack pStack, int pXPosition, int pYPosition) {
      this.renderGuiItemDecorations(pFr, pStack, pXPosition, pYPosition, (String)null);
   }

   /**
    * Renders the stack size and/or damage bar for the given ItemStack.
    */
   public void renderGuiItemDecorations(Font pFr, ItemStack pStack, int pXPosition, int pYPosition, @Nullable String pText) {
      if (!pStack.isEmpty()) {
         PoseStack posestack = new PoseStack();
         if (pStack.getCount() != 1 || pText != null) {
            String s = pText == null ? String.valueOf(pStack.getCount()) : pText;
            posestack.translate(0.0D, 0.0D, (double)(this.blitOffset + 200.0F));
            MultiBufferSource.BufferSource multibuffersource$buffersource = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
            pFr.drawInBatch(s, (float)(pXPosition + 19 - 2 - pFr.width(s)), (float)(pYPosition + 6 + 3), 16777215, true, posestack.last().pose(), multibuffersource$buffersource, false, 0, 15728880);
            multibuffersource$buffersource.endBatch();
         }

         if (pStack.isBarVisible()) {
            RenderSystem.disableDepthTest();
            RenderSystem.disableTexture();
            RenderSystem.disableBlend();
            Tesselator tesselator = Tesselator.getInstance();
            BufferBuilder bufferbuilder = tesselator.getBuilder();
            int i = pStack.getBarWidth();
            int j = pStack.getBarColor();
            this.fillRect(bufferbuilder, pXPosition + 2, pYPosition + 13, 13, 2, 0, 0, 0, 255);
            this.fillRect(bufferbuilder, pXPosition + 2, pYPosition + 13, i, 1, j >> 16 & 255, j >> 8 & 255, j & 255, 255);
            RenderSystem.enableBlend();
            RenderSystem.enableTexture();
            RenderSystem.enableDepthTest();
         }

         LocalPlayer localplayer = Minecraft.getInstance().player;
         float f = localplayer == null ? 0.0F : localplayer.getCooldowns().getCooldownPercent(pStack.getItem(), Minecraft.getInstance().getFrameTime());
         if (f > 0.0F) {
            RenderSystem.disableDepthTest();
            RenderSystem.disableTexture();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            Tesselator tesselator1 = Tesselator.getInstance();
            BufferBuilder bufferbuilder1 = tesselator1.getBuilder();
            this.fillRect(bufferbuilder1, pXPosition, pYPosition + Mth.floor(16.0F * (1.0F - f)), 16, Mth.ceil(16.0F * f), 255, 255, 255, 127);
            RenderSystem.enableTexture();
            RenderSystem.enableDepthTest();
         }

         net.minecraftforge.client.ItemDecoratorHandler.of(pStack).render(pFr, pStack, pXPosition, pYPosition, blitOffset);
      }
   }

   /**
    * Draw with the WorldRenderer
    */
   private void fillRect(BufferBuilder pRenderer, int pX, int pY, int pWidth, int pHeight, int pRed, int pGreen, int pBlue, int pAlpha) {
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      pRenderer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      pRenderer.vertex((double)(pX + 0), (double)(pY + 0), 0.0D).color(pRed, pGreen, pBlue, pAlpha).endVertex();
      pRenderer.vertex((double)(pX + 0), (double)(pY + pHeight), 0.0D).color(pRed, pGreen, pBlue, pAlpha).endVertex();
      pRenderer.vertex((double)(pX + pWidth), (double)(pY + pHeight), 0.0D).color(pRed, pGreen, pBlue, pAlpha).endVertex();
      pRenderer.vertex((double)(pX + pWidth), (double)(pY + 0), 0.0D).color(pRed, pGreen, pBlue, pAlpha).endVertex();
      BufferUploader.drawWithShader(pRenderer.end());
   }

   public void onResourceManagerReload(ResourceManager pResourceManager) {
      this.itemModelShaper.rebuildCache();
   }

   public BlockEntityWithoutLevelRenderer getBlockEntityRenderer() {
       return blockEntityRenderer;
   }
}
