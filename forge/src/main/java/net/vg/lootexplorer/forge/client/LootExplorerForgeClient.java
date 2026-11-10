package net.vg.lootexplorer.forge.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.vg.lootexplorer.LootExplorer;
import net.vg.lootexplorer.client.ClientNetworking;
import net.vg.lootexplorer.client.LootExplorerConfigScreen;

@Mod.EventBusSubscriber(modid = LootExplorer.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class LootExplorerForgeClient {
    // Forge initializes this CLIENT-filtered subscriber during mod construction,
    // before Architectury's own mod bus receives RegisterKeyMappingsEvent.
    static { ClientNetworking.init(); }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new LootExplorerConfigScreen(parent)));

    }
}
