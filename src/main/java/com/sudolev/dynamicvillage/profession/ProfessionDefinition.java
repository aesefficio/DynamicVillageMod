package com.sudolev.dynamicvillage.profession;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sudolev.dynamicvillage.condition.LoadCondition;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * A data-defined villager profession, loaded from
 * {@code config/dynamicvillage/professions/<name>.json} (see {@link ProfessionRegistrar}).
 *
 * <p><b>Why config, not a data pack?</b> Villager professions and their POI (job-site) types live in
 * frozen built-in registries that are populated during mod loading — <em>before</em> any world data
 * pack is read. New professions therefore cannot come from a world data pack; they must be
 * registered in the registry phase. The mod reads these definitions from the config directory, which
 * <em>is</em> available that early, so a modpack can ship new professions with only JSON (no Java) by
 * placing files in {@code config/dynamicvillage/professions/}. Their <em>trades</em> and
 * <em>buildings</em> then use the normal data-pack systems, which target any profession id.
 *
 * <p>The profession id doubles as the id of its POI type. For villagers to actually take the job, the
 * POI must be in the {@code minecraft:acquirable_job_site} tag — add a normal data-pack tag entry for
 * {@code id} (see DATAPACK.md).
 *
 * <p><b>Job sites are listed as blocks, not a tag.</b> Block <em>tags</em> come from data packs and
 * are not bound until a world loads — long after professions must be registered — so a tag can never
 * be resolved here. Use {@code job_site_block} (one block) or {@code job_site_blocks} (several).
 * {@code job_site_tag} is rejected with an explanatory error, for the benefit of packs written
 * against that older, non-functional field.
 *
 * @param id             profession id (also used as the POI id), e.g. {@code mypack:logistics_engineer}
 * @param jobSiteBlock   a single block id whose states form the POI
 * @param jobSiteBlocks  several block ids whose states together form the POI
 * @param jobSiteTag     unsupported; present only so it can be reported clearly (see above)
 * @param workSound      sound played while working (defaults to the generic villager work sound)
 * @param searchDistance POI validity/search range (defaults to 1, clamped to at least 1)
 * @param maxTickets     how many villagers may claim one job site (defaults to 1, clamped to at least 1)
 * @param conditions     optional load conditions; the profession is skipped if any is unmet
 */
public record ProfessionDefinition(
   ResourceLocation id,
   Optional<ResourceLocation> jobSiteBlock,
   List<ResourceLocation> jobSiteBlocks,
   Optional<ResourceLocation> jobSiteTag,
   Optional<ResourceLocation> workSound,
   int searchDistance,
   int maxTickets,
   List<LoadCondition> conditions
) {
   public static final Codec<ProfessionDefinition> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(ProfessionDefinition::id),
            ResourceLocation.CODEC.optionalFieldOf("job_site_block").forGetter(ProfessionDefinition::jobSiteBlock),
            ResourceLocation.CODEC.listOf().optionalFieldOf("job_site_blocks", List.of()).forGetter(ProfessionDefinition::jobSiteBlocks),
            ResourceLocation.CODEC.optionalFieldOf("job_site_tag").forGetter(ProfessionDefinition::jobSiteTag),
            ResourceLocation.CODEC.optionalFieldOf("work_sound").forGetter(ProfessionDefinition::workSound),
            Codec.INT.optionalFieldOf("search_distance", 1).forGetter(ProfessionDefinition::searchDistance),
            Codec.INT.optionalFieldOf("max_tickets", 1).forGetter(ProfessionDefinition::maxTickets),
            LoadCondition.CODEC.listOf().optionalFieldOf("conditions", List.of()).forGetter(ProfessionDefinition::conditions)
         )
         .apply(instance, ProfessionDefinition::new)
   );

   /** True when at least one job-site block was given via {@code job_site_block}/{@code job_site_blocks}. */
   public boolean hasValidJobSite() {
      return jobSiteBlock.isPresent() || !jobSiteBlocks.isEmpty();
   }

   /** Every block id this profession's job site is made of, from either field. */
   public List<ResourceLocation> allJobSiteBlocks() {
      if (jobSiteBlock.isEmpty()) {
         return jobSiteBlocks;
      }
      List<ResourceLocation> all = new ArrayList<>(jobSiteBlocks.size() + 1);
      all.add(jobSiteBlock.get());
      all.addAll(jobSiteBlocks);
      return all;
   }

   /** Search range, never below 1 (a 0/negative range would make the job site unusable). */
   public int effectiveSearchDistance() {
      return Math.max(1, searchDistance);
   }

   /** Ticket count, never below 1 (0 would mean no villager could ever claim the job site). */
   public int effectiveMaxTickets() {
      return Math.max(1, maxTickets);
   }
}
