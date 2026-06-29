package net.vg.lootexplorer.util;

import com.google.gson.*;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import dev.architectury.event.events.common.LifecycleEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.SeededContainerLoot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.vg.lootexplorer.Constants;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.*;
import java.util.function.Predicate;

public class LootHandler extends SimplePreparableReloadListener<Void> {
    public static List<String> tables = new ArrayList<>();
//    public static List<ItemStack> itemList = new ArrayList<>();
    public static List<ItemStack> containerList = new ArrayList<>();
    public static List<ItemStack> brushableList = new ArrayList<>();




    public static Map<String, List<ItemStack>> lootTableItemMap = new HashMap<>();
    private static MinecraftServer server;

    public static void register() {
        Constants.LOGGER.debug("Registering LootHandler");
        LifecycleEvent.SERVER_STARTED.register(minecraftServer  -> {
            server = minecraftServer;
            Constants.LOGGER.debug("Server started, applying LootHandler");
            new LootHandler().apply(null, server.getResourceManager(), null);
        });
    }

    @Override
    protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Constants.LOGGER.debug("Preparing LootHandler");
        return null;
    }

    @Override
    protected void apply(Void object, ResourceManager resourceManager, ProfilerFiller profiler) {
        Constants.LOGGER.debug("Applying LootHandler");
        tables.clear();
        lootTableItemMap.clear();

        Predicate<ResourceLocation> filter = id -> id.getPath().startsWith("loot_table/chests") || id.getPath().startsWith("loot_table/loot") || id.getPath().startsWith("loot_table/archaeology");
//        Predicate<ResourceLocation> filter = id -> id.getPath().startsWith("loot_table/chests/ancient_city");
        Constants.LOGGER.info("Filtering for loot tables in 'loot_table/chests' and 'loot_table/loot'");

        Map<ResourceLocation, Resource> resources = resourceManager.listResources("loot_table", filter);
        Constants.LOGGER.info("Found {} resources matching the filter", resources.size());


        resources.forEach((id, resource) -> {
            Constants.LOGGER.info("Processing loot table: {}, {}", id, resource);
            parseLootTable(id, resource);

            try (InputStreamReader reader = new InputStreamReader(resource.open())) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                List<ItemStack> itemList = processLootTable(json, id);

                String tableName = id.toString().replace(".json", "").replace("loot_table/", "");
                lootTableItemMap.put(tableName, itemList);
                Constants.LOGGER.debug("Mapped loot table {} to {} items", id, itemList.size());

                if (!tables.contains(id.toString())) {
                    tables.add(id.toString());
                    Constants.LOGGER.debug("Added loot table to list: {}", id);
                } else {
                    Constants.LOGGER.debug("Loot table already in list: {}", id);
                }
            } catch (Exception e) {
                Constants.LOGGER.error("Error processing loot table: {}", id, e);
            }
        });

        Constants.LOGGER.debug("Total loot tables found: {}", tables.size());
    }

    private List<ItemStack> processLootTable(JsonObject lootTableJson, ResourceLocation tableId) {
        List<ItemStack> items = new ArrayList<>();
        JsonArray pools = lootTableJson.getAsJsonArray("pools");
        if (pools == null) return items;

        for (JsonElement poolElement : pools) {
            JsonObject pool = poolElement.getAsJsonObject();
            JsonArray entries = pool.getAsJsonArray("entries");
            if (entries == null) continue;

            for (JsonElement entryElement : entries) {
                if (entryElement.isJsonObject()) {
                    JsonObject entry = entryElement.getAsJsonObject();
                    processEntry(entry, items, tableId);
                }
            }
        }
        return items;
    }

    private void processEntry(JsonObject entry, List<ItemStack> items, ResourceLocation tableId) {
        if (!entry.has("type")) return;

        try {
            String type = entry.has("type") ? entry.get("type").getAsString() : "";
            switch (type) {
                case "minecraft:item":
                    processItemEntry(entry, items, tableId);
                    break;
                case "minecraft:tag":
                    processTagEntry(entry, items, tableId);
                    break;
                case "minecraft:loot_table":
//                    Constants.LOGGER.debug("Nested loot table found in {}: {}", tableId, entry.get("value").getAsString());
                    break;
                case "minecraft:group":
                case "minecraft:sequence":
                case "minecraft:alternatives":
                    if (entry.has("children")) {
                        JsonArray children = entry.getAsJsonArray("children");
                        for (JsonElement child : children) {
                            if (child.isJsonObject()) {
                                processEntry(child.getAsJsonObject(), items, tableId);
                            }
                        }
                    }
                    break;
                default:
//                    Constants.LOGGER.warn("Unknown or missing entry type in {}: {}", tableId, type);
            }
        } catch (Exception e) {
            Constants.LOGGER.error("Error processing entry in loot table {}: {}", tableId, e.getMessage());
            // Optionally, you can add more detailed logging here
            // Constants.LOGGER.error("Full stack trace:", e);
        }
    }

    private void processItemEntry(JsonObject entry, List<ItemStack> items, ResourceLocation tableId) {
        if (!entry.has("name")) {
            Constants.LOGGER.warn("Item entry missing 'name' in loot table: {}", tableId);
            return;
        }

        String itemName = entry.get("name").getAsString();
        Item item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(itemName))
                .orElse(null);
        if (item != null) {
            ItemStack stack = new ItemStack(item);
            List<Component> lore = new ArrayList<>();
            boolean isEnchanted = false;

            // Process functions for additional information (e.g., enchantments)
            if (entry.has("functions")) {
                JsonArray functions = entry.getAsJsonArray("functions");
                for (JsonElement functionElement : functions) {
                    if (functionElement.isJsonObject()) {
                        JsonObject function = functionElement.getAsJsonObject();
                        isEnchanted |= processFunctionForItem(stack, function, lore, tableId);
                    }
                }
            }

            if (!lore.isEmpty()) {
                stack.set(DataComponents.LORE, new ItemLore(lore));
            }

            if (isEnchanted) {
                stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
                Component nameComponent = Component.literal("Enchanted " + stack.getHoverName().getString())
                        .withStyle(stack.is(Items.BOOK) ? ChatFormatting.YELLOW : ChatFormatting.AQUA);
                stack.set(DataComponents.ITEM_NAME, nameComponent);
            }

            items.add(stack);
            Constants.LOGGER.debug("Added item {} to loot table: {}", item.getDescriptionId(), tableId);
        } else {
            Constants.LOGGER.warn("Unknown item in loot table {}: {}", tableId, itemName);
        }
    }

    private boolean processFunctionForItem(ItemStack stack, JsonObject function, List<Component> lore, ResourceLocation tableId) {
        if (!function.has("function")) return false;

        String functionType = function.get("function").getAsString();
        Constants.LOGGER.info("Processing function {} for item in table {}", functionType, tableId);
        boolean isEnchanted = false;

        switch (functionType) {
            case "minecraft:set_enchantments":
                isEnchanted = handleSetEnchantments(function, lore);
                break;
            case "minecraft:enchant_with_levels":
            case "minecraft:enchant_randomly":
                isEnchanted = handleEnchantRandomly(function, lore);
                break;
            case "minecraft:set_name":
            case "minecraft:set_components":

                ResourceLocation functionId = ResourceLocation.parse(functionType);
//                LootItemFunctionType<?> a = BuiltInRegistries.LOOT_FUNCTION_TYPE.get(functionId);
                Optional<LootItemFunctionType<?>> functionTypeOptional = BuiltInRegistries.LOOT_FUNCTION_TYPE.getOptional(functionId);

//                if (functionType == null) {
//                    throw new IllegalArgumentException("Unknown loot function type: " + functionId);
//                }
//                stack.applyComponents(((SetComponentsFunctionMixin)a).getComponents());

                if (functionTypeOptional.isEmpty()) {
                    throw new IllegalArgumentException("Unknown loot function type: " + functionId);
                }

//                LootItemFunctionType<?> functionTypetyoe = functionTypeOptional.get();

                break;
            case "minecraft:set_count":
                break;
            // Add other cases as needed
            default:
                Constants.LOGGER.debug("Unhandled function type: {}", functionType);
        }

        return isEnchanted;
    }

    private boolean handleSetEnchantments(JsonObject function, List<Component> lore) {
        if (function.has("enchantments")) {
            JsonObject enchantments = function.getAsJsonObject("enchantments");
            for (Map.Entry<String, JsonElement> entry : enchantments.entrySet()) {
                String enchantmentId = entry.getKey();
                JsonElement levelElement = entry.getValue();
                String levelString = getLevelString(levelElement);
                lore.add(Component.literal(enchantmentId + " " + levelString).withStyle(ChatFormatting.GRAY));
            }
            return true;
        }
        return false;
    }

    private String getLevelString(JsonElement levelElement) {
        if (levelElement.isJsonPrimitive()) {
            return levelElement.getAsString();
        } else if (levelElement.isJsonObject()) {
            JsonObject levelObject = levelElement.getAsJsonObject();
            if (levelObject.has("min") && levelObject.has("max")) {
                JsonElement maxElement = levelObject.get("max");
                if (maxElement.isJsonObject()) {
                    // Handle the case where max is an object with min and max
                    JsonObject maxObject = maxElement.getAsJsonObject();
                    int minLevel = levelObject.get("min").getAsInt();
                    int maxMin = maxObject.get("min").getAsInt();
                    int maxMax = maxObject.get("max").getAsInt();
                    return minLevel + "-" + maxMin + "(" + maxMax + ")";
                } else {
                    // Handle the simple min-max case
                    int min = levelObject.get("min").getAsInt();
                    int max = levelObject.get("max").getAsInt();
                    return min == max ? String.valueOf(min) : min + "-" + max;
                }
            } else if (levelObject.has("min")) {
                return String.valueOf(levelObject.get("min").getAsInt());
            }
        }
        return "Unknown";
    }

    private boolean handleEnchantRandomly(JsonObject function, List<Component> lore) {
        List<String> optionsList = getOptionsList(function);
        for (String option : optionsList) {
            lore.add(Component.literal("Random: " + option).withStyle(ChatFormatting.GRAY));
        }
        return true;
    }

    private List<String> getOptionsList(JsonObject function) {
        List<String> optionsList = new ArrayList<>();

        if (function.has("options")) {
            JsonElement options = function.get("options");
            if (options.isJsonArray()) {
                JsonArray optionsArray = options.getAsJsonArray();
                for (JsonElement element : optionsArray) {
                    optionsList.add(element.getAsString());
                }
            } else {
                optionsList.add(options.getAsString());
            }
        } else if (function.has("enchantments")) {
            JsonElement enchantments = function.get("enchantments");
            if (enchantments.isJsonArray()) {
                JsonArray enchantmentsArray = enchantments.getAsJsonArray();
                for (JsonElement element : enchantmentsArray) {
                    optionsList.add(element.getAsString());
                }
            } else {
                optionsList.add(enchantments.getAsString());
            }
        }

        if (optionsList.isEmpty()) {
            optionsList.add("Unknown options");
        }

        return optionsList;
    }



    private void printEnchantWithLevels(JsonObject function) {
        System.out.println("Enchant with levels:");
        if (function.has("levels")) {
            int level = function.get("levels").getAsInt();
            System.out.println("  Enchantment level: " + level);
        } else {
            System.out.println("  Enchantment level not specified");
        }

        boolean treasure = function.has("treasure") && function.get("treasure").getAsBoolean();
        System.out.println("  Treasure enchantments allowed: " + treasure);

        System.out.println("  Possible enchantments: All enchantments available at the specified level");
    }

    private boolean printEnchantRandomly(JsonObject function) {
        System.out.println("Enchant randomly:");
        if (function.has("enchantments")) {
            JsonArray enchantments = function.getAsJsonArray("enchantments");
            System.out.println("  Specified enchantments:");
            for (JsonElement element : enchantments) {
                String enchantmentId = element.getAsString();
                System.out.println("    - " + enchantmentId);
            }
            return false;
        } else {
            System.out.println("  No specific enchantments specified. Any valid book enchantment is possible.");
            return true;
        }
    }

    public static ItemStack enchantBook(ItemStack eBook, Level level, ResourceLocation resourceLocation, int enchantLevel) {
        System.out.println("enchantBook called with eBook: " + eBook + ", resourceLocation: " + resourceLocation + ", enchantLevel: " + enchantLevel);

        RegistryAccess registryAccess = level.registryAccess();
//        Optional<Holder.Reference<Enchantment>> reference = registryAccess.registryOrThrow(Registries.ENCHANTMENT).getHolder(resourceLocation);
        Optional<Holder.Reference<Enchantment>> reference = registryAccess.lookupOrThrow(Registries.ENCHANTMENT).get(resourceLocation);



        if (reference.isEmpty()) {
            Constants.LOGGER.error("Reference was found Empty");
            return eBook;
        }

        Optional<ResourceKey<Enchantment>> key = reference.get().unwrapKey();

        if (key.isEmpty()) {
            Constants.LOGGER.error("Key was found Empty");
            return eBook;
        }

//        HolderGetter.Provider provider = registryAccess.asGetterLookup();
//        Enchantment enchantment = provider.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key.get()).value();
        Enchantment enchantment = registryAccess.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key.get()).value();


        Holder<Enchantment> entry = Holder.direct(enchantment);

        ItemEnchantments comp = eBook.getEnchantments();

        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(comp);
        mutable.upgrade(entry, enchantLevel);

        EnchantmentHelper.setEnchantments(eBook, mutable.toImmutable());

        return eBook;
    }




    private void processTagEntry(JsonObject entry, List<ItemStack> items, ResourceLocation tableId) {
        if (!entry.has("name")) {
            Constants.LOGGER.warn("Tag entry missing 'name' in loot table: {}", tableId);
            return;
        }

        String tagName = entry.get("name").getAsString();
        if (tagName.startsWith("#")) {
            tagName = tagName.substring(1); // Remove the leading '#'
        }
        TagKey<Item> tagKey = TagKey.create(Registries.ITEM, ResourceLocation.parse(tagName));
        Constants.LOGGER.debug("Tag entry found in {}: {}", tableId, tagName);
        // Here you would ideally get all items from the tag and add them to the item list
        // For simplicity, we're just logging it for now
    }

    public static LootTable deserializeLootTable(Resource resource) throws IOException {
        InputStream inputStream = resource.open();
        JsonObject json = JsonParser.parseReader(new InputStreamReader(inputStream)).getAsJsonObject();

        // Parse the JSON into a DataResult containing a Holder of LootTable
        DataResult<Holder<LootTable>> result = LootTable.CODEC.parse(JsonOps.INSTANCE, json);

        // Handle the DataResult
        return result.resultOrPartial(Constants.LOGGER::error)
                .map(Holder::value) // Extract the LootTable from the Holder
                .orElse(LootTable.EMPTY); // Default to an empty loot table if parsing fails
    }


    public static void buildChests() {
        Constants.LOGGER.debug("Starting buildChests method");
//        itemList.clear();
//        int counter = 1;
        containerList.clear();
        brushableList.clear();
        int chestCounter = 1;
        int archaeologyCounter = 1;


        Constants.LOGGER.debug("Total loot tables to process: {}", tables.size());

        for (final String table : tables) {
            if ((table.contains("chests") || table.contains("loot")) && !table.contains("archaeology")) {
                for (ItemStack chestItem : List.of(new ItemStack(Items.CHEST), new ItemStack(Items.BARREL), new ItemStack(Items.TRAPPED_CHEST))) {
                    createChestItem(table, chestItem, chestCounter);
                }
                for (Item archaeology : List.of(Items.SUSPICIOUS_GRAVEL, Items.SUSPICIOUS_SAND)) {
                    createArchaeologyItem(table, archaeology, archaeologyCounter);
                }
                archaeologyCounter++;
                chestCounter++;
            } else if (table.contains("archaeology")) {
                for (Item archaeology : List.of(Items.SUSPICIOUS_GRAVEL, Items.SUSPICIOUS_SAND)) {
                    createArchaeologyItem(table, archaeology, archaeologyCounter);
                }
                archaeologyCounter++;
            }
        }
    }

    private static void createChestItem(String table, ItemStack chestItem, int counter) {
        try {
            String tableName = table.replace(".json", "").replace("loot_table/", "");
            //
            ResourceLocation lootTableId = ResourceLocation.parse(tableName);
            ResourceKey<LootTable> lootTableKey = ResourceKey.create(ResourceKey.createRegistryKey(ResourceLocation.withDefaultNamespace("loot_table")), lootTableId);

            // Setting the Loot Table within the container
            SeededContainerLoot lootComponent = new SeededContainerLoot(lootTableKey, 0L);
            chestItem.set(DataComponents.CONTAINER_LOOT, lootComponent);


            // Determine the color of the lore based on the table name
            Component loreText;
            if (table.contains("minecraft:")) {
                loreText = Component.literal(tableName); // Default
            } else {
                loreText = Component.literal(tableName).setStyle(Style.EMPTY.withColor(0x55FF55)); // Green color
            }

            // Set the lore component
            ItemLore loreComponent = new ItemLore(List.of(loreText));
            chestItem.set(DataComponents.LORE, loreComponent);

            // Set the custom name for the chest item
//            String translationKey = chestItem.getDescriptionId();
            String translationKey = chestItem.getItemName().getString();
            Component itemTypeName = Component.translatable(translationKey);
            Component customName = Component.literal(itemTypeName.getString() + " (#" + String.format("%04d", counter) + ")");
            chestItem.set(DataComponents.ITEM_NAME, customName);

            // Add the chest stack to the item list
            containerList.add(chestItem);
        } catch (Exception e) {
            Constants.LOGGER.error("Error building chest for loot table {}", table, e);
        }
    }

    private static void createArchaeologyItem(String table, Item itemType, int counter) {
        try {
            Constants.LOGGER.debug("Creating archaeology item for table: {}", table);

            ItemStack archaeologyItem = new ItemStack(itemType);
            String tableName = table.replace(".json", "").replace("loot_table/", "");

            ResourceLocation lootTableId = ResourceLocation.parse(tableName);
            Constants.LOGGER.debug("Loot table identifier: {}", lootTableId);

            ResourceKey<LootTable> lootTableKey = ResourceKey.create(ResourceKey.createRegistryKey(ResourceLocation.withDefaultNamespace("loot_table")), lootTableId);
            Constants.LOGGER.debug("Loot table key: {}", lootTableKey);

            // Create NBT data for the block entity
            CompoundTag blockEntityNbt = new CompoundTag();
            blockEntityNbt.putString("LootTable", lootTableId.toString());
            blockEntityNbt.putString("id", "minecraft:brushable_block");

            // Create NbtComponent and set it to the block entity data
            CustomData nbtComponent = CustomData.of(blockEntityNbt);
            archaeologyItem.set(DataComponents.BLOCK_ENTITY_DATA, nbtComponent);

            Constants.LOGGER.debug("Set NBT data for archaeology item: {}", blockEntityNbt);


            Component loreText;
            if (table.contains("archaeology") && table.contains("minecraft:")) {
                loreText = Component.literal(tableName).setStyle(Style.EMPTY.withColor(Color.CYAN.getRGB()));
            } else if (table.contains("minecraft:")){
                loreText = Component.literal(tableName);
            } else {
                loreText = Component.literal(tableName).setStyle(Style.EMPTY.withColor(0x55FF55)); // Green color
            }

            ItemLore loreComponent = new ItemLore(List.of(loreText));
            archaeologyItem.set(DataComponents.LORE, loreComponent);

            // Set custom name
//            String translationKey = archaeologyItem.getDescriptionId();
            String translationKey = archaeologyItem.getItemName().getString();
            Component itemTypeName = Component.translatable(translationKey);
            Component customName = Component.literal(itemTypeName.getString() + " (#" + String.format("%04d", counter) + ")");
            archaeologyItem.set(DataComponents.ITEM_NAME, customName);

            // Add the archaeology item to the item list
            brushableList.add(archaeologyItem);
        } catch (Exception e) {
            Constants.LOGGER.error("Error building archaeology item for loot table {}", table, e);
        }
    }

    private static final Gson GSON = new Gson();

    public static void parseLootTable(ResourceLocation id, Resource resource) {
//        try (InputStreamReader reader = new InputStreamReader(resource.open())) {
//            JsonElement json = GsonHelper.parse(reader);
////            GSON.fromJson(json, LootTable.class);
////            LootTable lootTable = GSON.fromJson(json, LootTable.class);
//            LootTables.deserialize(json);
//            BuiltInLootTables.
//
//
//            // Access pools using reflection
//            List<LootPool> lootPools = ReflectionUtils.getPrivateFieldValue(LootTable.class, lootTable, "pools");
//
//            // Iterate through pools
//            for (LootPool pool : lootPools) {
//                // Process each pool as needed
//                System.out.println("Processing pool: " + pool);
//            }
//        } catch (IOException e) {
//            Constants.LOGGER.error("Error parsing loot table: " + id, e);
//        }
    }

}