package net.minecraft.world.level.gameevent.vibrations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.VibrationParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.GameEventTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipBlockStateContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class VibrationListener implements GameEventListener {
   protected final PositionSource listenerSource;
   protected final int listenerRange;
   protected final VibrationListener.VibrationListenerConfig config;
   @Nullable
   protected VibrationListener.ReceivingEvent receivingEvent;
   protected float receivingDistance;
   protected int travelTimeInTicks;

   public static Codec<VibrationListener> codec(VibrationListener.VibrationListenerConfig p_223782_) {
      return RecordCodecBuilder.create((p_223785_) -> {
         return p_223785_.group(PositionSource.CODEC.fieldOf("source").forGetter((p_223802_) -> {
            return p_223802_.listenerSource;
         }), ExtraCodecs.NON_NEGATIVE_INT.fieldOf("range").forGetter((p_223800_) -> {
            return p_223800_.listenerRange;
         }), VibrationListener.ReceivingEvent.CODEC.optionalFieldOf("event").forGetter((p_223798_) -> {
            return Optional.ofNullable(p_223798_.receivingEvent);
         }), Codec.floatRange(0.0F, Float.MAX_VALUE).fieldOf("event_distance").orElse(0.0F).forGetter((p_223796_) -> {
            return p_223796_.receivingDistance;
         }), ExtraCodecs.NON_NEGATIVE_INT.fieldOf("event_delay").orElse(0).forGetter((p_223794_) -> {
            return p_223794_.travelTimeInTicks;
         })).apply(p_223785_, (p_223788_, p_223789_, p_223790_, p_223791_, p_223792_) -> {
            return new VibrationListener(p_223788_, p_223789_, p_223782_, p_223790_.orElse((VibrationListener.ReceivingEvent)null), p_223791_, p_223792_);
         });
      });
   }

   public VibrationListener(PositionSource pListenerSource, int pListenerRange, VibrationListener.VibrationListenerConfig pConfig, @Nullable VibrationListener.ReceivingEvent pReceivingEvent, float pReceivingDistance, int pTravelTimeInTicks) {
      this.listenerSource = pListenerSource;
      this.listenerRange = pListenerRange;
      this.config = pConfig;
      this.receivingEvent = pReceivingEvent;
      this.receivingDistance = pReceivingDistance;
      this.travelTimeInTicks = pTravelTimeInTicks;
   }

   public void tick(Level pLevel) {
      if (pLevel instanceof ServerLevel serverlevel) {
         if (this.receivingEvent != null) {
            --this.travelTimeInTicks;
            if (this.travelTimeInTicks <= 0) {
               this.travelTimeInTicks = 0;
               this.config.onSignalReceive(serverlevel, this, new BlockPos(this.receivingEvent.pos), this.receivingEvent.gameEvent, this.receivingEvent.getEntity(serverlevel).orElse((Entity)null), this.receivingEvent.getProjectileOwner(serverlevel).orElse((Entity)null), this.receivingDistance);
               this.receivingEvent = null;
            }
         }
      }

   }

   /**
    * Gets the position of the listener itself.
    */
   public PositionSource getListenerSource() {
      return this.listenerSource;
   }

   /**
    * Gets the listening radius of the listener. Events within this radius will notify the listener when broadcasted.
    */
   public int getListenerRadius() {
      return this.listenerRange;
   }

   public boolean handleGameEvent(ServerLevel pLevel, GameEvent.Message pEventMessage) {
      if (this.receivingEvent != null) {
         return false;
      } else {
         GameEvent gameevent = pEventMessage.gameEvent();
         GameEvent.Context gameevent$context = pEventMessage.context();
         if (!this.config.isValidVibration(gameevent, gameevent$context)) {
            return false;
         } else {
            Optional<Vec3> optional = this.listenerSource.getPosition(pLevel);
            if (optional.isEmpty()) {
               return false;
            } else {
               Vec3 vec3 = pEventMessage.source();
               Vec3 vec31 = optional.get();
               if (!this.config.shouldListen(pLevel, this, new BlockPos(vec3), gameevent, gameevent$context)) {
                  return false;
               } else if (isOccluded(pLevel, vec3, vec31)) {
                  return false;
               } else {
                  this.scheduleSignal(pLevel, gameevent, gameevent$context, vec3, vec31);
                  return true;
               }
            }
         }
      }
   }

   private void scheduleSignal(ServerLevel pLevel, GameEvent pEvent, GameEvent.Context pContext, Vec3 pOrigin, Vec3 pDestination) {
      this.receivingDistance = (float)pOrigin.distanceTo(pDestination);
      this.receivingEvent = new VibrationListener.ReceivingEvent(pEvent, this.receivingDistance, pOrigin, pContext.sourceEntity());
      this.travelTimeInTicks = Mth.floor(this.receivingDistance);
      pLevel.sendParticles(new VibrationParticleOption(this.listenerSource, this.travelTimeInTicks), pOrigin.x, pOrigin.y, pOrigin.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
      this.config.onSignalSchedule();
   }

   private static boolean isOccluded(Level pLevel, Vec3 pFrom, Vec3 pTo) {
      Vec3 vec3 = new Vec3((double)Mth.floor(pFrom.x) + 0.5D, (double)Mth.floor(pFrom.y) + 0.5D, (double)Mth.floor(pFrom.z) + 0.5D);
      Vec3 vec31 = new Vec3((double)Mth.floor(pTo.x) + 0.5D, (double)Mth.floor(pTo.y) + 0.5D, (double)Mth.floor(pTo.z) + 0.5D);

      for(Direction direction : Direction.values()) {
         Vec3 vec32 = vec3.relative(direction, (double)1.0E-5F);
         if (pLevel.isBlockInLine(new ClipBlockStateContext(vec32, vec31, (p_223780_) -> {
            return p_223780_.is(BlockTags.OCCLUDES_VIBRATION_SIGNALS);
         })).getType() != HitResult.Type.BLOCK) {
            return false;
         }
      }

      return true;
   }

   public static record ReceivingEvent(GameEvent gameEvent, float distance, Vec3 pos, @Nullable UUID uuid, @Nullable UUID projectileOwnerUuid, @Nullable Entity entity) {
      public static final Codec<VibrationListener.ReceivingEvent> CODEC = RecordCodecBuilder.create((p_223835_) -> {
         return p_223835_.group(Registry.GAME_EVENT.byNameCodec().fieldOf("game_event").forGetter(VibrationListener.ReceivingEvent::gameEvent), Codec.floatRange(0.0F, Float.MAX_VALUE).fieldOf("distance").forGetter(VibrationListener.ReceivingEvent::distance), Vec3.CODEC.fieldOf("pos").forGetter(VibrationListener.ReceivingEvent::pos), ExtraCodecs.UUID.optionalFieldOf("source").forGetter((p_223850_) -> {
            return Optional.ofNullable(p_223850_.uuid());
         }), ExtraCodecs.UUID.optionalFieldOf("projectile_owner").forGetter((p_223843_) -> {
            return Optional.ofNullable(p_223843_.projectileOwnerUuid());
         })).apply(p_223835_, (p_223837_, p_223838_, p_223839_, p_223840_, p_223841_) -> {
            return new VibrationListener.ReceivingEvent(p_223837_, p_223838_, p_223839_, p_223840_.orElse((UUID)null), p_223841_.orElse((UUID)null));
         });
      });

      public ReceivingEvent(GameEvent pGameEvent, float pDistance, Vec3 pPos, @Nullable UUID pUuid, @Nullable UUID pProjectOwnerUuid) {
         this(pGameEvent, pDistance, pPos, pUuid, pProjectOwnerUuid, (Entity)null);
      }

      public ReceivingEvent(GameEvent pGameEvent, float pDistance, Vec3 pPos, @Nullable Entity pEntity) {
         this(pGameEvent, pDistance, pPos, pEntity == null ? null : pEntity.getUUID(), getProjectileOwner(pEntity), pEntity);
      }

      @Nullable
      private static UUID getProjectileOwner(@Nullable Entity pProjectile) {
         if (pProjectile instanceof Projectile projectile) {
            if (projectile.getOwner() != null) {
               return projectile.getOwner().getUUID();
            }
         }

         return null;
      }

      public Optional<Entity> getEntity(ServerLevel pLevel) {
         return Optional.ofNullable(this.entity).or(() -> {
            return Optional.ofNullable(this.uuid).map(pLevel::getEntity);
         });
      }

      public Optional<Entity> getProjectileOwner(ServerLevel pLevel) {
         return this.getEntity(pLevel).filter((p_223855_) -> {
            return p_223855_ instanceof Projectile;
         }).map((p_223848_) -> {
            return (Projectile)p_223848_;
         }).map(Projectile::getOwner).or(() -> {
            return Optional.ofNullable(this.projectileOwnerUuid).map(pLevel::getEntity);
         });
      }
   }

   public interface VibrationListenerConfig {
      default TagKey<GameEvent> getListenableEvents() {
         return GameEventTags.VIBRATIONS;
      }

      default boolean canTriggerAvoidVibration() {
         return false;
      }

      default boolean isValidVibration(GameEvent pEvent, GameEvent.Context pContext) {
         if (!pEvent.is(this.getListenableEvents())) {
            return false;
         } else {
            Entity entity = pContext.sourceEntity();
            if (entity != null) {
               if (entity.isSpectator()) {
                  return false;
               }

               if (entity.isSteppingCarefully() && pEvent.is(GameEventTags.IGNORE_VIBRATIONS_SNEAKING)) {
                  if (this.canTriggerAvoidVibration() && entity instanceof ServerPlayer) {
                     ServerPlayer serverplayer = (ServerPlayer)entity;
                     CriteriaTriggers.AVOID_VIBRATION.trigger(serverplayer);
                  }

                  return false;
               }

               if (entity.dampensVibrations()) {
                  return false;
               }
            }

            if (pContext.affectedState() != null) {
               return !pContext.affectedState().is(BlockTags.DAMPENS_VIBRATIONS);
            } else {
               return true;
            }
         }
      }

      boolean shouldListen(ServerLevel pLevel, GameEventListener pListener, BlockPos pPos, GameEvent pEvent, GameEvent.Context pContext);

      void onSignalReceive(ServerLevel pLevel, GameEventListener pListener, BlockPos pPos, GameEvent pEvent, @Nullable Entity p_223869_, @Nullable Entity p_223870_, float pDistance);

      default void onSignalSchedule() {
      }
   }
}