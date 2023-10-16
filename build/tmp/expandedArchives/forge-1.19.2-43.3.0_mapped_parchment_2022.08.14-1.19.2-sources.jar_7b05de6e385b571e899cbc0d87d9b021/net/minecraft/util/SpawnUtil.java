package net.minecraft.util;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class SpawnUtil {
   public static <T extends Mob> Optional<T> trySpawnMob(EntityType<T> p_216404_, MobSpawnType p_216405_, ServerLevel p_216406_, BlockPos p_216407_, int p_216408_, int p_216409_, int p_216410_, SpawnUtil.Strategy p_216411_) {
      BlockPos.MutableBlockPos blockpos$mutableblockpos = p_216407_.mutable();

      for(int i = 0; i < p_216408_; ++i) {
         int j = Mth.randomBetweenInclusive(p_216406_.random, -p_216409_, p_216409_);
         int k = Mth.randomBetweenInclusive(p_216406_.random, -p_216409_, p_216409_);
         blockpos$mutableblockpos.setWithOffset(p_216407_, j, p_216410_, k);
         if (p_216406_.getWorldBorder().isWithinBounds(blockpos$mutableblockpos) && moveToPossibleSpawnPosition(p_216406_, p_216410_, blockpos$mutableblockpos, p_216411_)) {
            T t = p_216404_.create(p_216406_, (CompoundTag)null, (Component)null, (Player)null, blockpos$mutableblockpos, p_216405_, false, false);
            if (t != null) {
               int res = net.minecraftforge.common.ForgeHooks.canEntitySpawn(t, p_216406_, t.getX(), t.getY(), t.getZ(), null, p_216405_);
               if (res == 1 || (res == 0 && t.checkSpawnRules(p_216406_, p_216405_) && t.checkSpawnObstruction(p_216406_))) {
                  p_216406_.addFreshEntityWithPassengers(t);
                  return Optional.of(t);
               }

               t.discard();
            }
         }
      }

      return Optional.empty();
   }

   private static boolean moveToPossibleSpawnPosition(ServerLevel p_216399_, int p_216400_, BlockPos.MutableBlockPos p_216401_, SpawnUtil.Strategy p_216402_) {
      BlockPos.MutableBlockPos blockpos$mutableblockpos = (new BlockPos.MutableBlockPos()).set(p_216401_);
      BlockState blockstate = p_216399_.getBlockState(blockpos$mutableblockpos);

      for(int i = p_216400_; i >= -p_216400_; --i) {
         p_216401_.move(Direction.DOWN);
         blockpos$mutableblockpos.setWithOffset(p_216401_, Direction.UP);
         BlockState blockstate1 = p_216399_.getBlockState(p_216401_);
         if (p_216402_.canSpawnOn(p_216399_, p_216401_, blockstate1, blockpos$mutableblockpos, blockstate)) {
            p_216401_.move(Direction.UP);
            return true;
         }

         blockstate = blockstate1;
      }

      return false;
   }

   public interface Strategy {
      SpawnUtil.Strategy LEGACY_IRON_GOLEM = (p_216422_, p_216423_, p_216424_, p_216425_, p_216426_) -> {
         return (p_216426_.isAir() || p_216426_.getMaterial().isLiquid()) && p_216424_.getMaterial().isSolidBlocking();
      };
      SpawnUtil.Strategy ON_TOP_OF_COLLIDER = (p_216416_, p_216417_, p_216418_, p_216419_, p_216420_) -> {
         return p_216420_.getCollisionShape(p_216416_, p_216419_).isEmpty() && Block.isFaceFull(p_216418_.getCollisionShape(p_216416_, p_216417_), Direction.UP);
      };

      boolean canSpawnOn(ServerLevel p_216428_, BlockPos p_216429_, BlockState p_216430_, BlockPos p_216431_, BlockState p_216432_);
   }
}
