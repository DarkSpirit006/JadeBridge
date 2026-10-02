package com.darkspirit69.jadebridge.provider;

import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamEncoder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Everything an entity provider may look at while answering one request. */
public final class EntityContext {

    private final ServerPlayer player;
    private final ServerLevel level;
    private final Entity entity;
    private final Vec3 hitVec;
    private final boolean showDetails;
    private final CompoundTag data;
    private final RegistryFriendlyByteBuf buffer;

    public EntityContext(
            ServerPlayer player,
            Entity entity,
            Vec3 hitVec,
            boolean showDetails,
            CompoundTag data,
            RegistryFriendlyByteBuf buffer) {
        this.player = player;
        this.level = player.level();
        this.entity = entity;
        this.hitVec = hitVec;
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

    public Entity entity() {
        return entity;
    }

    public <T extends Entity> T entity(Class<T> type) {
        if (!type.isInstance(entity)) {
            throw new IllegalStateException("Expected entity " + type.getSimpleName());
        }
        return type.cast(entity);
    }

    public Vec3 hitVec() {
        return hitVec;
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

    public <V> void put(Identifier id, StreamEncoder<? super RegistryFriendlyByteBuf, V> codec, V value) {
        buffer.clear();
        codec.encode(buffer, value);
        byte[] bytes = new byte[buffer.readableBytes()];
        buffer.readBytes(bytes);
        buffer.clear();
        data.put(id.toString(), new ByteArrayTag(bytes));
    }

    @Nullable
    public Object target() {
        return entity;
    }
}
