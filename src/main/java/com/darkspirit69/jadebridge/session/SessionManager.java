package com.darkspirit69.jadebridge.session;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import com.darkspirit69.jadebridge.Settings;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class SessionManager implements Listener {

    private final Map<UUID, JadeSession> sessions = new ConcurrentHashMap<>();
    private long totalBlockRequests;
    private long totalEntityRequests;
    private long rejectedRequests;

    /**
     * Creates or replaces the session for a player that just finished a Jade handshake.
     * Limits are read at establishment time so a config reload applies to new sessions.
     */
    public JadeSession establish(Player player, String protocolVersion, Settings settings) {
        JadeSession session = new JadeSession(
                player.getUniqueId(),
                protocolVersion,
                new RateLimiter(settings.blockRequestLimit()),
                new RateLimiter(settings.entityRequestLimit()));
        sessions.put(player.getUniqueId(), session);
        return session;
    }

    public JadeSession get(Player player) {
        return sessions.get(player.getUniqueId());
    }

    public boolean isJadeClient(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    public void countBlockRequest() {
        totalBlockRequests++;
    }

    public void countEntityRequest() {
        totalEntityRequests++;
    }

    public void countRejected() {
        rejectedRequests++;
    }

    public int activeSessions() {
        return sessions.size();
    }

    public long totalBlockRequests() {
        return totalBlockRequests;
    }

    public long totalEntityRequests() {
        return totalEntityRequests;
    }

    public long rejectedRequests() {
        return rejectedRequests;
    }

    public void clearCounters() {
        totalBlockRequests = 0;
        totalEntityRequests = 0;
        rejectedRequests = 0;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        sessions.remove(event.getPlayer().getUniqueId());
    }
}
