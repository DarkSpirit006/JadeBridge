package com.darkspirit69.jadebridge.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.darkspirit69.jadebridge.TestEnv;
import com.darkspirit69.jadebridge.provider.JadeProvider;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class EntityRequestTest {

    private static final JadeProvider PROVIDER = new JadeProvider() {
        @Override
        public Identifier id() {
            return Identifier.withDefaultNamespace("entity_health");
        }
    };

    @BeforeAll
    static void boot() {
        TestEnv.boot();
    }

    private static EntityRequest decode(byte[] bytes) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
                Unpooled.wrappedBuffer(bytes),
                RegistryAccess.EMPTY);
        return EntityRequest.decode(buf, id -> id == 0 ? PROVIDER : null);
    }

    private static byte[] encode(boolean showDetails, int entityId, int partIndex, float x, float y, float z, int... ids) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        ByteBufCodecs.BOOL.encode(buf, showDetails);
        ByteBufCodecs.VAR_INT.encode(buf, entityId);
        ByteBufCodecs.VAR_INT.encode(buf, partIndex);
        buf.writeFloat(x);
        buf.writeFloat(y);
        buf.writeFloat(z);
        ByteBufCodecs.COMPOUND_TAG.encode(buf, new CompoundTag());
        buf.writeVarInt(ids.length);
        for (int id : ids) {
            buf.writeVarInt(id);
        }
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        buf.release();
        return bytes;
    }

    @Test
    void wellFormedRequestDecodes() {
        EntityRequest request = decode(encode(true, 12345, -1, 1.5f, 64f, -2.5f, 0));
        assertTrue(request.showDetails());
        assertEquals(12345, request.entityId());
        assertEquals(-1, request.partIndex());
        assertEquals(1.5, request.hitVec().x, 1e-6);
        assertEquals(64, request.hitVec().y, 1e-6);
        assertEquals(-2.5, request.hitVec().z, 1e-6);
        assertEquals(java.util.List.of(PROVIDER), request.providers());
    }

    @Test
    void dragonPartIndexDecodes() {
        EntityRequest request = decode(encode(false, 7, 3, 0, 0, 0, 0));
        assertEquals(3, request.partIndex());
    }

    @Test
    void unknownProviderIdIsRejected() {
        assertThrows(Exception.class, () -> decode(encode(false, 1, -1, 0, 0, 0, 42)));
    }

    @Test
    void truncatedPayloadIsRejected() {
        byte[] full = encode(false, 1, -1, 0, 0, 0, 0);
        assertThrows(Exception.class, () -> decode(java.util.Arrays.copyOf(full, 5)));
    }

    @Test
    void emptyPayloadIsRejected() {
        assertThrows(Exception.class, () -> decode(new byte[0]));
    }

    private static void assertTrue(boolean value) {
        org.junit.jupiter.api.Assertions.assertTrue(value);
    }
}
