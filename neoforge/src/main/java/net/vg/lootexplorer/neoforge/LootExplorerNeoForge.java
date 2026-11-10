package net.vg.lootexplorer.neoforge;

import net.neoforged.fml.common.Mod;
import net.vg.lootexplorer.LootExplorer;

@Mod(LootExplorer.MOD_ID)
public final class LootExplorerNeoForge {
    public LootExplorerNeoForge() {
        LootExplorer.init();
    }
}
