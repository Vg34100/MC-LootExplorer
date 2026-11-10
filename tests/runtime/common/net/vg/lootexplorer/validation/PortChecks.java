package net.vg.lootexplorer.validation;

import com.google.gson.JsonParser;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.platform.Platform;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.vg.lootexplorer.config.LootExplorerConfig;
import net.vg.lootexplorer.networking.LootPreviewRequestPayload;
import net.vg.lootexplorer.networking.LootPreviewResponsePayload;
import net.vg.lootexplorer.util.*;

import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/** Explicit development-only assertions against the running game's real registries. */
public final class PortChecks {
    private static final AtomicBoolean ready = new AtomicBoolean();
    public static void register() {
        LifecycleEvent.SERVER_STARTED.register(server -> ready.set(true));
        TickEvent.SERVER_POST.register(server -> {
            if (ready.getAndSet(false)) {
                try { check(server); }
                catch (Throwable failure) {
                    failure.printStackTrace();
                    System.out.println("LOOTEXPLORER_PORT_CHECKS_FAILED");
                    throw new AssertionError("Port validation failed", failure);
                }
            }
        });
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static void check(MinecraftServer server) throws Exception {
        require(!LootHandler.tables.isEmpty(), "Common server loot discovery was skipped");
        for (String path : List.of("chests/simple_dungeon", "archaeology/desert_well")) {
            String table = "minecraft:" + path;
            require(LootHandler.lootTableItemMap.containsKey(table), "Missing discovered table " + table);
            require(!LootHandler.lootTableItemMap.get(table).isEmpty(), "Empty interpreted table " + table);
            var loot = LootTables.get(server, ModIds.id("minecraft", path));
            var builder = new LootParams.Builder(server.overworld())
                    .withParameter(LootContextParams.ORIGIN, Vec3.ZERO);
            if (path.startsWith("archaeology/")) {
                builder.withParameter(LootContextParams.THIS_ENTITY,
                        new net.minecraft.world.entity.decoration.ArmorStand(server.overworld(), 0, 64, 0));
                if (Platform.getMinecraftVersion().startsWith("26.")) {
                    builder.withParameter(LootContextParams.TOOL, new ItemStack(Items.BRUSH));
                }
            }
            var params = builder.create(loot.getParamSet());
            require(!loot.getRandomItems(params, 1L).isEmpty(), "Vanilla lookup/context failed " + table);
        }
        LootHandler.buildChests();
        require(!LootHandler.containerList.isEmpty(), "No container previews");
        require(!LootHandler.brushableList.isEmpty(), "No archaeology previews");
        for (var list : List.of(LootHandler.containerList, LootHandler.brushableList)) {
            ItemStack preview = list.get(0);
            require(ItemPreviewData.isPreview(preview), "Missing saved preview marker");
            require(!ItemPreviewData.lore(preview).isEmpty(), "Missing preview table lore");
        }
        var enchanted = LootHandler.lootTableItemMap.get("minecraft:chests/simple_dungeon").stream()
                .filter(stack -> stack.is(Items.BOOK)).toList();
        require(!enchanted.isEmpty(), "Loot modifier interpretation lost enchantments");
        require(enchanted.stream().allMatch(stack -> stack.hasFoil() && !ItemPreviewData.lore(stack).isEmpty()),
                "Enchantment glint/lore was lost");
        checkConfig();
        checkPackets(server);
        System.out.println("LOOTEXPLORER_PORT_CHECKS_PASS: chest/brushable discovery, vanilla sampling, previews, modifiers, JSON round-trip, packet codecs");
    }

    private static void checkConfig() throws Exception {
        var path = Platform.getConfigFolder().resolve(LootExplorerConfig.FILE_NAME);
        byte[] original = Files.readAllBytes(path);
        try {
            Files.writeString(path, "{\"includedLootTablePaths\":[\"chests/\",\"archaeology/\",\"custom/\"]}");
            LootExplorerConfig.load();
            require(LootExplorerConfig.getContainerPaths().contains("custom/"), "Legacy JSON migration lost custom path");
            require(LootExplorerConfig.getBrushablePaths().equals(List.of("archaeology/")), "Legacy brushable migration");
            LootExplorerConfig.savePaths(List.of(" chests ", "chests/", "custom", "extra/path/"), List.of(" archaeology/ "));
            LootExplorerConfig.load();
            require(LootExplorerConfig.getContainerPaths().equals(List.of("chests/", "custom/", "extra/path/")), "Dynamic path persistence/normalization");
            require(LootExplorerConfig.includesContainer("loot_tables/custom/example.json")
                    && LootExplorerConfig.includesContainer("loot_table/custom/example.json"), "Resource directory boundary");
            var json = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            require(json.has("containerLootTablePaths") && json.has("brushableLootTablePaths"), "Stable JSON keys");
        } finally {
            Files.write(path, original);
            LootExplorerConfig.load();
        }
    }

    private static void checkPackets(MinecraftServer server) throws Exception {
        require(LootPreviewRequestPayload.ID.toString().equals("lootexplorer:loot_preview_request"), "Request ID changed");
        require(LootPreviewResponsePayload.ID.toString().equals("lootexplorer:loot_preview_response"), "Response ID changed");
        var request = new LootPreviewRequestPayload("minecraft:chests/simple_dungeon");
        var stack = new ItemStack(Items.CHEST, 2);
        ItemPreviewData.mark(stack);
        ItemPreviewData.lore(stack, List.of(Component.literal("minecraft:chests/simple_dungeon")));
        var response = new LootPreviewResponsePayload(request.lootTablePath(), List.of(stack));
        var raw = Unpooled.buffer();
        try {
            Object buffer;
            try {
                Class<?> type = Class.forName("net.minecraft.network.RegistryFriendlyByteBuf");
                var ctor = java.util.Arrays.stream(type.getConstructors())
                        .filter(c -> c.getParameterCount() == 2 && c.getParameterTypes()[1].isInstance(server.registryAccess()))
                        .findFirst().orElseThrow();
                buffer = ctor.newInstance(raw, server.registryAccess());
                var codec = LootPreviewRequestPayload.class.getField("CODEC").get(null);
                var codecType = Class.forName("net.minecraft.network.codec.StreamCodec");
                var encode = codecType.getMethod("encode", Object.class, Object.class);
                var decode = codecType.getMethod("decode", Object.class);
                encode.invoke(codec, buffer, request);
                require(((LootPreviewRequestPayload) decode.invoke(codec, buffer)).lootTablePath().equals(request.lootTablePath()), "C2S codec");
                raw.clear();
                codec = LootPreviewResponsePayload.class.getField("CODEC").get(null);
                encode.invoke(codec, buffer, response);
                response = (LootPreviewResponsePayload) decode.invoke(codec, buffer);
            } catch (ClassNotFoundException legacy) {
                buffer = new FriendlyByteBuf(raw);
                ((FriendlyByteBuf) buffer).writeUtf(request.lootTablePath());
                require(((FriendlyByteBuf) buffer).readUtf().equals(request.lootTablePath()), "Legacy C2S codec");
                raw.clear();
                LootPreviewResponsePayload.class.getMethod("write", FriendlyByteBuf.class).invoke(response, buffer);
                response = (LootPreviewResponsePayload) LootPreviewResponsePayload.class.getMethod("read", FriendlyByteBuf.class).invoke(null, buffer);
            }
            require(response.lootTablePath().equals(request.lootTablePath()) && response.items().size() == 1, "S2C header");
            var received = response.items().get(0);
            require(received.is(Items.CHEST) && received.getCount() == 2 && ItemPreviewData.isPreview(received)
                    && ItemPreviewData.lore(received).get(0).getString().equals(request.lootTablePath()), "S2C item data lost");
        } finally { raw.release(); }
    }
}
