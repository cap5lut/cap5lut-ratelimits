package com.github.cap5lut.ratelimits;

/**
 * Rate limited acquire wrapper.
 * @param <T> Instance type.
 */
public class RateLimited<T> {
    /**
     * Wrapped acquire.
     */
    protected final T instance;

    /**
     * Rate limits for the acquire.
     */
    private final RateLimit[] rateLimits;

    /**
     * Wrappes an acquire and associates the rate limits to it.
     * @param instance Instance to wrap.
     * @param rateLimits Relevant rate limits.
     */
    public RateLimited(T instance, RateLimit...rateLimits) {
        this.instance = instance;
        this.rateLimits = rateLimits;
    }

    /**
     * Acquires all needed rate limit slots and returns the wrapped acquire.
     * @return Returns the wrapped acquire.
     * @throws InterruptedException if the thread was interrupted.
     */
    public T acquire() throws InterruptedException {
        RateLimit.acquireAll(rateLimits);
        return instance;
    }

    /**
     * Gets all rate limits.
     * @return Rate limits.
     */
    public RateLimit[] rateLimits() {
        RateLimit[] rateLimits = new RateLimit[this.rateLimits.length];
        System.arraycopy(this.rateLimits, 0, rateLimits, 0, rateLimits.length);
        return rateLimits;
    }
}