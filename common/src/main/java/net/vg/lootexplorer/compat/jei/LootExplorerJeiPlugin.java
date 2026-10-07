package net.vg.lootexplorer.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.vg.lootexplorer.LootExplorer;
import net.vg.lootexplorer.util.LootHandler;

import java.util.List;

@JeiPlugin
public final class LootExplorerJeiPlugin implements IModPlugin {
    private static final Identifier PLUGIN_ID = Identifier.fromNamespaceAndPath(LootExplorer.MOD_ID, "jei_plugin");

    @Override
    public Identifier getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        //? if >=26.1 {
        List<ItemStack> previews = runtime.getIngredientManager().getAllItemStacks().stream()
        //? } else {
        /*List<ItemStack> previews = runtime.getIngredientManager().getAllIngredients(VanillaTypes.ITEM_STACK).stream()
        *///? }
                .filter(LootHandler::isGeneratedPreview)
                .toList();
        if (!previews.isEmpty()) {
            runtime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, previews);
        }
    }
}
