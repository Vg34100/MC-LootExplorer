package net.vg.lootexplorer.networking;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.vg.lootexplorer.Constants;

public class LootPreviewRequestPayload implements CustomPacketPayload {
    private final String lootTablePath;

    public static final Type<LootPreviewRequestPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "loot_preview_request"));

    public static final StreamCodec<FriendlyByteBuf, LootPreviewRequestPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeUtf(payload.lootTablePath);
            },
            (buf) -> {
                return new LootPreviewRequestPayload(buf.readUtf());
            }
    );

    public LootPreviewRequestPayload(String lootTablePath) {
        this.lootTablePath = lootTablePath;
    }

    public String lootTablePath() {
        return lootTablePath;
    }

    @Override
    public Type<?> type() {
        return TYPE;
    }
}