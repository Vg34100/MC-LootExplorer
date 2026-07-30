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
    private static final String PATHS_KEY = "includedLootTablePaths";
    private static final List<String> DEFAULT_PATHS = List.of("chests/", "loot/", "archaeology/");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static List<String> includedLootTablePaths = DEFAULT_PATHS;

    private LootExplorerConfig() {}

    public static void load() {
        Path path = configPath();
        if (!Files.exists(path)) {
            savePaths(DEFAULT_PATHS);
            includedLootTablePaths = DEFAULT_PATHS;
            return;
        }

        try {
            JsonObject root = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), JsonObject.class);
            JsonArray paths = root == null ? null : root.getAsJsonArray(PATHS_KEY);
            if (paths == null) {
                throw new IllegalArgumentException("Missing '" + PATHS_KEY + "'");
            }

            List<String> loadedPaths = new ArrayList<>();
            for (JsonElement entry : paths) {
                if (entry.isJsonPrimitive() && entry.getAsJsonPrimitive().isString()) {
                    loadedPaths.add(entry.getAsString());
                }
            }
            includedLootTablePaths = normalize(loadedPaths);
        } catch (Exception e) {
            Constants.LOGGER.warn("Could not read {}. Using default loot-table paths.", path, e);
            includedLootTablePaths = DEFAULT_PATHS;
        }
    }

    public static List<String> getConfiguredPaths() {
        return List.copyOf(includedLootTablePaths);
    }

    public static boolean includes(String resourcePath) {
        if (!resourcePath.startsWith("loot_table/")) {
            return false;
        }
        String relativePath = resourcePath.substring("loot_table/".length());
        return includedLootTablePaths.stream().anyMatch(relativePath::startsWith);
    }

    public static void savePaths(List<String> paths) {
        List<String> normalizedPaths = normalize(paths);
        JsonObject root = new JsonObject();
        JsonArray array = new JsonArray();
        normalizedPaths.forEach(array::add);
        root.add(PATHS_KEY, array);

        try {
            Path path = configPath();
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(root) + System.lineSeparator(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Constants.LOGGER.error("Could not save LootExplorer configuration", e);
        }
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
