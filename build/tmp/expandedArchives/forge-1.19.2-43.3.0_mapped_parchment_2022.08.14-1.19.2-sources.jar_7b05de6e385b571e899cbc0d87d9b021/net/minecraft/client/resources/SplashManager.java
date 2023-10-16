package net.minecraft.client.resources;

import com.google.common.collect.Lists;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SplashManager extends SimplePreparableReloadListener<List<String>> {
   private static final ResourceLocation SPLASHES_LOCATION = new ResourceLocation("texts/splashes.txt");
   private static final RandomSource RANDOM = RandomSource.create();
   private final List<String> splashes = Lists.newArrayList();
   private final User user;

   public SplashManager(User pUser) {
      this.user = pUser;
   }

   /**
    * Performs any reloading that can be done off-thread, such as file IO
    */
   protected List<String> prepare(ResourceManager pResourceManager, ProfilerFiller pProfiler) {
      try {
         BufferedReader bufferedreader = Minecraft.getInstance().getResourceManager().openAsReader(SPLASHES_LOCATION);

         List list;
         try {
            list = bufferedreader.lines().map(String::trim).filter((p_118876_) -> {
               return p_118876_.hashCode() != 125780783;
            }).collect(Collectors.toList());
         } catch (Throwable throwable1) {
            if (bufferedreader != null) {
               try {
                  bufferedreader.close();
               } catch (Throwable throwable) {
                  throwable1.addSuppressed(throwable);
               }
            }

            throw throwable1;
         }

         if (bufferedreader != null) {
            bufferedreader.close();
         }

         return list;
      } catch (IOException ioexception) {
         return Collections.emptyList();
      }
   }

   protected void apply(List<String> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
      this.splashes.clear();
      this.splashes.addAll(pObject);
   }

   @Nullable
   public String getSplash() {
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(new Date());
      if (calendar.get(2) + 1 == 12 && calendar.get(5) == 24) {
         return "Merry X-mas!";
      } else if (calendar.get(2) + 1 == 1 && calendar.get(5) == 1) {
         return "Happy new year!";
      } else if (calendar.get(2) + 1 == 10 && calendar.get(5) == 31) {
         return "OOoooOOOoooo! Spooky!";
      } else if (this.splashes.isEmpty()) {
         return null;
      } else {
         return this.user != null && RANDOM.nextInt(this.splashes.size()) == 42 ? this.user.getName().toUpperCase(Locale.ROOT) + " IS YOU" : this.splashes.get(RANDOM.nextInt(this.splashes.size()));
      }
   }
}