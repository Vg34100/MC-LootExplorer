package net.vg.lootexplorer.inventory;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.vg.lootexplorer.Constants;

import java.util.List;

public class LootPreviewInventory {

    public static void open(Player player, ResourceKey<LootTable> lootTableKey) {
        Constants.LOGGER.info("Attempt Open before serverPlayer");
//        ServerLevel level = serverPlayer.serverLevel();

        Level level = player.level();
        Constants.LOGGER.info("Attempt Open");

//            LootTable lootTable = level.getServer().getLootData().getLootTable(lootTableKey.location());


        LootTable lootTable = player.level().getServer().reloadableRegistries().getLootTable(lootTableKey);



        if (lootTable != null) {
            SimpleContainer container = new SimpleContainer(27);

            LootParams lootParams = new LootParams.Builder(level.getServer().overworld())
                    .withParameter(LootContextParams.ORIGIN, player.position())
                    .withLuck(player.getLuck())
                    .create(lootTable.getParamSet());

            List<ItemStack> loot = lootTable.getRandomItems(lootParams);

            for (int i = 0; i < Math.min(loot.size(), 27); i++) {
                container.setItem(i, loot.get(i));
            }

            MenuProvider provider = new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.literal("Loot Preview");
                }

                @Override
                public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
                    return new ChestMenu(MenuType.GENERIC_9x3, containerId, playerInventory, container, 3);
                }
            };

            player.openMenu(provider);
            player.sendSystemMessage(Component.literal("Previewing loot table contents"));
        } else {
            //? if >=26.1 {
            player.sendOverlayMessage(Component.literal("Failed to load loot table"));
            //? } else {
            /*player.displayClientMessage(Component.literal("Failed to load loot table"), true);
            *///? }
        }
    }

}