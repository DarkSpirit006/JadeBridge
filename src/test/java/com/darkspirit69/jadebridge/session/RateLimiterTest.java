package com.darkspirit69.jadebridge.session;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RateLimiterTest {

    @Test
    void allowsUpToTheLimitPerWindow() {
        RateLimiter limiter = new RateLimiter(3);
        assertTrue(limiter.tryAcquire(0));
        assertTrue(limiter.tryAcquire(100));
        assertTrue(limiter.tryAcquire(200));
        assertFalse(limiter.tryAcquire(300));
        assertFalse(limiter.tryAcquire(999));
    }

    @Test
    void windowResetsAfterOneSecond() {
        RateLimiter limiter = new RateLimiter(2);
        assertTrue(limiter.tryAcquire(0));
        assertTrue(limiter.tryAcquire(100));
        assertFalse(limiter.tryAcquire(500));
        assertTrue(limiter.tryAcquire(1000), "new window opens exactly after 1000 ms");
    }

    @Test
    void customWindowLength() {
        RateLimiter limiter = new RateLimiter(1, 100);
        assertTrue(limiter.tryAcquire(0));
        assertFalse(limiter.tryAcquire(99));
        assertTrue(limiter.tryAcquire(100));
    }

    @Test
    void zeroLimitIsClampedToOne() {
        RateLimiter limiter = new RateLimiter(0);
        assertTrue(limiter.tryAcquire(0));
        assertFalse(limiter.tryAcquire(1));
    }
}
