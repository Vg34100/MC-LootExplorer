package net.vg.lootexplorer;

import net.vg.lootexplorer.item.ModItemGroups;
import net.vg.lootexplorer.config.LootExplorerConfig;
import net.vg.lootexplorer.util.LootHandler;
import net.vg.lootexplorer.util.NetworkHandler;

public final class LootExplorer {
    public static final String MOD_ID = Constants.MOD_ID;

    public static void init() {
        // Write common init code here.
        Constants.LOGGER.info("Initializing Loot Explorer");
        LootExplorerConfig.load();

        // Register the creative tab
        ModItemGroups.register();



        // Register the LootHandler
        LootHandler.register();
        NetworkHandler.register();

        Constants.LOGGER.info("Loot Explorer initialized");
    }
}
