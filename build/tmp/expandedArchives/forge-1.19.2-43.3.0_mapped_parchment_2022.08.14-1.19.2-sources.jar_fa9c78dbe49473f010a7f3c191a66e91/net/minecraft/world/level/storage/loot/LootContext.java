package net.minecraft.world.level.storage.loot;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * LootContext stores various context information for loot generation.
 * This includes the Level as well as any known {@link LootContextParam}s.
 */
public class LootContext {
   private final RandomSource random;
   private final float luck;
   private final ServerLevel level;
   private final Function<ResourceLocation, LootTable> lootTables;
   private final Set<LootTable> visitedTables = Sets.newLinkedHashSet();
   private final Function<ResourceLocation, LootItemCondition> conditions;
   private final Set<LootItemCondition> visitedConditions = Sets.newLinkedHashSet();
   private final Map<LootContextParam<?>, Object> params;
   private final Map<ResourceLocation, LootContext.DynamicDrop> dynamicDrops;

   LootContext(RandomSource pRandom, float pLuck, ServerLevel pLevel, Function<ResourceLocation, LootTable> pLootTables, Function<ResourceLocation, LootItemCondition> pConditions, Map<LootContextParam<?>, Object> pParams, Map<ResourceLocation, LootContext.DynamicDrop> pDynamicDrops) {
      this.random = pRandom;
      this.luck = pLuck;
      this.level = pLevel;
      this.lootTables = pLootTables;
      this.conditions = pConditions;
      this.params = ImmutableMap.copyOf(pParams);
      this.dynamicDrops = ImmutableMap.copyOf(pDynamicDrops);
   }

   /**
    * Check whether the given parameter is present in this context.
    */
   public boolean hasParam(LootContextParam<?> pParameter) {
      return this.params.containsKey(pParameter);
   }

   /**
    * Get the value of the given parameter.
    * 
    * @throws NoSuchElementException if the parameter is not present in this context
    */
   public <T> T getParam(LootContextParam<T> pParam) {
      T t = (T)this.params.get(pParam);
      if (t == null) {
         throw new NoSuchElementException(pParam.getName().toString());
      } else {
         return t;
      }
   }

   /**
    * Add the dynamic drops for the given dynamic drops name to the given consumer.
    * If no dynamic drops provider for the given name has been registered to this LootContext, nothing is generated.
    * 
    * @see DynamicDrops
    */
   public void addDynamicDrops(ResourceLocation pName, Consumer<ItemStack> pConsumer) {
      LootContext.DynamicDrop lootcontext$dynamicdrop = this.dynamicDrops.get(pName);
      if (lootcontext$dynamicdrop != null) {
         lootcontext$dynamicdrop.add(this, pConsumer);
      }

   }

   /**
    * Get the value of the given parameter if it is present in this context, null otherwise.
    */
   @Nullable
   public <T> T getParamOrNull(LootContextParam<T> pParameter) {
      return (T)this.params.get(pParameter);
   }

   public boolean addVisitedTable(LootTable pLootTable) {
      return this.visitedTables.add(pLootTable);
   }

   public void removeVisitedTable(LootTable pLootTable) {
      this.visitedTables.remove(pLootTable);
   }

   public boolean addVisitedCondition(LootItemCondition pCondition) {
      return this.visitedConditions.add(pCondition);
   }

   public void removeVisitedCondition(LootItemCondition pCondition) {
      this.visitedConditions.remove(pCondition);
   }

   public LootTable getLootTable(ResourceLocation pTableId) {
      return this.lootTables.apply(pTableId);
   }

   @Nullable
   public LootItemCondition getCondition(ResourceLocation pConditionId) {
      return this.conditions.apply(pConditionId);
   }

   public RandomSource getRandom() {
      return this.random;
   }

   /**
    * The luck value for this loot context. This is usually just the player's {@linkplain Attributes#LUCK luck value},
    * however it may be modified depending on the context of the looting.
    * When fishing for example it is increased based on the Luck of the Sea enchantment.
    */
   public float getLuck() {
      return this.luck;
   }

   public ServerLevel getLevel() {
      return this.level;
   }

   // ============================== FORGE START ==============================
   public int getLootingModifier() {
      return net.minecraftforge.common.ForgeHooks.getLootingLevel(getParamOrNull(LootContextParams.THIS_ENTITY), getParamOrNull(LootContextParams.KILLER_ENTITY), getParamOrNull(LootContextParams.DAMAGE_SOURCE));
   }

   private ResourceLocation queriedLootTableId;

   private LootContext(RandomSource rand, float luckIn, ServerLevel worldIn, Function<ResourceLocation, LootTable> lootTableManagerIn, Function<ResourceLocation, LootItemCondition> p_i225885_5_, Map<LootContextParam<?>, Object> parametersIn, Map<ResourceLocation, LootContext.DynamicDrop> conditionsIn, ResourceLocation queriedLootTableId) {
      this(rand, luckIn, worldIn, lootTableManagerIn, p_i225885_5_, parametersIn, conditionsIn);
      if (queriedLootTableId != null) this.queriedLootTableId = queriedLootTableId;
   }

   public void setQueriedLootTableId(ResourceLocation queriedLootTableId) {
      if (this.queriedLootTableId == null && queriedLootTableId != null) this.queriedLootTableId = queriedLootTableId;
   }
   public ResourceLocation getQueriedLootTableId() {
      return this.queriedLootTableId == null? net.minecraftforge.common.loot.LootTableIdCondition.UNKNOWN_LOOT_TABLE : this.queriedLootTableId;
   }
   // =============================== FORGE END ===============================

   public static class Builder {
      private final ServerLevel level;
      private final Map<LootContextParam<?>, Object> params = Maps.newIdentityHashMap();
      private final Map<ResourceLocation, LootContext.DynamicDrop> dynamicDrops = Maps.newHashMap();
      private RandomSource random;
      private float luck;
      private ResourceLocation queriedLootTableId; // Forge: correctly pass around loot table ID with copy constructor

      public Builder(ServerLevel pLevel) {
         this.level = pLevel;
      }

      public Builder(LootContext context) {
         this.level = context.level;
         this.params.putAll(context.params);
         this.dynamicDrops.putAll(context.dynamicDrops);
         this.random = context.random;
         this.luck = context.luck;
         this.queriedLootTableId = context.queriedLootTableId;
      }

      public LootContext.Builder withRandom(RandomSource pRandom) {
         this.random = pRandom;
         return this;
      }

      public LootContext.Builder withOptionalRandomSeed(long pSeed) {
         if (pSeed != 0L) {
            this.random = RandomSource.create(pSeed);
         }

         return this;
      }

      public LootContext.Builder withOptionalRandomSeed(long pSeed, RandomSource pRandom) {
         if (pSeed == 0L) {
            this.random = pRandom;
         } else {
            this.random = RandomSource.create(pSeed);
         }

         return this;
      }

      public LootContext.Builder withLuck(float pLuck) {
         this.luck = pLuck;
         return this;
      }

      public <T> LootContext.Builder withParameter(LootContextParam<T> pParameter, T pValue) {
         this.params.put(pParameter, pValue);
         return this;
      }

      public <T> LootContext.Builder withOptionalParameter(LootContextParam<T> pParameter, @Nullable T pValue) {
         if (pValue == null) {
            this.params.remove(pParameter);
         } else {
            this.params.put(pParameter, pValue);
         }

         return this;
      }

      /**
       * Registers a DynamicDrop to the LootContext.
       * 
       * @see LootContext.DynamicDrop
       */
      public LootContext.Builder withDynamicDrop(ResourceLocation pDynamicDropId, LootContext.DynamicDrop pDynamicDrop) {
         LootContext.DynamicDrop lootcontext$dynamicdrop = this.dynamicDrops.put(pDynamicDropId, pDynamicDrop);
         if (lootcontext$dynamicdrop != null) {
            throw new IllegalStateException("Duplicated dynamic drop '" + this.dynamicDrops + "'");
         } else {
            return this;
         }
      }

      public ServerLevel getLevel() {
         return this.level;
      }

      public <T> T getParameter(LootContextParam<T> pParameter) {
         T t = (T)this.params.get(pParameter);
         if (t == null) {
            throw new IllegalArgumentException("No parameter " + pParameter);
         } else {
            return t;
         }
      }

      @Nullable
      public <T> T getOptionalParameter(LootContextParam<T> pParameter) {
         return (T)this.params.get(pParameter);
      }

      public LootContext create(LootContextParamSet pParameterSet) {
         Set<LootContextParam<?>> set = Sets.difference(this.params.keySet(), pParameterSet.getAllowed());
         if (false && !set.isEmpty()) { // Forge: Allow mods to pass custom loot parameters (not part of the vanilla loot table) to the loot context.
            throw new IllegalArgumentException("Parameters not allowed in this parameter set: " + set);
         } else {
            Set<LootContextParam<?>> set1 = Sets.difference(pParameterSet.getRequired(), this.params.keySet());
            if (!set1.isEmpty()) {
               throw new IllegalArgumentException("Missing required parameters: " + set1);
            } else {
               RandomSource randomsource = this.random;
               if (randomsource == null) {
                  randomsource = RandomSource.create();
               }

               MinecraftServer minecraftserver = this.level.getServer();
               return new LootContext(randomsource, this.luck, this.level, minecraftserver.getLootTables()::get, minecraftserver.getPredicateManager()::get, this.params, this.dynamicDrops, this.queriedLootTableId);
            }
         }
      }
   }

   /**
    * DynamicDrop allows a loot generating object (e.g. a Block or Entity) to provide dynamic drops to a loot table.
    * An example of this are shulker boxes, which provide their contents as a dynamic drop source.
    * Dynamic drops are registered with a name using {@link LootContext.Builder#withDynamicDrop}.
    * 
    * These dynamic drops can then be referenced from a loot table using {@link DynamicLoot}.
    */
   @FunctionalInterface
   public interface DynamicDrop {
      void add(LootContext pLootContext, Consumer<ItemStack> pStackConsumer);
   }

   /**
    * Represents a type of entity that can be looked up in a {@link LootContext} using a {@link LootContextParam}.
    */
   public static enum EntityTarget {
      THIS("this", LootContextParams.THIS_ENTITY),
      KILLER("killer", LootContextParams.KILLER_ENTITY),
      DIRECT_KILLER("direct_killer", LootContextParams.DIRECT_KILLER_ENTITY),
      KILLER_PLAYER("killer_player", LootContextParams.LAST_DAMAGE_PLAYER);

      final String name;
      private final LootContextParam<? extends Entity> param;

      private EntityTarget(String pName, LootContextParam<? extends Entity> pParam) {
         this.name = pName;
         this.param = pParam;
      }

      public LootContextParam<? extends Entity> getParam() {
         return this.param;
      }

      // Forge: This method is patched in to expose the same name used in getByName so that ContextNbtProvider#forEntity serializes it properly
      public String getName() {
         return this.name;
      }

      public static LootContext.EntityTarget getByName(String pName) {
         for(LootContext.EntityTarget lootcontext$entitytarget : values()) {
            if (lootcontext$entitytarget.name.equals(pName)) {
               return lootcontext$entitytarget;
            }
         }

         throw new IllegalArgumentException("Invalid entity target " + pName);
      }

      public static class Serializer extends TypeAdapter<LootContext.EntityTarget> {
         public void write(JsonWriter pWriter, LootContext.EntityTarget pEntityTarget) throws IOException {
            pWriter.value(pEntityTarget.name);
         }

         public LootContext.EntityTarget read(JsonReader pReader) throws IOException {
            return LootContext.EntityTarget.getByName(pReader.nextString());
         }
      }
   }
}
