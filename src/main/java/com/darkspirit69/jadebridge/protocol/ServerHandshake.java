package com.darkspirit69.jadebridge.protocol;

import io.netty.buffer.Unpooled;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

/**
 * Body of {@code jade:server_handshake}. Field order is fixed by the Jade record
 * {@code ServerHandshakePacket(config, shearableBlocks, blockProviderIds, entityProviderIds)}:
 * a config map, the raw registry ids of shearable blocks, then the two provider id lists.
 * None of these fields need registry access, so a plain buffer is enough.
 */
public final class ServerHandshake {

    private static final StreamCodec<io.netty.buffer.ByteBuf, Identifier> ID = Identifier.STREAM_CODEC;
    private static final StreamCodec<io.netty.buffer.ByteBuf, Map<Identifier, Object>> CONFIG_MAP =
            ByteBufCodecs.map(HashMap::new, ID, PrimitiveValue.STREAM_CODEC);

    public static byte[] encode(
            Map<Identifier, Object> serverConfig,
            List<Block> shearableBlocks,
            List<Identifier> blockProviderIds,
            List<Identifier> entityProviderIds) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            encode(buf, serverConfig, shearableBlocks, blockProviderIds, entityProviderIds);
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            return bytes;
        } finally {
            buf.release();
        }
    }

    public static void encode(
            FriendlyByteBuf buf,
            Map<Identifier, Object> serverConfig,
            List<Block> shearableBlocks,
            List<Identifier> blockProviderIds,
            List<Identifier> entityProviderIds) {
        CONFIG_MAP.encode(buf, serverConfig);
        buf.writeVarInt(shearableBlocks.size());
        for (Block block : shearableBlocks) {
            buf.writeVarInt(BuiltInRegistries.BLOCK.getId(block));
        }
        encodeIdList(buf, blockProviderIds);
        encodeIdList(buf, entityProviderIds);
    }

    private static void encodeIdList(FriendlyByteBuf buf, List<Identifier> ids) {
        buf.writeVarInt(ids.size());
        for (Identifier id : ids) {
            ID.encode(buf, id);
        }
    }

    private ServerHandshake() {
    }
}
