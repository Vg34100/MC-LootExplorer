package net.vg.lootexplorer.util;

import net.minecraft.resources.Identifier;
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
            Identifier resourceLocation = ModIds.parse(lootTableLocation);


            // Get the ServerLevel
            ServerLevel serverLevel = (ServerLevel) player.level();

            // Get the LootTable
//            Holder<LootTable> lootTableHolder = player.getServer().reloadableRegistries().getLootTable(lootTableKey);
//            LootTable lootTable = lootTableHolder.value();
            LootTable lootTable = LootTables.get(player.level().getServer(), resourceLocation);
            // Generate several samples of the loot table
            for (int i = 0; i < samples; i++) {
                // Create a loot context with appropriate parameters
                LootParams lootParams = createLootParams(player, serverLevel, lootTable);

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
    public static LootParams createLootParams(Player player, ServerLevel serverLevel, LootTable table) {
        // This is similar to how the /loot command creates parameters
        LootParams.Builder builder = new LootParams.Builder(serverLevel)
                .withParameter(LootContextParams.ORIGIN, player.position())
                .withLuck(player.getLuck());

        if (table.getParamSet() == LootContextParamSets.ARCHAEOLOGY) {
            builder.withParameter(LootContextParams.THIS_ENTITY, player);
            //? if >=26.1 {
            /*builder.withParameter(LootContextParams.TOOL, player.getMainHandItem());
            *///? }
            return builder.create(LootContextParamSets.ARCHAEOLOGY);
        }
        return builder.create(LootContextParamSets.CHEST);
    }

    /**
     * Different method to handle loot tables that might require different parameters
     */
    public static List<ItemStack> generateSpecificLoot(Player player, String lootTableLocation, int samples, String type) {
        Identifier resourceLocation = ModIds.parse(lootTableLocation);

        ServerLevel serverLevel = (ServerLevel) player.level();
//        Holder<LootTable> lootTableHolder = LootTables.get(player.level().getServer(), resourceLocation);
//        LootTable lootTable = lootTableHolder.value();
        LootTable lootTable = LootTables.get(player.level().getServer(), resourceLocation);

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