package net.vg.lootexplorer.inventory;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class LootPreviewScreen extends AbstractContainerScreen<LootPreviewMenu> {
    private static final Identifier CONTAINER_BACKGROUND = Identifier.fromNamespaceAndPath("minecraft", "textures/gui/container/creative_inventory/tab_items.png");
    private static final int ROWS = 7;
    private static final int COLS = 9;
    private float scrollOffs;
    private boolean scrolling;
    private final List<ItemStack> items = new ArrayList<>();
    private boolean canScroll = false;

    public LootPreviewScreen(LootPreviewMenu menu, Inventory inventory, Component title, List<ItemStack> itemList) {
        super(menu, inventory, title, 195, 136 + (18 * 2));
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
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BACKGROUND, i, j, 0f, 0f, this.imageWidth, this.imageHeight, 256, 256);

        if (this.canScroll) {
            int scrollbarPos = (int)(137 * this.scrollOffs);
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BACKGROUND,
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
                    guiGraphics.item(stack, x, y);
                    guiGraphics.itemDecorations(this.font, stack, x, y);
                }
            }
        }

        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        guiGraphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
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
                        guiGraphics.setTooltipForNextFrame(this.font, items.get(index), mouseX, mouseY);
                        return;
                    }
                }
            }
        }

        super.extractTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (this.canScroll && button == 0
                && mouseX >= (double)(this.leftPos + 175) && mouseX < (double)(this.leftPos + 187)
                && mouseY >= (double)this.topPos && mouseY < (double)(this.topPos + this.imageHeight)) {
            this.scrolling = true;
            return true;
        }
        return super.mouseClicked(event, consumed);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        double mouseY = event.y();
        if (this.scrolling && this.canScroll) {
            int top = this.topPos + 18;
            int bottom = top + 112;
            this.scrollOffs = ((float)mouseY - (float)top - 7.5F) / ((float)(bottom - top) - 15.0F);
            this.scrollOffs = Mth.clamp(this.scrollOffs, 0.0F, 1.0F);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
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
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0) {
            this.scrolling = false;
        }
        return super.mouseReleased(event);
    }
}
