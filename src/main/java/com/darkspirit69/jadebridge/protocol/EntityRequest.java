package com.darkspirit69.jadebridge.protocol;

import com.darkspirit69.jadebridge.provider.JadeProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.phys.Vec3;

/**
 * Decoded {@code jade:request_entity}. Field order mirrors
 * {@code RequestEntityPacket(SyncData(showDetails, id, partIndex, hitVec, data), dataProviders)}.
 */
public record EntityRequest(
        boolean showDetails,
        int entityId,
        int partIndex,
        Vec3 hitVec,
        CompoundTag data,
        List<JadeProvider> providers) {

    private static final int MAX_PROVIDERS = 256;

    public static EntityRequest decode(RegistryFriendlyByteBuf buf, IntFunction<JadeProvider> providerById) {
        boolean showDetails = ByteBufCodecs.BOOL.decode(buf);
        int entityId = ByteBufCodecs.VAR_INT.decode(buf);
        int partIndex = ByteBufCodecs.VAR_INT.decode(buf);
        var hitVec = ByteBufCodecs.VECTOR3F.decode(buf);
        CompoundTag data = ByteBufCodecs.COMPOUND_TAG.decode(buf);
        int count = buf.readVarInt();
        if (count < 0 || count > MAX_PROVIDERS) {
            throw new IllegalArgumentException("Bad provider count: " + count);
        }
        List<JadeProvider> providers = new ArrayList<>(Math.min(count, 16));
        for (int i = 0; i < count; i++) {
            JadeProvider provider = providerById.apply(buf.readVarInt());
            if (provider == null) {
                throw new IllegalArgumentException("Unknown entity data provider id at index " + i);
            }
            providers.add(provider);
        }
        return new EntityRequest(showDetails, entityId, partIndex, new Vec3(hitVec.x(), hitVec.y(), hitVec.z()), data, providers);
    }

    public static RegistryFriendlyByteBuf buffer(byte[] bytes, RegistryAccess registryAccess) {
        return new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(bytes), registryAccess);
    }
}
