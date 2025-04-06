package net.vg.lootexplorer.mixin;

import net.minecraft.core.HolderSet;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.functions.EnchantWithLevelsFunction;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Optional;

@Mixin(EnchantWithLevelsFunction.class)
public interface EnchantWIthLevelsAccessor {
    @Accessor("levels")
    NumberProvider getLevels();

    @Accessor("options")
    Optional<HolderSet<Enchantment>> getOptions();
}
