package net.minecraft.client.gui.screens;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class WinScreen extends Screen {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final ResourceLocation LOGO_LOCATION = new ResourceLocation("textures/gui/title/minecraft.png");
   private static final ResourceLocation EDITION_LOCATION = new ResourceLocation("textures/gui/title/edition.png");
   private static final ResourceLocation VIGNETTE_LOCATION = new ResourceLocation("textures/misc/vignette.png");
   private static final Component SECTION_HEADING = Component.literal("============").withStyle(ChatFormatting.WHITE);
   private static final String NAME_PREFIX = "           ";
   private static final String OBFUSCATE_TOKEN = "" + ChatFormatting.WHITE + ChatFormatting.OBFUSCATED + ChatFormatting.GREEN + ChatFormatting.AQUA;
   private static final int LOGO_WIDTH = 274;
   private static final float SPEEDUP_FACTOR = 5.0F;
   private static final float SPEEDUP_FACTOR_FAST = 15.0F;
   private final boolean poem;
   private final Runnable onFinished;
   private float scroll;
   private List<FormattedCharSequence> lines;
   private IntSet centeredLines;
   private int totalScrollLength;
   private boolean speedupActive;
   private final IntSet speedupModifiers = new IntOpenHashSet();
   private float scrollSpeed;
   private final float unmodifiedScrollSpeed;

   public WinScreen(boolean pPoem, Runnable pOnFinished) {
      super(GameNarrator.NO_TITLE);
      this.poem = pPoem;
      this.onFinished = pOnFinished;
      if (!pPoem) {
         this.unmodifiedScrollSpeed = 0.75F;
      } else {
         this.unmodifiedScrollSpeed = 0.5F;
      }

      this.scrollSpeed = this.unmodifiedScrollSpeed;
   }

   private float calculateScrollSpeed() {
      return this.speedupActive ? this.unmodifiedScrollSpeed * (5.0F + (float)this.speedupModifiers.size() * 15.0F) : this.unmodifiedScrollSpeed;
   }

   public void tick() {
      this.minecraft.getMusicManager().tick();
      this.minecraft.getSoundManager().tick(false);
      float f = (float)(this.totalScrollLength + this.height + this.height + 24);
      if (this.scroll > f) {
         this.respawn();
      }

   }

   public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
      if (pKeyCode != 341 && pKeyCode != 345) {
         if (pKeyCode == 32) {
            this.speedupActive = true;
         }
      } else {
         this.speedupModifiers.add(pKeyCode);
      }

      this.scrollSpeed = this.calculateScrollSpeed();
      return super.keyPressed(pKeyCode, pScanCode, pModifiers);
   }

   public boolean keyReleased(int pKeyCode, int pScanCode, int pModifiers) {
      if (pKeyCode == 32) {
         this.speedupActive = false;
      } else if (pKeyCode == 341 || pKeyCode == 345) {
         this.speedupModifiers.remove(pKeyCode);
      }

      this.scrollSpeed = this.calculateScrollSpeed();
      return super.keyReleased(pKeyCode, pScanCode, pModifiers);
   }

   public void onClose() {
      this.respawn();
   }

   private void respawn() {
      this.onFinished.run();
      this.minecraft.setScreen((Screen)null);
   }

   protected void init() {
      if (this.lines == null) {
         this.lines = Lists.newArrayList();
         this.centeredLines = new IntOpenHashSet();
         if (this.poem) {
            this.wrapCreditsIO("texts/end.txt", this::addPoemFile);
         }

         this.wrapCreditsIO("texts/credits.json", this::addCreditsFile);
         if (this.poem) {
            this.wrapCreditsIO("texts/postcredits.txt", this::addPoemFile);
         }

         this.totalScrollLength = this.lines.size() * 12;
      }
   }

   private void wrapCreditsIO(String pCreditsLocation, WinScreen.CreditsReader pReader) {
      try {
         Reader reader = this.minecraft.getResourceManager().openAsReader(new ResourceLocation(pCreditsLocation));

         try {
            pReader.read(reader);
         } catch (Throwable throwable1) {
            if (reader != null) {
               try {
                  reader.close();
               } catch (Throwable throwable) {
                  throwable1.addSuppressed(throwable);
               }
            }

            throw throwable1;
         }

         if (reader != null) {
            reader.close();
         }
      } catch (Exception exception) {
         LOGGER.error("Couldn't load credits", (Throwable)exception);
      }

   }

   private void addPoemFile(Reader p_232818_) throws IOException {
      BufferedReader bufferedreader = new BufferedReader(p_232818_);
      RandomSource randomsource = RandomSource.create(8124371L);

      String s;
      while((s = bufferedreader.readLine()) != null) {
         int i;
         String s1;
         String s2;
         for(s = s.replaceAll("PLAYERNAME", this.minecraft.getUser().getName()); (i = s.indexOf(OBFUSCATE_TOKEN)) != -1; s = s1 + ChatFormatting.WHITE + ChatFormatting.OBFUSCATED + "XXXXXXXX".substring(0, randomsource.nextInt(4) + 3) + s2) {
            s1 = s.substring(0, i);
            s2 = s.substring(i + OBFUSCATE_TOKEN.length());
         }

         this.addPoemLines(s);
         this.addEmptyLine();
      }

      for(int j = 0; j < 8; ++j) {
         this.addEmptyLine();
      }

   }

   private void addCreditsFile(Reader p_232820_) {
      for(JsonElement jsonelement : GsonHelper.parseArray(p_232820_)) {
         JsonObject jsonobject = jsonelement.getAsJsonObject();
         String s = jsonobject.get("section").getAsString();
         this.addCreditsLine(SECTION_HEADING, true);
         this.addCreditsLine(Component.literal(s).withStyle(ChatFormatting.YELLOW), true);
         this.addCreditsLine(SECTION_HEADING, true);
         this.addEmptyLine();
         this.addEmptyLine();

         for(JsonElement jsonelement1 : jsonobject.getAsJsonArray("titles")) {
            JsonObject jsonobject1 = jsonelement1.getAsJsonObject();
            String s1 = jsonobject1.get("title").getAsString();
            JsonArray jsonarray = jsonobject1.getAsJsonArray("names");
            this.addCreditsLine(Component.literal(s1).withStyle(ChatFormatting.GRAY), false);

            for(JsonElement jsonelement2 : jsonarray) {
               String s2 = jsonelement2.getAsString();
               this.addCreditsLine(Component.literal("           ").append(s2).withStyle(ChatFormatting.WHITE), false);
            }

            this.addEmptyLine();
            this.addEmptyLine();
         }
      }

   }

   private void addEmptyLine() {
      this.lines.add(FormattedCharSequence.EMPTY);
   }

   private void addPoemLines(String pText) {
      this.lines.addAll(this.minecraft.font.split(Component.literal(pText), 274));
   }

   private void addCreditsLine(Component pCreditsLine, boolean pCentered) {
      if (pCentered) {
         this.centeredLines.add(this.lines.size());
      }

      this.lines.add(pCreditsLine.getVisualOrderText());
   }

   private void renderBg() {
      RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
      RenderSystem.setShaderTexture(0, GuiComponent.BACKGROUND_LOCATION);
      int i = this.width;
      float f = -this.scroll * 0.5F;
      float f1 = (float)this.height - 0.5F * this.scroll;
      float f2 = 0.015625F;
      float f3 = this.scroll / this.unmodifiedScrollSpeed;
      float f4 = f3 * 0.02F;
      float f5 = (float)(this.totalScrollLength + this.height + this.height + 24) / this.unmodifiedScrollSpeed;
      float f6 = (f5 - 20.0F - f3) * 0.005F;
      if (f6 < f4) {
         f4 = f6;
      }

      if (f4 > 1.0F) {
         f4 = 1.0F;
      }

      f4 *= f4;
      f4 = f4 * 96.0F / 255.0F;
      Tesselator tesselator = Tesselator.getInstance();
      BufferBuilder bufferbuilder = tesselator.getBuilder();
      bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
      bufferbuilder.vertex(0.0D, (double)this.height, (double)this.getBlitOffset()).uv(0.0F, f * 0.015625F).color(f4, f4, f4, 1.0F).endVertex();
      bufferbuilder.vertex((double)i, (double)this.height, (double)this.getBlitOffset()).uv((float)i * 0.015625F, f * 0.015625F).color(f4, f4, f4, 1.0F).endVertex();
      bufferbuilder.vertex((double)i, 0.0D, (double)this.getBlitOffset()).uv((float)i * 0.015625F, f1 * 0.015625F).color(f4, f4, f4, 1.0F).endVertex();
      bufferbuilder.vertex(0.0D, 0.0D, (double)this.getBlitOffset()).uv(0.0F, f1 * 0.015625F).color(f4, f4, f4, 1.0F).endVertex();
      tesselator.end();
   }

   public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
      this.scroll += pPartialTick * this.scrollSpeed;
      this.renderBg();
      int i = this.width / 2 - 137;
      int j = this.height + 50;
      float f = -this.scroll;
      pPoseStack.pushPose();
      pPoseStack.translate(0.0D, (double)f, 0.0D);
      RenderSystem.setShaderTexture(0, LOGO_LOCATION);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.enableBlend();
      this.blitOutlineBlack(i, j, (p_96890_, p_96891_) -> {
         this.blit(pPoseStack, p_96890_ + 0, p_96891_, 0, 0, 155, 44);
         this.blit(pPoseStack, p_96890_ + 155, p_96891_, 0, 45, 155, 44);
      });
      RenderSystem.disableBlend();
      RenderSystem.setShaderTexture(0, EDITION_LOCATION);
      blit(pPoseStack, i + 88, j + 37, 0.0F, 0.0F, 98, 14, 128, 16);
      int k = j + 100;

      for(int l = 0; l < this.lines.size(); ++l) {
         if (l == this.lines.size() - 1) {
            float f1 = (float)k + f - (float)(this.height / 2 - 6);
            if (f1 < 0.0F) {
               pPoseStack.translate(0.0D, (double)(-f1), 0.0D);
            }
         }

         if ((float)k + f + 12.0F + 8.0F > 0.0F && (float)k + f < (float)this.height) {
            FormattedCharSequence formattedcharsequence = this.lines.get(l);
            if (this.centeredLines.contains(l)) {
               this.font.drawShadow(pPoseStack, formattedcharsequence, (float)(i + (274 - this.font.width(formattedcharsequence)) / 2), (float)k, 16777215);
            } else {
               this.font.drawShadow(pPoseStack, formattedcharsequence, (float)i, (float)k, 16777215);
            }
         }

         k += 12;
      }

      pPoseStack.popPose();
      RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
      RenderSystem.setShaderTexture(0, VIGNETTE_LOCATION);
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR);
      int i1 = this.width;
      int j1 = this.height;
      Tesselator tesselator = Tesselator.getInstance();
      BufferBuilder bufferbuilder = tesselator.getBuilder();
      bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
      bufferbuilder.vertex(0.0D, (double)j1, (double)this.getBlitOffset()).uv(0.0F, 1.0F).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
      bufferbuilder.vertex((double)i1, (double)j1, (double)this.getBlitOffset()).uv(1.0F, 1.0F).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
      bufferbuilder.vertex((double)i1, 0.0D, (double)this.getBlitOffset()).uv(1.0F, 0.0F).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
      bufferbuilder.vertex(0.0D, 0.0D, (double)this.getBlitOffset()).uv(0.0F, 0.0F).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
      tesselator.end();
      RenderSystem.disableBlend();
      super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
   }

   @FunctionalInterface
   @OnlyIn(Dist.CLIENT)
   interface CreditsReader {
      void read(Reader pReader) throws IOException;
   }
}