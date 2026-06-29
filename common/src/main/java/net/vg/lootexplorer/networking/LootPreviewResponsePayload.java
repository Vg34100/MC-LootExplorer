package net.vg.lootexplorer.networking;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.vg.lootexplorer.Constants;

import java.util.ArrayList;
import java.util.List;

public class LootPreviewResponsePayload implements CustomPacketPayload {
    private final String lootTablePath;
    private final List<ItemStack> items;

    public static final Type<LootPreviewResponsePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "loot_preview_response"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LootPreviewResponsePayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeUtf(payload.lootTablePath);
                ItemStack.OPTIONAL_LIST_STREAM_CODEC.encode(buf, payload.items);
            },
            (buf) -> {
                String path = buf.readUtf();
                List<ItemStack> items = new ArrayList<>(ItemStack.OPTIONAL_LIST_STREAM_CODEC.decode(buf));
                return new LootPreviewResponsePayload(path, items);
            }
    );

    public LootPreviewResponsePayload(String lootTablePath, List<ItemStack> items) {
        this.lootTablePath = lootTablePath;
        this.items = items;
    }

    public String lootTablePath() {
        return lootTablePath;
    }

    public List<ItemStack> items() {
        return items;
    }

    @Override
    public Type<?> type() {
        return TYPE;
    }
}
