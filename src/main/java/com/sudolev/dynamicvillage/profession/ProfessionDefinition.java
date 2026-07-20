package com.sudolev.dynamicvillage.profession;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sudolev.dynamicvillage.condition.LoadCondition;
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
 * @param id           profession id (also used as the POI id), e.g. {@code mypack:logistics_engineer}
 * @param jobSiteBlock a block id whose states form the POI (use this OR {@code jobSiteTag})
 * @param jobSiteTag   a block tag whose blocks all form the POI (use this OR {@code jobSiteBlock})
 * @param workSound    sound played while working (defaults to the generic villager work sound)
 * @param searchDistance POI validity/search range (defaults to 1)
 * @param maxTickets   how many villagers may claim one job site (defaults to 1)
 * @param conditions   optional load conditions; the profession is skipped if any is unmet
 */
public record ProfessionDefinition(
   ResourceLocation id,
   Optional<ResourceLocation> jobSiteBlock,
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
            ResourceLocation.CODEC.optionalFieldOf("job_site_tag").forGetter(ProfessionDefinition::jobSiteTag),
            ResourceLocation.CODEC.optionalFieldOf("work_sound").forGetter(ProfessionDefinition::workSound),
            Codec.INT.optionalFieldOf("search_distance", 1).forGetter(ProfessionDefinition::searchDistance),
            Codec.INT.optionalFieldOf("max_tickets", 1).forGetter(ProfessionDefinition::maxTickets),
            LoadCondition.CODEC.listOf().optionalFieldOf("conditions", List.of()).forGetter(ProfessionDefinition::conditions)
         )
         .apply(instance, ProfessionDefinition::new)
   );

   /** True when exactly one of {@code jobSiteBlock}/{@code jobSiteTag} is set (the only valid form). */
   public boolean hasValidJobSite() {
      return jobSiteBlock.isPresent() ^ jobSiteTag.isPresent();
   }
}
