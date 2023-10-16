package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DripParticle extends TextureSheetParticle {
   private final Fluid type;
   protected boolean isGlowing;

   DripParticle(ClientLevel pLevel, double pX, double pY, double pZ, Fluid pType) {
      super(pLevel, pX, pY, pZ);
      this.setSize(0.01F, 0.01F);
      this.gravity = 0.06F;
      this.type = pType;
   }

   protected Fluid getType() {
      return this.type;
   }

   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
   }

   public int getLightColor(float pPartialTick) {
      return this.isGlowing ? 240 : super.getLightColor(pPartialTick);
   }

   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      this.preMoveUpdate();
      if (!this.removed) {
         this.yd -= (double)this.gravity;
         this.move(this.xd, this.yd, this.zd);
         this.postMoveUpdate();
         if (!this.removed) {
            this.xd *= (double)0.98F;
            this.yd *= (double)0.98F;
            this.zd *= (double)0.98F;
            BlockPos blockpos = new BlockPos(this.x, this.y, this.z);
            FluidState fluidstate = this.level.getFluidState(blockpos);
            if (fluidstate.getType() == this.type && this.y < (double)((float)blockpos.getY() + fluidstate.getHeight(this.level, blockpos))) {
               this.remove();
            }

         }
      }
   }

   protected void preMoveUpdate() {
      if (this.lifetime-- <= 0) {
         this.remove();
      }

   }

   protected void postMoveUpdate() {
   }

   @OnlyIn(Dist.CLIENT)
   static class CoolingDripHangParticle extends DripParticle.DripHangParticle {
      CoolingDripHangParticle(ClientLevel p_106068_, double p_106069_, double p_106070_, double p_106071_, Fluid p_106072_, ParticleOptions p_106073_) {
         super(p_106068_, p_106069_, p_106070_, p_106071_, p_106072_, p_106073_);
      }

      protected void preMoveUpdate() {
         this.rCol = 1.0F;
         this.gCol = 16.0F / (float)(40 - this.lifetime + 16);
         this.bCol = 4.0F / (float)(40 - this.lifetime + 8);
         super.preMoveUpdate();
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class DripHangParticle extends DripParticle {
      private final ParticleOptions fallingParticle;

      DripHangParticle(ClientLevel pLevel, double pX, double pY, double pZ, Fluid pType, ParticleOptions pFallingParticle) {
         super(pLevel, pX, pY, pZ, pType);
         this.fallingParticle = pFallingParticle;
         this.gravity *= 0.02F;
         this.lifetime = 40;
      }

      protected void preMoveUpdate() {
         if (this.lifetime-- <= 0) {
            this.remove();
            this.level.addParticle(this.fallingParticle, this.x, this.y, this.z, this.xd, this.yd, this.zd);
         }

      }

      protected void postMoveUpdate() {
         this.xd *= 0.02D;
         this.yd *= 0.02D;
         this.zd *= 0.02D;
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class DripLandParticle extends DripParticle {
      DripLandParticle(ClientLevel p_106102_, double p_106103_, double p_106104_, double p_106105_, Fluid p_106106_) {
         super(p_106102_, p_106103_, p_106104_, p_106105_, p_106106_);
         this.lifetime = (int)(16.0D / (Math.random() * 0.8D + 0.2D));
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class DripstoneFallAndLandParticle extends DripParticle.FallAndLandParticle {
      DripstoneFallAndLandParticle(ClientLevel p_171930_, double p_171931_, double p_171932_, double p_171933_, Fluid p_171934_, ParticleOptions p_171935_) {
         super(p_171930_, p_171931_, p_171932_, p_171933_, p_171934_, p_171935_);
      }

      protected void postMoveUpdate() {
         if (this.onGround) {
            this.remove();
            this.level.addParticle(this.landParticle, this.x, this.y, this.z, 0.0D, 0.0D, 0.0D);
            SoundEvent soundevent = this.getType() == Fluids.LAVA ? SoundEvents.POINTED_DRIPSTONE_DRIP_LAVA : SoundEvents.POINTED_DRIPSTONE_DRIP_WATER;
            float f = Mth.randomBetween(this.random, 0.3F, 1.0F);
            this.level.playLocalSound(this.x, this.y, this.z, soundevent, SoundSource.BLOCKS, f, 1.0F, false);
         }

      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class DripstoneLavaFallProvider implements ParticleProvider<SimpleParticleType> {
      protected final SpriteSet sprite;

      public DripstoneLavaFallProvider(SpriteSet pSprites) {
         this.sprite = pSprites;
      }

      public Particle createParticle(SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
         DripParticle dripparticle = new DripParticle.DripstoneFallAndLandParticle(pLevel, pX, pY, pZ, Fluids.LAVA, ParticleTypes.LANDING_LAVA);
         dripparticle.setColor(1.0F, 0.2857143F, 0.083333336F);
         dripparticle.pickSprite(this.sprite);
         return dripparticle;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class DripstoneLavaHangProvider implements ParticleProvider<SimpleParticleType> {
      protected final SpriteSet sprite;

      public DripstoneLavaHangProvider(SpriteSet pSprites) {
         this.sprite = pSprites;
      }

      public Particle createParticle(SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
         DripParticle dripparticle = new DripParticle.CoolingDripHangParticle(pLevel, pX, pY, pZ, Fluids.LAVA, ParticleTypes.FALLING_DRIPSTONE_LAVA);
         dripparticle.pickSprite(this.sprite);
         return dripparticle;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class DripstoneWaterFallProvider implements ParticleProvider<SimpleParticleType> {
      protected final SpriteSet sprite;

      public DripstoneWaterFallProvider(SpriteSet pSprites) {
         this.sprite = pSprites;
      }

      public Particle createParticle(SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
         DripParticle dripparticle = new DripParticle.DripstoneFallAndLandParticle(pLevel, pX, pY, pZ, Fluids.WATER, ParticleTypes.SPLASH);
         dripparticle.setColor(0.2F, 0.3F, 1.0F);
         dripparticle.pickSprite(this.sprite);
         return dripparticle;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class DripstoneWaterHangProvider implements ParticleProvider<SimpleParticleType> {
      protected final SpriteSet sprite;

      public DripstoneWaterHangProvider(SpriteSet pSprites) {
         this.sprite = pSprites;
      }

      public Particle createParticle(SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
         DripParticle dripparticle = new DripParticle.DripHangParticle(pLevel, pX, pY, pZ, Fluids.WATER, ParticleTypes.FALLING_DRIPSTONE_WATER);
         dripparticle.setColor(0.2F, 0.3F, 1.0F);
         dripparticle.pickSprite(this.sprite);
         return dripparticle;
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class FallAndLandParticle extends DripParticle.FallingParticle {
      protected final ParticleOptions landParticle;

      FallAndLandParticle(ClientLevel pLevel, double pX, double pY, double pZ, Fluid pType, ParticleOptions pLandParticle) {
         super(pLevel, pX, pY, pZ, pType);
         this.landParticle = pLandParticle;
      }

      protected void postMoveUpdate() {
         if (this.onGround) {
            this.remove();
            this.level.addParticle(this.landParticle, this.x, this.y, this.z, 0.0D, 0.0D, 0.0D);
         }

      }
   }

   @OnlyIn(Dist.CLIENT)
   static class FallingParticle extends DripParticle {
      FallingParticle(ClientLevel pLevel, double pX, double pY, double pZ, Fluid pType) {
         this(pLevel, pX, pY, pZ, pType, (int)(64.0D / (Math.random() * 0.8D + 0.2D)));
      }

      FallingParticle(ClientLevel pLevel, double pX, double pY, double pZ, Fluid pType, int pLifetime) {
         super(pLevel, pX, pY, pZ, pType);
         this.lifetime = pLifetime;
      }

      protected void postMoveUpdate() {
         if (this.onGround) {
            this.remove();
         }

      }
   }

   @OnlyIn(Dist.CLIENT)
   static class HoneyFallAndLandParticle extends DripParticle.FallAndLandParticle {
      HoneyFallAndLandParticle(ClientLevel p_106146_, double p_106147_, double p_106148_, double p_106149_, Fluid p_106150_, ParticleOptions p_106151_) {
         super(p_106146_, p_106147_, p_106148_, p_106149_, p_106150_, p_106151_);
      }

      protected void postMoveUpdate() {
         if (this.onGround) {
            this.remove();
            this.level.addParticle(this.landParticle, this.x, this.y, this.z, 0.0D, 0.0D, 0.0D);
            float f = Mth.randomBetween(this.random, 0.3F, 1.0F);
            this.level.playLocalSound(this.x, this.y, this.z, SoundEvents.BEEHIVE_DRIP, SoundSource.BLOCKS, f, 1.0F, false);
         }

      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class HoneyFallProvider implements ParticleProvider<SimpleParticleType> {
      protected final SpriteSet sprite;

      public HoneyFallProvider(SpriteSet pSprites) {
         this.sprite = pSprites;
      }

      public Particle createParticle(SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
         DripParticle dripparticle = new DripParticle.HoneyFallAndLandParticle(pLevel, pX, pY, pZ, Fluids.EMPTY, ParticleTypes.LANDING_HONEY);
         dripparticle.gravity = 0.01F;
         dripparticle.setColor(0.582F, 0.448F, 0.082F);
         dripparticle.pickSprite(this.sprite);
         return dripparticle;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class HoneyHangProvider implements ParticleProvider<SimpleParticleType> {
      protected final SpriteSet sprite;

      public HoneyHangProvider(SpriteSet pSprites) {
         this.sprite = pSprites;
      }

      public Particle createParticle(SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
         DripParticle.DripHangParticle dripparticle$driphangparticle = new DripParticle.DripHangParticle(pLevel, pX, pY, pZ, Fluids.EMPTY, ParticleTypes.FALLING_HONEY);
         dripparticle$driphangparticle.gravity *= 0.01F;
         dripparticle$driphangparticle.lifetime = 100;
         dripparticle$driphangparticle.setColor(0.622F, 0.508F, 0.082F);
         dripparticle$driphangparticle.pickSprite(this.sprite);
         return dripparticle$driphangparticle;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class HoneyLandProvider implements ParticleProvider<SimpleParticleType> {
      protected final SpriteSet sprite;

      public HoneyLandProvider(SpriteSet pSprites) {
         this.sprite = pSprites;
      }

      public Particle createParticle(SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
         DripParticle dripparticle = new DripParticle.DripLandParticle(pLevel, pX, pY, pZ, Fluids.EMPTY);
         dripparticle.lifetime = (int)(128.0D / (Math.random() * 0.8D + 0.2D));
         dripparticle.setColor(0.522F, 0.408F, 0.082F);
         dripparticle.pickSprite(this.sprite);
         return dripparticle;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class LavaFallProvider implements ParticleProvider<SimpleParticleType> {
      protected final SpriteSet sprite;

      public LavaFallProvider(SpriteSet pSprites) {
         this.sprite = pSprites;
      }

      public Particle createParticle(SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
         DripParticle dripparticle = new DripParticle.FallAndLandParticle(pLevel, pX, pY, pZ, Fluids.LAVA, ParticleTypes.LANDING_LAVA);
         dripparticle.setColor(1.0F, 0.2857143F, 0.083333336F);
         dripparticle.pickSprite(this.sprite);
         return dripparticle;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class LavaHangProvider implements ParticleProvider<SimpleParticleType> {
      protected final SpriteSet sprite;

      public LavaHangProvider(SpriteSet pSprites) {
         this.sprite = pSprites;
      }

      public Particle createParticle(SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
         DripParticle.CoolingDripHangParticle dripparticle$coolingdriphangparticle = new DripParticle.CoolingDripHangParticle(pLevel, pX, pY, pZ, Fluids.LAVA, ParticleTypes.FALLING_LAVA);
         dripparticle$coolingdriphangparticle.pickSprite(this.sprite);
         return dripparticle$coolingdriphangparticle;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class LavaLandProvider implements ParticleProvider<SimpleParticleType> {
      protected final SpriteSet sprite;

      public LavaLandProvider(SpriteSet pSprites) {
         this.sprite = pSprites;
      }

      public Particle createParticle(SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
         DripParticle dripparticle = new DripParticle.DripLandParticle(pLevel, pX, pY, pZ, Fluids.LAVA);
         dripparticle.setColor(1.0F, 0.2857143F, 0.083333336F);
         dripparticle.pickSprite(this.sprite);
         return dripparticle;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class NectarFallProvider implements ParticleProvider<SimpleParticleType> {
      protected final SpriteSet sprite;

      public NectarFallProvider(SpriteSet pSprites) {
         this.sprite = pSprites;
      }

      public Particle createParticle(SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
         DripParticle dripparticle = new DripParticle.FallingParticle(pLevel, pX, pY, pZ, Fluids.EMPTY);
         dripparticle.lifetime = (int)(16.0D / (Math.random() * 0.8D + 0.2D));
         dripparticle.gravity = 0.007F;
         dripparticle.setColor(0.92F, 0.782F, 0.72F);
         dripparticle.pickSprite(this.sprite);
         return dripparticle;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class ObsidianTearFallProvider implements ParticleProvider<SimpleParticleType> {
      protected final SpriteSet sprite;

      public ObsidianTearFallProvider(SpriteSet pSprites) {
         this.sprite = pSprites;
      }

      public Particle createParticle(SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
         DripParticle dripparticle = new DripParticle.FallAndLandParticle(pLevel, pX, pY, pZ, Fluids.EMPTY, ParticleTypes.LANDING_OBSIDIAN_TEAR);
         dripparticle.isGlowing = true;
         dripparticle.gravity = 0.01F;
         dripparticle.setColor(0.51171875F, 0.03125F, 0.890625F);
         dripparticle.pickSprite(this.sprite);
         return dripparticle;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class ObsidianTearHangProvider implements ParticleProvider<SimpleParticleType> {
      protected final SpriteSet sprite;

      public ObsidianTearHangProvider(SpriteSet pSprites) {
         this.sprite = pSprites;
      }

      public Particle createParticle(SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
         DripParticle.DripHangParticle dripparticle$driphangparticle = new DripParticle.DripHangParticle(pLevel, pX, pY, pZ, Fluids.EMPTY, ParticleTypes.FALLING_OBSIDIAN_TEAR);
         dripparticle$driphangparticle.isGlowing = true;
         dripparticle$driphangparticle.gravity *= 0.01F;
         dripparticle$driphangparticle.lifetime = 100;
         dripparticle$driphangparticle.setColor(0.51171875F, 0.03125F, 0.890625F);
         dripparticle$driphangparticle.pickSprite(this.sprite);
         return dripparticle$driphangparticle;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class ObsidianTearLandProvider implements ParticleProvider<SimpleParticleType> {
      protected final SpriteSet sprite;

      public ObsidianTearLandProvider(SpriteSet pSprites) {
         this.sprite = pSprites;
      }

      public Particle createParticle(SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
         DripParticle dripparticle = new DripParticle.DripLandParticle(pLevel, pX, pY, pZ, Fluids.EMPTY);
         dripparticle.isGlowing = true;
         dripparticle.lifetime = (int)(28.0D / (Math.random() * 0.8D + 0.2D));
         dripparticle.setColor(0.51171875F, 0.03125F, 0.890625F);
         dripparticle.pickSprite(this.sprite);
         return dripparticle;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class SporeBlossomFallProvider implements ParticleProvider<SimpleParticleType> {
      protected final SpriteSet sprite;
      private final RandomSource random;

      public SporeBlossomFallProvider(SpriteSet pSprites) {
         this.sprite = pSprites;
         this.random = RandomSource.create();
      }

      public Particle createParticle(SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
         int i = (int)(64.0F / Mth.randomBetween(this.random, 0.1F, 0.9F));
         DripParticle dripparticle = new DripParticle.FallingParticle(pLevel, pX, pY, pZ, Fluids.EMPTY, i);
         dripparticle.gravity = 0.005F;
         dripparticle.setColor(0.32F, 0.5F, 0.22F);
         dripparticle.pickSprite(this.sprite);
         return dripparticle;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class WaterFallProvider implements ParticleProvider<SimpleParticleType> {
      protected final SpriteSet sprite;

      public WaterFallProvider(SpriteSet pSprites) {
         this.sprite = pSprites;
      }

      public Particle createParticle(SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
         DripParticle dripparticle = new DripParticle.FallAndLandParticle(pLevel, pX, pY, pZ, Fluids.WATER, ParticleTypes.SPLASH);
         dripparticle.setColor(0.2F, 0.3F, 1.0F);
         dripparticle.pickSprite(this.sprite);
         return dripparticle;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class WaterHangProvider implements ParticleProvider<SimpleParticleType> {
      protected final SpriteSet sprite;

      public WaterHangProvider(SpriteSet pSprites) {
         this.sprite = pSprites;
      }

      public Particle createParticle(SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
         DripParticle dripparticle = new DripParticle.DripHangParticle(pLevel, pX, pY, pZ, Fluids.WATER, ParticleTypes.FALLING_WATER);
         dripparticle.setColor(0.2F, 0.3F, 1.0F);
         dripparticle.pickSprite(this.sprite);
         return dripparticle;
      }
   }
}