package net.vg.lootexplorer.inventory;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class LootPreviewMenu extends AbstractContainerMenu {
    public LootPreviewMenu(int containerId, Inventory playerInventory) {
        super(null, containerId);
    }


    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int i) {
        return ItemStack.EMPTY;
    }
}
