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
    private static final int ROWS_PER_PAGE = 4;
    private static final Component TITLE = Component.translatable("lootexplorer.config.title");
    private static final Component RESTART_NOTE = Component.translatable("lootexplorer.config.restart_note");

    private final Screen parent;
    private final PathPanel containers = new PathPanel(Component.literal("Container Loot Tables"));
    private final PathPanel brushables = new PathPanel(Component.literal("Brushable Loot Tables"));

    public LootExplorerConfigScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
        containers.paths.addAll(LootExplorerConfig.getContainerPaths());
        brushables.paths.addAll(LootExplorerConfig.getBrushablePaths());
    }

    @Override
    protected void init() {
        containers.collect();
        brushables.collect();
        rebuild();
    }

    private void rebuild() {
        clearWidgets();

        int gap = 8;
        int panelWidth = Math.min(210, (this.width - 32 - gap) / 2);
        int totalWidth = panelWidth * 2 + gap;
        int left = (this.width - totalWidth) / 2;
        addPanel(containers, left, panelWidth);
        addPanel(brushables, left + panelWidth + gap, panelWidth);

        int buttonY = this.height - 28;
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> saveAndClose())
                .bounds(this.width / 2 - 102, buttonY, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> onClose())
                .bounds(this.width / 2 + 2, buttonY, 100, 20).build());
    }

    private void addPanel(PathPanel panel, int left, int width) {
        panel.fields.clear();
        int pageCount = panel.pageCount();
        panel.page = Math.clamp(panel.page, 0, pageCount - 1);
        int start = panel.page * ROWS_PER_PAGE;
        int end = Math.min(panel.paths.size(), start + ROWS_PER_PAGE);
        int rowWidth = width - 24;

        for (int index = start; index < end; index++) {
            int rowY = 74 + (index - start) * 22;
            EditBox field = new EditBox(this.font, left, rowY, rowWidth, 20,
                    Component.translatable("lootexplorer.config.path"));
            field.setMaxLength(128);
            field.setValue(panel.paths.get(index));
            panel.fields.add(field);
            addRenderableWidget(field);

            int pathIndex = index;
            addRenderableWidget(Button.builder(Component.literal("-"), button -> {
                        panel.collect();
                        panel.paths.remove(pathIndex);
                        rebuild();
                    }).bounds(left + rowWidth + 4, rowY, 20, 20).build());
        }

        int controlsY = 166;
        Button previous = addRenderableWidget(Button.builder(Component.literal("<"), button -> {
                    panel.collect();
                    panel.page--;
                    rebuild();
                }).bounds(left, controlsY, 20, 20).build());
        previous.active = panel.page > 0;
        addRenderableWidget(Button.builder(Component.translatable("lootexplorer.config.add_path"), button -> {
                    panel.collect();
                    panel.paths.add("");
                    panel.page = (panel.paths.size() - 1) / ROWS_PER_PAGE;
                    rebuild();
                }).bounds(left + 24, controlsY, width - 48, 20).build());
        Button next = addRenderableWidget(Button.builder(Component.literal(">"), button -> {
                    panel.collect();
                    panel.page++;
                    rebuild();
                }).bounds(left + width - 20, controlsY, 20, 20).build());
        next.active = panel.page < pageCount - 1;
    }

    private void saveAndClose() {
        containers.collect();
        brushables.collect();
        LootExplorerConfig.savePaths(containers.paths, brushables.paths);
        onClose();
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        extractMenuBackground(guiGraphics);
        int gap = 8;
        int panelWidth = Math.min(210, (this.width - 32 - gap) / 2);
        int panelLeft = (this.width - (panelWidth * 2 + gap)) / 2;
        guiGraphics.centeredText(this.font, TITLE, this.width / 2, 16, 0xFFFFFF);
        guiGraphics.centeredText(this.font, RESTART_NOTE, this.width / 2, 34, 0xAAAAAA);
        guiGraphics.centeredText(this.font, containers.title, panelLeft + panelWidth / 2, 56, 0xFFFFFF);
        guiGraphics.centeredText(this.font, brushables.title, panelLeft + panelWidth + gap + panelWidth / 2, 56, 0xFFFFFF);
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
    }

    private static final class PathPanel {
        private final Component title;
        private final List<String> paths = new ArrayList<>();
        private final List<EditBox> fields = new ArrayList<>();
        private int page;

        private PathPanel(Component title) {
            this.title = title;
        }

        private int pageCount() {
            return Math.max(1, (paths.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);
        }

        private void collect() {
            int start = page * ROWS_PER_PAGE;
            for (int index = 0; index < fields.size(); index++) {
                paths.set(start + index, fields.get(index).getValue());
            }
        }
    }
}
