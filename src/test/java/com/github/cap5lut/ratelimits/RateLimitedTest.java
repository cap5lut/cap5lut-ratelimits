package com.github.cap5lut.ratelimits;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static com.github.cap5lut.ratelimits.Measurement.measure;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class RateLimitedTest {
    private final RateLimit.Factory[] factories = new RateLimit.Factory[] {
            ConcurrentRateLimit::new
    };

    @Test
    void acquire() {
        for(RateLimit.Factory factory: factories) {
            measure(() -> {
                Integer i = 0;
                RateLimited<Integer> num = new RateLimited<>(i, factory.create(1, 250, TimeUnit.MILLISECONDS));
                assertSame(i, num.acquire());
                return num.acquire();
            })
                    .assertEquals(250, TimeUnit.MILLISECONDS);
        }
    }

    @Test
    void rateLimits() {
        for(RateLimit.Factory factory: factories) {
            RateLimit[] rateLimits = new RateLimit[]{factory.create(1, 250, TimeUnit.MILLISECONDS)};
            assertArrayEquals(rateLimits, rateLimits[0].limit(1).rateLimits());
        }
    }
}