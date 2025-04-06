package net.vg.lootexplorer.neoforge;

import net.vg.lootexplorer.LootExplorer;
import net.neoforged.fml.common.Mod;

@Mod(LootExplorer.MOD_ID)
public final class LootExplorerNeoForge {
    public LootExplorerNeoForge() {
        // Run our common setup.
        LootExplorer.init();
    }
}
