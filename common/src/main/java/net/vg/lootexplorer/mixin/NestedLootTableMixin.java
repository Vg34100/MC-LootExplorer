package net.vg.lootexplorer.mixin;

import com.mojang.datafixers.util.Either;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;
//? if >=1.21 {
import net.minecraft.world.level.storage.loot.entries.NestedLootTable;
//? } else {
/*import net.minecraft.world.level.storage.loot.entries.LootTableReference;
*///? }
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

//? if >=1.21 {
@Mixin(NestedLootTable.class)
//? } else {
/*@Mixin(LootTableReference.class)
*///? }
public interface NestedLootTableMixin {
    //? if >=26.3 {
    /*@Accessor("value")
    net.minecraft.core.HolderSet<LootTable> getValue();
    *///? } else if >=1.21 {
    @Accessor("contents")
    Either<ResourceKey<LootTable>, LootTable> getContents();
    //? } else {
    /*@Accessor("name")
    net.minecraft.resources.Identifier getName();
    *///? }
}