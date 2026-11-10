package net.vg.lootexplorer.networking;

//? if >=1.21 {
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//? }
import net.minecraft.resources.Identifier;
import net.vg.lootexplorer.util.ModIds;

public class LootPreviewRequestPayload
        //? if >=1.21 {
        implements CustomPacketPayload
        //? }
{
    private final String lootTablePath;

    public static final Identifier ID = ModIds.id("loot_preview_request");

    //? if >=1.21 {
    public static final Type<LootPreviewRequestPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, LootPreviewRequestPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeUtf(payload.lootTablePath);
            },
            (buf) -> {
                return new LootPreviewRequestPayload(buf.readUtf());
            }
    );

    //? }

    public LootPreviewRequestPayload(String lootTablePath) {
        this.lootTablePath = lootTablePath;
    }

    public String lootTablePath() {
        return lootTablePath;
    }

    //? if >=1.21 {
    @Override
    public Type<?> type() {
        return TYPE;
    }
    //? }
}
