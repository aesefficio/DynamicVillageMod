package net.minecraft.data.info;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import org.slf4j.Logger;

public class WorldgenRegistryDumpReport implements DataProvider {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final DataGenerator generator;

   public WorldgenRegistryDumpReport(DataGenerator pGenerator) {
      this.generator = pGenerator;
   }

   public void run(CachedOutput pOutput) {
      RegistryAccess registryaccess = RegistryAccess.BUILTIN.get();
      DynamicOps<JsonElement> dynamicops = RegistryOps.create(JsonOps.INSTANCE, registryaccess);
      RegistryAccess.knownRegistries().forEach((p_236219_) -> {
         this.dumpRegistryCap(pOutput, registryaccess, dynamicops, p_236219_);
      });
   }

   private <T> void dumpRegistryCap(CachedOutput p_236205_, RegistryAccess p_236206_, DynamicOps<JsonElement> p_236207_, RegistryAccess.RegistryData<T> p_236208_) {
      ResourceKey<? extends Registry<T>> resourcekey = p_236208_.key();
      Registry<T> registry = p_236206_.ownedRegistryOrThrow(resourcekey);
      DataGenerator.PathProvider datagenerator$pathprovider = this.generator.createPathProvider(DataGenerator.Target.REPORTS, net.minecraftforge.common.ForgeHooks.prefixNamespace(resourcekey.location())); // FORGE: Custom data-pack registries are prefixed with their namespace

      for(Map.Entry<ResourceKey<T>, T> entry : registry.entrySet()) {
         dumpValue(datagenerator$pathprovider.json(entry.getKey().location()), p_236205_, p_236207_, p_236208_.codec(), entry.getValue());
      }

   }

   private static <E> void dumpValue(Path p_236210_, CachedOutput p_236211_, DynamicOps<JsonElement> p_236212_, Encoder<E> p_236213_, E p_236214_) {
      try {
         Optional<JsonElement> optional = p_236213_.encodeStart(p_236212_, p_236214_).resultOrPartial((p_206405_) -> {
            LOGGER.error("Couldn't serialize element {}: {}", p_236210_, p_206405_);
         });
         if (optional.isPresent()) {
            DataProvider.saveStable(p_236211_, optional.get(), p_236210_);
         }
      } catch (IOException ioexception) {
         LOGGER.error("Couldn't save element {}", p_236210_, ioexception);
      }

   }

   /**
    * Gets a name for this provider, to use in logging.
    */
   public String getName() {
      return "Worldgen";
   }
}
