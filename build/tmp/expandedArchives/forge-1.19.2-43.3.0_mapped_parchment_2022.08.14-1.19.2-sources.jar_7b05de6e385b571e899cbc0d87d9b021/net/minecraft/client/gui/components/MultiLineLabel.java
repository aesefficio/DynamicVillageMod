package net.minecraft.client.gui.components;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface MultiLineLabel {
   MultiLineLabel EMPTY = new MultiLineLabel() {
      public int renderCentered(PoseStack p_94378_, int p_94379_, int p_94380_) {
         return p_94380_;
      }

      public int renderCentered(PoseStack p_94382_, int p_94383_, int p_94384_, int p_94385_, int p_94386_) {
         return p_94384_;
      }

      public int renderLeftAligned(PoseStack p_94388_, int p_94389_, int p_94390_, int p_94391_, int p_94392_) {
         return p_94390_;
      }

      public int renderLeftAlignedNoShadow(PoseStack p_94394_, int p_94395_, int p_94396_, int p_94397_, int p_94398_) {
         return p_94396_;
      }

      public void renderBackgroundCentered(PoseStack p_210824_, int p_210825_, int p_210826_, int p_210827_, int p_210828_, int p_210829_) {
      }

      public int getLineCount() {
         return 0;
      }

      public int getWidth() {
         return 0;
      }
   };

   static MultiLineLabel create(Font pFont, FormattedText pFormattedText, int p_94344_) {
      return createFixed(pFont, pFont.split(pFormattedText, p_94344_).stream().map((p_94374_) -> {
         return new MultiLineLabel.TextWithWidth(p_94374_, pFont.width(p_94374_));
      }).collect(ImmutableList.toImmutableList()));
   }

   static MultiLineLabel create(Font pFont, FormattedText pFormattedText, int p_94348_, int p_94349_) {
      return createFixed(pFont, pFont.split(pFormattedText, p_94348_).stream().limit((long)p_94349_).map((p_94371_) -> {
         return new MultiLineLabel.TextWithWidth(p_94371_, pFont.width(p_94371_));
      }).collect(ImmutableList.toImmutableList()));
   }

   static MultiLineLabel create(Font pFont, Component... p_94352_) {
      return createFixed(pFont, Arrays.stream(p_94352_).map(Component::getVisualOrderText).map((p_94360_) -> {
         return new MultiLineLabel.TextWithWidth(p_94360_, pFont.width(p_94360_));
      }).collect(ImmutableList.toImmutableList()));
   }

   static MultiLineLabel create(Font pFont, List<Component> p_169038_) {
      return createFixed(pFont, p_169038_.stream().map(Component::getVisualOrderText).map((p_169035_) -> {
         return new MultiLineLabel.TextWithWidth(p_169035_, pFont.width(p_169035_));
      }).collect(ImmutableList.toImmutableList()));
   }

   static MultiLineLabel createFixed(final Font pFont, final List<MultiLineLabel.TextWithWidth> p_94363_) {
      return p_94363_.isEmpty() ? EMPTY : new MultiLineLabel() {
         private final int width = p_94363_.stream().mapToInt((p_232527_) -> {
            return p_232527_.width;
         }).max().orElse(0);

         public int renderCentered(PoseStack p_94406_, int p_94407_, int p_94408_) {
            return this.renderCentered(p_94406_, p_94407_, p_94408_, 9, 16777215);
         }

         public int renderCentered(PoseStack p_94410_, int p_94411_, int p_94412_, int p_94413_, int p_94414_) {
            int i = p_94412_;

            for(MultiLineLabel.TextWithWidth multilinelabel$textwithwidth : p_94363_) {
               pFont.drawShadow(p_94410_, multilinelabel$textwithwidth.text, (float)(p_94411_ - multilinelabel$textwithwidth.width / 2), (float)i, p_94414_);
               i += p_94413_;
            }

            return i;
         }

         public int renderLeftAligned(PoseStack p_94416_, int p_94417_, int p_94418_, int p_94419_, int p_94420_) {
            int i = p_94418_;

            for(MultiLineLabel.TextWithWidth multilinelabel$textwithwidth : p_94363_) {
               pFont.drawShadow(p_94416_, multilinelabel$textwithwidth.text, (float)p_94417_, (float)i, p_94420_);
               i += p_94419_;
            }

            return i;
         }

         public int renderLeftAlignedNoShadow(PoseStack p_94422_, int p_94423_, int p_94424_, int p_94425_, int p_94426_) {
            int i = p_94424_;

            for(MultiLineLabel.TextWithWidth multilinelabel$textwithwidth : p_94363_) {
               pFont.draw(p_94422_, multilinelabel$textwithwidth.text, (float)p_94423_, (float)i, p_94426_);
               i += p_94425_;
            }

            return i;
         }

         public void renderBackgroundCentered(PoseStack p_210831_, int p_210832_, int p_210833_, int p_210834_, int p_210835_, int p_210836_) {
            int i = p_94363_.stream().mapToInt((p_232524_) -> {
               return p_232524_.width;
            }).max().orElse(0);
            if (i > 0) {
               GuiComponent.fill(p_210831_, p_210832_ - i / 2 - p_210835_, p_210833_ - p_210835_, p_210832_ + i / 2 + p_210835_, p_210833_ + p_94363_.size() * p_210834_ + p_210835_, p_210836_);
            }

         }

         public int getLineCount() {
            return p_94363_.size();
         }

         public int getWidth() {
            return this.width;
         }
      };
   }

   int renderCentered(PoseStack pPoseStack, int pWidth, int pTextWithWidthList);

   int renderCentered(PoseStack pPoseStack, int pWidth, int pX, int pY, int pColor);

   int renderLeftAligned(PoseStack pPoseStack, int pX, int pY, int pLineHeight, int pColor);

   int renderLeftAlignedNoShadow(PoseStack pPoseStack, int pX, int pY, int pLineHeight, int pColor);

   void renderBackgroundCentered(PoseStack pPoseStack, int pX, int pY, int p_210820_, int p_210821_, int pColor);

   int getLineCount();

   int getWidth();

   @OnlyIn(Dist.CLIENT)
   public static class TextWithWidth {
      final FormattedCharSequence text;
      final int width;

      TextWithWidth(FormattedCharSequence pText, int pWidth) {
         this.text = pText;
         this.width = pWidth;
      }
   }
}