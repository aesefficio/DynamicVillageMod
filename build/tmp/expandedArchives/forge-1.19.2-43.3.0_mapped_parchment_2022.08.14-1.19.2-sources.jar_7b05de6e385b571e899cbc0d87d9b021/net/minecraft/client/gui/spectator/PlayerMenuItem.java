package net.minecraft.client.gui.spectator;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundTeleportToEntityPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PlayerMenuItem implements SpectatorMenuItem {
   private final GameProfile profile;
   private final ResourceLocation location;
   private final Component name;

   public PlayerMenuItem(GameProfile pProfile) {
      this.profile = pProfile;
      Minecraft minecraft = Minecraft.getInstance();
      this.location = minecraft.getSkinManager().getInsecureSkinLocation(pProfile);
      this.name = Component.literal(pProfile.getName());
   }

   public void selectItem(SpectatorMenu pMenu) {
      Minecraft.getInstance().getConnection().send(new ServerboundTeleportToEntityPacket(this.profile.getId()));
   }

   public Component getName() {
      return this.name;
   }

   public void renderIcon(PoseStack pPoseStack, float pShadeColor, int pAlpha) {
      RenderSystem.setShaderTexture(0, this.location);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, (float)pAlpha / 255.0F);
      PlayerFaceRenderer.draw(pPoseStack, 2, 2, 12);
   }

   public boolean isEnabled() {
      return true;
   }
}