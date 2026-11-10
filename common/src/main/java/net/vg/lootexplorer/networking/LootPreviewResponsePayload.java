package net.vg.lootexplorer.networking;

//? if >=1.21 {
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//? }
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.vg.lootexplorer.util.ModIds;

import java.util.ArrayList;
import java.util.List;

public class LootPreviewResponsePayload
        //? if >=1.21 {
        implements CustomPacketPayload
        //? }
{
    private final String lootTablePath;
    private final List<ItemStack> items;

    public static final Identifier ID = ModIds.id("loot_preview_response");

    //? if >=1.21 {
    public static final Type<LootPreviewResponsePayload> TYPE = new Type<>(ID);

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

    //? }

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

    //? if >=1.21 {
    @Override
    public Type<?> type() {
        return TYPE;
    }
    //? }
    //? if <1.21 {
    /*public void write(net.minecraft.network.FriendlyByteBuf buffer) {
        buffer.writeUtf(lootTablePath);
        buffer.writeVarInt(items.size());
        items.forEach(buffer::writeItem);
    }

    public static LootPreviewResponsePayload read(net.minecraft.network.FriendlyByteBuf buffer) {
        String path = buffer.readUtf();
        int count = buffer.readVarInt();
        if (count < 0 || count > 32768) throw new IllegalArgumentException("Invalid preview item count");
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < count; i++) items.add(buffer.readItem());
        return new LootPreviewResponsePayload(path, items);
    }
    *///? }
}
