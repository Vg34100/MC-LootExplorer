package net.vg.lootexplorer.util;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.SeededContainerLoot;
import net.vg.lootexplorer.Constants;
import net.vg.lootexplorer.inventory.LootPreviewInventory;
import net.vg.lootexplorer.inventory.LootPreviewMenu;
import net.vg.lootexplorer.inventory.LootPreviewScreen;

import java.util.List;

public class ModKeyMaps {

    //? if >=26.1 {
    public static final KeyMapping.Category LOOT_EXPLORER_CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "misc"));
    //? } else {
    /*private static final String LOOT_EXPLORER_CATEGORY = "category.lootexplorer";
    *///? }

    public static final KeyMapping CUSTOM_KEYMAPPING = new KeyMapping(
            "key.copy_loot_table",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_O,
            LOOT_EXPLORER_CATEGORY
    );

    public static final KeyMapping PREVIEW_LOOT_TABLE_KEY = new KeyMapping(
            "key.preview_loot_table",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_INSERT,
            LOOT_EXPLORER_CATEGORY
    );

    public static void register() {
        KeyMappingRegistry.register(CUSTOM_KEYMAPPING);
        KeyMappingRegistry.register(PREVIEW_LOOT_TABLE_KEY);

        ClientTickEvent.CLIENT_POST.register(minecraft -> {
            while (CUSTOM_KEYMAPPING.consumeClick()) {
                handleCopyLootTable(minecraft);
            }
            while (PREVIEW_LOOT_TABLE_KEY.consumeClick()) {
                if (minecraft.player != null && minecraft.player.getMainHandItem() != null) {
                    ItemStack mainHandItem = minecraft.player.getMainHandItem();

                    if (mainHandItem.has(DataComponents.LORE)) {
                        ItemLore loreTag = mainHandItem.get(DataComponents.LORE);
                        if (loreTag != null && !loreTag.lines().isEmpty()) {
                            Component loreText = loreTag.lines().getFirst();
                            String loreString = loreText.getString();

                            loreString = net.minecraft.util.StringUtil.stripColor(loreString);
                            List<ItemStack> items = LootHandler.lootTableItemMap.get(loreString);
                            Constants.LOGGER.info(items.toString());
                            LootPreviewMenu menu = new LootPreviewMenu(0, minecraft.player.getInventory());
                            LootPreviewScreen screen = new LootPreviewScreen(menu, minecraft.player.getInventory(), Component.literal("Loot Preview"), items);
                            //? if >=26.2 {
                            /*minecraft.gui.setScreen(screen);
                            *///? } else {
                            minecraft.setScreen(screen);
                            //? }
                        }
                    }
                }
            }
        });
    }

    private static void handleCopyLootTable(Minecraft minecraft) {
        if (minecraft.player != null && minecraft.player.getMainHandItem() != null) {
            ItemStack mainHandItem = minecraft.player.getMainHandItem();

            if (mainHandItem.has(DataComponents.LORE)) {
                ItemLore loreTag = mainHandItem.get(DataComponents.LORE);
                if (loreTag != null && !loreTag.lines().isEmpty()) {
                    Component loreText = loreTag.lines().getFirst();
                    String loreString = loreText.getString();

                    loreString = net.minecraft.util.StringUtil.stripColor(loreString);
                    minecraft.keyboardHandler.setClipboard(loreString);
                    //? if >=26.1 {
                    minecraft.player.sendOverlayMessage(Component.literal("Copied loot table name to clipboard: " + loreString));
                    //? } else {
                    /*minecraft.player.displayClientMessage(Component.literal("Copied loot table name to clipboard: " + loreString), true);
                    *///? }
                }
            }
        }
    }

    private static void handlePreviewLootTable(Minecraft minecraft) {
        if (minecraft.player != null && minecraft.player.getMainHandItem() != null) {
            Constants.LOGGER.info("Attempting Preview Loot Table");
            ItemStack mainHandItem = minecraft.player.getMainHandItem();

            if (mainHandItem.has(DataComponents.CONTAINER_LOOT)) {
                SeededContainerLoot containerLoot = mainHandItem.get(DataComponents.CONTAINER_LOOT);
                if (containerLoot != null) {
                    LootPreviewInventory.open(minecraft.player, containerLoot.lootTable());
                } else {
                    //? if >=26.1 {
                    minecraft.player.sendOverlayMessage(Component.literal("No loot table found for this item."));
                    //? } else {
                    /*minecraft.player.displayClientMessage(Component.literal("No loot table found for this item."), true);
                    *///? }
                }
            } else {
                //? if >=26.1 {
                minecraft.player.sendOverlayMessage(Component.literal("This item doesn't have a loot table."));
                //? } else {
                /*minecraft.player.displayClientMessage(Component.literal("This item doesn't have a loot table."), true);
                *///? }
            }
        }
    }
}
