package com.darkspirit69.jadebridge.session;

/**
 * Fixed-window rate limiter, one instance per player and request type.
 * Window and limit never change mid-window, which is all the Jade protocol needs.
 */
public final class RateLimiter {

    private final int limit;
    private final long windowMillis;
    private long windowStart;
    private int used;

    public RateLimiter(int limitPerSecond) {
        this(limitPerSecond, 1000L);
    }

    public RateLimiter(int limit, long windowMillis) {
        this.limit = Math.max(1, limit);
        this.windowMillis = windowMillis;
    }

    public boolean tryAcquire(long now) {
        if (now - windowStart >= windowMillis) {
            windowStart = now;
            used = 0;
        }
        if (used >= limit) {
            return false;
        }
        used++;
        return true;
    }

    public int limit() {
        return limit;
    }
}
