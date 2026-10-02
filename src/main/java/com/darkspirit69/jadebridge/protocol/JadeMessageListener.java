package com.darkspirit69.jadebridge.protocol;

import com.darkspirit69.jadebridge.JadeProtocol;
import com.darkspirit69.jadebridge.Settings;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;

/**
 * Single entry point for all {@code jade:*} plugin messages. Paper delivers them on
 * the server thread, so handlers can touch the world directly.
 */
public final class JadeMessageListener implements PluginMessageListener, Listener {

    private final Settings settings;
    private final HandshakeHandler handshake;
    private final BlockRequestHandler blockRequests;
    private final EntityRequestHandler entityRequests;
    private final org.bukkit.plugin.Plugin plugin;

    public JadeMessageListener(
            org.bukkit.plugin.Plugin plugin,
            Settings settings,
            HandshakeHandler handshake,
            BlockRequestHandler blockRequests,
            EntityRequestHandler entityRequests) {
        this.plugin = plugin;
        this.settings = settings;
        this.handshake = handshake;
        this.blockRequests = blockRequests;
        this.entityRequests = entityRequests;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] bytes) {
        if (!settings.isEnabled()) {
            return;
        }
        switch (channel) {
            case JadeProtocol.CHANNEL_CLIENT_HANDSHAKE -> handshake.handle(player, bytes);
            case JadeProtocol.CHANNEL_REQUEST_BLOCK -> blockRequests.handle(player, bytes);
            case JadeProtocol.CHANNEL_REQUEST_ENTITY -> entityRequests.handle(player, bytes);
            default -> {
            }
        }
    }

    @EventHandler
    public void onChannelRegister(PlayerRegisterChannelEvent event) {
        if (JadeProtocol.CHANNEL_SERVER_HANDSHAKE.equals(event.getChannel())) {
            handshake.flushPendingReply(event.getPlayer());
        }
        if (settings.isDebug() && JadeProtocol.CHANNEL_RECEIVE_DATA.equals(event.getChannel())) {
            plugin.getSLF4JLogger().info("Jade client detected: " + event.getPlayer().getName());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        handshake.discardPendingReply(event.getPlayer().getUniqueId());
    }
}
