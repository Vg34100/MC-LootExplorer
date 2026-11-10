package net.vg.lootexplorer.fabric.client;

import net.fabricmc.api.ClientModInitializer;

public final class LootExplorerFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        net.vg.lootexplorer.client.ClientNetworking.init();
    }
}
