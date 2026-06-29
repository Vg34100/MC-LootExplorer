package net.vg.lootexplorer.util;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.vg.lootexplorer.Constants;

import java.util.ArrayList;
import java.util.List;

public class LootGenerator {

    /**
     * Generate loot items from a loot table using a similar approach to the /loot command.
     *
     * @param player The player requesting the loot preview
     * @param lootTableLocation The location of the loot table
     * @param samples Number of samples to generate from the loot table
     * @return A list of generated items from the loot table
     */
    public static List<ItemStack> generateLootItems(Player player, String lootTableLocation, int samples) {
        Constants.LOGGER.info("Generating loot items from table: {}", lootTableLocation);
        List<ItemStack> allItems = new ArrayList<>();

        try {
            // Parse the loot table location
            ResourceLocation resourceLocation = ResourceLocation.parse(lootTableLocation);
            ResourceKey<LootTable> lootTableKey = ResourceKey.create(Registries.LOOT_TABLE, resourceLocation);

            // Get the ServerLevel
            ServerLevel serverLevel = player.getServer().overworld();

            // Get the LootTable
//            Holder<LootTable> lootTableHolder = player.getServer().reloadableRegistries().getLootTable(lootTableKey);
//            LootTable lootTable = lootTableHolder.value();
            LootTable lootTable = player.getServer().reloadableRegistries().getLootTable(lootTableKey);
            // Generate several samples of the loot table
            for (int i = 0; i < samples; i++) {
                // Create a loot context with appropriate parameters
                LootParams lootParams = createLootParams(player, serverLevel);

                // Get random items from the loot table using a different seed each time
                List<ItemStack> lootItems = lootTable.getRandomItems(lootParams, i + 1).stream()
                        .map(ItemStack::copy)
                        .toList();

                allItems.addAll(lootItems);
            }

            Constants.LOGGER.info("Generated {} items from loot table", allItems.size());
        } catch (Exception e) {
            Constants.LOGGER.error("Error generating loot items from table {}: {}", lootTableLocation, e.getMessage());
        }

        return allItems;
    }

    /**
     * Create appropriate LootParams for the loot table based on the context
     */
    private static LootParams createLootParams(Player player, ServerLevel serverLevel) {
        // This is similar to how the /loot command creates parameters
        LootParams.Builder builder = new LootParams.Builder(serverLevel)
                .withParameter(LootContextParams.ORIGIN, player.position())
                .withLuck(player.getLuck());

        // Create loot parameters with the chest loot context set
        return builder.create(LootContextParamSets.CHEST);
    }

    /**
     * Different method to handle loot tables that might require different parameters
     */
    public static List<ItemStack> generateSpecificLoot(Player player, String lootTableLocation, int samples, String type) {
        ResourceLocation resourceLocation = ResourceLocation.parse(lootTableLocation);
        ResourceKey<LootTable> lootTableKey = ResourceKey.create(Registries.LOOT_TABLE, resourceLocation);
        ServerLevel serverLevel = player.getServer().overworld();
//        Holder<LootTable> lootTableHolder = player.getServer().reloadableRegistries().getLootTable(lootTableKey);
//        LootTable lootTable = lootTableHolder.value();
        LootTable lootTable = player.getServer().reloadableRegistries().getLootTable(lootTableKey);

        List<ItemStack> allItems = new ArrayList<>();

        for (int i = 0; i < samples; i++) {
            LootParams lootParams;

            switch (type) {
                case "fishing":
                    lootParams = new LootParams.Builder(serverLevel)
                            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(player.blockPosition()))
                            .withParameter(LootContextParams.TOOL, player.getMainHandItem())
                            .withOptionalParameter(LootContextParams.THIS_ENTITY, player)
                            .create(LootContextParamSets.FISHING);
                    break;
                case "entity":
                    lootParams = new LootParams.Builder(serverLevel)
                            .withParameter(LootContextParams.ORIGIN, player.position())
                            .withParameter(LootContextParams.DAMAGE_SOURCE, player.damageSources().generic())
                            .withOptionalParameter(LootContextParams.THIS_ENTITY, player)
                            .create(LootContextParamSets.ENTITY);
                    break;
                case "block":
                    lootParams = new LootParams.Builder(serverLevel)
                            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(player.blockPosition()))
                            .withParameter(LootContextParams.TOOL, player.getMainHandItem())
                            .create(LootContextParamSets.BLOCK);
                    break;
                default:
                    lootParams = new LootParams.Builder(serverLevel)
                            .withParameter(LootContextParams.ORIGIN, player.position())
                            .create(LootContextParamSets.CHEST);
            }

            List<ItemStack> lootItems = lootTable.getRandomItems(lootParams, i + 1).stream()
                    .map(ItemStack::copy)
                    .toList();

            allItems.addAll(lootItems);
        }

        return allItems;
    }
}