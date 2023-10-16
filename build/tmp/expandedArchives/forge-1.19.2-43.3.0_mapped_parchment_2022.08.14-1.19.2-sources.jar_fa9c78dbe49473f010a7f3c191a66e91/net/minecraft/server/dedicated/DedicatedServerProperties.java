package net.minecraft.server.dedicated;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import org.slf4j.Logger;

public class DedicatedServerProperties extends Settings<DedicatedServerProperties> {
   static final Logger LOGGER = LogUtils.getLogger();
   private static final Pattern SHA1 = Pattern.compile("^[a-fA-F0-9]{40}$");
   public final boolean onlineMode = this.get("online-mode", true);
   public final boolean preventProxyConnections = this.get("prevent-proxy-connections", false);
   public final String serverIp = this.get("server-ip", "");
   public final boolean spawnAnimals = this.get("spawn-animals", true);
   public final boolean spawnNpcs = this.get("spawn-npcs", true);
   public final boolean pvp = this.get("pvp", true);
   public final boolean allowFlight = this.get("allow-flight", false);
   public final String motd = this.get("motd", "A Minecraft Server");
   public final boolean forceGameMode = this.get("force-gamemode", false);
   public final boolean enforceWhitelist = this.get("enforce-whitelist", false);
   public final Difficulty difficulty = this.get("difficulty", dispatchNumberOrString(Difficulty::byId, Difficulty::byName), Difficulty::getKey, Difficulty.EASY);
   public final GameType gamemode = this.get("gamemode", dispatchNumberOrString(GameType::byId, GameType::byName), GameType::getName, GameType.SURVIVAL);
   public final String levelName = this.get("level-name", "world");
   public final int serverPort = this.get("server-port", 25565);
   @Nullable
   public final Boolean announcePlayerAchievements = this.getLegacyBoolean("announce-player-achievements");
   public final boolean enableQuery = this.get("enable-query", false);
   public final int queryPort = this.get("query.port", 25565);
   public final boolean enableRcon = this.get("enable-rcon", false);
   public final int rconPort = this.get("rcon.port", 25575);
   public final String rconPassword = this.get("rcon.password", "");
   public final boolean hardcore = this.get("hardcore", false);
   public final boolean allowNether = this.get("allow-nether", true);
   public final boolean spawnMonsters = this.get("spawn-monsters", true);
   public final boolean useNativeTransport = this.get("use-native-transport", true);
   public final boolean enableCommandBlock = this.get("enable-command-block", false);
   public final int spawnProtection = this.get("spawn-protection", 16);
   public final int opPermissionLevel = this.get("op-permission-level", 4);
   public final int functionPermissionLevel = this.get("function-permission-level", 2);
   public final long maxTickTime = this.get("max-tick-time", TimeUnit.MINUTES.toMillis(1L));
   public final int maxChainedNeighborUpdates = this.get("max-chained-neighbor-updates", 1000000);
   public final int rateLimitPacketsPerSecond = this.get("rate-limit", 0);
   public final int viewDistance = this.get("view-distance", 10);
   public final int simulationDistance = this.get("simulation-distance", 10);
   public final int maxPlayers = this.get("max-players", 20);
   public final int networkCompressionThreshold = this.get("network-compression-threshold", 256);
   public final boolean broadcastRconToOps = this.get("broadcast-rcon-to-ops", true);
   public final boolean broadcastConsoleToOps = this.get("broadcast-console-to-ops", true);
   public final int maxWorldSize = this.get("max-world-size", (p_139771_) -> {
      return Mth.clamp(p_139771_, 1, 29999984);
   }, 29999984);
   public final boolean syncChunkWrites = this.get("sync-chunk-writes", true);
   public final boolean enableJmxMonitoring = this.get("enable-jmx-monitoring", false);
   public final boolean enableStatus = this.get("enable-status", true);
   public final boolean hideOnlinePlayers = this.get("hide-online-players", false);
   public final int entityBroadcastRangePercentage = this.get("entity-broadcast-range-percentage", (p_139769_) -> {
      return Mth.clamp(p_139769_, 10, 1000);
   }, 100);
   public final String textFilteringConfig = this.get("text-filtering-config", "");
   public Optional<MinecraftServer.ServerResourcePackInfo> serverResourcePackInfo;
   public final boolean previewsChat = this.get("previews-chat", false);
   public final Settings<DedicatedServerProperties>.MutableValue<Integer> playerIdleTimeout = this.getMutable("player-idle-timeout", 0);
   public final Settings<DedicatedServerProperties>.MutableValue<Boolean> whiteList = this.getMutable("white-list", false);
   public final boolean enforceSecureProfile = this.get("enforce-secure-profile", true);
   private final DedicatedServerProperties.WorldGenProperties worldGenProperties = new DedicatedServerProperties.WorldGenProperties(this.get("level-seed", ""), this.get("generator-settings", (p_211543_) -> {
      return GsonHelper.parse(!p_211543_.isEmpty() ? p_211543_ : "{}");
   }, new JsonObject()), this.get("generate-structures", true), this.get("level-type", (p_211541_) -> {
      return p_211541_.toLowerCase(Locale.ROOT);
   }, WorldPresets.NORMAL.location().toString()));
   @Nullable
   private WorldGenSettings worldGenSettings;

   public DedicatedServerProperties(Properties p_180926_) {
      super(p_180926_);
      this.serverResourcePackInfo = getServerPackInfo(this.get("resource-pack", ""), this.get("resource-pack-sha1", ""), this.getLegacyString("resource-pack-hash"), this.get("require-resource-pack", false), this.get("resource-pack-prompt", ""));
   }

   public static DedicatedServerProperties fromFile(Path p_180930_) {
      return new DedicatedServerProperties(loadFromFile(p_180930_));
   }

   protected DedicatedServerProperties reload(RegistryAccess p_139761_, Properties p_139762_) {
      DedicatedServerProperties dedicatedserverproperties = new DedicatedServerProperties(p_139762_);
      dedicatedserverproperties.getWorldGenSettings(p_139761_);
      return dedicatedserverproperties;
   }

   @Nullable
   private static Component parseResourcePackPrompt(String p_214815_) {
      if (!Strings.isNullOrEmpty(p_214815_)) {
         try {
            return Component.Serializer.fromJson(p_214815_);
         } catch (Exception exception) {
            LOGGER.warn("Failed to parse resource pack prompt '{}'", p_214815_, exception);
         }
      }

      return null;
   }

   private static Optional<MinecraftServer.ServerResourcePackInfo> getServerPackInfo(String p_214809_, String p_214810_, @Nullable String p_214811_, boolean p_214812_, String p_214813_) {
      if (p_214809_.isEmpty()) {
         return Optional.empty();
      } else {
         String s;
         if (!p_214810_.isEmpty()) {
            s = p_214810_;
            if (!Strings.isNullOrEmpty(p_214811_)) {
               LOGGER.warn("resource-pack-hash is deprecated and found along side resource-pack-sha1. resource-pack-hash will be ignored.");
            }
         } else if (!Strings.isNullOrEmpty(p_214811_)) {
            LOGGER.warn("resource-pack-hash is deprecated. Please use resource-pack-sha1 instead.");
            s = p_214811_;
         } else {
            s = "";
         }

         if (s.isEmpty()) {
            LOGGER.warn("You specified a resource pack without providing a sha1 hash. Pack will be updated on the client only if you change the name of the pack.");
         } else if (!SHA1.matcher(s).matches()) {
            LOGGER.warn("Invalid sha1 for resource-pack-sha1");
         }

         Component component = parseResourcePackPrompt(p_214813_);
         return Optional.of(new MinecraftServer.ServerResourcePackInfo(p_214809_, s, p_214812_, component));
      }
   }

   public WorldGenSettings getWorldGenSettings(RegistryAccess p_180928_) {
      if (this.worldGenSettings == null) {
         this.worldGenSettings = this.worldGenProperties.create(p_180928_);
      }

      return this.worldGenSettings;
   }

   public static record WorldGenProperties(String levelSeed, JsonObject generatorSettings, boolean generateStructures, String levelType) {
      private static final Map<String, ResourceKey<WorldPreset>> LEGACY_PRESET_NAMES = Map.of("default", WorldPresets.NORMAL, "largebiomes", WorldPresets.LARGE_BIOMES);

      public WorldGenSettings create(RegistryAccess p_214827_) {
         long i = WorldGenSettings.parseSeed(this.levelSeed()).orElse(RandomSource.create().nextLong());
         Registry<WorldPreset> registry = p_214827_.registryOrThrow(Registry.WORLD_PRESET_REGISTRY);
         Holder<WorldPreset> holder = registry.getHolder(WorldPresets.NORMAL).or(() -> {
            return registry.holders().findAny();
         }).orElseThrow(() -> {
            return new IllegalStateException("Invalid datapack contents: can't find default preset");
         });
         Holder<WorldPreset> holder1 = Optional.ofNullable(ResourceLocation.tryParse(this.levelType)).map((p_214821_) -> {
            return ResourceKey.create(Registry.WORLD_PRESET_REGISTRY, p_214821_);
         }).or(() -> {
            return Optional.ofNullable(LEGACY_PRESET_NAMES.get(this.levelType));
         }).flatMap(registry::getHolder).orElseGet(() -> {
            DedicatedServerProperties.LOGGER.warn("Failed to parse level-type {}, defaulting to {}", this.levelType, holder.unwrapKey().map((p_214819_) -> {
               return p_214819_.location().toString();
            }).orElse("[unnamed]"));
            return holder;
         });
         WorldGenSettings worldgensettings = holder1.value().createWorldGenSettings(i, this.generateStructures, false);
         if (holder1.is(WorldPresets.FLAT)) {
            RegistryOps<JsonElement> registryops = RegistryOps.create(JsonOps.INSTANCE, p_214827_);
            Optional<FlatLevelGeneratorSettings> optional = FlatLevelGeneratorSettings.CODEC.parse(new Dynamic<>(registryops, this.generatorSettings())).resultOrPartial(DedicatedServerProperties.LOGGER::error);
            if (optional.isPresent()) {
               Registry<StructureSet> registry1 = p_214827_.registryOrThrow(Registry.STRUCTURE_SET_REGISTRY);
               return WorldGenSettings.replaceOverworldGenerator(p_214827_, worldgensettings, new FlatLevelSource(registry1, optional.get()));
            }
         }

         return worldgensettings;
      }
   }
}