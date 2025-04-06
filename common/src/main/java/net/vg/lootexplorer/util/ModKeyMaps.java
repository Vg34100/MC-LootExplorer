package net.vg.lootexplorer.util;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.SeededContainerLoot;
import net.vg.lootexplorer.Constants;
import net.vg.lootexplorer.inventory.LootPreviewInventory;
import net.vg.lootexplorer.inventory.LootPreviewMenu;
import net.vg.lootexplorer.inventory.LootPreviewScreen;

import java.util.List;

public class ModKeyMaps {

    public static final KeyMapping CUSTOM_KEYMAPPING = new KeyMapping(
            "key.copy_loot_table", // The translation key of the name shown in the Controls screen
            InputConstants.Type.KEYSYM, // This key mapping is for Keyboards by default
            InputConstants.KEY_O, // The default keycode
            "category.lootexplorer" // The category translation key used to categorize in the Controls screen
    );

    public static final KeyMapping PREVIEW_LOOT_TABLE_KEY = new KeyMapping(
            "key.preview_loot_table",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_INSERT,
            "category.lootexplorer"
    );

    public static void register() {
        KeyMappingRegistry.register(CUSTOM_KEYMAPPING);
        KeyMappingRegistry.register(PREVIEW_LOOT_TABLE_KEY);




        ClientTickEvent.CLIENT_POST.register(minecraft -> {
            while (CUSTOM_KEYMAPPING.consumeClick()) {
                handleCopyLootTable(minecraft);
            }
            while (PREVIEW_LOOT_TABLE_KEY.consumeClick()) {
//                handlePreviewLootTable(minecraft);
                if (minecraft.player != null && minecraft.player.getMainHandItem() != null) {
                    ItemStack mainHandItem = minecraft.player.getMainHandItem();

                    // Check if the item has lore
                    if (mainHandItem.has(DataComponents.LORE)) {
                        ItemLore loreTag = mainHandItem.get(DataComponents.LORE);
                        if (loreTag != null && !loreTag.lines().isEmpty()) {
                            // Get the first line of the lore
                            Component loreText = loreTag.lines().getFirst();
                            String loreString = loreText.getString();

                            // Remove any formatting codes
                            loreString = net.minecraft.util.StringUtil.stripColor(loreString);
                            List<ItemStack> items = LootHandler.lootTableItemMap.get(loreString);
                            Constants.LOGGER.info(items.toString());
                            LootPreviewMenu menu = new LootPreviewMenu(0, minecraft.player.getInventory());
                            LootPreviewScreen screen = new LootPreviewScreen(menu, minecraft.player.getInventory(), Component.literal("Loot Preview"), items);
                            minecraft.setScreen(screen);
                        }
                    }

                 }
            }
        });
    }

    /**
     * Copies the lore of the player's currently held item.
     */
    private static void handleCopyLootTable(Minecraft minecraft) {
        if (minecraft.player != null && minecraft.player.getMainHandItem() != null) {
            // Get the item in the player's main hand
            ItemStack mainHandItem = minecraft.player.getMainHandItem();

            // Check if the item has lore
            if (mainHandItem.has(DataComponents.LORE)) {
                ItemLore loreTag = mainHandItem.get(DataComponents.LORE);
                if (loreTag != null && !loreTag.lines().isEmpty()) {
                    // Get the first line of the lore
                    Component loreText = loreTag.lines().getFirst();
                    String loreString = loreText.getString();

                    // Remove any formatting codes
                    loreString = net.minecraft.util.StringUtil.stripColor(loreString);

                    // Copy the lore text to the clipboard
                    minecraft.keyboardHandler.setClipboard(loreString);

                    // Send a message to the player
                    minecraft.player.displayClientMessage(Component.literal("Copied loot table name to clipboard: " + loreString), true);
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
                    minecraft.player.displayClientMessage(Component.literal("No loot table found for this item."), true);
                }
            } else {
                minecraft.player.displayClientMessage(Component.literal("This item doesn't have a loot table."), true);
            }
        }
    }
}
