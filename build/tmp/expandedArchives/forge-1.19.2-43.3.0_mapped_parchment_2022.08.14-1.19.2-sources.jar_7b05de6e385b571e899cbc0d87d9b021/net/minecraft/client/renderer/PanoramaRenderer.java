package net.minecraft.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PanoramaRenderer {
   private final Minecraft minecraft;
   private final CubeMap cubeMap;
   private float time;

   public PanoramaRenderer(CubeMap pCubeMap) {
      this.cubeMap = pCubeMap;
      this.minecraft = Minecraft.getInstance();
   }

   public void render(float pDeltaT, float pAlpha) {
      this.time += pDeltaT;
      this.cubeMap.render(this.minecraft, Mth.sin(this.time * 0.001F) * 5.0F + 25.0F, -this.time * 0.1F, pAlpha);
   }
}