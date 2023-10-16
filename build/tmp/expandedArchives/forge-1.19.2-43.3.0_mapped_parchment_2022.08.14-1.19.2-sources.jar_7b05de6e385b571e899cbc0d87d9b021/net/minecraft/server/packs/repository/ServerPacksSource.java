package net.minecraft.server.packs.repository;

import java.util.function.Consumer;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.VanillaPackResources;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;

public class ServerPacksSource implements RepositorySource {
   public static final PackMetadataSection BUILT_IN_METADATA = new PackMetadataSection(Component.translatable("dataPack.vanilla.description"), PackType.SERVER_DATA.getVersion(SharedConstants.getCurrentVersion()));
   public static final String VANILLA_ID = "vanilla";
   private final VanillaPackResources vanillaPack = new VanillaPackResources(BUILT_IN_METADATA, "minecraft");

   public void loadPacks(Consumer<Pack> pInfoConsumer, Pack.PackConstructor pInfoFactory) {
      Pack pack = Pack.create("vanilla", false, () -> {
         return this.vanillaPack;
      }, pInfoFactory, Pack.Position.BOTTOM, PackSource.BUILT_IN);
      if (pack != null) {
         pInfoConsumer.accept(pack);
      }

   }
}