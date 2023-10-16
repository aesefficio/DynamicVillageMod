package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Clearable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public class JukeboxBlockEntity extends BlockEntity implements Clearable {
   private ItemStack record = ItemStack.EMPTY;
   private int ticksSinceLastEvent;
   private long tickCount;
   private long recordStartedTick;
   private boolean isPlaying;

   public JukeboxBlockEntity(BlockPos pPos, BlockState pBlockState) {
      super(BlockEntityType.JUKEBOX, pPos, pBlockState);
   }

   public void load(CompoundTag pTag) {
      super.load(pTag);
      if (pTag.contains("RecordItem", 10)) {
         this.setRecord(ItemStack.of(pTag.getCompound("RecordItem")));
      }

      this.isPlaying = pTag.getBoolean("IsPlaying");
      this.recordStartedTick = pTag.getLong("RecordStartTick");
      this.tickCount = pTag.getLong("TickCount");
   }

   protected void saveAdditional(CompoundTag pTag) {
      super.saveAdditional(pTag);
      if (!this.getRecord().isEmpty()) {
         pTag.put("RecordItem", this.getRecord().save(new CompoundTag()));
      }

      pTag.putBoolean("IsPlaying", this.isPlaying);
      pTag.putLong("RecordStartTick", this.recordStartedTick);
      pTag.putLong("TickCount", this.tickCount);
   }

   public ItemStack getRecord() {
      return this.record;
   }

   public void setRecord(ItemStack pRecord) {
      this.record = pRecord;
      this.setChanged();
   }

   public void playRecord() {
      this.recordStartedTick = this.tickCount;
      this.isPlaying = true;
   }

   public void clearContent() {
      this.setRecord(ItemStack.EMPTY);
      this.isPlaying = false;
   }

   public static void playRecordTick(Level p_239938_, BlockPos p_239939_, BlockState p_239940_, JukeboxBlockEntity p_239941_) {
      ++p_239941_.ticksSinceLastEvent;
      if (recordIsPlaying(p_239940_, p_239941_)) {
         Item item = p_239941_.getRecord().getItem();
         if (item instanceof RecordItem) {
            RecordItem recorditem = (RecordItem)item;
            if (recordShouldStopPlaying(p_239941_, recorditem)) {
               p_239938_.gameEvent(GameEvent.JUKEBOX_STOP_PLAY, p_239939_, GameEvent.Context.of(p_239940_));
               p_239941_.isPlaying = false;
            } else if (shouldSendJukeboxPlayingEvent(p_239941_)) {
               p_239941_.ticksSinceLastEvent = 0;
               p_239938_.gameEvent(GameEvent.JUKEBOX_PLAY, p_239939_, GameEvent.Context.of(p_239940_));
            }
         }
      }

      ++p_239941_.tickCount;
   }

   private static boolean recordIsPlaying(BlockState p_240054_, JukeboxBlockEntity p_240055_) {
      return p_240054_.getValue(JukeboxBlock.HAS_RECORD) && p_240055_.isPlaying;
   }

   private static boolean recordShouldStopPlaying(JukeboxBlockEntity p_239767_, RecordItem p_239768_) {
      return p_239767_.tickCount >= p_239767_.recordStartedTick + (long)p_239768_.getLengthInTicks();
   }

   private static boolean shouldSendJukeboxPlayingEvent(JukeboxBlockEntity p_239366_) {
      return p_239366_.ticksSinceLastEvent >= 20;
   }
}