package net.vg.lootexplorer.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.architectury.platform.Platform;
import net.vg.lootexplorer.Constants;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class LootExplorerConfig {
    public static final String FILE_NAME = "lootexplorer.json";
    private static final String LEGACY_PATHS_KEY = "includedLootTablePaths";
    private static final String CONTAINER_PATHS_KEY = "containerLootTablePaths";
    private static final String BRUSHABLE_PATHS_KEY = "brushableLootTablePaths";
    private static final List<String> DEFAULT_CONTAINER_PATHS = List.of("chests/", "loot/");
    private static final List<String> DEFAULT_BRUSHABLE_PATHS = List.of("archaeology/");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static List<String> containerLootTablePaths = DEFAULT_CONTAINER_PATHS;
    private static List<String> brushableLootTablePaths = DEFAULT_BRUSHABLE_PATHS;

    private LootExplorerConfig() {}

    public static void load() {
        Path path = configPath();
        if (!Files.exists(path)) {
            savePaths(DEFAULT_CONTAINER_PATHS, DEFAULT_BRUSHABLE_PATHS);
            containerLootTablePaths = DEFAULT_CONTAINER_PATHS;
            brushableLootTablePaths = DEFAULT_BRUSHABLE_PATHS;
            return;
        }

        try {
            JsonObject root = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), JsonObject.class);
            if (root == null) {
                throw new IllegalArgumentException("Missing configuration root");
            }
            if (root.has(LEGACY_PATHS_KEY)) {
                List<String> legacyPaths = readPaths(root.getAsJsonArray(LEGACY_PATHS_KEY));
                containerLootTablePaths = normalize(legacyPaths.stream()
                        .filter(legacyPath -> !legacyPath.equals("archaeology/"))
                        .toList());
                brushableLootTablePaths = normalize(legacyPaths.stream()
                        .filter(legacyPath -> legacyPath.equals("archaeology/"))
                        .toList());
                savePaths(containerLootTablePaths, brushableLootTablePaths);
                return;
            }
            containerLootTablePaths = normalize(readPaths(root.getAsJsonArray(CONTAINER_PATHS_KEY)));
            brushableLootTablePaths = normalize(readPaths(root.getAsJsonArray(BRUSHABLE_PATHS_KEY)));
        } catch (Exception e) {
            Constants.LOGGER.warn("Could not read {}. Using default loot-table paths.", path, e);
            containerLootTablePaths = DEFAULT_CONTAINER_PATHS;
            brushableLootTablePaths = DEFAULT_BRUSHABLE_PATHS;
        }
    }

    public static List<String> getContainerPaths() {
        return List.copyOf(containerLootTablePaths);
    }

    public static List<String> getBrushablePaths() {
        return List.copyOf(brushableLootTablePaths);
    }

    public static boolean includesContainer(String resourcePath) {
        return includes(resourcePath, containerLootTablePaths);
    }

    public static boolean includesBrushable(String resourcePath) {
        return includes(resourcePath, brushableLootTablePaths);
    }

    private static boolean includes(String resourcePath, List<String> paths) {
        if (!resourcePath.startsWith("loot_table/")) {
            return false;
        }
        String relativePath = resourcePath.substring("loot_table/".length());
        return paths.stream().anyMatch(relativePath::startsWith);
    }

    public static void savePaths(List<String> containerPaths, List<String> brushablePaths) {
        List<String> normalizedContainerPaths = normalize(containerPaths);
        List<String> normalizedBrushablePaths = normalize(brushablePaths);
        JsonObject root = new JsonObject();
        root.add(CONTAINER_PATHS_KEY, toJsonArray(normalizedContainerPaths));
        root.add(BRUSHABLE_PATHS_KEY, toJsonArray(normalizedBrushablePaths));

        try {
            Path path = configPath();
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(root) + System.lineSeparator(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Constants.LOGGER.error("Could not save LootExplorer configuration", e);
        }
    }

    private static List<String> readPaths(JsonArray paths) {
        if (paths == null) {
            return List.of();
        }
        List<String> loadedPaths = new ArrayList<>();
        for (JsonElement entry : paths) {
            if (entry.isJsonPrimitive() && entry.getAsJsonPrimitive().isString()) {
                loadedPaths.add(entry.getAsString());
            }
        }
        return loadedPaths;
    }

    private static JsonArray toJsonArray(List<String> paths) {
        JsonArray array = new JsonArray();
        paths.forEach(array::add);
        return array;
    }

    private static List<String> normalize(List<String> paths) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String path : paths) {
            if (path == null) {
                continue;
            }
            String value = path.trim().replace('\\', '/');
            while (value.startsWith("loot_table/")) {
                value = value.substring("loot_table/".length());
            }
            while (value.startsWith("/")) {
                value = value.substring(1);
            }
            if (!value.isBlank()) {
                normalized.add(value.endsWith("/") ? value : value + "/");
            }
        }
        return List.copyOf(normalized);
    }

    private static Path configPath() {
        return Platform.getConfigFolder().resolve(FILE_NAME);
    }
}
