package net.vg.lootexplorer.inventory;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class LootPreviewScreen extends AbstractContainerScreen<LootPreviewMenu> {
    private static final ResourceLocation CONTAINER_BACKGROUND = ResourceLocation.withDefaultNamespace("textures/gui/container/creative_inventory/tab_items.png");
    private static final int ROWS = 7; // Increased from 5 to 7 to make it taller
    private static final int COLS = 9;
    private float scrollOffs;
    private boolean scrolling;
    private final List<ItemStack> items = new ArrayList<>();
    private boolean canScroll = false;


    public LootPreviewScreen(LootPreviewMenu menu, Inventory inventory, Component title, List<ItemStack> itemList) {
        super(menu, inventory, title);
        this.imageWidth = 195;
        this.imageHeight = 136 + (18 * 2); // Increased height to accommodate 7 rows
        this.inventoryLabelY = this.imageHeight - 94;

        this.items.addAll(itemList);
    }


    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        updateCanScroll();
    }


    private void updateCanScroll() {
        this.canScroll = this.items.size() > ROWS * COLS;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBg(guiGraphics, partialTick, mouseX, mouseY);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
//        RenderSystem.setShader(GameRenderer::getPositionTexShader);
//        RenderSystem.setShader(CoreShaders.RENDERTYPE_GUI);
//        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
//        RenderSystem.setShaderTexture(0, CONTAINER_BACKGROUND);
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
//        guiGraphics.blit(CONTAINER_BACKGROUND, i, j, 0, 0, this.imageWidth, this.imageHeight);
        guiGraphics.blit(RenderType::guiTextured, CONTAINER_BACKGROUND, i, j, 0f, 0f, this.imageWidth, this.imageHeight, 256, 256);

        if (this.canScroll) {
            int scrollbarPos = (int)(137 * this.scrollOffs); // Adjusted for 7 rows
//            guiGraphics.blit(CONTAINER_BACKGROUND, i + 175, j + 18 + scrollbarPos, 232, 0, 12, 15);
            guiGraphics.blit(RenderType::guiTextured, CONTAINER_BACKGROUND,
                    i + 175, j + 18 + scrollbarPos,  // destination x,y
                    232f, 0f,                        // source u,v
                    12, 15,                          // width, height
                    256, 256);                       // texture dimensions
        }

        int itemIndex = this.canScroll ? (int)(this.scrollOffs * (float)(Math.max(0, items.size() - (ROWS * COLS)) / COLS) + 0.5F) * COLS : 0;
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int index = itemIndex + row * COLS + col;
                if (index < items.size()) {
                    ItemStack stack = items.get(index);
                    int x = i + 8 + col * 18;
                    int y = j + 18 + row * 18;
                    guiGraphics.renderItem(stack, x, y);
                    guiGraphics.renderItemDecorations(this.font, stack, x, y);
                }
            }
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderTooltip(guiGraphics, mouseX, mouseY);

        if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            guiGraphics.renderTooltip(this.font, this.hoveredSlot.getItem(), mouseX, mouseY);
        } else {
            int i = this.leftPos;
            int j = this.topPos;
            int itemIndex = this.canScroll ? (int)(this.scrollOffs * (float)(Math.max(0, items.size() - (ROWS * COLS)) / COLS) + 0.5F) * COLS : 0;

            for (int row = 0; row < ROWS; row++) {
                for (int col = 0; col < COLS; col++) {
                    int index = itemIndex + row * COLS + col;
                    if (index < items.size()) {
                        int x = i + 8 + col * 18;
                        int y = j + 18 + row * 18;
                        if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                            guiGraphics.renderTooltip(this.font, items.get(index), mouseX, mouseY);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        // Removed the inventory label rendering
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.canScroll && button == 0 && mouseX >= (double)(this.leftPos + 175) && mouseX < (double)(this.leftPos + 187) &&
                mouseY >= (double)this.topPos && mouseY < (double)(this.topPos + this.imageHeight)) {
            this.scrolling = true;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.scrolling && this.canScroll) {
            int i = this.topPos + 18;
            int j = i + 112;
            this.scrollOffs = ((float)mouseY - (float)i - 7.5F) / ((float)(j - i) - 15.0F);
            this.scrollOffs = Mth.clamp(this.scrollOffs, 0.0F, 1.0F);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double d, double e, double f, double g) {
        if (this.canScroll) {
            float a = (float)g / (float)(items.size() / COLS - ROWS);
            this.scrollOffs = Mth.clamp(this.scrollOffs - a, 0.0F, 1.0F);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.scrolling = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
}