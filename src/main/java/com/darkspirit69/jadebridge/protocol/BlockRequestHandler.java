package com.darkspirit69.jadebridge.protocol;

import com.darkspirit69.jadebridge.JadeProtocol;
import com.darkspirit69.jadebridge.Settings;
import com.darkspirit69.jadebridge.provider.BlockContext;
import com.darkspirit69.jadebridge.provider.BlockDataProvider;
import com.darkspirit69.jadebridge.provider.ProviderRegistry;
import com.darkspirit69.jadebridge.session.JadeSession;
import com.darkspirit69.jadebridge.session.SessionManager;
import com.darkspirit69.jadebridge.util.ReachCheck;
import io.netty.buffer.Unpooled;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Handles {@code jade:request_block}. Every payload is treated as untrusted: it must
 * come from a handshaked session, pass the rate limit, decode cleanly and point at a
 * loaded block within the player's reach.
 */
public final class BlockRequestHandler {

    private final Plugin plugin;
    private final Settings settings;
    private final ProviderRegistry providers;
    private final SessionManager sessions;
    private final ReachCheck reach;
    private final Set<Object> failedProviders = new HashSet<>();

    public BlockRequestHandler(
            Plugin plugin,
            Settings settings,
            ProviderRegistry providers,
            SessionManager sessions,
            ReachCheck reach) {
        this.plugin = plugin;
        this.settings = settings;
        this.providers = providers;
        this.sessions = sessions;
        this.reach = reach;
    }

    public void handle(Player player, byte[] bytes) {
        if (bytes.length == 0 || bytes.length > JadeProtocol.MAX_INCOMING_SIZE) {
            return;
        }
        JadeSession session = sessions.get(player);
        if (session == null || !session.tryAcquireBlockRequest(System.currentTimeMillis())) {
            sessions.countRejected();
            return;
        }

        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
                Unpooled.wrappedBuffer(bytes),
                serverPlayer.level().registryAccess());
        try {
            BlockRequest request = BlockRequest.decode(buf, providers::blockProvider);
            if (buf.readableBytes() != 0) {
                throw new IllegalArgumentException("Trailing bytes in request");
            }
            sessions.countBlockRequest();
            respond(serverPlayer, request);
        } catch (Exception e) {
            sessions.countRejected();
            if (settings.isDebug()) {
                plugin.getSLF4JLogger().warn("Dropped malformed block request from " + player.getName() + ": " + e);
            }
        } finally {
            buf.release();
        }
    }

    private void respond(ServerPlayer player, BlockRequest request) {
        CompoundTag tag = request.data();
        BlockPos pos = request.hit().getBlockPos();
        tag.putInt("x", pos.getX());
        tag.putInt("y", pos.getY());
        tag.putInt("z", pos.getZ());

        if (reach.isOutOfReach(player, pos, player.blockInteractionRange()) || !player.level().isLoaded(pos)) {
            ReceiveData.send(player.getBukkitEntity(), plugin, tag);
            return;
        }

        BlockState state = player.level().getBlockState(pos);
        BlockEntity blockEntity = state.hasBlockEntity() ? player.level().getBlockEntity(pos) : null;
        RegistryFriendlyByteBuf scratch = new RegistryFriendlyByteBuf(
                Unpooled.buffer(),
                player.level().registryAccess());
        try {
            BlockContext context = new BlockContext(
                    player,
                    request.hit(),
                    state,
                    blockEntity,
                    request.showDetails(),
                    tag,
                    scratch);
            for (BlockDataProvider provider : providers.providersFor(
                    state.getBlock().getClass(),
                    blockEntity != null ? blockEntity.getClass() : null)) {
                if (!request.providers().contains(provider)) {
                    continue;
                }
                try {
                    provider.appendServerData(context);
                } catch (Exception e) {
                    logProviderError(provider, e);
                }
            }
        } finally {
            scratch.release();
        }
        ReceiveData.send(player.getBukkitEntity(), plugin, tag);
    }

    private void logProviderError(BlockDataProvider provider, Exception e) {
        if (failedProviders.add(provider)) {
            plugin.getSLF4JLogger().warn("Block data provider " + provider.id() + " failed; further errors suppressed", e);
        } else if (settings.isDebug()) {
            plugin.getSLF4JLogger().warn("Block data provider " + provider.id() + " failed again: " + e);
        }
    }
}
