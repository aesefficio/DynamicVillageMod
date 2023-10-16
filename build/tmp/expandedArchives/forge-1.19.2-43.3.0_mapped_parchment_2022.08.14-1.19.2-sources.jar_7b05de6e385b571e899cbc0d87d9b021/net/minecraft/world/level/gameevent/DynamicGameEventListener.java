package net.minecraft.world.level.gameevent;

import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;

public class DynamicGameEventListener<T extends GameEventListener> {
   private T listener;
   @Nullable
   private SectionPos lastSection;

   public DynamicGameEventListener(T p_223615_) {
      this.listener = p_223615_;
   }

   public void add(ServerLevel p_223618_) {
      this.move(p_223618_);
   }

   public void updateListener(T p_223629_, @Nullable Level p_223630_) {
      T t = this.listener;
      if (t != p_223629_) {
         if (p_223630_ instanceof ServerLevel) {
            ServerLevel serverlevel = (ServerLevel)p_223630_;
            ifChunkExists(serverlevel, this.lastSection, (p_223640_) -> {
               p_223640_.unregister(t);
            });
            ifChunkExists(serverlevel, this.lastSection, (p_223633_) -> {
               p_223633_.register(p_223629_);
            });
         }

         this.listener = p_223629_;
      }
   }

   public T getListener() {
      return this.listener;
   }

   public void remove(ServerLevel p_223635_) {
      ifChunkExists(p_223635_, this.lastSection, (p_223644_) -> {
         p_223644_.unregister(this.listener);
      });
   }

   public void move(ServerLevel p_223642_) {
      this.listener.getListenerSource().getPosition(p_223642_).map(SectionPos::of).ifPresent((p_223621_) -> {
         if (this.lastSection == null || !this.lastSection.equals(p_223621_)) {
            ifChunkExists(p_223642_, this.lastSection, (p_223637_) -> {
               p_223637_.unregister(this.listener);
            });
            this.lastSection = p_223621_;
            ifChunkExists(p_223642_, this.lastSection, (p_223627_) -> {
               p_223627_.register(this.listener);
            });
         }

      });
   }

   private static void ifChunkExists(LevelReader p_223623_, @Nullable SectionPos p_223624_, Consumer<GameEventDispatcher> p_223625_) {
      if (p_223624_ != null) {
         ChunkAccess chunkaccess = p_223623_.getChunk(p_223624_.x(), p_223624_.z(), ChunkStatus.FULL, false);
         if (chunkaccess != null) {
            p_223625_.accept(chunkaccess.getEventDispatcher(p_223624_.y()));
         }

      }
   }
}