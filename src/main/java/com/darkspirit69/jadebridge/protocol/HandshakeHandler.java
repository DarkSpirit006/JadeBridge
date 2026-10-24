package com.darkspirit69.jadebridge.protocol;

import com.darkspirit69.jadebridge.JadeProtocol;
import com.darkspirit69.jadebridge.Settings;
import com.darkspirit69.jadebridge.provider.ProviderRegistry;
import com.darkspirit69.jadebridge.session.SessionManager;
import com.darkspirit69.jadebridge.util.ShearableBlocks;
import io.netty.buffer.Unpooled;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Handles {@code jade:client_handshake}: validate the protocol version, remember
 * the session and answer with {@code jade:server_handshake} (config overrides,
 * shearable blocks, provider ids).
 *
 * The Jade client sends its handshake once, without retrying, and it can arrive
 * before the client's {@code minecraft:register} does. Bukkit silently drops
 * plugin messages to channels the receiving player has not registered, so in
 * that order the reply is held back and flushed as soon as the channel shows up.
 */
public final class HandshakeHandler {

    private final Plugin plugin;
    private final Settings settings;
    private final ProviderRegistry providers;
    private final SessionManager sessions;
    private final Map<UUID, byte[]> pendingReplies = new HashMap<>();

    public HandshakeHandler(Plugin plugin, Settings settings, ProviderRegistry providers, SessionManager sessions) {
        this.plugin = plugin;
        this.settings = settings;
        this.providers = providers;
        this.sessions = sessions;
    }

    public void handle(Player player, byte[] bytes) {
        if (bytes.length == 0 || bytes.length > JadeProtocol.MAX_INCOMING_SIZE) {
            return;
        }
        String version = readVersion(bytes);
        if (version == null) {
            if (settings.isDebug()) {
                plugin.getSLF4JLogger().warn("Malformed handshake from " + player.getName());
            }
            return;
        }

        if (!JadeProtocol.PROTOCOL_VERSION.equals(version)) {
            // same message and channel Jade's server sends, so the client translates it
            ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
            serverPlayer.sendSystemMessage(
                    Component.translatable("jade.protocolMismatch", "JadeBridge " + plugin.getPluginMeta().getVersion()),
                    false);
            return;
        }

        sessions.establish(player, version, settings);
        byte[] body = ServerHandshake.encode(
                settings.configOverrides(),
                ShearableBlocks.get(),
                providers.blockProviderIds(),
                providers.entityProviderIds());
        if (player.getListeningPluginChannels().contains(JadeProtocol.CHANNEL_SERVER_HANDSHAKE)) {
            sendReply(player, body);
            if (settings.isDebug()) {
                plugin.getSLF4JLogger().info("Jade handshake with " + player.getName() + " (protocol " + version + ")");
            }
        } else {
            pendingReplies.put(player.getUniqueId(), body);
            if (settings.isDebug()) {
                plugin.getSLF4JLogger().info("Jade handshake with " + player.getName() + " (protocol " + version
                        + "); reply held until the channel is registered");
            }
        }
    }

    /**
     * Called when the player registers {@code jade:server_handshake}, which is the
     * earliest moment a reply can actually reach them.
     */
    public void flushPendingReply(Player player) {
        byte[] body = pendingReplies.remove(player.getUniqueId());
        if (body == null) {
            return;
        }
        sendReply(player, body);
        if (settings.isDebug()) {
            plugin.getSLF4JLogger().info("Flushed pending server handshake to " + player.getName());
        }
    }

    public void discardPendingReply(UUID playerId) {
        pendingReplies.remove(playerId);
    }

    private void sendReply(Player player, byte[] body) {
        player.sendPluginMessage(plugin, JadeProtocol.CHANNEL_SERVER_HANDSHAKE, body);
    }

    private static String readVersion(byte[] bytes) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));
        try {
            return buf.readUtf(16);
        } catch (Exception e) {
            return null;
        } finally {
            buf.release();
        }
    }
}
