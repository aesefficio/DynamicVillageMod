package net.minecraft.world.level.block.entity;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.UUID;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class SignBlockEntity extends BlockEntity {
   public static final int LINES = 4;
   private static final String[] RAW_TEXT_FIELD_NAMES = new String[]{"Text1", "Text2", "Text3", "Text4"};
   private static final String[] FILTERED_TEXT_FIELD_NAMES = new String[]{"FilteredText1", "FilteredText2", "FilteredText3", "FilteredText4"};
   private final Component[] messages = new Component[]{CommonComponents.EMPTY, CommonComponents.EMPTY, CommonComponents.EMPTY, CommonComponents.EMPTY};
   private final Component[] filteredMessages = new Component[]{CommonComponents.EMPTY, CommonComponents.EMPTY, CommonComponents.EMPTY, CommonComponents.EMPTY};
   private boolean isEditable = true;
   @Nullable
   private UUID playerWhoMayEdit;
   @Nullable
   private FormattedCharSequence[] renderMessages;
   private boolean renderMessagedFiltered;
   private DyeColor color = DyeColor.BLACK;
   private boolean hasGlowingText;

   public SignBlockEntity(BlockPos pPos, BlockState pBlockState) {
      super(BlockEntityType.SIGN, pPos, pBlockState);
   }

   protected void saveAdditional(CompoundTag pTag) {
      super.saveAdditional(pTag);

      for(int i = 0; i < 4; ++i) {
         Component component = this.messages[i];
         String s = Component.Serializer.toJson(component);
         pTag.putString(RAW_TEXT_FIELD_NAMES[i], s);
         Component component1 = this.filteredMessages[i];
         if (!component1.equals(component)) {
            pTag.putString(FILTERED_TEXT_FIELD_NAMES[i], Component.Serializer.toJson(component1));
         }
      }

      pTag.putString("Color", this.color.getName());
      pTag.putBoolean("GlowingText", this.hasGlowingText);
   }

   public void load(CompoundTag pTag) {
      this.isEditable = false;
      super.load(pTag);
      this.color = DyeColor.byName(pTag.getString("Color"), DyeColor.BLACK);

      for(int i = 0; i < 4; ++i) {
         String s = pTag.getString(RAW_TEXT_FIELD_NAMES[i]);
         Component component = this.loadLine(s);
         this.messages[i] = component;
         String s1 = FILTERED_TEXT_FIELD_NAMES[i];
         if (pTag.contains(s1, 8)) {
            this.filteredMessages[i] = this.loadLine(pTag.getString(s1));
         } else {
            this.filteredMessages[i] = component;
         }
      }

      this.renderMessages = null;
      this.hasGlowingText = pTag.getBoolean("GlowingText");
   }

   private Component loadLine(String pLine) {
      Component component = this.deserializeTextSafe(pLine);
      if (this.level instanceof ServerLevel) {
         try {
            return ComponentUtils.updateForEntity(this.createCommandSourceStack((ServerPlayer)null), component, (Entity)null, 0);
         } catch (CommandSyntaxException commandsyntaxexception) {
         }
      }

      return component;
   }

   private Component deserializeTextSafe(String pText) {
      try {
         Component component = Component.Serializer.fromJson(pText);
         if (component != null) {
            return component;
         }
      } catch (Exception exception) {
      }

      return CommonComponents.EMPTY;
   }

   public Component getMessage(int pIndex, boolean pFiltered) {
      return this.getMessages(pFiltered)[pIndex];
   }

   public void setMessage(int pLine, Component pMessage) {
      this.setMessage(pLine, pMessage, pMessage);
   }

   public void setMessage(int pLine, Component pMessage, Component pFilteredMessage) {
      this.messages[pLine] = pMessage;
      this.filteredMessages[pLine] = pFilteredMessage;
      this.renderMessages = null;
   }

   public FormattedCharSequence[] getRenderMessages(boolean pRenderMessagedFiltered, Function<Component, FormattedCharSequence> pMessageTransformer) {
      if (this.renderMessages == null || this.renderMessagedFiltered != pRenderMessagedFiltered) {
         this.renderMessagedFiltered = pRenderMessagedFiltered;
         this.renderMessages = new FormattedCharSequence[4];

         for(int i = 0; i < 4; ++i) {
            this.renderMessages[i] = pMessageTransformer.apply(this.getMessage(i, pRenderMessagedFiltered));
         }
      }

      return this.renderMessages;
   }

   private Component[] getMessages(boolean pFiltered) {
      return pFiltered ? this.filteredMessages : this.messages;
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   /**
    * Get an NBT compound to sync to the client with SPacketChunkData, used for initial loading of the chunk or when
    * many blocks change at once. This compound comes back to you clientside in {@link handleUpdateTag}
    */
   public CompoundTag getUpdateTag() {
      return this.saveWithoutMetadata();
   }

   public boolean onlyOpCanSetNbt() {
      return true;
   }

   public boolean isEditable() {
      return this.isEditable;
   }

   public void setEditable(boolean pIsEditable) {
      this.isEditable = pIsEditable;
      if (!pIsEditable) {
         this.playerWhoMayEdit = null;
      }

   }

   public void setAllowedPlayerEditor(UUID pPlayWhoMayEdit) {
      this.playerWhoMayEdit = pPlayWhoMayEdit;
   }

   @Nullable
   public UUID getPlayerWhoMayEdit() {
      return this.playerWhoMayEdit;
   }

   public boolean executeClickCommands(ServerPlayer pLevel) {
      for(Component component : this.getMessages(pLevel.isTextFilteringEnabled())) {
         Style style = component.getStyle();
         ClickEvent clickevent = style.getClickEvent();
         if (clickevent != null && clickevent.getAction() == ClickEvent.Action.RUN_COMMAND) {
            pLevel.getServer().getCommands().performPrefixedCommand(this.createCommandSourceStack(pLevel), clickevent.getValue());
         }
      }

      return true;
   }

   public CommandSourceStack createCommandSourceStack(@Nullable ServerPlayer pPlayer) {
      String s = pPlayer == null ? "Sign" : pPlayer.getName().getString();
      Component component = (Component)(pPlayer == null ? Component.literal("Sign") : pPlayer.getDisplayName());
      return new CommandSourceStack(CommandSource.NULL, Vec3.atCenterOf(this.worldPosition), Vec2.ZERO, (ServerLevel)this.level, 2, s, component, this.level.getServer(), pPlayer);
   }

   public DyeColor getColor() {
      return this.color;
   }

   public boolean setColor(DyeColor pColor) {
      if (pColor != this.getColor()) {
         this.color = pColor;
         this.markUpdated();
         return true;
      } else {
         return false;
      }
   }

   public boolean hasGlowingText() {
      return this.hasGlowingText;
   }

   public boolean setHasGlowingText(boolean pHasGlowingText) {
      if (this.hasGlowingText != pHasGlowingText) {
         this.hasGlowingText = pHasGlowingText;
         this.markUpdated();
         return true;
      } else {
         return false;
      }
   }

   private void markUpdated() {
      this.setChanged();
      this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
   }
}