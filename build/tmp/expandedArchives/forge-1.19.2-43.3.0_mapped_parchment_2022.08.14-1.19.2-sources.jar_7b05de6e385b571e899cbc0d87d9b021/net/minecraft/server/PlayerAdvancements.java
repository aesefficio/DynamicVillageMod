package net.minecraft.server;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.internal.Streams;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSelectAdvancementsTabPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.GameRules;
import org.slf4j.Logger;

public class PlayerAdvancements {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int VISIBILITY_DEPTH = 2;
   private static final Gson GSON = (new GsonBuilder()).registerTypeAdapter(AdvancementProgress.class, new AdvancementProgress.Serializer()).registerTypeAdapter(ResourceLocation.class, new ResourceLocation.Serializer()).setPrettyPrinting().create();
   private static final TypeToken<Map<ResourceLocation, AdvancementProgress>> TYPE_TOKEN = new TypeToken<Map<ResourceLocation, AdvancementProgress>>() {
   };
   private final DataFixer dataFixer;
   private final PlayerList playerList;
   private final File file;
   private final Map<Advancement, AdvancementProgress> advancements = Maps.newLinkedHashMap();
   private final Set<Advancement> visible = Sets.newLinkedHashSet();
   private final Set<Advancement> visibilityChanged = Sets.newLinkedHashSet();
   private final Set<Advancement> progressChanged = Sets.newLinkedHashSet();
   private ServerPlayer player;
   @Nullable
   private Advancement lastSelectedTab;
   private boolean isFirstPacket = true;

   public PlayerAdvancements(DataFixer p_135973_, PlayerList p_135974_, ServerAdvancementManager p_135975_, File p_135976_, ServerPlayer p_135977_) {
      this.dataFixer = p_135973_;
      this.playerList = p_135974_;
      this.file = p_135976_;
      this.player = p_135977_;
      this.load(p_135975_);
   }

   public void setPlayer(ServerPlayer pPlayer) {
      this.player = pPlayer;
   }

   public void stopListening() {
      for(CriterionTrigger<?> criteriontrigger : CriteriaTriggers.all()) {
         criteriontrigger.removePlayerListeners(this);
      }

   }

   public void reload(ServerAdvancementManager pManager) {
      this.stopListening();
      this.advancements.clear();
      this.visible.clear();
      this.visibilityChanged.clear();
      this.progressChanged.clear();
      this.isFirstPacket = true;
      this.lastSelectedTab = null;
      this.load(pManager);
   }

   private void registerListeners(ServerAdvancementManager pManager) {
      for(Advancement advancement : pManager.getAllAdvancements()) {
         this.registerListeners(advancement);
      }

   }

   private void ensureAllVisible() {
      List<Advancement> list = Lists.newArrayList();

      for(Map.Entry<Advancement, AdvancementProgress> entry : this.advancements.entrySet()) {
         if (entry.getValue().isDone()) {
            list.add(entry.getKey());
            this.progressChanged.add(entry.getKey());
         }
      }

      for(Advancement advancement : list) {
         this.ensureVisibility(advancement);
      }

   }

   private void checkForAutomaticTriggers(ServerAdvancementManager pManager) {
      for(Advancement advancement : pManager.getAllAdvancements()) {
         if (advancement.getCriteria().isEmpty()) {
            this.award(advancement, "");
            advancement.getRewards().grant(this.player);
         }
      }

   }

   private void load(ServerAdvancementManager pManager) {
      if (this.file.isFile()) {
         try {
            JsonReader jsonreader = new JsonReader(new StringReader(Files.toString(this.file, StandardCharsets.UTF_8)));

            try {
               jsonreader.setLenient(false);
               Dynamic<JsonElement> dynamic = new Dynamic<>(JsonOps.INSTANCE, Streams.parse(jsonreader));
               if (!dynamic.get("DataVersion").asNumber().result().isPresent()) {
                  dynamic = dynamic.set("DataVersion", dynamic.createInt(1343));
               }

               dynamic = this.dataFixer.update(DataFixTypes.ADVANCEMENTS.getType(), dynamic, dynamic.get("DataVersion").asInt(0), SharedConstants.getCurrentVersion().getWorldVersion());
               dynamic = dynamic.remove("DataVersion");
               Map<ResourceLocation, AdvancementProgress> map = GSON.getAdapter(TYPE_TOKEN).fromJsonTree(dynamic.getValue());
               if (map == null) {
                  throw new JsonParseException("Found null for advancements");
               }

               Stream<Map.Entry<ResourceLocation, AdvancementProgress>> stream = map.entrySet().stream().sorted(Comparator.comparing(Map.Entry::getValue));

               for(Map.Entry<ResourceLocation, AdvancementProgress> entry : stream.collect(Collectors.toList())) {
                  Advancement advancement = pManager.getAdvancement(entry.getKey());
                  if (advancement == null) {
                     LOGGER.warn("Ignored advancement '{}' in progress file {} - it doesn't exist anymore?", entry.getKey(), this.file);
                  } else {
                     this.startProgress(advancement, entry.getValue());
                  }
               }
            } catch (Throwable throwable1) {
               try {
                  jsonreader.close();
               } catch (Throwable throwable) {
                  throwable1.addSuppressed(throwable);
               }

               throw throwable1;
            }

            jsonreader.close();
         } catch (JsonParseException jsonparseexception) {
            LOGGER.error("Couldn't parse player advancements in {}", this.file, jsonparseexception);
         } catch (IOException ioexception) {
            LOGGER.error("Couldn't access player advancements in {}", this.file, ioexception);
         }
      }

      this.checkForAutomaticTriggers(pManager);

      if (net.minecraftforge.common.ForgeConfig.SERVER.fixAdvancementLoading.get())
         net.minecraftforge.common.AdvancementLoadFix.loadVisibility(this, this.visible, this.visibilityChanged, this.advancements, this.progressChanged, this::shouldBeVisible);
      else
      this.ensureAllVisible();
      this.registerListeners(pManager);
   }

   public void save() {
      Map<ResourceLocation, AdvancementProgress> map = Maps.newHashMap();

      for(Map.Entry<Advancement, AdvancementProgress> entry : this.advancements.entrySet()) {
         AdvancementProgress advancementprogress = entry.getValue();
         if (advancementprogress.hasProgress()) {
            map.put(entry.getKey().getId(), advancementprogress);
         }
      }

      if (this.file.getParentFile() != null) {
         this.file.getParentFile().mkdirs();
      }

      JsonElement jsonelement = GSON.toJsonTree(map);
      jsonelement.getAsJsonObject().addProperty("DataVersion", SharedConstants.getCurrentVersion().getWorldVersion());

      try {
         OutputStream outputstream = new FileOutputStream(this.file);

         try {
            Writer writer = new OutputStreamWriter(outputstream, Charsets.UTF_8.newEncoder());

            try {
               GSON.toJson(jsonelement, writer);
            } catch (Throwable throwable2) {
               try {
                  writer.close();
               } catch (Throwable throwable1) {
                  throwable2.addSuppressed(throwable1);
               }

               throw throwable2;
            }

            writer.close();
         } catch (Throwable throwable3) {
            try {
               outputstream.close();
            } catch (Throwable throwable) {
               throwable3.addSuppressed(throwable);
            }

            throw throwable3;
         }

         outputstream.close();
      } catch (IOException ioexception) {
         LOGGER.error("Couldn't save player advancements to {}", this.file, ioexception);
      }

   }

   public boolean award(Advancement pAdvancement, String pCriterionKey) {
      // Forge: don't grant advancements for fake players
      if (this.player instanceof net.minecraftforge.common.util.FakePlayer) return false;
      boolean flag = false;
      AdvancementProgress advancementprogress = this.getOrStartProgress(pAdvancement);
      boolean flag1 = advancementprogress.isDone();
      if (advancementprogress.grantProgress(pCriterionKey)) {
         this.unregisterListeners(pAdvancement);
         this.progressChanged.add(pAdvancement);
         flag = true;
         net.minecraftforge.event.ForgeEventFactory.onAdvancementProgressedEvent(this.player, pAdvancement, advancementprogress, pCriterionKey, net.minecraftforge.event.entity.player.AdvancementEvent.AdvancementProgressEvent.ProgressType.GRANT);
         if (!flag1 && advancementprogress.isDone()) {
            pAdvancement.getRewards().grant(this.player);
            if (pAdvancement.getDisplay() != null && pAdvancement.getDisplay().shouldAnnounceChat() && this.player.level.getGameRules().getBoolean(GameRules.RULE_ANNOUNCE_ADVANCEMENTS)) {
               this.playerList.broadcastSystemMessage(Component.translatable("chat.type.advancement." + pAdvancement.getDisplay().getFrame().getName(), this.player.getDisplayName(), pAdvancement.getChatComponent()), false);
            }
            net.minecraftforge.common.ForgeHooks.onAdvancement(this.player, pAdvancement);
            net.minecraftforge.event.ForgeEventFactory.onAdvancementEarnedEvent(this.player, pAdvancement);
         }
      }

      if (advancementprogress.isDone()) {
         this.ensureVisibility(pAdvancement);
      }

      return flag;
   }

   public boolean revoke(Advancement pAdvancement, String pCriterionKey) {
      boolean flag = false;
      AdvancementProgress advancementprogress = this.getOrStartProgress(pAdvancement);
      if (advancementprogress.revokeProgress(pCriterionKey)) {
         this.registerListeners(pAdvancement);
         this.progressChanged.add(pAdvancement);
         flag = true;
         net.minecraftforge.event.ForgeEventFactory.onAdvancementProgressedEvent(this.player, pAdvancement, advancementprogress, pCriterionKey, net.minecraftforge.event.entity.player.AdvancementEvent.AdvancementProgressEvent.ProgressType.REVOKE);
      }

      if (!advancementprogress.hasProgress()) {
         this.ensureVisibility(pAdvancement);
      }

      return flag;
   }

   private void registerListeners(Advancement pAdvancement) {
      AdvancementProgress advancementprogress = this.getOrStartProgress(pAdvancement);
      if (!advancementprogress.isDone()) {
         for(Map.Entry<String, Criterion> entry : pAdvancement.getCriteria().entrySet()) {
            CriterionProgress criterionprogress = advancementprogress.getCriterion(entry.getKey());
            if (criterionprogress != null && !criterionprogress.isDone()) {
               CriterionTriggerInstance criteriontriggerinstance = entry.getValue().getTrigger();
               if (criteriontriggerinstance != null) {
                  CriterionTrigger<CriterionTriggerInstance> criteriontrigger = CriteriaTriggers.getCriterion(criteriontriggerinstance.getCriterion());
                  if (criteriontrigger != null) {
                     criteriontrigger.addPlayerListener(this, new CriterionTrigger.Listener<>(criteriontriggerinstance, pAdvancement, entry.getKey()));
                  }
               }
            }
         }

      }
   }

   private void unregisterListeners(Advancement pAdvancement) {
      AdvancementProgress advancementprogress = this.getOrStartProgress(pAdvancement);

      for(Map.Entry<String, Criterion> entry : pAdvancement.getCriteria().entrySet()) {
         CriterionProgress criterionprogress = advancementprogress.getCriterion(entry.getKey());
         if (criterionprogress != null && (criterionprogress.isDone() || advancementprogress.isDone())) {
            CriterionTriggerInstance criteriontriggerinstance = entry.getValue().getTrigger();
            if (criteriontriggerinstance != null) {
               CriterionTrigger<CriterionTriggerInstance> criteriontrigger = CriteriaTriggers.getCriterion(criteriontriggerinstance.getCriterion());
               if (criteriontrigger != null) {
                  criteriontrigger.removePlayerListener(this, new CriterionTrigger.Listener<>(criteriontriggerinstance, pAdvancement, entry.getKey()));
               }
            }
         }
      }

   }

   public void flushDirty(ServerPlayer pServerPlayer) {
      if (this.isFirstPacket || !this.visibilityChanged.isEmpty() || !this.progressChanged.isEmpty()) {
         Map<ResourceLocation, AdvancementProgress> map = Maps.newHashMap();
         Set<Advancement> set = Sets.newLinkedHashSet();
         Set<ResourceLocation> set1 = Sets.newLinkedHashSet();

         for(Advancement advancement : this.progressChanged) {
            if (this.visible.contains(advancement)) {
               map.put(advancement.getId(), this.advancements.get(advancement));
            }
         }

         for(Advancement advancement1 : this.visibilityChanged) {
            if (this.visible.contains(advancement1)) {
               set.add(advancement1);
            } else {
               set1.add(advancement1.getId());
            }
         }

         if (this.isFirstPacket || !map.isEmpty() || !set.isEmpty() || !set1.isEmpty()) {
            pServerPlayer.connection.send(new ClientboundUpdateAdvancementsPacket(this.isFirstPacket, set, set1, map));
            this.visibilityChanged.clear();
            this.progressChanged.clear();
         }
      }

      this.isFirstPacket = false;
   }

   public void setSelectedTab(@Nullable Advancement pAdvancement) {
      Advancement advancement = this.lastSelectedTab;
      if (pAdvancement != null && pAdvancement.getParent() == null && pAdvancement.getDisplay() != null) {
         this.lastSelectedTab = pAdvancement;
      } else {
         this.lastSelectedTab = null;
      }

      if (advancement != this.lastSelectedTab) {
         this.player.connection.send(new ClientboundSelectAdvancementsTabPacket(this.lastSelectedTab == null ? null : this.lastSelectedTab.getId()));
      }

   }

   public AdvancementProgress getOrStartProgress(Advancement pAdvancement) {
      AdvancementProgress advancementprogress = this.advancements.get(pAdvancement);
      if (advancementprogress == null) {
         advancementprogress = new AdvancementProgress();
         this.startProgress(pAdvancement, advancementprogress);
      }

      return advancementprogress;
   }

   private void startProgress(Advancement pAdvancement, AdvancementProgress pProgress) {
      pProgress.update(pAdvancement.getCriteria(), pAdvancement.getRequirements());
      this.advancements.put(pAdvancement, pProgress);
   }

   private void ensureVisibility(Advancement pAdvancement) {
      boolean flag = this.shouldBeVisible(pAdvancement);
      boolean flag1 = this.visible.contains(pAdvancement);
      if (flag && !flag1) {
         this.visible.add(pAdvancement);
         this.visibilityChanged.add(pAdvancement);
         if (this.advancements.containsKey(pAdvancement)) {
            this.progressChanged.add(pAdvancement);
         }
      } else if (!flag && flag1) {
         this.visible.remove(pAdvancement);
         this.visibilityChanged.add(pAdvancement);
      }

      if (flag != flag1 && pAdvancement.getParent() != null) {
         this.ensureVisibility(pAdvancement.getParent());
      }

      for(Advancement advancement : pAdvancement.getChildren()) {
         this.ensureVisibility(advancement);
      }

   }

   private boolean shouldBeVisible(Advancement pAdvancement) {
      for(int i = 0; pAdvancement != null && i <= 2; ++i) {
         if (i == 0 && this.hasCompletedChildrenOrSelf(pAdvancement)) {
            return true;
         }

         if (pAdvancement.getDisplay() == null) {
            return false;
         }

         AdvancementProgress advancementprogress = this.getOrStartProgress(pAdvancement);
         if (advancementprogress.isDone()) {
            return true;
         }

         if (pAdvancement.getDisplay().isHidden()) {
            return false;
         }

         pAdvancement = pAdvancement.getParent();
      }

      return false;
   }

   private boolean hasCompletedChildrenOrSelf(Advancement pAdvancement) {
      AdvancementProgress advancementprogress = this.getOrStartProgress(pAdvancement);
      if (advancementprogress.isDone()) {
         return true;
      } else {
         for(Advancement advancement : pAdvancement.getChildren()) {
            if (this.hasCompletedChildrenOrSelf(advancement)) {
               return true;
            }
         }

         return false;
      }
   }
}
