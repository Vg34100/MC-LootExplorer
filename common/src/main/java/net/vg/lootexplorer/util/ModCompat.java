package net.vg.lootexplorer.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModCompat {
    private static final String[] IRON_CHESTS = {
            "copper_chest", "iron_chest", "gold_chest", "diamond_chest", "emerald_chest",
            "crystal_chest", "obsidian_chest", "netherite_chest", "christmas_chest"
    };
    private static final String[] EXTRA_CHESTS_WOODS = {
            "acacia", "bamboo", "birch", "cherry", "crimson", "dark_oak", "jungle", "pale_oak", "spruce", "warped"
    };
    private static final String[] BETTER_ARCHEOLOGY_BRUSHABLES = {
            "suspicious_dirt", "suspicious_red_sand"
    };

    private ModCompat() {}

    static void register() {
        registerIronChests();
        registerExtraChests();
        registerBetterArcheology();
    }

    private static void registerIronChests() {
        for (String chest : IRON_CHESTS) {
            findItem("ironchest", chest).ifPresent(LootHandler::registerContainer);
        }
    }

    private static void registerExtraChests() {
        for (String wood : EXTRA_CHESTS_WOODS) {
            findItem("extrachests", wood + "_chest").ifPresent(LootHandler::registerContainer);
            findItem("extrachests", wood + "_trapped_chest").ifPresent(LootHandler::registerContainer);
        }
    }

    private static void registerBetterArcheology() {
        BuiltInRegistries.BLOCK_ENTITY_TYPE.getOptional(id("betterarcheology", "sus_block"))
                .ifPresent(blockEntityType -> {
                    for (String brushable : BETTER_ARCHEOLOGY_BRUSHABLES) {
                        findItem("betterarcheology", brushable)
                                .ifPresent(item -> LootHandler.registerBrushable(item, blockEntityType));
                    }
                });
    }

    private static java.util.Optional<Item> findItem(String namespace, String path) {
        return BuiltInRegistries.ITEM.getOptional(id(namespace, path));
    }

    private static Identifier id(String namespace, String path) {
        return Identifier.fromNamespaceAndPath(namespace, path);
    }
}
