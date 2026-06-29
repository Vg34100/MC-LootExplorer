package net.vg.lootexplorer.util;

import dev.architectury.networking.NetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.vg.lootexplorer.Constants;
import net.vg.lootexplorer.inventory.LootPreviewMenu;
import net.vg.lootexplorer.inventory.LootPreviewScreen;
import net.vg.lootexplorer.networking.LootPreviewRequestPayload;
import net.vg.lootexplorer.networking.LootPreviewResponsePayload;

import java.util.List;

public class NetworkHandler {

    public static void register() {
        // Register the request handler (client to server)
        NetworkManager.registerReceiver(
                NetworkManager.c2s(),
                LootPreviewRequestPayload.TYPE,
                LootPreviewRequestPayload.CODEC,
                (payload, context) -> {
                    // Handle the request on the server side
                    context.queue(() -> {
                        if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                            handleLootPreviewRequest(payload.lootTablePath(), serverPlayer);
                        }
                    });
                }
        );

        // Register the response handler (server to client)
        NetworkManager.registerReceiver(
                NetworkManager.s2c(),
                LootPreviewResponsePayload.TYPE,
                LootPreviewResponsePayload.CODEC,
                (payload, context) -> {
                    // Handle the response on the client side
                    context.queue(() -> {
                        displayLootPreview(payload.lootTablePath(), payload.items());
                    });
                }
        );
    }

    /**
     * Send a request from the client to the server for loot table preview
     */
    public static void requestLootPreview(String lootTablePath) {
        Constants.LOGGER.info("Requesting loot preview for: {}", lootTablePath);
        LootPreviewRequestPayload payload = new LootPreviewRequestPayload(lootTablePath);
        NetworkManager.sendToServer(payload);
    }

    /**
     * Handle the loot preview request on the server side
     */
    private static void handleLootPreviewRequest(String lootTablePath, ServerPlayer player) {
        Constants.LOGGER.info("Handling loot preview request for: {}", lootTablePath);

        // Generate loot items (5 samples)
        List<ItemStack> lootItems = LootGenerator.generateLootItems(player, lootTablePath, 5);

        // Send the response back to the client
        LootPreviewResponsePayload response = new LootPreviewResponsePayload(lootTablePath, lootItems);
        NetworkManager.sendToPlayer(player, response);
    }

    /**
     * Display the loot preview on the client side
     */
    private static void displayLootPreview(String lootTablePath, List<ItemStack> items) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            LootPreviewMenu menu = new LootPreviewMenu(0, minecraft.player.getInventory());
            LootPreviewScreen screen = new LootPreviewScreen(
                    menu,
                    minecraft.player.getInventory(),
                    Component.literal("Loot Table: " + lootTablePath),
                    items
            );
            minecraft.setScreen(screen);
        }
    }
}