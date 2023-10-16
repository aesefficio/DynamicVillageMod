package net.minecraft.world.level.entity;

import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.world.phys.AABB;

public interface LevelEntityGetter<T extends EntityAccess> {
   @Nullable
   T get(int pId);

   @Nullable
   T get(UUID pUuid);

   Iterable<T> getAll();

   <U extends T> void get(EntityTypeTest<T, U> pTest, Consumer<U> pConsumer);

   void get(AABB pBoundingBox, Consumer<T> pConsumer);

   <U extends T> void get(EntityTypeTest<T, U> pTest, AABB pBounds, Consumer<U> pConsumer);
}