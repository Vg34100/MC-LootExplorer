package net.vg.lootexplorer.fabric.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.vg.lootexplorer.client.LootExplorerConfigScreen;

public final class LootExplorerModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return LootExplorerConfigScreen::new;
    }
}
