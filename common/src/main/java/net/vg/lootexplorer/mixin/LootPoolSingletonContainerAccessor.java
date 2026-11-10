package net.vg.lootexplorer.mixin;

//? if >=26.3 {
/*import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
*///? } else {
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
//? }
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

//? if >=26.3 {
/*@Mixin(LootPoolEntryContainer.class)
*///? } else {
@Mixin(LootPoolSingletonContainer.class)
//? }
public interface LootPoolSingletonContainerAccessor {
    //? if >=26.3 {
    /*@Accessor("modifier")
    java.util.Optional<net.minecraft.core.Holder<LootItemFunction>> getModifier();
    default List<LootItemFunction> getFunctions() { return getModifier().map(net.minecraft.core.Holder::value).stream().toList(); }
    *///? } else if <1.21 {
    /*@Accessor("functions")
    LootItemFunction[] getFunctionArray();
    default List<LootItemFunction> getFunctions() { return List.of(getFunctionArray()); }
    *///? } else {
    @Accessor("functions")
    List<LootItemFunction> getFunctions();
    //? }
}
