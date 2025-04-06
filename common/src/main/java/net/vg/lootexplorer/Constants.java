package net.vg.lootexplorer;

import dev.architectury.platform.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Constants {

    public static final String MOD_ID = "lootexplorer";
    public static final String MOD_NAME = "Loot Explorer";
    public static final String MOD_VERSION = fetchModVersion();


    public static final Logger LOGGER = LoggerFactory.getLogger("lootexplorer");


    public static String fetchModVersion() {
        String DEFAULT_VERSION = "1.0.0";

        if (Platform.isModLoaded(MOD_ID)) {
            return Platform.getMod(MOD_ID).getVersion();
        } else {
            return DEFAULT_VERSION;
        }
    }
}