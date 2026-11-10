package net.vg.lootexplorer.validation;

public final class PortCheckMod implements net.fabricmc.api.ModInitializer {
    @Override public void onInitialize() { PortChecks.register(); }
}
