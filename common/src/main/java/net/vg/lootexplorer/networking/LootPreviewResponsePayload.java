package net.vg.lootexplorer.networking;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.vg.lootexplorer.Constants;

import java.util.ArrayList;
import java.util.List;

public class LootPreviewResponsePayload implements CustomPacketPayload {
    private final String lootTablePath;
    private final List<ItemStack> items;

    public static final Type<LootPreviewResponsePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "loot_preview_response"));

    public static final StreamCodec<FriendlyByteBuf, LootPreviewResponsePayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeUtf(payload.lootTablePath);
                buf.writeInt(payload.items.size());
                for (ItemStack item : payload.items) {
                    Tag nbt = ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, item).getOrThrow();
                    buf.writeNbt((CompoundTag) nbt);
                }
            },
            (buf) -> {
                String path = buf.readUtf();
                int count = buf.readInt();
                List<ItemStack> items = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    CompoundTag tag = buf.readNbt();
                    items.add(tag != null ? ItemStack.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow() : ItemStack.EMPTY);
                }
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