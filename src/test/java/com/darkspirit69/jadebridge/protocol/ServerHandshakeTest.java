package com.darkspirit69.jadebridge.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.darkspirit69.jadebridge.TestEnv;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Map;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Decodes the encoded handshake by hand, field by field, so the wire layout is
 * pinned down independently of the encoder.
 */
class ServerHandshakeTest {

    @BeforeAll
    static void boot() {
        TestEnv.boot();
    }

    @Test
    void emptyHandshakeLayout() {
        byte[] bytes = ServerHandshake.encode(Map.of(), List.of(), List.of(), List.of());
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));
        assertEquals(4, bytes.length);
        assertEquals(0, buf.readVarInt()); // config map size
        assertEquals(0, buf.readVarInt()); // shearable block count
        assertEquals(0, buf.readVarInt()); // block provider count
        assertEquals(0, buf.readVarInt()); // entity provider count
        assertEquals(0, buf.readableBytes());
    }

    @Test
    void fullHandshakeLayout() {
        Map<Identifier, Object> config = Map.of(
                Identifier.withDefaultNamespace("entity_health"), Boolean.TRUE,
                Identifier.withDefaultNamespace("potion_effects.limit"), 4);
        List<Identifier> blockIds = List.of(
                Identifier.fromNamespaceAndPath("jade", "object_name"),
                Identifier.withDefaultNamespace("furnace"));
        List<Identifier> entityIds = List.of(Identifier.withDefaultNamespace("animal_owner"));

        byte[] bytes = ServerHandshake.encode(config, List.of(), blockIds, entityIds);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));

        assertEquals(2, buf.readVarInt());
        Map<Identifier, Object> decoded = new java.util.HashMap<>();
        for (int i = 0; i < 2; i++) {
            Identifier key = Identifier.STREAM_CODEC.decode(buf);
            decoded.put(key, PrimitiveValue.STREAM_CODEC.decode(buf));
        }
        assertEquals(Boolean.TRUE, decoded.get(Identifier.withDefaultNamespace("entity_health")));
        assertEquals(4, decoded.get(Identifier.withDefaultNamespace("potion_effects.limit")));

        assertEquals(0, buf.readVarInt()); // no shearable blocks in this test
        assertEquals(2, buf.readVarInt());
        assertEquals(blockIds.get(0), Identifier.STREAM_CODEC.decode(buf));
        assertEquals(blockIds.get(1), Identifier.STREAM_CODEC.decode(buf));
        assertEquals(1, buf.readVarInt());
        assertEquals(entityIds.get(0), Identifier.STREAM_CODEC.decode(buf));
        assertEquals(0, buf.readableBytes());
    }
}
