package net.vg.lootexplorer.client;

import dev.architectury.networking.NetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.vg.lootexplorer.inventory.LootPreviewMenu;
import net.vg.lootexplorer.inventory.LootPreviewScreen;
import net.vg.lootexplorer.networking.LootPreviewResponsePayload;
import net.vg.lootexplorer.util.ModKeyMaps;

import java.util.List;

/** Physical-client registration; common/server bootstrap never loads this class. */
public final class ClientNetworking {
    private ClientNetworking() {}

    public static void init() {
        ModKeyMaps.register();
        //? if >=1.21 {
        NetworkManager.registerReceiver(NetworkManager.s2c(), LootPreviewResponsePayload.TYPE,
                LootPreviewResponsePayload.CODEC, (payload, context) ->
                        context.queue(() -> displayLootPreview(payload.lootTablePath(), payload.items())));
        //? } else {
        /*NetworkManager.registerReceiver(NetworkManager.s2c(), LootPreviewResponsePayload.ID, (buffer, context) -> {
            LootPreviewResponsePayload payload = LootPreviewResponsePayload.read(buffer);
            context.queue(() -> displayLootPreview(payload.lootTablePath(), payload.items()));
        });
        *///? }
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
            //? if >=26.2 {
            /*minecraft.gui.setScreen(screen);
            *///? } else {
            minecraft.setScreen(screen);
            //? }
        }
    }
}
