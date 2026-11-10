package net.vg.lootexplorer.util;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.loot.LootTable;

/** Vanilla loot manager before 1.21; reloadable loot registry afterward. */
public final class LootTables {
    private LootTables() {}
    public static LootTable get(MinecraftServer server, Identifier id) {
        //? if >=1.21 {
        return server.reloadableRegistries().getLootTable(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.LOOT_TABLE, id));
        //? } else {
        /*return server.getLootData().getLootTable(id);
        *///? }
    }
}
