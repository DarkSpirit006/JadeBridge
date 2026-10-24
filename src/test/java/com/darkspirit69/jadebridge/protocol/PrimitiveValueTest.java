package com.darkspirit69.jadebridge.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.darkspirit69.jadebridge.TestEnv;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PrimitiveValueTest {

    @BeforeAll
    static void boot() {
        TestEnv.boot();
    }

    private static FriendlyByteBuf buf() {
        return new FriendlyByteBuf(Unpooled.buffer());
    }

    @Test
    void booleanEncoding() {
        FriendlyByteBuf buf = buf();
        PrimitiveValue.STREAM_CODEC.encode(buf, Boolean.TRUE);
        PrimitiveValue.STREAM_CODEC.encode(buf, Boolean.FALSE);
        assertEquals(2, buf.readableBytes());
        assertEquals(Boolean.TRUE, PrimitiveValue.STREAM_CODEC.decode(buf));
        assertEquals(Boolean.FALSE, PrimitiveValue.STREAM_CODEC.decode(buf));
    }

    @Test
    void smallIntegersUseSingleByteTag() {
        FriendlyByteBuf buf = buf();
        PrimitiveValue.STREAM_CODEC.encode(buf, 5);
        PrimitiveValue.STREAM_CODEC.encode(buf, 107); // Byte.MAX_VALUE - 20
        assertEquals(2, buf.readableBytes());
        assertEquals(5, PrimitiveValue.STREAM_CODEC.decode(buf));
        assertEquals(107, PrimitiveValue.STREAM_CODEC.decode(buf));
    }

    @Test
    void largeIntegersUseVarInt() {
        FriendlyByteBuf buf = buf();
        PrimitiveValue.STREAM_CODEC.encode(buf, Integer.MAX_VALUE);
        PrimitiveValue.STREAM_CODEC.encode(buf, -3);
        assertEquals(Integer.MAX_VALUE, PrimitiveValue.STREAM_CODEC.decode(buf));
        assertEquals(-3, PrimitiveValue.STREAM_CODEC.decode(buf));
    }

    @Test
    void nonIntegralNumbersUseFloat() {
        FriendlyByteBuf buf = buf();
        PrimitiveValue.STREAM_CODEC.encode(buf, 1.5f);
        assertEquals(1.5f, ((Number) PrimitiveValue.STREAM_CODEC.decode(buf)).floatValue());
    }

    @Test
    void stringsRoundTrip() {
        FriendlyByteBuf buf = buf();
        PrimitiveValue.STREAM_CODEC.encode(buf, "minecraft:entity_health");
        assertEquals("minecraft:entity_health", PrimitiveValue.STREAM_CODEC.decode(buf));
    }

    @Test
    void unknownTagIsRejected() {
        FriendlyByteBuf buf = buf();
        buf.writeByte(7);
        assertThrows(Exception.class, () -> PrimitiveValue.STREAM_CODEC.decode(buf));
    }

    @Test
    void junkValuesAreRejectedOnEncode() {
        FriendlyByteBuf buf = buf();
        assertThrows(Exception.class, () -> PrimitiveValue.STREAM_CODEC.encode(buf, new Object()));
    }
}
