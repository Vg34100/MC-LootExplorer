package net.vg.lootexplorer;

import net.vg.lootexplorer.item.ModItemGroups;
import net.vg.lootexplorer.util.LootHandler;
import net.vg.lootexplorer.util.ModKeyMaps;

public final class LootExplorer {
    public static final String MOD_ID = "lootexplorer";

    public static void init() {
        // Write common init code here.
        Constants.LOGGER.info("Initializing Loot Explorer");

        // Register the creative tab
        ModItemGroups.register();



        // Register the LootHandler
        LootHandler.register();
        ModKeyMaps.register();

        Constants.LOGGER.info("Loot Explorer initialized");
    }
}
