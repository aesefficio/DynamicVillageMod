package net.minecraft.client.gui.components;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PlayerTabOverlay extends GuiComponent {
   private static final Ordering<PlayerInfo> PLAYER_ORDERING = Ordering.from(new PlayerTabOverlay.PlayerInfoComparator());
   public static final int MAX_ROWS_PER_COL = 20;
   public static final int HEART_EMPTY_CONTAINER = 16;
   public static final int HEART_EMPTY_CONTAINER_BLINKING = 25;
   public static final int HEART_FULL = 52;
   public static final int HEART_HALF_FULL = 61;
   public static final int HEART_GOLDEN_FULL = 160;
   public static final int HEART_GOLDEN_HALF_FULL = 169;
   public static final int HEART_GHOST_FULL = 70;
   public static final int HEART_GHOST_HALF_FULL = 79;
   private final Minecraft minecraft;
   private final Gui gui;
   @Nullable
   private Component footer;
   @Nullable
   private Component header;
   /** The last time the playerlist was opened (went from not being renderd, to being rendered) */
   private long visibilityId;
   /** Weither or not the playerlist is currently being rendered */
   private boolean visible;

   public PlayerTabOverlay(Minecraft pMinecraft, Gui pGui) {
      this.minecraft = pMinecraft;
      this.gui = pGui;
   }

   public Component getNameForDisplay(PlayerInfo pPlayerInfo) {
      return pPlayerInfo.getTabListDisplayName() != null ? this.decorateName(pPlayerInfo, pPlayerInfo.getTabListDisplayName().copy()) : this.decorateName(pPlayerInfo, PlayerTeam.formatNameForTeam(pPlayerInfo.getTeam(), Component.literal(pPlayerInfo.getProfile().getName())));
   }

   private Component decorateName(PlayerInfo pPlayerInfo, MutableComponent pName) {
      return pPlayerInfo.getGameMode() == GameType.SPECTATOR ? pName.withStyle(ChatFormatting.ITALIC) : pName;
   }

   /**
    * Called by GuiIngame to update the information stored in the playerlist, does not actually render the list,
    * however.
    */
   public void setVisible(boolean pVisible) {
      if (pVisible && !this.visible) {
         this.visibilityId = Util.getMillis();
      }

      this.visible = pVisible;
   }

   public void render(PoseStack pPoseStack, int pWidth, Scoreboard pScoreboard, @Nullable Objective pObjective) {
      ClientPacketListener clientpacketlistener = this.minecraft.player.connection;
      List<PlayerInfo> list = PLAYER_ORDERING.sortedCopy(clientpacketlistener.getOnlinePlayers());
      int i = 0;
      int j = 0;

      for(PlayerInfo playerinfo : list) {
         int k = this.minecraft.font.width(this.getNameForDisplay(playerinfo));
         i = Math.max(i, k);
         if (pObjective != null && pObjective.getRenderType() != ObjectiveCriteria.RenderType.HEARTS) {
            k = this.minecraft.font.width(" " + pScoreboard.getOrCreatePlayerScore(playerinfo.getProfile().getName(), pObjective).getScore());
            j = Math.max(j, k);
         }
      }

      list = list.subList(0, Math.min(list.size(), 80));
      int i3 = list.size();
      int j3 = i3;

      int k3;
      for(k3 = 1; j3 > 20; j3 = (i3 + k3 - 1) / k3) {
         ++k3;
      }

      boolean flag = this.minecraft.isLocalServer() || this.minecraft.getConnection().getConnection().isEncrypted();
      int l;
      if (pObjective != null) {
         if (pObjective.getRenderType() == ObjectiveCriteria.RenderType.HEARTS) {
            l = 90;
         } else {
            l = j;
         }
      } else {
         l = 0;
      }

      int i1 = Math.min(k3 * ((flag ? 9 : 0) + i + l + 13), pWidth - 50) / k3;
      int j1 = pWidth / 2 - (i1 * k3 + (k3 - 1) * 5) / 2;
      int k1 = 10;
      int l1 = i1 * k3 + (k3 - 1) * 5;
      List<FormattedCharSequence> list1 = null;
      if (this.header != null) {
         list1 = this.minecraft.font.split(this.header, pWidth - 50);

         for(FormattedCharSequence formattedcharsequence : list1) {
            l1 = Math.max(l1, this.minecraft.font.width(formattedcharsequence));
         }
      }

      List<FormattedCharSequence> list2 = null;
      if (this.footer != null) {
         list2 = this.minecraft.font.split(this.footer, pWidth - 50);

         for(FormattedCharSequence formattedcharsequence1 : list2) {
            l1 = Math.max(l1, this.minecraft.font.width(formattedcharsequence1));
         }
      }

      if (list1 != null) {
         fill(pPoseStack, pWidth / 2 - l1 / 2 - 1, k1 - 1, pWidth / 2 + l1 / 2 + 1, k1 + list1.size() * 9, Integer.MIN_VALUE);

         for(FormattedCharSequence formattedcharsequence2 : list1) {
            int i2 = this.minecraft.font.width(formattedcharsequence2);
            this.minecraft.font.drawShadow(pPoseStack, formattedcharsequence2, (float)(pWidth / 2 - i2 / 2), (float)k1, -1);
            k1 += 9;
         }

         ++k1;
      }

      fill(pPoseStack, pWidth / 2 - l1 / 2 - 1, k1 - 1, pWidth / 2 + l1 / 2 + 1, k1 + j3 * 9, Integer.MIN_VALUE);
      int l3 = this.minecraft.options.getBackgroundColor(553648127);

      for(int i4 = 0; i4 < i3; ++i4) {
         int j4 = i4 / j3;
         int j2 = i4 % j3;
         int k2 = j1 + j4 * i1 + j4 * 5;
         int l2 = k1 + j2 * 9;
         fill(pPoseStack, k2, l2, k2 + i1, l2 + 8, l3);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         if (i4 < list.size()) {
            PlayerInfo playerinfo1 = list.get(i4);
            GameProfile gameprofile = playerinfo1.getProfile();
            if (flag) {
               Player player = this.minecraft.level.getPlayerByUUID(gameprofile.getId());
               boolean flag1 = player != null && LivingEntityRenderer.isEntityUpsideDown(player);
               boolean flag2 = player != null && player.isModelPartShown(PlayerModelPart.HAT);
               RenderSystem.setShaderTexture(0, playerinfo1.getSkinLocation());
               PlayerFaceRenderer.draw(pPoseStack, k2, l2, 8, flag2, flag1);
               k2 += 9;
            }

            this.minecraft.font.drawShadow(pPoseStack, this.getNameForDisplay(playerinfo1), (float)k2, (float)l2, playerinfo1.getGameMode() == GameType.SPECTATOR ? -1862270977 : -1);
            if (pObjective != null && playerinfo1.getGameMode() != GameType.SPECTATOR) {
               int l4 = k2 + i + 1;
               int i5 = l4 + l;
               if (i5 - l4 > 5) {
                  this.renderTablistScore(pObjective, l2, gameprofile.getName(), l4, i5, playerinfo1, pPoseStack);
               }
            }

            this.renderPingIcon(pPoseStack, i1, k2 - (flag ? 9 : 0), l2, playerinfo1);
         }
      }

      if (list2 != null) {
         k1 += j3 * 9 + 1;
         fill(pPoseStack, pWidth / 2 - l1 / 2 - 1, k1 - 1, pWidth / 2 + l1 / 2 + 1, k1 + list2.size() * 9, Integer.MIN_VALUE);

         for(FormattedCharSequence formattedcharsequence3 : list2) {
            int k4 = this.minecraft.font.width(formattedcharsequence3);
            this.minecraft.font.drawShadow(pPoseStack, formattedcharsequence3, (float)(pWidth / 2 - k4 / 2), (float)k1, -1);
            k1 += 9;
         }
      }

   }

   protected void renderPingIcon(PoseStack pPoseStack, int p_94540_, int p_94541_, int pY, PlayerInfo pPlayerInfo) {
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.setShaderTexture(0, GUI_ICONS_LOCATION);
      int i = 0;
      int j;
      if (pPlayerInfo.getLatency() < 0) {
         j = 5;
      } else if (pPlayerInfo.getLatency() < 150) {
         j = 0;
      } else if (pPlayerInfo.getLatency() < 300) {
         j = 1;
      } else if (pPlayerInfo.getLatency() < 600) {
         j = 2;
      } else if (pPlayerInfo.getLatency() < 1000) {
         j = 3;
      } else {
         j = 4;
      }

      this.setBlitOffset(this.getBlitOffset() + 100);
      this.blit(pPoseStack, p_94541_ + p_94540_ - 11, pY, 0, 176 + j * 8, 10, 8);
      this.setBlitOffset(this.getBlitOffset() - 100);
   }

   private void renderTablistScore(Objective pObjective, int pY, String p_94533_, int p_94534_, int p_94535_, PlayerInfo pPlayerInfo, PoseStack pPoseStack) {
      int i = pObjective.getScoreboard().getOrCreatePlayerScore(p_94533_, pObjective).getScore();
      if (pObjective.getRenderType() == ObjectiveCriteria.RenderType.HEARTS) {
         RenderSystem.setShaderTexture(0, GUI_ICONS_LOCATION);
         long j = Util.getMillis();
         if (this.visibilityId == pPlayerInfo.getRenderVisibilityId()) {
            if (i < pPlayerInfo.getLastHealth()) {
               pPlayerInfo.setLastHealthTime(j);
               pPlayerInfo.setHealthBlinkTime((long)(this.gui.getGuiTicks() + 20));
            } else if (i > pPlayerInfo.getLastHealth()) {
               pPlayerInfo.setLastHealthTime(j);
               pPlayerInfo.setHealthBlinkTime((long)(this.gui.getGuiTicks() + 10));
            }
         }

         if (j - pPlayerInfo.getLastHealthTime() > 1000L || this.visibilityId != pPlayerInfo.getRenderVisibilityId()) {
            pPlayerInfo.setLastHealth(i);
            pPlayerInfo.setDisplayHealth(i);
            pPlayerInfo.setLastHealthTime(j);
         }

         pPlayerInfo.setRenderVisibilityId(this.visibilityId);
         pPlayerInfo.setLastHealth(i);
         int k = Mth.ceil((float)Math.max(i, pPlayerInfo.getDisplayHealth()) / 2.0F);
         int l = Math.max(Mth.ceil((float)(i / 2)), Math.max(Mth.ceil((float)(pPlayerInfo.getDisplayHealth() / 2)), 10));
         boolean flag = pPlayerInfo.getHealthBlinkTime() > (long)this.gui.getGuiTicks() && (pPlayerInfo.getHealthBlinkTime() - (long)this.gui.getGuiTicks()) / 3L % 2L == 1L;
         if (k > 0) {
            int i1 = Mth.floor(Math.min((float)(p_94535_ - p_94534_ - 4) / (float)l, 9.0F));
            if (i1 > 3) {
               for(int j1 = k; j1 < l; ++j1) {
                  this.blit(pPoseStack, p_94534_ + j1 * i1, pY, flag ? 25 : 16, 0, 9, 9);
               }

               for(int l1 = 0; l1 < k; ++l1) {
                  this.blit(pPoseStack, p_94534_ + l1 * i1, pY, flag ? 25 : 16, 0, 9, 9);
                  if (flag) {
                     if (l1 * 2 + 1 < pPlayerInfo.getDisplayHealth()) {
                        this.blit(pPoseStack, p_94534_ + l1 * i1, pY, 70, 0, 9, 9);
                     }

                     if (l1 * 2 + 1 == pPlayerInfo.getDisplayHealth()) {
                        this.blit(pPoseStack, p_94534_ + l1 * i1, pY, 79, 0, 9, 9);
                     }
                  }

                  if (l1 * 2 + 1 < i) {
                     this.blit(pPoseStack, p_94534_ + l1 * i1, pY, l1 >= 10 ? 160 : 52, 0, 9, 9);
                  }

                  if (l1 * 2 + 1 == i) {
                     this.blit(pPoseStack, p_94534_ + l1 * i1, pY, l1 >= 10 ? 169 : 61, 0, 9, 9);
                  }
               }
            } else {
               float f = Mth.clamp((float)i / 20.0F, 0.0F, 1.0F);
               int k1 = (int)((1.0F - f) * 255.0F) << 16 | (int)(f * 255.0F) << 8;
               String s = "" + (float)i / 2.0F;
               if (p_94535_ - this.minecraft.font.width(s + "hp") >= p_94534_) {
                  s = s + "hp";
               }

               this.minecraft.font.drawShadow(pPoseStack, s, (float)((p_94535_ + p_94534_) / 2 - this.minecraft.font.width(s) / 2), (float)pY, k1);
            }
         }
      } else {
         String s1 = "" + ChatFormatting.YELLOW + i;
         this.minecraft.font.drawShadow(pPoseStack, s1, (float)(p_94535_ - this.minecraft.font.width(s1)), (float)pY, 16777215);
      }

   }

   public void setFooter(@Nullable Component pFooter) {
      this.footer = pFooter;
   }

   public void setHeader(@Nullable Component pHeader) {
      this.header = pHeader;
   }

   public void reset() {
      this.header = null;
      this.footer = null;
   }

   @OnlyIn(Dist.CLIENT)
   static class PlayerInfoComparator implements Comparator<PlayerInfo> {
      public int compare(PlayerInfo p_94564_, PlayerInfo p_94565_) {
         PlayerTeam playerteam = p_94564_.getTeam();
         PlayerTeam playerteam1 = p_94565_.getTeam();
         return ComparisonChain.start().compareTrueFirst(p_94564_.getGameMode() != GameType.SPECTATOR, p_94565_.getGameMode() != GameType.SPECTATOR).compare(playerteam != null ? playerteam.getName() : "", playerteam1 != null ? playerteam1.getName() : "").compare(p_94564_.getProfile().getName(), p_94565_.getProfile().getName(), String::compareToIgnoreCase).result();
      }
   }
}