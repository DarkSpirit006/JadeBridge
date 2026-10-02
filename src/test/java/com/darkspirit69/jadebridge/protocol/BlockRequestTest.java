package com.darkspirit69.jadebridge.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.darkspirit69.jadebridge.TestEnv;
import com.darkspirit69.jadebridge.provider.JadeProvider;
import io.netty.buffer.Unpooled;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BlockRequestTest {

    private static final JadeProvider PROVIDER_A = provider("jade:test_a");
    private static final JadeProvider PROVIDER_B = provider("jade:test_b");

    @BeforeAll
    static void boot() {
        TestEnv.boot();
    }

    private static JadeProvider provider(String path) {
        return new JadeProvider() {
            private final Identifier id = Identifier.parse(path);

            @Override
            public Identifier id() {
                return id;
            }
        };
    }

    private static BlockRequest decode(byte[] bytes) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
                Unpooled.wrappedBuffer(bytes),
                RegistryAccess.EMPTY);
        return BlockRequest.decode(buf, id -> id == 0 ? PROVIDER_A : id == 1 ? PROVIDER_B : null);
    }

    private static byte[] encode(BlockHitResult hit, CompoundTag data, int... providerIds) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        ByteBufCodecs.BOOL.encode(buf, true);
        BlockHitResult.STREAM_CODEC.encode(buf, hit);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, ItemStack.EMPTY);
        ByteBufCodecs.COMPOUND_TAG.encode(buf, data);
        buf.writeVarInt(providerIds.length);
        for (int id : providerIds) {
            buf.writeVarInt(id);
        }
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        buf.release();
        return bytes;
    }

    @Test
    void wellFormedRequestDecodes() {
        BlockHitResult hit = new BlockHitResult(new Vec3(1, 2, 3), Direction.UP, new BlockPos(-5, 64, 300), true);
        CompoundTag data = new CompoundTag();
        data.putBoolean("SortItems", true);
        BlockRequest request = decode(encode(hit, data, 0, 1));

        assertTrue(request.showDetails());
        assertEquals(hit.getBlockPos(), request.hit().getBlockPos());
        assertEquals(hit.getDirection(), request.hit().getDirection());
        assertEquals(hit.isInside(), request.hit().isInside());
        assertTrue(request.serversideRep().isEmpty());
        assertTrue(request.data().getBooleanOr("SortItems", false));
        assertEquals(List.of(PROVIDER_A, PROVIDER_B), request.providers());
    }

    @Test
    void unknownProviderIdIsRejected() {
        BlockHitResult hit = new BlockHitResult(new Vec3(0, 0, 0), Direction.UP, BlockPos.ZERO, false);
        assertThrows(Exception.class, () -> decode(encode(hit, new CompoundTag(), 99)));
    }

    @Test
    void negativeProviderCountIsRejected() {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        ByteBufCodecs.BOOL.encode(buf, false);
        BlockHitResult.STREAM_CODEC.encode(buf, 
                new BlockHitResult(new Vec3(0, 0, 0), Direction.UP, BlockPos.ZERO, false));
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, ItemStack.EMPTY);
        ByteBufCodecs.COMPOUND_TAG.encode(buf, new CompoundTag());
        buf.writeVarInt(1000); // beyond MAX_PROVIDERS

        RegistryFriendlyByteBuf in = new RegistryFriendlyByteBuf(
                Unpooled.wrappedBuffer(buf.array(), 0, buf.readableBytes()),
                RegistryAccess.EMPTY);
        assertThrows(Exception.class, () -> BlockRequest.decode(in, id -> PROVIDER_A));
        buf.release();
    }

    @Test
    void truncatedPayloadIsRejected() {
        BlockHitResult hit = new BlockHitResult(new Vec3(0, 0, 0), Direction.UP, BlockPos.ZERO, false);
        byte[] full = encode(hit, new CompoundTag(), 0);
        assertThrows(Exception.class, () -> decode(java.util.Arrays.copyOf(full, full.length - 2)));
    }

    @Test
    void emptyPayloadIsRejected() {
        assertThrows(Exception.class, () -> decode(new byte[0]));
    }

    @Test
    void malformedNbtIsRejected() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeByte(1); // showDetails
        // hit result bytes missing entirely, junk nbt header only
        buf.writeByte(0x7f);
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        buf.release();
        assertThrows(Exception.class, () -> decode(bytes));
    }

    @Test
    void providerIdsAreVarInts() {
        // a two-byte VarInt id must decode as one provider, not two
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        ByteBufCodecs.BOOL.encode(buf, false);
        BlockHitResult.STREAM_CODEC.encode(buf, new BlockHitResult(new Vec3(0, 0, 0), Direction.UP, BlockPos.ZERO, false));
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, ItemStack.EMPTY);
        ByteBufCodecs.COMPOUND_TAG.encode(buf, new CompoundTag());
        buf.writeVarInt(1);
        buf.writeVarInt(200);
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        buf.release();

        RegistryFriendlyByteBuf in = new RegistryFriendlyByteBuf(
                Unpooled.wrappedBuffer(bytes), RegistryAccess.EMPTY);
        BlockRequest request = BlockRequest.decode(in, id -> id == 200 ? PROVIDER_A : null);
        assertEquals(List.of(PROVIDER_A), request.providers());
    }
}
