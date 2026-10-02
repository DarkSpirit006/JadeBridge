package com.darkspirit69.jadebridge.session;

import java.util.UUID;

public final class JadeSession {

    private final UUID playerId;
    private final long establishedAt;
    private final String protocolVersion;
    private final RateLimiter blockRequests;
    private final RateLimiter entityRequests;

    JadeSession(UUID playerId, String protocolVersion, RateLimiter blockRequests, RateLimiter entityRequests) {
        this.playerId = playerId;
        this.protocolVersion = protocolVersion;
        this.establishedAt = System.currentTimeMillis();
        this.blockRequests = blockRequests;
        this.entityRequests = entityRequests;
    }

    public UUID playerId() {
        return playerId;
    }

    public String protocolVersion() {
        return protocolVersion;
    }

    public long establishedAt() {
        return establishedAt;
    }

    public boolean tryAcquireBlockRequest(long now) {
        return blockRequests.tryAcquire(now);
    }

    public boolean tryAcquireEntityRequest(long now) {
        return entityRequests.tryAcquire(now);
    }
}
