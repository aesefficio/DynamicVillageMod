package net.minecraft.world.level.storage.loot;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * Context for validating loot tables. Loot tables are validated recursively by checking that all functions, conditions,
 * etc. (implementing {@link LootContextUser}) are valid according to their LootTable's {@link LootContextParamSet}.
 */
public class ValidationContext {
   private final Multimap<String, String> problems;
   private final Supplier<String> context;
   private final LootContextParamSet params;
   private final Function<ResourceLocation, LootItemCondition> conditionResolver;
   private final Set<ResourceLocation> visitedConditions;
   private final Function<ResourceLocation, LootTable> tableResolver;
   private final Set<ResourceLocation> visitedTables;
   private String contextCache;

   public ValidationContext(LootContextParamSet pParams, Function<ResourceLocation, LootItemCondition> pConditionResolver, Function<ResourceLocation, LootTable> pTableResolver) {
      this(HashMultimap.create(), () -> {
         return "";
      }, pParams, pConditionResolver, ImmutableSet.of(), pTableResolver, ImmutableSet.of());
   }

   public ValidationContext(Multimap<String, String> pProblems, Supplier<String> pContext, LootContextParamSet pParams, Function<ResourceLocation, LootItemCondition> pConditionResolver, Set<ResourceLocation> pVisitedConditions, Function<ResourceLocation, LootTable> pTableResolver, Set<ResourceLocation> pVisitedTables) {
      this.problems = pProblems;
      this.context = pContext;
      this.params = pParams;
      this.conditionResolver = pConditionResolver;
      this.visitedConditions = pVisitedConditions;
      this.tableResolver = pTableResolver;
      this.visitedTables = pVisitedTables;
   }

   private String getContext() {
      if (this.contextCache == null) {
         this.contextCache = this.context.get();
      }

      return this.contextCache;
   }

   /**
    * Report a problem to this ValidationContext.
    */
   public void reportProblem(String pProblem) {
      this.problems.put(this.getContext(), pProblem);
   }

   /**
    * Create a new ValidationContext with {@code childName} being added to the context.
    */
   public ValidationContext forChild(String pChildName) {
      return new ValidationContext(this.problems, () -> {
         return this.getContext() + pChildName;
      }, this.params, this.conditionResolver, this.visitedConditions, this.tableResolver, this.visitedTables);
   }

   /**
    * Create a new ValidationContext for validating a given LootTable
    */
   public ValidationContext enterTable(String pContextName, ResourceLocation pLootTableId) {
      ImmutableSet<ResourceLocation> immutableset = ImmutableSet.<ResourceLocation>builder().addAll(this.visitedTables).add(pLootTableId).build();
      return new ValidationContext(this.problems, () -> {
         return this.getContext() + pContextName;
      }, this.params, this.conditionResolver, this.visitedConditions, this.tableResolver, immutableset);
   }

   /**
    * Create a new ValidationContext for validating a given LootItemCondition.
    */
   public ValidationContext enterCondition(String pContextName, ResourceLocation pConditionId) {
      ImmutableSet<ResourceLocation> immutableset = ImmutableSet.<ResourceLocation>builder().addAll(this.visitedConditions).add(pConditionId).build();
      return new ValidationContext(this.problems, () -> {
         return this.getContext() + pContextName;
      }, this.params, this.conditionResolver, immutableset, this.tableResolver, this.visitedTables);
   }

   /**
    * Check whether a LootTable with the given ID has been validated with this context.
    */
   public boolean hasVisitedTable(ResourceLocation pLootTableId) {
      return this.visitedTables.contains(pLootTableId);
   }

   /**
    * Check whether a loot condition with the given ID has been validated by this context.
    */
   public boolean hasVisitedCondition(ResourceLocation pConditionId) {
      return this.visitedConditions.contains(pConditionId);
   }

   /**
    * Get all problems that have been recorded. The resulting Multimap maps the {@linkplain #getContext context} at the
    * time the problem occured to the problems.
    */
   public Multimap<String, String> getProblems() {
      return ImmutableMultimap.copyOf(this.problems);
   }

   /**
    * Validate the given LootContextUser.
    */
   public void validateUser(LootContextUser pLootContextUser) {
      this.params.validateUser(this, pLootContextUser);
   }

   @Nullable
   public LootTable resolveLootTable(ResourceLocation pLootTableId) {
      return this.tableResolver.apply(pLootTableId);
   }

   @Nullable
   public LootItemCondition resolveCondition(ResourceLocation pConditionId) {
      return this.conditionResolver.apply(pConditionId);
   }

   /**
    * Create a new ValidationContext with the given LootContextParamSet.
    */
   public ValidationContext setParams(LootContextParamSet pParams) {
      return new ValidationContext(this.problems, this.context, pParams, this.conditionResolver, this.visitedConditions, this.tableResolver, this.visitedTables);
   }
}