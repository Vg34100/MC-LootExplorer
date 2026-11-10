package net.vg.lootexplorer.forge;

import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.vg.lootexplorer.LootExplorer;

@Mod(LootExplorer.MOD_ID)
public final class LootExplorerForge {
    public LootExplorerForge() {
        EventBuses.registerModEventBus(LootExplorer.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        LootExplorer.init();
    }
}
