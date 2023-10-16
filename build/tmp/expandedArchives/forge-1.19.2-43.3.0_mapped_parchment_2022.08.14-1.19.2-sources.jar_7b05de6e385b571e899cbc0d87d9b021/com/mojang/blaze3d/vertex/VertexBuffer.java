package com.mojang.blaze3d.vertex;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Matrix4f;
import java.nio.ByteBuffer;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class VertexBuffer implements AutoCloseable {
   private int vertexBufferId;
   private int indexBufferId;
   private int arrayObjectId;
   @Nullable
   private VertexFormat format;
   @Nullable
   private RenderSystem.AutoStorageIndexBuffer sequentialIndices;
   private VertexFormat.IndexType indexType;
   private int indexCount;
   private VertexFormat.Mode mode;

   public VertexBuffer() {
      RenderSystem.assertOnRenderThread();
      this.vertexBufferId = GlStateManager._glGenBuffers();
      this.indexBufferId = GlStateManager._glGenBuffers();
      this.arrayObjectId = GlStateManager._glGenVertexArrays();
   }

   public void upload(BufferBuilder.RenderedBuffer pBuffer) {
      if (!this.isInvalid()) {
         RenderSystem.assertOnRenderThread();

         try {
            BufferBuilder.DrawState bufferbuilder$drawstate = pBuffer.drawState();
            this.format = this.uploadVertexBuffer(bufferbuilder$drawstate, pBuffer.vertexBuffer());
            this.sequentialIndices = this.uploadIndexBuffer(bufferbuilder$drawstate, pBuffer.indexBuffer());
            this.indexCount = bufferbuilder$drawstate.indexCount();
            this.indexType = bufferbuilder$drawstate.indexType();
            this.mode = bufferbuilder$drawstate.mode();
         } finally {
            pBuffer.release();
         }

      }
   }

   private VertexFormat uploadVertexBuffer(BufferBuilder.DrawState pDrawState, ByteBuffer pBuffer) {
      boolean flag = false;
      if (!pDrawState.format().equals(this.format)) {
         if (this.format != null) {
            this.format.clearBufferState();
         }

         GlStateManager._glBindBuffer(34962, this.vertexBufferId);
         pDrawState.format().setupBufferState();
         flag = true;
      }

      if (!pDrawState.indexOnly()) {
         if (!flag) {
            GlStateManager._glBindBuffer(34962, this.vertexBufferId);
         }

         RenderSystem.glBufferData(34962, pBuffer, 35044);
      }

      return pDrawState.format();
   }

   @Nullable
   private RenderSystem.AutoStorageIndexBuffer uploadIndexBuffer(BufferBuilder.DrawState pDrawState, ByteBuffer pBuffer) {
      if (!pDrawState.sequentialIndex()) {
         GlStateManager._glBindBuffer(34963, this.indexBufferId);
         RenderSystem.glBufferData(34963, pBuffer, 35044);
         return null;
      } else {
         RenderSystem.AutoStorageIndexBuffer rendersystem$autostorageindexbuffer = RenderSystem.getSequentialBuffer(pDrawState.mode());
         if (rendersystem$autostorageindexbuffer != this.sequentialIndices || !rendersystem$autostorageindexbuffer.hasStorage(pDrawState.indexCount())) {
            rendersystem$autostorageindexbuffer.bind(pDrawState.indexCount());
         }

         return rendersystem$autostorageindexbuffer;
      }
   }

   public void bind() {
      BufferUploader.invalidate();
      GlStateManager._glBindVertexArray(this.arrayObjectId);
   }

   public static void unbind() {
      BufferUploader.invalidate();
      GlStateManager._glBindVertexArray(0);
   }

   public void draw() {
      RenderSystem.drawElements(this.mode.asGLMode, this.indexCount, this.getIndexType().asGLType);
   }

   private VertexFormat.IndexType getIndexType() {
      RenderSystem.AutoStorageIndexBuffer rendersystem$autostorageindexbuffer = this.sequentialIndices;
      return rendersystem$autostorageindexbuffer != null ? rendersystem$autostorageindexbuffer.type() : this.indexType;
   }

   public void drawWithShader(Matrix4f pModelViewMatrix, Matrix4f pProjectionMatrix, ShaderInstance pShaderInstance) {
      if (!RenderSystem.isOnRenderThread()) {
         RenderSystem.recordRenderCall(() -> {
            this._drawWithShader(pModelViewMatrix.copy(), pProjectionMatrix.copy(), pShaderInstance);
         });
      } else {
         this._drawWithShader(pModelViewMatrix, pProjectionMatrix, pShaderInstance);
      }

   }

   private void _drawWithShader(Matrix4f pModelViewMatrix, Matrix4f pProjectionMatrix, ShaderInstance pShaderInstance) {
      for(int i = 0; i < 12; ++i) {
         int j = RenderSystem.getShaderTexture(i);
         pShaderInstance.setSampler("Sampler" + i, j);
      }

      if (pShaderInstance.MODEL_VIEW_MATRIX != null) {
         pShaderInstance.MODEL_VIEW_MATRIX.set(pModelViewMatrix);
      }

      if (pShaderInstance.PROJECTION_MATRIX != null) {
         pShaderInstance.PROJECTION_MATRIX.set(pProjectionMatrix);
      }

      if (pShaderInstance.INVERSE_VIEW_ROTATION_MATRIX != null) {
         pShaderInstance.INVERSE_VIEW_ROTATION_MATRIX.set(RenderSystem.getInverseViewRotationMatrix());
      }

      if (pShaderInstance.COLOR_MODULATOR != null) {
         pShaderInstance.COLOR_MODULATOR.set(RenderSystem.getShaderColor());
      }

      if (pShaderInstance.FOG_START != null) {
         pShaderInstance.FOG_START.set(RenderSystem.getShaderFogStart());
      }

      if (pShaderInstance.FOG_END != null) {
         pShaderInstance.FOG_END.set(RenderSystem.getShaderFogEnd());
      }

      if (pShaderInstance.FOG_COLOR != null) {
         pShaderInstance.FOG_COLOR.set(RenderSystem.getShaderFogColor());
      }

      if (pShaderInstance.FOG_SHAPE != null) {
         pShaderInstance.FOG_SHAPE.set(RenderSystem.getShaderFogShape().getIndex());
      }

      if (pShaderInstance.TEXTURE_MATRIX != null) {
         pShaderInstance.TEXTURE_MATRIX.set(RenderSystem.getTextureMatrix());
      }

      if (pShaderInstance.GAME_TIME != null) {
         pShaderInstance.GAME_TIME.set(RenderSystem.getShaderGameTime());
      }

      if (pShaderInstance.SCREEN_SIZE != null) {
         Window window = Minecraft.getInstance().getWindow();
         pShaderInstance.SCREEN_SIZE.set((float)window.getWidth(), (float)window.getHeight());
      }

      if (pShaderInstance.LINE_WIDTH != null && (this.mode == VertexFormat.Mode.LINES || this.mode == VertexFormat.Mode.LINE_STRIP)) {
         pShaderInstance.LINE_WIDTH.set(RenderSystem.getShaderLineWidth());
      }

      RenderSystem.setupShaderLights(pShaderInstance);
      pShaderInstance.apply();
      this.draw();
      pShaderInstance.clear();
   }

   public void close() {
      if (this.vertexBufferId >= 0) {
         RenderSystem.glDeleteBuffers(this.vertexBufferId);
         this.vertexBufferId = -1;
      }

      if (this.indexBufferId >= 0) {
         RenderSystem.glDeleteBuffers(this.indexBufferId);
         this.indexBufferId = -1;
      }

      if (this.arrayObjectId >= 0) {
         RenderSystem.glDeleteVertexArrays(this.arrayObjectId);
         this.arrayObjectId = -1;
      }

   }

   public VertexFormat getFormat() {
      return this.format;
   }

   public boolean isInvalid() {
      return this.arrayObjectId == -1;
   }
}