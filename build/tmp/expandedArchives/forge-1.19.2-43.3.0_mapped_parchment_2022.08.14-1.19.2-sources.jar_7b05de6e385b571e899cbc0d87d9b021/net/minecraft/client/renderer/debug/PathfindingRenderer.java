package net.minecraft.client.renderer.debug;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.Locale;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PathfindingRenderer implements DebugRenderer.SimpleDebugRenderer {
   private final Map<Integer, Path> pathMap = Maps.newHashMap();
   private final Map<Integer, Float> pathMaxDist = Maps.newHashMap();
   private final Map<Integer, Long> creationMap = Maps.newHashMap();
   private static final long TIMEOUT = 5000L;
   private static final float MAX_RENDER_DIST = 80.0F;
   private static final boolean SHOW_OPEN_CLOSED = true;
   private static final boolean SHOW_OPEN_CLOSED_COST_MALUS = false;
   private static final boolean SHOW_OPEN_CLOSED_NODE_TYPE_WITH_TEXT = false;
   private static final boolean SHOW_OPEN_CLOSED_NODE_TYPE_WITH_BOX = true;
   private static final boolean SHOW_GROUND_LABELS = true;
   private static final float TEXT_SCALE = 0.02F;

   public void addPath(int pEntityId, Path pPath, float pMaxDistanceToWaypoint) {
      this.pathMap.put(pEntityId, pPath);
      this.creationMap.put(pEntityId, Util.getMillis());
      this.pathMaxDist.put(pEntityId, pMaxDistanceToWaypoint);
   }

   public void render(PoseStack pPoseStack, MultiBufferSource pBufferSource, double pCamX, double pCamY, double pCamZ) {
      if (!this.pathMap.isEmpty()) {
         long i = Util.getMillis();

         for(Integer integer : this.pathMap.keySet()) {
            Path path = this.pathMap.get(integer);
            float f = this.pathMaxDist.get(integer);
            renderPath(path, f, true, true, pCamX, pCamY, pCamZ);
         }

         for(Integer integer1 : this.creationMap.keySet().toArray(new Integer[0])) {
            if (i - this.creationMap.get(integer1) > 5000L) {
               this.pathMap.remove(integer1);
               this.creationMap.remove(integer1);
            }
         }

      }
   }

   public static void renderPath(Path pPath, float pMaxDistanceToWaypoint, boolean pRenderOpenAndClosedSet, boolean pRenderText, double pX, double pY, double pZ) {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableTexture();
      RenderSystem.lineWidth(6.0F);
      doRenderPath(pPath, pMaxDistanceToWaypoint, pRenderOpenAndClosedSet, pRenderText, pX, pY, pZ);
      RenderSystem.enableTexture();
      RenderSystem.disableBlend();
   }

   private static void doRenderPath(Path pPath, float pMaxDistanceToWaypoint, boolean pRenderOpenAndClosedSet, boolean pRenderText, double pX, double pY, double pZ) {
      renderPathLine(pPath, pX, pY, pZ);
      BlockPos blockpos = pPath.getTarget();
      if (distanceToCamera(blockpos, pX, pY, pZ) <= 80.0F) {
         DebugRenderer.renderFilledBox((new AABB((double)((float)blockpos.getX() + 0.25F), (double)((float)blockpos.getY() + 0.25F), (double)blockpos.getZ() + 0.25D, (double)((float)blockpos.getX() + 0.75F), (double)((float)blockpos.getY() + 0.75F), (double)((float)blockpos.getZ() + 0.75F))).move(-pX, -pY, -pZ), 0.0F, 1.0F, 0.0F, 0.5F);

         for(int i = 0; i < pPath.getNodeCount(); ++i) {
            Node node = pPath.getNode(i);
            if (distanceToCamera(node.asBlockPos(), pX, pY, pZ) <= 80.0F) {
               float f = i == pPath.getNextNodeIndex() ? 1.0F : 0.0F;
               float f1 = i == pPath.getNextNodeIndex() ? 0.0F : 1.0F;
               DebugRenderer.renderFilledBox((new AABB((double)((float)node.x + 0.5F - pMaxDistanceToWaypoint), (double)((float)node.y + 0.01F * (float)i), (double)((float)node.z + 0.5F - pMaxDistanceToWaypoint), (double)((float)node.x + 0.5F + pMaxDistanceToWaypoint), (double)((float)node.y + 0.25F + 0.01F * (float)i), (double)((float)node.z + 0.5F + pMaxDistanceToWaypoint))).move(-pX, -pY, -pZ), f, 0.0F, f1, 0.5F);
            }
         }
      }

      if (pRenderOpenAndClosedSet) {
         for(Node node2 : pPath.getClosedSet()) {
            if (distanceToCamera(node2.asBlockPos(), pX, pY, pZ) <= 80.0F) {
               DebugRenderer.renderFilledBox((new AABB((double)((float)node2.x + 0.5F - pMaxDistanceToWaypoint / 2.0F), (double)((float)node2.y + 0.01F), (double)((float)node2.z + 0.5F - pMaxDistanceToWaypoint / 2.0F), (double)((float)node2.x + 0.5F + pMaxDistanceToWaypoint / 2.0F), (double)node2.y + 0.1D, (double)((float)node2.z + 0.5F + pMaxDistanceToWaypoint / 2.0F))).move(-pX, -pY, -pZ), 1.0F, 0.8F, 0.8F, 0.5F);
            }
         }

         for(Node node3 : pPath.getOpenSet()) {
            if (distanceToCamera(node3.asBlockPos(), pX, pY, pZ) <= 80.0F) {
               DebugRenderer.renderFilledBox((new AABB((double)((float)node3.x + 0.5F - pMaxDistanceToWaypoint / 2.0F), (double)((float)node3.y + 0.01F), (double)((float)node3.z + 0.5F - pMaxDistanceToWaypoint / 2.0F), (double)((float)node3.x + 0.5F + pMaxDistanceToWaypoint / 2.0F), (double)node3.y + 0.1D, (double)((float)node3.z + 0.5F + pMaxDistanceToWaypoint / 2.0F))).move(-pX, -pY, -pZ), 0.8F, 1.0F, 1.0F, 0.5F);
            }
         }
      }

      if (pRenderText) {
         for(int j = 0; j < pPath.getNodeCount(); ++j) {
            Node node1 = pPath.getNode(j);
            if (distanceToCamera(node1.asBlockPos(), pX, pY, pZ) <= 80.0F) {
               DebugRenderer.renderFloatingText(String.valueOf((Object)node1.type), (double)node1.x + 0.5D, (double)node1.y + 0.75D, (double)node1.z + 0.5D, -1, 0.02F, true, 0.0F, true);
               DebugRenderer.renderFloatingText(String.format(Locale.ROOT, "%.2f", node1.costMalus), (double)node1.x + 0.5D, (double)node1.y + 0.25D, (double)node1.z + 0.5D, -1, 0.02F, true, 0.0F, true);
            }
         }
      }

   }

   public static void renderPathLine(Path pPath, double pX, double pY, double pZ) {
      Tesselator tesselator = Tesselator.getInstance();
      BufferBuilder bufferbuilder = tesselator.getBuilder();
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      bufferbuilder.begin(VertexFormat.Mode.LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);

      for(int i = 0; i < pPath.getNodeCount(); ++i) {
         Node node = pPath.getNode(i);
         if (!(distanceToCamera(node.asBlockPos(), pX, pY, pZ) > 80.0F)) {
            float f = (float)i / (float)pPath.getNodeCount() * 0.33F;
            int j = i == 0 ? 0 : Mth.hsvToRgb(f, 0.9F, 0.9F);
            int k = j >> 16 & 255;
            int l = j >> 8 & 255;
            int i1 = j & 255;
            bufferbuilder.vertex((double)node.x - pX + 0.5D, (double)node.y - pY + 0.5D, (double)node.z - pZ + 0.5D).color(k, l, i1, 255).endVertex();
         }
      }

      tesselator.end();
   }

   private static float distanceToCamera(BlockPos pPos, double pX, double pY, double pZ) {
      return (float)(Math.abs((double)pPos.getX() - pX) + Math.abs((double)pPos.getY() - pY) + Math.abs((double)pPos.getZ() - pZ));
   }
}