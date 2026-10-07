package net.vg.lootexplorer.inventory;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

// Legacy uses immediate rendering and positional input; layout and scroll behavior match the canonical screen.
public class LootPreviewScreen extends AbstractContainerScreen<LootPreviewMenu> {
    private static final ResourceLocation CONTAINER_BACKGROUND = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/creative_inventory/tab_items.png");
    private static final int ROWS = 7;
    private static final int COLS = 9;
    private float scrollOffs;
    private boolean scrolling;
    private final List<ItemStack> items = new ArrayList<>();
    private boolean canScroll = false;

    public LootPreviewScreen(LootPreviewMenu menu, Inventory inventory, Component title, List<ItemStack> itemList) {
        super(menu, inventory, title);
        this.imageWidth = 195;
        this.imageHeight = 136 + (18 * 2);
        this.inventoryLabelY = this.imageHeight - 94;
        if (itemList != null) {
            this.items.addAll(itemList);
        }
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        this.canScroll = this.items.size() > ROWS * COLS;
    }

    private int getScrollItemIndex() {
        if (!canScroll) return 0;
        int maxRows = Math.max(0, (int) Math.ceil((double) items.size() / COLS) - ROWS);
        return (int) (this.scrollOffs * maxRows + 0.5F) * COLS;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        guiGraphics.blit(CONTAINER_BACKGROUND, i, j, 0f, 0f, this.imageWidth, this.imageHeight, 256, 256);

        if (this.canScroll) {
            int scrollbarPos = (int)(137 * this.scrollOffs);
            guiGraphics.blit(CONTAINER_BACKGROUND,
                    i + 175, j + 18 + scrollbarPos,
                    232f, 0f, 12, 15, 256, 256);
        }

        int itemIndex = getScrollItemIndex();
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
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int i = this.leftPos;
        int j = this.topPos;
        int itemIndex = getScrollItemIndex();

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int index = itemIndex + row * COLS + col;
                if (index < items.size()) {
                    int x = i + 8 + col * 18;
                    int y = j + 18 + row * 18;
                    if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                        guiGraphics.renderTooltip(this.font, items.get(index), mouseX, mouseY);
                        return;
                    }
                }
            }
        }

        super.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.canScroll && button == 0
                && mouseX >= (double)(this.leftPos + 175) && mouseX < (double)(this.leftPos + 187)
                && mouseY >= (double)this.topPos && mouseY < (double)(this.topPos + this.imageHeight)) {
            this.scrolling = true;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.scrolling && this.canScroll) {
            int top = this.topPos + 18;
            int bottom = top + 112;
            this.scrollOffs = ((float)mouseY - (float)top - 7.5F) / ((float)(bottom - top) - 15.0F);
            this.scrollOffs = Mth.clamp(this.scrollOffs, 0.0F, 1.0F);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double d, double e, double f, double g) {
        if (this.canScroll && !items.isEmpty()) {
            int maxRows = Math.max(1, (int) Math.ceil((double) items.size() / COLS) - ROWS);
            float delta = (float)g / (float)maxRows;
            this.scrollOffs = Mth.clamp(this.scrollOffs - delta, 0.0F, 1.0F);
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
