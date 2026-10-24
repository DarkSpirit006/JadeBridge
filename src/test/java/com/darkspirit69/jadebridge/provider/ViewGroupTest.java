package com.darkspirit69.jadebridge.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.darkspirit69.jadebridge.TestEnv;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Pins down the view group wire format (the shape Jade's item storage, fluid,
 * energy and progress views all share) using a VarInt stand-in for the view codec.
 */
class ViewGroupTest {

    private static final StreamCodec<io.netty.buffer.ByteBuf, Integer> VIEW = ByteBufCodecs.VAR_INT;

    @BeforeAll
    static void boot() {
        TestEnv.boot();
    }

    @Test
    void plainGroupRoundTrip() {
        StreamCodec<io.netty.buffer.ByteBuf, ViewGroup<Integer>> codec = ViewGroup.codec(VIEW);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        codec.encode(buf, new ViewGroup<>(List.of(1, 300, 7)));

        // list size varint + three view varints + two absent-optional flags
        assertEquals(7, buf.readableBytes());
        ViewGroup<Integer> decoded = codec.decode(buf);
        assertEquals(List.of(1, 300, 7), decoded.views());
        assertNull(decoded.id());
        assertNull(decoded.extraData());
        buf.release();
    }

    @Test
    void groupWithIdAndExtraDataRoundTrip() {
        StreamCodec<io.netty.buffer.ByteBuf, ViewGroup<Integer>> codec = ViewGroup.codec(VIEW);
        CompoundTag extra = new CompoundTag();
        extra.putFloat("Progress", 0.5f);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        codec.encode(buf, new ViewGroup<>(List.of(9), "group", extra));

        ViewGroup<Integer> decoded = codec.decode(buf);
        assertEquals(List.of(9), decoded.views());
        assertEquals("group", decoded.id());
        assertEquals(0.5f, decoded.extraData().getFloatOr("Progress", -1));
        buf.release();
    }

    @Test
    void listCodecRoundTrip() {
        StreamCodec<io.netty.buffer.ByteBuf, Map.Entry<Identifier, List<ViewGroup<Integer>>>> codec =
                ViewGroup.listCodec(VIEW);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        codec.encode(buf, Map.entry(
                Identifier.withDefaultNamespace("item_storage.default"),
                List.of(new ViewGroup<>(List.of(4)), new ViewGroup<>(List.of(5, 6), "sorted", null))));

        Map.Entry<Identifier, List<ViewGroup<Integer>>> decoded = codec.decode(buf);
        assertEquals(Identifier.withDefaultNamespace("item_storage.default"), decoded.getKey());
        assertEquals(2, decoded.getValue().size());
        assertEquals(List.of(4), decoded.getValue().get(0).views());
        assertEquals("sorted", decoded.getValue().get(1).id());
        assertEquals(6, decoded.getValue().get(1).views().get(1));
        buf.release();
    }
}
