package net.minecraft.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.VibrationParticleOption;
import net.minecraft.util.Mth;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class VibrationSignalParticle extends TextureSheetParticle {
   private final PositionSource target;
   private float yRot;
   private float yRotO;

   VibrationSignalParticle(ClientLevel pLevel, double pX, double pY, double pZ, PositionSource pTarget, int pLifetime) {
      super(pLevel, pX, pY, pZ, 0.0D, 0.0D, 0.0D);
      this.quadSize = 0.3F;
      this.target = pTarget;
      this.lifetime = pLifetime;
   }

   public void render(VertexConsumer pBuffer, Camera pRenderInfo, float pPartialTicks) {
      float f = Mth.sin(((float)this.age + pPartialTicks - ((float)Math.PI * 2F)) * 0.05F) * 2.0F;
      float f1 = Mth.lerp(pPartialTicks, this.yRotO, this.yRot);
      float f2 = 1.0472F;
      this.renderSignal(pBuffer, pRenderInfo, pPartialTicks, (p_172487_) -> {
         p_172487_.mul(Vector3f.YP.rotation(f1));
         p_172487_.mul(Vector3f.XP.rotation(-1.0472F));
         p_172487_.mul(Vector3f.YP.rotation(f));
      });
      this.renderSignal(pBuffer, pRenderInfo, pPartialTicks, (p_172473_) -> {
         p_172473_.mul(Vector3f.YP.rotation(-(float)Math.PI + f1));
         p_172473_.mul(Vector3f.XP.rotation(1.0472F));
         p_172473_.mul(Vector3f.YP.rotation(f));
      });
   }

   private void renderSignal(VertexConsumer pBuffer, Camera pRenderInfo, float pPartialTicks, Consumer<Quaternion> pQuaternionConsumer) {
      Vec3 vec3 = pRenderInfo.getPosition();
      float f = (float)(Mth.lerp((double)pPartialTicks, this.xo, this.x) - vec3.x());
      float f1 = (float)(Mth.lerp((double)pPartialTicks, this.yo, this.y) - vec3.y());
      float f2 = (float)(Mth.lerp((double)pPartialTicks, this.zo, this.z) - vec3.z());
      Vector3f vector3f = new Vector3f(0.5F, 0.5F, 0.5F);
      vector3f.normalize();
      Quaternion quaternion = new Quaternion(vector3f, 0.0F, true);
      pQuaternionConsumer.accept(quaternion);
      Vector3f vector3f1 = new Vector3f(-1.0F, -1.0F, 0.0F);
      vector3f1.transform(quaternion);
      Vector3f[] avector3f = new Vector3f[]{new Vector3f(-1.0F, -1.0F, 0.0F), new Vector3f(-1.0F, 1.0F, 0.0F), new Vector3f(1.0F, 1.0F, 0.0F), new Vector3f(1.0F, -1.0F, 0.0F)};
      float f3 = this.getQuadSize(pPartialTicks);

      for(int i = 0; i < 4; ++i) {
         Vector3f vector3f2 = avector3f[i];
         vector3f2.transform(quaternion);
         vector3f2.mul(f3);
         vector3f2.add(f, f1, f2);
      }

      float f6 = this.getU0();
      float f7 = this.getU1();
      float f4 = this.getV0();
      float f5 = this.getV1();
      int j = this.getLightColor(pPartialTicks);
      pBuffer.vertex((double)avector3f[0].x(), (double)avector3f[0].y(), (double)avector3f[0].z()).uv(f7, f5).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(j).endVertex();
      pBuffer.vertex((double)avector3f[1].x(), (double)avector3f[1].y(), (double)avector3f[1].z()).uv(f7, f4).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(j).endVertex();
      pBuffer.vertex((double)avector3f[2].x(), (double)avector3f[2].y(), (double)avector3f[2].z()).uv(f6, f4).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(j).endVertex();
      pBuffer.vertex((double)avector3f[3].x(), (double)avector3f[3].y(), (double)avector3f[3].z()).uv(f6, f5).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(j).endVertex();
   }

   public int getLightColor(float pPartialTick) {
      return 240;
   }

   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
   }

   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      if (this.age++ >= this.lifetime) {
         this.remove();
      } else {
         Optional<Vec3> optional = this.target.getPosition(this.level);
         if (optional.isEmpty()) {
            this.remove();
         } else {
            int i = this.lifetime - this.age;
            double d0 = 1.0D / (double)i;
            Vec3 vec3 = optional.get();
            this.x = Mth.lerp(d0, this.x, vec3.x());
            this.y = Mth.lerp(d0, this.y, vec3.y());
            this.z = Mth.lerp(d0, this.z, vec3.z());
            this.setPos(this.x, this.y, this.z); // FORGE: update the particle's bounding box
            this.yRotO = this.yRot;
            this.yRot = (float)Mth.atan2(this.x - vec3.x(), this.z - vec3.z());
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class Provider implements ParticleProvider<VibrationParticleOption> {
      private final SpriteSet sprite;

      public Provider(SpriteSet pSprites) {
         this.sprite = pSprites;
      }

      public Particle createParticle(VibrationParticleOption pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
         VibrationSignalParticle vibrationsignalparticle = new VibrationSignalParticle(pLevel, pX, pY, pZ, pType.getDestination(), pType.getArrivalInTicks());
         vibrationsignalparticle.pickSprite(this.sprite);
         vibrationsignalparticle.setAlpha(1.0F);
         return vibrationsignalparticle;
      }
   }
}
