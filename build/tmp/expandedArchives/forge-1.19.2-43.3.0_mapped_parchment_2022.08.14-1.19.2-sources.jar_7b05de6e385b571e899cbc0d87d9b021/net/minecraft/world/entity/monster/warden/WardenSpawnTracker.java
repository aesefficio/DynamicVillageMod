package net.minecraft.world.entity.monster.warden;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class WardenSpawnTracker {
   public static final Codec<WardenSpawnTracker> CODEC = RecordCodecBuilder.create((p_219589_) -> {
      return p_219589_.group(ExtraCodecs.NON_NEGATIVE_INT.fieldOf("ticks_since_last_warning").orElse(0).forGetter((p_219607_) -> {
         return p_219607_.ticksSinceLastWarning;
      }), ExtraCodecs.NON_NEGATIVE_INT.fieldOf("warning_level").orElse(0).forGetter((p_219604_) -> {
         return p_219604_.warningLevel;
      }), ExtraCodecs.NON_NEGATIVE_INT.fieldOf("cooldown_ticks").orElse(0).forGetter((p_219601_) -> {
         return p_219601_.cooldownTicks;
      })).apply(p_219589_, WardenSpawnTracker::new);
   });
   public static final int MAX_WARNING_LEVEL = 4;
   private static final double PLAYER_SEARCH_RADIUS = 16.0D;
   private static final int WARNING_CHECK_DIAMETER = 48;
   private static final int DECREASE_WARNING_LEVEL_EVERY_INTERVAL = 12000;
   private static final int WARNING_LEVEL_INCREASE_COOLDOWN = 200;
   private int ticksSinceLastWarning;
   private int warningLevel;
   private int cooldownTicks;

   public WardenSpawnTracker(int p_219568_, int p_219569_, int p_219570_) {
      this.ticksSinceLastWarning = p_219568_;
      this.warningLevel = p_219569_;
      this.cooldownTicks = p_219570_;
   }

   public void tick() {
      if (this.ticksSinceLastWarning >= 12000) {
         this.decreaseWarningLevel();
         this.ticksSinceLastWarning = 0;
      } else {
         ++this.ticksSinceLastWarning;
      }

      if (this.cooldownTicks > 0) {
         --this.cooldownTicks;
      }

   }

   public void reset() {
      this.ticksSinceLastWarning = 0;
      this.warningLevel = 0;
      this.cooldownTicks = 0;
   }

   public static OptionalInt tryWarn(ServerLevel pLevel, BlockPos pPos, ServerPlayer pPlayer) {
      if (hasNearbyWarden(pLevel, pPos)) {
         return OptionalInt.empty();
      } else {
         List<ServerPlayer> list = getNearbyPlayers(pLevel, pPos);
         if (!list.contains(pPlayer)) {
            list.add(pPlayer);
         }

         if (list.stream().anyMatch((p_219582_) -> {
            return p_219582_.getWardenSpawnTracker().onCooldown();
         })) {
            return OptionalInt.empty();
         } else {
            Optional<WardenSpawnTracker> optional = list.stream().map(Player::getWardenSpawnTracker).max(Comparator.comparingInt((p_219598_) -> {
               return p_219598_.warningLevel;
            }));
            WardenSpawnTracker wardenspawntracker = optional.get();
            wardenspawntracker.increaseWarningLevel();
            list.forEach((p_219587_) -> {
               p_219587_.getWardenSpawnTracker().copyData(wardenspawntracker);
            });
            return OptionalInt.of(wardenspawntracker.warningLevel);
         }
      }
   }

   private boolean onCooldown() {
      return this.cooldownTicks > 0;
   }

   private static boolean hasNearbyWarden(ServerLevel pLevel, BlockPos pPos) {
      AABB aabb = AABB.ofSize(Vec3.atCenterOf(pPos), 48.0D, 48.0D, 48.0D);
      return !pLevel.getEntitiesOfClass(Warden.class, aabb).isEmpty();
   }

   private static List<ServerPlayer> getNearbyPlayers(ServerLevel pLevel, BlockPos pPos) {
      Vec3 vec3 = Vec3.atCenterOf(pPos);
      Predicate<ServerPlayer> predicate = (p_219592_) -> {
         return p_219592_.position().closerThan(vec3, 16.0D);
      };
      return pLevel.getPlayers(predicate.and(LivingEntity::isAlive).and(EntitySelector.NO_SPECTATORS));
   }

   private void increaseWarningLevel() {
      if (!this.onCooldown()) {
         this.ticksSinceLastWarning = 0;
         this.cooldownTicks = 200;
         this.setWarningLevel(this.getWarningLevel() + 1);
      }

   }

   private void decreaseWarningLevel() {
      this.setWarningLevel(this.getWarningLevel() - 1);
   }

   public void setWarningLevel(int pWarningLevel) {
      this.warningLevel = Mth.clamp(pWarningLevel, 0, 4);
   }

   public int getWarningLevel() {
      return this.warningLevel;
   }

   private void copyData(WardenSpawnTracker pOther) {
      this.warningLevel = pOther.warningLevel;
      this.cooldownTicks = pOther.cooldownTicks;
      this.ticksSinceLastWarning = pOther.ticksSinceLastWarning;
   }
}