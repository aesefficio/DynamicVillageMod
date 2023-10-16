package net.minecraft.client.gui.screens.inventory;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CartographyTableScreen extends AbstractContainerScreen<CartographyTableMenu> {
   private static final ResourceLocation BG_LOCATION = new ResourceLocation("textures/gui/container/cartography_table.png");

   public CartographyTableScreen(CartographyTableMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
      super(pMenu, pPlayerInventory, pTitle);
      this.titleLabelY -= 2;
   }

   public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
      super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
      this.renderTooltip(pPoseStack, pMouseX, pMouseY);
   }

   protected void renderBg(PoseStack pPoseStack, float pPartialTick, int pX, int pY) {
      this.renderBackground(pPoseStack);
      RenderSystem.setShader(GameRenderer::getPositionTexShader);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.setShaderTexture(0, BG_LOCATION);
      int i = this.leftPos;
      int j = this.topPos;
      this.blit(pPoseStack, i, j, 0, 0, this.imageWidth, this.imageHeight);
      ItemStack itemstack = this.menu.getSlot(1).getItem();
      boolean flag = itemstack.is(Items.MAP);
      boolean flag1 = itemstack.is(Items.PAPER);
      boolean flag2 = itemstack.is(Items.GLASS_PANE);
      ItemStack itemstack1 = this.menu.getSlot(0).getItem();
      boolean flag3 = false;
      Integer integer;
      MapItemSavedData mapitemsaveddata;
      if (itemstack1.is(Items.FILLED_MAP)) {
         integer = MapItem.getMapId(itemstack1);
         mapitemsaveddata = MapItem.getSavedData(integer, this.minecraft.level);
         if (mapitemsaveddata != null) {
            if (mapitemsaveddata.locked) {
               flag3 = true;
               if (flag1 || flag2) {
                  this.blit(pPoseStack, i + 35, j + 31, this.imageWidth + 50, 132, 28, 21);
               }
            }

            if (flag1 && mapitemsaveddata.scale >= 4) {
               flag3 = true;
               this.blit(pPoseStack, i + 35, j + 31, this.imageWidth + 50, 132, 28, 21);
            }
         }
      } else {
         integer = null;
         mapitemsaveddata = null;
      }

      this.renderResultingMap(pPoseStack, integer, mapitemsaveddata, flag, flag1, flag2, flag3);
   }

   private void renderResultingMap(PoseStack pPoseStack, @Nullable Integer pMapId, @Nullable MapItemSavedData pMapData, boolean pHasMap, boolean pHasPaper, boolean pHasGlassPane, boolean pIsMaxSize) {
      int i = this.leftPos;
      int j = this.topPos;
      if (pHasPaper && !pIsMaxSize) {
         this.blit(pPoseStack, i + 67, j + 13, this.imageWidth, 66, 66, 66);
         this.renderMap(pPoseStack, pMapId, pMapData, i + 85, j + 31, 0.226F);
      } else if (pHasMap) {
         this.blit(pPoseStack, i + 67 + 16, j + 13, this.imageWidth, 132, 50, 66);
         this.renderMap(pPoseStack, pMapId, pMapData, i + 86, j + 16, 0.34F);
         RenderSystem.setShaderTexture(0, BG_LOCATION);
         pPoseStack.pushPose();
         pPoseStack.translate(0.0D, 0.0D, 1.0D);
         this.blit(pPoseStack, i + 67, j + 13 + 16, this.imageWidth, 132, 50, 66);
         this.renderMap(pPoseStack, pMapId, pMapData, i + 70, j + 32, 0.34F);
         pPoseStack.popPose();
      } else if (pHasGlassPane) {
         this.blit(pPoseStack, i + 67, j + 13, this.imageWidth, 0, 66, 66);
         this.renderMap(pPoseStack, pMapId, pMapData, i + 71, j + 17, 0.45F);
         RenderSystem.setShaderTexture(0, BG_LOCATION);
         pPoseStack.pushPose();
         pPoseStack.translate(0.0D, 0.0D, 1.0D);
         this.blit(pPoseStack, i + 66, j + 12, 0, this.imageHeight, 66, 66);
         pPoseStack.popPose();
      } else {
         this.blit(pPoseStack, i + 67, j + 13, this.imageWidth, 0, 66, 66);
         this.renderMap(pPoseStack, pMapId, pMapData, i + 71, j + 17, 0.45F);
      }

   }

   private void renderMap(PoseStack pPoseStack, @Nullable Integer pMapId, @Nullable MapItemSavedData pMapData, int pX, int pY, float pScale) {
      if (pMapId != null && pMapData != null) {
         pPoseStack.pushPose();
         pPoseStack.translate((double)pX, (double)pY, 1.0D);
         pPoseStack.scale(pScale, pScale, 1.0F);
         MultiBufferSource.BufferSource multibuffersource$buffersource = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
         this.minecraft.gameRenderer.getMapRenderer().render(pPoseStack, multibuffersource$buffersource, pMapId, pMapData, true, 15728880);
         multibuffersource$buffersource.endBatch();
         pPoseStack.popPose();
      }

   }
}