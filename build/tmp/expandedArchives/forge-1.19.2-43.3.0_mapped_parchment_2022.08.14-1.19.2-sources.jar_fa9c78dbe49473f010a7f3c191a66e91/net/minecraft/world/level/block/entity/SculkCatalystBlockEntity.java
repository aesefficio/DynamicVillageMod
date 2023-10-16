package net.minecraft.world.level.block.entity;

import com.google.common.annotations.VisibleForTesting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SculkCatalystBlock;
import net.minecraft.world.level.block.SculkSpreader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.PositionSource;

public class SculkCatalystBlockEntity extends BlockEntity implements GameEventListener {
   private final BlockPositionSource blockPosSource = new BlockPositionSource(this.worldPosition);
   private final SculkSpreader sculkSpreader = SculkSpreader.createLevelSpreader();

   public SculkCatalystBlockEntity(BlockPos p_222774_, BlockState p_222775_) {
      super(BlockEntityType.SCULK_CATALYST, p_222774_, p_222775_);
   }

   public boolean handleEventsImmediately() {
      return true;
   }

   /**
    * Gets the position of the listener itself.
    */
   public PositionSource getListenerSource() {
      return this.blockPosSource;
   }

   /**
    * Gets the listening radius of the listener. Events within this radius will notify the listener when broadcasted.
    */
   public int getListenerRadius() {
      return 8;
   }

   public boolean handleGameEvent(ServerLevel p_222777_, GameEvent.Message p_222778_) {
      if (this.isRemoved()) {
         return false;
      } else {
         GameEvent.Context gameevent$context = p_222778_.context();
         if (p_222778_.gameEvent() == GameEvent.ENTITY_DIE) {
            Entity $$4 = gameevent$context.sourceEntity();
            if ($$4 instanceof LivingEntity) {
               LivingEntity livingentity = (LivingEntity)$$4;
               if (!livingentity.wasExperienceConsumed()) {
                  int i = livingentity.getExperienceReward();
                  if (livingentity.shouldDropExperience() && i > 0) {
                     this.sculkSpreader.addCursors(new BlockPos(p_222778_.source().relative(Direction.UP, 0.5D)), i);
                     LivingEntity livingentity1 = livingentity.getLastHurtByMob();
                     if (livingentity1 instanceof ServerPlayer) {
                        ServerPlayer serverplayer = (ServerPlayer)livingentity1;
                        DamageSource damagesource = livingentity.getLastDamageSource() == null ? DamageSource.playerAttack(serverplayer) : livingentity.getLastDamageSource();
                        CriteriaTriggers.KILL_MOB_NEAR_SCULK_CATALYST.trigger(serverplayer, gameevent$context.sourceEntity(), damagesource);
                     }
                  }

                  livingentity.skipDropExperience();
                  SculkCatalystBlock.bloom(p_222777_, this.worldPosition, this.getBlockState(), p_222777_.getRandom());
               }

               return true;
            }
         }

         return false;
      }
   }

   public static void serverTick(Level p_222780_, BlockPos p_222781_, BlockState p_222782_, SculkCatalystBlockEntity p_222783_) {
      p_222783_.sculkSpreader.updateCursors(p_222780_, p_222781_, p_222780_.getRandom(), true);
   }

   public void load(CompoundTag p_222787_) {
      super.load(p_222787_);
      this.sculkSpreader.load(p_222787_);
   }

   protected void saveAdditional(CompoundTag p_222789_) {
      this.sculkSpreader.save(p_222789_);
      super.saveAdditional(p_222789_);
   }

   @VisibleForTesting
   public SculkSpreader getSculkSpreader() {
      return this.sculkSpreader;
   }
}