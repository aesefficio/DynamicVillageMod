package net.minecraft.client.gui;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Vector3f;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.gui.components.SubtitleOverlay;
import net.minecraft.client.gui.components.spectator.SpectatorGui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.MobEffectTextureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringUtil;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class Gui extends GuiComponent {
   protected static final ResourceLocation VIGNETTE_LOCATION = new ResourceLocation("textures/misc/vignette.png");
   protected static final ResourceLocation WIDGETS_LOCATION = new ResourceLocation("textures/gui/widgets.png");
   protected static final ResourceLocation PUMPKIN_BLUR_LOCATION = new ResourceLocation("textures/misc/pumpkinblur.png");
   protected static final ResourceLocation SPYGLASS_SCOPE_LOCATION = new ResourceLocation("textures/misc/spyglass_scope.png");
   protected static final ResourceLocation POWDER_SNOW_OUTLINE_LOCATION = new ResourceLocation("textures/misc/powder_snow_outline.png");
   protected static final Component DEMO_EXPIRED_TEXT = Component.translatable("demo.demoExpired");
   protected static final Component SAVING_TEXT = Component.translatable("menu.savingLevel");
   protected static final int COLOR_WHITE = 16777215;
   protected static final float MIN_CROSSHAIR_ATTACK_SPEED = 5.0F;
   protected static final int NUM_HEARTS_PER_ROW = 10;
   protected static final int LINE_HEIGHT = 10;
   protected static final String SPACER = ": ";
   protected static final float PORTAL_OVERLAY_ALPHA_MIN = 0.2F;
   protected static final int HEART_SIZE = 9;
   protected static final int HEART_SEPARATION = 8;
   protected static final float AUTOSAVE_FADE_SPEED_FACTOR = 0.2F;
   protected final RandomSource random = RandomSource.create();
   protected final Minecraft minecraft;
   protected final ItemRenderer itemRenderer;
   protected final ChatComponent chat;
   protected int tickCount;
   @Nullable
   protected Component overlayMessageString;
   protected int overlayMessageTime;
   protected boolean animateOverlayMessageColor;
   protected boolean chatDisabledByPlayerShown;
   public float vignetteBrightness = 1.0F;
   protected int toolHighlightTimer;
   protected ItemStack lastToolHighlight = ItemStack.EMPTY;
   protected final DebugScreenOverlay debugScreen;
   protected final SubtitleOverlay subtitleOverlay;
   /** The spectator GUI for this in-game GUI instance */
   protected final SpectatorGui spectatorGui;
   protected final PlayerTabOverlay tabList;
   protected final BossHealthOverlay bossOverlay;
   /** A timer for the current title and subtitle displayed */
   protected int titleTime;
   /** The current title displayed */
   @Nullable
   protected Component title;
   /** The current sub-title displayed */
   @Nullable
   protected Component subtitle;
   /** The time that the title take to fade in */
   protected int titleFadeInTime;
   /** The time that the title is display */
   protected int titleStayTime;
   /** The time that the title take to fade out */
   protected int titleFadeOutTime;
   protected int lastHealth;
   protected int displayHealth;
   /** The last recorded system time */
   protected long lastHealthTime;
   /** Used with updateCounter to make the heart bar flash */
   protected long healthBlinkTime;
   protected int screenWidth;
   protected int screenHeight;
   protected float autosaveIndicatorValue;
   protected float lastAutosaveIndicatorValue;
   protected float scopeScale;

   public Gui(Minecraft pMinecraft, ItemRenderer pItemRenderer) {
      this.minecraft = pMinecraft;
      this.itemRenderer = pItemRenderer;
      this.debugScreen = new DebugScreenOverlay(pMinecraft);
      this.spectatorGui = new SpectatorGui(pMinecraft);
      this.chat = new ChatComponent(pMinecraft);
      this.tabList = new PlayerTabOverlay(pMinecraft, this);
      this.bossOverlay = new BossHealthOverlay(pMinecraft);
      this.subtitleOverlay = new SubtitleOverlay(pMinecraft);
      this.resetTitleTimes();
   }

   /**
    * Set the differents times for the titles to their default values
    */
   public void resetTitleTimes() {
      this.titleFadeInTime = 10;
      this.titleStayTime = 70;
      this.titleFadeOutTime = 20;
   }

   public void render(PoseStack pPoseStack, float pPartialTick) {
      this.screenWidth = this.minecraft.getWindow().getGuiScaledWidth();
      this.screenHeight = this.minecraft.getWindow().getGuiScaledHeight();
      Font font = this.getFont();
      RenderSystem.enableBlend();
      if (Minecraft.useFancyGraphics()) {
         this.renderVignette(this.minecraft.getCameraEntity());
      } else {
         RenderSystem.enableDepthTest();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.defaultBlendFunc();
      }

      float f = this.minecraft.getDeltaFrameTime();
      this.scopeScale = Mth.lerp(0.5F * f, this.scopeScale, 1.125F);
      if (this.minecraft.options.getCameraType().isFirstPerson()) {
         if (this.minecraft.player.isScoping()) {
            this.renderSpyglassOverlay(this.scopeScale);
         } else {
            this.scopeScale = 0.5F;
            ItemStack itemstack = this.minecraft.player.getInventory().getArmor(3);
            if (itemstack.is(Blocks.CARVED_PUMPKIN.asItem())) {
               this.renderTextureOverlay(PUMPKIN_BLUR_LOCATION, 1.0F);
            }
         }
      }

      if (this.minecraft.player.getTicksFrozen() > 0) {
         this.renderTextureOverlay(POWDER_SNOW_OUTLINE_LOCATION, this.minecraft.player.getPercentFrozen());
      }

      float f2 = Mth.lerp(pPartialTick, this.minecraft.player.oPortalTime, this.minecraft.player.portalTime);
      if (f2 > 0.0F && !this.minecraft.player.hasEffect(MobEffects.CONFUSION)) {
         this.renderPortalOverlay(f2);
      }

      if (this.minecraft.gameMode.getPlayerMode() == GameType.SPECTATOR) {
         this.spectatorGui.renderHotbar(pPoseStack);
      } else if (!this.minecraft.options.hideGui) {
         this.renderHotbar(pPartialTick, pPoseStack);
      }

      if (!this.minecraft.options.hideGui) {
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.setShader(GameRenderer::getPositionTexShader);
         RenderSystem.setShaderTexture(0, GUI_ICONS_LOCATION);
         RenderSystem.enableBlend();
         this.renderCrosshair(pPoseStack);
         RenderSystem.setShader(GameRenderer::getPositionTexShader);
         RenderSystem.defaultBlendFunc();
         this.minecraft.getProfiler().push("bossHealth");
         this.bossOverlay.render(pPoseStack);
         this.minecraft.getProfiler().pop();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.setShaderTexture(0, GUI_ICONS_LOCATION);
         if (this.minecraft.gameMode.canHurtPlayer()) {
            this.renderPlayerHealth(pPoseStack);
         }

         this.renderVehicleHealth(pPoseStack);
         RenderSystem.disableBlend();
         int i = this.screenWidth / 2 - 91;
         if (this.minecraft.player.isRidingJumpable()) {
            this.renderJumpMeter(pPoseStack, i);
         } else if (this.minecraft.gameMode.hasExperience()) {
            this.renderExperienceBar(pPoseStack, i);
         }

         if (this.minecraft.options.heldItemTooltips && this.minecraft.gameMode.getPlayerMode() != GameType.SPECTATOR) {
            this.renderSelectedItemName(pPoseStack);
         } else if (this.minecraft.player.isSpectator()) {
            this.spectatorGui.renderTooltip(pPoseStack);
         }
      }

      if (this.minecraft.player.getSleepTimer() > 0) {
         this.minecraft.getProfiler().push("sleep");
         RenderSystem.disableDepthTest();
         float f3 = (float)this.minecraft.player.getSleepTimer();
         float f1 = f3 / 100.0F;
         if (f1 > 1.0F) {
            f1 = 1.0F - (f3 - 100.0F) / 10.0F;
         }

         int j = (int)(220.0F * f1) << 24 | 1052704;
         fill(pPoseStack, 0, 0, this.screenWidth, this.screenHeight, j);
         RenderSystem.enableDepthTest();
         this.minecraft.getProfiler().pop();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      }

      if (this.minecraft.isDemo()) {
         this.renderDemoOverlay(pPoseStack);
      }

      this.renderEffects(pPoseStack);
      if (this.minecraft.options.renderDebug) {
         this.debugScreen.render(pPoseStack);
      }

      if (!this.minecraft.options.hideGui) {
         if (this.overlayMessageString != null && this.overlayMessageTime > 0) {
            this.minecraft.getProfiler().push("overlayMessage");
            float f4 = (float)this.overlayMessageTime - pPartialTick;
            int i1 = (int)(f4 * 255.0F / 20.0F);
            if (i1 > 255) {
               i1 = 255;
            }

            if (i1 > 8) {
               pPoseStack.pushPose();
               pPoseStack.translate((double)(this.screenWidth / 2), (double)(this.screenHeight - 68), 0.0D);
               RenderSystem.enableBlend();
               RenderSystem.defaultBlendFunc();
               int k1 = 16777215;
               if (this.animateOverlayMessageColor) {
                  k1 = Mth.hsvToRgb(f4 / 50.0F, 0.7F, 0.6F) & 16777215;
               }

               int k = i1 << 24 & -16777216;
               int l = font.width(this.overlayMessageString);
               this.drawBackdrop(pPoseStack, font, -4, l, 16777215 | k);
               font.drawShadow(pPoseStack, this.overlayMessageString, (float)(-l / 2), -4.0F, k1 | k);
               RenderSystem.disableBlend();
               pPoseStack.popPose();
            }

            this.minecraft.getProfiler().pop();
         }

         if (this.title != null && this.titleTime > 0) {
            this.minecraft.getProfiler().push("titleAndSubtitle");
            float f5 = (float)this.titleTime - pPartialTick;
            int j1 = 255;
            if (this.titleTime > this.titleFadeOutTime + this.titleStayTime) {
               float f6 = (float)(this.titleFadeInTime + this.titleStayTime + this.titleFadeOutTime) - f5;
               j1 = (int)(f6 * 255.0F / (float)this.titleFadeInTime);
            }

            if (this.titleTime <= this.titleFadeOutTime) {
               j1 = (int)(f5 * 255.0F / (float)this.titleFadeOutTime);
            }

            j1 = Mth.clamp(j1, 0, 255);
            if (j1 > 8) {
               pPoseStack.pushPose();
               pPoseStack.translate((double)(this.screenWidth / 2), (double)(this.screenHeight / 2), 0.0D);
               RenderSystem.enableBlend();
               RenderSystem.defaultBlendFunc();
               pPoseStack.pushPose();
               pPoseStack.scale(4.0F, 4.0F, 4.0F);
               int l1 = j1 << 24 & -16777216;
               int i2 = font.width(this.title);
               this.drawBackdrop(pPoseStack, font, -10, i2, 16777215 | l1);
               font.drawShadow(pPoseStack, this.title, (float)(-i2 / 2), -10.0F, 16777215 | l1);
               pPoseStack.popPose();
               if (this.subtitle != null) {
                  pPoseStack.pushPose();
                  pPoseStack.scale(2.0F, 2.0F, 2.0F);
                  int k2 = font.width(this.subtitle);
                  this.drawBackdrop(pPoseStack, font, 5, k2, 16777215 | l1);
                  font.drawShadow(pPoseStack, this.subtitle, (float)(-k2 / 2), 5.0F, 16777215 | l1);
                  pPoseStack.popPose();
               }

               RenderSystem.disableBlend();
               pPoseStack.popPose();
            }

            this.minecraft.getProfiler().pop();
         }

         this.subtitleOverlay.render(pPoseStack);
         Scoreboard scoreboard = this.minecraft.level.getScoreboard();
         Objective objective = null;
         PlayerTeam playerteam = scoreboard.getPlayersTeam(this.minecraft.player.getScoreboardName());
         if (playerteam != null) {
            int j2 = playerteam.getColor().getId();
            if (j2 >= 0) {
               objective = scoreboard.getDisplayObjective(3 + j2);
            }
         }

         Objective objective1 = objective != null ? objective : scoreboard.getDisplayObjective(1);
         if (objective1 != null) {
            this.displayScoreboardSidebar(pPoseStack, objective1);
         }

         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         pPoseStack.pushPose();
         pPoseStack.translate(0.0D, (double)(this.screenHeight - 48), 0.0D);
         this.minecraft.getProfiler().push("chat");
         this.chat.render(pPoseStack, this.tickCount);
         this.minecraft.getProfiler().pop();
         pPoseStack.popPose();
         objective1 = scoreboard.getDisplayObjective(0);
         if (!this.minecraft.options.keyPlayerList.isDown() || this.minecraft.isLocalServer() && this.minecraft.player.connection.getOnlinePlayers().size() <= 1 && objective1 == null) {
            this.tabList.setVisible(false);
         } else {
            this.tabList.setVisible(true);
            this.tabList.render(pPoseStack, this.screenWidth, scoreboard, objective1);
         }

         this.renderSavingIndicator(pPoseStack);
      }

      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   protected void drawBackdrop(PoseStack pPoseStack, Font pFont, int pHeightOffset, int pMessageWidth, int pColor) {
      int i = this.minecraft.options.getBackgroundColor(0.0F);
      if (i != 0) {
         int j = -pMessageWidth / 2;
         fill(pPoseStack, j - 2, pHeightOffset - 2, j + pMessageWidth + 2, pHeightOffset + 9 + 2, FastColor.ARGB32.multiply(i, pColor));
      }

   }

   public void renderCrosshair(PoseStack pPoseStack) {
      Options options = this.minecraft.options;
      if (options.getCameraType().isFirstPerson()) {
         if (this.minecraft.gameMode.getPlayerMode() != GameType.SPECTATOR || this.canRenderCrosshairForSpectator(this.minecraft.hitResult)) {
            if (options.renderDebug && !options.hideGui && !this.minecraft.player.isReducedDebugInfo() && !options.reducedDebugInfo().get()) {
               Camera camera = this.minecraft.gameRenderer.getMainCamera();
               PoseStack posestack = RenderSystem.getModelViewStack();
               posestack.pushPose();
               posestack.translate((double)(this.screenWidth / 2), (double)(this.screenHeight / 2), (double)this.getBlitOffset());
               posestack.mulPose(Vector3f.XN.rotationDegrees(camera.getXRot()));
               posestack.mulPose(Vector3f.YP.rotationDegrees(camera.getYRot()));
               posestack.scale(-1.0F, -1.0F, -1.0F);
               RenderSystem.applyModelViewMatrix();
               RenderSystem.renderCrosshair(10);
               posestack.popPose();
               RenderSystem.applyModelViewMatrix();
            } else {
               RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
               int i = 15;
               this.blit(pPoseStack, (this.screenWidth - 15) / 2, (this.screenHeight - 15) / 2, 0, 0, 15, 15);
               if (this.minecraft.options.attackIndicator().get() == AttackIndicatorStatus.CROSSHAIR) {
                  float f = this.minecraft.player.getAttackStrengthScale(0.0F);
                  boolean flag = false;
                  if (this.minecraft.crosshairPickEntity != null && this.minecraft.crosshairPickEntity instanceof LivingEntity && f >= 1.0F) {
                     flag = this.minecraft.player.getCurrentItemAttackStrengthDelay() > 5.0F;
                     flag &= this.minecraft.crosshairPickEntity.isAlive();
                     flag &= this.minecraft.player.canHit(this.minecraft.crosshairPickEntity, 0);
                  }

                  int j = this.screenHeight / 2 - 7 + 16;
                  int k = this.screenWidth / 2 - 8;
                  if (flag) {
                     this.blit(pPoseStack, k, j, 68, 94, 16, 16);
                  } else if (f < 1.0F) {
                     int l = (int)(f * 17.0F);
                     this.blit(pPoseStack, k, j, 36, 94, 16, 4);
                     this.blit(pPoseStack, k, j, 52, 94, l, 4);
                  }
               }
            }

         }
      }
   }

   private boolean canRenderCrosshairForSpectator(HitResult pRayTrace) {
      if (pRayTrace == null) {
         return false;
      } else if (pRayTrace.getType() == HitResult.Type.ENTITY) {
         return ((EntityHitResult)pRayTrace).getEntity() instanceof MenuProvider;
      } else if (pRayTrace.getType() == HitResult.Type.BLOCK) {
         BlockPos blockpos = ((BlockHitResult)pRayTrace).getBlockPos();
         Level level = this.minecraft.level;
         return level.getBlockState(blockpos).getMenuProvider(level, blockpos) != null;
      } else {
         return false;
      }
   }

   public void renderEffects(PoseStack pPoseStack) {
      Collection<MobEffectInstance> collection = this.minecraft.player.getActiveEffects();
      if (!collection.isEmpty()) {
         Screen $$4 = this.minecraft.screen;
         if ($$4 instanceof EffectRenderingInventoryScreen) {
            EffectRenderingInventoryScreen effectrenderinginventoryscreen = (EffectRenderingInventoryScreen)$$4;
            if (effectrenderinginventoryscreen.canSeeEffects()) {
               return;
            }
         }

         RenderSystem.enableBlend();
         int j1 = 0;
         int k1 = 0;
         MobEffectTextureManager mobeffecttexturemanager = this.minecraft.getMobEffectTextures();
         List<Runnable> list = Lists.newArrayListWithExpectedSize(collection.size());
         RenderSystem.setShaderTexture(0, AbstractContainerScreen.INVENTORY_LOCATION);

         for(MobEffectInstance mobeffectinstance : Ordering.natural().reverse().sortedCopy(collection)) {
            MobEffect mobeffect = mobeffectinstance.getEffect();
            var renderer = net.minecraftforge.client.extensions.common.IClientMobEffectExtensions.of(mobeffectinstance);
            if (!renderer.isVisibleInGui(mobeffectinstance)) continue;
            // Rebind in case previous renderHUDEffect changed texture
            RenderSystem.setShaderTexture(0, AbstractContainerScreen.INVENTORY_LOCATION);
            if (mobeffectinstance.showIcon()) {
               int i = this.screenWidth;
               int j = 1;
               if (this.minecraft.isDemo()) {
                  j += 15;
               }

               if (mobeffect.isBeneficial()) {
                  ++j1;
                  i -= 25 * j1;
               } else {
                  ++k1;
                  i -= 25 * k1;
                  j += 26;
               }

               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
               float f = 1.0F;
               if (mobeffectinstance.isAmbient()) {
                  this.blit(pPoseStack, i, j, 165, 166, 24, 24);
               } else {
                  this.blit(pPoseStack, i, j, 141, 166, 24, 24);
                  if (mobeffectinstance.getDuration() <= 200) {
                     int k = 10 - mobeffectinstance.getDuration() / 20;
                     f = Mth.clamp((float)mobeffectinstance.getDuration() / 10.0F / 5.0F * 0.5F, 0.0F, 0.5F) + Mth.cos((float)mobeffectinstance.getDuration() * (float)Math.PI / 5.0F) * Mth.clamp((float)k / 10.0F * 0.25F, 0.0F, 0.25F);
                  }
               }

               if (renderer.renderGuiIcon(mobeffectinstance, this, pPoseStack, i, j, this.getBlitOffset(), f)) continue;
               TextureAtlasSprite textureatlassprite = mobeffecttexturemanager.get(mobeffect);
               int l = i;
               int i1 = j;
               float f1 = f;
               list.add(() -> {
                  RenderSystem.setShaderTexture(0, textureatlassprite.atlas().location());
                  RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, f1);
                  blit(pPoseStack, l + 3, i1 + 3, this.getBlitOffset(), 18, 18, textureatlassprite);
               });
            }
         }

         list.forEach(Runnable::run);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   public void renderHotbar(float pPartialTick, PoseStack pPoseStack) {
      Player player = this.getCameraPlayer();
      if (player != null) {
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.setShader(GameRenderer::getPositionTexShader);
         RenderSystem.setShaderTexture(0, WIDGETS_LOCATION);
         ItemStack itemstack = player.getOffhandItem();
         HumanoidArm humanoidarm = player.getMainArm().getOpposite();
         int i = this.screenWidth / 2;
         int j = this.getBlitOffset();
         int k = 182;
         int l = 91;
         this.setBlitOffset(-90);
         this.blit(pPoseStack, i - 91, this.screenHeight - 22, 0, 0, 182, 22);
         this.blit(pPoseStack, i - 91 - 1 + player.getInventory().selected * 20, this.screenHeight - 22 - 1, 0, 22, 24, 22);
         if (!itemstack.isEmpty()) {
            if (humanoidarm == HumanoidArm.LEFT) {
               this.blit(pPoseStack, i - 91 - 29, this.screenHeight - 23, 24, 22, 29, 24);
            } else {
               this.blit(pPoseStack, i + 91, this.screenHeight - 23, 53, 22, 29, 24);
            }
         }

         this.setBlitOffset(j);
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         int i1 = 1;

         for(int j1 = 0; j1 < 9; ++j1) {
            int k1 = i - 90 + j1 * 20 + 2;
            int l1 = this.screenHeight - 16 - 3;
            this.renderSlot(k1, l1, pPartialTick, player, player.getInventory().items.get(j1), i1++);
         }

         if (!itemstack.isEmpty()) {
            int j2 = this.screenHeight - 16 - 3;
            if (humanoidarm == HumanoidArm.LEFT) {
               this.renderSlot(i - 91 - 26, j2, pPartialTick, player, itemstack, i1++);
            } else {
               this.renderSlot(i + 91 + 10, j2, pPartialTick, player, itemstack, i1++);
            }
         }

         if (this.minecraft.options.attackIndicator().get() == AttackIndicatorStatus.HOTBAR) {
            float f = this.minecraft.player.getAttackStrengthScale(0.0F);
            if (f < 1.0F) {
               int k2 = this.screenHeight - 20;
               int l2 = i + 91 + 6;
               if (humanoidarm == HumanoidArm.RIGHT) {
                  l2 = i - 91 - 22;
               }

               RenderSystem.setShaderTexture(0, GuiComponent.GUI_ICONS_LOCATION);
               int i2 = (int)(f * 19.0F);
               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
               this.blit(pPoseStack, l2, k2, 0, 94, 18, 18);
               this.blit(pPoseStack, l2, k2 + 18 - i2, 18, 112 - i2, 18, i2);
            }
         }

         RenderSystem.disableBlend();
      }
   }

   public void renderJumpMeter(PoseStack pPoseStack, int pX) {
      this.minecraft.getProfiler().push("jumpBar");
      RenderSystem.setShaderTexture(0, GuiComponent.GUI_ICONS_LOCATION);
      float f = this.minecraft.player.getJumpRidingScale();
      int i = 182;
      int j = (int)(f * 183.0F);
      int k = this.screenHeight - 32 + 3;
      this.blit(pPoseStack, pX, k, 0, 84, 182, 5);
      if (j > 0) {
         this.blit(pPoseStack, pX, k, 0, 89, j, 5);
      }

      this.minecraft.getProfiler().pop();
   }

   public void renderExperienceBar(PoseStack pPoseStack, int pXPos) {
      this.minecraft.getProfiler().push("expBar");
      RenderSystem.setShaderTexture(0, GuiComponent.GUI_ICONS_LOCATION);
      int i = this.minecraft.player.getXpNeededForNextLevel();
      if (i > 0) {
         int j = 182;
         int k = (int)(this.minecraft.player.experienceProgress * 183.0F);
         int l = this.screenHeight - 32 + 3;
         this.blit(pPoseStack, pXPos, l, 0, 64, 182, 5);
         if (k > 0) {
            this.blit(pPoseStack, pXPos, l, 0, 69, k, 5);
         }
      }

      this.minecraft.getProfiler().pop();
      if (this.minecraft.player.experienceLevel > 0) {
         this.minecraft.getProfiler().push("expLevel");
         String s = "" + this.minecraft.player.experienceLevel;
         int i1 = (this.screenWidth - this.getFont().width(s)) / 2;
         int j1 = this.screenHeight - 31 - 4;
         this.getFont().draw(pPoseStack, s, (float)(i1 + 1), (float)j1, 0);
         this.getFont().draw(pPoseStack, s, (float)(i1 - 1), (float)j1, 0);
         this.getFont().draw(pPoseStack, s, (float)i1, (float)(j1 + 1), 0);
         this.getFont().draw(pPoseStack, s, (float)i1, (float)(j1 - 1), 0);
         this.getFont().draw(pPoseStack, s, (float)i1, (float)j1, 8453920);
         this.minecraft.getProfiler().pop();
      }

   }

   public void renderSelectedItemName(PoseStack pPoseStack) {
      this.minecraft.getProfiler().push("selectedItemName");
      if (this.toolHighlightTimer > 0 && !this.lastToolHighlight.isEmpty()) {
         MutableComponent mutablecomponent = Component.empty().append(this.lastToolHighlight.getHoverName()).withStyle(this.lastToolHighlight.getRarity().getStyleModifier());
         if (this.lastToolHighlight.hasCustomHoverName()) {
            mutablecomponent.withStyle(ChatFormatting.ITALIC);
         }

         Component highlightTip = this.lastToolHighlight.getHighlightTip(mutablecomponent);
         int i = this.getFont().width(highlightTip);
         int j = (this.screenWidth - i) / 2;
         int k = this.screenHeight - 59;
         if (!this.minecraft.gameMode.canHurtPlayer()) {
            k += 14;
         }

         int l = (int)((float)this.toolHighlightTimer * 256.0F / 10.0F);
         if (l > 255) {
            l = 255;
         }

         if (l > 0) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            fill(pPoseStack, j - 2, k - 2, j + i + 2, k + 9 + 2, this.minecraft.options.getBackgroundColor(0));
            Font font = net.minecraftforge.client.extensions.common.IClientItemExtensions.of(lastToolHighlight).getFont(lastToolHighlight, net.minecraftforge.client.extensions.common.IClientItemExtensions.FontContext.SELECTED_ITEM_NAME);
            if (font == null) {
               this.getFont().drawShadow(pPoseStack, highlightTip, (float)j, (float)k, 16777215 + (l << 24));
            } else {
               j = (this.screenWidth - font.width(highlightTip)) / 2;
               font.drawShadow(pPoseStack, highlightTip, (float)j, (float)k, 16777215 + (l << 24));
            }
            RenderSystem.disableBlend();
         }
      }

      this.minecraft.getProfiler().pop();
   }

   public void renderDemoOverlay(PoseStack pPoseStack) {
      this.minecraft.getProfiler().push("demo");
      Component component;
      if (this.minecraft.level.getGameTime() >= 120500L) {
         component = DEMO_EXPIRED_TEXT;
      } else {
         component = Component.translatable("demo.remainingTime", StringUtil.formatTickDuration((int)(120500L - this.minecraft.level.getGameTime())));
      }

      int i = this.getFont().width(component);
      this.getFont().drawShadow(pPoseStack, component, (float)(this.screenWidth - i - 10), 5.0F, 16777215);
      this.minecraft.getProfiler().pop();
   }

   public void displayScoreboardSidebar(PoseStack pPoseStack, Objective pObjective) {
      Scoreboard scoreboard = pObjective.getScoreboard();
      Collection<Score> collection = scoreboard.getPlayerScores(pObjective);
      List<Score> list = collection.stream().filter((p_93027_) -> {
         return p_93027_.getOwner() != null && !p_93027_.getOwner().startsWith("#");
      }).collect(Collectors.toList());
      if (list.size() > 15) {
         collection = Lists.newArrayList(Iterables.skip(list, collection.size() - 15));
      } else {
         collection = list;
      }

      List<Pair<Score, Component>> list1 = Lists.newArrayListWithCapacity(collection.size());
      Component component = pObjective.getDisplayName();
      int i = this.getFont().width(component);
      int j = i;
      int k = this.getFont().width(": ");

      for(Score score : collection) {
         PlayerTeam playerteam = scoreboard.getPlayersTeam(score.getOwner());
         Component component1 = PlayerTeam.formatNameForTeam(playerteam, Component.literal(score.getOwner()));
         list1.add(Pair.of(score, component1));
         j = Math.max(j, this.getFont().width(component1) + k + this.getFont().width(Integer.toString(score.getScore())));
      }

      int i2 = collection.size() * 9;
      int j2 = this.screenHeight / 2 + i2 / 3;
      int k2 = 3;
      int l2 = this.screenWidth - j - 3;
      int l = 0;
      int i1 = this.minecraft.options.getBackgroundColor(0.3F);
      int j1 = this.minecraft.options.getBackgroundColor(0.4F);

      for(Pair<Score, Component> pair : list1) {
         ++l;
         Score score1 = pair.getFirst();
         Component component2 = pair.getSecond();
         String s = "" + ChatFormatting.RED + score1.getScore();
         int k1 = j2 - l * 9;
         int l1 = this.screenWidth - 3 + 2;
         fill(pPoseStack, l2 - 2, k1, l1, k1 + 9, i1);
         this.getFont().draw(pPoseStack, component2, (float)l2, (float)k1, -1);
         this.getFont().draw(pPoseStack, s, (float)(l1 - this.getFont().width(s)), (float)k1, -1);
         if (l == collection.size()) {
            fill(pPoseStack, l2 - 2, k1 - 9 - 1, l1, k1 - 1, j1);
            fill(pPoseStack, l2 - 2, k1 - 1, l1, k1, i1);
            this.getFont().draw(pPoseStack, component, (float)(l2 + j / 2 - i / 2), (float)(k1 - 9), -1);
         }
      }

   }

   private Player getCameraPlayer() {
      return !(this.minecraft.getCameraEntity() instanceof Player) ? null : (Player)this.minecraft.getCameraEntity();
   }

   private LivingEntity getPlayerVehicleWithHealth() {
      Player player = this.getCameraPlayer();
      if (player != null) {
         Entity entity = player.getVehicle();
         if (entity == null) {
            return null;
         }

         if (entity instanceof LivingEntity) {
            return (LivingEntity)entity;
         }
      }

      return null;
   }

   private int getVehicleMaxHearts(LivingEntity pMountEntity) {
      if (pMountEntity != null && pMountEntity.showVehicleHealth()) {
         float f = pMountEntity.getMaxHealth();
         int i = (int)(f + 0.5F) / 2;
         if (i > 30) {
            i = 30;
         }

         return i;
      } else {
         return 0;
      }
   }

   private int getVisibleVehicleHeartRows(int pMountHealth) {
      return (int)Math.ceil((double)pMountHealth / 10.0D);
   }

   private void renderPlayerHealth(PoseStack pPoseStack) {
      Player player = this.getCameraPlayer();
      if (player != null) {
         int i = Mth.ceil(player.getHealth());
         boolean flag = this.healthBlinkTime > (long)this.tickCount && (this.healthBlinkTime - (long)this.tickCount) / 3L % 2L == 1L;
         long j = Util.getMillis();
         if (i < this.lastHealth && player.invulnerableTime > 0) {
            this.lastHealthTime = j;
            this.healthBlinkTime = (long)(this.tickCount + 20);
         } else if (i > this.lastHealth && player.invulnerableTime > 0) {
            this.lastHealthTime = j;
            this.healthBlinkTime = (long)(this.tickCount + 10);
         }

         if (j - this.lastHealthTime > 1000L) {
            this.lastHealth = i;
            this.displayHealth = i;
            this.lastHealthTime = j;
         }

         this.lastHealth = i;
         int k = this.displayHealth;
         this.random.setSeed((long)(this.tickCount * 312871));
         FoodData fooddata = player.getFoodData();
         int l = fooddata.getFoodLevel();
         int i1 = this.screenWidth / 2 - 91;
         int j1 = this.screenWidth / 2 + 91;
         int k1 = this.screenHeight - 39;
         float f = Math.max((float)player.getAttributeValue(Attributes.MAX_HEALTH), (float)Math.max(k, i));
         int l1 = Mth.ceil(player.getAbsorptionAmount());
         int i2 = Mth.ceil((f + (float)l1) / 2.0F / 10.0F);
         int j2 = Math.max(10 - (i2 - 2), 3);
         int k2 = k1 - (i2 - 1) * j2 - 10;
         int l2 = k1 - 10;
         int i3 = player.getArmorValue();
         int j3 = -1;
         if (player.hasEffect(MobEffects.REGENERATION)) {
            j3 = this.tickCount % Mth.ceil(f + 5.0F);
         }

         this.minecraft.getProfiler().push("armor");

         for(int k3 = 0; k3 < 10; ++k3) {
            if (i3 > 0) {
               int l3 = i1 + k3 * 8;
               if (k3 * 2 + 1 < i3) {
                  this.blit(pPoseStack, l3, k2, 34, 9, 9, 9);
               }

               if (k3 * 2 + 1 == i3) {
                  this.blit(pPoseStack, l3, k2, 25, 9, 9, 9);
               }

               if (k3 * 2 + 1 > i3) {
                  this.blit(pPoseStack, l3, k2, 16, 9, 9, 9);
               }
            }
         }

         this.minecraft.getProfiler().popPush("health");
         this.renderHearts(pPoseStack, player, i1, k1, j2, j3, f, i, k, l1, flag);
         LivingEntity livingentity = this.getPlayerVehicleWithHealth();
         int k5 = this.getVehicleMaxHearts(livingentity);
         if (k5 == 0) {
            this.minecraft.getProfiler().popPush("food");

            for(int i4 = 0; i4 < 10; ++i4) {
               int j4 = k1;
               int k4 = 16;
               int l4 = 0;
               if (player.hasEffect(MobEffects.HUNGER)) {
                  k4 += 36;
                  l4 = 13;
               }

               if (player.getFoodData().getSaturationLevel() <= 0.0F && this.tickCount % (l * 3 + 1) == 0) {
                  j4 = k1 + (this.random.nextInt(3) - 1);
               }

               int i5 = j1 - i4 * 8 - 9;
               this.blit(pPoseStack, i5, j4, 16 + l4 * 9, 27, 9, 9);
               if (i4 * 2 + 1 < l) {
                  this.blit(pPoseStack, i5, j4, k4 + 36, 27, 9, 9);
               }

               if (i4 * 2 + 1 == l) {
                  this.blit(pPoseStack, i5, j4, k4 + 45, 27, 9, 9);
               }
            }

            l2 -= 10;
         }

         this.minecraft.getProfiler().popPush("air");
         int l5 = player.getMaxAirSupply();
         int i6 = Math.min(player.getAirSupply(), l5);
         if (player.isEyeInFluid(FluidTags.WATER) || i6 < l5) {
            int j6 = this.getVisibleVehicleHeartRows(k5) - 1;
            l2 -= j6 * 10;
            int k6 = Mth.ceil((double)(i6 - 2) * 10.0D / (double)l5);
            int l6 = Mth.ceil((double)i6 * 10.0D / (double)l5) - k6;

            for(int j5 = 0; j5 < k6 + l6; ++j5) {
               if (j5 < k6) {
                  this.blit(pPoseStack, j1 - j5 * 8 - 9, l2, 16, 18, 9, 9);
               } else {
                  this.blit(pPoseStack, j1 - j5 * 8 - 9, l2, 25, 18, 9, 9);
               }
            }
         }

         this.minecraft.getProfiler().pop();
      }
   }

   protected void renderHearts(PoseStack pPoseStack, Player pPlayer, int pX, int pY, int pHeight, int p_168694_, float p_168695_, int p_168696_, int p_168697_, int p_168698_, boolean p_168699_) {
      Gui.HeartType gui$hearttype = Gui.HeartType.forPlayer(pPlayer);
      int i = 9 * (pPlayer.level.getLevelData().isHardcore() ? 5 : 0);
      int j = Mth.ceil((double)p_168695_ / 2.0D);
      int k = Mth.ceil((double)p_168698_ / 2.0D);
      int l = j * 2;

      for(int i1 = j + k - 1; i1 >= 0; --i1) {
         int j1 = i1 / 10;
         int k1 = i1 % 10;
         int l1 = pX + k1 * 8;
         int i2 = pY - j1 * pHeight;
         if (p_168696_ + p_168698_ <= 4) {
            i2 += this.random.nextInt(2);
         }

         if (i1 < j && i1 == p_168694_) {
            i2 -= 2;
         }

         this.renderHeart(pPoseStack, Gui.HeartType.CONTAINER, l1, i2, i, p_168699_, false);
         int j2 = i1 * 2;
         boolean flag = i1 >= j;
         if (flag) {
            int k2 = j2 - l;
            if (k2 < p_168698_) {
               boolean flag1 = k2 + 1 == p_168698_;
               this.renderHeart(pPoseStack, gui$hearttype == Gui.HeartType.WITHERED ? gui$hearttype : Gui.HeartType.ABSORBING, l1, i2, i, false, flag1);
            }
         }

         if (p_168699_ && j2 < p_168697_) {
            boolean flag2 = j2 + 1 == p_168697_;
            this.renderHeart(pPoseStack, gui$hearttype, l1, i2, i, true, flag2);
         }

         if (j2 < p_168696_) {
            boolean flag3 = j2 + 1 == p_168696_;
            this.renderHeart(pPoseStack, gui$hearttype, l1, i2, i, false, flag3);
         }
      }

   }

   private void renderHeart(PoseStack pPoseStack, Gui.HeartType pHeartType, int pX, int pY, int p_168705_, boolean p_168706_, boolean p_168707_) {
      this.blit(pPoseStack, pX, pY, pHeartType.getX(p_168707_, p_168706_), p_168705_, 9, 9);
   }

   private void renderVehicleHealth(PoseStack pPoseStack) {
      LivingEntity livingentity = this.getPlayerVehicleWithHealth();
      if (livingentity != null) {
         int i = this.getVehicleMaxHearts(livingentity);
         if (i != 0) {
            int j = (int)Math.ceil((double)livingentity.getHealth());
            this.minecraft.getProfiler().popPush("mountHealth");
            int k = this.screenHeight - 39;
            int l = this.screenWidth / 2 + 91;
            int i1 = k;
            int j1 = 0;

            for(boolean flag = false; i > 0; j1 += 20) {
               int k1 = Math.min(i, 10);
               i -= k1;

               for(int l1 = 0; l1 < k1; ++l1) {
                  int i2 = 52;
                  int j2 = 0;
                  int k2 = l - l1 * 8 - 9;
                  this.blit(pPoseStack, k2, i1, 52 + j2 * 9, 9, 9, 9);
                  if (l1 * 2 + 1 + j1 < j) {
                     this.blit(pPoseStack, k2, i1, 88, 9, 9, 9);
                  }

                  if (l1 * 2 + 1 + j1 == j) {
                     this.blit(pPoseStack, k2, i1, 97, 9, 9, 9);
                  }
               }

               i1 -= 10;
            }

         }
      }
   }

   protected void renderTextureOverlay(ResourceLocation pTextureLocation, float pAlpha) {
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShader(GameRenderer::getPositionTexShader);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, pAlpha);
      RenderSystem.setShaderTexture(0, pTextureLocation);
      Tesselator tesselator = Tesselator.getInstance();
      BufferBuilder bufferbuilder = tesselator.getBuilder();
      bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
      bufferbuilder.vertex(0.0D, (double)this.screenHeight, -90.0D).uv(0.0F, 1.0F).endVertex();
      bufferbuilder.vertex((double)this.screenWidth, (double)this.screenHeight, -90.0D).uv(1.0F, 1.0F).endVertex();
      bufferbuilder.vertex((double)this.screenWidth, 0.0D, -90.0D).uv(1.0F, 0.0F).endVertex();
      bufferbuilder.vertex(0.0D, 0.0D, -90.0D).uv(0.0F, 0.0F).endVertex();
      tesselator.end();
      RenderSystem.depthMask(true);
      RenderSystem.enableDepthTest();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   protected void renderSpyglassOverlay(float p_168676_) {
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShader(GameRenderer::getPositionTexShader);
      RenderSystem.setShaderTexture(0, SPYGLASS_SCOPE_LOCATION);
      Tesselator tesselator = Tesselator.getInstance();
      BufferBuilder bufferbuilder = tesselator.getBuilder();
      float f = (float)Math.min(this.screenWidth, this.screenHeight);
      float f1 = Math.min((float)this.screenWidth / f, (float)this.screenHeight / f) * p_168676_;
      float f2 = f * f1;
      float f3 = f * f1;
      float f4 = ((float)this.screenWidth - f2) / 2.0F;
      float f5 = ((float)this.screenHeight - f3) / 2.0F;
      float f6 = f4 + f2;
      float f7 = f5 + f3;
      bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
      bufferbuilder.vertex((double)f4, (double)f7, -90.0D).uv(0.0F, 1.0F).endVertex();
      bufferbuilder.vertex((double)f6, (double)f7, -90.0D).uv(1.0F, 1.0F).endVertex();
      bufferbuilder.vertex((double)f6, (double)f5, -90.0D).uv(1.0F, 0.0F).endVertex();
      bufferbuilder.vertex((double)f4, (double)f5, -90.0D).uv(0.0F, 0.0F).endVertex();
      tesselator.end();
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      RenderSystem.disableTexture();
      bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      bufferbuilder.vertex(0.0D, (double)this.screenHeight, -90.0D).color(0, 0, 0, 255).endVertex();
      bufferbuilder.vertex((double)this.screenWidth, (double)this.screenHeight, -90.0D).color(0, 0, 0, 255).endVertex();
      bufferbuilder.vertex((double)this.screenWidth, (double)f7, -90.0D).color(0, 0, 0, 255).endVertex();
      bufferbuilder.vertex(0.0D, (double)f7, -90.0D).color(0, 0, 0, 255).endVertex();
      bufferbuilder.vertex(0.0D, (double)f5, -90.0D).color(0, 0, 0, 255).endVertex();
      bufferbuilder.vertex((double)this.screenWidth, (double)f5, -90.0D).color(0, 0, 0, 255).endVertex();
      bufferbuilder.vertex((double)this.screenWidth, 0.0D, -90.0D).color(0, 0, 0, 255).endVertex();
      bufferbuilder.vertex(0.0D, 0.0D, -90.0D).color(0, 0, 0, 255).endVertex();
      bufferbuilder.vertex(0.0D, (double)f7, -90.0D).color(0, 0, 0, 255).endVertex();
      bufferbuilder.vertex((double)f4, (double)f7, -90.0D).color(0, 0, 0, 255).endVertex();
      bufferbuilder.vertex((double)f4, (double)f5, -90.0D).color(0, 0, 0, 255).endVertex();
      bufferbuilder.vertex(0.0D, (double)f5, -90.0D).color(0, 0, 0, 255).endVertex();
      bufferbuilder.vertex((double)f6, (double)f7, -90.0D).color(0, 0, 0, 255).endVertex();
      bufferbuilder.vertex((double)this.screenWidth, (double)f7, -90.0D).color(0, 0, 0, 255).endVertex();
      bufferbuilder.vertex((double)this.screenWidth, (double)f5, -90.0D).color(0, 0, 0, 255).endVertex();
      bufferbuilder.vertex((double)f6, (double)f5, -90.0D).color(0, 0, 0, 255).endVertex();
      tesselator.end();
      RenderSystem.enableTexture();
      RenderSystem.depthMask(true);
      RenderSystem.enableDepthTest();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private void updateVignetteBrightness(Entity pEntity) {
      if (pEntity != null) {
         BlockPos blockpos = new BlockPos(pEntity.getX(), pEntity.getEyeY(), pEntity.getZ());
         float f = LightTexture.getBrightness(pEntity.level.dimensionType(), pEntity.level.getMaxLocalRawBrightness(blockpos));
         float f1 = Mth.clamp(1.0F - f, 0.0F, 1.0F);
         this.vignetteBrightness += (f1 - this.vignetteBrightness) * 0.01F;
      }
   }

   public void renderVignette(Entity pEntity) {
      WorldBorder worldborder = this.minecraft.level.getWorldBorder();
      float f = (float)worldborder.getDistanceToBorder(pEntity);
      double d0 = Math.min(worldborder.getLerpSpeed() * (double)worldborder.getWarningTime() * 1000.0D, Math.abs(worldborder.getLerpTarget() - worldborder.getSize()));
      double d1 = Math.max((double)worldborder.getWarningBlocks(), d0);
      if ((double)f < d1) {
         f = 1.0F - (float)((double)f / d1);
      } else {
         f = 0.0F;
      }

      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);
      RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
      if (f > 0.0F) {
         f = Mth.clamp(f, 0.0F, 1.0F);
         RenderSystem.setShaderColor(0.0F, f, f, 1.0F);
      } else {
         float f1 = this.vignetteBrightness;
         f1 = Mth.clamp(f1, 0.0F, 1.0F);
         RenderSystem.setShaderColor(f1, f1, f1, 1.0F);
      }

      RenderSystem.setShader(GameRenderer::getPositionTexShader);
      RenderSystem.setShaderTexture(0, VIGNETTE_LOCATION);
      Tesselator tesselator = Tesselator.getInstance();
      BufferBuilder bufferbuilder = tesselator.getBuilder();
      bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
      bufferbuilder.vertex(0.0D, (double)this.screenHeight, -90.0D).uv(0.0F, 1.0F).endVertex();
      bufferbuilder.vertex((double)this.screenWidth, (double)this.screenHeight, -90.0D).uv(1.0F, 1.0F).endVertex();
      bufferbuilder.vertex((double)this.screenWidth, 0.0D, -90.0D).uv(1.0F, 0.0F).endVertex();
      bufferbuilder.vertex(0.0D, 0.0D, -90.0D).uv(0.0F, 0.0F).endVertex();
      tesselator.end();
      RenderSystem.depthMask(true);
      RenderSystem.enableDepthTest();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.defaultBlendFunc();
   }

   protected void renderPortalOverlay(float pTimeInPortal) {
      if (pTimeInPortal < 1.0F) {
         pTimeInPortal *= pTimeInPortal;
         pTimeInPortal *= pTimeInPortal;
         pTimeInPortal = pTimeInPortal * 0.8F + 0.2F;
      }

      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, pTimeInPortal);
      RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
      RenderSystem.setShader(GameRenderer::getPositionTexShader);
      TextureAtlasSprite textureatlassprite = this.minecraft.getBlockRenderer().getBlockModelShaper().getParticleIcon(Blocks.NETHER_PORTAL.defaultBlockState());
      float f = textureatlassprite.getU0();
      float f1 = textureatlassprite.getV0();
      float f2 = textureatlassprite.getU1();
      float f3 = textureatlassprite.getV1();
      Tesselator tesselator = Tesselator.getInstance();
      BufferBuilder bufferbuilder = tesselator.getBuilder();
      bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
      bufferbuilder.vertex(0.0D, (double)this.screenHeight, -90.0D).uv(f, f3).endVertex();
      bufferbuilder.vertex((double)this.screenWidth, (double)this.screenHeight, -90.0D).uv(f2, f3).endVertex();
      bufferbuilder.vertex((double)this.screenWidth, 0.0D, -90.0D).uv(f2, f1).endVertex();
      bufferbuilder.vertex(0.0D, 0.0D, -90.0D).uv(f, f1).endVertex();
      tesselator.end();
      RenderSystem.depthMask(true);
      RenderSystem.enableDepthTest();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private void renderSlot(int pX, int pY, float pPartialTick, Player pPlayer, ItemStack pStack, int p_168683_) {
      if (!pStack.isEmpty()) {
         PoseStack posestack = RenderSystem.getModelViewStack();
         float f = (float)pStack.getPopTime() - pPartialTick;
         if (f > 0.0F) {
            float f1 = 1.0F + f / 5.0F;
            posestack.pushPose();
            posestack.translate((double)(pX + 8), (double)(pY + 12), 0.0D);
            posestack.scale(1.0F / f1, (f1 + 1.0F) / 2.0F, 1.0F);
            posestack.translate((double)(-(pX + 8)), (double)(-(pY + 12)), 0.0D);
            RenderSystem.applyModelViewMatrix();
         }

         this.itemRenderer.renderAndDecorateItem(pPlayer, pStack, pX, pY, p_168683_);
         RenderSystem.setShader(GameRenderer::getPositionColorShader);
         if (f > 0.0F) {
            posestack.popPose();
            RenderSystem.applyModelViewMatrix();
         }

         this.itemRenderer.renderGuiItemDecorations(this.minecraft.font, pStack, pX, pY);
      }
   }

   public void tick(boolean pPause) {
      this.tickAutosaveIndicator();
      if (!pPause) {
         this.tick();
      }

   }

   /**
    * The update tick for the ingame UI
    */
   private void tick() {
      if (this.overlayMessageTime > 0) {
         --this.overlayMessageTime;
      }

      if (this.titleTime > 0) {
         --this.titleTime;
         if (this.titleTime <= 0) {
            this.title = null;
            this.subtitle = null;
         }
      }

      ++this.tickCount;
      Entity entity = this.minecraft.getCameraEntity();
      if (entity != null) {
         this.updateVignetteBrightness(entity);
      }

      if (this.minecraft.player != null) {
         ItemStack itemstack = this.minecraft.player.getInventory().getSelected();
         if (itemstack.isEmpty()) {
            this.toolHighlightTimer = 0;
         } else if (!this.lastToolHighlight.isEmpty() && itemstack.getItem() == this.lastToolHighlight.getItem() && (itemstack.getHoverName().equals(this.lastToolHighlight.getHoverName()) && itemstack.getHighlightTip(itemstack.getHoverName()).equals(lastToolHighlight.getHighlightTip(lastToolHighlight.getHoverName())))) {
            if (this.toolHighlightTimer > 0) {
               --this.toolHighlightTimer;
            }
         } else {
            this.toolHighlightTimer = 40;
         }

         this.lastToolHighlight = itemstack;
      }

   }

   private void tickAutosaveIndicator() {
      MinecraftServer minecraftserver = this.minecraft.getSingleplayerServer();
      boolean flag = minecraftserver != null && minecraftserver.isCurrentlySaving();
      this.lastAutosaveIndicatorValue = this.autosaveIndicatorValue;
      this.autosaveIndicatorValue = Mth.lerp(0.2F, this.autosaveIndicatorValue, flag ? 1.0F : 0.0F);
   }

   public void setNowPlaying(Component pDisplayName) {
      Component component = Component.translatable("record.nowPlaying", pDisplayName);
      this.setOverlayMessage(component, true);
      this.minecraft.getNarrator().sayNow(component);
   }

   public void setOverlayMessage(Component pComponent, boolean pAnimateColor) {
      this.setChatDisabledByPlayerShown(false);
      this.overlayMessageString = pComponent;
      this.overlayMessageTime = 60;
      this.animateOverlayMessageColor = pAnimateColor;
   }

   public void setChatDisabledByPlayerShown(boolean p_238398_) {
      this.chatDisabledByPlayerShown = p_238398_;
   }

   public boolean isShowingChatDisabledByPlayer() {
      return this.chatDisabledByPlayerShown && this.overlayMessageTime > 0;
   }

   public void setTimes(int pTitleFadeInTime, int pTitleStayTime, int pTitleFadeOutTime) {
      if (pTitleFadeInTime >= 0) {
         this.titleFadeInTime = pTitleFadeInTime;
      }

      if (pTitleStayTime >= 0) {
         this.titleStayTime = pTitleStayTime;
      }

      if (pTitleFadeOutTime >= 0) {
         this.titleFadeOutTime = pTitleFadeOutTime;
      }

      if (this.titleTime > 0) {
         this.titleTime = this.titleFadeInTime + this.titleStayTime + this.titleFadeOutTime;
      }

   }

   public void setSubtitle(Component pSubtitle) {
      this.subtitle = pSubtitle;
   }

   public void setTitle(Component pTitle) {
      this.title = pTitle;
      this.titleTime = this.titleFadeInTime + this.titleStayTime + this.titleFadeOutTime;
   }

   public void clear() {
      this.title = null;
      this.subtitle = null;
      this.titleTime = 0;
   }

   /**
    * returns a pointer to the persistant Chat GUI, containing all previous chat messages and such
    */
   public ChatComponent getChat() {
      return this.chat;
   }

   public int getGuiTicks() {
      return this.tickCount;
   }

   public Font getFont() {
      return this.minecraft.font;
   }

   public SpectatorGui getSpectatorGui() {
      return this.spectatorGui;
   }

   public PlayerTabOverlay getTabList() {
      return this.tabList;
   }

   /**
    * Reset the GuiPlayerTabOverlay's message header and footer
    */
   public void onDisconnected() {
      this.tabList.reset();
      this.bossOverlay.reset();
      this.minecraft.getToasts().clear();
      this.minecraft.options.renderDebug = false;
      this.chat.clearMessages(true);
   }

   /**
    * Accessor for the GuiBossOverlay
    */
   public BossHealthOverlay getBossOverlay() {
      return this.bossOverlay;
   }

   public void clearCache() {
      this.debugScreen.clearChunkCache();
   }

   private void renderSavingIndicator(PoseStack p_193835_) {
      if (this.minecraft.options.showAutosaveIndicator().get() && (this.autosaveIndicatorValue > 0.0F || this.lastAutosaveIndicatorValue > 0.0F)) {
         int i = Mth.floor(255.0F * Mth.clamp(Mth.lerp(this.minecraft.getFrameTime(), this.lastAutosaveIndicatorValue, this.autosaveIndicatorValue), 0.0F, 1.0F));
         if (i > 8) {
            Font font = this.getFont();
            int j = font.width(SAVING_TEXT);
            int k = 16777215 | i << 24 & -16777216;
            font.drawShadow(p_193835_, SAVING_TEXT, (float)(this.screenWidth - j - 10), (float)(this.screenHeight - 15), k);
         }
      }

   }

   @OnlyIn(Dist.CLIENT)
   static enum HeartType {
      CONTAINER(0, false),
      NORMAL(2, true),
      POISIONED(4, true),
      WITHERED(6, true),
      ABSORBING(8, false),
      FROZEN(9, false);

      private final int index;
      private final boolean canBlink;

      private HeartType(int pIndex, boolean pCanBlink) {
         this.index = pIndex;
         this.canBlink = pCanBlink;
      }

      public int getX(boolean p_168735_, boolean p_168736_) {
         int i;
         if (this == CONTAINER) {
            i = p_168736_ ? 1 : 0;
         } else {
            int j = p_168735_ ? 1 : 0;
            int k = this.canBlink && p_168736_ ? 2 : 0;
            i = j + k;
         }

         return 16 + (this.index * 2 + i) * 9;
      }

      static Gui.HeartType forPlayer(Player pPlayer) {
         Gui.HeartType gui$hearttype;
         if (pPlayer.hasEffect(MobEffects.POISON)) {
            gui$hearttype = POISIONED;
         } else if (pPlayer.hasEffect(MobEffects.WITHER)) {
            gui$hearttype = WITHERED;
         } else if (pPlayer.isFullyFrozen()) {
            gui$hearttype = FROZEN;
         } else {
            gui$hearttype = NORMAL;
         }

         return gui$hearttype;
      }
   }
}
