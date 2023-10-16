package net.minecraft.world.level.block.entity;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.SculkSensorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.vibrations.VibrationListener;
import org.slf4j.Logger;

public class SculkSensorBlockEntity extends BlockEntity implements VibrationListener.VibrationListenerConfig {
   private static final Logger LOGGER = LogUtils.getLogger();
   private VibrationListener listener;
   private int lastVibrationFrequency;

   public SculkSensorBlockEntity(BlockPos pPos, BlockState pBlockState) {
      super(BlockEntityType.SCULK_SENSOR, pPos, pBlockState);
      this.listener = new VibrationListener(new BlockPositionSource(this.worldPosition), ((SculkSensorBlock)pBlockState.getBlock()).getListenerRange(), this, (VibrationListener.ReceivingEvent)null, 0.0F, 0);
   }

   public void load(CompoundTag pTag) {
      super.load(pTag);
      this.lastVibrationFrequency = pTag.getInt("last_vibration_frequency");
      if (pTag.contains("listener", 10)) {
         VibrationListener.codec(this).parse(new Dynamic<>(NbtOps.INSTANCE, pTag.getCompound("listener"))).resultOrPartial(LOGGER::error).ifPresent((p_222817_) -> {
            this.listener = p_222817_;
         });
      }

   }

   protected void saveAdditional(CompoundTag pTag) {
      super.saveAdditional(pTag);
      pTag.putInt("last_vibration_frequency", this.lastVibrationFrequency);
      VibrationListener.codec(this).encodeStart(NbtOps.INSTANCE, this.listener).resultOrPartial(LOGGER::error).ifPresent((p_222820_) -> {
         pTag.put("listener", p_222820_);
      });
   }

   public VibrationListener getListener() {
      return this.listener;
   }

   public int getLastVibrationFrequency() {
      return this.lastVibrationFrequency;
   }

   public boolean canTriggerAvoidVibration() {
      return true;
   }

   public boolean shouldListen(ServerLevel pLevel, GameEventListener pListener, BlockPos pPos, GameEvent pEvent, @Nullable GameEvent.Context pContext) {
      return !this.isRemoved() && (!pPos.equals(this.getBlockPos()) || pEvent != GameEvent.BLOCK_DESTROY && pEvent != GameEvent.BLOCK_PLACE) ? SculkSensorBlock.canActivate(this.getBlockState()) : false;
   }

   public void onSignalReceive(ServerLevel pLevel, GameEventListener pListener, BlockPos pPos, GameEvent pEvent, @Nullable Entity p_222807_, @Nullable Entity p_222808_, float pDistance) {
      BlockState blockstate = this.getBlockState();
      if (SculkSensorBlock.canActivate(blockstate)) {
         this.lastVibrationFrequency = SculkSensorBlock.VIBRATION_FREQUENCY_FOR_EVENT.getInt(pEvent);
         SculkSensorBlock.activate(p_222807_, pLevel, this.worldPosition, blockstate, getRedstoneStrengthForDistance(pDistance, pListener.getListenerRadius()));
      }

   }

   public void onSignalSchedule() {
      this.setChanged();
   }

   public static int getRedstoneStrengthForDistance(float pDistance, int pRadius) {
      double d0 = (double)pDistance / (double)pRadius;
      return Math.max(1, 15 - Mth.floor(d0 * 15.0D));
   }

   public void setLastVibrationFrequency(int pLastVibrationFrequency) {
      this.lastVibrationFrequency = pLastVibrationFrequency;
   }
}