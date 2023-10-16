package net.minecraft.data.advancements;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.advancements.Advancement;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

public class AdvancementProvider implements DataProvider {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final DataGenerator.PathProvider pathProvider;
   private final List<Consumer<Consumer<Advancement>>> tabs = ImmutableList.of(new TheEndAdvancements(), new HusbandryAdvancements(), new AdventureAdvancements(), new NetherAdvancements(), new StoryAdvancements());
   protected net.minecraftforge.common.data.ExistingFileHelper fileHelper;

   @Deprecated
   public AdvancementProvider(DataGenerator pGenerator) {
      this.pathProvider = pGenerator.createPathProvider(DataGenerator.Target.DATA_PACK, "advancements");
   }

   public AdvancementProvider(DataGenerator generatorIn, net.minecraftforge.common.data.ExistingFileHelper fileHelperIn) {
      this.pathProvider = generatorIn.createPathProvider(DataGenerator.Target.DATA_PACK, "advancements");
      this.fileHelper = fileHelperIn;
   }

   public void run(CachedOutput pOutput) {
      Set<ResourceLocation> set = Sets.newHashSet();
      Consumer<Advancement> consumer = (p_236162_) -> {
         if (!set.add(p_236162_.getId())) {
            throw new IllegalStateException("Duplicate advancement " + p_236162_.getId());
         } else {
            Path path = this.pathProvider.json(p_236162_.getId());

            try {
               DataProvider.saveStable(pOutput, p_236162_.deconstruct().serializeToJson(), path);
            } catch (IOException ioexception) {
               LOGGER.error("Couldn't save advancement {}", path, ioexception);
            }

         }
      };

      registerAdvancements(consumer, fileHelper);
   }

   /**
    * This method registers all {@link Advancement advancements}.
    * Mods can override this method to register their own custom advancements.
    *
    * @param consumer a {@link Consumer} which saves any advancements provided
    * @param fileHelper the existing file helper to check for the existence of files like parent advancements
    * @see Advancement.Builder#save(Consumer, ResourceLocation, net.minecraftforge.common.data.ExistingFileHelper)
   */
   protected void registerAdvancements(Consumer<Advancement> consumer, net.minecraftforge.common.data.ExistingFileHelper fileHelper) {
      for(Consumer<Consumer<Advancement>> consumer1 : this.tabs) {
         consumer1.accept(consumer);
      }

   }

   /**
    * Gets a name for this provider, to use in logging.
    */
   public String getName() {
      return "Advancements";
   }
}
