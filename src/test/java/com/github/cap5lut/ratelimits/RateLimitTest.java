package com.github.cap5lut.ratelimits;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.cap5lut.ratelimits.Measurement.measure;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

abstract class RateLimitTest {
    private final RateLimit.Factory factory;

    RateLimitTest(RateLimit.Factory factory) {
        this.factory = factory;
    }

    @Test
    void acquireAll() {
        measure(() -> {
            RateLimit r1 = factory.create(1, 250, TimeUnit.MILLISECONDS);
            RateLimit r2 = factory.create(1, 500, TimeUnit.MILLISECONDS);

            r1.acquire();
            r2.acquire();

            RateLimit.acquireAll(r1, r2);
            return null;
        }).assertEquals(500, TimeUnit.MILLISECONDS);
    }

    @Test
    void limit() throws InterruptedException {
        AtomicInteger i = new AtomicInteger(0);
        RateLimited<AtomicInteger> num = factory.create(1, 250, TimeUnit.MILLISECONDS).limit(i);
        assertNotNull(num);
        assertEquals(i, num.acquire());
    }

    @Test
    void acquire() {
        measure(() -> {
            RateLimit rateLimit = factory.create(1, 250, TimeUnit.MILLISECONDS);
            rateLimit.acquire();
            rateLimit.acquire();
            return null;
        }).assertEquals(250, TimeUnit.MILLISECONDS);
    }

    @Test
    void acquireAndGetRateID() throws InterruptedException {
        RateLimit rateLimit = factory.create(1, 10, TimeUnit.SECONDS);
        assertEquals(rateLimit.getCurrentRateID(), rateLimit.acquireAndGetRateID());
    }

    @Test
    void cancel() throws InterruptedException {
        RateLimit rateLimit = factory.create(1, 10, TimeUnit.SECONDS);
        rateLimit.acquire();
        assertEquals(0, rateLimit.getRemaining());
        rateLimit.cancel(rateLimit.getCurrentRateID());
        assertEquals(1, rateLimit.getRemaining());
    }

    @Test
    void getCapacity() {
        assertEquals(1, factory.create(1, 10, TimeUnit.SECONDS).getCapacity());
        assertEquals(10, factory.create(10, 10, TimeUnit.SECONDS).getCapacity());
    }

    @Test
    void getNextReset() throws InterruptedException {
        RateLimit rateLimit = factory.create(1, 250, TimeUnit.MILLISECONDS);
        long expectedNextReset = rateLimit.getNextReset() + rateLimit.getResetInterval();
        rateLimit.acquire();
        rateLimit.acquire();
        assertEquals(expectedNextReset, rateLimit.getNextReset());
    }

    @Test
    void getRemaining() throws InterruptedException {
        RateLimit rateLimit = factory.create(1, 250, TimeUnit.MILLISECONDS);
        assertEquals(1, rateLimit.getRemaining());
        rateLimit.acquire();
        assertEquals(0, rateLimit.getRemaining());
        Thread.sleep(400);
        assertEquals(1, rateLimit.getRemaining());
    }

    @Test
    void getCurrentRateID() throws InterruptedException {
        RateLimit rateLimit = factory.create(1, 250, TimeUnit.MILLISECONDS);
        assertEquals(0, rateLimit.getCurrentRateID());
        rateLimit.acquire();
        rateLimit.acquire();
        assertEquals(1, rateLimit.getCurrentRateID());
    }

    @Test
    void getResetInterval() {
        assertEquals(TimeUnit.SECONDS.toNanos(10), factory.create(1, 10, TimeUnit.SECONDS).getResetInterval());
        assertEquals(TimeUnit.MILLISECONDS.toNanos(10), factory.create(1, 10, TimeUnit.MILLISECONDS).getResetInterval());
    }
}