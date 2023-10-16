package net.minecraft.network.chat;

import java.util.Arrays;
import java.util.Collection;

public class CommonComponents {
   public static final Component EMPTY = Component.empty();
   public static final Component OPTION_ON = Component.translatable("options.on");
   public static final Component OPTION_OFF = Component.translatable("options.off");
   public static final Component GUI_DONE = Component.translatable("gui.done");
   public static final Component GUI_CANCEL = Component.translatable("gui.cancel");
   public static final Component GUI_YES = Component.translatable("gui.yes");
   public static final Component GUI_NO = Component.translatable("gui.no");
   public static final Component GUI_PROCEED = Component.translatable("gui.proceed");
   public static final Component GUI_BACK = Component.translatable("gui.back");
   public static final Component GUI_ACKNOWLEDGE = Component.translatable("gui.acknowledge");
   public static final Component CONNECT_FAILED = Component.translatable("connect.failed");
   public static final Component NEW_LINE = Component.literal("\n");
   public static final Component NARRATION_SEPARATOR = Component.literal(". ");
   public static final Component ELLIPSIS = Component.literal("...");

   public static MutableComponent days(long p_239423_) {
      return Component.translatable("gui.days", p_239423_);
   }

   public static MutableComponent hours(long p_240042_) {
      return Component.translatable("gui.hours", p_240042_);
   }

   public static MutableComponent minutes(long p_239878_) {
      return Component.translatable("gui.minutes", p_239878_);
   }

   public static Component optionStatus(boolean pIsEnabled) {
      return pIsEnabled ? OPTION_ON : OPTION_OFF;
   }

   public static MutableComponent optionStatus(Component pMessage, boolean pComposed) {
      return Component.translatable(pComposed ? "options.on.composed" : "options.off.composed", pMessage);
   }

   public static MutableComponent optionNameValue(Component pCaption, Component pValueMessage) {
      return Component.translatable("options.generic_value", pCaption, pValueMessage);
   }

   public static MutableComponent joinForNarration(Component pFirstComponent, Component pSecondComponent) {
      return Component.empty().append(pFirstComponent).append(NARRATION_SEPARATOR).append(pSecondComponent);
   }

   public static Component joinLines(Component... pLines) {
      return joinLines(Arrays.asList(pLines));
   }

   public static Component joinLines(Collection<? extends Component> pLines) {
      return ComponentUtils.formatList(pLines, NEW_LINE);
   }
}