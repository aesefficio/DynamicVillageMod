package net.minecraft.client.gui.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CreateFlatWorldScreen extends Screen {
   private static final int SLOT_TEX_SIZE = 128;
   private static final int SLOT_BG_SIZE = 18;
   private static final int SLOT_STAT_HEIGHT = 20;
   private static final int SLOT_BG_X = 1;
   private static final int SLOT_BG_Y = 1;
   private static final int SLOT_FG_X = 2;
   private static final int SLOT_FG_Y = 2;
   protected final CreateWorldScreen parent;
   private final Consumer<FlatLevelGeneratorSettings> applySettings;
   FlatLevelGeneratorSettings generator;
   /** The text used to identify the material for a layer */
   private Component columnType;
   /** The text used to identify the height of a layer */
   private Component columnHeight;
   private CreateFlatWorldScreen.DetailsList list;
   /** The remove layer button */
   private Button deleteLayerButton;

   public CreateFlatWorldScreen(CreateWorldScreen pParent, Consumer<FlatLevelGeneratorSettings> pApplySettings, FlatLevelGeneratorSettings pGenerator) {
      super(Component.translatable("createWorld.customize.flat.title"));
      this.parent = pParent;
      this.applySettings = pApplySettings;
      this.generator = pGenerator;
   }

   public FlatLevelGeneratorSettings settings() {
      return this.generator;
   }

   public void setConfig(FlatLevelGeneratorSettings pGenerator) {
      this.generator = pGenerator;
   }

   protected void init() {
      this.columnType = Component.translatable("createWorld.customize.flat.tile");
      this.columnHeight = Component.translatable("createWorld.customize.flat.height");
      this.list = new CreateFlatWorldScreen.DetailsList();
      this.addWidget(this.list);
      this.deleteLayerButton = this.addRenderableWidget(new Button(this.width / 2 - 155, this.height - 52, 150, 20, Component.translatable("createWorld.customize.flat.removeLayer"), (p_95845_) -> {
         if (this.hasValidSelection()) {
            List<FlatLayerInfo> list = this.generator.getLayersInfo();
            int i = this.list.children().indexOf(this.list.getSelected());
            int j = list.size() - i - 1;
            list.remove(j);
            this.list.setSelected(list.isEmpty() ? null : this.list.children().get(Math.min(i, list.size() - 1)));
            this.generator.updateLayers();
            this.list.resetRows();
            this.updateButtonValidity();
         }
      }));
      this.addRenderableWidget(new Button(this.width / 2 + 5, this.height - 52, 150, 20, Component.translatable("createWorld.customize.presets"), (p_95843_) -> {
         this.minecraft.setScreen(new PresetFlatWorldScreen(this));
         this.generator.updateLayers();
         this.updateButtonValidity();
      }));
      this.addRenderableWidget(new Button(this.width / 2 - 155, this.height - 28, 150, 20, CommonComponents.GUI_DONE, (p_95839_) -> {
         this.applySettings.accept(this.generator);
         this.minecraft.setScreen(this.parent);
         this.generator.updateLayers();
      }));
      this.addRenderableWidget(new Button(this.width / 2 + 5, this.height - 28, 150, 20, CommonComponents.GUI_CANCEL, (p_95833_) -> {
         this.minecraft.setScreen(this.parent);
         this.generator.updateLayers();
      }));
      this.generator.updateLayers();
      this.updateButtonValidity();
   }

   /**
    * Would update whether or not the edit and remove buttons are enabled, but is currently disabled and always disables
    * the buttons (which are invisible anyways)
    */
   void updateButtonValidity() {
      this.deleteLayerButton.active = this.hasValidSelection();
   }

   /**
    * Returns whether there is a valid layer selection
    */
   private boolean hasValidSelection() {
      return this.list.getSelected() != null;
   }

   public void onClose() {
      this.minecraft.setScreen(this.parent);
   }

   public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
      this.renderBackground(pPoseStack);
      this.list.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
      drawCenteredString(pPoseStack, this.font, this.title, this.width / 2, 8, 16777215);
      int i = this.width / 2 - 92 - 16;
      drawString(pPoseStack, this.font, this.columnType, i, 32, 16777215);
      drawString(pPoseStack, this.font, this.columnHeight, i + 2 + 213 - this.font.width(this.columnHeight), 32, 16777215);
      super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
   }

   @OnlyIn(Dist.CLIENT)
   class DetailsList extends ObjectSelectionList<CreateFlatWorldScreen.DetailsList.Entry> {
      public DetailsList() {
         super(CreateFlatWorldScreen.this.minecraft, CreateFlatWorldScreen.this.width, CreateFlatWorldScreen.this.height, 43, CreateFlatWorldScreen.this.height - 60, 24);

         for(int i = 0; i < CreateFlatWorldScreen.this.generator.getLayersInfo().size(); ++i) {
            this.addEntry(new CreateFlatWorldScreen.DetailsList.Entry());
         }

      }

      public void setSelected(@Nullable CreateFlatWorldScreen.DetailsList.Entry pEntry) {
         super.setSelected(pEntry);
         CreateFlatWorldScreen.this.updateButtonValidity();
      }

      protected boolean isFocused() {
         return CreateFlatWorldScreen.this.getFocused() == this;
      }

      protected int getScrollbarPosition() {
         return this.width - 70;
      }

      public void resetRows() {
         int i = this.children().indexOf(this.getSelected());
         this.clearEntries();

         for(int j = 0; j < CreateFlatWorldScreen.this.generator.getLayersInfo().size(); ++j) {
            this.addEntry(new CreateFlatWorldScreen.DetailsList.Entry());
         }

         List<CreateFlatWorldScreen.DetailsList.Entry> list = this.children();
         if (i >= 0 && i < list.size()) {
            this.setSelected(list.get(i));
         }

      }

      @OnlyIn(Dist.CLIENT)
      class Entry extends ObjectSelectionList.Entry<CreateFlatWorldScreen.DetailsList.Entry> {
         public void render(PoseStack pPoseStack, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pIsMouseOver, float pPartialTick) {
            FlatLayerInfo flatlayerinfo = CreateFlatWorldScreen.this.generator.getLayersInfo().get(CreateFlatWorldScreen.this.generator.getLayersInfo().size() - pIndex - 1);
            BlockState blockstate = flatlayerinfo.getBlockState();
            ItemStack itemstack = this.getDisplayItem(blockstate);
            this.blitSlot(pPoseStack, pLeft, pTop, itemstack);
            CreateFlatWorldScreen.this.font.draw(pPoseStack, itemstack.getHoverName(), (float)(pLeft + 18 + 5), (float)(pTop + 3), 16777215);
            Component component;
            if (pIndex == 0) {
               component = Component.translatable("createWorld.customize.flat.layer.top", flatlayerinfo.getHeight());
            } else if (pIndex == CreateFlatWorldScreen.this.generator.getLayersInfo().size() - 1) {
               component = Component.translatable("createWorld.customize.flat.layer.bottom", flatlayerinfo.getHeight());
            } else {
               component = Component.translatable("createWorld.customize.flat.layer", flatlayerinfo.getHeight());
            }

            CreateFlatWorldScreen.this.font.draw(pPoseStack, component, (float)(pLeft + 2 + 213 - CreateFlatWorldScreen.this.font.width(component)), (float)(pTop + 3), 16777215);
         }

         private ItemStack getDisplayItem(BlockState pState) {
            Item item = pState.getBlock().asItem();
            if (item == Items.AIR) {
               if (pState.is(Blocks.WATER)) {
                  item = Items.WATER_BUCKET;
               } else if (pState.is(Blocks.LAVA)) {
                  item = Items.LAVA_BUCKET;
               }
            }

            return new ItemStack(item);
         }

         public Component getNarration() {
            FlatLayerInfo flatlayerinfo = CreateFlatWorldScreen.this.generator.getLayersInfo().get(CreateFlatWorldScreen.this.generator.getLayersInfo().size() - DetailsList.this.children().indexOf(this) - 1);
            ItemStack itemstack = this.getDisplayItem(flatlayerinfo.getBlockState());
            return (Component)(!itemstack.isEmpty() ? Component.translatable("narrator.select", itemstack.getHoverName()) : CommonComponents.EMPTY);
         }

         public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
            if (pButton == 0) {
               DetailsList.this.setSelected(this);
               return true;
            } else {
               return false;
            }
         }

         private void blitSlot(PoseStack pPoseStack, int pX, int pY, ItemStack pStack) {
            this.blitSlotBg(pPoseStack, pX + 1, pY + 1);
            if (!pStack.isEmpty()) {
               CreateFlatWorldScreen.this.itemRenderer.renderGuiItem(pStack, pX + 2, pY + 2);
            }

         }

         private void blitSlotBg(PoseStack pPoseStack, int pX, int pY) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShaderTexture(0, GuiComponent.STATS_ICON_LOCATION);
            GuiComponent.blit(pPoseStack, pX, pY, CreateFlatWorldScreen.this.getBlitOffset(), 0.0F, 0.0F, 18, 18, 128, 128);
         }
      }
   }
}