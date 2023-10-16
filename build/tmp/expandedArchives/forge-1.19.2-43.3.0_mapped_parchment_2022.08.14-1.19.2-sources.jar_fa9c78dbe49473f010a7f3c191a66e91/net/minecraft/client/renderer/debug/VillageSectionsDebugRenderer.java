package net.minecraft.client.renderer.debug;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Set;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class VillageSectionsDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
   private static final int MAX_RENDER_DIST_FOR_VILLAGE_SECTIONS = 60;
   private final Set<SectionPos> villageSections = Sets.newHashSet();

   VillageSectionsDebugRenderer() {
   }

   public void clear() {
      this.villageSections.clear();
   }

   public void setVillageSection(SectionPos pPos) {
      this.villageSections.add(pPos);
   }

   public void setNotVillageSection(SectionPos pPos) {
      this.villageSections.remove(pPos);
   }

   public void render(PoseStack pPoseStack, MultiBufferSource pBufferSource, double pCamX, double pCamY, double pCamZ) {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableTexture();
      this.doRender(pCamX, pCamY, pCamZ);
      RenderSystem.enableTexture();
      RenderSystem.disableBlend();
   }

   private void doRender(double pX, double pY, double pZ) {
      BlockPos blockpos = new BlockPos(pX, pY, pZ);
      this.villageSections.forEach((p_113708_) -> {
         if (blockpos.closerThan(p_113708_.center(), 60.0D)) {
            highlightVillageSection(p_113708_);
         }

      });
   }

   private static void highlightVillageSection(SectionPos pPos) {
      float f = 1.0F;
      BlockPos blockpos = pPos.center();
      BlockPos blockpos1 = blockpos.offset(-1.0D, -1.0D, -1.0D);
      BlockPos blockpos2 = blockpos.offset(1.0D, 1.0D, 1.0D);
      DebugRenderer.renderFilledBox(blockpos1, blockpos2, 0.2F, 1.0F, 0.2F, 0.15F);
   }
}