package com.darkspirit69.jadebridge.protocol;

import com.darkspirit69.jadebridge.provider.JadeProvider;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Decoded {@code jade:request_block}. Field order mirrors
 * {@code RequestBlockPacket(SyncData(showDetails, hit, serversideRep, data), dataProviders)}.
 */
public record BlockRequest(
        boolean showDetails,
        BlockHitResult hit,
        ItemStack serversideRep,
        CompoundTag data,
        List<JadeProvider> providers) {

    private static final int MAX_PROVIDERS = 256;

    public static BlockRequest decode(RegistryFriendlyByteBuf buf, java.util.function.IntFunction<JadeProvider> providerById) {
        boolean showDetails = ByteBufCodecs.BOOL.decode(buf);
        BlockHitResult hit = BlockHitResult.STREAM_CODEC.decode(buf);
        ItemStack serversideRep = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        CompoundTag data = ByteBufCodecs.COMPOUND_TAG.decode(buf);
        int count = buf.readVarInt();
        if (count < 0 || count > MAX_PROVIDERS) {
            throw new IllegalArgumentException("Bad provider count: " + count);
        }
        List<JadeProvider> providers = new ArrayList<>(Math.min(count, 16));
        for (int i = 0; i < count; i++) {
            JadeProvider provider = providerById.apply(buf.readVarInt());
            if (provider == null) {
                throw new IllegalArgumentException("Unknown block data provider id at index " + i);
            }
            providers.add(provider);
        }
        return new BlockRequest(showDetails, hit, serversideRep, data, providers);
    }

    public static RegistryFriendlyByteBuf buffer(byte[] bytes, RegistryAccess registryAccess) {
        return new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(bytes), registryAccess);
    }
}
