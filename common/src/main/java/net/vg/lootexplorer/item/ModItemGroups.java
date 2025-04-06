package net.vg.lootexplorer.item;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.vg.lootexplorer.Constants;
import net.vg.lootexplorer.util.LootHandler;

public class ModItemGroups {

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Constants.MOD_ID, Registries.CREATIVE_MODE_TAB);

    public static final RegistrySupplier<CreativeModeTab> CONTAINER_GROUP = TABS.register(
            "container_group",
            () -> CreativeTabRegistry.create(builder -> {
                builder.title(Component.translatable("item_group.container_group"));
                builder.icon(() -> new ItemStack(Items.CHEST));
                builder.displayItems((parameters, output) -> {
                    LootHandler.buildChests();
                    LootHandler.containerList.forEach(output::accept);
                });
            })
    );

    public static final RegistrySupplier<CreativeModeTab> BRUSHABLE_GROUP = TABS.register(
            "brushable_group",
            () -> CreativeTabRegistry.create(builder -> {
                builder.title(Component.translatable("item_group.brushable_group"));
                builder.icon(() -> new ItemStack(Items.SUSPICIOUS_SAND));
                builder.displayItems((parameters, output) -> {
                    LootHandler.buildChests();
                    LootHandler.brushableList.forEach(output::accept);
                });
            })
    );

    public static void register() {
        TABS.register();
    }


}
