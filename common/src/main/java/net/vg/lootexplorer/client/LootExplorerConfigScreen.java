package net.vg.lootexplorer.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.vg.lootexplorer.config.LootExplorerConfig;

import java.util.ArrayList;
import java.util.List;

public final class LootExplorerConfigScreen extends Screen {
    private static final int ROWS_PER_PAGE = 6;
    private static final Component TITLE = Component.translatable("lootexplorer.config.title");
    private static final Component RESTART_NOTE = Component.translatable("lootexplorer.config.restart_note");

    private final Screen parent;
    private final List<String> paths = new ArrayList<>();
    private final List<EditBox> visiblePathFields = new ArrayList<>();
    private int page;

    public LootExplorerConfigScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
        this.paths.addAll(LootExplorerConfig.getConfiguredPaths());
    }

    @Override
    protected void init() {
        rebuildPage();
    }

    private void rebuildPage() {
        collectVisiblePaths();
        clearWidgets();
        visiblePathFields.clear();

        int pageCount = Math.max(1, (paths.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);
        page = Math.clamp(page, 0, pageCount - 1);
        int start = page * ROWS_PER_PAGE;
        int end = Math.min(paths.size(), start + ROWS_PER_PAGE);
        int width = Math.min(360, this.width - 40);
        int left = (this.width - width) / 2;

        for (int index = start; index < end; index++) {
            int rowY = 62 + (index - start) * 26;
            EditBox field = new EditBox(this.font, left, rowY, width - 34, 20,
                    Component.translatable("lootexplorer.config.path"));
            field.setMaxLength(128);
            field.setValue(paths.get(index));
            visiblePathFields.add(field);
            addRenderableWidget(field);

            int pathIndex = index;
            addRenderableWidget(Button.builder(Component.literal("-"), button -> removePath(pathIndex))
                    .bounds(left + width - 28, rowY, 28, 20)
                    .build());
        }

        int controlsY = 62 + ROWS_PER_PAGE * 26 + 8;
        Button previous = addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> {
                    collectVisiblePaths();
                    page--;
                    rebuildPage();
                })
                .bounds(left, controlsY, 80, 20)
                .build());
        previous.active = page > 0;

        addRenderableWidget(Button.builder(Component.translatable("lootexplorer.config.add_path"), button -> {
                    collectVisiblePaths();
                    paths.add("");
                    page = (paths.size() - 1) / ROWS_PER_PAGE;
                    rebuildPage();
                })
                .bounds(left + 90, controlsY, width - 180, 20)
                .build());

        Button next = addRenderableWidget(Button.builder(Component.translatable("gui.proceed"), button -> {
                    collectVisiblePaths();
                    page++;
                    rebuildPage();
                })
                .bounds(left + width - 80, controlsY, 80, 20)
                .build());
        next.active = page < pageCount - 1;

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> saveAndClose())
                .bounds(this.width / 2 - 102, this.height - 28, 100, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> onClose())
                .bounds(this.width / 2 + 2, this.height - 28, 100, 20)
                .build());
    }

    private void removePath(int index) {
        collectVisiblePaths();
        paths.remove(index);
        rebuildPage();
    }

    private void collectVisiblePaths() {
        int start = page * ROWS_PER_PAGE;
        for (int index = 0; index < visiblePathFields.size(); index++) {
            paths.set(start + index, visiblePathFields.get(index).getValue());
        }
    }

    private void saveAndClose() {
        collectVisiblePaths();
        LootExplorerConfig.savePaths(paths);
        onClose();
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        extractMenuBackground(guiGraphics);
        guiGraphics.centeredText(this.font, TITLE, this.width / 2, 20, 0xFFFFFF);
        guiGraphics.centeredText(this.font, RESTART_NOTE, this.width / 2, 40, 0xAAAAAA);
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
    }
}
