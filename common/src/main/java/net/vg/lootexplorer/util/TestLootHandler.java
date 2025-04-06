package net.vg.lootexplorer.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.entries.CompositeEntryBase;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.entries.TagEntry;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;
import net.vg.lootexplorer.mixin.*;

import java.util.List;
import java.util.Set;

public class TestLootHandler {

    public static void resolveEntry(LootPoolEntryContainer entry, Set<ItemLike> items) {
        if (entry instanceof LootItem) {
            // resolveItem
            ItemLike item = resolveItem(((LootItemAccessor)entry).getItem().value(), ((LootPoolSingletonContainerAccessor)entry).getFunctions());
            if (item != null) {
                items.add(item);
            }
        }
//        else if (entry instanceof NestedLootTable) {
//            ((NestedLootTableMixin) entry).getContents().map({
//               key ->
//            });
//
//        }
        else if (entry instanceof TagEntry) {
            BuiltInRegistries.ITEM.getTagOrEmpty(((TagEntryMixin)entry).getTag()).forEach(
                    item -> resolveItem(item.value(), ((LootPoolSingletonContainerAccessor)entry).getFunctions())
            );
        } else if (entry instanceof CompositeEntryBase) {
            ((CompositeEntryBaseMixin)entry).getChildren().forEach( child -> resolveEntry(child, items));
        }
    }

    public static Item resolveItem(Item item, List<LootItemFunction> functions) {
        if (item == Items.AIR)
            return null;
        ItemStack itemStack = new ItemStack(item);

        functions.forEach(
                function -> {
                    if (function instanceof SetComponentsFunction) {
                        itemStack.applyComponents(((SetComponentsFunctionMixin)function).getComponents());
                    }
//                    else if (function instanceof EnchantRandomlyFunction) {
//                        ((EnchantRandomlyFunctionMixin)function).getOptions().ifPresentOrElse();
//                    } else if (function instanceof EnchantWithLevelsFunction) {
////                        function.levels;
////                        function.treasure;
//                    }

                }
        );

        return itemStack.getItem();
    }
}
