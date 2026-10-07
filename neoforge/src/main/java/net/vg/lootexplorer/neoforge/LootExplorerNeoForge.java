package net.vg.lootexplorer.neoforge;

import net.vg.lootexplorer.LootExplorer;
import net.vg.lootexplorer.client.LootExplorerConfigScreen;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = LootExplorer.MOD_ID, dist = Dist.CLIENT)
public final class LootExplorerNeoForge {
    public LootExplorerNeoForge() {
        // Run our common setup.
        LootExplorer.init();

        ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory.class,
                () -> (container, parent) -> new LootExplorerConfigScreen(parent));
    }
}
