package net.vg.lootexplorer.neoforge.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.vg.lootexplorer.LootExplorer;
import net.vg.lootexplorer.client.ClientNetworking;
import net.vg.lootexplorer.client.LootExplorerConfigScreen;

@Mod(value = LootExplorer.MOD_ID, dist = Dist.CLIENT)
public final class LootExplorerNeoForgeClient {
    public LootExplorerNeoForgeClient() {
        ClientNetworking.init();
        ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory.class,
                () -> (container, parent) -> new LootExplorerConfigScreen(parent));
    }
}
