package com.github.cap5lut.ratelimits;

class ConcurrentRateLimitTest extends RateLimitTest {
    ConcurrentRateLimitTest() {
        super(ConcurrentRateLimit::new);
    }
}