package net.minecraft.client.gui.screens.multiplayer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DirectJoinServerScreen;
import net.minecraft.client.gui.screens.EditServerScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.server.LanServer;
import net.minecraft.client.server.LanServerDetection;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class JoinMultiplayerScreen extends Screen {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final ServerStatusPinger pinger = new ServerStatusPinger();
   private final Screen lastScreen;
   protected ServerSelectionList serverSelectionList;
   private ServerList servers;
   private Button editButton;
   private Button selectButton;
   private Button deleteButton;
   /** The text to be displayed when the player's cursor hovers over a server listing. */
   @Nullable
   private List<Component> toolTip;
   private ServerData editingServer;
   private LanServerDetection.LanServerList lanServerList;
   @Nullable
   private LanServerDetection.LanServerDetector lanServerDetector;
   private boolean initedOnce;

   public JoinMultiplayerScreen(Screen pLastScreen) {
      super(Component.translatable("multiplayer.title"));
      this.lastScreen = pLastScreen;
   }

   protected void init() {
      super.init();
      this.minecraft.keyboardHandler.setSendRepeatsToGui(true);
      if (this.initedOnce) {
         this.serverSelectionList.updateSize(this.width, this.height, 32, this.height - 64);
      } else {
         this.initedOnce = true;
         this.servers = new ServerList(this.minecraft);
         this.servers.load();
         this.lanServerList = new LanServerDetection.LanServerList();

         try {
            this.lanServerDetector = new LanServerDetection.LanServerDetector(this.lanServerList);
            this.lanServerDetector.start();
         } catch (Exception exception) {
            LOGGER.warn("Unable to start LAN server detection: {}", (Object)exception.getMessage());
         }

         this.serverSelectionList = new ServerSelectionList(this, this.minecraft, this.width, this.height, 32, this.height - 64, 36);
         this.serverSelectionList.updateOnlineServers(this.servers);
      }

      this.addWidget(this.serverSelectionList);
      this.selectButton = this.addRenderableWidget(new Button(this.width / 2 - 154, this.height - 52, 100, 20, Component.translatable("selectServer.select"), (p_99728_) -> {
         this.joinSelectedServer();
      }));
      this.addRenderableWidget(new Button(this.width / 2 - 50, this.height - 52, 100, 20, Component.translatable("selectServer.direct"), (p_99724_) -> {
         this.editingServer = new ServerData(I18n.get("selectServer.defaultName"), "", false);
         this.minecraft.setScreen(new DirectJoinServerScreen(this, this::directJoinCallback, this.editingServer));
      }));
      this.addRenderableWidget(new Button(this.width / 2 + 4 + 50, this.height - 52, 100, 20, Component.translatable("selectServer.add"), (p_99720_) -> {
         this.editingServer = new ServerData(I18n.get("selectServer.defaultName"), "", false);
         this.minecraft.setScreen(new EditServerScreen(this, this::addServerCallback, this.editingServer));
      }));
      this.editButton = this.addRenderableWidget(new Button(this.width / 2 - 154, this.height - 28, 70, 20, Component.translatable("selectServer.edit"), (p_99715_) -> {
         ServerSelectionList.Entry serverselectionlist$entry = this.serverSelectionList.getSelected();
         if (serverselectionlist$entry instanceof ServerSelectionList.OnlineServerEntry) {
            ServerData serverdata = ((ServerSelectionList.OnlineServerEntry)serverselectionlist$entry).getServerData();
            this.editingServer = new ServerData(serverdata.name, serverdata.ip, false);
            this.editingServer.copyFrom(serverdata);
            this.minecraft.setScreen(new EditServerScreen(this, this::editServerCallback, this.editingServer));
         }

      }));
      this.deleteButton = this.addRenderableWidget(new Button(this.width / 2 - 74, this.height - 28, 70, 20, Component.translatable("selectServer.delete"), (p_99710_) -> {
         ServerSelectionList.Entry serverselectionlist$entry = this.serverSelectionList.getSelected();
         if (serverselectionlist$entry instanceof ServerSelectionList.OnlineServerEntry) {
            String s = ((ServerSelectionList.OnlineServerEntry)serverselectionlist$entry).getServerData().name;
            if (s != null) {
               Component component = Component.translatable("selectServer.deleteQuestion");
               Component component1 = Component.translatable("selectServer.deleteWarning", s);
               Component component2 = Component.translatable("selectServer.deleteButton");
               Component component3 = CommonComponents.GUI_CANCEL;
               this.minecraft.setScreen(new ConfirmScreen(this::deleteCallback, component, component1, component2, component3));
            }
         }

      }));
      this.addRenderableWidget(new Button(this.width / 2 + 4, this.height - 28, 70, 20, Component.translatable("selectServer.refresh"), (p_99706_) -> {
         this.refreshServerList();
      }));
      this.addRenderableWidget(new Button(this.width / 2 + 4 + 76, this.height - 28, 75, 20, CommonComponents.GUI_CANCEL, (p_99699_) -> {
         this.minecraft.setScreen(this.lastScreen);
      }));
      this.onSelectedChange();
   }

   public void tick() {
      super.tick();
      if (this.lanServerList.isDirty()) {
         List<LanServer> list = this.lanServerList.getServers();
         this.lanServerList.markClean();
         this.serverSelectionList.updateNetworkServers(list);
      }

      this.pinger.tick();
   }

   public void removed() {
      this.minecraft.keyboardHandler.setSendRepeatsToGui(false);
      if (this.lanServerDetector != null) {
         this.lanServerDetector.interrupt();
         this.lanServerDetector = null;
      }

      this.pinger.removeAll();
   }

   private void refreshServerList() {
      this.minecraft.setScreen(new JoinMultiplayerScreen(this.lastScreen));
   }

   private void deleteCallback(boolean p_99712_) {
      ServerSelectionList.Entry serverselectionlist$entry = this.serverSelectionList.getSelected();
      if (p_99712_ && serverselectionlist$entry instanceof ServerSelectionList.OnlineServerEntry) {
         this.servers.remove(((ServerSelectionList.OnlineServerEntry)serverselectionlist$entry).getServerData());
         this.servers.save();
         this.serverSelectionList.setSelected((ServerSelectionList.Entry)null);
         this.serverSelectionList.updateOnlineServers(this.servers);
      }

      this.minecraft.setScreen(this);
   }

   private void editServerCallback(boolean p_99717_) {
      ServerSelectionList.Entry serverselectionlist$entry = this.serverSelectionList.getSelected();
      if (p_99717_ && serverselectionlist$entry instanceof ServerSelectionList.OnlineServerEntry) {
         ServerData serverdata = ((ServerSelectionList.OnlineServerEntry)serverselectionlist$entry).getServerData();
         serverdata.name = this.editingServer.name;
         serverdata.ip = this.editingServer.ip;
         serverdata.copyFrom(this.editingServer);
         this.servers.save();
         this.serverSelectionList.updateOnlineServers(this.servers);
      }

      this.minecraft.setScreen(this);
   }

   private void addServerCallback(boolean p_99722_) {
      if (p_99722_) {
         ServerData serverdata = this.servers.unhide(this.editingServer.ip);
         if (serverdata != null) {
            serverdata.copyNameIconFrom(this.editingServer);
            this.servers.save();
         } else {
            this.servers.add(this.editingServer, false);
            this.servers.save();
         }

         this.serverSelectionList.setSelected((ServerSelectionList.Entry)null);
         this.serverSelectionList.updateOnlineServers(this.servers);
      }

      this.minecraft.setScreen(this);
   }

   private void directJoinCallback(boolean p_99726_) {
      if (p_99726_) {
         ServerData serverdata = this.servers.get(this.editingServer.ip);
         if (serverdata == null) {
            this.servers.add(this.editingServer, true);
            this.servers.save();
            this.join(this.editingServer);
         } else {
            this.join(serverdata);
         }
      } else {
         this.minecraft.setScreen(this);
      }

   }

   public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
      if (super.keyPressed(pKeyCode, pScanCode, pModifiers)) {
         return true;
      } else if (pKeyCode == 294) {
         this.refreshServerList();
         return true;
      } else if (this.serverSelectionList.getSelected() != null) {
         if (pKeyCode != 257 && pKeyCode != 335) {
            return this.serverSelectionList.keyPressed(pKeyCode, pScanCode, pModifiers);
         } else {
            this.joinSelectedServer();
            return true;
         }
      } else {
         return false;
      }
   }

   public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
      this.toolTip = null;
      this.renderBackground(pPoseStack);
      this.serverSelectionList.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
      drawCenteredString(pPoseStack, this.font, this.title, this.width / 2, 20, 16777215);
      super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
      if (this.toolTip != null) {
         this.renderComponentTooltip(pPoseStack, this.toolTip, pMouseX, pMouseY);
      }

   }

   public void joinSelectedServer() {
      ServerSelectionList.Entry serverselectionlist$entry = this.serverSelectionList.getSelected();
      if (serverselectionlist$entry instanceof ServerSelectionList.OnlineServerEntry) {
         this.join(((ServerSelectionList.OnlineServerEntry)serverselectionlist$entry).getServerData());
      } else if (serverselectionlist$entry instanceof ServerSelectionList.NetworkServerEntry) {
         LanServer lanserver = ((ServerSelectionList.NetworkServerEntry)serverselectionlist$entry).getServerData();
         this.join(new ServerData(lanserver.getMotd(), lanserver.getAddress(), true));
      }

   }

   private void join(ServerData pServer) {
      ConnectScreen.startConnecting(this, this.minecraft, ServerAddress.parseString(pServer.ip), pServer);
   }

   public void setSelected(ServerSelectionList.Entry pSelected) {
      this.serverSelectionList.setSelected(pSelected);
      this.onSelectedChange();
   }

   protected void onSelectedChange() {
      this.selectButton.active = false;
      this.editButton.active = false;
      this.deleteButton.active = false;
      ServerSelectionList.Entry serverselectionlist$entry = this.serverSelectionList.getSelected();
      if (serverselectionlist$entry != null && !(serverselectionlist$entry instanceof ServerSelectionList.LANHeader)) {
         this.selectButton.active = true;
         if (serverselectionlist$entry instanceof ServerSelectionList.OnlineServerEntry) {
            this.editButton.active = true;
            this.deleteButton.active = true;
         }
      }

   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.lastScreen);
   }

   public ServerStatusPinger getPinger() {
      return this.pinger;
   }

   public void setToolTip(List<Component> pToolTip) {
      this.toolTip = pToolTip;
   }

   public ServerList getServers() {
      return this.servers;
   }
}
