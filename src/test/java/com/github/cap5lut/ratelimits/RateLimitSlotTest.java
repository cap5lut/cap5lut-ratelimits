package com.github.cap5lut.ratelimits;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitSlotTest {

    @Test
    void cancel() throws InterruptedException {
        RateLimit rateLimit = new ConcurrentRateLimit(1, 250, TimeUnit.MILLISECONDS);
        RateLimitSlot slot = rateLimit.acquire();
        assertEquals(0, rateLimit.getRemaining());
        slot.cancel();
        assertEquals(1, rateLimit.getRemaining());
    }

    @Test
    void getRateLimit() throws InterruptedException {
        RateLimit rateLimit = new ConcurrentRateLimit(1, 10, TimeUnit.SECONDS);
        RateLimitSlot slot = rateLimit.acquire();
        assertSame(rateLimit, slot.getRateLimit());
    }

    @Test
    void isValid() throws InterruptedException {
        RateLimit rateLimit = new ConcurrentRateLimit(1, 250, TimeUnit.MILLISECONDS);
        RateLimitSlot slot = rateLimit.acquire();
        assertTrue(slot.isValid());
        rateLimit.acquire();
        assertFalse(slot.isValid());
    }

    @Test
    void reacquire() throws InterruptedException {
        RateLimit rateLimit = new ConcurrentRateLimit(1, 250, TimeUnit.MILLISECONDS);
        RateLimitSlot slot = rateLimit.acquire();
        assertTrue(slot.isValid());
        rateLimit.acquire();
        assertFalse(slot.isValid());
        slot.reacquire();
        assertTrue(slot.isValid());
    }
}