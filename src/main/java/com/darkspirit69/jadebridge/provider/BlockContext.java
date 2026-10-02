package com.darkspirit69.jadebridge.provider;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamEncoder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * Everything a block provider may look at while answering one request, plus the
 * response tag and the buffer used to stream-codec values into NBT.
 */
public final class BlockContext {

    private final ServerPlayer player;
    private final ServerLevel level;
    private final BlockHitResult hit;
    private final BlockState state;
    @Nullable
    private final BlockEntity blockEntity;
    private final boolean showDetails;
    private final CompoundTag data;
    private final RegistryFriendlyByteBuf buffer;

    public BlockContext(
            ServerPlayer player,
            BlockHitResult hit,
            BlockState state,
            @Nullable BlockEntity blockEntity,
            boolean showDetails,
            CompoundTag data,
            RegistryFriendlyByteBuf buffer) {
        this.player = player;
        this.level = player.level();
        this.hit = hit;
        this.state = state;
        this.blockEntity = blockEntity;
        this.showDetails = showDetails;
        this.data = data;
        this.buffer = buffer;
    }

    public ServerPlayer player() {
        return player;
    }

    public ServerLevel level() {
        return level;
    }

    public BlockHitResult hit() {
        return hit;
    }

    public BlockPos pos() {
        return hit.getBlockPos();
    }

    public BlockState state() {
        return state;
    }

    @Nullable
    public BlockEntity blockEntity() {
        return blockEntity;
    }

    public <T extends BlockEntity> T blockEntity(Class<T> type) {
        if (!type.isInstance(blockEntity)) {
            throw new IllegalStateException("Expected block entity " + type.getSimpleName());
        }
        return type.cast(blockEntity);
    }

    public boolean showDetails() {
        return showDetails;
    }

    public CompoundTag data() {
        return data;
    }

    public RegistryFriendlyByteBuf buffer() {
        return buffer;
    }

    /**
     * Encodes a streamed value the same way Jade's {@code AccessorImpl.encodeAsNbt} does:
     * raw codec bytes stored as a byte array tag under the provider id.
     */
    public <V> void put(Identifier id, StreamEncoder<? super RegistryFriendlyByteBuf, V> codec, V value) {
        buffer.clear();
        codec.encode(buffer, value);
        byte[] bytes = new byte[buffer.readableBytes()];
        buffer.readBytes(bytes);
        buffer.clear();
        data.put(id.toString(), new ByteArrayTag(bytes));
    }
}
