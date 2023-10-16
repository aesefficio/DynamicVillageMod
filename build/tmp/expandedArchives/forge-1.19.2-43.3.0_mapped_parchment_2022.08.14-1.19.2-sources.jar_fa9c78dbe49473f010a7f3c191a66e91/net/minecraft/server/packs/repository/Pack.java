package net.minecraft.server.packs.repository;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import org.slf4j.Logger;

public class Pack {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final String id;
   private final Supplier<PackResources> supplier;
   private final Component title;
   private final Component description;
   private final PackCompatibility compatibility;
   private final Pack.Position defaultPosition;
   private final boolean required;
   private final boolean fixedPosition;
   private final boolean hidden; // Forge: Allow packs to be hidden from the UI entirely
   private final PackSource packSource;

   @Nullable
   public static Pack create(String pId, boolean pRequired, Supplier<PackResources> pSupplier, Pack.PackConstructor pFactory, Pack.Position pDefaultPosition, PackSource pPackSource) {
      try {
         PackResources packresources = pSupplier.get();

         Pack pack;
         label54: {
            try {
               PackMetadataSection packmetadatasection = packresources.getMetadataSection(PackMetadataSection.SERIALIZER);
               if (packmetadatasection != null) {
                  pack = pFactory.create(pId, Component.literal(packresources.getName()), pRequired, pSupplier, packmetadatasection, pDefaultPosition, pPackSource, packresources.isHidden());
                  break label54;
               }

               LOGGER.warn("Couldn't find pack meta for pack {}", (Object)pId);
            } catch (Throwable throwable1) {
               if (packresources != null) {
                  try {
                     packresources.close();
                  } catch (Throwable throwable) {
                     throwable1.addSuppressed(throwable);
                  }
               }

               throw throwable1;
            }

            if (packresources != null) {
               packresources.close();
            }

            return null;
         }

         if (packresources != null) {
            packresources.close();
         }

         return pack;
      } catch (IOException ioexception) {
         LOGGER.warn("Couldn't get pack info for: {}", (Object)ioexception.toString());
         return null;
      }
   }

   @Deprecated
   public Pack(String pId, boolean pRequired, Supplier<PackResources> pSupplier, Component pTitle, Component pDescription, PackCompatibility pCompatibility, Pack.Position pDefaultPosition, boolean pFixedPosition, PackSource pPackSource) {
       this(pId, pRequired, pSupplier, pTitle, pDescription, pCompatibility, pDefaultPosition, pFixedPosition, pPackSource, false);
   }

   public Pack(String pId, boolean pRequired, Supplier<PackResources> pSupplier, Component pTitle, Component pDescription, PackCompatibility pCompatibility, Pack.Position pDefaultPosition, boolean pFixedPosition, PackSource pPackSource, boolean hidden) {
      this.id = pId;
      this.supplier = pSupplier;
      this.title = pTitle;
      this.description = pDescription;
      this.compatibility = pCompatibility;
      this.required = pRequired;
      this.defaultPosition = pDefaultPosition;
      this.fixedPosition = pFixedPosition;
      this.packSource = pPackSource;
      this.hidden = hidden;
   }

   @Deprecated
   public Pack(String pId, Component pTitle, boolean pRequired, Supplier<PackResources> pSupplier, PackMetadataSection pMetadata, PackType pType, Pack.Position pDefaultPosition, PackSource pPackSource) {
      this(pId, pRequired, pSupplier, pTitle, pMetadata.getDescription(), PackCompatibility.forMetadata(pMetadata, pType), pDefaultPosition, false, pPackSource, false);
   }

   public Pack(String pId, Component pTitle, boolean pRequired, Supplier<PackResources> pSupplier, PackMetadataSection pMetadata, PackType pType, Pack.Position pDefaultPosition, PackSource pPackSource, boolean hidden) {
      this(pId, pRequired, pSupplier, pTitle, pMetadata.getDescription(), PackCompatibility.forMetadata(pMetadata, pType), pDefaultPosition, false, pPackSource, hidden);
   }

   public Component getTitle() {
      return this.title;
   }

   public Component getDescription() {
      return this.description;
   }

   /**
    * 
    * @param pGreen used to indicate either a successful operation or datapack enabled status
    */
   public Component getChatLink(boolean pGreen) {
      return ComponentUtils.wrapInSquareBrackets(this.packSource.decorate(Component.literal(this.id))).withStyle((p_10441_) -> {
         return p_10441_.withColor(pGreen ? ChatFormatting.GREEN : ChatFormatting.RED).withInsertion(StringArgumentType.escapeIfRequired(this.id)).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.empty().append(this.title).append("\n").append(this.description)));
      });
   }

   public PackCompatibility getCompatibility() {
      return this.compatibility;
   }

   public PackResources open() {
      return this.supplier.get();
   }

   public String getId() {
      return this.id;
   }

   public boolean isRequired() {
      return this.required;
   }

   public boolean isFixedPosition() {
      return this.fixedPosition;
   }

   public Pack.Position getDefaultPosition() {
      return this.defaultPosition;
   }

   public PackSource getPackSource() {
      return this.packSource;
   }

   public boolean isHidden() { return hidden; }

   public boolean equals(Object p_10448_) {
      if (this == p_10448_) {
         return true;
      } else if (!(p_10448_ instanceof Pack)) {
         return false;
      } else {
         Pack pack = (Pack)p_10448_;
         return this.id.equals(pack.id);
      }
   }

   public int hashCode() {
      return this.id.hashCode();
   }

   @FunctionalInterface
   public interface PackConstructor {
      @Deprecated
      @Nullable
      default Pack create(String pId, Component pTitle, boolean pRequired, Supplier<PackResources> pSupplier, PackMetadataSection pMetadata, Pack.Position pDefaultPosition, PackSource pPackSource)
      {
         return create(pId, pTitle, pRequired, pSupplier, pMetadata, pDefaultPosition, pPackSource, false);
      }

      @Nullable
      Pack create(String pId, Component pTitle, boolean pRequired, Supplier<PackResources> pSupplier, PackMetadataSection pMetadata, Pack.Position pDefaultPosition, PackSource pPackSource, boolean hidden);
   }

   public static enum Position {
      TOP,
      BOTTOM;

      public <T> int insert(List<T> p_10471_, T p_10472_, Function<T, Pack> p_10473_, boolean p_10474_) {
         Pack.Position pack$position = p_10474_ ? this.opposite() : this;
         if (pack$position == BOTTOM) {
            int j;
            for(j = 0; j < p_10471_.size(); ++j) {
               Pack pack1 = p_10473_.apply(p_10471_.get(j));
               if (!pack1.isFixedPosition() || pack1.getDefaultPosition() != this) {
                  break;
               }
            }

            p_10471_.add(j, p_10472_);
            return j;
         } else {
            int i;
            for(i = p_10471_.size() - 1; i >= 0; --i) {
               Pack pack = p_10473_.apply(p_10471_.get(i));
               if (!pack.isFixedPosition() || pack.getDefaultPosition() != this) {
                  break;
               }
            }

            p_10471_.add(i + 1, p_10472_);
            return i + 1;
         }
      }

      public Pack.Position opposite() {
         return this == TOP ? BOTTOM : TOP;
      }
   }
}
