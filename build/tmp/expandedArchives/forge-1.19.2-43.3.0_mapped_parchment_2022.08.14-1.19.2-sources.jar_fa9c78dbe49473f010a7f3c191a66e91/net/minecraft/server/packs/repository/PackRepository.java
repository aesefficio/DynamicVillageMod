package net.minecraft.server.packs.repository;

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;

public class PackRepository {
   private final Set<RepositorySource> sources;
   private Map<String, Pack> available = ImmutableMap.of();
   private List<Pack> selected = ImmutableList.of();
   private final Pack.PackConstructor constructor;

   public PackRepository(Pack.PackConstructor pConstructor, RepositorySource... pSources) {
      this.constructor = pConstructor;
      this.sources = new java.util.LinkedHashSet<>(List.of(pSources)); //Forge: This needs to be a mutable set, so that we can add to it later on.
   }

   public PackRepository(PackType pType, RepositorySource... pSources) {
      this((p_143894_, p_143895_, p_143896_, p_143897_, p_143898_, p_143899_, p_143900_, hidden) -> {
         return new Pack(p_143894_, p_143895_, p_143896_, p_143897_, p_143898_, pType, p_143899_, p_143900_, hidden);
      }, pSources);
      net.minecraftforge.fml.ModLoader.get().postEvent(new net.minecraftforge.event.AddPackFindersEvent(pType, sources::add));
   }

   public void reload() {
      List<String> list = this.selected.stream().map(Pack::getId).collect(ImmutableList.toImmutableList());
      this.available = this.discoverAvailable();
      this.selected = this.rebuildSelected(list);
   }

   private Map<String, Pack> discoverAvailable() {
      Map<String, Pack> map = Maps.newTreeMap();

      for(RepositorySource repositorysource : this.sources) {
         repositorysource.loadPacks((p_143903_) -> {
            map.put(p_143903_.getId(), p_143903_);
         }, this.constructor);
      }

      return ImmutableMap.copyOf(map);
   }

   public void setSelected(Collection<String> pIds) {
      this.selected = this.rebuildSelected(pIds);
   }

   private List<Pack> rebuildSelected(Collection<String> pIds) {
      List<Pack> list = this.getAvailablePacks(pIds).collect(Collectors.toList());

      for(Pack pack : this.available.values()) {
         if (pack.isRequired() && !list.contains(pack)) {
            pack.getDefaultPosition().insert(list, pack, Functions.identity(), false);
         }
      }

      return ImmutableList.copyOf(list);
   }

   private Stream<Pack> getAvailablePacks(Collection<String> pIds) {
      return pIds.stream().map(this.available::get).filter(Objects::nonNull);
   }

   public Collection<String> getAvailableIds() {
      return this.available.keySet();
   }

   /**
    * Gets all known packs, including those that are not enabled.
    */
   public Collection<Pack> getAvailablePacks() {
      return this.available.values();
   }

   public Collection<String> getSelectedIds() {
      return this.selected.stream().map(Pack::getId).collect(ImmutableSet.toImmutableSet());
   }

   /**
    * Gets all packs that have been enabled.
    */
   public Collection<Pack> getSelectedPacks() {
      return this.selected;
   }

   @Nullable
   public Pack getPack(String pId) {
      return this.available.get(pId);
   }

   public synchronized void addPackFinder(RepositorySource packFinder) {
      this.sources.add(packFinder);
   }

   public boolean isAvailable(String pId) {
      return this.available.containsKey(pId);
   }

   public List<PackResources> openAllSelected() {
      return this.selected.stream().map(Pack::open).collect(ImmutableList.toImmutableList());
   }
}
