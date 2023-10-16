package net.minecraft.client.gui.screens.multiplayer;

import com.google.common.collect.Lists;
import com.google.common.hash.Hashing;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.LoadingDotsText;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.server.LanServer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ServerSelectionList extends ObjectSelectionList<ServerSelectionList.Entry> {
   static final Logger LOGGER = LogUtils.getLogger();
   static final ThreadPoolExecutor THREAD_POOL = new ScheduledThreadPoolExecutor(5, (new ThreadFactoryBuilder()).setNameFormat("Server Pinger #%d").setDaemon(true).setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER)).build());
   static final ResourceLocation ICON_MISSING = new ResourceLocation("textures/misc/unknown_server.png");
   static final ResourceLocation ICON_OVERLAY_LOCATION = new ResourceLocation("textures/gui/server_selection.png");
   static final Component SCANNING_LABEL = Component.translatable("lanServer.scanning");
   static final Component CANT_RESOLVE_TEXT = Component.translatable("multiplayer.status.cannot_resolve").withStyle(ChatFormatting.DARK_RED);
   static final Component CANT_CONNECT_TEXT = Component.translatable("multiplayer.status.cannot_connect").withStyle(ChatFormatting.DARK_RED);
   static final Component INCOMPATIBLE_TOOLTIP = Component.translatable("multiplayer.status.incompatible");
   static final Component NO_CONNECTION_TOOLTIP = Component.translatable("multiplayer.status.no_connection");
   static final Component PINGING_TOOLTIP = Component.translatable("multiplayer.status.pinging");
   private final JoinMultiplayerScreen screen;
   private final List<ServerSelectionList.OnlineServerEntry> onlineServers = Lists.newArrayList();
   private final ServerSelectionList.Entry lanHeader = new ServerSelectionList.LANHeader();
   private final List<ServerSelectionList.NetworkServerEntry> networkServers = Lists.newArrayList();

   public ServerSelectionList(JoinMultiplayerScreen pScreen, Minecraft pMinecraft, int pWidth, int pHeight, int pY0, int pY1, int pItemHeight) {
      super(pMinecraft, pWidth, pHeight, pY0, pY1, pItemHeight);
      this.screen = pScreen;
   }

   private void refreshEntries() {
      this.clearEntries();
      this.onlineServers.forEach((p_169979_) -> {
         this.addEntry(p_169979_);
      });
      this.addEntry(this.lanHeader);
      this.networkServers.forEach((p_169976_) -> {
         this.addEntry(p_169976_);
      });
   }

   public void setSelected(@Nullable ServerSelectionList.Entry pEntry) {
      super.setSelected(pEntry);
      this.screen.onSelectedChange();
   }

   public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
      ServerSelectionList.Entry serverselectionlist$entry = this.getSelected();
      return serverselectionlist$entry != null && serverselectionlist$entry.keyPressed(pKeyCode, pScanCode, pModifiers) || super.keyPressed(pKeyCode, pScanCode, pModifiers);
   }

   protected void moveSelection(AbstractSelectionList.SelectionDirection pOrdering) {
      this.moveSelection(pOrdering, (p_169973_) -> {
         return !(p_169973_ instanceof ServerSelectionList.LANHeader);
      });
   }

   public void updateOnlineServers(ServerList pServers) {
      this.onlineServers.clear();

      for(int i = 0; i < pServers.size(); ++i) {
         this.onlineServers.add(new ServerSelectionList.OnlineServerEntry(this.screen, pServers.get(i)));
      }

      this.refreshEntries();
   }

   public void updateNetworkServers(List<LanServer> pLanServers) {
      this.networkServers.clear();

      for(LanServer lanserver : pLanServers) {
         this.networkServers.add(new ServerSelectionList.NetworkServerEntry(this.screen, lanserver));
      }

      this.refreshEntries();
   }

   protected int getScrollbarPosition() {
      return super.getScrollbarPosition() + 30;
   }

   public int getRowWidth() {
      return super.getRowWidth() + 85;
   }

   protected boolean isFocused() {
      return this.screen.getFocused() == this;
   }

   @OnlyIn(Dist.CLIENT)
   public abstract static class Entry extends ObjectSelectionList.Entry<ServerSelectionList.Entry> {
   }

   @OnlyIn(Dist.CLIENT)
   public static class LANHeader extends ServerSelectionList.Entry {
      private final Minecraft minecraft = Minecraft.getInstance();

      public void render(PoseStack pPoseStack, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pIsMouseOver, float pPartialTick) {
         int i = pTop + pHeight / 2 - 9 / 2;
         this.minecraft.font.draw(pPoseStack, ServerSelectionList.SCANNING_LABEL, (float)(this.minecraft.screen.width / 2 - this.minecraft.font.width(ServerSelectionList.SCANNING_LABEL) / 2), (float)i, 16777215);
         String s = LoadingDotsText.get(Util.getMillis());
         this.minecraft.font.draw(pPoseStack, s, (float)(this.minecraft.screen.width / 2 - this.minecraft.font.width(s) / 2), (float)(i + 9), 8421504);
      }

      public Component getNarration() {
         return CommonComponents.EMPTY;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class NetworkServerEntry extends ServerSelectionList.Entry {
      private static final int ICON_WIDTH = 32;
      private static final Component LAN_SERVER_HEADER = Component.translatable("lanServer.title");
      private static final Component HIDDEN_ADDRESS_TEXT = Component.translatable("selectServer.hiddenAddress");
      private final JoinMultiplayerScreen screen;
      protected final Minecraft minecraft;
      protected final LanServer serverData;
      private long lastClickTime;

      protected NetworkServerEntry(JoinMultiplayerScreen pScreen, LanServer pServerData) {
         this.screen = pScreen;
         this.serverData = pServerData;
         this.minecraft = Minecraft.getInstance();
      }

      public void render(PoseStack pPoseStack, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pIsMouseOver, float pPartialTick) {
         this.minecraft.font.draw(pPoseStack, LAN_SERVER_HEADER, (float)(pLeft + 32 + 3), (float)(pTop + 1), 16777215);
         this.minecraft.font.draw(pPoseStack, this.serverData.getMotd(), (float)(pLeft + 32 + 3), (float)(pTop + 12), 8421504);
         if (this.minecraft.options.hideServerAddress) {
            this.minecraft.font.draw(pPoseStack, HIDDEN_ADDRESS_TEXT, (float)(pLeft + 32 + 3), (float)(pTop + 12 + 11), 3158064);
         } else {
            this.minecraft.font.draw(pPoseStack, this.serverData.getAddress(), (float)(pLeft + 32 + 3), (float)(pTop + 12 + 11), 3158064);
         }

      }

      public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
         this.screen.setSelected(this);
         if (Util.getMillis() - this.lastClickTime < 250L) {
            this.screen.joinSelectedServer();
         }

         this.lastClickTime = Util.getMillis();
         return false;
      }

      public LanServer getServerData() {
         return this.serverData;
      }

      public Component getNarration() {
         return Component.translatable("narrator.select", Component.empty().append(LAN_SERVER_HEADER).append(" ").append(this.serverData.getMotd()));
      }
   }

   @OnlyIn(Dist.CLIENT)
   public class OnlineServerEntry extends ServerSelectionList.Entry {
      private static final int ICON_WIDTH = 32;
      private static final int ICON_HEIGHT = 32;
      private static final int ICON_OVERLAY_X_MOVE_RIGHT = 0;
      private static final int ICON_OVERLAY_X_MOVE_LEFT = 32;
      private static final int ICON_OVERLAY_X_MOVE_DOWN = 64;
      private static final int ICON_OVERLAY_X_MOVE_UP = 96;
      private static final int ICON_OVERLAY_Y_UNSELECTED = 0;
      private static final int ICON_OVERLAY_Y_SELECTED = 32;
      private final JoinMultiplayerScreen screen;
      private final Minecraft minecraft;
      private final ServerData serverData;
      private final ResourceLocation iconLocation;
      @Nullable
      private String lastIconB64;
      @Nullable
      private DynamicTexture icon;
      private long lastClickTime;

      protected OnlineServerEntry(JoinMultiplayerScreen pScreen, ServerData pServerData) {
         this.screen = pScreen;
         this.serverData = pServerData;
         this.minecraft = Minecraft.getInstance();
         this.iconLocation = new ResourceLocation("servers/" + Hashing.sha1().hashUnencodedChars(pServerData.ip) + "/icon");
         AbstractTexture abstracttexture = this.minecraft.getTextureManager().getTexture(this.iconLocation, MissingTextureAtlasSprite.getTexture());
         if (abstracttexture != MissingTextureAtlasSprite.getTexture() && abstracttexture instanceof DynamicTexture) {
            this.icon = (DynamicTexture)abstracttexture;
         }

      }

      public void render(PoseStack pPoseStack, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pIsMouseOver, float pPartialTick) {
         if (!this.serverData.pinged) {
            this.serverData.pinged = true;
            this.serverData.ping = -2L;
            this.serverData.motd = CommonComponents.EMPTY;
            this.serverData.status = CommonComponents.EMPTY;
            ServerSelectionList.THREAD_POOL.submit(() -> {
               try {
                  this.screen.getPinger().pingServer(this.serverData, () -> {
                     this.minecraft.execute(this::updateServerList);
                  });
               } catch (UnknownHostException unknownhostexception) {
                  this.serverData.ping = -1L;
                  this.serverData.motd = ServerSelectionList.CANT_RESOLVE_TEXT;
               } catch (Exception exception) {
                  this.serverData.ping = -1L;
                  this.serverData.motd = ServerSelectionList.CANT_CONNECT_TEXT;
               }

            });
         }

         boolean flag = this.serverData.protocol != SharedConstants.getCurrentVersion().getProtocolVersion();
         this.minecraft.font.draw(pPoseStack, this.serverData.name, (float)(pLeft + 32 + 3), (float)(pTop + 1), 16777215);
         List<FormattedCharSequence> list = this.minecraft.font.split(this.serverData.motd, pWidth - 32 - 2);

         for(int i = 0; i < Math.min(list.size(), 2); ++i) {
            this.minecraft.font.draw(pPoseStack, list.get(i), (float)(pLeft + 32 + 3), (float)(pTop + 12 + 9 * i), 8421504);
         }

         Component component1 = (Component)(flag ? this.serverData.version.copy().withStyle(ChatFormatting.RED) : this.serverData.status);
         int j = this.minecraft.font.width(component1);
         this.minecraft.font.draw(pPoseStack, component1, (float)(pLeft + pWidth - j - 15 - 2), (float)(pTop + 1), 8421504);
         int k = 0;
         int l;
         List<Component> list1;
         Component component;
         if (flag) {
            l = 5;
            component = ServerSelectionList.INCOMPATIBLE_TOOLTIP;
            list1 = this.serverData.playerList;
         } else if (this.serverData.pinged && this.serverData.ping != -2L) {
            if (this.serverData.ping < 0L) {
               l = 5;
            } else if (this.serverData.ping < 150L) {
               l = 0;
            } else if (this.serverData.ping < 300L) {
               l = 1;
            } else if (this.serverData.ping < 600L) {
               l = 2;
            } else if (this.serverData.ping < 1000L) {
               l = 3;
            } else {
               l = 4;
            }

            if (this.serverData.ping < 0L) {
               component = ServerSelectionList.NO_CONNECTION_TOOLTIP;
               list1 = Collections.emptyList();
            } else {
               component = Component.translatable("multiplayer.status.ping", this.serverData.ping);
               list1 = this.serverData.playerList;
            }
         } else {
            k = 1;
            l = (int)(Util.getMillis() / 100L + (long)(pIndex * 2) & 7L);
            if (l > 4) {
               l = 8 - l;
            }

            component = ServerSelectionList.PINGING_TOOLTIP;
            list1 = Collections.emptyList();
         }

         RenderSystem.setShader(GameRenderer::getPositionTexShader);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.setShaderTexture(0, GuiComponent.GUI_ICONS_LOCATION);
         GuiComponent.blit(pPoseStack, pLeft + pWidth - 15, pTop, (float)(k * 10), (float)(176 + l * 8), 10, 8, 256, 256);
         String s = this.serverData.getIconB64();
         if (!Objects.equals(s, this.lastIconB64)) {
            if (this.uploadServerIcon(s)) {
               this.lastIconB64 = s;
            } else {
               this.serverData.setIconB64((String)null);
               this.updateServerList();
            }
         }

         if (this.icon == null) {
            this.drawIcon(pPoseStack, pLeft, pTop, ServerSelectionList.ICON_MISSING);
         } else {
            this.drawIcon(pPoseStack, pLeft, pTop, this.iconLocation);
         }

         int i1 = pMouseX - pLeft;
         int j1 = pMouseY - pTop;
         if (i1 >= pWidth - 15 && i1 <= pWidth - 5 && j1 >= 0 && j1 <= 8) {
            this.screen.setToolTip(Collections.singletonList(component));
         } else if (i1 >= pWidth - j - 15 - 2 && i1 <= pWidth - 15 - 2 && j1 >= 0 && j1 <= 8) {
            this.screen.setToolTip(list1);
         }

         net.minecraftforge.client.ForgeHooksClient.drawForgePingInfo(this.screen, serverData, pPoseStack, pLeft, pTop, pWidth, i1, j1);

         if (this.minecraft.options.touchscreen().get() || pIsMouseOver) {
            RenderSystem.setShaderTexture(0, ServerSelectionList.ICON_OVERLAY_LOCATION);
            GuiComponent.fill(pPoseStack, pLeft, pTop, pLeft + 32, pTop + 32, -1601138544);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            int k1 = pMouseX - pLeft;
            int l1 = pMouseY - pTop;
            if (this.canJoin()) {
               if (k1 < 32 && k1 > 16) {
                  GuiComponent.blit(pPoseStack, pLeft, pTop, 0.0F, 32.0F, 32, 32, 256, 256);
               } else {
                  GuiComponent.blit(pPoseStack, pLeft, pTop, 0.0F, 0.0F, 32, 32, 256, 256);
               }
            }

            if (pIndex > 0) {
               if (k1 < 16 && l1 < 16) {
                  GuiComponent.blit(pPoseStack, pLeft, pTop, 96.0F, 32.0F, 32, 32, 256, 256);
               } else {
                  GuiComponent.blit(pPoseStack, pLeft, pTop, 96.0F, 0.0F, 32, 32, 256, 256);
               }
            }

            if (pIndex < this.screen.getServers().size() - 1) {
               if (k1 < 16 && l1 > 16) {
                  GuiComponent.blit(pPoseStack, pLeft, pTop, 64.0F, 32.0F, 32, 32, 256, 256);
               } else {
                  GuiComponent.blit(pPoseStack, pLeft, pTop, 64.0F, 0.0F, 32, 32, 256, 256);
               }
            }
         }

      }

      public void updateServerList() {
         this.screen.getServers().save();
      }

      protected void drawIcon(PoseStack pPoseStack, int pX, int pY, ResourceLocation pTextureLocation) {
         RenderSystem.setShaderTexture(0, pTextureLocation);
         RenderSystem.enableBlend();
         GuiComponent.blit(pPoseStack, pX, pY, 0.0F, 0.0F, 32, 32, 32, 32);
         RenderSystem.disableBlend();
      }

      private boolean canJoin() {
         return true;
      }

      private boolean uploadServerIcon(@Nullable String pIcon) {
         if (pIcon == null) {
            this.minecraft.getTextureManager().release(this.iconLocation);
            if (this.icon != null && this.icon.getPixels() != null) {
               this.icon.getPixels().close();
            }

            this.icon = null;
         } else {
            try {
               NativeImage nativeimage = NativeImage.fromBase64(pIcon);
               Validate.validState(nativeimage.getWidth() == 64, "Must be 64 pixels wide");
               Validate.validState(nativeimage.getHeight() == 64, "Must be 64 pixels high");
               if (this.icon == null) {
                  this.icon = new DynamicTexture(nativeimage);
               } else {
                  this.icon.setPixels(nativeimage);
                  this.icon.upload();
               }

               this.minecraft.getTextureManager().register(this.iconLocation, this.icon);
            } catch (Throwable throwable) {
               ServerSelectionList.LOGGER.error("Invalid icon for server {} ({})", this.serverData.name, this.serverData.ip, throwable);
               return false;
            }
         }

         return true;
      }

      public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
         if (Screen.hasShiftDown()) {
            ServerSelectionList serverselectionlist = this.screen.serverSelectionList;
            int i = serverselectionlist.children().indexOf(this);
            if (i == -1) {
               return true;
            }

            if (pKeyCode == 264 && i < this.screen.getServers().size() - 1 || pKeyCode == 265 && i > 0) {
               this.swap(i, pKeyCode == 264 ? i + 1 : i - 1);
               return true;
            }
         }

         return super.keyPressed(pKeyCode, pScanCode, pModifiers);
      }

      private void swap(int p_99872_, int p_99873_) {
         this.screen.getServers().swap(p_99872_, p_99873_);
         this.screen.serverSelectionList.updateOnlineServers(this.screen.getServers());
         ServerSelectionList.Entry serverselectionlist$entry = this.screen.serverSelectionList.children().get(p_99873_);
         this.screen.serverSelectionList.setSelected(serverselectionlist$entry);
         ServerSelectionList.this.ensureVisible(serverselectionlist$entry);
      }

      public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
         double d0 = pMouseX - (double)ServerSelectionList.this.getRowLeft();
         double d1 = pMouseY - (double)ServerSelectionList.this.getRowTop(ServerSelectionList.this.children().indexOf(this));
         if (d0 <= 32.0D) {
            if (d0 < 32.0D && d0 > 16.0D && this.canJoin()) {
               this.screen.setSelected(this);
               this.screen.joinSelectedServer();
               return true;
            }

            int i = this.screen.serverSelectionList.children().indexOf(this);
            if (d0 < 16.0D && d1 < 16.0D && i > 0) {
               this.swap(i, i - 1);
               return true;
            }

            if (d0 < 16.0D && d1 > 16.0D && i < this.screen.getServers().size() - 1) {
               this.swap(i, i + 1);
               return true;
            }
         }

         this.screen.setSelected(this);
         if (Util.getMillis() - this.lastClickTime < 250L) {
            this.screen.joinSelectedServer();
         }

         this.lastClickTime = Util.getMillis();
         return false;
      }

      public ServerData getServerData() {
         return this.serverData;
      }

      public Component getNarration() {
         return Component.translatable("narrator.select", this.serverData.name);
      }
   }
}
