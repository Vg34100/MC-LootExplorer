package net.vg.lootexplorer.util;

import com.google.gson.*;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import dev.architectury.event.events.common.LifecycleEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
//? if >=1.21 {
import net.minecraft.world.item.enchantment.ItemEnchantments;
//? }
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.vg.lootexplorer.Constants;
import net.vg.lootexplorer.config.LootExplorerConfig;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.*;
import java.util.function.Predicate;

public class LootHandler extends SimplePreparableReloadListener<Void> {
    //? if >=1.21 {
    private static final String LOOT_DIRECTORY = "loot_table";
    //? } else {
    /*private static final String LOOT_DIRECTORY = "loot_tables";
    *///? }
    public record BrushableEntry(Item item, BlockEntityType<?> blockEntityType) {}

    public static List<String> tables = new ArrayList<>();
    private static final Set<String> containerTables = new HashSet<>();
    private static final Set<String> brushableTables = new HashSet<>();
//    public static List<ItemStack> itemList = new ArrayList<>();
    public static List<ItemStack> containerList = new ArrayList<>();
    public static List<ItemStack> brushableList = new ArrayList<>();
    public static final List<Item> additionalContainerItems = new ArrayList<>();
    public static final List<BrushableEntry> additionalBrushableItems = new ArrayList<>();




    public static Map<String, List<ItemStack>> lootTableItemMap = new HashMap<>();
    private static MinecraftServer server;

    public static void register() {
        Constants.LOGGER.debug("Registering LootHandler");
        LifecycleEvent.SERVER_STARTED.register(minecraftServer  -> {
            server = minecraftServer;
            LootExplorerConfig.load();
            ModCompat.register();
            Constants.LOGGER.debug("Server started, applying LootHandler");
            new LootHandler().apply(null, server.getResourceManager(), null);
        });
    }

    public static void registerContainer(Item item) {
        if (!additionalContainerItems.contains(item)) {
            additionalContainerItems.add(item);
        }
    }

    public static void registerBrushable(Item item, BlockEntityType<?> blockEntityType) {
        BrushableEntry entry = new BrushableEntry(item, blockEntityType);
        if (!additionalBrushableItems.contains(entry)) {
            additionalBrushableItems.add(entry);
        }
    }

    public static boolean isGeneratedPreview(ItemStack stack) {
        return ItemPreviewData.isPreview(stack);
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
        containerTables.clear();
        brushableTables.clear();
        lootTableItemMap.clear();

        Predicate<Identifier> filter = id -> LootExplorerConfig.includesContainer(id.getPath())
                || LootExplorerConfig.includesBrushable(id.getPath());
//        Predicate<ResourceLocation> filter = id -> id.getPath().startsWith("loot_table/chests/ancient_city");
        Constants.LOGGER.info("Filtering loot tables using container paths {} and brushable paths {}",
                LootExplorerConfig.getContainerPaths(), LootExplorerConfig.getBrushablePaths());

        Map<Identifier, Resource> resources = resourceManager.listResources(LOOT_DIRECTORY, filter::test);
        Constants.LOGGER.info("Found {} resources matching the filter", resources.size());


        resources.forEach((id, resource) -> {
            Constants.LOGGER.info("Processing loot table: {}, {}", id, resource);
            parseLootTable(id, resource);

            try (InputStreamReader reader = new InputStreamReader(resource.open())) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                List<ItemStack> itemList = processLootTable(json, id);

                String tableName = id.toString().replace(".json", "").replace(LOOT_DIRECTORY + "/", "");
                lootTableItemMap.put(tableName, itemList);
                if (LootExplorerConfig.includesContainer(id.getPath())) {
                    containerTables.add(id.toString());
                }
                if (LootExplorerConfig.includesBrushable(id.getPath())) {
                    brushableTables.add(id.toString());
                }
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

    private List<ItemStack> processLootTable(JsonObject lootTableJson, Identifier tableId) {
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

    private void processEntry(JsonObject entry, List<ItemStack> items, Identifier tableId) {
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

    private void processItemEntry(JsonObject entry, List<ItemStack> items, Identifier tableId) {
        if (!entry.has("name")) {
            Constants.LOGGER.warn("Item entry missing 'name' in loot table: {}", tableId);
            return;
        }

        String itemName = entry.get("name").getAsString();
        Item item = BuiltInRegistries.ITEM.getOptional(ModIds.parse(itemName))
                .orElse(null);
        if (item != null) {
            ItemStack stack = new ItemStack(item);
            List<Component> lore = new ArrayList<>();
            // 26.3 moved modifier lists/objects to a structured field; both
            // representations retain the same preview interpretation.
            boolean isEnchanted = processModifiers(stack, entry.get("functions"), lore, tableId);
            isEnchanted |= processModifiers(stack, entry.get("modifier"), lore, tableId);

            if (!lore.isEmpty()) {
                ItemPreviewData.lore(stack, lore);
            }

            if (isEnchanted) {
                ItemPreviewData.glint(stack);
                Component nameComponent = Component.literal("Enchanted " + stack.getHoverName().getString())
                        .withStyle(stack.is(Items.BOOK) ? ChatFormatting.YELLOW : ChatFormatting.AQUA);
                ItemPreviewData.name(stack, nameComponent);
            }

            items.add(stack);
            Constants.LOGGER.debug("Added item {} to loot table: {}", item.getDescriptionId(), tableId);
        } else {
            Constants.LOGGER.warn("Unknown item in loot table {}: {}", tableId, itemName);
        }
    }

    private boolean processModifiers(ItemStack stack, JsonElement modifiers, List<Component> lore, Identifier tableId) {
        if (modifiers == null) return false;
        if (modifiers.isJsonObject()) return processFunctionForItem(stack, modifiers.getAsJsonObject(), lore, tableId);
        boolean enchanted = false;
        if (modifiers.isJsonArray()) {
            for (JsonElement modifier : modifiers.getAsJsonArray()) {
                enchanted |= processModifiers(stack, modifier, lore, tableId);
            }
        }
        return enchanted;
    }

    private boolean processFunctionForItem(ItemStack stack, JsonObject function, List<Component> lore, Identifier tableId) {
        String typeKey = function.has("function") ? "function" : "type";
        if (!function.has(typeKey)) return false;

        String functionType = function.get(typeKey).getAsString();
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

                Identifier functionId = ModIds.parse(functionType);
                boolean functionExists = BuiltInRegistries.LOOT_FUNCTION_TYPE.getOptional(functionId).isPresent();
                if (!functionExists) {
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

    public static ItemStack enchantBook(ItemStack eBook, Level level, Identifier resourceLocation, int enchantLevel) {
        System.out.println("enchantBook called with eBook: " + eBook + ", resourceLocation: " + resourceLocation + ", enchantLevel: " + enchantLevel);

        //? if >=1.21 {
        RegistryAccess registryAccess = level.registryAccess();
//        Optional<Holder.Reference<Enchantment>> reference = registryAccess.registryOrThrow(Registries.ENCHANTMENT).getHolder(resourceLocation);
        //? if >=26.1 {
        Optional<Holder.Reference<Enchantment>> reference = registryAccess.lookupOrThrow(Registries.ENCHANTMENT).get(resourceLocation);
        //? } else {
        /*Optional<Holder.Reference<Enchantment>> reference = registryAccess.registryOrThrow(Registries.ENCHANTMENT).getHolder(resourceLocation);
        *///? }



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

        //? } else {
        /*Enchantment enchantment = BuiltInRegistries.ENCHANTMENT.get(resourceLocation);
        if (enchantment != null) {
            if (eBook.is(Items.ENCHANTED_BOOK)) {
                net.minecraft.world.item.EnchantedBookItem.addEnchantment(eBook,
                        new net.minecraft.world.item.enchantment.EnchantmentInstance(enchantment, enchantLevel));
            } else { eBook.enchant(enchantment, enchantLevel); }
        }
        *///? }

        return eBook;
    }




    private void processTagEntry(JsonObject entry, List<ItemStack> items, Identifier tableId) {
        if (!entry.has("name")) {
            Constants.LOGGER.warn("Tag entry missing 'name' in loot table: {}", tableId);
            return;
        }

        String tagName = entry.get("name").getAsString();
        if (tagName.startsWith("#")) {
            tagName = tagName.substring(1); // Remove the leading '#'
        }
        TagKey<Item> tagKey = TagKey.create(Registries.ITEM, ModIds.parse(tagName));
        Constants.LOGGER.debug("Tag entry found in {}: {}", tableId, tagName);
        // Here you would ideally get all items from the tag and add them to the item list
        // For simplicity, we're just logging it for now
    }

    public static LootTable deserializeLootTable(Resource resource) throws IOException {
        InputStream inputStream = resource.open();
        JsonObject json = JsonParser.parseReader(new InputStreamReader(inputStream)).getAsJsonObject();

        //? if >=1.21 {
        // Parse the JSON into a DataResult containing a Holder of LootTable
        DataResult<Holder<LootTable>> result = LootTable.CODEC.parse(JsonOps.INSTANCE, json);

        // Handle the DataResult
        return result.resultOrPartial(Constants.LOGGER::error)
                .map(Holder::value) // Extract the LootTable from the Holder
                .orElse(LootTable.EMPTY); // Default to an empty loot table if parsing fails
        //? } else {
        /*return net.minecraft.world.level.storage.loot.Deserializers.createLootTableSerializer().create().fromJson(json, LootTable.class);
        *///? }

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
            if (containerTables.contains(table)) {
                for (ItemStack chestItem : List.of(new ItemStack(Items.CHEST), new ItemStack(Items.BARREL), new ItemStack(Items.TRAPPED_CHEST))) {
                    createChestItem(table, chestItem, chestCounter);
                }
                for (Item container : additionalContainerItems) {
                    createChestItem(table, new ItemStack(container), chestCounter);
                }
                chestCounter++;
            }
            if (brushableTables.contains(table)) {
                for (Item archaeology : List.of(Items.SUSPICIOUS_GRAVEL, Items.SUSPICIOUS_SAND)) {
                    //? if >=26.2 {
                    /*createArchaeologyItem(table, archaeology, net.minecraft.world.level.block.entity.BlockEntityTypes.BRUSHABLE_BLOCK, archaeologyCounter);
                    *///? } else {
                    createArchaeologyItem(table, archaeology, BlockEntityType.BRUSHABLE_BLOCK, archaeologyCounter);
                    //? }
                }
                for (BrushableEntry archaeology : additionalBrushableItems) {
                    createArchaeologyItem(table, archaeology.item(), archaeology.blockEntityType(), archaeologyCounter);
                }
                archaeologyCounter++;
            }
        }
    }

    private static void createChestItem(String table, ItemStack chestItem, int counter) {
        try {
            String tableName = table.replace(".json", "").replace(LOOT_DIRECTORY + "/", "");
            //
            Identifier lootTableId = ModIds.parse(tableName);
            ItemPreviewData.containerLoot(chestItem, lootTableId);
            markGeneratedPreview(chestItem);


            // Determine the color of the lore based on the table name
            Component loreText;
            if (table.contains("minecraft:")) {
                loreText = Component.literal(tableName); // Default
            } else {
                loreText = Component.literal(tableName).setStyle(Style.EMPTY.withColor(0x55FF55)); // Green color
            }

            // Set the lore component
            ItemPreviewData.lore(chestItem, List.of(loreText));

            // Set the custom name for the chest item
//            String translationKey = chestItem.getDescriptionId();
            //? if >=26.1 {
            String translationKey = chestItem.getItemName().getString();
            //? } else {
            /*String translationKey = chestItem.getHoverName().getString();
            *///? }
            Component itemTypeName = Component.translatable(translationKey);
            Component customName = Component.literal(itemTypeName.getString() + " (#" + String.format("%04d", counter) + ")");
            ItemPreviewData.name(chestItem, customName);

            // Add the chest stack to the item list
            containerList.add(chestItem);
        } catch (Exception e) {
            Constants.LOGGER.error("Error building chest for loot table {}", table, e);
        }
    }

    private static void createArchaeologyItem(String table, Item itemType, BlockEntityType<?> blockEntityType, int counter) {
        try {
            Constants.LOGGER.debug("Creating archaeology item for table: {}", table);

            ItemStack archaeologyItem = new ItemStack(itemType);
            String tableName = table.replace(".json", "").replace(LOOT_DIRECTORY + "/", "");

            Identifier lootTableId = ModIds.parse(tableName);
            Constants.LOGGER.debug("Loot table identifier: {}", lootTableId);

            ItemPreviewData.brushableLoot(archaeologyItem, blockEntityType, lootTableId);
            markGeneratedPreview(archaeologyItem);




            Component loreText;
            if (table.contains("archaeology") && table.contains("minecraft:")) {
                loreText = Component.literal(tableName).setStyle(Style.EMPTY.withColor(Color.CYAN.getRGB()));
            } else if (table.contains("minecraft:")){
                loreText = Component.literal(tableName);
            } else {
                loreText = Component.literal(tableName).setStyle(Style.EMPTY.withColor(0x55FF55)); // Green color
            }

            ItemPreviewData.lore(archaeologyItem, List.of(loreText));

            // Set custom name
//            String translationKey = archaeologyItem.getDescriptionId();
            //? if >=26.1 {
            String translationKey = archaeologyItem.getItemName().getString();
            //? } else {
            /*String translationKey = archaeologyItem.getHoverName().getString();
            *///? }
            Component itemTypeName = Component.translatable(translationKey);
            Component customName = Component.literal(itemTypeName.getString() + " (#" + String.format("%04d", counter) + ")");
            ItemPreviewData.name(archaeologyItem, customName);

            // Add the archaeology item to the item list
            brushableList.add(archaeologyItem);
        } catch (Exception e) {
            Constants.LOGGER.error("Error building archaeology item for loot table {}", table, e);
        }
    }

    private static final Gson GSON = new Gson();

    private static void markGeneratedPreview(ItemStack stack) {
        ItemPreviewData.mark(stack);
    }

    public static void parseLootTable(Identifier id, Resource resource) {
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
