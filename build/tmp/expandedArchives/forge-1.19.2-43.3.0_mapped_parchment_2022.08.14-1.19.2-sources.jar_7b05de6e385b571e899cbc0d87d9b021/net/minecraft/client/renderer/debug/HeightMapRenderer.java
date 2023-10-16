package net.minecraft.client.renderer.debug;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Vector3f;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class HeightMapRenderer implements DebugRenderer.SimpleDebugRenderer {
   private final Minecraft minecraft;
   private static final int CHUNK_DIST = 2;
   private static final float BOX_HEIGHT = 0.09375F;

   public HeightMapRenderer(Minecraft pMinecraft) {
      this.minecraft = pMinecraft;
   }

   public void render(PoseStack pPoseStack, MultiBufferSource pBufferSource, double pCamX, double pCamY, double pCamZ) {
      LevelAccessor levelaccessor = this.minecraft.level;
      RenderSystem.disableBlend();
      RenderSystem.disableTexture();
      RenderSystem.enableDepthTest();
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      BlockPos blockpos = new BlockPos(pCamX, 0.0D, pCamZ);
      Tesselator tesselator = Tesselator.getInstance();
      BufferBuilder bufferbuilder = tesselator.getBuilder();
      bufferbuilder.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

      for(int i = -2; i <= 2; ++i) {
         for(int j = -2; j <= 2; ++j) {
            ChunkAccess chunkaccess = levelaccessor.getChunk(blockpos.offset(i * 16, 0, j * 16));

            for(Map.Entry<Heightmap.Types, Heightmap> entry : chunkaccess.getHeightmaps()) {
               Heightmap.Types heightmap$types = entry.getKey();
               ChunkPos chunkpos = chunkaccess.getPos();
               Vector3f vector3f = this.getColor(heightmap$types);

               for(int k = 0; k < 16; ++k) {
                  for(int l = 0; l < 16; ++l) {
                     int i1 = SectionPos.sectionToBlockCoord(chunkpos.x, k);
                     int j1 = SectionPos.sectionToBlockCoord(chunkpos.z, l);
                     float f = (float)((double)((float)levelaccessor.getHeight(heightmap$types, i1, j1) + (float)heightmap$types.ordinal() * 0.09375F) - pCamY);
                     LevelRenderer.addChainedFilledBoxVertices(bufferbuilder, (double)((float)i1 + 0.25F) - pCamX, (double)f, (double)((float)j1 + 0.25F) - pCamZ, (double)((float)i1 + 0.75F) - pCamX, (double)(f + 0.09375F), (double)((float)j1 + 0.75F) - pCamZ, vector3f.x(), vector3f.y(), vector3f.z(), 1.0F);
                  }
               }
            }
         }
      }

      tesselator.end();
      RenderSystem.enableTexture();
   }

   private Vector3f getColor(Heightmap.Types pTypes) {
      switch (pTypes) {
         case WORLD_SURFACE_WG:
            return new Vector3f(1.0F, 1.0F, 0.0F);
         case OCEAN_FLOOR_WG:
            return new Vector3f(1.0F, 0.0F, 1.0F);
         case WORLD_SURFACE:
            return new Vector3f(0.0F, 0.7F, 0.0F);
         case OCEAN_FLOOR:
            return new Vector3f(0.0F, 0.0F, 0.5F);
         case MOTION_BLOCKING:
            return new Vector3f(0.0F, 0.3F, 0.3F);
         case MOTION_BLOCKING_NO_LEAVES:
            return new Vector3f(0.0F, 0.5F, 0.5F);
         default:
            return new Vector3f(0.0F, 0.0F, 0.0F);
      }
   }
}