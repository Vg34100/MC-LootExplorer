package net.vg.lootexplorer.util;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.minecraft.server.level.ServerPlayer;
import net.vg.lootexplorer.Constants;
import net.vg.lootexplorer.networking.LootPreviewRequestPayload;
import net.vg.lootexplorer.networking.LootPreviewResponsePayload;

/** Common packet registration, with the S2C receiver isolated in client code. */
public final class NetworkHandler {
    private NetworkHandler() {}

    public static void register() {
        //? if >=1.21 {
        NetworkManager.registerReceiver(NetworkManager.c2s(), LootPreviewRequestPayload.TYPE,
                LootPreviewRequestPayload.CODEC, (payload, context) ->
                        context.queue(() -> handleLootPreviewRequest(payload.lootTablePath(), context)));
        if (Platform.getEnvironment() == Env.SERVER) {
            NetworkManager.registerS2CPayloadType(LootPreviewResponsePayload.TYPE, LootPreviewResponsePayload.CODEC);
        }
        //? } else {
        /*NetworkManager.registerReceiver(NetworkManager.c2s(), LootPreviewRequestPayload.ID, (buffer, context) -> {
            String path = buffer.readUtf();
            context.queue(() -> handleLootPreviewRequest(path, context));
        });
        *///? }
    }

    public static void requestLootPreview(String lootTablePath) {
        // Optional server installation: never send a channel the peer lacks.
        //? if >=1.21 {
        if (NetworkManager.canServerReceive(LootPreviewRequestPayload.TYPE)) {
            NetworkManager.sendToServer(new LootPreviewRequestPayload(lootTablePath));
        }
        //? } else {
        /*if (NetworkManager.canServerReceive(LootPreviewRequestPayload.ID)) {
            var buffer = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            try {
                buffer.writeUtf(lootTablePath);
                NetworkManager.sendToServer(LootPreviewRequestPayload.ID, buffer);
            } finally { buffer.release(); }
        }
        *///? }
    }

    private static void handleLootPreviewRequest(String path, NetworkManager.PacketContext context) {
        if (context.getEnvironment() != Env.SERVER || !(context.getPlayer() instanceof ServerPlayer player)
                || path.length() > 256 || net.minecraft.resources.Identifier.tryParse(path) == null) return;
        Constants.LOGGER.info("Handling loot preview request for: {}", path);
        LootPreviewResponsePayload response = new LootPreviewResponsePayload(path, LootGenerator.generateLootItems(player, path, 5));
        //? if >=1.21 {
        if (NetworkManager.canPlayerReceive(player, LootPreviewResponsePayload.TYPE)) {
            NetworkManager.sendToPlayer(player, response);
        }
        //? } else {
        /*if (NetworkManager.canPlayerReceive(player, LootPreviewResponsePayload.ID)) {
            var buffer = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            try {
                response.write(buffer);
                NetworkManager.sendToPlayer(player, LootPreviewResponsePayload.ID, buffer);
            } finally { buffer.release(); }
        }
        *///? }
    }
}