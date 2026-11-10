package net.vg.lootexplorer.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
//? if >=1.21 {
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.SeededContainerLoot;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
//? }

import java.util.List;

/** The preview's data is components on 1.21+, vanilla item NBT on 1.20.1. */
public final class ItemPreviewData {
    private static final String MARKER = "lootexplorer_preview";
    private ItemPreviewData() {}

    public static boolean isPreview(ItemStack stack) {
        //? if >=1.21 {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        //? if >=26.1 {
        return data != null && data.copyTag().getBooleanOr(MARKER, false);
        //? } else {
        /*return data != null && data.copyTag().getBoolean(MARKER);
        *///? }
        //? } else {
        /*return stack.hasTag() && stack.getTag().getBoolean(MARKER);
        *///? }
    }

    public static void mark(ItemStack stack) {
        //? if >=1.21 {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(MARKER, true));
        //? } else {
        /*stack.getOrCreateTag().putBoolean(MARKER, true);
        *///? }
    }

    public static void lore(ItemStack stack, List<Component> lines) {
        //? if >=1.21 {
        stack.set(DataComponents.LORE, new ItemLore(lines));
        //? } else {
        /*var lore = new net.minecraft.nbt.ListTag();
        lines.forEach(line -> lore.add(net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(line))));
        stack.getOrCreateTagElement("display").put("Lore", lore);
        *///? }
    }

    public static List<Component> lore(ItemStack stack) {
        //? if >=1.21 {
        ItemLore lore = stack.get(DataComponents.LORE);
        return lore == null ? List.of() : lore.lines();
        //? } else {
        /*CompoundTag display = stack.getTagElement("display");
        if (display == null) return List.of();
        var result = new java.util.ArrayList<Component>();
        var lines = display.getList("Lore", net.minecraft.nbt.Tag.TAG_STRING);
        for (int i = 0; i < lines.size(); i++) {
            Component line = Component.Serializer.fromJson(lines.getString(i));
            if (line != null) result.add(line);
        }
        return result;
        *///? }
    }

    public static void name(ItemStack stack, Component name) {
        //? if >=1.21 {
        stack.set(DataComponents.ITEM_NAME, name);
        //? } else {
        /*stack.setHoverName(name);
        *///? }
    }

    public static void glint(ItemStack stack) {
        //? if >=1.21 {
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        //? } else {
        /*// A nonempty sentinel gives vanilla glint without applying a real enchantment.
        var glint = new net.minecraft.nbt.ListTag();
        glint.add(new CompoundTag());
        stack.getOrCreateTag().put("Enchantments", glint);
        *///? }
    }

    public static void containerLoot(ItemStack stack, Identifier id) {
        //? if >=1.21 {
        stack.set(DataComponents.CONTAINER_LOOT, new SeededContainerLoot(ResourceKey.create(Registries.LOOT_TABLE, id), 0L));
        //? if >=26.1 {
        var tooltip = stack.getOrDefault(DataComponents.TOOLTIP_DISPLAY, net.minecraft.world.item.component.TooltipDisplay.DEFAULT);
        stack.set(DataComponents.TOOLTIP_DISPLAY, tooltip.withHidden(DataComponents.CONTAINER_LOOT, true));
        //? } else {
        /*stack.set(DataComponents.HIDE_ADDITIONAL_TOOLTIP, net.minecraft.util.Unit.INSTANCE);
        *///? }
        //? } else {
        /*CompoundTag data = stack.getOrCreateTagElement("BlockEntityTag");
        data.putString("LootTable", id.toString());
        data.putLong("LootTableSeed", 0L);
        *///? }
    }

    public static void brushableLoot(ItemStack stack, BlockEntityType<?> type, Identifier id) {
        CompoundTag data = new CompoundTag();
        data.putString("LootTable", id.toString());
        //? if >=26.1 {
        @SuppressWarnings("unchecked")
        var typed = (net.minecraft.world.item.component.TypedEntityData<BlockEntityType<?>>) (Object)
                net.minecraft.world.item.component.TypedEntityData.of(type, data);
        stack.set(DataComponents.BLOCK_ENTITY_DATA, typed);
        //? } else if >=1.21 {
        /*data.putString("id", net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type).toString());
        stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(data));
        *///? } else {
        /*data.putString("id", net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type).toString());
        stack.getOrCreateTag().put("BlockEntityTag", data);
        *///? }
    }
}
